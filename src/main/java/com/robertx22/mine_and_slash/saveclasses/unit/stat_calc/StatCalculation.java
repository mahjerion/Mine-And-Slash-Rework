package com.robertx22.mine_and_slash.saveclasses.unit.stat_calc;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryInventories;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.MercenaryStatUtils;
import net.minecraft.world.item.ItemStack;
import com.robertx22.mine_and_slash.database.data.item_set.EquippedSets;
import com.robertx22.mine_and_slash.database.data.item_set.SetBonus;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.AddPerPercentOfOther;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.AttributeStat;
import com.robertx22.mine_and_slash.database.data.stats.types.core_stats.base.ICoreStat;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatCalcInfoData;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.saveclasses.unit.GearData;
import com.robertx22.mine_and_slash.saveclasses.unit.InCalcStatContainer;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.AddToAfterCalcEnd;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.Cached;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.CommonStatUtils;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.MobStatUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatCalculation {

    public static List<StatContext> getStatsWithoutSuppGems(LivingEntity entity, EntityData data) {
        List<StatContext> statContexts = new ArrayList<>();

        statContexts = collectStatsWithCtx(entity, data, data.equipmentCache.getGear());

        statContexts.removeIf(x -> x.stats.isEmpty());

        // oh this is for allowing player to see stat calc info on client
        if (entity instanceof Player p) {
            var pd = Load.player(p);
            pd.ctxs = new StatCalcInfoData();
            for (StatContext ctx : statContexts) {
                if (ctx instanceof SimpleStatCtx s) {
                    pd.ctxs.list.add(s);
                }
            }
        }

        return statContexts;
    }

    // the List<StatContext> is modified so i cant reuse it until the code is redone and fixed
    // todo trying to rewrite calc code..
    public static void calc(Unit unit, List<StatContext> statsWithoutSuppGems, LivingEntity entity, Spell spell, int skillGem) {

        if (entity.level().isClientSide) {
            return;
        }

        EntityData data = Load.Unit(entity);
        unit.clearStats();

        List<StatContext> gemstats = new ArrayList<>();

        if (entity instanceof Player p) {
            PlayerData playerData = Load.player(p);
            gemstats.addAll(collectGemStats(p, data, playerData, skillGem));
            gemstats.addAll(collectSpellStats(p, data, playerData, spell));
        } else if (entity instanceof MercenaryEntity merc) {
            gemstats.addAll(collectMercGemStats(merc, data, skillGem));
            gemstats.addAll(collectSpellStats(merc, data, spell));
        }

        InCalcStatContainer statCalc = new InCalcStatContainer();

        var allstats = new ArrayList<StatContext>();

        allstats.addAll(gemstats);
        allstats.addAll(statsWithoutSuppGems);
        allstats.add(CtxStats.addStatCtxModifierStats(allstats));

        var sc = new CtxStats(allstats);

        sc.applyToInCalc(statCalc);


        InCalc incalc = new InCalc(unit);
        incalc.addVanillaHpToStats(entity, statCalc);
        incalc.modify(data, statCalc);

        unit.setStats(statCalc.calculate());

        // apply stats that add to others

        var stats = new HashMap<String, StatData>(unit.getStats().stats);

        var copiedStats = unit.getStats().clone();

        // we add calculated corestats to incalc so %intellect works
        for (Map.Entry<String, StatData> en : stats.entrySet()) {
            if (en.getValue().GetStat() instanceof ICoreStat aff) {
                aff.affectStats(data, en.getValue(), statCalc);
            }
        }
        unit.setStats(statCalc.calculate());
        copiedStats = unit.getStats().clone();


        var addToAfterCalcStats = stats.entrySet().stream()
                .filter(en -> en.getValue().GetStat() instanceof AddToAfterCalcEnd)
                .sorted((a, b) -> {
                    var statA = a.getValue().GetStat();
                    var statB = b.getValue().GetStat();

                    int priorityA = (statA instanceof AddPerPercentOfOther addA) ? addA.priority : Integer.MAX_VALUE;
                    int priorityB = (statB instanceof AddPerPercentOfOther addB) ? addB.priority : Integer.MAX_VALUE;

                    return Integer.compare(priorityA, priorityB);
                })
                .toList();

        int lastPriority = Integer.MIN_VALUE;

        for (var en : addToAfterCalcStats) {
            AddToAfterCalcEnd aff = (AddToAfterCalcEnd) en.getValue().GetStat();

            // Get current priority
            int currentPriority = (aff instanceof AddPerPercentOfOther addStat) ?
                    addStat.priority : Integer.MAX_VALUE;

            // Only clone if priority has increased (new priority tier)
            if (currentPriority > lastPriority && lastPriority != Integer.MIN_VALUE) {
                copiedStats = unit.getStats().clone();
            }

            aff.affectStats(copiedStats, unit.getStats(), en.getValue());
            lastPriority = currentPriority;
        }

        for (StatData stat : unit.getStats().stats.values()) {
            stat.softCapStat(unit);
        }

        Cached.VANILLA_STAT_UIDS_TO_CLEAR_EVERY_STAT_CALC.forEach(x -> {
            AttributeInstance in = entity.getAttribute(x.left);
            if (in != null && in.getModifier(x.right) != null) {
                in.removeModifier(x.right);
            }
        });

        unit.getStats().stats.values()
                .forEach(x -> {
                    if (x.GetStat() instanceof AttributeStat) {
                        AttributeStat stat = (AttributeStat) x.GetStat();
                        stat.addToEntity(entity, x);
                    }
                });


    }


    private static List<StatContext> collectGemStats(Player p, EntityData data, PlayerData playerData, int skillGem) {
        List<StatContext> statContexts = new ArrayList<>();

        if (skillGem > -1 && skillGem <= GemInventoryHelper.MAX_SKILL_GEMS) {
            var gem = playerData.getSkillGemInventory().getHotbarGem(skillGem);
            // clipped to the links this skill has actually unlocked, the same way collectMercGemStats
            // below is. gems over that count are no longer thrown out of the socket, so this is the
            // only thing keeping them from applying for free.
            for (SkillGemData d : gem.getActiveSupportDatas(p)) {
                if (d.getSupport() != null) {
                    statContexts.add(new SimpleStatCtx(StatContext.StatCtxType.SUPPORT_GEM, d.getSupport().GetAllStats(data, d)));
                }
            }

        }
        return statContexts;
    }

    private static List<StatContext> collectSpellStats(Player p, EntityData data, PlayerData playerData, Spell spell) {
        return collectSpellStats(p, data, spell);
    }

    // the innate stats a skill gem grants while socketed. entity typed rather than player typed so a
    // mercenary's equipped skills grant theirs too - Spell.getStats needs nothing player specific.
    private static List<StatContext> collectSpellStats(LivingEntity en, EntityData data, Spell spell) {
        List<StatContext> statContexts = new ArrayList<>();

        if (spell != null) {
            var stats = spell.getStats(en);
            if (!stats.isEmpty()) {
                statContexts.add(new SimpleStatCtx(StatContext.StatCtxType.INNATE_SPELL, stats));
            }

            if (spell.config.usesSupportGemsFromAnotherSpell()) {
                var other = spell.config.getSpellUsedForSuppGems();
                var stats2 = other.getStats(en);

                if (!stats2.isEmpty()) {
                    statContexts.add(new SimpleStatCtx(StatContext.StatCtxType.INNATE_SPELL, stats2));
                }
            }

        }
        return statContexts;
    }

    /**
     * Support gems socketed under one of the mercenary's equipped skills. The mercenary equivalent of
     * {@link #collectGemStats}: same SUPPORT_GEM context, read out of the mercenary's own inventory,
     * and clipped to the slots its level has actually unlocked so a gem left in a slot that later
     * locked (a level reset, a datapack change) stops contributing instead of silently counting.
     */
    private static List<StatContext> collectMercGemStats(MercenaryEntity merc, EntityData data, int skillSlot) {
        List<StatContext> statContexts = new ArrayList<>();

        if (skillSlot < 0 || skillSlot >= MercenaryClass.EQUIPPED_SKILLS) {
            return statContexts;
        }
        MercenaryData mercData = merc.getMercData();
        if (mercData == null) {
            return statContexts;
        }

        int unlocked = mercData.getSupportSlots(skillSlot);
        MyInventory inv = mercData.getSupports();

        for (int i = 0; i < unlocked; i++) {
            ItemStack stack = inv.getItem(MercenaryInventories.supportIndex(skillSlot, i));
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            if (gem != null && gem.getSupport() != null) {
                statContexts.add(new SimpleStatCtx(StatContext.StatCtxType.SUPPORT_GEM, gem.getSupport().GetAllStats(data, gem)));
            }
        }
        return statContexts;
    }


    private static List<StatContext> collectStatsWithCtx(LivingEntity entity, EntityData data, List<GearData> gears) {
        List<StatContext> statContexts = new ArrayList<>();


        statContexts.addAll(CommonStatUtils.addExactCustomStats(entity));

        statContexts.add(data.equipmentCache.getStatusEffectStats());

        statContexts.addAll(addGearStats(gears));
        statContexts.addAll(addItemSetStats(gears));
        statContexts.addAll(CommonStatUtils.addMapAffixStats(entity));
        statContexts.addAll(CommonStatUtils.addBaseStats(entity));


        if (entity instanceof Player p) {
            statContexts.addAll(Load.player(p).cachedStats.statContexts);
            statContexts.add(Load.player(p).cachedStats.getStatCompatStats());
            if (Load.player(p).cachedStats.enchantCompat != null) {
                statContexts.add(Load.player(p).cachedStats.enchantCompat);
            }
            var omen = Load.player(p).cachedStats.omenStats;
            if (omen != null) {
                statContexts.add(omen);
            }
        } else if (entity instanceof MercenaryEntity merc) {
            // deliberately none of the mob paths below. a mercenary is a companion, not a monster -
            // mob base stats, affixes, dimension multipliers and map tier scaling would all make its
            // power a function of where it happens to be standing instead of its own gear and level.
            statContexts.addAll(MercenaryStatUtils.getStats(merc));
        } else {
            statContexts.addAll(MobStatUtils.getMobBaseStats(data, entity));

            if (data.isSummon()) {
                statContexts.addAll(MobStatUtils.addSummonStats((TamableAnimal) entity));
            } else {
                statContexts.addAll(MobStatUtils.getAffixStats(entity));
                statContexts.addAll(MobStatUtils.getWorldMultiplierStats(entity));
                statContexts.addAll(MobStatUtils.addMapTierStats(entity));
                statContexts.addAll(MobStatUtils.getMobConfigStats(entity, data));
            }
        }

        return statContexts;
    }

    // diablo style set bonuses. this can't live in GearData like the other gear stats do, because a
    // set bonus depends on the whole equipped combination, not on one item.
    static List<StatContext> addItemSetStats(List<GearData> gears) {
        List<StatContext> ctxs = new ArrayList<>();

        for (EquippedSets equipped : EquippedSets.of(gears)) {
            List<ExactStatData> stats = new ArrayList<>();
            for (SetBonus bonus : equipped.set.getSortedBonuses()) {
                if (equipped.isActive(bonus)) {
                    stats.addAll(bonus.getStats(equipped.avgLevel));
                }
            }
            if (!stats.isEmpty()) {
                ctxs.add(new SimpleStatCtx(StatContext.StatCtxType.ITEM_SET, stats));
            }
        }
        return ctxs;
    }

    static List<StatContext> addGearStats(List<GearData> gears) {
        List<StatContext> ctxs = new ArrayList<>();
        gears.forEach(x -> {
            ctxs.addAll(x.cachedStats);
        });
        return ctxs;
    }
}
