# Feima's Move Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-DB6D34.svg)](https://files.minecraftforge.net/net/minecraftforge/forge/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#license)

**Slide, crawl, and move like an FPS character — in Minecraft.**

**像 FPS 角色一样滑铲、趴下、战斗 —— 在 Minecraft 里。**

- Slide, crawl, three-tier stamina, inertia steering, slide-jump.
- 滑铲、趴下、三档耐力、惯性转向、滑铲跳。


---

## Features · 特性

### 🛝 Slide · 滑铲

Press **`C`** to slide. Requires being on the ground, holding forward, and having enough space ahead.

按 **`C`** 触发。需要处于地面、按住前进键、前方有足够空间。

- Sprinting is blocked during a slide, and the slide cancels sprint on start.
- Hitting a block face-on ends the slide instantly — no wall-hugging.

- 滑铲期间禁止疾跑，启动时自动退出疾跑。
- 正面撞墙立即结束，不会尴尬地贴墙打滑。

### 🦘 Slide-Jump · 滑铲跳

Press **`Space`** while sliding to launch forward. Horizontal speed scales with your current stamina tier. Direction follows your current look by default.

滑铲中按 **`Space`** 向前飞跃。水平速度随当前耐力档位缩放，方向默认跟随当前视角。

### 🎯 Inertia Steering · 惯性转向

Momentum follows your aim, but with weight — a limited turn angle and angular speed, not an instant snap.

动量跟随视角，但有分量感 —— 最大偏转角与角速度限制，不会瞬间掉头。

### ⚡ Stamina I / II / III · 三档耐力

| Stamina | Tier | Default Speed |
|---|---:|---:|
| `>= 60%` | I | `0.60 block/tick` |
| `30% ~ <60%` | II | `0.45 block/tick` |
| `< 30%` | III | `0.30 block/tick` |

Each slide costs stamina on start, drains while sliding, and regenerates after a short delay. Chain slides naturally step down through the tiers.

每次滑铲启动扣一次耐力，滑铲期间持续消耗，停止后延迟回复。连续滑铲会自然依次降档。

### 🧍 Crawl · 趴下

Toggle with **`Z`**. Uses vanilla `Pose.SWIMMING` — hitbox and eye height handled by the game. Mutually exclusive with sliding.

按 **`Z`** 切换。使用原版 `Pose.SWIMMING`，碰撞箱与眼高交给游戏本体处理，与滑铲互斥。

### 🌐 Multiplayer Sync · 多人同步

Optimistic client prediction with server authority. Smooth input, anti-cheat intact, and other players see your slides correctly.

客户端乐观预测 + 服务端权威。手感顺滑、防作弊、别人也能看到你滑。

---

## Controls · 按键

| Key | Action |
|:---:|:---|
| `C` | Slide / 滑铲 |
| `Z` | Crawl / 趴下 |
| `Space` | Slide-Jump (while sliding) / 滑铲跳（滑铲中） |

All keys are rebindable in **Options → Controls**.
全部可在 **选项 → 控制** 中改键。

---

## Requirements · 需求

