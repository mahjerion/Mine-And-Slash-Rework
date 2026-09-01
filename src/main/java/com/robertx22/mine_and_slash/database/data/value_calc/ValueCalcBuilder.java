package com.robertx22.mine_and_slash.database.data.value_calc;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.WeaponDamage;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class ValueCalcBuilder {
    ValueCalculation calc;

    public static ValueCalcBuilder of(String id) {
        ValueCalcBuilder b = new ValueCalcBuilder();
        b.calc = new ValueCalculation();
        b.calc.id = id;
        return b;
    }

    private ValueCalcBuilder baseValue(float min, float max) {
        calc.base = new LeveledValue(min, max);
        return this;
    }

    private ValueCalcBuilder defaultBaseValue(float v1, float v2) {
        float min = 2 * v1;
        float max = 6 * v2;
        return this.baseValue(min, max);
    }

    public ValueCalcBuilder attackScaling(float min, float max) {
        this.calc.dmg_effectiveness = new ScalingCalc(Health.getInstance(), new LeveledValue(min, max));


        defaultBaseValue(min, max);
        return statScaling(WeaponDamage.getInstance(), min, max);

    }

    public ValueCalcBuilder capScaling(float min) {
        this.calc.cap_to_wep_dmg = min;
        this.calc.dmg_effectiveness.multi.max += (min / 2F);
        this.calc.dmg_effectiveness.multi.min += (min / 2F);
        return this;
    }

    public ValueCalcBuilder spellScaling(float min, float max) {
        this.calc.dmg_effectiveness = new ScalingCalc(Health.getInstance(), new LeveledValue(min, max));

        defaultBaseValue(min, max);
        return statScaling(WeaponDamage.getInstance(), min, max);

    }

    /**
     * A flat, level independent calc for a skill only a monster casts.
     * <p>
     * Differs from {@link #spellScaling} in the two ways a mob cast skill needs. Every value is
     * pinned min == max, because {@code LeveledValue.getValue} short circuits on that and a mob
     * skill is authored {@code default_lvl 1 / max_lvl 1} - interpolating would hand it about a
     * ninth of the range. And {@code base} is given outright rather than derived as
     * {@code 2*min .. 6*max}, so it can be set to zero: a monster has no Weapon Damage stat, so
     * {@code multi} multiplies nothing and {@code dmg_effectiveness} is the real per hit knob -
     * see {@code ValueCalculation.mobBaseDamage}.
     */
    public ValueCalcBuilder flatWeaponScaling(float multi, float base) {
        this.calc.dmg_effectiveness = new ScalingCalc(Health.getInstance(), new LeveledValue(multi, multi));
        this.calc.base = new LeveledValue(base, base);
        return statScaling(WeaponDamage.getInstance(), multi, multi);
    }

    public ValueCalcBuilder statScaling(Stat stat, float min, float max) {
        calc.stat_scalings.add(new ScalingCalc(stat, new LeveledValue(min, max)));
        return this;
    }

    public ValueCalcBuilder targetStatScaling(Stat stat, float min, float max) {
        calc.target_stat_scalings.add(new ScalingCalc(stat, new LeveledValue(min, max)));
        return this;
    }

    public ValueCalculation build() {
        calc.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        return calc;
    }
}
