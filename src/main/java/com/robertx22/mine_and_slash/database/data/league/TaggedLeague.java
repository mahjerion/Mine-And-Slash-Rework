package com.robertx22.mine_and_slash.database.data.league;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

// A league that has no spatial bounds of its own - it's resolved from the loot context instead
// (the killed mob's rarity/encounter tag, or an explicit assignment on a LootInfo), never from a
// position. Prophecy is the original of this shape: it's a curse applied to a whole map, not a place,
// so League.getFromPosition can never find it. Strongbox/Imprisoned Monster are marker blocks scattered
// inside the dungeon dimension with no structure of their own, and Uber/Pinnacle are boss rarities.
// See LootLeagueResolvers for how these get attached to a LootInfo.
public class TaggedLeague extends League {

    private final ChatFormatting color;
    private final String locName;

    public TaggedLeague(String id, ChatFormatting color, String locName) {
        super(id);
        this.color = color;
        this.locName = locName;
    }

    @Override
    public boolean isInSide(ServerLevel serverLevel, BlockPos blockPos) {
        return false;
    }

    @Override
    public ChatFormatting getTextColor() {
        return color;
    }

    @Override
    public String modid() {
        return SlashRef.MODID;
    }

    @Override
    public String locName() {
        return locName;
    }
}
