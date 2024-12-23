package com.robertx22.orbs_of_crafting.register;

import com.robertx22.orbs_of_crafting.keys.ExileKey;
import com.robertx22.orbs_of_crafting.keys.ExileKeyHolder;
import com.robertx22.orbs_of_crafting.keys.KeyInfo;
import com.robertx22.orbs_of_crafting.register.mods.DestroyItemMod;
import com.robertx22.orbs_of_crafting.register.mods.DoNothingItemMod;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.reqs.vanilla.VanillaItemMod;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class Modifications extends ExileKeyHolder<ItemModification> {
    public static Modifications INSTANCE = new Modifications();
    public ExileKey<ItemModification, KeyInfo> DESTROY_ITEM = ExileKey.ofId(this, "destroy_item", x -> new DestroyItemMod(x.GUID()));
    public ExileKey<ItemModification, KeyInfo> DO_NOTHING = ExileKey.ofId(this, "do_nothing", x -> new DoNothingItemMod(x.GUID()));

    public ExileKey<ItemModification, KeyInfo> ENCHANT_30_LEVELS = ExileKey.ofId(this, "enchant_with_30_levels", x -> {
        var fun = EnchantWithLevelsFunction.enchantWithLevels(ConstantValue.exactly(30.0F)).allowTreasure().build();
        return new VanillaItemMod(x.GUID(), "Applies Enchantments worth 30 Levels", fun);
    });
    public ExileKey<ItemModification, KeyInfo> ENCHANT_20_LEVELS = ExileKey.ofId(this, "enchant_with_20_levels", x -> {
        var fun = EnchantWithLevelsFunction.enchantWithLevels(ConstantValue.exactly(20)).allowTreasure().build();
        return new VanillaItemMod(x.GUID(), "Applies Enchantments worth 20 Levels", fun);
    });
    public ExileKey<ItemModification, KeyInfo> ENCHANT_10_LEVELS = ExileKey.ofId(this, "enchant_with_10_levels", x -> {
        var fun = EnchantWithLevelsFunction.enchantWithLevels(ConstantValue.exactly(10)).allowTreasure().build();
        return new VanillaItemMod(x.GUID(), "Applies Enchantments worth 10 Levels", fun);
    });

    @Override
    public void loadClass() {

    }
}
