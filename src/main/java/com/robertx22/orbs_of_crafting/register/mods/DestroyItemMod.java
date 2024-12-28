package com.robertx22.orbs_of_crafting.register.mods;

import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.VanillaItemModSers;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class DestroyItemMod extends ItemModification {

    public DestroyItemMod(String id) {
        super(VanillaItemModSers.DESTROY_ITEM, id);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return DestroyItemMod.class;
    }


    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {

    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.BAD;
    }

    @Override
    public void applyMod(Player p, StackHolder stack, ItemModificationResult r) {
        stack.stack = Items.COAL.getDefaultInstance();
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
