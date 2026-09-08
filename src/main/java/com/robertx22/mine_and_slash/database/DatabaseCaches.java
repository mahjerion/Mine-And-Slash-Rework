package com.robertx22.mine_and_slash.database;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.data.item_set.ItemSet;
import com.robertx22.mine_and_slash.database.data.atlas.AtlasNodeLayout;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.VanillaStatData;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
        // unique guid -> set map points at ItemSet objects from the database we just replaced
        ItemSet.resetCache();
        // per-entity EntityConfig caches point at objects from the database we just replaced
        EntityData.invalidateEntityConfigCaches();
        // the parsed Atlas grid resolves cells against the AtlasNode registry, which we may have just
        // replaced. On the client this runs from TellClientResetCaches, which is sent on
        // PlayerLoggedInEvent - after every registry sync packet - so the next read reparses against
        // the completed registry.
        ExileDB.AtlasNodeLayouts().getList().forEach(AtlasNodeLayout::invalidateCalcData);
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

        Cached.EXILE_EFFECT_VANILLA_MODIFIERS = new ArrayList<>();

        for (ExileEffect eff : ExileDB.ExileEffects().getList()) {
            for (VanillaStatData mc : eff.mc_stats) {
                try {
                    Attribute attribute = mc.getAttribute();
                    if (attribute != null) {
                        Cached.EXILE_EFFECT_VANILLA_MODIFIERS.add(new Cached.ExileEffectVanillaModifier(eff.GUID(), attribute, mc.getUUID()));
                    }
                } catch (Exception e) {
                    // a datapack effect naming an attribute or uuid that doesn't parse must not stop
                    // the rest of the list from building
                    e.printStackTrace();
                }
            }
        }
    }
}
