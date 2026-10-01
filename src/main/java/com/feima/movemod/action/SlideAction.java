package com.feima.movemod.action;

import com.feima.movemod.config.MoveConfig;
import com.feima.movemod.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlideAction {

    public static final SlideAction INSTANCE = new SlideAction();

    private static final double MIN_SPEED = 1.0E-4;
    private static final int SPEED_RESYNC_TICKS = 5;

    /** 撞墙判定阈值：实际水平位移 / 上一 tick 写入速度，低于此值视为撞墙。 */
    private static final double WALL_STOP_RATIO = 0.3D;

    /** 滑铲启动前向正前方探测的距离（方块）。 */
    private static final double SPACE_CHECK_DIST = 0.35D;

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

        // 需要疾跑（可配置）
        if (MoveConfig.INSTANCE.requireSprint.get() && !player.isSprinting()) return false;

        // 需要向前移动输入（后退 / 侧移 / 静止都不触发）
        if (!hasForwardInput(player)) return false;

        // 前方需要有足够空间，避免贴墙启动时模型抖动
        if (!hasSpaceToSlide(player)) return false;

        // 耐力不足且未开启"空耐力也能滑" → 拒绝
        if (!MoveConfig.INSTANCE.allowWhenEmpty.get()
                && MoveConfig.INSTANCE.staminaEnabled.get()
                && !StaminaTracker.INSTANCE.canStart(player)) {
            return false;
        }

        return true;
    }

    /**
     * 是否按下了"前进"键。
     * <p>
     * 直接读物理按键 {@code keyUp.isDown()}，不用 {@code forwardImpulse}：
     * 后者由 {@code Input.tick()} 写入，边沿帧会读到 0，导致误判。
     * <p>
     * 服务端拿不到玩家按键，直接放行；客户端已在发包前严格预检。
     */
    private boolean hasForwardInput(Player player) {
        if (!(player instanceof LocalPlayer)) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return true;
        return mc.options.keyUp.isDown();
    }

    /**
     * 前方是否有滑铲碰撞箱能通过的空间。
     * <p>
     * 以玩家为起点、朝向前方 {@link #SPACE_CHECK_DIST} 格处，
     * 用滑铲尺寸的 AABB 做碰撞检测。贴墙时不通过，避免启动瞬间抖动。
     */
    private boolean hasSpaceToSlide(Player player) {
        double w = MoveConfig.INSTANCE.hitboxWidth.get() / 2.0D;
        double h = MoveConfig.INSTANCE.hitboxHeight.get();

        Vec3 fwd = yawToHorizontal(player.getYRot());
        double cx = player.getX() + fwd.x * SPACE_CHECK_DIST;
        double cz = player.getZ() + fwd.z * SPACE_CHECK_DIST;

        AABB box = new AABB(
                cx - w, player.getY(), cz - w,
                cx + w, player.getY() + h, cz + w
        );
        return player.level().noCollision(player, box);
    }

    private void commitStart(Player player) {
        float yaw = player.getYRot();
        player.setSprinting(false);

        boolean lowStamina = !StaminaTracker.INSTANCE.canStart(player);
        double speed;
        if (lowStamina && MoveConfig.INSTANCE.staminaEnabled.get()) {
            speed = MoveConfig.INSTANCE.staminaLevel3Speed.get();
        } else {
            speed = StaminaTracker.INSTANCE.speedFor(player);
        }
        StaminaTracker.INSTANCE.consumeUpTo(
                player, MoveConfig.INSTANCE.staminaCostOnStart.get());

        State state = new State(yaw, speed);
        state.lastEndX = player.getX();
        state.lastEndZ = player.getZ();
        states(player).put(player.getUUID(), state);
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
    public void applyRemoteState(Player player, boolean sliding, double stamina, double speed) {
        if (!player.level().isClientSide) return;

        if (player instanceof LocalPlayer) {
            if (MoveConfig.INSTANCE.staminaEnabled.get()) {
                StaminaTracker.INSTANCE.set(player, stamina);
            }

            if (sliding) {
                State state = clientStates.get(player.getUUID());
                if (state == null) {
                    // 客户端预测被拒 / 未建立本地状态，但服务端权威接受了
                    // → 补建本地状态，让动画 / HUD / 本地速度覆盖生效
                    state = new State(player.getYRot(), speed);
                    state.lastEndX = player.getX();
                    state.lastEndZ = player.getZ();
                    clientStates.put(player.getUUID(), state);
                    player.refreshDimensions();
                } else if (state.ticks <= SPEED_RESYNC_TICKS) {
                    state.speed = speed;
                }
            } else {
                if (clientStates.remove(player.getUUID()) != null) {
                    player.refreshDimensions();
                }
            }
            return;
        }

        if (sliding) {
            if (clientStates.containsKey(player.getUUID())) return;
            State s = new State(player.getYRot(), 0.0D);
            s.lastEndX = player.getX();
            s.lastEndZ = player.getZ();
            clientStates.put(player.getUUID(), s);
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

        // ---- 撞墙检测（硬编码阈值 WALL_STOP_RATIO）----
        if (state.ticks > 1) {
            double dx = player.getX() - state.lastEndX;
            double dz = player.getZ() - state.lastEndZ;
            double actualH = Math.sqrt(dx * dx + dz * dz);
            if (actualH < state.lastAppliedSpeed * WALL_STOP_RATIO) {
                stop(player);
                return;
            }
        }

        // ---- 耐力持续消耗 ----
        if (MoveConfig.INSTANCE.staminaEnabled.get()) {
            double perTick = MoveConfig.INSTANCE.staminaCostPerTick.get();
            if (perTick > 0.0D) {
                StaminaTracker.INSTANCE.consumeUpTo(player, perTick);
                if (!MoveConfig.INSTANCE.allowWhenEmpty.get()
                        && StaminaTracker.INSTANCE.get(player) <= 0.0D) {
                    stop(player);
                    return;
                }
            }
        }

        // ---- 饥饿消耗 ----
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

        state.lastAppliedSpeed = state.speed;
        state.lastEndX = player.getX();
        state.lastEndZ = player.getZ();

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

    public void forget(UUID id) {
        serverStates.remove(id);
        clientStates.remove(id);
        serverTriggerCds.remove(id);
        clientTriggerCds.remove(id);
    }

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
        double lastAppliedSpeed;
        double lastEndX;
        double lastEndZ;

        State(float yaw, double speed) {
            this.initialYaw = yaw;
            this.initialDirection = yawToHorizontal(yaw);
            this.direction = this.initialDirection;
            this.currentYaw = yaw;
            this.speed = speed;
        }
    }
}