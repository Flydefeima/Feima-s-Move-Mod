package com.feima.movemod.event;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.CrawlAction;
import com.feima.movemod.action.SlideAction;
import com.feima.movemod.action.StaminaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

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
        CrawlAction.INSTANCE.forget(id);
    }
}