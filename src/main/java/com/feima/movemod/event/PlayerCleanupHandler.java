package com.feima.movemod.event;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.SlideAction;
import com.feima.movemod.action.StaminaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 服务端玩家离线时清理所有状态 Map，避免长期运行的内存泄漏。
 *
 * 客户端侧的清理在 {@link com.feima.movemod.client.SlideAnimationController}
 * 里通过 ClientPlayerNetworkEvent.LoggingOut 处理。
 */
@Mod.EventBusSubscriber(modid = FeimaMoveMod.MODID)
public final class PlayerCleanupHandler {

    private PlayerCleanupHandler() {}

    @SubscribeEvent
    public static void onServerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        forgetAll(player.getUUID());
    }

    private static void forgetAll(UUID id) {
        SlideAction.INSTANCE.forget(id);
        StaminaTracker.INSTANCE.forget(id);
    }
}