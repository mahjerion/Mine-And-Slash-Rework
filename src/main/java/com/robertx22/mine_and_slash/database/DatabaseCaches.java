package com.robertx22.mine_and_slash.database;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.AttributeStat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.uncommon.error_checks.base.ErrorChecks;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.Cached;
import com.robertx22.mine_and_slash.vanilla_mc.packets.TellClientResetCaches;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseCaches {

    public static void init() {

        // can't find a client load event
        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedInEvent.class, x -> {
            Packets.sendToClient(x.getEntity(), new TellClientResetCaches());
            // maybe i delay this by a second to make sure the caches dont generate too early?
        });

        // this is only called on server I think
        ExileEvents.AFTER_DATABASE_LOADED.register(new EventConsumer<ExileEvents.AfterDatabaseLoaded>() {
            @Override
            public void accept(ExileEvents.AfterDatabaseLoaded event) {
                resetCaches();
            }
        });

    }

    public static void resetCaches() {
        setupStatsThatAffectVanillaStatsList();
        setupMaxSpellCharges();
        ErrorChecks.getAll().forEach(x -> x.check());
        GearSlot.CACHED = new HashMap<>();
    }

    private static void setupMaxSpellCharges() {
        Cached.MAX_SPELL_CHARGES = new HashMap<>();

        for (Spell spell : ExileDB.Spells().getList()) {
            if (spell.config.charges > 0) {
                Cached.MAX_SPELL_CHARGES.put(spell.config.charge_name, spell.config.charges);
            }
        }
    }

    private static void setupStatsThatAffectVanillaStatsList() {
        Cached.VANILLA_STAT_UIDS_TO_CLEAR_EVERY_STAT_CALC = new ArrayList<>();

        ExileDB.Stats()
                .getFilterWrapped(x -> x instanceof AttributeStat).list.forEach(x -> {
                    AttributeStat attri = (AttributeStat) x;
                    Cached.VANILLA_STAT_UIDS_TO_CLEAR_EVERY_STAT_CALC.add(ImmutablePair.of(attri.attribute, attri.uuid));
                });
    }
}
