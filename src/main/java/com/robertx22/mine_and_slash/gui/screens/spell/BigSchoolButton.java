package com.robertx22.mine_and_slash.gui.screens.spell;

import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BigSchoolButton extends ImageButton {

    static int SIZE = 36;

    SpellSchoolScreen scren;

    public BigSchoolButton(SpellSchoolScreen scren, int xPos, int yPos) {
        super(xPos, yPos, SIZE, SIZE, 0, 0, 0, SlashRef.guiId(""), (button) -> {
        });
        this.scren = scren;
    }

    @Override
    public void onPress() {

    }

    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        var school = scren.currentSchool();

        if (school != null) {
            this.renderTexture(pGuiGraphics, school.getIconLoc(), this.getX(), this.getY(), 0, 0, this.yDiffTex, SIZE, SIZE, SIZE, SIZE);

            if (this.isHoveredOrFocused()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(school.locName().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));
            }
        }
    }


}