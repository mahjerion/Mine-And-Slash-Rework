package com.robertx22.mine_and_slash.uncommon.datasaving;

import com.robertx22.mine_and_slash.capability.chunk.ChunkCap;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerBackpackData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.capability.world.WorldData;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class Load {

    // todo give a blank one for mobs

    public static Unit getSpellUnit(Entity entity, Spell spell) {
        if (spell != null && entity instanceof Player p && player(p).canHaveSpellUnit(spell)) {
            return player(p).getSpellUnitStats(spell);
        }
        // a mercenary sockets support gems under its skills the same way a player does, and those
        // stats live only on the per-spell unit. without this branch they never reached anything
        // that resolves stats through here - a Life Leech support on a merc skill did nothing.
        // same lookup SpellCastContext already uses, and it is cached and invalidated on gear change.
        if (spell != null && entity instanceof MercenaryEntity merc) {
            MercenaryData mercData = merc.getMercData();
            if (mercData != null) {
                return mercData.getSpellUnit(merc, spell);
            }
        }

        return Unit(entity).getUnit();
    }

    public static EntityData Unit(Entity entity) {
        return entity.getCapability(EntityData.INSTANCE).orElseGet(() -> new EntityData((LivingEntity) entity));
    }

    public static PlayerData player(Player player) {
        return player.getCapability(PlayerData.INSTANCE).orElseGet(() -> new PlayerData(player));
    }

    public static PlayerBackpackData backpacks(Player player) {
        return player.getCapability(PlayerBackpackData.INSTANCE).orElse(null);
    }

    public static WorldData worldData(Level l) {
        return l.getServer().overworld().getCapability(WorldData.INSTANCE).orElse(null);
    }

    // todo add connected maps
    public static MapData mapAt(Level l, BlockPos pos) {
        try {
            return WorldUtils.ifMapData(l, pos).get();
        } catch (Exception e) {
            return null;
        }
    }

    public static ChunkCap chunkData(LevelChunk c) {
        return c.getCapability(ChunkCap.INSTANCE).orElseGet(null);
    }

}
