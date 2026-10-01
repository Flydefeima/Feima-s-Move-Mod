package com.feima.movemod.client;

import com.feima.movemod.FeimaMoveMod;
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

    /** 用于跳跃键的边沿检测 */
    private static boolean wasJumpDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // ---- 滑铲键 ----
        while (KeyBindings.SLIDE.consumeClick()) {
            if (player != null) {
                // 乐观预测：本地立即开始，视觉零延迟
                SlideAction.INSTANCE.tryStartClient(player);
            }
            // 通知服务端；服务端回执（接受→广播 true；拒绝→发 false + 权威耐力）
            NetworkHandler.sendSlide();
        }

        // ---- 跳跃键（滑铲跳）----
        // LivingJumpEvent 在某些 Forge 版本客户端不触发，
        // 这里用 keyJump 边沿检测兜底：本地立即结束滑铲并应用滑铲跳速度，
        // 同时发一个包让服务端立即同步执行，避免 RTT 期间的视觉延迟。
        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !wasJumpDown && player != null
                && SlideAction.INSTANCE.isSliding(player)) {
            SlideAction.INSTANCE.trySlideJump(player);
            NetworkHandler.sendSlideJump();
        }
        wasJumpDown = jumpDown;
    }
}