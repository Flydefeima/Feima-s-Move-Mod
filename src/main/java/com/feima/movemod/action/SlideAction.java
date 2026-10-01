package com.feima.movemod.action;

import com.feima.movemod.config.MoveConfig;
import com.feima.movemod.network.NetworkHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑铲动作 — FPS 风格方向模型（锥形限幅 + 角惯性）+ 三档耐力系统。
 *
 * 【方向模型】 触发时锁定 initialYaw（永不变），每 tick 用锥形限幅 + 角惯性
 *             把 currentYaw 平滑逼近目标方向。
 *
 * 【耐力】     启动门槛：耐力 ≥ stamina.costOnStart
 *              启动消耗：costOnStart
 *              每 tick 消耗：costPerTick，耗尽即结束
 *              初速 = 按【启动前】耐力比例决定的档位速度（三档固定值）
 *              滑铲跳水平速度也按当前档位缩放（jumpScaleFor）
 *
 * 【饥饿】     独立开关，通过 Player#causeFoodExhaustion 走原版机制
 */
public final class SlideAction {

    public static final SlideAction INSTANCE = new SlideAction();

    private static final double MIN_SPEED = 1.0E-4;
    /** 收到服务端权威包时，本地滑铲处于此 tick 数之前允许覆盖初速 */
    private static final int SPEED_RESYNC_TICKS = 5;

    private final Map<UUID, State> serverStates = new ConcurrentHashMap<>();
    private final Map<UUID, State> clientStates = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> serverTriggerCds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> clientTriggerCds = new ConcurrentHashMap<>();

    private SlideAction() {}

    private Map<UUID, State> states(Player p) {
        return p.level().isClientSide ? clientStates : serverStates;
    }

    private Map<UUID, Integer> triggerCds(Player p) {
        return p.level().isClientSide ? clientTriggerCds : serverTriggerCds;
    }

    public boolean isSliding(Player player) {
        return states(player).containsKey(player.getUUID());
    }

    /** 当前滑铲的实际初速（供网络广播读取）；不在滑铲中返回 0。 */
    public double currentSpeed(Player player) {
        State s = states(player).get(player.getUUID());
        return s == null ? 0.0D : s.speed;
    }

    // ============================================================
    // 启动
    // ============================================================
    public boolean tryStart(Player player) {
        if (player.level().isClientSide) return false;
        if (serverStates.containsKey(player.getUUID())) return false;
        if (!canStart(player)) return false;

        commitStart(player);
        return true;
    }

    public boolean tryStartClient(LocalPlayer player) {
        if (!player.level().isClientSide) return false;
        if (clientStates.containsKey(player.getUUID())) return false;
        if (!canStart(player)) return false;

        commitStart(player);
        return true;
    }

    private boolean canStart(Player player) {
        if (!MoveConfig.INSTANCE.enabled.get()) return false;
        if (player.isSpectator() || player.isDeadOrDying()) return false;
        if (!player.onGround()) return false;
        if (isTriggerOnCd(player)) return false;
        if (!StaminaTracker.INSTANCE.canStart(player)) return false;
        return true;
    }

    /**
     * 两端共用：真正进入滑铲态。调用方已确保 canStart() 通过。
     *
     * 顺序：先用【启动前】的耐力决定档位速度 → 再消耗启动成本。
     * 这样玩家看到的是“我点的时候是几档，这一滑就跑几档”。
     */
    private void commitStart(Player player) {
        float yaw = player.getYRot();
        player.setSprinting(false);

        double speed = StaminaTracker.INSTANCE.speedFor(player);

        StaminaTracker.INSTANCE.consume(
                player, MoveConfig.INSTANCE.staminaCostOnStart.get());

        states(player).put(player.getUUID(), new State(yaw, speed));
        setTriggerCd(player);
        player.refreshDimensions();
    }

    // ============================================================
    // 滑铲跳
    // ============================================================
    public boolean trySlideJump(Player player) {
        if (player.level().isClientSide && !(player instanceof LocalPlayer)) return false;

        State state = states(player).get(player.getUUID());
        if (state == null) return false;

        Vec3 dir = resolveJumpDirection(player, state);
        // 水平速度按当前档位缩放；垂直不受影响
        double scale = StaminaTracker.INSTANCE.jumpScaleFor(player);
        double forward = MoveConfig.INSTANCE.slideJumpForward.get() * scale;
        double up      = MoveConfig.INSTANCE.slideJumpUp.get();

        double currentY = player.getDeltaMovement().y;
        double newY = Math.max(currentY, up);

        player.setDeltaMovement(dir.x * forward, newY, dir.z * forward);
        player.hasImpulse = true;
        stop(player);
        return true;
    }

    private Vec3 resolveJumpDirection(Player player, State state) {
        if (MoveConfig.INSTANCE.slideJumpFollowLook.get()) {
            return yawToHorizontal(player.getYRot());
        }
        return state.initialDirection;
    }

