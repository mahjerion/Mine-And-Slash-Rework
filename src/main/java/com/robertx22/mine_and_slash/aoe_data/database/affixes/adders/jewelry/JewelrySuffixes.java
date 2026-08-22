package com.robertx22.mine_and_slash.aoe_data.database.affixes.adders.jewelry;

import com.robertx22.mine_and_slash.aoe_data.database.affixes.AffixBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.affixes.ElementalAffixBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.stats.OffenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.old.DatapackStats;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.stats.types.core_stats.AllAttributes;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.TreasureQuality;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.TreasureQuantity;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.HealthRegen;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShieldRegen;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.ManaRegen;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import java.util.Arrays;

public class JewelrySuffixes implements ExileRegistryInit {

    @Override
    public void registerAll() {

        ElementalAffixBuilder.start()
                .guid(x -> x.guidName + "_ele_dmg_jewelry")
                .add(Elements.Fire, "of Embers")
                .add(Elements.Cold, "of Ice")
                .add(Elements.Shadow, "of Venom")
                .stats(x -> Arrays.asList(new StatMod(3, 10, OffenseStats.ELEMENTAL_DAMAGE.get(x), ModType.FLAT)))
                .includesTags(SlotTags.jewelry_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_the_philosopher")
                .Named("of the Philosopher")
                .coreStat(DatapackStats.INT)
                .includesTags(SlotTags.jewelry_family, SlotTags.armor_family)
                .excludesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_the_titan")
                .Named("of the Titan")
                .coreStat(DatapackStats.STR)
                .includesTags(SlotTags.jewelry_family, SlotTags.armor_family)
                .excludesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_the_wind")
                .Named("of the Wind")
                .coreStat(DatapackStats.DEX)
                .includesTags(SlotTags.jewelry_family, SlotTags.armor_family)
                .excludesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();


        AffixBuilder.Normal("of_the_sky")
                .Named("of the Sky")
                .stats(new StatMod(0.1F, 0.4F, AllAttributes.getInstance(), ModType.FLAT))
                .includesTags(SlotTags.jewelry_family)
                .Weight(50)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_the_troll")
                .Named("of the Troll")
                .stats(new StatMod(3, 15, HealthRegen.getInstance(), ModType.PERCENT))
                .includesTags(SlotTags.jewelry_family, SlotTags.armor_family, SlotTags.shield)
                .Weight(200)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_spirit_markings")
                .Named("of Spirit Markings")
                .stats(new StatMod(3, 15, ManaRegen.getInstance(), ModType.PERCENT))
                .includesTags(SlotTags.jewelry_family, SlotTags.armor_family, SlotTags.tome)
                .Weight(200)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_azure_skies")
                .Named("of Azure Skies")
                .stats(new StatMod(3, 15, MagicShieldRegen.getInstance(), ModType.PERCENT))
                .includesTags(SlotTags.jewelry_family, SlotTags.tome)
                .Weight(200)
                .Suffix()
                .Build();


        AffixBuilder.Normal("of_treasure")
                .Named("of Treasure")
                .stats(new StatMod(3, 10F, TreasureQuality.getInstance(), ModType.FLAT))
                .includesTags(SlotTags.jewelry_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_affluence")
                .Named("of Affluence")
                .stats(new StatMod(3, 10, TreasureQuantity.getInstance(), ModType.FLAT))
                .includesTags(SlotTags.jewelry_family)
                .Suffix()
                .Build();

    }
}
