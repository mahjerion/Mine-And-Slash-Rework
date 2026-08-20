package com.robertx22.mine_and_slash.uncommon.interfaces.data_items;

import com.robertx22.library_of_exile.utils.AllItemStackSavers;
import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ISalvagable {

    List<ItemStack> getSalvageResult(ExileStack stack);

    public ToggleAutoSalvageRarity.SalvageType getSalvageType();

    default String getSalvageConfigurationId() {
        return null;
    }

    // a more specific id than getSalvageConfigurationId(), configured per rarity.
    // gear returns its BaseGearType GUID, so players can filter staves apart from swords,
    // or cloth apart from plate, and datapacked gear types work with zero extra code.
    default String getSubFilterId() {
        return null;
    }

    // sockets on this item, or -1 when the runed socket filter does not apply to it
    default int getSocketFilterCount() {
        return -1;
    }

    // the map layout this item will run, or null when it isn't a map, predates layouts, or is exempt.
    // takes the stack because the layout lives in dungeon_realm's nbt, not in MapItemData's own fields
    default String getMapLayoutId(ExileStack stack) {
        return null;
    }

  
    default boolean isSalvagable(ExileStack stack) {
        return !stack.get(StackKeys.CUSTOM).getOrCreate().data.get(CustomItemData.KEYS.SALVAGING_DISABLED);
    }

    static ISalvagable load(ItemStack stack) {

        for (ItemstackDataSaver<? extends ISalvagable> saver : AllItemStackSavers.getAllOfClass(ISalvagable.class)) {
            ISalvagable data = saver.loadFrom(stack);
            if (data != null) {
                return data;
            }
        }
        return null;
    }
}
