package com.robertx22.mine_and_slash.mixin_methods;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.auto_item.AutoItem;
import com.robertx22.mine_and_slash.database.data.profession.items.CraftedSoulItem;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.SavedGearSoul;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.items.SoulExtractorItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.RarityStoneItem;
import com.robertx22.orbs_of_crafting.misc.ClickContext;
import com.robertx22.orbs_of_crafting.misc.LocReqContext;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class OnItemInteract {

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
        public abstract Result tryApply(ClickContext ctx);
    }

    static List<ClickFeature> CLICKS = new ArrayList<>();


    public static void register() {


        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {

                if (StackSaving.JEWEL.has(ctx.target)) {
                    ItemStack jewel = ctx.target.copy();
                    var data = StackSaving.JEWEL.loadFrom(jewel);

                    if (data.uniq.isCraftableUnique()) {
                        ItemStack cost = data.uniq.getStackNeededForUpgrade();

                        if (cost.getItem() == ctx.currency.getItem()) {
                            if (ctx.currency.getCount() >= cost.getCount()) {
                                if (data.uniq.getCraftedTier().canUpgradeMore()) {
                                    if (ctx.refuseIfStacked()) {
                                        return new Result(true);
                                    }
                                    data.uniq.upgradeUnique(data);

                                    StackSaving.JEWEL.saveTo(jewel, data);

                                    ctx.consumeCurrency(cost.getCount());
                                    ctx.updateTarget(jewel);

                                    return new Result(true).ding();
                                }
                            }
                        }
                    }
                }

                return new Result(false);
            }
        });


        // todo replace repair stones with datapack currencies
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {
                if (ctx.target.isDamaged() && ctx.currency.getItem() instanceof RarityStoneItem) {

                    if (!StackSaving.GEARS.has(ctx.target) && !StackSaving.OMEN.has(ctx.target)) {
                        ctx.player.sendSystemMessage(Chats.NOT_GEAR_OR_LACKS_SOUL.locName().withStyle(ChatFormatting.RED));
                        return new Result(false);
                    }

                    if (ctx.refuseIfStacked()) {
                        return new Result(true);
                    }

                    RarityStoneItem essence = (RarityStoneItem) ctx.currency.getItem();

                    SoundUtils.playSound(ctx.player, SoundEvents.ANVIL_USE, 1, 1);

                    int repair = essence.getTotalRepair();

                    ItemStack repaired = ctx.target.copy();
                    repaired.setDamageValue(repaired.getDamageValue() - repair);

                    ctx.consumeCurrency(1);
                    ctx.updateTarget(repaired);
                    return new Result(true).ding();
                }
                return new Result(false);
            }
        });

        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {
                if (ctx.currency.getItem() instanceof StatSoulItem || ctx.currency.getItem() instanceof CraftedSoulItem) {
                    StatSoulData data = StackSaving.STAT_SOULS.loadFrom(ctx.currency);
                    if (ctx.currency.getItem() instanceof CraftedSoulItem cs) {
                        data = cs.getSoul(ctx.currency);
                    }
                    if (data != null) {
                        var res = data.canInsertIntoStack(ctx.target);

                        if (res.can) {
                            // this used to silently do nothing on a stack, which read as the click
                            // being ignored. refuseIfStacked tells the player why instead
                            if (ctx.refuseIfStacked()) {
                                return new Result(true);
                            }
                            ItemStack result = data.insertAsUnidentifiedOn(ctx.target.copyWithCount(1), ctx.player);
                            ctx.consumeCurrency(1);
                            ctx.replaceTarget(result);
                            return new Result(true).ding();
                        } else {
                            if (res.answer != null) {
                                ctx.player.sendSystemMessage(res.answer);
                            }
                        }
                    }

                }
                return new Result(false);
            }
        });


        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {
                if (ctx.currency.getItem() instanceof IItemAsCurrency c) {
                    if (!ctx.target.isEmpty()) {
                        if (ctx.refuseIfStacked()) {
                            return new Result(true);
                        }
                        LocReqContext req = new LocReqContext(ctx.player, ctx.target.copyWithCount(1), ctx.currency);

                        var effect = c.currencyEffect(ctx.currency);
                        var can = effect.canItemBeModified(req);
                        if (can.can) {
                            ItemStack result = effect.modifyItem(req).stack.copy();
                            ctx.consumeCurrency(1);
                            ctx.replaceTarget(result); // the currency builds a new item, so the old one goes away
                            return new Result(true);
                        } else {
                            ctx.player.sendSystemMessage(can.answer);
                        }
                    }
                }
                return new Result(false);
            }
        });


        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {
                if (ctx.currency.getItem() instanceof SoulExtractorItem se) {

                    GearItemData gear = StackSaving.GEARS.loadFrom(ctx.target);

                    if (gear != null) {
                        try {

                            if (se.canExtract(gear.getRarity())) {
                                if (ctx.refuseIfStacked()) {
                                    return new Result(true);
                                }
                                StatSoulData soul = new StatSoulData();
                                soul.slot = gear.GetBaseGearType().getGearSlot().GUID();
                                var ex = ExileStack.of(ctx.target);

                                soul.rar = gear.rar;

                                soul.gear = new SavedGearSoul(ex.get(StackKeys.GEAR).get(), ex.get(StackKeys.POTENTIAL).getOrCreate(), ex.get(StackKeys.CUSTOM).getOrCreate());

                                ItemStack soulstack = soul.toStack();

                                SoundUtils.playSound(ctx.player, SoundEvents.EXPERIENCE_ORB_PICKUP);

                                ctx.consumeCurrency(1);
                                ctx.consumeTarget(1);
                                PlayerUtils.giveItem(soulstack, ctx.player);
                                return new Result(true).ding();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                return new Result(false);
            }
        });
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(ClickContext ctx) {
                if (ctx.currency.is(SlashItems.SOUL_CLEANER.get())) {

                    GearItemData gear = StackSaving.GEARS.loadFrom(ctx.target);

                    if (gear != null && !ServerContainer.get().isSoulCleanBanned(ctx.target.getItem())) {
                        if (ctx.refuseIfStacked()) {
                            return new Result(true);
                        }
                        try {
                            ItemStack cleaned = ctx.target.copy();
                            cleaned.getOrCreateTag().remove(StackSaving.GEARS.GUID());

                            ctx.consumeCurrency(1);
                            ctx.updateTarget(cleaned);
                            return new Result(true).ding();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return new Result(false);
            }
        });


        ForgeEvents.registerForgeEvent(ItemStackedOnOtherEvent.class, x -> {
            var player = x.getPlayer();

            if (player.level().isClientSide) {
                return;
            }
            if (x.getClickAction() != ClickAction.SECONDARY) {
                // return;
            }

            ClickContext ctx = ClickContext.of(x);

            if (!ctx.isValid()) {
                return;
            }

            for (ClickFeature click : CLICKS) {
                var result = click.tryApply(ctx);

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

        ForgeEvents.registerForgeEvent(PlayerEvent.ItemCraftedEvent.class, x -> {
            try {
                if (!x.getEntity().level().isClientSide) {
                    ItemStack stack = x.getCrafting();
                    AutoItem.tryInsertTo(stack, x.getEntity());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ForgeEvents.registerForgeEvent(EntityItemPickupEvent.class, x -> {
            try {
                if (!x.getEntity().level().isClientSide) {
                    ItemStack stack = x.getItem().getItem();
                    AutoItem.tryInsertTo(stack, x.getEntity());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
