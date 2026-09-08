package com.robertx22.mine_and_slash.uncommon.interfaces.data_items;

import net.minecraft.world.entity.ai.attributes.Attribute;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Cached {

    public static List<ImmutablePair<Attribute, UUID>> VANILLA_STAT_UIDS_TO_CLEAR_EVERY_STAT_CALC = new ArrayList<>();
    public static HashMap<String, Integer> MAX_SPELL_CHARGES = new HashMap<>();

    /**
     * Every vanilla attribute modifier an exile effect can put on an entity (mc_stats), keyed by the
     * effect that owns it. Each stat calc strips the ones whose effect is no longer active on the
     * entity - see StatCalculation.calc. The modifiers are transient and only ever removed by the
     * effect's own onRemove, so any removal path that skips it (or a re-apply racing it) leaves a
     * x0 attack damage / movement speed on the entity for the rest of its life.
     */
    public static List<ExileEffectVanillaModifier> EXILE_EFFECT_VANILLA_MODIFIERS = new ArrayList<>();

    public record ExileEffectVanillaModifier(String effectId, Attribute attribute, UUID uuid) {
    }

 
}
