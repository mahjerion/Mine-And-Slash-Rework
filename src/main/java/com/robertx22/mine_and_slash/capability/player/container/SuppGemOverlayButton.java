package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.MaxLinks;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class SuppGemOverlayButton extends ImageButton {

    public static int BUTTON_SIZE_X = 20;
    public static int BUTTON_SIZE_Y = 19;

    int spellSlot;
    int supportIndex;

    boolean can;

    MaxLinks links;

    // the tooltip only gets rebuilt when the state below actually changes
    boolean tooltipDirty = true;

    public SuppGemOverlayButton(int spellSlot, int supportIndex, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SlashRef.guiId("blocked_slot"), (button) -> {
            //Minecraft.getInstance().setScreen(new InvGuiScreen(GuiInventoryGrids.ofSelectableSpells(ClientOnly.getPlayer(), slot)));
        });
        this.spellSlot = spellSlot;
        this.supportIndex = supportIndex;
    }

    // socketing a skill gem while the screen is open changes how many links it has, so this can't be
    // a snapshot taken when the screen was built
    private void refreshState() {

        Player player = ClientOnly.getPlayer();

        MaxLinks newLinks = null;
        if (player != null) {
            SkillGemData spell = Load.player(player).spellCastingData.getSpellData(spellSlot).getData();
            if (spell != null) {
                newLinks = spell.getMaxLinks(player);
            }
        }
        boolean newCan = newLinks != null && newLinks.links >= supportIndex + 1;

        if (newCan != this.can || !isSameLinks(this.links, newLinks)) {
            this.tooltipDirty = true;
        }

        this.can = newCan;
        this.links = newLinks;
    }

    private static boolean isSameLinks(MaxLinks one, MaxLinks two) {
        if (one == null || two == null) {
            return one == two;
        }
        return one.links == two.links && one.cappedByLevel == two.cappedByLevel && one.cappedBySpellLevel == two.cappedBySpellLevel;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {

        refreshState();

        if (tooltipDirty) {
            tooltipDirty = false;

            List<Component> tooltip = new ArrayList<>();
            if (!can) {
                tooltip.add(Words.LockedSuppGemSlot.locName().withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());

                if (links == null) {
                    tooltip.add(Words.NoSocketedSpell.locName().withStyle(ChatFormatting.YELLOW));

                } else {
                    if (links.cappedByLevel) {
                        tooltip.add(Words.IncreaseYourLevel.locName().withStyle(ChatFormatting.YELLOW));
                    }
                    if (links.cappedBySpellLevel) {
                        tooltip.add(Words.IncreaseSpellLevel.locName().withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
            this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));
        }

        super.render(gui, mouseX, mouseY, delta);
    }


    static ResourceLocation id = SlashRef.guiId("blocked_slot");

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {

        //  super.renderWidget(gui, mouseX, mouseY, delta);

        if (!can) {
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(id, getX(), getY(), BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);
        }
    }


}
