package com.robertx22.orbs_of_crafting.register.mods;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationSers;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class DestroyItemMod extends ItemModification {

    public DestroyItemMod(String id) {
        super(ItemModificationSers.DESTROY_ITEM, id);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return DestroyItemMod.class;
    }


    @Override
    public void applyINTERNAL(ExileStack stack, ItemModificationResult r) {

    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.BAD;
    }

    @Override
    public void applyMod(Player p, ExileStack stack, ItemModificationResult r) {
        stack.setStack(Items.COAL.getDefaultInstance());
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public String locDescForLangFile() {
        return "DESTROYS the Item!";
    }
}
