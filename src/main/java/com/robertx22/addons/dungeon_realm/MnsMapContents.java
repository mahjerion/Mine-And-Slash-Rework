package com.robertx22.addons.dungeon_realm;

import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;

public class MnsMapContents extends ExileKeyHolder<MapContent> {

    public static MnsMapContents INSTANCE = new MnsMapContents(MMORPG.REGISTER_INFO);

    public MnsMapContents(ModRequiredRegisterInfo info) {
        super(info);
    }

    public ExileKey<MapContent, KeyInfo> PROPHECY = ExileKey.ofId(this, "prophecy", x -> MapContent.of(x.GUID(), 1000, SlashBlocks.PROPHECY_ALTAR.getRegistryObject().getKey().location().toString(), 3, 5));

    // Strongbox bonus encounter: 5-7 boxes scattered per map when picked as bonus content.
    public ExileKey<MapContent, KeyInfo> STRONGBOX = ExileKey.ofId(this, "strongbox", x -> MapContent.of(x.GUID(), 1000, SlashBlocks.STRONGBOX.getRegistryObject().getKey().location().toString(), 3, 5));

    // Imprisoned Monster bonus encounter: 1 caged elite per map when picked as bonus content.
    public ExileKey<MapContent, KeyInfo> IMPRISONED_MONSTER = ExileKey.ofId(this, "imprisoned_monster", x -> MapContent.of(x.GUID(), 1000, SlashBlocks.IMPRISONED_MONSTER.getRegistryObject().getKey().location().toString(), 2, 3));

    // Shrine bonus encounter: 1-2 buff shrines scattered per map when picked as bonus content.
    public ExileKey<MapContent, KeyInfo> SHRINE = ExileKey.ofId(this, "shrine", x -> MapContent.of(x.GUID(), 1000, SlashBlocks.SHRINE.getRegistryObject().getKey().location().toString(), 2, 3));


    @Override
    public void loadClass() {

    }
}
