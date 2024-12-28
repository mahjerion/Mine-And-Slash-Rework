package com.robertx22.orbs_of_crafting.main;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.ExileCurrency;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;

import java.util.ArrayList;
import java.util.List;

// todo

public class OnClick {
    static List<ClickFeature> CLICKS = new ArrayList<>();

    private static class Result {

        public boolean can;

        public Result(boolean can) {
            this.can = can;
        }

        private boolean doDing = false;

        public Result ding() {
            this.doDing = true;
            return this;
        }
    }

    private abstract static class ClickFeature {
        public abstract Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot);
    }

    public static void register() {    // new datapack currencies
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                var opt = ExileCurrency.get(currency);

                if (opt.isPresent()) {
                    LocReqContext ctx = new LocReqContext(player, craftedStack, currency);

                    var cur = opt.get();
                    if (!craftedStack.isEmpty()) {
                        var can = cur.canItemBeModified(ctx);

                        if (can.can) {
                            var result = cur.modifyItem(ctx);

                            if (result.resultEnum != ModifyResult.SUCCESS) {
                                player.sendSystemMessage(result.result.answer);
                                return new Result(false);
                            }

                            craftedStack.shrink(1); // seems the currency creates a copy of a new item, so we delete the old one
                            currency.shrink(1);
                            // PlayerUtils.giveItem(result, player);
                            slot.set(result.stack.copy());

                            if (result.outcome == ItemModification.OutcomeType.BAD) {
                                SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.GLASS_BREAK, 1, 1);
                                return new Result(true);
                            } else {
                                return new Result(true).ding();
                            }
                        } else {
                            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.VILLAGER_NO, 1, 1);
                            player.sendSystemMessage(can.answer);
                        }
                    }

                }
                return new Result(false);
            }
        });

        ForgeEvents.registerForgeEvent(ItemStackedOnOtherEvent.class, x -> {
            Player player = x.getPlayer();

            if (player.level().isClientSide) {
                return;
            }
            if (x.getClickAction() != ClickAction.SECONDARY) {
                // return;
            }

            ItemStack currency = x.getStackedOnItem();
            ItemStack craftedStack = x.getCarriedItem();


            for (ClickFeature click : CLICKS) {
                var result = click.tryApply(player, craftedStack, currency, x.getSlot());

                if (result.doDing) {
                    SoundUtils.ding(player.level(), player.blockPosition());
                    SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ANVIL_USE, 1, 1);
                }

                if (result.can) {
                    x.setCanceled(true);
                    break;
                }
            }


        });

    }
}
