package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.library_of_exile.wrappers.ExileText;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.CoreStat;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
 * One of the three core attribute panels along the top right. Read only - a mercenary allocates its
 * own attributes from its class profile, so there is nothing to click (design section 3).
 */
public class MercCoreStatButton extends AbstractButton {

    private final MercenaryScreen screen;
    private final Stat stat;
    private final ChatFormatting color;

    private int lastValue = Integer.MIN_VALUE;
    private boolean tooltipDirty = true;

    public MercCoreStatButton(MercenaryScreen screen, Stat stat, ChatFormatting color, int x, int y) {
        super(x, y, MercGui.CORE_BOX_W, MercGui.CORE_BOX_H, Component.empty());
        this.screen = screen;
        this.stat = stat;
        this.color = color;
    }

    private int getValue() {
        return (int) screen.getStatValue(stat);
    }

    @Override
    public void onPress() {
        // nothing to allocate - automated
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        int value = getValue();
        if (value != lastValue) {
            lastValue = value;
            tooltipDirty = true;
        }

        // no background blit - the art already draws a panel behind each of these three boxes, so all
        // this has to contribute is the number, centred in it.
        gui.setColor(1F, 1F, 1F, 1F);

        var font = Minecraft.getInstance().font;
        Component text = Component.literal(String.valueOf(value)).withStyle(color);
        gui.drawString(font, text,
                getX() + width / 2 - font.width(text) / 2,
                getY() + height / 2 - 4,
                0xFFFFFF, true);

        if (tooltipDirty) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip())));
            tooltipDirty = false;
        }
    }

    /**
     * The same tooltip the player gets from hovering an attribute on the hub - what one point grants,
     * then what the current total grants. Built by the identical call
     * {@code MainHubScreen.AllocateStatButton.setTooltipMod()} uses;
     * {@link CoreStat#getCoreStatTooltip} is EntityData typed, so it takes a mercenary unchanged.
     * <p>
     * No allocate/remove buttons here on purpose - a mercenary spends its own points.
     */
    private List<Component> buildTooltip() {
        List<Component> list = new ArrayList<>();
        list.add(stat.locName().withStyle(ChatFormatting.GREEN));
        list.add(ExileText.ofText("").get());

        MercenaryEntity merc = screen.getMercEntity();

        if (merc == null || !(stat instanceof CoreStat core)) {
            // nothing to read the real numbers off yet
            list.add(Words.MercenaryNotSummoned.locName().withStyle(ChatFormatting.RED));
            return list;
        }

        var unit = Load.Unit(merc);
        list.addAll(core.getCoreStatTooltip(unit, unit.getUnit().getCalculatedStat(stat)));

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
