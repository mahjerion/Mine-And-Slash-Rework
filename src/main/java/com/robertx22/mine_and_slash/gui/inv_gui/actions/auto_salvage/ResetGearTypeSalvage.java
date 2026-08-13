package com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage;

import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// row header of the per gear type filter grid, names the gear type and resets its whole row
public class ResetGearTypeSalvage extends GuiAction {

    public BaseGearType gearType;

    public ResetGearTypeSalvage(BaseGearType gearType) {
        this.gearType = gearType;
    }

    @Override
    public List<Component> getTooltip(Player p) {
        var list = new ArrayList<Component>();

        list.add(gearType.locName().withStyle(ChatFormatting.GOLD));

        int overridden = Load.player(p).config.salvage.getConfiguredRaritiesForGearType(gearType.GUID()).size();

        list.add(Gui.SALVAGE_GT_OVERRIDE_COUNT.locName(overridden, ExileDB.GearRarities().getList().size())
                .withStyle(ChatFormatting.GRAY));
        list.add(Gui.SALVAGE_GT_RESET.locName().withStyle(ChatFormatting.DARK_GRAY));

        return list;
    }

    @Override
    public ItemStack getItemStackIcon() {
        // datapacked gear types get a sensible icon for free, no png needed
        if (!gearType.possible_items.isEmpty()) {
            return new ItemStack(gearType.possible_items.get(0).getItem());
        }
        return new ItemStack(gearType.family().craftItem.get());
    }

    @Override
    public void doAction(Player p, Object data) {
        Load.player(p).config.salvage.clearGearTypeSalvageConfig(gearType.GUID());
        Load.player(p).playerDataSync.setDirtyAndSync(p);
    }

    @Override
    public void clientAction(Player p, Object obj) {

    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {

    }

    @Override
    public Object loadExtraData(FriendlyByteBuf buf) {
        return null;
    }

    @Override
    public String GUID() {
        return "gt_sal_reset_" + gearType.GUID();
    }
}
