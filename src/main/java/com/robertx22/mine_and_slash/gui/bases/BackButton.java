package com.robertx22.mine_and_slash.gui.bases;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// A "go back one screen" affordance. The mod has no screen history stack, so the destination is
// passed in - top level screens hand it the Main Hub, sub screens hand it their parent.
//
// The art is the left arrow of leftright.png, the same 256x256 sheet LeftRightButton and
// StatDirectionNavigationButton draw from: 22x22 cells, left arrow at u=0, hover row at v=22.
// Those two pass yDiffTex=0 and so never show their hover frame; this passes SIZE and does.
public class BackButton extends ImageButton {

    public static final int SIZE = 22;

    private static final ResourceLocation TEXTURE = SlashRef.guiId("leftright/leftright");

    // the tooltip is the name of where you are going, so hovering answers "back to what?"
    public BackButton(int x, int y, Component tooltip, Runnable onBack) {
        super(x, y, SIZE, SIZE, 0, 0, SIZE, TEXTURE, b -> onBack.run());
        setTooltip(Tooltip.create(tooltip));
    }
}
