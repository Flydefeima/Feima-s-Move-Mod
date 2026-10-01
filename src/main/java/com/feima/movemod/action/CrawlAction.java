package com.feima.movemod.action;

import com.feima.movemod.config.MoveConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 趴下（Crawl）—— 使用原版 {@link Pose#SWIMMING}。
 *
 * 碰撞箱 / 眼高完全由原版 Player.getDimensions / getEyeHeight 处理，
 * 不使用滑铲的 PlayerHitboxMixin / EyeHeightMixin。
 *
 * 触发：需要 onGround
 * 恢复：不检查 onGround（允许趴下时跳跃起身）
 * 互斥：正在滑铲时无法趴下
 */
public final class CrawlAction {

    public static final CrawlAction INSTANCE = new CrawlAction();

    private final Set<UUID> serverCrawling = ConcurrentHashMap.newKeySet();
    private final Set<UUID> clientCrawling = ConcurrentHashMap.newKeySet();

    private CrawlAction() {}

    private Set<UUID> data(Player p) {
        return p.level().isClientSide ? clientCrawling : serverCrawling;
    }

    public boolean isCrawling(Player player) {
        return data(player).contains(player.getUUID());
    }

    // ---------------- 服务端 ----------------
    public boolean tryStart(Player player) {
        if (player.level().isClientSide) return false;
        if (isCrawling(player)) return false;
        if (!canStart(player)) return false;
        data(player).add(player.getUUID());
        player.refreshDimensions();
        return true;
    }

    public void stop(Player player) {
        if (!data(player).remove(player.getUUID())) return;
        player.refreshDimensions();
    }

    private boolean canStart(Player player) {
        if (!MoveConfig.INSTANCE.enabled.get()) return false;
        if (!MoveConfig.INSTANCE.crawlEnabled.get()) return false;
        if (player.isSpectator() || player.isDeadOrDying()) return false;
        if (!player.onGround()) return false;
        if (SlideAction.INSTANCE.isSliding(player)) return false; // 与滑铲互斥
        return true;
    }

    // ---------------- 客户端预测 ----------------
    public boolean tryStartClient(LocalPlayer player) {
        if (!player.level().isClientSide) return false;
        if (isCrawling(player)) return false;
        if (!canStart(player)) return false;
        data(player).add(player.getUUID());
        player.refreshDimensions();
        return true;
    }

    public void stopClient(LocalPlayer player) {
        if (!data(player).remove(player.getUUID())) return;
        player.refreshDimensions();
    }

    // ---------------- 远端同步 ----------------
    public void applyRemoteState(Player player, boolean crawling) {
        if (!player.level().isClientSide) return;
        boolean changed = crawling
                ? data(player).add(player.getUUID())
                : data(player).remove(player.getUUID());
        if (changed) player.refreshDimensions();
    }

    public void forget(UUID id) {
        serverCrawling.remove(id);
        clientCrawling.remove(id);
    }

    /** 每 tick 由 PlayerPoseMixin 在 updatePlayerPose() TAIL 处调用。 */
    public void forcePose(Player player) {
        if (!isCrawling(player)) return;
        if (!MoveConfig.INSTANCE.enabled.get()) return;
        if (!MoveConfig.INSTANCE.crawlEnabled.get()) return;
        if (SlideAction.INSTANCE.isSliding(player)) return;
        if (player.getPose() != Pose.SWIMMING) {
            player.setPose(Pose.SWIMMING);
            player.refreshDimensions();
        }
    }
}