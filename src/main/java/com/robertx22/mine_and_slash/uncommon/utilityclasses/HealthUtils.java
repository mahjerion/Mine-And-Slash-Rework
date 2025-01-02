package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class HealthUtils {


    static AttributeModifier getHeartsAttributeMod(float num) {
        return new AttributeModifier(
                UUID.fromString("3fb10485-f309-128f-afc6-a55b0d6cf4c1"),
                BuiltInRegistries.ATTRIBUTE.getKey(Attributes.MAX_HEALTH).toString(),
                num,
                AttributeModifier.Operation.ADDITION
        );
    }

    public static void addHearts(LivingEntity en) {

        if (CompatConfig.get().HEALTH_SYSTEM.get().usesVanillaHearts()) {

            var curmax = en.getMaxHealth();

            var data = Load.Unit(en);

            int cur = (int) data.getUnit().healthData().getValue();

            if (data.lastHealth != cur) {
                data.lastHealth = cur;
            }

            var mod = getHeartsAttributeMod(cur);

            var at = en.getAttribute(Attributes.MAX_HEALTH);

            if (en.getAttributes().hasModifier(Attributes.MAX_HEALTH, mod.getId())) {
                at.removeModifier(mod.getId());
            }
            data.heartsWithoutMnsHealth = (int) en.getMaxHealth();

            at.addPermanentModifier(mod);

            var aftermax = en.getMaxHealth();

            if (aftermax > curmax) {
                float toheal = aftermax - curmax;
                en.heal(toheal); // todo maybe use sethealth here instead
            }
        } else {
            var mod = getHeartsAttributeMod(0);
            var at = en.getAttribute(Attributes.MAX_HEALTH);
            if (en.getAttributes().hasModifier(Attributes.MAX_HEALTH, mod.getId())) {
                at.removeModifier(mod.getId());
            }
        }
    }

    public static void heal(LivingEntity en, float heal) {
        en.heal(heal);
    }


    public static float realToVanilla(LivingEntity en, float dmg) {
        if (CompatConfig.get().HEALTH_SYSTEM.get().usesVanillaHearts()) {
            return dmg;
        }
        float multi = dmg / getMaxHealth(en);
        float max = en.getMaxHealth();
        float total = multi * max;
        return total;
    }


    public static float getMaxHealth(LivingEntity en) {

        if (CompatConfig.get().HEALTH_SYSTEM.get().usesVanillaHearts()) {
            return en.getMaxHealth();
        }

        EntityData data = Load.Unit(en);

        if (en.level().isClientSide) {
            return data.getSyncedMaxHealth(); // for client, health needs to be synced
        }
        try {
            return data.getUnit().healthData().getValue();
        } catch (Exception e) {
            return 1;
        }

    }

    public static int getCurrentHealth(LivingEntity entity) {
        if (CompatConfig.get().HEALTH_SYSTEM.get().usesVanillaHearts()) {
            return (int) entity.getHealth();
        }

        float multi = entity.getHealth() / entity.getMaxHealth();
        float max = getMaxHealth(entity);
        return (int) (max * multi);

    }

}
