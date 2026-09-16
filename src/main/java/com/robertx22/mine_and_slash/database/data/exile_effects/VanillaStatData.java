package com.robertx22.mine_and_slash.database.data.exile_effects;

import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

    public UUID getUUID() {
        return UUID.fromString(uuid);
    }

    /**
     * The modifier amount this stat should currently be worth on its holder.
     *
     * @param strMulti the holder's effect strength multiplier (ExileEffectInstanceData.str_multi).
     *                 The mns stats of an effect already scale by it, these vanilla modifiers have
     *                 to as well or a target resistant to an effect still eats its whole attribute
     *                 lockdown.
     */
    public float getTargetAmount(int stacks, float strMulti) {

        float amount = val * stacks * strMulti;

        // a MULTIPLY_TOTAL past -1 is already a full shutdown (the attribute floors at its own
        // minimum, 0 for the speed/damage ones every effect here uses), so clamping loses nothing.
        // it has to happen AFTER strMulti, or an attacker with +effect strength scales the -10 the
        // cc effects use straight back past -1, and two negative multipliers on one attribute
        // multiply back into a large POSITIVE one instead of stacking their reductions.
        if (type == ModType.MORE) {
            amount = Math.max(amount, -1F);
        }

        return amount;
    }

    public void applyVanillaStats(LivingEntity en, int stacks) {
        applyVanillaStats(en, stacks, 1F);
    }

    public void applyVanillaStats(LivingEntity en, int stacks, float strMulti) {

        AttributeInstance in = en.getAttribute(getAttribute());

        if (in == null) {
            return; // the entity has no such attribute at all - most mobs have no generic.attack_speed
        }

        // remove first: vanilla keys a modifier by uuid, so re-adding one that is already there is
        // a no-op and the stale amount would survive a stack or strength change
        if (in.getModifier(getUUID()) != null) {
            in.removeModifier(getUUID());
        }

        in.addTransientModifier(new AttributeModifier(getUUID(), "", getTargetAmount(stacks, strMulti), type.operation));
    }

    /**
     * Same as {@link #applyVanillaStats} but leaves the attribute instance untouched when the
     * modifier already reads exactly what it should. The reconcile pass re-asserts every active
     * effect's modifiers on a fixed cadence, and without this that would churn a remove + add (and
     * the setDirty/resync each one costs) every second per effect per entity for no change at all.
     *
     * @return true if the entity's modifier was actually added or corrected
     */
    public boolean applyIfDifferent(LivingEntity en, int stacks, float strMulti) {

        AttributeInstance in = en.getAttribute(getAttribute());

        if (in == null) {
            return false;
        }

        float target = getTargetAmount(stacks, strMulti);
        AttributeModifier current = in.getModifier(getUUID());

        if (current != null && current.getOperation() == type.operation && current.getAmount() == target) {
            return false;
        }

        if (current != null) {
            in.removeModifier(getUUID());
        }

        in.addTransientModifier(new AttributeModifier(getUUID(), "", target, type.operation));

        return true;
    }

    public void removeVanillaStats(LivingEntity en) {
        AttributeInstance in = en.getAttribute(getAttribute());

        if (in != null && in.getModifier(getUUID()) != null) {
            in.removeModifier(getUUID());
        }
    }
}
