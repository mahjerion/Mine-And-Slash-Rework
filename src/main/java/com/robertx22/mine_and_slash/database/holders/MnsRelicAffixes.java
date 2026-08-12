package com.robertx22.mine_and_slash.database.holders;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.LootTypeKey;
import com.robertx22.library_of_exile.database.relic.affix.RelicAffix;
import com.robertx22.library_of_exile.database.relic.stat.RelicMod;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MnsRelicAffixes extends ExileKeyHolder<RelicAffix> {

    public static MnsRelicAffixes INSTANCE = new MnsRelicAffixes(MMORPG.REGISTER_INFO);

    public MnsRelicAffixes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    static String TYPE = SlashRef.MODID;

    // empty relic_type means "any relic type can roll this" - see RelicGenerator
    static String IMPLICIT_TYPE = "";

    public ExileKeyMap<RelicAffix, LootTypeKey> LOOT_TYPE = new ExileKeyMap<RelicAffix, LootTypeKey>(this, "bonus")
            .ofList(Arrays.stream(LootType.values()).filter(x -> x != LootType.All).map(e -> new LootTypeKey(e)).collect(Collectors.toList()))
            .build((id, info) -> {
                return new RelicAffix(id, TYPE, new RelicMod(MnsRelicStats.INSTANCE.LOOT_TYPE.get(info), 1, 10));
            });

    // Implicits, one of which every relic rolls into its dedicated implicit slot. Empty relic_type on
    // purpose: a league mechanic belongs to whichever mod registered it, not to a relic type, so any
    // relic can roll any of these. Flat 100 so the guarantee doesn't depend on the affix roll.
    public ExileKey<RelicAffix, KeyInfo> GUARANTEE_PROPHECY = ExileKey.ofId(this, "guarantee_prophecy_content", x -> {
        return new RelicAffix(x.GUID(), IMPLICIT_TYPE, new RelicMod(MnsRelicStats.INSTANCE.GUARANTEE_PROPHECY, 100, 100)).setImplicit();
    });
    public ExileKey<RelicAffix, KeyInfo> GUARANTEE_STRONGBOX = ExileKey.ofId(this, "guarantee_strongbox_content", x -> {
        return new RelicAffix(x.GUID(), IMPLICIT_TYPE, new RelicMod(MnsRelicStats.INSTANCE.GUARANTEE_STRONGBOX, 100, 100)).setImplicit();
    });
    public ExileKey<RelicAffix, KeyInfo> GUARANTEE_IMPRISONED_MONSTER = ExileKey.ofId(this, "guarantee_imprisoned_monster_content", x -> {
        return new RelicAffix(x.GUID(), IMPLICIT_TYPE, new RelicMod(MnsRelicStats.INSTANCE.GUARANTEE_IMPRISONED_MONSTER, 100, 100)).setImplicit();
    });
    public ExileKey<RelicAffix, KeyInfo> GUARANTEE_SHRINE = ExileKey.ofId(this, "guarantee_shrine_content", x -> {
        return new RelicAffix(x.GUID(), IMPLICIT_TYPE, new RelicMod(MnsRelicStats.INSTANCE.GUARANTEE_SHRINE, 100, 100)).setImplicit();
    });


    @Override
    public void loadClass() {

    }
}
