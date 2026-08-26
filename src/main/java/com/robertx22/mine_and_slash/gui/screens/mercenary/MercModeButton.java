package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
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

/** Cycles Aggressive -> Defensive -> Idle. */
public class MercModeButton extends AbstractButton {

    private final MercenaryScreen screen;

    private MercenaryData.CombatMode lastMode = null;
    private boolean tooltipDirty = true;

    public MercModeButton(MercenaryScreen screen, int x, int y) {
        super(x, y, MercGui.MODE_BUTTON_W, MercGui.MODE_BUTTON_H, Component.empty());
        this.screen = screen;
    }

    private MercenaryData.CombatMode getMode() {
        MercenaryData data = screen.getMercData();
        return data == null ? MercenaryData.CombatMode.AGGRESSIVE : data.mode;
    }

    @Override
    public void onPress() {
        Packets.sendToServer(MercenaryActionPacket.cycleMode());
        tooltipDirty = true;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        MercenaryData.CombatMode mode = getMode();
        if (mode != lastMode) {
            lastMode = mode;
            tooltipDirty = true;
        }

        // the art draws the socket; this just puts the right 16x16 mode icon in it. the hitbox is the
        // whole ornate cell, so the icon is offset into its centre rather than drawn at getX().
        gui.setColor(1F, 1F, 1F, 1F);
        RenderUtils.render16Icon(gui, MercGui.combatMode(mode.id),
                getX() + (MercGui.MODE_ICON_X - MercGui.MODE_BUTTON_X),
                getY() + (MercGui.MODE_ICON_Y - MercGui.MODE_BUTTON_Y));

        if (tooltipDirty) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(mode))));
            tooltipDirty = false;
        }
    }

    private List<Component> buildTooltip(MercenaryData.CombatMode mode) {
        List<Component> list = new ArrayList<>();
        list.add(Words.MercenaryCombatMode.locName().withStyle(ChatFormatting.GOLD));
        list.add(mode.locName().withStyle(mode.format));
        list.add(switch (mode) {
            case AGGRESSIVE -> Words.MercenaryModeAggressiveTip.locName().withStyle(ChatFormatting.GRAY);
            case DEFENSIVE -> Words.MercenaryModeDefensiveTip.locName().withStyle(ChatFormatting.GRAY);
            case IDLE -> Words.MercenaryModeIdleTip.locName().withStyle(ChatFormatting.GRAY);
        });
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
