package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns mercenaries off entirely for this character, and back on again.
 * <p>
 * Distinct from {@link MercModeButton}'s Idle setting, which only stops the mercenary fighting - the
 * entity is still out and still following. This dismisses it and stops the tick bringing it back.
 * Nothing is lost: gear, level, exp and equipped skills all live in the owner's MercenaryData, and
 * the entity itself is never written to the world save anyway.
 * <p>
 * The flag lives on MercenaryStorageData rather than MercenaryData, so it covers every mercenary
 * class rather than only the one currently selected.
 */
public class MercToggleButton extends AbstractButton {

    // the state can change from under the screen (another packet, a relog), and rebuilding a
    // tooltip every frame is real work - MercModeButton tracks its mode the same way
    private Boolean lastDisabled = null;
    private boolean tooltipDirty = true;

    public MercToggleButton(int x, int y) {
        super(x, y, MercGui.TOGGLE_BUTTON_SIZE, MercGui.TOGGLE_BUTTON_SIZE, Component.empty());
    }

    private boolean isDisabled() {
        var p = ClientOnly.getPlayer();
        return p != null && Load.player(p).mercs.disabled;
    }

    @Override
    public void onPress() {
        Packets.sendToServer(MercenaryActionPacket.toggleDisabled());
        tooltipDirty = true;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        boolean disabled = isDisabled();

        if (lastDisabled == null || lastDisabled != disabled) {
            lastDisabled = disabled;
            tooltipDirty = true;
        }

        gui.setColor(1F, 1F, 1F, 1F);
        RenderUtils.render16Icon(gui, disabled ? MercGui.TOGGLE_OFF : MercGui.TOGGLE_ON, getX(), getY());

        if (tooltipDirty) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(disabled))));
            tooltipDirty = false;
        }
    }

    private List<Component> buildTooltip(boolean disabled) {
        List<Component> list = new ArrayList<>();
        list.add(Words.MercenaryToggle.locName().withStyle(ChatFormatting.GOLD));
        list.add(disabled
                ? Words.DISABLED.locName().withStyle(ChatFormatting.RED)
                : Words.ENABLED.locName().withStyle(ChatFormatting.GREEN));
        list.add(Words.MercenaryToggleTip.locName().withStyle(ChatFormatting.GRAY));
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
