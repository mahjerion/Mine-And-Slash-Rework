package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.CycleRunedSocketFilter;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.vanilla_mc.packets.InvGuiPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

// the salvage rarity grid, plus the runed socket threshold that the grid has no cell for
public class SalvageConfigScreen extends InvGuiScreen {

    static int BUTTON_SIZE_X = 110;
    static int BUTTON_SIZE_Y = 16;

    Button socketButton = null;
    int shownThreshold = -1;

    public SalvageConfigScreen() {
        super(GuiInventoryGrids.ofSalvageConfig());
    }

    @Override
    protected void init() {
        super.init();

        socketButton = null;
        shownThreshold = -1;

        // nothing to filter on if no rarity rolls enough sockets for a threshold to mean anything
        if (GearRarity.maxRunedSockets() < 2) {
            return;
        }

        socketButton = Button.builder(socketLabel(), b -> Packets.sendToServer(new InvGuiPacket(new GuiItemData(new CycleRunedSocketFilter()))))
                .bounds(guiLeft + sizeX / 2 - BUTTON_SIZE_X / 2, guiTop + sizeY + 2, BUTTON_SIZE_X, BUTTON_SIZE_Y)
                .build();

        publicAddButton(socketButton);
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        // the value comes back from the server a tick after the click, so poll it rather than
        // assuming the click landed. rebuilding the label and tooltip is only worth it when it moved
        if (socketButton != null && shownThreshold != threshold()) {
            shownThreshold = threshold();
            socketButton.setMessage(socketLabel());
            socketButton.setTooltip(Tooltip.create(socketTooltip()));
        }

        super.render(gui, x, y, ticks);
    }

    private int threshold() {
        return Load.player(mc.player).config.salvage.getRunedMinSockets();
    }

    private Component socketLabel() {
        int min = threshold();

        Component value = min < 1
                ? Gui.SALVAGE_RUNED_SOCKETS_OFF.locName()
                : Component.literal(min + "+");

        return Gui.SALVAGE_RUNED_SOCKETS.locName(value);
    }

    private Component socketTooltip() {
        int min = threshold();

        Component tip = min < 1
                ? Gui.SALVAGE_RUNED_SOCKETS_TIP_OFF.locName().withStyle(ChatFormatting.GRAY)
                : Gui.SALVAGE_RUNED_SOCKETS_TIP.locName(Component.literal("" + min)).withStyle(ChatFormatting.GOLD);

        return Component.empty()
                .append(tip)
                .append(Component.literal("\n"))
                .append(Gui.SALVAGE_RUNED_SOCKETS_TIP2.locName().withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Gui.SALVAGE_RUNED_SOCKETS_CYCLE.locName(Component.literal("" + GearRarity.maxRunedSockets())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