    // ============================================================
    // 远端同步
    // ============================================================
    /**
     * @param stamina 服务端权威耐力值
     * @param speed   服务端实际使用的滑铲初速（sliding=false 时忽略）
     */
    public void applyRemoteState(Player player, boolean sliding, double stamina, double speed) {
        if (!player.level().isClientSide) return;

        // ---- 本地玩家 ----
        if (player instanceof LocalPlayer) {
            // 无条件同步耐力
            if (MoveConfig.INSTANCE.staminaEnabled.get()) {
                StaminaTracker.INSTANCE.set(player, stamina);
            }

            if (sliding) {
                // 服务端确认启动：本地滑铲刚开始时，用权威初速覆盖本地预测
                State state = clientStates.get(player.getUUID());
                if (state != null && state.ticks <= SPEED_RESYNC_TICKS) {
                    state.speed = speed;
                }
            } else {
                // 服务端说未滑铲：可能是拒绝，也可能是结束
                if (clientStates.remove(player.getUUID()) != null) {
                    player.refreshDimensions();
                }
            }
            return;
        }

        // ---- 远端玩家 ----
        if (sliding) {
            if (clientStates.containsKey(player.getUUID())) return;
            clientStates.put(player.getUUID(), new State(player.getYRot(), 0.0D));
            player.refreshDimensions();
        } else {
            if (clientStates.remove(player.getUUID()) != null) {
                player.refreshDimensions();
            }
        }
    }

    // ============================================================
    // Tick
    // ============================================================
    public void tick(Player player) {
        if (player.level().isClientSide && !(player instanceof LocalPlayer)) return;

        tickTriggerCd(player);
        StaminaTracker.INSTANCE.tick(player);

        State state = states(player).get(player.getUUID());
        if (state == null) return;

        state.ticks++;

        // ---- 耐力持续消耗 ----
        if (MoveConfig.INSTANCE.staminaEnabled.get()) {
            double perTick = MoveConfig.INSTANCE.staminaCostPerTick.get();
            if (perTick > 0.0D) {
                StaminaTracker.INSTANCE.consumeUpTo(player, perTick);
                if (StaminaTracker.INSTANCE.get(player) <= 0.0D) {
                    stop(player);
                    return;
                }
            }
        }

        // ---- 饥饿消耗（原版机制）----
        if (MoveConfig.INSTANCE.hungerEnabled.get()) {
            double exhaust = MoveConfig.INSTANCE.hungerPerTick.get();
            if (exhaust > 0.0D) {
                player.causeFoodExhaustion((float) exhaust);
            }
        }

        // ---- 方向：锥形限幅 + 角惯性 ----
        if (MoveConfig.INSTANCE.followLook.get()) {
            float deltaYaw = Mth.wrapDegrees(player.getYRot() - state.initialYaw);
            double maxOffset = MoveConfig.INSTANCE.maxTurnOffset.get();

            double targetOffset = Mth.clamp(deltaYaw, -maxOffset, maxOffset);
            float targetYaw = Mth.wrapDegrees((float) (state.initialYaw + targetOffset));

            float maxStep = MoveConfig.INSTANCE.turnSpeed.get().floatValue();
            float diff = Mth.wrapDegrees(targetYaw - state.currentYaw);

            if (maxStep <= 0.0F) {
                state.currentYaw = targetYaw;
            } else {
                state.currentYaw = Mth.wrapDegrees(
                        state.currentYaw + Mth.clamp(diff, -maxStep, maxStep)
                );
            }
            state.direction = yawToHorizontal(state.currentYaw);
        }

        // ---- 写入运动 ----
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(
                state.direction.x * state.speed,
                motion.y,
                state.direction.z * state.speed
        );

        int decayDelay  = MoveConfig.INSTANCE.decayDelay.get();
        double friction = MoveConfig.INSTANCE.friction.get();
        double endSpeed = MoveConfig.INSTANCE.endSpeed.get();

        if (state.ticks >= decayDelay) state.speed *= friction;

        if (state.speed <= endSpeed || state.speed < MIN_SPEED || !player.onGround()) {
            stop(player);
        }
    }

    // ============================================================
    // 停止
    // ============================================================
    public void stop(Player player) {
        if (states(player).remove(player.getUUID()) == null) return;

        player.refreshDimensions();
        if (!player.level().isClientSide) {
            NetworkHandler.broadcastSlideState(player, false);
        }
    }

    // ============================================================
    // 触发 CD
    // ============================================================
    private boolean isTriggerOnCd(Player player) {
        Integer cd = triggerCds(player).get(player.getUUID());
        return cd != null && cd > 0;
    }

    private void setTriggerCd(Player player) {
        int cd = MoveConfig.INSTANCE.slideTriggerCd.get();
        if (cd <= 0) {
            triggerCds(player).remove(player.getUUID());
        } else {
            triggerCds(player).put(player.getUUID(), cd);
        }
    }

    private void tickTriggerCd(Player player) {
        Map<UUID, Integer> map = triggerCds(player);
        UUID id = player.getUUID();
        Integer cd = map.get(id);
        if (cd == null || cd <= 0) return;
        map.put(id, cd - 1);
    }

    // ============================================================
    // 玩家离线清理
    // ============================================================
    public void forget(UUID id) {
        serverStates.remove(id);
        clientStates.remove(id);
        serverTriggerCds.remove(id);
        clientTriggerCds.remove(id);
    }

    // ============================================================
    // 工具
    // ============================================================
    private static Vec3 yawToHorizontal(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
    }

    private static final class State {
        final float initialYaw;
        final Vec3 initialDirection;
        Vec3 direction;
        float currentYaw;
        double speed;
        int ticks;

        State(float yaw, double speed) {
            this.initialYaw = yaw;
            this.initialDirection = yawToHorizontal(yaw);
            this.direction = this.initialDirection;
            this.currentYaw = yaw;
            this.speed = speed;
        }
    }
}