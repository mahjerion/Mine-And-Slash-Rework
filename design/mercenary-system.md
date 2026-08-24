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
* The top left has the class icon and selection arrows; the top middle has the entity model render, 2 slots for hands, and 4 slots for armor, and then a toggle button for Merc behavior; the top right has the core stats and a damage icon and defense icon that on hover, displays the mercs damage/defense stats (simplified vs. the players version) similar to the players; below the top portion is an experience bar; below that on the left is the skill, support, and aura slots (left of the aura slots is the spirit capacity); to the right of that is the class skill tree; at the bottom left is the Stats menu button similar to the hub in the player's hub window, but the button is on the inside of the GUI rather than on the outside - hovering it displays "Stats" and the elongated bar and clicking it brings the player to the Stats GUI screen similar to the player but instead displays the stats for the Merc.
* Switching Mercs: You can select from different mercs similar to picking your class as a player, but doing so unsummons your merc. Once you're on a new merc selected, it will spawn after 3s.

------------------------------
## 2. Entity & Visuals

* Base Texture: Temporary placeholder using the Vindicator model/texture.
* Entity Type: Must register as a custom companion entity.
* Targeting/Proc Registration: For compatibility with Mine and Slash mechanics, the Mercenary must count as a player entity regarding proc effects, stat calculations, and combat triggers.

------------------------------
## 3. Progression & Stats

* Independent Scaling: Mercenaries carry their own independent stat blocks.
* Leveling Mechanics:
* Gains experience and levels up alongside the player. They don't receive experience from give experience commands, only from kills.
   * Restriction: Only gains XP while alive and actively engaged. They don't lose XP on death. The Mercenary can never level above the player's level.
   * No Penalties: Mercenaries don't have experience penalties from being too far apart in level from the player.
* Party Consideration: for the purposes of experience and gameplay, Mercenaries are allies and if the player is in a team with another player, the buffs and heals should work between the teammate player and your mercenary. However, Mercenaries don't count as a player when considering team experience sharing. They just receive same experience as the player, modified by bonus experience.
* Stat Allocation: Automated. The Mercenary allocates its own attribute points upon leveling up based on its defined archetype/datapack profile.
* Resource Pools:
* Uses Health and Magic Shield.
   * Dead Stats: Completely ignores Mana and Energy (values are non-existent or locked to 0). Skills do not cost resources.
* Each character save data contains their own Mercenary data. So a new character starts with level 1 mercenaries.

------------------------------
## 4. Equipment System

* Allowed Equipment: Full armor sets (Helmet, Chestplate, Leggings, Boots) and Weapons.
* When equipping a 2-hander, it should reject any off-hands or weapons placed in the off-hand slot. Similarly, each slot should reject any item that's not legal.
* Disallowed Equipment: Jewelry slots (Rings, Amulets, etc.) are restricted.

------------------------------
## 5. Combat Mechanics & AI Behavior

* Ally: The Mercenary is considered an ally to the player. They are not a "summon" like Summon Zombies, but still belong to the player.
* Basic Attacks: Uses standard melee or ranged basic attacks when all equipped skills are on cooldown.
* Deaths: When the Merc dies, it comes back once out of combat and after 3s. Experience is not lost on death for Mercs.
* Combat States: Skills can only be cast while the Mercenary is actively in combat.
* Aiming Logic: Spellcasting or projectile-based skills must explicitly lock onto and aim at the current combat target.
* Combat Modes: Three toggleable AI states managed via GUI:
   1. Aggressive: Attacks any hostile target in radius; prioritizes player's target.
   2. Defensive: Only attacks targets that strike the player or the mercenary.
   3. Idle: Follows the player passively; does not engage in combat.
* Any kills from the Mercenary are attributed to the player but don't count for on-kill stats, just the kill participation (for stuff like FTB Quests and loot tables). However, Mercenaries will take into account Bonus Experience, drop rate bonuses and Magic Find from the player and combine it with their own on kill. Player's don't get to benefit from Bonus Experience, Magic Find or drop rate bonuses from Mercenary stats though.

------------------------------
## 6. Skill System (ESO Companion Style)

* Skill Pool & Unlocks: Mercenaries automatically learn specific skills as they level up and reach the required level to unlock the skill.
* Skill Caps: Every skill has a Maximum Level of 1.
* Equipped Limit: A Mercenary can know many skills but can only equip up to 4 active skills at any given time.
* Priority System: The GUI must allow players to arrange the 4 equipped skills into a custom casting priority queue (e.g., Cast Skill A first, if on cooldown cast Skill B, etc.).
* Support Gems: Every 5 Mercenary levels after the skill unlocks, it gets +1 Support Gem slot, up to a max of 3. Eg. unlock a skill at 5, at level 10, 15, and 20, it will get +1 Support Gem slot. The GUI should indicate it's locked otherwise similar to the player's Spell Screen.
* Aura Gems: Up to 2 Aura Gems can be equipped by the Merc. It starts with 25 Spirit/Aura Capacity and gains 2.5 every 10 levels. At level 100, it should have 50 base Spirit.

------------------------------
## 7. Datapack Architecture (Extensibility)
The entire system must be data-driven via Minecraft datapacks so developers/servers can easily add new types of mercenaries. The JSON structure should dictate:

* Mercenary ID, Registry Name, and Texture/Model.
* Base stats and stat growth per level.
* Skill tree/list defining which skill unlocks at what exact level.

------------------------------
## 8. Commands
Create a set of commands that lets an OP'd player modify the level and stats, and lets the player reset the mercenary completely.
