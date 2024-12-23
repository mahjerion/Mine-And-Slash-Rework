package com.robertx22.orbs_of_crafting.register.reqs.vanilla;

import com.google.gson.JsonObject;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationSers;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;


public class VanillaItemMod extends ItemModification {

    transient String desc;

    public JsonObject vanillaFunction = null;

    public VanillaItemMod(String id, String desc, LootItemFunction function) {
        super(ItemModificationSers.VANILLA, id);
        this.desc = desc;
        this.vanillaFunction = (JsonObject) LootDataType.MODIFIER.parser().toJsonTree(function);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return VanillaItemMod.class;
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.NEUTRAL;
    }

    @Override
    public void applyINTERNAL(ExileStack stack, ItemModificationResult r) {
    }

    @Override
    public void applyMod(Player p, ExileStack stack, ItemModificationResult r) {
        var opt = LootDataType.MODIFIER.deserialize(new ResourceLocation(""), vanillaFunction, p.getServer().getResourceManager());

        opt.ifPresent(x -> {
            LootParams lootparams = (new LootParams.Builder((ServerLevel) p.level()))
                    .withParameter(LootContextParams.THIS_ENTITY, p)
                    .withParameter(LootContextParams.ORIGIN, p.position())
                    .create(LootContextParamSet.builder().build());

            var ctx = new LootContext.Builder(lootparams).create(SlashRef.id("vanilla_function"));


            var res = x.apply(stack.getStack(), ctx);
            stack.setStack(res);
        });

    }


    @Override
    public MutableComponent getDescWithParams() {
        return getDescParams();
    }

    @Override
    public String locDescForLangFile() {
        return desc;
    }
}
