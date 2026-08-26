package com.robertx22.mine_and_slash.saveclasses.mercenary;

import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_calc.StatCalculation;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Everything one mercenary class remembers for one character. Mirrors what
 * {@link com.robertx22.mine_and_slash.characters.CharacterData} does for the player: plain fields go
 * through gson, and anything holding an ItemStack is transient and written to raw nbt by PlayerData.
 */
public class MercenaryData {

    public enum CombatMode implements IAutoLocName {
        AGGRESSIVE("aggressive", "Aggressive", ChatFormatting.RED),
        DEFENSIVE("defensive", "Defensive", ChatFormatting.YELLOW),
        IDLE("idle", "Idle", ChatFormatting.GRAY);

        public final String id;
        public final String name;
        public final ChatFormatting format;

        CombatMode(String id, String name, ChatFormatting format) {
            this.id = id;
            this.name = name;
            this.format = format;
        }

        public CombatMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public AutoLocGroup locNameGroup() {
            return AutoLocGroup.Misc;
        }

        @Override
        public String locNameLangFileGUID() {
            return SlashRef.MODID + ".merc_mode." + id;
        }

        @Override
        public String locNameForLangFile() {
            return name;
        }

        @Override
        public String GUID() {
            return id;
        }
    }

    public String class_id = "";
    public int lvl = 1;
    public int exp = 0;
    public CombatMode mode = CombatMode.AGGRESSIVE;

    /** the 4 active skills, in cast priority order. holes are allowed so a slot can be left empty. */
    public List<String> equipped_skills = new ArrayList<>();

    // core stat GUID -> flat value that replaces whatever the class profile would have produced.
    // only the op command writes here; normally attribute allocation is entirely automated.
    private HashMap<String, Float> stat_overrides = new HashMap<>();

    /**
     * Always reach the overrides through here. A save written before this field existed has no key
     * for it, and while gson does keep the initializer for a class with a default constructor, a null
     * here would take out the whole stat calculation for every existing mercenary.
     */
    public HashMap<String, Float> getStatOverrides() {
        if (stat_overrides == null) {
            stat_overrides = new HashMap<>();
        }
        return stat_overrides;
    }

    // gson can't serialize an ItemStack, so these are transient and PlayerData writes them as raw
    // nbt - the same arrangement CharacterData uses for equipment/gems/auras/jewels. gson only keeps
    // a transient field's initializer because this class has a default constructor; don't rely on
    // that, always come through the getters.
    public transient MyInventory gear = MercenaryInventories.newGear();
    public transient MyInventory supports = MercenaryInventories.newSupports();
    public transient MyInventory auras = MercenaryInventories.newAuras();

    // per equipped-skill Unit, folding in that skill's support gems. same idea and same invalidation
    // point as PlayerData.spellUnits / setSpellUnitsDirty.
    private transient HashMap<String, Unit> spellUnits = new HashMap<>();

    // the contexts the last full recalc collected, kept so a per skill Unit can be built without
    // gathering gear, auras and base stats all over again. mirrors
    // CachedPlayerStats.allStatsWithoutSuppGems.
    public transient List<StatContext> allStatsWithoutSuppGems = new ArrayList<>();

    public MercenaryData() {
    }

    public MercenaryData(String classId) {
        this.class_id = classId;
    }

    public MyInventory getGear() {
        if (gear == null) {
            gear = MercenaryInventories.newGear();
        }
        return gear;
    }

    public MyInventory getSupports() {
        if (supports == null) {
            supports = MercenaryInventories.newSupports();
        }
        return supports;
    }

    public MyInventory getAuras() {
        if (auras == null) {
            auras = MercenaryInventories.newAuras();
        }
        return auras;
    }

    public MercenaryClass getMercClass() {
        if (class_id.isEmpty() || !ExileDB.Mercenaries().isRegistered(class_id)) {
            return null;
        }
        return ExileDB.Mercenaries().get(class_id);
    }

    // ------------------------------------------------------------------ skills

    /** the spell in equipped slot {@code index}, or null. never throws on a short/holed list. */
    public String getEquippedSkill(int index) {
        if (index < 0 || index >= equipped_skills.size()) {
            return null;
        }
        String id = equipped_skills.get(index);
        return id == null || id.isEmpty() ? null : id;
    }

    public Spell getEquippedSpell(int index) {
        String id = getEquippedSkill(index);
        if (id == null || !ExileDB.Spells().isRegistered(id)) {
            return null;
        }
        return ExileDB.Spells().get(id);
    }

    public void setEquippedSkill(int index, String spellId) {
        if (index < 0 || index >= MercenaryClass.EQUIPPED_SKILLS) {
            return;
        }
        while (equipped_skills.size() <= index) {
            equipped_skills.add("");
        }
        equipped_skills.set(index, spellId == null ? "" : spellId);
    }

    /** which equipped slot holds this spell, or -1 */
    public int slotOfSpell(String spellId) {
        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            if (spellId.equals(getEquippedSkill(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Moves an equipped skill one place up or down the priority queue. The queue IS the list order,
     * so this is a straight swap.
     */
    public void moveSkillPriority(int index, boolean up) {
        int other = up ? index - 1 : index + 1;
        if (index < 0 || other < 0 || index >= MercenaryClass.EQUIPPED_SKILLS || other >= MercenaryClass.EQUIPPED_SKILLS) {
            return;
        }
        String a = getEquippedSkill(index);
        String b = getEquippedSkill(other);
        setEquippedSkill(index, b);
        setEquippedSkill(other, a);
    }

    /** how many support slots equipped skill {@code index} currently has unlocked */
    public int getSupportSlots(int index) {
        MercenaryClass mc = getMercClass();
        String spell = getEquippedSkill(index);
        if (mc == null || spell == null) {
            return 0;
        }
        return mc.getSupportSlots(spell, lvl);
    }

    // ------------------------------------------------------------------ spell units

    /**
     * The Unit to score a given skill with: the mercenary's own stats plus the support gems socketed
     * under that skill. Falls back to the plain Unit for anything not equipped, which is what
     * SpellCastContext would have used anyway.
     */
    public Unit getSpellUnit(MercenaryEntity merc, Spell spell) {
        String id = spell.GUID();

        Unit cached = spellUnits.get(id);
        if (cached != null) {
            return cached;
        }

        int slot = slotOfSpell(id);
        if (slot < 0 || allStatsWithoutSuppGems == null || allStatsWithoutSuppGems.isEmpty()) {
            return Load.Unit(merc).getUnit();
        }

        Unit unit = new Unit();
        StatCalculation.calc(unit, allStatsWithoutSuppGems, merc, spell, slot);
        spellUnits.put(id, unit);
        return unit;
    }

    public void setSpellUnitsDirty() {
        spellUnits = new HashMap<>();
    }

    // ------------------------------------------------------------------ spirit

    public float getTotalSpirit() {
        MercenaryClass mc = getMercClass();
        return mc == null ? 0 : mc.getSpirit(lvl);
    }
}
