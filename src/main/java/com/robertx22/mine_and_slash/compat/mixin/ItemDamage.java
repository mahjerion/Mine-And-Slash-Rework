package com.robertx22.mine_and_slash.compat.mixin;

import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ItemDamage {

    public static <T extends LivingEntity> void hurtAndBreak(ItemStack stack, int pAmount, T pEntity, Consumer<T> pOnBroken) {
        int max = CompatConfig.get().ITEM_DAMAGE_CAP_PER_HIT.get();

        if (pAmount > max) {
            pAmount = max;
        }


        if (!pEntity.level().isClientSide && (!(pEntity instanceof Player) || !((Player) pEntity).getAbilities().instabuild)) {
            if (stack.isDamageableItem()) {
                pAmount = stack.getItem().damageItem(stack, pAmount, pEntity, pOnBroken);
                if (stack.hurt(pAmount, pEntity.getRandom(), pEntity instanceof ServerPlayer ? (ServerPlayer) pEntity : null)) {
                    pOnBroken.accept(pEntity);
                    Item item = stack.getItem();
                    stack.shrink(1);
                    if (pEntity instanceof Player) {
                        ((Player) pEntity).awardStat(Stats.ITEM_BROKEN.get(item));
                    }

                    stack.setDamageValue(0);
                }

            }
        }
    }
}
