package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailment;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.AilmentSpeed;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailments;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentDuration;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentEffectStat;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentResistance;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class EntityAilmentData {

    // the slow is re-stamped every second from onTick so it tracks the decaying meter. it only
    // has to outlive that interval - a long duration here would pin the enemy at the tier the
    // freeze had at its peak, because vanilla never lets a lower amplifier replace a higher one
    private static final int SLOW_TICKS = 30;

    // below this the pool is not worth a burst (shatterAccumulated casts to int) or a map entry
    private static final float MIN_KEPT_DMG = 0.5F;
    // and below this the meter can no longer produce a slow above the weakest tier, so the chill
    // ends rather than trailing a barely visible Slowness I for another 40 seconds. raise it to
    // shorten the fade, lower it to lengthen - at the default 10%/sec decay this is ~22s from full
    private static final float MIN_KEPT_STRENGTH = 0.1F;

    public HashMap<UUID, OneData> datas = new HashMap<UUID, OneData>();


    public static class OneData {

        public HashMap<String, List<DotData>> dotMap = new HashMap<String, List<DotData>>();
        public HashMap<String, Float> strMap = new HashMap<String, Float>();
        public HashMap<String, Float> dmgMap = new HashMap<String, Float>();
        // the attacker's Ailment Duration multiplier, snapshotted when the ailment lands. dots
        // bake duration into their tick count at that same moment; freeze/electrify have no tick
        // count, so it's kept here and divides the per second decay instead
        public HashMap<String, Float> durMap = new HashMap<String, Float>();

        public boolean isEmpty() {
            return dotMap.isEmpty() && strMap.isEmpty() && dmgMap.isEmpty() && durMap.isEmpty();
        }

    }

    // freeze is the only ailment that reads strMap - electrify accumulates and nothing else, so
    // building a meter for it would be per hit and per second work with no gameplay effect
    private static boolean usesStrengthMeter(Ailment ailment) {
        return ailment != null && ailment.GUID().equals(Ailments.FREEZE.GUID());
    }

    public boolean hasAccumulated(LivingEntity caster, Ailment ailment) {
        var data = datas.get(caster.getUUID());
        return data != null && data.dmgMap.getOrDefault(ailment.GUID(), 0F) > 0;
    }

    // returns whether a burst actually happened. the caller flags the triggering hit as an
    // ailment proc off this, so an empty pool doesn't announce a proc that dealt nothing
    public boolean shatterAccumulated(LivingEntity caster, LivingEntity target, Ailment ailment, Spell spell) {

        var data = datas.get(caster.getUUID());

        if (data == null) {
            return false; // nothing was ever banked against this target, don't create an entry
        }

        float dmg = data.dmgMap.getOrDefault(ailment.GUID(), 0F);

        if (dmg <= 0) {
            return false;
        }

        data.dmgMap.remove(ailment.GUID());
        // shattering consumes the ice. the meter drove the slow, and it shouldn't outlive the
        // burst it just paid for
        data.strMap.remove(ailment.GUID());
        data.durMap.remove(ailment.GUID());

        var b = EventBuilder.ofDamage(caster, target, (int) dmg).setupDamage(AttackType.dot, WeaponTypes.none, PlayStyle.INT).set(x -> {
            x.calcSourceEffects = false;
            x.calcTargetEffects = false;
            x.setElement(ailment.element);
            x.setisAilmentDamage(ailment);
            x.data.setBoolean(EventData.IS_AILMENT_PROC, true);

            if (spell != null) {
                x.data.setString(EventData.WEAPON_TYPE, spell.getWeapon(caster).id);
                x.data.setString(EventData.SPELL, spell.GUID());
            }
        });
        var ev = b.build();
        // the damage chat message is sent from inside activate(), sending it here too double posts
        ev.Activate();

        return true;
    }

    public void onAilmentCausingDamage(LivingEntity caster, LivingEntity target, Ailment ailment, float dmg, Unit unit) {
        if (!datas.containsKey(caster.getUUID())) {
            datas.put(caster.getUUID(), new OneData());
        }
        var data = datas.get(caster.getUUID());


        AilmentDuration dur = new AilmentDuration(ailment);
        AilmentResistance res = new AilmentResistance(ailment);
        AilmentEffectStat eff = new AilmentEffectStat(ailment);

        float speed = unit.getCalculatedStat(AilmentSpeed.INSTANCE).getMultiplier();
        float effMulti = unit.getCalculatedStat(eff).getMultiplier();
        float durMulti = Math.max(0.01F, unit.getCalculatedStat(dur).getMultiplier());

        // resistance has to REDUCE the ailment - 100% is documented as immunity. clamped at 0 so
        // a stack past 100% can't flip the sign and start banking negative damage, which would
        // then block every future proc on this target until it was out-summed
        float resMulti = Math.max(0, Load.Unit(target).getUnit().getCalculatedStat(res).getReverseMultiplier());

        dmg = dmg * ailment.damageEffectivenessMulti; // make sure this isnt done multiple times
        dmg *= effMulti;
        dmg *= resMulti;

        if (ailment.isDot) {
            // otherwise dots will add whole damage EVERY tick
            float secmulti = 1F / ((float) ailment.durationTicks / 20F);
            dmg *= secmulti;

            dmg *= speed;

            int ticks = ailment.durationTicks;
            ticks /= speed;
            ticks *= durMulti;

            if (ticks < 21) {
                ticks = 21;
            }
            if (!data.dotMap.containsKey(ailment.GUID())) {
                data.dotMap.put(ailment.GUID(), new ArrayList<>());
            }
            data.dotMap.get(ailment.GUID()).add(new DotData(ticks, dmg));
        } else {

            // freeze/electrify bank the damage instead of dealing it, and a proc stat releases it
            // later. the pool decays in onTick - Duration is what slows that bleed off, and it's
            // the only thing Duration can mean for an ailment with no tick count
            data.dmgMap.put(ailment.GUID(), data.dmgMap.getOrDefault(ailment.GUID(), 0F) + dmg);
            data.durMap.put(ailment.GUID(), durMulti);

            if (usesStrengthMeter(ailment)) {

                float max = Load.Unit(target).getUnit().healthData().getValue() + Load.Unit(target).getUnit().magicShieldData().getValue();

                float forFull = max * ailment.percentHealthRequiredForFullStrength;

                if (forFull > 0) {
                    // scale the INCREMENT, then clamp. the stored value has already been through
                    // strength and resistance on every previous hit, so re-applying them to the
                    // running total compounded it, and clamping before the multipliers let the
                    // result escape its own 0..1 range. dmg already carries both multipliers, so
                    // dividing by forFull applies each exactly once
                    float add = dmg / forFull;
                    float strength = MathHelper.clamp(data.strMap.getOrDefault(ailment.GUID(), 0F) + add, 0, 1);
                    data.strMap.put(ailment.GUID(), strength);
                }
            }
        }

        if (usesStrengthMeter(ailment)) {
            applySlow(target, ailment, data);
        }

    }

    private void applySlow(LivingEntity target, Ailment ailment, OneData data) {

        float strength = data.strMap.getOrDefault(ailment.GUID(), 0F);

        if (strength <= 0) {
            return;
        }

        int tier = ailment.getSlowTier(strength);

        int max = Load.Unit(target).getMobRarity().max_slow_from_chill;

        // max_slow_from_chill is datapack data and the pack sets it as high as 100, well
        // past the amplifier where vanilla slowness flips into a speed boost, so cap it
        // in the engine too. the rarity value still applies as a further reduction.
        if (tier > Ailment.MAX_SAFE_SLOW_TIER) {
            tier = Ailment.MAX_SAFE_SLOW_TIER;
        }
        if (tier > max) {
            tier = max;
        }
        if (tier > -1) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, tier));
        }
    }

    // how much of a non dot ailment bleeds off per second. the attacker's Ailment Duration
    // divides it, so more Duration = the banked pool and the chill both last longer
    private float decayPerSecond(String ailmentId, OneData data) {

        Ailment ail = ExileDB.Ailments().get(ailmentId);

        if (ail == null || ail.percentLostEveryXSeconds <= 0) {
            return 0;
        }
        float durMulti = Math.max(0.01F, data.durMap.getOrDefault(ailmentId, 1F));

        return MathHelper.clamp(ail.percentLostEveryXSeconds / durMulti, 0, 1);
    }


    public void onTick(LivingEntity en) {


        for (Map.Entry<UUID, OneData> entry : this.datas.entrySet()) {
            var data = entry.getValue();

            for (Map.Entry<String, List<DotData>> e : data.dotMap.entrySet()) {
                for (DotData d : e.getValue()) {
                    d.ticks -= 1;
                }
            }

            if (en.tickCount % 20 == 0) { // make sure the needed ticks is divisible by 20 for this reason, this is so this isn't calculated every tick
                for (Map.Entry<String, Float> e : data.strMap.entrySet()) {
                    if (e.getValue() > 0) {
                        e.setValue(e.getValue() - (e.getValue() * decayPerSecond(e.getKey(), data)));
                    }
                }
                // the banked burst damage has to bleed off with the meter. without this the pool
                // only ever grows, and a fight long enough to build one turns a single proc into
                // a one shot
                for (Map.Entry<String, Float> e : data.dmgMap.entrySet()) {
                    if (e.getValue() > 0) {
                        e.setValue(e.getValue() - (e.getValue() * decayPerSecond(e.getKey(), data)));
                    }
                }

                // re-stamp the slow from the current meter so it follows the decay down instead
                // of holding the tier it had when the last freeze landed
                for (Map.Entry<String, Float> e : data.strMap.entrySet()) {
                    if (e.getValue() > 0) {
                        Ailment ail = ExileDB.Ailments().get(e.getKey());
                        if (usesStrengthMeter(ail)) {
                            applySlow(en, ail, data);
                        }
                    }
                }

                data.strMap.values().removeIf(x -> x <= MIN_KEPT_STRENGTH);
                data.dmgMap.values().removeIf(x -> x <= MIN_KEPT_DMG);
                data.durMap.keySet().removeIf(x -> !data.strMap.containsKey(x) && !data.dmgMap.containsKey(x));
            }


            if (en.tickCount % 20 == 0) {

                if (!data.dotMap.isEmpty()) {
                    UUID id = entry.getKey();

                    if (id != null) {

                        ServerLevel s = (ServerLevel) en.level();
                        Entity entity = s.getEntity(id);

                        if (entity instanceof LivingEntity caster) {
                            for (Map.Entry<String, List<DotData>> e : data.dotMap.entrySet()) {
                                float dmg = 0;

                                for (DotData d : e.getValue()) {
                                    if (d.ticks > 0) {
                                        dmg += d.dmg;
                                    }
                                }

                                if (dmg > 1) {

                                    Ailment ailment = ExileDB.Ailments().get(e.getKey());
                                    // todo will probably have to tweak this
                                    EventBuilder.ofDamage(caster, en, dmg).setupDamage(AttackType.dot, WeaponTypes.none, PlayStyle.INT).set(x -> {
                                                x.setElement(ailment.element);
                                                x.setisAilmentDamage(ailment);
                                                x.calcTargetEffects = false;
                                                x.calcSourceEffects = false;
                                            }).build()
                                            .Activate();
                                }
                            }
                        }

                    }
                }
                for (List<DotData> l : data.dotMap.values()) {
                    l.removeIf(x -> x.ticks < 1);
                }
                // and drop the now empty lists. leaving them behind kept isEmpty() false forever
                // after the first dot, so the sweep below could never reclaim the entry
                data.dotMap.values().removeIf(List::isEmpty);
            }
        }

        if (en.tickCount % 400 == 0) {
            ServerLevel s = (ServerLevel) en.level();

            this.datas.entrySet().removeIf(x -> {
                if (x.getValue().isEmpty()) {
                    return true;
                }
                Entity entity = s.getEntity(x.getKey());
                return entity instanceof LivingEntity == false;
            });
        }


    }


    public class DotData {
        public float ticks;
        public float dmg;


        public DotData(int ticks, float dmg) {
            this.ticks = ticks;
            this.dmg = dmg;
        }
    }
}
