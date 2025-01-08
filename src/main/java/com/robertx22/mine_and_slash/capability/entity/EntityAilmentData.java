package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailment;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.AilmentSpeed;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailments;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentDuration;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentEffectStat;
import com.robertx22.mine_and_slash.database.data.stats.types.ailment.AilmentResistance;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
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

    // todo freeze and electrify


    public HashMap<UUID, OneData> datas = new HashMap<UUID, OneData>();


    public static class OneData {

        public HashMap<String, List<DotData>> dotMap = new HashMap<String, List<DotData>>();
        public HashMap<String, Float> strMap = new HashMap<String, Float>();
        public HashMap<String, Float> dmgMap = new HashMap<String, Float>();

        public boolean isEmpty() {
            return dotMap.isEmpty() && strMap.isEmpty() && dmgMap.isEmpty();
        }

    }


    // todo have to call this with stats or ways that do it. I'll have stats that have chance to do it etc.
    public void shatterAccumulated(LivingEntity caster, LivingEntity target, Ailment ailment, Spell spell) {

        if (!datas.containsKey(caster.getUUID())) {
            datas.put(caster.getUUID(), new OneData());
        }
        var data = datas.get(caster.getUUID());

        float dmg = data.dmgMap.getOrDefault(ailment.GUID(), 0F);
        data.dmgMap.put(ailment.GUID(), 0F);

        if (dmg > 0) {
            var b = EventBuilder.ofDamage(caster, target, (int) dmg).setupDamage(AttackType.dot, WeaponTypes.none, PlayStyle.INT).set(x -> {
                x.calcSourceEffects = false;
                x.calcTargetEffects = false;
                x.setElement(ailment.element);
                x.setisAilmentDamage(ailment);

                if (spell != null) {
                    x.data.setString(EventData.WEAPON_TYPE, spell.getWeapon(caster).id);
                    x.data.setString(EventData.SPELL, spell.GUID());
                }
            });

            b.build().Activate();
        }
    }

    public void onAilmentCausingDamage(LivingEntity caster, LivingEntity target, Ailment ailment, float dmg) {

        if (!datas.containsKey(caster.getUUID())) {
            datas.put(caster.getUUID(), new OneData());
        }
        var data = datas.get(caster.getUUID());


        AilmentDuration dur = new AilmentDuration(ailment);
        AilmentResistance res = new AilmentResistance(ailment);
        AilmentEffectStat eff = new AilmentEffectStat(ailment);

        float speed = Load.Unit(caster).getUnit().getCalculatedStat(AilmentSpeed.INSTANCE).getMultiplier();


        dmg = dmg * ailment.damageEffectivenessMulti; // make sure this isnt done multiple times
        dmg *= Load.Unit(caster).getUnit().getCalculatedStat(eff).getMultiplier();

        var resist = Load.Unit(target).getUnit().getCalculatedStat(res);
        float resmulti = resist.getReverseMultiplier();
        dmg *= resmulti;

        if (ailment.isDot) {
            // otherwise dots will add whole damage EVERY tick
            float secmulti = 1F / ((float) ailment.durationTicks / 20F);
            dmg *= secmulti;
        }


        if (ailment.isDot) {
            dmg *= speed;


            int ticks = ailment.durationTicks;
            var stat = Load.Unit(caster).getUnit().getCalculatedStat(AilmentSpeed.INSTANCE).getMultiplier();
            ticks /= stat;
            ticks *= Load.Unit(caster).getUnit().getCalculatedStat(dur).getMultiplier();

            if (ticks < 21) {
                ticks = 21;
            }
            if (!data.dotMap.containsKey(ailment.GUID())) {
                data.dotMap.put(ailment.GUID(), new ArrayList<>());
            }
            data.dotMap.get(ailment.GUID()).add(new DotData(ticks, dmg));
        } else {


            float strength = 0;

            float max = Load.Unit(target).getUnit().healthData().getValue() + Load.Unit(target).getUnit().magicShieldData().getValue();

            float forFull = max * ailment.percentHealthRequiredForFullStrength;

            if (!data.strMap.containsKey(ailment.GUID())) {
                data.strMap.put(ailment.GUID(), 0F);
            }
            if (!data.dmgMap.containsKey(ailment.GUID())) {
                data.dmgMap.put(ailment.GUID(), 0F);
            }
            data.dmgMap.put(ailment.GUID(), data.dmgMap.get(ailment.GUID()) + dmg);


            float add = dmg / forFull;
            strength = MathHelper.clamp(data.strMap.get(ailment.GUID()) + (add), 0, 1);
            strength *= Load.Unit(caster).getUnit().getCalculatedStat(eff).getMultiplier();
            strength *= Load.Unit(target).getUnit().getCalculatedStat(res).getMultiplier();

            data.strMap.put(ailment.GUID(), strength);

        }

        if (ailment.GUID().equals(Ailments.FREEZE.GUID())) {
            float freeze = data.strMap.getOrDefault(Ailments.FREEZE.GUID(), 0F);
            if (freeze > 0) {

                int tier = Ailments.FREEZE.getSlowTier(freeze);

                int max = Load.Unit(target).getMobRarity().max_slow_from_chill;

                if (tier > max) {
                    tier = max;
                }
                if (tier > -1) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, tier));
                }
            }
        }

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
                        Ailment ail = ExileDB.Ailments().get(e.getKey());
                        e.setValue(e.getValue() - (e.getValue() * ail.percentLostEveryXSeconds));
                    }
                }
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
