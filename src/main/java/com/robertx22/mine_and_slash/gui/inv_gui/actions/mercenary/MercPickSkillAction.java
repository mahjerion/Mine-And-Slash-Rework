package com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.gui.bases.GuiMousePosition;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.gui.screens.mercenary.MercenaryScreen;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Picks one of the mercenary's learned skills into an active slot. Mercenary skills are not gems -
 * they are learned by levelling - so unlike {@link com.robertx22.mine_and_slash.gui.inv_gui.actions.PickSpellAction}
 * there is no item involved, just the spell id.
 */
public class MercPickSkillAction extends GuiAction<Integer> {

    /** which of the 4 active slots the pick is for. set just before the grid is built. */
    public static int SLOT = 0;

    private final Spell spell;

    public MercPickSkillAction(Spell spell) {
        this.spell = spell;
    }

    @Override
    public ResourceLocation getIcon() {
        return spell.getIconLoc();
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeInt(SLOT);
    }

    @Override
    public Integer loadExtraData(FriendlyByteBuf buf) {
        return buf.readInt();
    }

    @Override
    public List<Component> getTooltip(Player p) {
        // scored off the mercenary, not the viewing player - it is the mercenary that will cast it
        return spell.GetTooltipString(ClientMercenary.tooltipInfo());
    }

    @Override
    public void doAction(Player p, Object obj) {
        // the level and duplicate checks live in exactly one place, shared with the packet
        MercenaryActionPacket.equipSkill(p, (int) obj, spell.GUID());
    }

    @Override
    public void clientAction(Player p, Object obj) {
        GuiMousePosition.save();
        Minecraft.getInstance().setScreen(new MercenaryScreen());
    }

    @Override
    public String GUID() {
        return "merc_skill_" + spell.GUID();
    }
}
