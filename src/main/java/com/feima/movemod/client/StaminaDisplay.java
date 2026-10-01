package com.feima.movemod.client;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.StaminaTracker;
import com.feima.movemod.config.MoveConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 耐力数字显示 —— 临时占位方案。
 *
 * 用类似 title 的大字号文字在屏幕上显示耐力，等有 HUD 美术资源后
 * 可以把 {@link #onRenderGui} 替换成画贴图条的版本，配置项保持不变。
 *
 * 显示逻辑：
 *   - alwaysShow = true  → 耐力未满时常驻
 *   - alwaysShow = false → 每次数值变化后 hold 一段时间再淡出
 *
 * 数值来源：{@link StaminaTracker#get}（本地玩家双端同步过，权威）。
 */
@Mod.EventBusSubscriber(modid = FeimaMoveMod.MODID, value = Dist.CLIENT)
public final class StaminaDisplay {

    private StaminaDisplay() {}

    /** 上次显示过的整数值，用于检测变化。-1 表示尚未初始化 */
    private static int lastValue = -1;
    /** 剩余保持 tick 数（变化后置为 holdTicks） */
    private static int holdRemaining = 0;

    // ============================================================
    // Tick：检测数值变化、推进淡出计时
    // ============================================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isEnabled()) {
            lastValue = -1;
            holdRemaining = 0;
            return;
        }

        int value = (int) Math.round(StaminaTracker.INSTANCE.get(player));

        // 首次进入世界：静默初始化，不触发显示
        if (lastValue < 0) {
            lastValue = value;
            holdRemaining = 0;
            return;
        }

        if (value != lastValue) {
            lastValue = value;
            if (!MoveConfig.INSTANCE.staminaDisplayAlwaysShow.get()) {
                holdRemaining = MoveConfig.INSTANCE.staminaDisplayHoldTicks.get();
            }
        } else if (holdRemaining > 0) {
            holdRemaining--;
        }
    }

    // ============================================================
    // Render：绘制数字
    // ============================================================
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean alwaysShow = MoveConfig.INSTANCE.staminaDisplayAlwaysShow.get();
        double max = StaminaTracker.INSTANCE.getMax();
        double cur = StaminaTracker.INSTANCE.get(player);

        // 常显模式下，满耐力不显示（避免总是糊在屏幕上）
        if (alwaysShow && cur >= max) return;

        // 变化淡出模式下，hold 用完就不再显示
        if (!alwaysShow && holdRemaining <= 0) return;

        // ---- 透明度 ----
        int alpha = 255;
        if (!alwaysShow) {
            int fade = MoveConfig.INSTANCE.staminaDisplayFadeTicks.get();
            if (fade > 0 && holdRemaining < fade) {
                alpha = (int) (255L * holdRemaining / fade);
            }
        }
        if (alpha <= 0) return;

        // ---- 文本 ----
        String text = buildText(player, cur, max);
        if (text.isEmpty()) return;

        int rgb = parseColor(MoveConfig.INSTANCE.staminaDisplayColor.get());
        int argb = (alpha << 24) | rgb;

        float scale = MoveConfig.INSTANCE.staminaDisplayScale.get().floatValue();
        boolean shadow = MoveConfig.INSTANCE.staminaDisplayShadow.get();

        GuiGraphics g = event.getGuiGraphics();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float cx, cy;
        String pos = normalizePos(MoveConfig.INSTANCE.staminaDisplayPosition.get());
        switch (pos) {
            case "bottom" -> {
                cx = screenW / 2.0F;
                cy = screenH - 60.0F;
            }
            case "topleft" -> {
                cx = 60.0F;
                cy = 40.0F;
            }
            case "topright" -> {
                cx = screenW - 60.0F;
                cy = 40.0F;
            }
            default -> { // center
                cx = screenW / 2.0F;
                cy = screenH / 3.0F;
            }
        }

        int textW = mc.font.width(text);
        int lineH = mc.font.lineHeight;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0.0F);
        pose.scale(scale, scale, 1.0F);
        // 以 (cx, cy) 为中心绘制
        g.drawString(mc.font, text, -textW / 2, -lineH / 2, argb, shadow);
        pose.popPose();
    }

    // ============================================================
    // 工具
    // ============================================================
    private static boolean isEnabled() {
        if (!MoveConfig.INSTANCE.enabled.get()) return false;
        if (!MoveConfig.INSTANCE.staminaEnabled.get()) return false;
        if (!MoveConfig.INSTANCE.staminaDisplayEnabled.get()) return false;
        return true;
    }

    private static String buildText(LocalPlayer player, double cur, double max) {
        String mode = MoveConfig.INSTANCE.staminaDisplayMode.get();
        if (mode == null) mode = "value";

        int value = (int) Math.round(cur);
        int maxInt = (int) Math.round(max);

        return switch (mode.toLowerCase()) {
            case "valuemax" -> value + "/" + maxInt;
            case "percent" -> {
                int pct = max > 0.0D ? (int) Math.round(cur / max * 100.0D) : 0;
                yield pct + "%";
            }
            case "level" -> levelRoman(StaminaTracker.INSTANCE.levelOf(player));
            default -> String.valueOf(value); // value
        };
    }

    private static String levelRoman(int level) {
        return switch (level) {
            case StaminaTracker.LEVEL_1 -> "I";
            case StaminaTracker.LEVEL_2 -> "II";
            default -> "III";
        };
    }

    /** 解析 "#RRGGBB" 或 "RRGGBB" 为 0x00RRGGBB，非法时返回白色。 */
    private static int parseColor(String s) {
        if (s == null) return 0xFFFFFF;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return 0xFFFFFF;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private static String normalizePos(String s) {
        if (s == null) return "center";
        // 把 top_left / TopLeft / top-left 都规整成 topleft
        return s.trim().toLowerCase().replace("_", "").replace("-", "");
    }
}