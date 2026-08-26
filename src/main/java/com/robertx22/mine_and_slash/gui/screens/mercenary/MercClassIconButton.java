package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

/**
 * The class portrait in the top left corner. Read only - the arrows underneath are what changes the
 * class - it exists as a widget purely so hovering the art can name the class it belongs to.
 * <p>
 * MercenaryClass is an IAutoLocName keyed on {@code mmorpg.mercenary.<id>}, so {@code locName()}
 * already resolves without any new localization entry.
 */
public class MercClassIconButton extends AbstractButton {

    private final MercenaryScreen screen;

    // the class can change while the screen is open, and rebuilding a tooltip every frame is real
    // work - MercSkillButton tracks its spell the same way
    private String lastClassId = null;
    private boolean tooltipDirty = true;

    public MercClassIconButton(MercenaryScreen screen, int x, int y) {
        super(x, y, MercGui.CLASS_ICON_SIZE, MercGui.CLASS_ICON_SIZE, Component.empty());
        this.screen = screen;
    }

    @Override
    public void onPress() {
        // nothing to click - the arrows below cycle the class
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        MercenaryClass mercClass = screen.getMercClass();

        String id = mercClass == null ? null : mercClass.GUID();
        if (id == null ? lastClassId != null : !id.equals(lastClassId)) {
            lastClassId = id;
            tooltipDirty = true;
        }

        if (mercClass == null) {
            return;
        }

        // the art leaves a 38x38 opening here; this is the blit MercenaryScreen.renderClassIcon used
        // to do before the rect got an owner
        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(mercClass.getIconLoc(), getX(), getY(), 0, 0,
                MercGui.CLASS_ICON_SIZE, MercGui.CLASS_ICON_SIZE, MercGui.CLASS_ICON_SIZE, MercGui.CLASS_ICON_SIZE);

        if (tooltipDirty) {
            setTooltip(Tooltip.create(mercClass.locName().withStyle(ChatFormatting.GOLD)));
            tooltipDirty = false;
        }
    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
