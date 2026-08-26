package com.robertx22.mine_and_slash.uncommon.stat_calculation;

import com.robertx22.mine_and_slash.aoe_data.database.base_stats.BaseStatsAdder;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.stat_compat.StatCompat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.maps.MapItemData;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CommonStatUtils {


    // no idea about perf impact lets keep this player only for now
    public static StatContext addStatCompat(LivingEntity en) {
        List<ExactStatData> list = new ArrayList<>();

        for (StatCompat c : ExileDB.StatCompat().getList()) {
            if (c.isAttributeCompat()) {
                var data = c.getResult(en, Load.Unit(en).getLevel());
                if (data != null) {
                    list.add(data);
                }
            }
        }

        return new SimpleStatCtx(StatContext.StatCtxType.VANILLA_STAT_COMPAT, list);
    }

    public static List<StatContext> addExactCustomStats(LivingEntity en) {
        return Load.Unit(en)
                .getCustomExactStats()
                .getStatAndContext(en);
    }

    public static List<StatContext> addBaseStats(LivingEntity en) {

        try {

            String id;

            if (en instanceof MercenaryEntity) {
                // a companion is neither a character nor a monster, so it gets a datapack entry of
                // its own - a pack can retune merc resources and regen without moving the player's.
                id = BaseStatsAdder.MERCENARY;

                // the entry only exists once datagen has emitted it. falling through to the empty
                // default would leave a mercenary on ~1 hp, so read the player entry - the old
                // behaviour - rather than nothing at all.
                if (!ExileDB.BaseStats().isRegistered(id)) {
                    id = CompatConfig.get().baseStatsDatapack().id;
                }
            } else if (en instanceof Player) {
                id = CompatConfig.get().baseStatsDatapack().id;
            } else {
                id = BaseStatsAdder.MOB;
            }

            BaseStatsConfig stats = ExileDB.BaseStats().get(id);

            Objects.requireNonNull(stats);

            return stats.getStatAndContext(en);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Arrays.asList();

    }

    public static List<StatContext> addMapAffixStats(LivingEntity en) {


        var list = new ArrayList<StatContext>();

        // todo make this CONNECTED to other mods if they spawn in there.
        // an obelisk that spawned in a map should also have map stats in it and count as a map
        // ALSO, obelisks shouldnt be able to take more than 1 map per block, inside these dimensions

        WorldUtils.ifMapData(en.level(), en.blockPosition()).ifPresent(map -> {
            MapItemData data = map.map;
            for (StatContext stat : data.getStatAndContext(en)) {
                list.add(stat);
            }
        });

        return list;

    }

}
