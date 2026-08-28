package com.robertx22.mine_and_slash.aoe_data.database.ailments;

import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocDesc;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.StringUTIL;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;

import java.util.function.Function;

public class Ailment implements ExileRegistry<Ailment>, IAutoLocName, IAutoLocDesc {

    String id;

    public Elements element;

    public boolean isDot;

    public boolean isStrengthEffect;

    public float damageEffectivenessMulti;

    //public int lostOccursEverySeconds = 3;

    public float percentLostEveryXSeconds;

    public int durationTicks;

    public float percentHealthRequiredForFullStrength = 0.25F;

    Function<Ailment, String> desc;

    public int getDurationSeconds() {
        return durationTicks / 20;
    }

    public int getPercentDamage() {
        return (int) (damageEffectivenessMulti * 100F);
    }

    // freeze/electrify accumulate damage that a proc stat later releases in one burst. that burst
    // has its own player facing name - it is never called "Freeze"/"Electrify" in the ui
    public Words procNameWord() {
        return element == Elements.Nature ? Words.SHOCK : Words.SHATTER;
    }

    public Ailment(String id, Elements element, boolean isDot, boolean isStrengthEffect, float damageEffectivenessMulti, float percentLostEveryXSeconds, int durationTicks, Function<Ailment, String> desc) {
        this.id = id;
        this.isStrengthEffect = isStrengthEffect;
        this.element = element;
        this.isDot = isDot;
        this.desc = desc;
        this.damageEffectivenessMulti = damageEffectivenessMulti;
        this.percentLostEveryXSeconds = percentLostEveryXSeconds;
        this.durationTicks = durationTicks;

        Ailments.ALL.add(this);
    }

    // vanilla slowness is a MULTIPLY_TOTAL modifier of -0.15 * (amplifier + 1), so amplifier 6 is
    // already -1.05: a NEGATIVE multiplier. any second negative multiplier on the entity (a
    // move_speed stat past -100%, another cc effect) multiplies the two back into a large POSITIVE
    // speed instead of stacking the slows. 5 is the last amplifier that stays above -100%.
    public static final int MAX_SAFE_SLOW_TIER = 5;

    public int getSlowTier(float multi) {

        if (multi == 0) {
            return -1;
        }

        int tier = (int) (multi * 10D);

        if (multi >= 1) {
            tier = MAX_SAFE_SLOW_TIER;
        }
        return Math.min(tier, MAX_SAFE_SLOW_TIER);
    }


    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.AILMENT;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.StatusEffects;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".ailment." + GUID();
    }

    @Override
    public String locNameForLangFile() {
        return StringUTIL.capitalise(id);
    }


    @Override
    public AutoLocGroup locDescGroup() {
        return AutoLocGroup.StatusEffects;
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".ailment.desc." + GUID();
    }


    @Override
    public String locDescForLangFile() {
        return desc.apply(this);
    }
}
