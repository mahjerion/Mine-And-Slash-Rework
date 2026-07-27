package com.robertx22.mine_and_slash.saveclasses.spells;

import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.spell_school.SpellSchool;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.events.MineAndSlashEvents;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;


public class SpellSchoolsData implements IStatCtx {

    public HashMap<String, Integer> allocated_lvls = new HashMap<>();

    // the order the player allocated their classes in. allocated_lvls and school() are both unordered,
    // so without this the gui shuffles the class shortcuts around when a second class is picked up
    public List<String> school_order = new ArrayList<>();


    public Set<String> school() {
        Set<String> list = new HashSet<>();
        for (Perk perk : getAllPerks()) {
            var sc = perk.getSpellSchool();
            if (!sc.isPresent()) {
                allocated_lvls.remove(perk.GUID());
            } else {
                list.add(sc.get().GUID());
            }
        }
        return list;
    }

    /**
     * The player's classes in the order they were first allocated. The first entry is the class the
     * gui should show on the left and open on by default.
     */
    public List<String> allocatedSchoolsInOrder() {

        var current = school();

        List<String> list = new ArrayList<>();

        for (String id : school_order) {
            if (current.contains(id) && !list.contains(id)) {
                list.add(id);
            }
        }
        // saves from before the order was tracked have none of this recorded
        for (String id : current) {
            if (!list.contains(id)) {
                list.add(id);
            }
        }

        return list;
    }

    private void rememberSchoolOrder(SpellSchool school) {
        // seed the order of classes allocated before this was tracked, so they don't shift around later
        for (String id : school()) {
            if (!school_order.contains(id)) {
                school_order.add(id);
            }
        }
        if (!school_order.contains(school.GUID())) {
            school_order.add(school.GUID());
        }
    }

    public void removeUnlearnedPerks(Player player) {
        var iterator = this.allocated_lvls.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() < 1) {
                MineAndSlashEvents.PERK_UNLEARNED_AND_REMOVED.callEvents(new MineAndSlashEvents.OnPerkUnlearnedAndRemoved(player, entry.getKey()));
                iterator.remove();
            }
        }

        // a fully unlearned class shouldn't keep holding its gui slot
        var current = school();
        school_order.removeIf(x -> !current.contains(x));
    }

    public List<Perk> getAllPerks() {
        List<Perk> all = new ArrayList<>();
        allocated_lvls.entrySet().forEach(x -> {
            if (x.getValue() > 0) {
                all.add(ExileDB.Perks().get(x.getKey()));
            }
        });
        return all;
    }

    public void reset(PointType type, Player player) {

        var schools = school();

        // just in case of updates
        for (Perk perk : getAllPerks()) {
            var sch = perk.getSpellSchool();
            if (!sch.isPresent() || sch.get() == null || !schools.contains(sch.get().GUID())) {
                this.allocated_lvls.remove(perk.GUID());
            }
        }
        for (Perk perk : getAllPerks()) {

            if (type == PointType.SPELL && perk.isSpell()) {
                MineAndSlashEvents.PERK_UNLEARNED_AND_REMOVED.callEvents(new MineAndSlashEvents.OnPerkUnlearnedAndRemoved(player, perk.GUID()));
                this.allocated_lvls.remove(perk.GUID());
            }
            if (type == PointType.PASSIVE && perk.isPassive()) {
                this.allocated_lvls.remove(perk.GUID());
            }
        }

        removeUnlearnedPerks(player);

    }

    public enum PointType {
        SPELL, PASSIVE;

        public PlayerPointsType getGeneralType() {
            return this == SPELL ? PlayerPointsType.SPELLS : PlayerPointsType.PASSIVES;
        }

        public boolean is(String perkid) {
            if (this == SPELL) {
                return ExileDB.Perks().get(perkid).isSpell();
            } else {
                return !ExileDB.Perks().get(perkid).isSpell();
            }

        }
    }


    public int getLevel(String id) {
        return allocated_lvls.getOrDefault(id, 0);
    }


    public int getSpentPoints(PointType type) {
        int total = 0;

        for (Map.Entry<String, Integer> en : allocated_lvls.entrySet()) {
            if (type.is(en.getKey())) {
                total += en.getValue();
            }
        }

        return total;
    }

    public ExplainedResult canLearn(Player en, SpellSchool school, Perk perk) {

        PointType type = perk.getPointType();

        if (type.getGeneralType().getFreePoints(en) < 1) {
            return ExplainedResult.failure(Chats.NOT_ENOUGH_POINTS.locName().withStyle(ChatFormatting.RED));
        }
        if (!school.isLevelEnoughFor(en, perk)) {
            return ExplainedResult.failure(ExplainedResultUtil.createErrorAndReason(Chats.LEARN_ERROR, Chats.TOO_LOW_LEVEL));
        }
        if (!school.isLevelEnoughForSpellLevelUp(en, perk, this.getLevel(perk.GUID()))) {
            return ExplainedResult.failure(Chats.TOO_LOW_LEVEL_TO_UPGRADE_SPELL.locName().withStyle(ChatFormatting.RED));
        }
        if (this.school().size() > 1 && !this.school().contains(school.GUID())) {
            return ExplainedResult.failure(Chats.MAX_2_CLASSES.locName().withStyle(ChatFormatting.RED));
        }
        if (allocated_lvls.getOrDefault(perk.GUID(), 0) >= perk.getMaxLevel()) {
            return ExplainedResult.failure(Chats.PERK_MAXED.locName().withStyle(ChatFormatting.RED));
        }

        return ExplainedResult.success();
    }

    public boolean canUnlearn(Player en, SpellSchool school, Perk perk) {
        if (getLevel(perk.id) < 1) {
            return false;
        }
        if (!perk.getPointType().getGeneralType().hasResetPoints(en)) {
            return false;
        }
        return true;
    }


    public void learn(Perk perk, SpellSchool school) {
        rememberSchoolOrder(school);
        int current = allocated_lvls.getOrDefault(perk.GUID(), 0);
        allocated_lvls.put(perk.GUID(), current + 1);
    }

    public void unlearn(Player player, Perk perk, SpellSchool school) {
        int current = allocated_lvls.getOrDefault(perk.GUID(), 0);
        if (current > 0) {
            perk.getPointType().getGeneralType().reduceResetPoints(player, 1);
            allocated_lvls.put(perk.GUID(), current - 1);
        }

        removeUnlearnedPerks(player);
    }


    @Override
    public List<StatContext> getStatAndContext(LivingEntity en) {
        List<ExactStatData> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> s : this.allocated_lvls.entrySet()) {
            if (ExileDB.Perks().isRegistered(s.getKey())) {
                for (OptScaleExactStat stat : ExileDB.Perks().get(s.getKey()).stats) {
                    var data = stat.toExactStat(Load.Unit(en).getLevel());
                    data.percentIncrease = (s.getValue() - 1) * 100;
                    data.increaseByAddedPercent();
                    stats.add(data);
                }
            }
        }
        return Arrays.asList(new SimpleStatCtx(StatContext.StatCtxType.PASSIVES, stats));
    }

}
