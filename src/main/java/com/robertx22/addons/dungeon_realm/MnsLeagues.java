package com.robertx22.addons.dungeon_realm;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.database.data.league.TaggedLeague;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import net.minecraft.ChatFormatting;

// The main mod's own leagues. All of these are TaggedLeagues - they have no spatial bounds, so a
// UniqueGear tagged with one only drops when a LootLeagueResolver (or an explicit assignment, as
// GearProphecy does) puts that league on the LootInfo. Harvest/Obelisk/Reward Room live in their own
// mods and are structure-bounded instead.
public class MnsLeagues extends ExileKeyHolder<League> {

    public static MnsLeagues INSTANCE = new MnsLeagues(MMORPG.REGISTER_INFO);

    public MnsLeagues(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    // no mobs of its own - only the Prophecy reward roll ever grants these (GearProphecy)
    public ExileKey<League, KeyInfo> PROPHECY = ExileKey.ofId(this, "prophecy",
            x -> new TaggedLeague(x.GUID(), ChatFormatting.LIGHT_PURPLE, "Prophecy"));

    public ExileKey<League, KeyInfo> STRONGBOX = ExileKey.ofId(this, "strongbox",
            x -> new TaggedLeague(x.GUID(), ChatFormatting.GOLD, "Strongbox"));

    public ExileKey<League, KeyInfo> IMPRISONED_MONSTER = ExileKey.ofId(this, "imprisoned_monster",
            x -> new TaggedLeague(x.GUID(), ChatFormatting.DARK_AQUA, "Imprisoned Monster"));

    public ExileKey<League, KeyInfo> UBER = ExileKey.ofId(this, "uber",
            x -> new TaggedLeague(x.GUID(), ChatFormatting.DARK_RED, "Uber Bosses"));

    public ExileKey<League, KeyInfo> PINNACLE = ExileKey.ofId(this, "pinnacle",
            x -> new TaggedLeague(x.GUID(), ChatFormatting.RED, "Pinnacle Bosses"));

    @Override
    public void loadClass() {

    }
}
