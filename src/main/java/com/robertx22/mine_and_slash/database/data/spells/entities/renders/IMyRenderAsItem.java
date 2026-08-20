package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public interface IMyRenderAsItem {
    ItemStack getItem();

    /**
     * Extra offset applied at render time, so an entity whose position is derived from something
     * else (an orbiting projectile following its caster) can place itself exactly for the current
     * partial tick instead of being lerped between its last two ticked positions.
     */
    default Vec3 getSmoothRenderOffset(float partialTicks) {
        return Vec3.ZERO;
    }
}
