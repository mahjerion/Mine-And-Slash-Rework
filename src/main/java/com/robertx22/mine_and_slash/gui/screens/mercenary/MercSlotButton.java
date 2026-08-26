package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.gui.inv_gui.GuiInventoryGrids;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary.MercEquipAction;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * One item slot on the mercenary screen - a piece of gear, a support gem or an aura gem.
 * <p>
 * Left click opens a grid of the legal items in your inventory for this slot; right click hands
 * whatever is in it back. That is the "click the slot and it brings up legal entries" option from the
 * design, chosen so the screen stays at the mocked up 256x256 with no inventory row bolted underneath.
 */
public class MercSlotButton extends AbstractButton {

    private final MercenaryScreen screen;
    private final MercenarySlotType type;
    private final int index;


    public MercSlotButton(MercenaryScreen screen, MercenarySlotType type, int index, int x, int y, int size) {
        super(x, y, size, size, Component.empty());
        this.screen = screen;
        this.type = type;
        this.index = index;
    }

    private ItemStack getStack() {
        MercenaryData data = screen.getMercData();
        if (data == null) {
            return ItemStack.EMPTY;
        }
        var inv = type.inventoryOf(data);
        return index < inv.getContainerSize() ? inv.getItem(index) : ItemStack.EMPTY;
    }

    /** which of the 4 active skills this support socket sits under */
    private int skillSlot() {
        return index / MercenaryClass.SUPPORTS_PER_SKILL;
    }

    /** support slots past what the mercenary's level has unlocked are drawn shut */
    private boolean isLocked() {
        if (type != MercenarySlotType.SUPPORT) {
            return false;
        }
        MercenaryData data = screen.getMercData();
        if (data == null) {
            return true;
        }
        int within = index % MercenaryClass.SUPPORTS_PER_SKILL;
        return within >= data.getSupportSlots(skillSlot());
    }

    @Override
    public void onPress() {
        if (isLocked()) {
            return;
        }
        // the grid is built from the local inventory, so the action needs to know where it is going
        MercEquipAction.TARGET_TYPE = type;
        MercEquipAction.TARGET_INDEX = index;

        // cancelling the pick returns to the mercenary screen, matching where MercEquipAction leaves
        // you after a successful one
        Minecraft.getInstance().setScreen(new InvGuiScreen(
                GuiInventoryGrids.ofMercSlotChoices(ClientOnly.getPlayer(), type, index),
                Words.Mercenary.locName(), () -> ClientOnly.setScreen(new MercenaryScreen())));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1 && this.isMouseOver(mx, my) && this.active) {
            // right click takes the item back rather than opening the picker
            if (!getStack().isEmpty()) {
                Packets.sendToServer(MercenaryActionPacket.clearSlot(type, index));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        ItemStack stack = getStack();
        boolean locked = isLocked();

        gui.setColor(1F, 1F, 1F, 1F);

        if (locked) {
            // identical to SuppGemOverlayButton: blocked_slot.png drawn 2px out from the slot it
            // covers, with the same blit arguments, so a locked mercenary socket is indistinguishable
            // from a locked one on the skill gem screen.
            gui.blit(MercGui.BLOCKED_SLOT, getX() - MercGui.LOCK_OFFSET, getY() - MercGui.LOCK_OFFSET,
                    MercGui.LOCK_W, MercGui.LOCK_W, MercGui.LOCK_W, MercGui.LOCK_W, MercGui.LOCK_W, MercGui.LOCK_W);
        } else if (!stack.isEmpty()) {
            // deliberately NOT RenderUtils.renderStack - that pushes a translate(x, y) and then also
            // passes x,y to renderItem, so the icon lands at double the offset and ends up nowhere
            // near its slot. the item still hit-tests here, which is why the tooltip worked while
            // nothing was drawn.
            gui.renderItem(stack, getX(), getY());
        }

        // only while hovered, but then every frame: gear and gem tooltips expand on shift and alt, and a
        // tooltip cached on stack-changed-since-last-frame never sees the key go down. off the hover it
        // is not drawn anyway, so this is no more work overall than caching was
        if (isHovered()) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(stack, locked))));
        }
    }

    private List<Component> buildTooltip(ItemStack stack, boolean locked) {
        List<Component> list = new ArrayList<>();

        if (locked) {
            // same wording the skill gem screen uses for its locked sockets
            list.add(Words.LockedSuppGemSlot.locName().withStyle(ChatFormatting.RED));
            list.add(Component.empty());

            if (screen.getMercData() != null && screen.getMercData().getEquippedSkill(skillSlot()) == null) {
                list.add(Words.NoSocketedSpell.locName().withStyle(ChatFormatting.YELLOW));
            } else {
                list.add(Words.IncreaseYourLevel.locName().withStyle(ChatFormatting.YELLOW));
            }
            return list;
        }

        if (stack.isEmpty()) {
            // one line for all three slot types - gear, support and aura all equip the same way
            list.add(Words.MercenaryClickToEquip.locName().withStyle(ChatFormatting.GRAY));
            return list;
        }

        list.addAll(stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.NORMAL));
        list.add(Words.MercenaryRightClickClear.locName().withStyle(ChatFormatting.DARK_GRAY));
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
