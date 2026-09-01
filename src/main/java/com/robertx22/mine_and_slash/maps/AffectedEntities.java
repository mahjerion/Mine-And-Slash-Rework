package com.robertx22.mine_and_slash.maps;

import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public enum AffectedEntities {
    Mobs, Players, None, All;

    public static AffectedEntities of(LivingEntity en) {
        if (Load.Unit(en).isSummon()) {
            return None;
        }
        // a mercenary is a companion, not a monster, but it is not a Player object either - so it
        // used to fall through to Mobs and drink the map's "monsters are stronger here" affixes:
        // up to +100 crit chance, +50% crit damage, +100% physical as elemental, +50% health,
        // +100% armour. It takes the player side instead, curses included, which is both what the
        // design wants and why its damage no longer scales with where its owner is standing.
        if (en instanceof Player || en instanceof MercenaryEntity) {
            return Players;
        } else {
            return Mobs;
        }
    }
}