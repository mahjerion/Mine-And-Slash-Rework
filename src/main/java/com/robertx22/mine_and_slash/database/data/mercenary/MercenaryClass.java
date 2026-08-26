package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A hireable companion archetype. Everything about a mercenary that isn't per player state lives
 * here so servers can add their own without touching code.
 * <p>
 * The skill grid deliberately mirrors {@link com.robertx22.mine_and_slash.database.data.spell_school.SpellSchool}
 * - same {@code HashMap<String, PointData>} shape, same {@code lvl_reqs} row list - because the
 * mercenary screen renders the exact same 1/5/10/15/20/25/30 level track the class trees use.
 */
public class MercenaryClass implements JsonExileRegistry<MercenaryClass>, IAutoGson<MercenaryClass>, IAutoLocName {

    public static MercenaryClass SERIALIZER = new MercenaryClass();

    public static int MAX_Y_ROWS = 7;
    public static int MAX_X_ROWS = 10;

    /** how many active skills a mercenary can have slotted at once */
    public static final int EQUIPPED_SKILLS = 4;
    /** hard cap on support gems per equipped skill */
    public static final int SUPPORTS_PER_SKILL = 3;
    /** one extra support slot per this many mercenary levels past the skill's unlock level */
    public static final int LEVELS_PER_SUPPORT_SLOT = 5;
    /** aura gem slots */
    public static final int AURA_SLOTS = 2;

    public String id = "";

    /** file name under textures/gui/mercenary/classes/ */
    public String icon = "fighter";

    /** full ResourceLocation of the entity texture, e.g. "minecraft:textures/entity/zombie/zombie.png" */
    public String texture = "minecraft:textures/entity/zombie/zombie.png";

    /** how the mercenary moves and fights: melee closes in, ranged keeps its distance and kites */
    public enum AiBehavior {
        MELEE, RANGED
    }

    public AiBehavior ai_behavior = AiBehavior.MELEE;

    /** core stat GUID -> value the mercenary starts with at level 1 */
    public HashMap<String, Float> base_stats = new HashMap<>();

    /** core stat GUID -> value gained on every level up */
    public HashMap<String, Float> stats_per_level = new HashMap<>();

    /** core stat GUID -> value gained on every 10th level */
    public HashMap<String, Float> stats_per_10_lvl = new HashMap<>();

    /** spell GUID -> position on the skill grid. the y row decides the unlock level via lvl_reqs */
    public HashMap<String, PointData> skills = new HashMap<>();

    public List<Integer> lvl_reqs = Arrays.asList(1, 5, 10, 15, 20, 25, 30);

    public float base_spirit = -75;

    public float spirit_per_10_lvl = 2.5F;

    public transient String locname = "";

    public int getLevelNeededFor(PointData point) {
        if (point == null) {
            return 1;
        }
        // a datapack can put a skill on a row the lvl_reqs list doesn't describe. clamp instead of
        // throwing - the entry is still usable, it just unlocks at the last defined row.
        int row = Math.max(0, Math.min(point.y, lvl_reqs.size() - 1));
        return lvl_reqs.get(row);
    }

    public int getUnlockLevel(String spellId) {
        return getLevelNeededFor(skills.get(spellId));
    }

    public boolean isSkillUnlocked(String spellId, int mercLevel) {
        return skills.containsKey(spellId) && mercLevel >= getUnlockLevel(spellId);
    }

    /**
     * Support slots open one at a time, every {@link #LEVELS_PER_SUPPORT_SLOT} levels after the
     * skill itself unlocks. A skill unlocked at 5 gets its slots at 10, 15 and 20.
     */
    public int getSupportSlots(String spellId, int mercLevel) {
        if (!isSkillUnlocked(spellId, mercLevel)) {
            return 0;
        }
        int past = mercLevel - getUnlockLevel(spellId);
        return Math.max(0, Math.min(past / LEVELS_PER_SUPPORT_SLOT, SUPPORTS_PER_SKILL));
    }

    public float getSpirit(int mercLevel) {
        return base_spirit + (mercLevel / 10) * spirit_per_10_lvl;
    }

    /** every core stat this class ever touches, so callers don't have to union the three maps */
    public List<String> getAllCoreStatIds() {
        List<String> list = new ArrayList<>();
        for (String s : base_stats.keySet()) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }
        for (String s : stats_per_level.keySet()) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }
        for (String s : stats_per_10_lvl.keySet()) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }
        return list;
    }

    /** base + per level + per 10 levels, the automated allocation the design asks for */
    public float getCoreStatValue(String statId, int mercLevel) {
        float val = base_stats.getOrDefault(statId, 0F);
        val += stats_per_level.getOrDefault(statId, 0F) * (mercLevel - 1);
        val += stats_per_10_lvl.getOrDefault(statId, 0F) * (mercLevel / 10);
        return val;
    }

    /** unlocked spells, lowest unlock level first, so "learned in order" is the default ordering */
    public List<Spell> getLearnedSpells(int mercLevel) {
        List<Spell> list = new ArrayList<>();
        for (Map.Entry<String, PointData> en : skills.entrySet()) {
            if (mercLevel < getLevelNeededFor(en.getValue())) {
                continue;
            }
            Spell spell = ExileDB.Spells().get(en.getKey());
            if (spell != null) {
                list.add(spell);
            }
        }
        list.sort(Comparator.comparingInt(x -> getUnlockLevel(x.GUID())));
        return list;
    }

    public ResourceLocation getIconLoc() {
        return SlashRef.guiId("mercenary/classes/" + icon);
    }

    public ResourceLocation getTextureLoc() {
        return new ResourceLocation(texture);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.MERCENARY;
    }

    @Override
    public Class<MercenaryClass> getClassForSerialization() {
        return MercenaryClass.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".mercenary." + id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }
}
