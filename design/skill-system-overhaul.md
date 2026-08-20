## Game Design Document: Skill System Overhaul## 📋 Context & Problem Statement

* Current State: Players can cast multiple spells simultaneously by binding them to a single key.
* The Issue: This mechanical loophole bypasses intentional balance. It allows unintended combos (e.g., dual-casting high-damage spells) that trivialize builds.
* Lore/Logic Conflict: Simultaneous casting breaks thematic immersion—a caster cannot logically invoke two separate spells at the exact same moment.

## 🎯 Design Philosophy

* Active Engagement: Players must make conscious tactical decisions for every skill activation.
* Input Integrity: One key press must equal exactly one skill execution at a time.
* Target Impact: This change primarily targets the top 5% of players utilizing min-maxed keybinds. The remaining 95% of "honest" players will feel zero negative disruption to their core gameplay loop.

## 🛠️ The Solution: Global Cooldown (GCD) Implementation
Remove simultaneous multi-casting entirely while maintaining fluid gameplay through a structured Global Cooldown system.
## 🔄 System Architecture Comparison

| System Element | Current System | New System (Proposed) |
|---|---|---|
| Global Cooldown | None. | True Cast Speed acts as a baseline GCD applied to all spells after any cast. It also no longer functions as a flat increase and instead increases casting frequency. Eg. 100% Cast Speed results in 2x casts. |
| Local Cooldowns | Dictated by the Cooldown stat; acts similarly to Cast Speed in traditional aRPGs. | Used sparingly. Spells go on a local cooldown equal to the Max of (Its Own Cooldown vs. Cast Speed). |
| Channeling Speed | Governed by the "Cast Speed" stat (used primarily for channel effects). | Officially renamed to Channel Speed. |
| Stat Overlaps | Innate Cast Speed → Cooldown conversion causes unintended stat doubling. | Conversion removed. Stats are cleanly separated. |

## 📊 Stat & Skill Remapping

* Craft to Exile 2 datapack path, do NOT update server files - I will do it on my own: C:\Users\Kelvin\curseforge\minecraft\Instances\Craft to Exile 2\config\openloader\data\cte_mns\data\mmorpg
* Cooldown → Cast Speed: Existing instances of "Cooldown" on gear/skills will convert to "Cast Speed."
* Cast Speed → Channel Speed: Existing instances of "Cast Speed" will either remain or convert to "Channel Speed" depending on the skill type.
* Archetype Separation:
* Cast Speed applies exclusively to Intelligence (spell) skills.
   * Attack Speed applies to non-Intelligence skills. The old "Attack Speed to Attack Cooldown" conversion will shift to a clean Attack Cast Speed metric.
* Balance Pass: Audit all skills to ensure their new Cast Speeds feel fluid. For the majority of skills, the new Cast Speed should exactly match their current local cooldowns if they are ≤ 2 seconds.
* Notes: Skills that use charges and Brawler skills (only applicable to datapack) should have a shorter "cast speed"/post-cast cooldown, at about 75% of other skills.
* Notes: Channel skills should all share a 0.5s post-cast cooldown/cast speed. Remember that Channel Skills can use Channel Speed now.

------------------------------
## ⚠️ Caveats & Compensations## 1. The Input Problem: Built-In Macro (Skill Queue) System
To prevent a massive increase in tedious keystrokes for players transitioning away from bound keys:

* The Shared Bind Evolution: Keep the current shared keybind interface but completely change the execution behavior.
* Sequential Queueing: Pressing the shared key will no longer trigger spells simultaneously. Instead, it will queue them sequentially from left to right, automatically respecting each spell's individual Cast Speed / GCD delay.

## 2. The Scaling Problem: End-Game Monster Health Tuning
Because upper-tier monsters were unknowingly balanced against the inflated burst damage of the old simultaneous casting system, their current health pools are too high.

* Late-Game Nerf: Reduce monster health scaling in the late-game/end-game by ~30%.
* Early-Game Retention: Low-level and mid-level monsters will remain mostly unaffected to preserve the leveling experience.

------------------------------
## 🛑 Rejected Alternatives

* Alternative Considered: Apply a flat, built-in cast delay to all spells using existing mechanics to save engineering time.
* Reason for Rejection: This introduces a jarring input delay before a spell activates. It makes moment-to-moment combat feel incredibly clunky, unresponsive, and unpolished.

