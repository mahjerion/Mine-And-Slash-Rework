package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.resources.ResourceLocation;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.OpenEntityStatsRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Stats button in the bottom left corner. It sits inside the frame rather than hanging off the
 * outside like the hub's, collapsing to an icon and expanding into the long bar on hover -
 * {@code buttons.png} carries both, the icon at v=0 and the bar at v=28.
 * <p>
 * It opens the ordinary {@link StatScreen}, which already takes any LivingEntity. Because the
 * mercenary is a real world entity, the client's copy of its Unit has to be synced first, which is
 * exactly what {@link OpenEntityStatsRequestPacket} already does for mobs.
 */
public class MercStatsButton extends AbstractButton {

    private static final ResourceLocation STATS_ICON = SlashRef.id("textures/gui/main_hub/icons/stats.png");

    private final MercenaryScreen screen;

    public MercStatsButton(MercenaryScreen screen, int x, int y) {
        super(x, y, MercGui.STATS_BUTTON_W, MercGui.STATS_BUTTON_H, Component.empty());
        this.screen = screen;
        // no tooltip - hovering already expands the button into a bar that spells out "Stats",
        // so a floating tooltip on top of that is just noise
    }

    @Override
    public void onPress() {
        MercenaryEntity merc = screen.getMercEntity();
        if (merc == null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Words.MercenaryNotSummoned.locName().withStyle(ChatFormatting.RED), false);
            return;
        }
        // the server sends the mercenary's Unit back and opens the screen on arrival
        Packets.sendToServer(new OpenEntityStatsRequestPacket(merc));
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        boolean hovered = isHovered();

        // the hover state is a wider sprite, not a recolour, so the drawn width changes with it
        int w = hovered ? MercGui.STATS_BUTTON_HOVER_W : MercGui.STATS_BUTTON_W;
        int v = hovered ? MercGui.STATS_BUTTON_H : 0;

        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.BUTTONS, getX(), getY(), 0, v, w, height, 256, 256);

        // the sprite leaves an empty recessed square; fill it with the hub's stats icon. the square is
        // in the same spot on both the collapsed and the expanded sprite, so this is unconditional.
        RenderUtils.render16Icon(gui, STATS_ICON,
                getX() + MercGui.STATS_ICON_DX, getY() + MercGui.STATS_ICON_DY);

        if (hovered) {
            var font = Minecraft.getInstance().font;
            Component text = Words.Stats.locName().withStyle(ChatFormatting.YELLOW);
            gui.drawString(font, text, getX() + MercGui.STATS_LABEL_OFFSET, getY() + height / 2 - 4, 0xFFFFFF, true);
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
