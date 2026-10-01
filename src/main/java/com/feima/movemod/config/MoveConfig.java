package com.feima.movemod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class MoveConfig {

    public static final ForgeConfigSpec SPEC;
    public static final MoveConfig INSTANCE;

    static {
        Pair<MoveConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(MoveConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ============================================================
    // 字段
    // ============================================================
    // ---- slide ----
    public final ForgeConfigSpec.BooleanValue enabled;
    public final ForgeConfigSpec.BooleanValue requireSprint;
    public final ForgeConfigSpec.BooleanValue allowWhenEmpty;

    // motion（直接挂在 slide 下）
    public final ForgeConfigSpec.DoubleValue startSpeed;
    public final ForgeConfigSpec.IntValue    decayDelay;
    public final ForgeConfigSpec.DoubleValue friction;
    public final ForgeConfigSpec.DoubleValue endSpeed;
    public final ForgeConfigSpec.IntValue    slideTriggerCd;

    // steering
    public final ForgeConfigSpec.BooleanValue followLook;
    public final ForgeConfigSpec.DoubleValue  turnFactor;
    public final ForgeConfigSpec.DoubleValue  maxTurnOffset;
    public final ForgeConfigSpec.DoubleValue  turnOffsetZeroYaw;
    public final ForgeConfigSpec.DoubleValue  turnSpeed;

    // jump
    public final ForgeConfigSpec.DoubleValue  slideJumpForward;
    public final ForgeConfigSpec.DoubleValue  slideJumpUp;
    public final ForgeConfigSpec.BooleanValue slideJumpFollowLook;

    // stamina
    public final ForgeConfigSpec.BooleanValue staminaEnabled;
    public final ForgeConfigSpec.DoubleValue  staminaMax;
    public final ForgeConfigSpec.DoubleValue  staminaCostOnStart;
    public final ForgeConfigSpec.DoubleValue  staminaCostPerTick;
    public final ForgeConfigSpec.DoubleValue  staminaRegenPerTick;
    public final ForgeConfigSpec.IntValue     staminaRegenDelayTicks;
    public final ForgeConfigSpec.DoubleValue  staminaLevel1Threshold;
    public final ForgeConfigSpec.DoubleValue  staminaLevel2Threshold;
    public final ForgeConfigSpec.DoubleValue  staminaLevel2Speed;
    public final ForgeConfigSpec.DoubleValue  staminaLevel3Speed;

    // stamina display
    public final ForgeConfigSpec.BooleanValue        staminaDisplayEnabled;
    public final ForgeConfigSpec.ConfigValue<String> staminaDisplayMode;
    public final ForgeConfigSpec.BooleanValue        staminaDisplayAlwaysShow;
    public final ForgeConfigSpec.IntValue            staminaDisplayHoldTicks;
    public final ForgeConfigSpec.IntValue            staminaDisplayFadeTicks;
    public final ForgeConfigSpec.ConfigValue<String> staminaDisplayPosition;
    public final ForgeConfigSpec.DoubleValue         staminaDisplayScale;
    public final ForgeConfigSpec.ConfigValue<String> staminaDisplayColor;
    public final ForgeConfigSpec.BooleanValue        staminaDisplayShadow;

    // hunger（直接挂在 slide 下）
    public final ForgeConfigSpec.BooleanValue hungerEnabled;
    public final ForgeConfigSpec.DoubleValue  hungerPerTick;

    // hitbox
    public final ForgeConfigSpec.DoubleValue hitboxWidth;
    public final ForgeConfigSpec.DoubleValue hitboxHeight;
    public final ForgeConfigSpec.DoubleValue eyeHeight;

    // ---- crawl（与 slide 同级）----
    public final ForgeConfigSpec.BooleanValue crawlEnabled;

    // ============================================================
    // 构造
    // ============================================================
    private MoveConfig(ForgeConfigSpec.Builder b) {

        // ============================================================
        // slide
        // ============================================================
        b.comment("滑铲").push("slide");

        enabled = b.comment("滑铲总开关").define("enabled", true);

        requireSprint = b
                .comment("true 时需要疾跑状态才能触发滑铲。")
                .define("requireSprint", false);

        allowWhenEmpty = b
                .comment("true 时耐力为 0 也能滑铲，速度降为三档（最慢）。",
                         "false 时耐力不足直接拒绝滑铲。")
                .define("allowWhenEmpty", true);

        // ---------- motion ----------
        startSpeed = b
                .comment("一档初速度（方块/tick）")
                .defineInRange("startSpeed", 0.6D, 0.0D, 5.0D);
        decayDelay = b
                .comment("速度开始衰减前的保持时长（tick）")
                .defineInRange("decayDelay", 3, 0, 200);
        friction = b
                .comment("每 tick 速度衰减系数")
                .defineInRange("friction", 0.9D, 0.0D, 1.0D);
        endSpeed = b
                .comment("末速度，速度降到 ≤ 此值则结束滑铲")
                .defineInRange("endSpeed", 0.2D, 0.0D, 5.0D);
        slideTriggerCd = b
                .comment("两次滑铲启动之间的最短间隔（tick）")
                .defineInRange("slideTriggerCd", 22, 0, 200);

        // ---------- steering ----------
        b.comment("转向").push("steering");
        followLook = b
                .comment("是否允许视角影响滑铲方向")
                .define("followLook", true);
        turnFactor = b
                .comment("方向跟随视角的比例（0~1）")
                .defineInRange("turnFactor", 0.5D, 0.0D, 1.0D);
        maxTurnOffset = b
                .comment("相对初始方向的最大偏移角度（度）")
                .defineInRange("maxTurnOffset", 45.0D, 0.0D, 180.0D);
        turnOffsetZeroYaw = b
                .comment("超过此视角偏移则目标方向归零（度）")
                .defineInRange("turnOffsetZeroYaw", 120.0D, 0.0D, 180.0D);
        turnSpeed = b
                .comment("方向追赶视角的最大角速度（度/tick）")
                .defineInRange("turnSpeed", 3.0D, 0.0D, 30.0D);
        b.pop();

        // ---------- jump ----------
        b.comment("滑铲跳").push("jump");
        slideJumpForward = b
                .comment("水平速度（方块/tick）")
                .defineInRange("slideJumpForward", 1.0D, 0.0D, 5.0D);
        slideJumpUp = b
                .comment("向上速度（方块/tick）")
                .defineInRange("slideJumpUp", 0.42D, 0.0D, 5.0D);
        slideJumpFollowLook = b
                .comment("true 用当前视角方向，false 用滑铲初始方向")
                .define("slideJumpFollowLook", true);
        b.pop();

        // ---------- stamina ----------
        b.comment("耐力").push("stamina");
        staminaEnabled = b
                .comment("是否启用耐力")
                .define("enabled", true);
        staminaMax = b
                .comment("耐力上限")
                .defineInRange("max", 100.0D, 1.0D, 10000.0D);
        staminaCostOnStart = b
                .comment("启动时一次性消耗")
                .defineInRange("costOnStart", 20.0D, 0.0D, 10000.0D);
        staminaCostPerTick = b
                .comment("滑铲期间每 tick 消耗")
                .defineInRange("costPerTick", 0.4D, 0.0D, 100.0D);
        staminaRegenPerTick = b
                .comment("每 tick 恢复量")
                .defineInRange("regenPerTick", 0.6D, 0.0D, 100.0D);
        staminaRegenDelayTicks = b
                .comment("停止消耗后多久开始恢复（tick）")
                .defineInRange("regenDelayTicks", 20, 0, 400);

        b.comment("档位阈值（耐力比例 0~1）").push("thresholds");
        staminaLevel1Threshold = b
                .comment("一档阈值")
                .defineInRange("level1", 0.6D, 0.0D, 1.0D);
        staminaLevel2Threshold = b
                .comment("二档阈值")
                .defineInRange("level2", 0.3D, 0.0D, 1.0D);
        b.pop();

        b.comment("各档速度（方块/tick）").push("speeds");
        staminaLevel2Speed = b
                .comment("二档速度")
                .defineInRange("level2Speed", 0.45D, 0.0D, 5.0D);
        staminaLevel3Speed = b
                .comment("三档速度")
                .defineInRange("level3Speed", 0.3D, 0.0D, 5.0D);
        b.pop();

        b.comment("耐力显示").push("display");
        staminaDisplayEnabled = b.comment("是否显示耐力").define("enabled", true);
        staminaDisplayMode = b
                .comment("显示格式：value / valueMax / percent / level")
                .define("mode", "value");
        staminaDisplayAlwaysShow = b
                .comment("true 常驻，false 变化后短暂显示再淡出")
                .define("alwaysShow", false);
        staminaDisplayHoldTicks = b
                .comment("保持不透明的时间（tick）")
                .defineInRange("holdTicks", 20, 0, 400);
        staminaDisplayFadeTicks = b
                .comment("淡出时间（tick）")
                .defineInRange("fadeTicks", 20, 0, 400);
        staminaDisplayPosition = b
                .comment("位置：center / bottom / topLeft / topRight")
                .define("position", "bottom");
        staminaDisplayScale = b
                .comment("字号缩放")
                .defineInRange("scale", 1.5D, 0.5D, 10.0D);
        staminaDisplayColor = b
                .comment("颜色（#RRGGBB）")
                .define("color", "#FFFFFF");
        staminaDisplayShadow = b
                .comment("是否绘制文字阴影")
                .define("shadow", true);
        b.pop(); // display
        b.pop(); // stamina

        // ---------- hunger ----------
        hungerEnabled = b
                .comment("是否启用滑铲饱食度消耗")
                .define("hungerEnabled", false);
        hungerPerTick = b
                .comment("滑铲期间每 tick 增加的疲劳度")
                .defineInRange("hungerPerTick", 0.1D, 0.0D, 10.0D);

        // ---------- hitbox ----------
        b.comment("滑铲碰撞箱与眼高").push("hitbox");
        hitboxWidth = b
                .comment("宽度")
                .defineInRange("hitboxWidth", 0.6D, 0.0D, 5.0D);
        hitboxHeight = b
                .comment("高度")
                .defineInRange("hitboxHeight", 0.6D, 0.0D, 5.0D);
        eyeHeight = b
                .comment("眼睛高度")
                .defineInRange("eyeHeight", 0.5D, 0.0D, 5.0D);
        b.pop(); // hitbox

        b.pop(); // slide

        // ============================================================
        // crawl（与 slide 同级）
        // ============================================================
        b.comment("趴下").push("crawl");
        crawlEnabled = b
                .comment("趴下总开关")
                .define("enabled", true);
        b.pop();
    }
}