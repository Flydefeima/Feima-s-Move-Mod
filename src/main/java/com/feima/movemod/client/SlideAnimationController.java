package com.feima.movemod.client;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.SlideAction;
import com.feima.movemod.action.StaminaTracker;
import com.feima.movemod.config.MoveConfig;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = FeimaMoveMod.MODID, value = Dist.CLIENT)
public final class SlideAnimationController {

    private static final ResourceLocation SLIDE_ANIMATION =
            new ResourceLocation(FeimaMoveMod.MODID, "sliding");

    private static final Map<UUID, Boolean> lastSliding = new ConcurrentHashMap<>();

    private SlideAnimationController() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            lastSliding.clear();
            return;
        }

        boolean enabled = MoveConfig.INSTANCE.enabled.get();

        for (AbstractClientPlayer player : level.players()) {
            UUID id = player.getUUID();

            boolean sliding = enabled && SlideAction.INSTANCE.isSliding(player);
            Boolean prevBoxed = lastSliding.put(id, sliding);
            boolean prev = prevBoxed != null && prevBoxed;

            if (prev == sliding) continue;

            if (sliding) {
                play(player, SLIDE_ANIMATION);
            } else {
                play(player, null);
            }
        }
    }

    /** 客户端离线清理：删除客户端所有与该玩家相关的状态 */
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (event.getPlayer() == null) return;
        UUID id = event.getPlayer().getUUID();
        forget(id);
        SlideAction.INSTANCE.forget(id);
        StaminaTracker.INSTANCE.forget(id);
    }

    public static void forget(UUID id) {
        lastSliding.remove(id);
    }

    private static void play(AbstractClientPlayer player, ResourceLocation animation) {
        ModifierLayer<IAnimation> layer = getLayer(player);
        if (layer == null) return;

        if (animation == null) {
            layer.setAnimation(null);
            return;
        }

        var anim = PlayerAnimationRegistry.getAnimation(animation);
        if (anim == null) {
            FeimaMoveMod.LOGGER.warn("[Feima Move] 找不到动画: {}", animation);
            return;
        }
        layer.setAnimation(new KeyframeAnimationPlayer(anim));
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
        return (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(PlayerAnimationSetup.LAYER_ID);
    }
}