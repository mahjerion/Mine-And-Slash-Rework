package com.robertx22.mine_and_slash.gui.screens.map_device;

import com.robertx22.addons.map_device.MapDeviceActionPacket;
import com.robertx22.addons.map_device.MapDeviceClientState;
import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.gui.inv_gui.GuiInventoryGrids;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * One slot of the map device: the map, or one of the four relics. Same behaviour as the mercenary
 * screen's {@code MercSlotButton}: left click opens the picker of legal items from your inventory,
 * right click hands the item back. The cell art belongs to the screen (map_slot / relic_slots textures);
 * this only draws the item and handles the clicks.
 */
public class MapDeviceSlotButton extends AbstractButton {

    private final MapDeviceScreen screen;
    private final int slot;

    public MapDeviceSlotButton(MapDeviceScreen screen, int slot, int x, int y) {
        super(x, y, MapDeviceGui.SLOT_SIZE, MapDeviceGui.SLOT_SIZE, Component.empty());
        this.screen = screen;
        this.slot = slot;
    }

    private ItemStack getStack() {
        return screen.getState().getStack(slot);
    }

    private boolean isMapSlot() {
        return slot == IMapDeviceBlockEntity.MAP_SLOT;
    }

    @Override
    public void onPress() {
        MapDeviceClientState state = screen.getState();
        // cancelling the pick returns to the device screen, matching where the equip action leaves you
        Minecraft.getInstance().setScreen(new InvGuiScreen(
                GuiInventoryGrids.ofMapDeviceSlotChoices(ClientOnly.getPlayer(), state, slot),
                screen.getTitleText(), () -> ClientOnly.setScreen(new MapDeviceScreen(MapDeviceClientState.last == null ? state : MapDeviceClientState.last))));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1 && this.isMouseOver(mx, my) && this.active) {
            // right click takes the item back rather than opening the picker
            if (!getStack().isEmpty()) {
                Packets.sendToServer(MapDeviceActionPacket.clearSlot(screen.getState().pos, slot));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        gui.setColor(1F, 1F, 1F, 1F);
        // the cell art is part of the screen's map_slot / relic_slots textures, drawn underneath

        ItemStack stack = getStack();
        if (!stack.isEmpty()) {
            // the button starts on the cell's shadow line, so the 16 icon sits one pixel further in.
            // deliberately NOT RenderUtils.renderStack, see MercSlotButton
            int ix = getX() + MapDeviceGui.SLOT_INSET;
            int iy = getY() + MapDeviceGui.SLOT_INSET;
            gui.renderItem(stack, ix, iy);
            gui.renderItemDecorations(Minecraft.getInstance().font, stack, ix, iy);
        }

        // rebuilt every frame while hovered so shift-expanded tooltips follow the key
        if (isHovered()) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(stack))));
        }
    }

    private List<Component> buildTooltip(ItemStack stack) {
        List<Component> list = new ArrayList<>();

        if (stack.isEmpty()) {
            list.add((isMapSlot() ? Words.MAP_DEVICE_SLOT_MAP_HINT : Words.MAP_DEVICE_SLOT_RELIC_HINT).locName().withStyle(ChatFormatting.GRAY));
            return list;
        }

        list.addAll(stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.NORMAL));
        list.add(Component.empty());
        list.add(Words.MAP_DEVICE_RIGHT_CLICK_RETURN.locName().withStyle(ChatFormatting.YELLOW));
        return list;
    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
