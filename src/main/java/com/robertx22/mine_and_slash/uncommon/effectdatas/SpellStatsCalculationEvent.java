package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class SpellStatsCalculationEvent extends EffectEvent {
    public static String ID = "on_spell_stat_calc";

    @Override
    public String GUID() {
        return ID;
    }

    int lvl;

    public CalculatedSpellData savedData;

    @Override
    public String getName() {
        return "Spell Calc Event";
    }

    public SpellStatsCalculationEvent(LivingEntity caster, String spellid) {
        super(caster, caster);

        Spell spell = ExileDB.Spells().get(spellid);


        this.savedData = create(Load.Unit(caster).getLevel(), caster, spell);
        this.lvl = Load.Unit(caster).getLevel();


        this.data.setString(EventData.STYLE, spell.config.getStyle().id);

        this.data.setString(EventData.SUMMON_TYPE, spell.config.summonType.id);

        this.data.setupNumber(EventData.AGGRO_RADIUS, spell.config.aggro_radius);

        this.data.setString(EventData.SPELL, spellid);

        float manamultilvl = GameBalanceConfig.get().MANA_COST_SCALING.getMultiFor(lvl);
        if (caster instanceof Player p) {
            var gem = Load.player(p).getSkillGemInventory().getSpellGem(spell);
            if (gem != null) {
                manamultilvl *= gem.getManaCostMulti(p);
            }
        }
        this.data.setupNumber(EventData.CAST_TICKS, spell.config.getCastTimeTicks());
        this.data.setupNumber(EventData.CAST_SPEED_TICKS, spell.config.getCastSpeedTicks());
        this.data.setupNumber(EventData.CAST_SPEED_PERCENT, 0F);
        this.data.setupNumber(EventData.CHANNEL_SPEED_PERCENT, 0F);
        this.data.setupNumber(EventData.MANA_COST, manamultilvl * spell.config.mana_cost.getValue(caster, spell));
        this.data.setupNumber(EventData.ENERGY_COST, manamultilvl * spell.config.ene_cost.getValue(caster, spell));
        this.data.setupNumber(EventData.COOLDOWN_TICKS, spell.config.cooldown_ticks);
        this.data.setupNumber(EventData.CHARGE_COOLDOWN_TICKS, spell.config.charge_regen);
        this.data.setupNumber(EventData.PROJECTILE_SPEED_MULTI, 1F);
        this.data.setupNumber(EventData.PROJECTILE_YAW_SPEED_MULTI, 1F);
        this.data.setupNumber(EventData.PROJECTILE_SPREAD_RANDOMNESS, 1F);
        this.data.setupNumber(EventData.DURATION_MULTI, 1F);
        this.data.setupNumber(EventData.AREA_MULTI, 1);
        this.data.setupNumber(EventData.AGGRO_RADIUS_MULTI, 1);

        // todo test spells like summon duration multi etc
    }


    @Override
    protected void activate() {

        int cd = (int) Mth.clamp(data.getNumber(EventData.COOLDOWN_TICKS).number, getSpell().config.cooldown_ticks * GameBalanceConfig.get().MIN_SPELL_COOLDOWN_MULTI, 1000000);
        this.data.getNumber(EventData.COOLDOWN_TICKS).number = cd; // cap it to 80% cooldown


        cd = (int) Mth.clamp(data.getNumber(EventData.CHARGE_COOLDOWN_TICKS).number, getSpell().config.charge_regen * GameBalanceConfig.get().MIN_SPELL_COOLDOWN_MULTI, 1000000);
        this.data.getNumber(EventData.CHARGE_COOLDOWN_TICKS).number = cd; // cap it to 80% cooldown

        // speed is a frequency multiplier now: +100% means twice the casts, not ten times them. every
        // source sums into one percent first, so two +50% rolls give +100% rather than compounding.
        float pct = data.getNumber(EventData.CAST_SPEED_PERCENT).number;

        // keyed on the TAG, not config.isChannel(). Every other channel stat - channel_spell_dmg,
        // channel_cdr, channel_cast_time, the two channelling support gems - is conditioned on the
        // tag alone, and this one being the odd double-gated exception left a skill that is a
        // channel in every way except the boolean with the identity but not the speed. That case is
        // real: a mercenary cannot use a true channel at all (castTimeTicksFor forces one to fire
        // instantly, since there is no held input), so its channels are authored as tagged
        // multicasts. Nothing existing shifts - every spell carrying the tag today also sets the
        // boolean.
        if (getSpell().is(SpellTags.channel)) {
            // a channel's beat belongs to Channel Speed. the general pot still bleeds through at a
            // fraction, so ordinary gear is not dead weight for a channel build - and because this
            // weights the pot rather than naming stats, a per tag roll like Fire Skill Speed counts too
            pct = pct * (float) GameBalanceConfig.get().CHANNEL_GENERAL_SPEED_TRANSFER
                    + data.getNumber(EventData.CHANNEL_SPEED_PERCENT).number;
        }

        // floored above -100 so it can never divide by zero
        float speedMulti = 1F + Math.max(-99F, pct) / 100F;

        this.data.getNumber(EventData.CAST_SPEED_TICKS).number = Math.max(
                GameBalanceConfig.get().GLOBAL_COOLDOWN_TICKS,
                data.getNumber(EventData.CAST_SPEED_TICKS).number / speedMulti);

        // the cast itself speeds up with the recovery, otherwise a long cast time would swallow the
        // gain and slow skills would feel untouched by the stat
        this.data.getNumber(EventData.CAST_TICKS).number = data.getNumber(EventData.CAST_TICKS).number / speedMulti;

        this.savedData.data = data;
    }

    private CalculatedSpellData create(int lvl, LivingEntity caster, Spell spell) {
        Objects.requireNonNull(caster);

        CalculatedSpellData data = new CalculatedSpellData(this);
        data.spell_id = spell.GUID();
        data.lvl = lvl;
        data.caster_uuid = caster.getUUID().toString();


        return data;
    }


}
