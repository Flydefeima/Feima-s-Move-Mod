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

| Option | Default | Description |
|:---|:---:|:---|
| `slide.enabled` | `true` | 滑铲总开关 / Master switch |
| `slide.requireSprint` | `false` | 必须疾跑才能滑铲 / Require sprinting |
| `slide.allowWhenEmpty` | `true` | 零耐力也能滑铲（降为三档）/ Allow sliding at zero stamina |
| `slide.startSpeed` | `0.6` | I 档初速度 / Tier-I initial speed |
| `slide.decayDelay` | `3` | 开始衰减前的保持 tick / Ticks before decay |
| `slide.friction` | `0.9` | 每 tick 衰减系数 / Per-tick decay factor |
| `slide.endSpeed` | `0.2` | 低于此值结束滑铲 / End slide threshold |
| `slide.slideTriggerCd` | `22` | 两次滑铲的最短间隔 / Cooldown |
| `slide.requireSprint` | `false` | 需疾跑触发 / Require sprint |
| `slide.hungerEnabled` | `false` | 消耗饱食度 / Hunger cost |
| `crawl.enabled` | `true` | 趴下总开关 / Crawl master switch |

Nested groups: `slide.steering` · `slide.jump` · `slide.stamina` · `slide.stamina.display` · `slide.hitbox`.

---

- Author: Feima
- License: MIT