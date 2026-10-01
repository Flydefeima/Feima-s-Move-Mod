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
    public final ForgeConfigSpec.BooleanValue enabled;

    // motion
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

    // hunger
    public final ForgeConfigSpec.BooleanValue hungerEnabled;
    public final ForgeConfigSpec.DoubleValue  hungerPerTick;

    // hitbox
    public final ForgeConfigSpec.DoubleValue hitboxWidth;
    public final ForgeConfigSpec.DoubleValue hitboxHeight;
    public final ForgeConfigSpec.DoubleValue eyeHeight;

    // ============================================================
    // 构造
    // ============================================================
    private MoveConfig(ForgeConfigSpec.Builder b) {

        b.comment("Feima's Move Mod — 滑铲").push("slide");

        enabled = b.comment("滑铲总开关").define("enabled", true);

        // ============================================================
        // motion
        // ============================================================
        b.comment("运动参数").push("motion");
        startSpeed = b
                .comment("一档（满耐力档）初速度（方块/tick）")
                .defineInRange("startSpeed", 0.6D, 0.0D, 5.0D);
        decayDelay = b
                .comment("速度开始衰减的时刻，在此之前保持初速度")
                .defineInRange("decayDelay", 3, 0, 200);
        friction = b
                .comment("速度衰减系数，0.90 表示每 tick 变慢 10%")
                .defineInRange("friction", 0.9D, 0.0D, 1.0D);
        endSpeed = b
                .comment("末速度（方块/tick），速度降到 ≤ 此值则结束滑铲")
                .defineInRange("endSpeed", 0.2D, 0.0D, 5.0D);
        slideTriggerCd = b
                .comment("滑铲触发 CD（tick）：两次滑铲启动之间的最短间隔。")
                .defineInRange("slideTriggerCd", 22, 0, 200);
        b.pop(); // motion

        // ============================================================
        // steering
        // ============================================================
        b.comment("视角转动偏移量",
                 "视角自由转动，动量方向在有限范围内跟随，转向有惯性。")
                .push("steering");
        followLook = b
                .comment("是否允许视角影响滑铲方向。",
                         "true  → 视角转动带来方向修正（FPS 侧滑推荐）",
                         "false → 方向完全锁定在触发瞬间")
                .define("followLook", true);
        turnFactor = b
                .comment("视角偏移系数：目标方向跟随视角的比例。",
                         "0.0 = 完全锁定，0.5 = 明显跟随，1.0 = 完全跟随")
                .defineInRange("turnFactor", 0.5D, 0.0D, 1.0D);
        maxTurnOffset = b
                .comment("滑铲方向相对初始方向的最大偏移角度（度）。",
                         "不管视角转多少度，动量方向偏移不超过这个值。")
                .defineInRange("maxTurnOffset", 45.0D, 0.0D, 180.0D);
        turnOffsetZeroYaw = b
                .comment("视角归零阈值（度）：超过此角度时目标偏移强制归零。",
                         "0 = 禁用视角转动的动量偏移，180 = 不会触发")
                .defineInRange("turnOffsetZeroYaw", 120.0D, 0.0D, 180.0D);
        turnSpeed = b
                .comment("方向追赶视角的最大角速度（度/tick）。",
                         "0 = 无偏移，3.0 = 推荐，6.0 = 较快跟随")
                .defineInRange("turnSpeed", 3.0D, 0.0D, 30.0D);
        b.pop(); // steering

        // ============================================================
        // jump
        // ============================================================
        b.comment("滑铲跳（滑铲中按跳跃触发，会取消滑铲并向前飞跃）").push("jump");
        slideJumpForward = b
                .comment("一档时滑铲跳的水平速度（方块/tick）。",
                         "实际速度 = 此值 × 当前耐力档位比例（level2Speed/startSpeed 等）")
                .defineInRange("slideJumpForward", 1.0D, 0.0D, 5.0D);
        slideJumpUp = b
                .comment("滑铲跳时的向上速度（方块/tick）。不受耐力影响。")
                .defineInRange("slideJumpUp", 0.42D, 0.0D, 5.0D);
        slideJumpFollowLook = b
                .comment("滑铲跳方向来源。",
                         "false 使用滑铲触发瞬间的方向",
                         "true  使用当前视角方向")
                .define("slideJumpFollowLook", true);
        b.pop(); // jump

        // ============================================================
        // stamina
        // ============================================================
        b.comment("耐力条：三档初速，玩家自行调控。",
                 "",
                 "机制：",
                 "  1. 每次滑铲启动消耗 costOnStart",
                 "  2. 滑铲期间每 tick 持续消耗 costPerTick",
                 "  3. 停止消耗 regenDelayTicks 后每 tick 恢复 regenPerTick",
                 "  4. 按【启动前】的耐力比例决定这一滑的档位：",
                 "       比例 ≥ level1Threshold          → 一档 motion.startSpeed",
                 "       level2Threshold ≤ 比例 < level1 → 二档 level2Speed",
                 "       比例 < level2Threshold          → 三档 level3Speed",
                 "  5. 耐力 < costOnStart 时无法启动滑铲",
                 "",
                 "效果：连滑会依次掉档，但每一滑的初速是固定值（不是连续衰减）。",
                 "      玩家能直观感受“我现在是几档”。")
                .push("stamina");

        staminaEnabled = b
                .comment("是否启用耐力条。关闭后滑铲不受耐力限制，也不消耗耐力。")
                .define("enabled", true);
        staminaMax = b
                .comment("耐力上限。")
                .defineInRange("max", 100.0D, 1.0D, 10000.0D);
        staminaCostOnStart = b
                .comment("每次滑铲启动时一次性消耗的耐力。",
                         "同时也是启动门槛：耐力低于此值无法启动滑铲。")
                .defineInRange("costOnStart", 20.0D, 0.0D, 10000.0D);
        staminaCostPerTick = b
                .comment("滑铲期间每 tick 持续消耗的耐力。0 = 关闭持续消耗。")
                .defineInRange("costPerTick", 0.4D, 0.0D, 100.0D);
        staminaRegenPerTick = b
                .comment("每 tick 恢复的耐力（0.6 → 12/秒）。")
                .defineInRange("regenPerTick", 0.6D, 0.0D, 100.0D);
        staminaRegenDelayTicks = b
                .comment("耐力停止消耗后多久开始恢复（tick）。")
                .defineInRange("regenDelayTicks", 20, 0, 400);

        // ---- thresholds ----
        b.comment("档位阈值（用耐力比例表示，0~1）。",
                 "要求 level1 > level2，否则请自行调整。")
                .push("thresholds");
        staminaLevel1Threshold = b
                .comment("一档阈值：耐力比例 ≥ 此值 → 用 motion.startSpeed。",
                         "0.6 = 耐力 60% 以上为一档")
                .defineInRange("level1", 0.6D, 0.0D, 1.0D);
        staminaLevel2Threshold = b
                .comment("二档阈值：耐力比例 ≥ 此值且 < level1 → 用 level2Speed。",
                         "0.3 = 耐力 30%~60% 为二档；低于 30% 为三档")
                .defineInRange("level2", 0.3D, 0.0D, 1.0D);
        b.pop(); // thresholds

        // ---- speeds ----
        b.comment("各档速度（方块/tick）。一档速度复用 motion.startSpeed。").push("speeds");
        staminaLevel2Speed = b
                .comment("二档初速。",
                         "对应原 combo 机制的 comboSpeed2。")
                .defineInRange("level2Speed", 0.45D, 0.0D, 5.0D);
        staminaLevel3Speed = b
                .comment("三档初速。",
                         "对应原 combo 机制的 comboSpeed3。")
                .defineInRange("level3Speed", 0.3D, 0.0D, 5.0D);
        b.pop(); // speeds

        // ---- display ----
        b.comment("耐力数字显示（临时方案，等有 HUD 美术资源后可替换）。",
                 "用类似 title 的大字号数字表示当前耐力。")
                .push("display");
        staminaDisplayEnabled = b
                .comment("是否显示耐力数字。")
                .define("enabled", true);
        staminaDisplayMode = b
                .comment("显示格式：",
                         "  value    当前值（如 72）",
                         "  valueMax 当前值/上限（如 72/100）",
                         "  percent  百分比（如 72%）",
                         "  level    档位罗马数字（I / II / III）")
                .define("mode", "value");
        staminaDisplayAlwaysShow = b
                .comment("true  = 耐力未满时常驻显示",
                         "false = 只在耐力变化时短暂显示，然后淡出")
                .define("alwaysShow", false);
        staminaDisplayHoldTicks = b
                .comment("耐力变化后保持全不透明的时间（tick）。仅在 alwaysShow=false 时生效。")
                .defineInRange("holdTicks", 20, 0, 400);
        staminaDisplayFadeTicks = b
                .comment("保持时间结束后淡出所用的时间（tick）。仅在 alwaysShow=false 时生效。")
                .defineInRange("fadeTicks", 20, 0, 400);
        staminaDisplayPosition = b
                .comment("显示位置：",
                         "  center    屏幕水平居中、偏上（默认，title 风格）",
                         "  bottom    水平居中、hotbar 上方",
                         "  topLeft   左上角",
                         "  topRight  右上角")
                .define("position", "center");
        staminaDisplayScale = b
                .comment("字号缩放。1.0 = 原版字号，3.0 = title 大小。")
                .defineInRange("scale", 3.0D, 0.5D, 10.0D);
        staminaDisplayColor = b
                .comment("文字颜色，十六进制 RGB，例如 #FFFFFF、#66CCFF。")
                .define("color", "#FFFFFF");
        staminaDisplayShadow = b
                .comment("是否绘制文字阴影。")
                .define("shadow", true);
        b.pop(); // display

        b.pop(); // stamina

        // ============================================================
        // hunger
        // ============================================================
        b.comment("饱食度消耗：让滑铲与生存模式产生联系。",
                 "通过原版 causeFoodExhaustion 走原版饥饿机制。")
                .push("hunger");
        hungerEnabled = b
                .comment("是否启用滑铲饱食度消耗。")
                .define("enabled", false);
        hungerPerTick = b
                .comment("滑铲期间每 tick 增加的疲劳度。",
                         "原版疾跑约为 0.1/tick，滑铲建议 0.03~0.08。",
                         "累积到 4.0 会扣 1 点饱食度。")
                .defineInRange("perTick", 0.05D, 0.0D, 10.0D);
        b.pop(); // hunger

        // ============================================================
        // hitbox
        // ============================================================
        b.comment("碰撞箱与眼高").push("hitbox");
        hitboxWidth = b
                .comment("滑铲时碰撞箱宽度")
                .defineInRange("hitboxWidth", 0.6D, 0.0D, 5.0D);
        hitboxHeight = b
                .comment("滑铲时碰撞箱高度")
                .defineInRange("hitboxHeight", 0.6D, 0.0D, 5.0D);
        eyeHeight = b
                .comment("滑铲时眼睛高度")
                .defineInRange("eyeHeight", 0.55D, 0.0D, 5.0D);
        b.pop(); // hitbox

        b.pop(); // slide
    }
}