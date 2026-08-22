package com.robertx22.mine_and_slash.aoe_data.database.affixes.adders;

import com.robertx22.mine_and_slash.aoe_data.database.affixes.AffixBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.SpellChangeStats;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.ArmorPenetration;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.SkillDamage;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class WeaponSuffixes implements ExileRegistryInit {

    @Override
    public void registerAll() {

        AffixBuilder.Normal("of_pene")
                .Named("of Penetration")
                .stats(new StatMod(5, 25, ArmorPenetration.getInstance(), ModType.FLAT))
                .includesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_vampirism")
                .Named("of Vampirism")
                .stats(new StatMod(1, 5, ResourceStats.LIFESTEAL.get(), ModType.FLAT))
                .includesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();


        AffixBuilder.Normal("of_gluttony")
                .Named("of Gluttony")
                .stats(new StatMod(1, 6, ResourceStats.RESOURCE_ON_KILL.get(ResourceType.health), ModType.FLAT))
                .includesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_consumption")
                .Named("of Consumption")
                .stats(new StatMod(1, 6, ResourceStats.RESOURCE_ON_KILL.get(ResourceType.mana), ModType.FLAT))
                .includesTags(SlotTags.weapon_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_fast_cast")
                .Named("of Faster Casting")
                .stats(new StatMod(14, 40, SpellChangeStats.CAST_SPEED.get(), ModType.FLAT))
                .includesTags(SlotTags.mage_weapon, SlotTags.jewelry_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_less_cd")
                .Named("of Repetition")
                .stats(new StatMod(6, 15, SpellChangeStats.COOLDOWN_REDUCTION.get(), ModType.FLAT))
                .includesTags(SlotTags.mage_weapon, SlotTags.jewelry_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("of_spell_dmg")
                .Named("of Spell Damage")
                .stats(new StatMod(6, 15, SkillDamage.getInstance(), ModType.FLAT))
                .includesTags(SlotTags.mage_weapon, SlotTags.jewelry_family)
                .Suffix()
                .Build();

        AffixBuilder.Normal("heal_suff")
                .Named("of Restoration")
                .stats(new StatMod(5, 20, ResourceStats.HEAL_STRENGTH.get(), ModType.FLAT))
                .includesTags(SlotTags.staff)
                .Suffix()
                .Build();
    }
}
