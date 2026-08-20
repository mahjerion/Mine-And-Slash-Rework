package com.robertx22.mine_and_slash.prophecy.gui;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.prophecy.RerollProphecyPacket;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RerollProphecyButton extends ImageButton {

    static ResourceLocation TEXTURE = new ResourceLocation(SlashRef.MODID, "textures/gui/prophecy/reroll_button.png");

    static int SIZE = 18;

    Minecraft mc = Minecraft.getInstance();

    public RerollProphecyButton(int x, int y) {
        super(x, y, SIZE, SIZE, 0, 0, SIZE, TEXTURE, (button) -> {
            Packets.sendToServer(new RerollProphecyPacket());
        });
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {

        var data = Load.player(mc.player).prophecy;
        int maxRerolls = ServerContainer.get().PROPHECY_MAX_REROLLS_PER_MAP.get();

        this.active = data.rerollsUsed < maxRerolls;

        List<MutableComponent> list = new ArrayList<>();
        if (data.usedFreeRoll) {
            list.add(Words.REROLL_PROPHECY_OFFERS.locName());
            list.add(Words.COSTS_FAVOR.locName(data.getRerollCost()));
            list.add(Words.PROPHECY_REROLLS_REMAINING.locName(Math.max(0, maxRerolls - data.rerollsUsed), maxRerolls));
        } else {
            list.add(Words.PROPHECY_NOT_ACTIVE_THIS_MAP.locName());
        }

        this.setTooltip(Tooltip.create(TextUTIL.mergeList(list)));

        super.render(gui, mouseX, mouseY, delta);
    }
}
