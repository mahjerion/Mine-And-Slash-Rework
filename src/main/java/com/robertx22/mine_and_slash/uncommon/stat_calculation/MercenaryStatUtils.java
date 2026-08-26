package com.robertx22.mine_and_slash.uncommon.stat_calculation;

import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.aura.AuraGem;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.stats.types.spirit.AuraCapacity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The mercenary equivalent of {@link com.robertx22.mine_and_slash.event_hooks.my_events.CachedPlayerStats}:
 * everything that is neither gear nor base stats, and that a plain mob would never get.
 * <p>
 * Gear needs nothing here - {@code CachedEntityStats.recalcGears/recalcWeapon} already walk a
 * non player's equipment slots, so mirroring the mercenary's gear inventory onto the entity is enough
 * (see {@code MercenaryManager.applyGear}).
 */
public class MercenaryStatUtils {

    public static List<StatContext> getStats(MercenaryEntity merc) {
        List<StatContext> list = new ArrayList<>();

        MercenaryData data = merc.getMercData();
        if (data == null) {
            return list;
        }
        MercenaryClass mc = data.getMercClass();
        if (mc == null) {
            return list;
        }

        list.addAll(getCoreStats(mc, data));
        list.addAll(getAuraStats(merc, data));
        list.add(getSpiritStat(mc, data));

        return list;
    }

    /**
     * The design's "automated allocation": the class profile decides how strength, dexterity and
     * intelligence grow, and the mercenary spends nothing by hand. Emitted the same way
     * {@code StatPointsData.getStatAndContext} emits a player's spent points, so the two land in the
     * calculation identically.
     */
    private static List<StatContext> getCoreStats(MercenaryClass mc, MercenaryData data) {
        List<ExactStatData> stats = new ArrayList<>();

        // the class profile decides the set, but an op override can replace any single value
        HashMap<String, Float> overrides = data.getStatOverrides();

        Set<String> ids = new LinkedHashSet<>(mc.getAllCoreStatIds());
        ids.addAll(overrides.keySet());

        for (String statId : ids) {
            if (!ExileDB.Stats().isRegistered(statId)) {
                continue;
            }
            float value = overrides.containsKey(statId)
                    ? overrides.get(statId)
                    : mc.getCoreStatValue(statId, data.lvl);
            if (value == 0) {
                continue;
            }
            stats.add(ExactStatData.levelScaled(value, ExileDB.Stats().get(statId), ModType.FLAT, 1));
        }

        if (stats.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(new SimpleStatCtx(StatContext.StatCtxType.STAT_POINTS, stats));
    }

    /** mirrors GemInventoryHelper.getAuraStats over the mercenary's own 2 aura slots */
    private static List<StatContext> getAuraStats(MercenaryEntity merc, MercenaryData data) {
        List<StatContext> ctx = new ArrayList<>();

        for (ItemStack stack : getAuraStacks(data)) {
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            if (gem == null) {
                continue;
            }
            AuraGem aura = gem.getAura();
            if (aura == null) {
                continue;
            }
            ctx.add(new SimpleStatCtx(StatContext.StatCtxType.AURA, aura.GetAllStats(Load.Unit(merc), gem)));
        }
        return ctx;
    }

    /**
     * Spirit is a stat rather than a bare number so aura gems and gear that grant capacity still work
     * on a mercenary. The class profile supplies the base - 25 at level 1 rising to 50 at 100 - which
     * is deliberately far below the player's own {@link AuraCapacity} base.
     */
    private static StatContext getSpiritStat(MercenaryClass mc, MercenaryData data) {
        return new SimpleStatCtx(StatContext.StatCtxType.MISC, List.of(
                ExactStatData.levelScaled(mc.getSpirit(data.lvl), AuraCapacity.getInstance(), ModType.FLAT, 1)));
    }

    // ------------------------------------------------------------------ spirit accounting

    public static List<ItemStack> getAuraStacks(MercenaryData data) {
        List<ItemStack> list = new ArrayList<>();
        MyInventory inv = data.getAuras();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            list.add(inv.getItem(i));
        }
        return list;
    }

    public static List<SkillGemData> getAuraGems(MercenaryData data) {
        List<SkillGemData> list = new ArrayList<>();
        for (ItemStack stack : getAuraStacks(data)) {
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            if (gem != null && gem.getAura() != null) {
                list.add(gem);
            }
        }
        return list;
    }

    /** same reservation maths as GemInventoryHelper.getSpiritReserved, minus the player only bits */
    public static int getSpiritReserved(MercenaryData data) {
        int reserved = 0;
        for (SkillGemData gem : getAuraGems(data)) {
            reserved += (int) (gem.getAura().reservation * 100F);
        }
        return reserved;
    }

    public static int getTotalSpirit(MercenaryEntity merc, MercenaryData data) {
        // read the calculated stat when the mercenary is actually in the world, so gear and aura
        // granted capacity counts. fall back to the flat class value when it isn't spawned - the gui
        // has to show a number either way.
        if (merc != null) {
            float calculated = Load.Unit(merc).getUnit().getCalculatedStat(AuraCapacity.getInstance()).getValue();
            if (calculated > 0) {
                return (int) calculated;
            }
        }
        return (int) data.getTotalSpirit();
    }

    public static int getRemainingSpirit(MercenaryEntity merc, MercenaryData data) {
        return getTotalSpirit(merc, data) - getSpiritReserved(data);
    }
}
