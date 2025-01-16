package com.robertx22.addons.orbs_of_crafting.currency.base;

import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.gui.texts.textblocks.WorksOnBlock;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.orbs_of_crafting.main.LocReqContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.List;


public abstract class GearCurrency extends CodeCurrency {

    @Override
    public WorksOnBlock.ItemType usedOn() {
        return WorksOnBlock.ItemType.GEAR;
    }

    public abstract List<GearOutcome> getOutcomes();

    public abstract int getPotentialLoss();

    public boolean spendsGearPotential() {
        return getPotentialLoss() > 0;
    }

    @Override
    public void internalModifyMethod(LocReqContext ctx) {
        //GearItemData data = ctx.stack.GEAR.get();

        GearOutcome outcome = getOutcome();

        ExileStack ex = ExileStack.of(ctx.stack);

        StackKeys.POTENTIAL.get(ex).edit(x -> x.spend(getPotentialLoss()));

        ctx.stack = ex.getStack();


        Player player = ctx.player;
        if (outcome.getOutcomeType() == OutcomeType.GOOD) {
            SoundUtils.ding(player.level(), player.blockPosition());
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ANVIL_USE, 1, 1);
        } else {
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.GLASS_BREAK, 1, 1);
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.VILLAGER_NO, 1, 1);
        }
        outcome.modify(ctx);
    }

    private GearOutcome getOutcome() {
        return RandomUtils.weightedRandom(getOutcomes());
    }

    @Override
    public ExplainedResult canItemBeModified(LocReqContext context) {
        ExileStack ex = ExileStack.of(context.stack);

        GearItemData data = ex.get(StackKeys.GEAR).get();


        if (data == null) {
            return ExplainedResult.failure(Chats.NOT_GEAR.locName());
        }

        if (ex.isCorrupted() && this.spendsGearPotential()) {
            return ExplainedResult.failure(Chats.CORRUPT_CANT_BE_MODIFIED.locName());
        }

        if (!ex.get(StackKeys.POTENTIAL).has() || ex.get(StackKeys.POTENTIAL).get().potential < 1) {
            if (this.spendsGearPotential()) {
                return ExplainedResult.failure(Chats.GEAR_NO_POTENTIAL.locName());
            }
        }

        var can = canBeModified(context);
        if (!can.can) {
            return can;
        }
        return super.canItemBeModified(context);
    }

    public abstract ExplainedResult canBeModified(LocReqContext data);


}
