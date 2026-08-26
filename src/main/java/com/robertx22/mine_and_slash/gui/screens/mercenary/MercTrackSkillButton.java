package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
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

/**
 * One skill on the class track down the right hand side. Its row is its unlock level, so the track
 * reads as a progression the way the artwork's 1/5/10/15/20/25/30 axis implies.
 * <p>
 * Clicking an unlocked skill slots it into the first free active slot.
 */
public class MercTrackSkillButton extends AbstractButton {

    private final MercenaryScreen screen;
    private final Spell spell;
    private final int unlockLevel;


    public MercTrackSkillButton(MercenaryScreen screen, Spell spell, int unlockLevel, int x, int y) {
        super(x, y, MercGui.TRACK_ICON_SIZE, MercGui.TRACK_ICON_SIZE, Component.empty());
        this.screen = screen;
        this.spell = spell;
        this.unlockLevel = unlockLevel;
    }

    private boolean isUnlocked() {
        return screen.getMercLevel() >= unlockLevel;
    }

    @Override
    public void onPress() {
        if (!isUnlocked()) {
            return;
        }
        MercenaryData data = screen.getMercData();
        if (data == null) {
            return;
        }
        // already equipped? leave it where it is rather than shuffling the priority queue
        if (data.slotOfSpell(spell.GUID()) > -1) {
            return;
        }
        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            if (data.getEquippedSkill(i) == null) {
                Packets.sendToServer(MercenaryActionPacket.setSkill(i, spell.GUID()));
                return;
            }
        }
        // every slot full - the player picks which one to replace from the slot itself
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        boolean unlocked = isUnlocked();

        // the slot frame sits behind the icon and stays at full colour whether or not the skill is
        // unlocked - only the icon greys out, the same way the class tree reads. the 18x18 frame is
        // one pixel out on each side from the 16x16 icon.
        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.SPELL_SLOT, getX() - 1, getY() - 1,
                MercGui.SPELL_SLOT_SIZE, MercGui.SPELL_SLOT_SIZE,
                MercGui.SPELL_SLOT_SIZE, MercGui.SPELL_SLOT_SIZE,
                MercGui.SPELL_SLOT_SIZE, MercGui.SPELL_SLOT_SIZE);

        // greyed out rather than hidden, so the track shows what is still to come
        if (unlocked) {
            gui.setColor(1F, 1F, 1F, 1F);
        } else {
            gui.setColor(0.35F, 0.35F, 0.35F, 1F);
        }
        gui.blit(spell.getIconLoc(), getX(), getY(), 0, 0, width, height, width, height);
        gui.setColor(1F, 1F, 1F, 1F);

        // rebuilt per frame while hovered rather than cached - a cached tooltip never sees shift or alt
        // go down, so the expanded spell tooltip could not be reached from this screen
        if (isHovered()) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(unlocked))));
        }
    }

    private List<Component> buildTooltip(boolean unlocked) {
        List<Component> list = new ArrayList<>();
        list.addAll(spell.GetTooltipString(ClientMercenary.tooltipInfo()));
        if (!ClientMercenary.isOut()) {
            // the numbers above fell back to the player's stats, so say why they can be off
            list.add(Words.MercenaryNotSummoned.locName().withStyle(ChatFormatting.RED));
        }
        if (!unlocked) {
            list.add(Words.MercenarySkillLockedTip.locName(unlockLevel).withStyle(ChatFormatting.RED));
        }
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
