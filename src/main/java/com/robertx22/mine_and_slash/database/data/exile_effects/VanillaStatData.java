package com.robertx22.mine_and_slash.database.data.exile_effects;

import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class VanillaStatData {

    float val;
    String uuid;
    String id;
    ModType type;

    public static VanillaStatData create(Attribute attri, float val, ModType type, UUID uuid) {
        VanillaStatData data = new VanillaStatData();
        data.id = BuiltInRegistries.ATTRIBUTE.getKey(attri)
                .toString();
        data.uuid = uuid.toString();
        data.type = type;
        data.val = val;
        return data;
    }

    public Attribute getAttribute() {
        return BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(id));
    }

    public void applyVanillaStats(LivingEntity en, int stacks) {
        applyVanillaStats(en, stacks, 1F);
    }

    /**
     * @param strMulti the holder's effect strength multiplier (ExileEffectInstanceData.str_multi).
     *                 The mns stats of an effect already scale by it, these vanilla modifiers have
     *                 to as well or a target resistant to an effect still eats its whole attribute
     *                 lockdown.
     */
    public void applyVanillaStats(LivingEntity en, int stacks, float strMulti) {

        float amount = val * stacks * strMulti;

        // a MULTIPLY_TOTAL past -1 is already a full shutdown (the attribute floors at its own
        // minimum, 0 for the speed/damage ones every effect here uses), so clamping loses nothing.
        // it has to happen AFTER strMulti, or an attacker with +effect strength scales the -10 the
        // cc effects use straight back past -1, and two negative multipliers on one attribute
        // multiply back into a large POSITIVE one instead of stacking their reductions.
        if (type == ModType.MORE) {
            amount = Math.max(amount, -1F);
        }

        AttributeModifier mod = new AttributeModifier(UUID.fromString(uuid), "", amount, type.operation);
        Attribute attri = getAttribute();

        this.removeVanillaStats(en);

        if (en.getAttribute(attri) != null) {
            if (!en.getAttribute(attri)
                    .hasModifier(mod)) {
                en.getAttribute(attri)
                        .addTransientModifier(mod);
            }
        }

    }

    public void removeVanillaStats(LivingEntity en) {
        AttributeModifier mod = new AttributeModifier(UUID.fromString(uuid), "", val, type.operation);
        Attribute attri = getAttribute();

        if (en.getAttribute(attri) != null) {
            if (en.getAttribute(attri)
                    .hasModifier(mod)) {
                en.getAttribute(attri)
                        .removeModifier(mod);
            }
        }
    }
}
