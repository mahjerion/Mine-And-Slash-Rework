## Feature Specification: Mercenaries System for "Mine and Slash"## Objective
Implement a Mercenary (Companion) System for the Mine and Slash ARPG Minecraft mod. The system draws inspiration from ARPG companions (like Diablo 2/3) and Elder Scrolls Online (ESO) companions, allowing players to hire, equip, and level up AI combat partners.
------------------------------
## 1. UI & Accessibility

* Access Point: Accessible via a dedicated button or tab on the left-side GUI of the main Mine and Slash menu.
* Interface Requirements:
* Displays current Mercenary stats, level, and XP bar.
   * Contains equipment slots.
   * Contains a skill management panel (slotting and priority configuration).
   * Contains combat mode toggles.

------------------------------
## 2. Entity & Visuals

* Base Texture: Temporary placeholder using the Vindicator model/texture.
* Entity Type: Must register as a custom companion entity.
* Targeting/Proc Registration: For compatibility with Mine and Slash mechanics, the Mercenary must count as a player entity regarding proc effects, stat calculations, and combat triggers.

------------------------------
## 3. Progression & Stats

* Independent Scaling: Mercenaries carry their own independent stat blocks.
* Leveling Mechanics:
* Gains experience and levels up alongside the player.
   * Restriction: Only gains XP while alive and actively engaged.
* Stat Allocation: Automated. The Mercenary allocates its own attribute points upon leveling up based on its defined archetype/datapack profile.
* Resource Pools:
* Uses Health and Magic Shield.
   * Dead Stats: Completely ignores Mana and Energy (values are non-existent or locked to 0). Skills do not cost resources.

------------------------------
## 4. Equipment System

* Allowed Equipment: Full armor sets (Helmet, Chestplate, Leggings, Boots) and Weapons.
* Disallowed Equipment: Jewelry slots (Rings, Amulets, etc.) are restricted.

------------------------------
## 5. Combat Mechanics & AI Behavior

* Basic Attacks: Uses standard melee or ranged basic attacks when all equipped skills are on cooldown.
* Combat States: Skills can only be cast while the Mercenary is actively in combat.
* Aiming Logic: Spellcasting or projectile-based skills must explicitly lock onto and aim at the current combat target.
* Combat Modes: Three toggleable AI states managed via GUI:
1. Aggressive: Attacks any hostile target in radius; prioritizes player's target.
   2. Defensive: Only attacks targets that strike the player or the mercenary.
   3. Idle: Follows the player passively; does not engage in combat.

------------------------------
## 6. Skill System (ESO Companion Style)

* Skill Pool & Unlocks: Mercenaries automatically learn specific skills as they level up.
* Skill Caps: Every skill has a Maximum Level of 1.
* Equipped Limit: A Mercenary can know many skills but can only equip up to 4 active skills at any given time.
* Priority System: The GUI must allow players to arrange the 4 equipped skills into a custom casting priority queue (e.g., Cast Skill A first, if on cooldown cast Skill B, etc.).

------------------------------
## 7. Datapack Architecture (Extensibility)
The entire system must be data-driven via Minecraft datapacks so developers/servers can easily add new types of mercenaries. The JSON structure should dictate:

* Mercenary ID, Registry Name, and Texture/Model.
* Base stats and stat growth per level.
* Skill tree/list defining which skill unlocks at what exact level.

------------------------------

