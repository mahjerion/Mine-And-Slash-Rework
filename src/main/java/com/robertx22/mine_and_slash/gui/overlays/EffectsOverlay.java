package com.robertx22.mine_and_slash.gui.overlays;

import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.config.forge.overlay.OverlayType;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class EffectsOverlay {

    public static void render(GuiGraphics gui, boolean horizontal) {
        Player p = ClientOnly.getPlayer();

        var config = ClientConfigs.getConfig().getOverlayConfig(OverlayType.EFFECTS_VERTICAL);


        int x = config.getPos().x;
        int y = config.getPos().y;

        int size = 16;
        int spacing = 18;

        int bgX = 18;
        int bgY = 24;

        // Minecraft mc = Minecraft.getInstance();

        for (Map.Entry<String, ExileEffectInstanceData> en : Load.Unit(p).getStatusEffectsData().exileMap.entrySet()) {
            if (!en.getValue().shouldRemove()) {
                var eff = ExileDB.ExileEffects().get(en.getKey());

                gui.blit(SlashRef.guiId("effect/effect_bg"), x, y, bgX, bgY, 0, 0, bgX, bgY, bgX, bgY);
                gui.blit(eff.getTexture(), x + 1, y + 1, size, size, 0, 0, size, size, size, size);

                gui.blit(SlashRef.guiId("effect/effect_overlay"), x, y, bgX, bgY, 0, 0, bgX, bgY, bgX, bgY);
                // gui.drawString(mc.font, en.getValue().stacks + "", (int) x + 13, (int) y + 2, ChatFormatting.WHITE.getColor(), true);

                GuiUtils.renderScaledText(gui, (int) x + 15, (int) y + 5, 0.6F, en.getValue().stacks + "", ChatFormatting.WHITE);


                String text = en.getValue().getDurationString();

                GuiUtils.renderScaledText(gui, (int) x + 10, (int) y + 21, 0.6F, text, ChatFormatting.YELLOW);

                if (horizontal) {
                    x += bgX;
                } else {
                    y += bgY;
                }
            }
        }

    }
}
