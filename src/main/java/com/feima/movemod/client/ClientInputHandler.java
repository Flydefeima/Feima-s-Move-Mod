package com.feima.movemod.client;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.CrawlAction;
import com.feima.movemod.action.SlideAction;
import com.feima.movemod.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FeimaMoveMod.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {

    private ClientInputHandler() {}

    /** 跳跃键的边沿检测 */
    private static boolean wasJumpDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // ---- 滑铲键（C）----
        // 只有本地预检通过才发包；本地拒绝则一切都不发生（服务端不会接到请求）。
        while (KeyBindings.SLIDE.consumeClick()) {
            if (player != null && SlideAction.INSTANCE.tryStartClient(player)) {
                NetworkHandler.sendSlide();
            }
        }

        // ---- 趴下键（Z，切换式）----
        while (KeyBindings.CRAWL.consumeClick()) {
            if (player == null) break;

            if (CrawlAction.INSTANCE.isCrawling(player)) {
                CrawlAction.INSTANCE.stopClient(player);
                NetworkHandler.sendCrawlSet(false);
            } else if (CrawlAction.INSTANCE.tryStartClient(player)) {
                NetworkHandler.sendCrawlSet(true);
            }
        }

        // ---- 跳跃键（滑铲跳）----
        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !wasJumpDown && player != null
                && SlideAction.INSTANCE.isSliding(player)) {
            SlideAction.INSTANCE.trySlideJump(player);
            NetworkHandler.sendSlideJump();
        }
        wasJumpDown = jumpDown;
    }
}