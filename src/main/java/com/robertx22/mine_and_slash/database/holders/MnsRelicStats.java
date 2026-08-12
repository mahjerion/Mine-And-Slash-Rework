package com.robertx22.mine_and_slash.database.holders;

import com.robertx22.addons.dungeon_realm.MnsMapContents;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.LootTypeKey;
import com.robertx22.library_of_exile.database.relic.stat.GuaranteeContentRS;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.database.data.relic.stat.MnsLootTypeBonusRS;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MnsRelicStats extends ExileKeyHolder<RelicStat> {

    public static MnsRelicStats INSTANCE = new MnsRelicStats(MMORPG.REGISTER_INFO);

    public MnsRelicStats(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKeyMap<RelicStat, LootTypeKey> LOOT_TYPE = new ExileKeyMap<RelicStat, LootTypeKey>(this, "bonus")
            .ofList(Arrays.stream(LootType.values()).filter(x -> x != LootType.All).map(e -> new LootTypeKey(e)).collect(Collectors.toList()))
            .build((id, info) -> {
                return new MnsLootTypeBonusRS(id, info.type);
            });

    // one guarantee stat per league mechanic this mod owns. These back the implicit affixes relics
    // roll - a successful roll claims one of the map's bonus content slots outright instead of just
    // nudging the weighted pick like the event chance stats do.
    public ExileKey<RelicStat, KeyInfo> GUARANTEE_PROPHECY = ExileKey.ofId(this, "guarantee_prophecy_content", x -> {
        return new GuaranteeContentRS(x.GUID(), SlashRef.MODID, MnsMapContents.INSTANCE.PROPHECY.GUID(), "Prophecy");
    });
    public ExileKey<RelicStat, KeyInfo> GUARANTEE_STRONGBOX = ExileKey.ofId(this, "guarantee_strongbox_content", x -> {
        return new GuaranteeContentRS(x.GUID(), SlashRef.MODID, MnsMapContents.INSTANCE.STRONGBOX.GUID(), "Strongbox");
    });
    public ExileKey<RelicStat, KeyInfo> GUARANTEE_IMPRISONED_MONSTER = ExileKey.ofId(this, "guarantee_imprisoned_monster_content", x -> {
        return new GuaranteeContentRS(x.GUID(), SlashRef.MODID, MnsMapContents.INSTANCE.IMPRISONED_MONSTER.GUID(), "Imprisoned Monster");
    });
    public ExileKey<RelicStat, KeyInfo> GUARANTEE_SHRINE = ExileKey.ofId(this, "guarantee_shrine_content", x -> {
        return new GuaranteeContentRS(x.GUID(), SlashRef.MODID, MnsMapContents.INSTANCE.SHRINE.GUID(), "Shrine");
    });


    @Override
    public void loadClass() {

    }
}