- Minecraft **1.20.1**
- Minecraft Forge **47.x**
- Java **17**
- [Player Animator](https://github.com/KosmX/minecraftPlayerAnimator) — **required / 必需**

---

## Configuration · 配置

Tune it your way in `config/feimamovemod-common.toml`.

在 `config/feimamovemod-common.toml` 中按你的手感调整。

### Slide

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.enabled` | Boolean | `true` | `true` / `false` | Master switch for sliding. |
| `slide.requireSprint` | Boolean | `false` | `true` / `false` | If true, the player must be sprinting to trigger a slide. |
| `slide.allowWhenEmpty` | Boolean | `true` | `true` / `false` | If true, sliding is allowed at 0 stamina and speed drops to tier 3 (slowest). If false, insufficient stamina prevents sliding. |
| `slide.startSpeed` | Double | `0.6` | `0.0`–`5.0` | Tier 1 initial speed (blocks/tick). |
| `slide.decayDelay` | Integer | `3` | `0`–`200` | Ticks to hold speed before decay starts. |
| `slide.friction` | Double | `0.9` | `0.0`–`1.0` | Speed decay factor per tick. |
| `slide.endSpeed` | Double | `0.2` | `0.0`–`5.0` | End speed. Slide ends when speed drops to this value or below. |
| `slide.slideTriggerCd` | Integer | `22` | `0`–`200` | Minimum cooldown between slide starts (ticks). |

### Slide - Steering

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.steering.followLook` | Boolean | `true` | `true` / `false` | Whether look direction can affect slide direction. |
| `slide.steering.turnFactor` | Double | `0.5` | `0.0`–`1.0` | Ratio of direction following look direction. |
| `slide.steering.maxTurnOffset` | Double | `45.0` | `0.0`–`180.0` | Maximum offset angle relative to initial direction (degrees). |
| `slide.steering.turnOffsetZeroYaw` | Double | `120.0` | `0.0`–`180.0` | If look offset exceeds this angle, target direction is zeroed (degrees). |
| `slide.steering.turnSpeed` | Double | `3.0` | `0.0`–`30.0` | Maximum angular speed for direction to catch up to look direction (degrees/tick). |

### Slide - Jump

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.jump.slideJumpForward` | Double | `1.0` | `0.0`–`5.0` | Horizontal speed (blocks/tick). |
| `slide.jump.slideJumpUp` | Double | `0.42` | `0.0`–`5.0` | Upward speed (blocks/tick). |
| `slide.jump.slideJumpFollowLook` | Boolean | `true` | `true` / `false` | If true, use current look direction. If false, use initial slide direction. |

### Slide - Stamina

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.stamina.enabled` | Boolean | `true` | `true` / `false` | Enable stamina. |
| `slide.stamina.max` | Double | `100.0` | `1.0`–`10000.0` | Maximum stamina. |
| `slide.stamina.costOnStart` | Double | `20.0` | `0.0`–`10000.0` | One-time stamina cost on slide start. |
| `slide.stamina.costPerTick` | Double | `0.4` | `0.0`–`100.0` | Stamina cost per tick while sliding. |
| `slide.stamina.regenPerTick` | Double | `0.6` | `0.0`–`100.0` | Stamina regenerated per tick. |
| `slide.stamina.regenDelayTicks` | Integer | `20` | `0`–`400` | Delay after stamina use stops before regeneration begins (ticks). |

### Slide - Stamina Thresholds

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.stamina.thresholds.level1` | Double | `0.6` | `0.0`–`1.0` | Tier 1 threshold (stamina ratio 0–1). |
| `slide.stamina.thresholds.level2` | Double | `0.3` | `0.0`–`1.0` | Tier 2 threshold (stamina ratio 0–1). |

### Slide - Stamina Speeds

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.stamina.speeds.level2Speed` | Double | `0.45` | `0.0`–`5.0` | Tier 2 speed (blocks/tick). |
| `slide.stamina.speeds.level3Speed` | Double | `0.3` | `0.0`–`5.0` | Tier 3 speed (blocks/tick). |

### Slide - Stamina Display

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.stamina.display.enabled` | Boolean | `true` | `true` / `false` | Enable stamina display. |
| `slide.stamina.display.mode` | String | `"value"` | `value`, `valueMax`, `percent`, `level` | Display format. |
| `slide.stamina.display.alwaysShow` | Boolean | `false` | `true` / `false` | If true, always visible. If false, shown briefly after changes then fades out. |
| `slide.stamina.display.holdTicks` | Integer | `20` | `0`–`400` | Time to remain opaque (ticks). |
| `slide.stamina.display.fadeTicks` | Integer | `20` | `0`–`400` | Fade-out time (ticks). |
| `slide.stamina.display.position` | String | `"bottom"` | `center`, `bottom`, `topLeft`, `topRight` | Display position. |
| `slide.stamina.display.scale` | Double | `1.5` | `0.5`–`10.0` | Text scale. |
| `slide.stamina.display.color` | String | `"#FFFFFF"` | `#RRGGBB` | Text color. |
| `slide.stamina.display.shadow` | Boolean | `true` | `true` / `false` | Draw text shadow. |

### Slide - Hunger

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.hungerEnabled` | Boolean | `false` | `true` / `false` | Enable hunger/exhaustion consumption while sliding. |
| `slide.hungerPerTick` | Double | `0.1` | `0.0`–`10.0` | Exhaustion added per tick while sliding. |

### Slide - Hitbox

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `slide.hitbox.hitboxWidth` | Double | `0.6` | `0.0`–`5.0` | Slide hitbox width. |
| `slide.hitbox.hitboxHeight` | Double | `0.6` | `0.0`–`5.0` | Slide hitbox height. |
| `slide.hitbox.eyeHeight` | Double | `0.5` | `0.0`–`5.0` | Eye height while sliding. |

### Crawl

| Option | Type | Default | Range / Values | Description |
| --- | --- | --- | --- | --- |
| `crawl.enabled` | Boolean | `true` | `true` / `false` | Master switch for crawling. |

---

- Author: Feima
- License: MIT