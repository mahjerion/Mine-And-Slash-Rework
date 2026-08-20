package com.robertx22.mine_and_slash.uncommon.stat_calculation;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.misc.BonusExp;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlayerStatUtils {

    public static List<StatContext> addToolStats(Player p) {
        ItemStack stack = p.getMainHandItem();
        if (StackSaving.TOOL.has(stack)) {
            return Arrays.asList(new SimpleStatCtx(StatContext.StatCtxType.TOOL, StackSaving.TOOL.loadFrom(stack).GetAllStats()));
        } else {
            return Arrays.asList();
        }
    }

    public static StatContext addBonusExpPerCharacters(Player p) {

        var data = Load.Unit(p);
        var pd = Load.player(p);
        int lvl = data.getLevel();


        // hoisted out of the filter below - it cannot change during the stream, and getCurrent()
        // builds a blank CharacterData on a miss. Objects.equals because that blank leaves `name`
        // null, as does any character stored before it was named. an NPE here would take down the
        // whole recalcAllocated pass, not just this stat context.
        String currentName = pd.characters.getCurrent().name;

        List<Integer> all = pd.characters.getAllCharacters().stream()
                .filter(x -> !Objects.equals(x.name, currentName))
                .map(x -> x.lvl)
                .collect(Collectors.toList());


        int higher = (int) all.stream().filter(x -> x > lvl).count();

        if (higher > 0) {
            return new SimpleStatCtx(StatContext.StatCtxType.BONUS_XP_PER_CHARACTER, Arrays.asList(ExactStatData.noScaling(
                    ServerContainer.get().BONUS_EXP_PERCENT_PER_HIGHER_LVL_CHARACTERS.get() * higher, ModType.FLAT, BonusExp.getInstance().GUID())));
        }
        return new SimpleStatCtx(StatContext.StatCtxType.BONUS_XP_PER_CHARACTER, Arrays.asList());
    }

    public static List<StatContext> addNewbieElementalResists(EntityData data) {
        List<ExactStatData> stats = new ArrayList<>();

        if (!CompatConfig.get().newbieResists()) {
            return Arrays.asList(new SimpleStatCtx(StatContext.StatCtxType.NEWBIE_RESISTS, stats));
        }

        int value = 50;

        if (data.getLevel() > 24) {
            value = 25;
        }
        if (data.getLevel() > 49) {
            value = 0;
        }
        if (data.getLevel() > 74) {
            value = -25;
        }
        for (Elements ele : Elements.getAllSingle()) {
            if (ele != Elements.Physical) {
                stats.add(ExactStatData.noScaling(value, ModType.FLAT, new ElementalResist(ele).GUID()));
            }
        }
        return Arrays.asList(new SimpleStatCtx(StatContext.StatCtxType.NEWBIE_RESISTS, stats));

    }

}
