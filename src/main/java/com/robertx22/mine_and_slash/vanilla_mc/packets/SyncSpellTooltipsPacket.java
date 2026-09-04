package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellTooltipInfos;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

// the finished spell tooltip numbers, sent whenever the player's stats are recalculated. the client
// can't work any of them out itself, see SpellTooltipCalculation
public class SyncSpellTooltipsPacket extends MyPacket<SyncSpellTooltipsPacket> {

    static String LOC = "st";

    SpellTooltipInfos data;

    public SyncSpellTooltipsPacket() {
        this(new SpellTooltipInfos());
    }

    public SyncSpellTooltipsPacket(SpellTooltipInfos data) {
        this.data = data;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("sync_spell_tooltips");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.data = LoadSave.loadOrBlank(SpellTooltipInfos.class, new SpellTooltipInfos(), buf.readNbt(), LOC, new SpellTooltipInfos());
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        CompoundTag nbt = new CompoundTag();
        LoadSave.Save(data, nbt, LOC);
        buf.writeNbt(nbt);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Load.player(ctx.getPlayer()).spellTooltips = data;
    }

    @Override
    public MyPacket<SyncSpellTooltipsPacket> newInstance() {
        return new SyncSpellTooltipsPacket();
    }
}
