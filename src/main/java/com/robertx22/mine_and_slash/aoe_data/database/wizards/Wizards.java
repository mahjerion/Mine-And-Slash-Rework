package com.robertx22.mine_and_slash.aoe_data.database.wizards;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.spells.schools.WitchSpells;
import com.robertx22.mine_and_slash.database.data.wizard.WizardType;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;

import java.util.Arrays;
import java.util.List;

/**
 * The four elemental caster monsters.
 * <p>
 * They exist because elemental damage was almost absent from maps: a normal monster deals physical
 * weapon damage, and the only route to fire, cold, lightning or chaos was rolling an elemental mob
 * affix - a chance on top of a rarity chance. Rather than raise those odds, these put elemental
 * damage in the spawn pool directly, so a map reliably contains monsters whose whole job is to deal
 * it and a player's elemental resistances have something to answer.
 * <p>
 * Registered after {@link WitchSpells} in {@code GeneratedData} - a wizard names spell guids, which
 * have to exist first.
 * <p>
 * Each id matches its entity type's path, which is how {@code WizardEntity} finds its own entry.
 * The three-skill loadouts are deliberately one long range opener, one mid range and one that only
 * comes into play up close, so the mob has something to do at every distance rather than standing
 * idle whenever its single skill is out of reach. Chaos has only one because the base mod has only
 * one chaos spell suited to a monster - a pack with a fuller chaos school should give it more.
 */
public class Wizards implements ExileRegistryInit {

    public static String FIRE = "fire_wizard";
    public static String ICE = "ice_wizard";
    public static String LIGHTNING = "lightning_wizard";
    public static String CHAOS = "chaos_wizard";

    @Override
    public void registerAll() {

        of(FIRE, Arrays.asList(
                WitchSpells.WITCH_FIREBALL,
                WitchSpells.WITCH_FIRE_NOVA,
                WitchSpells.WITCH_METEOR));

        of(ICE, Arrays.asList(
                WitchSpells.WITCH_FROSTBALL,
                WitchSpells.WITCH_CHILLING_FIELD,
                WitchSpells.WITCH_FROZEN_ORB));

        of(LIGHTNING, Arrays.asList(
                WitchSpells.WITCH_LIGHTNING_SPEAR,
                WitchSpells.WITCH_LIGHTNING_TOTEM,
                WitchSpells.WITCH_CHAIN_LIGHTNING));

        of(CHAOS, Arrays.asList(
                WitchSpells.WITCH_POISON_BALL));
    }

    private void of(String id, List<String> spells) {
        WizardType w = new WizardType(id, SlashRef.MODID + ":textures/entity/" + id + ".png", spells);
        w.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }
}
