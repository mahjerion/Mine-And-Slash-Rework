package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.tags.imp.EffectTag;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ExileEffectUtils {

    public static int countEffectsWithTag(LivingEntity en, EffectTag tag) {

        int amount = 0;

        for (String k : Load.Unit(en)
                .getStatusEffectsData().exileMap.keySet()) {
            ExileEffect eff = ExileDB.ExileEffects()
                    .get(k);
            if (eff.hasTag(tag)) {
                amount++;
            }
        }

        return amount;

    }

    public static Vec3 EnsureNotNaN(Vec3 v) {
        if (Double.isNaN(v.x)) {
            v = new Vec3(0, v.y, v.z);
        }

        if (Double.isNaN(v.y)) {
            v = new Vec3(v.x, 0, v.z);
        }

        if (Double.isNaN(v.z)) {
            v = new Vec3(v.x, v.y, 0);
        }

        return v;
    }
}
