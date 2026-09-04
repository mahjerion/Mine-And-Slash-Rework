package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.capability.DirtySync;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.CommonStatUtils;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.PlayerStatUtils;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.SpellTooltipCalculation;
import com.robertx22.mine_and_slash.vanilla_mc.packets.SyncSpellTooltipsPacket;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class CachedPlayerStats {

    public Player p;
    public List<StatContext> allStatsWithoutSuppGems = new ArrayList<>();

    public List<StatContext> statContexts = new ArrayList<>();


    public StatContext enchantCompat = null;

    private StatContext statCompat;

    public DirtySync ENCHANT_COMPAT = new DirtySync("enchant_compat", x -> {
        this.enchantCompat = GearItemData.getEnchantCompatStats(p, Load.Unit(p).equipmentCache.getGear());
    });

    public DirtySync STAT_COMPAT = new DirtySync("stat_compat", x -> {
        recalcStatCompat();
        Load.Unit(p).equipmentCache.STAT_CALC.setDirty();
    });
    public StatContext omenStats;

    public StatContext getStatCompatStats() {
        if (statCompat == null) {
            recalcStatCompat();
        }
        return statCompat;
    }

    private void recalcStatCompat() {
        this.statCompat = CommonStatUtils.addStatCompat(p);
    }

    // I guess these could be all stats that don't change often, fine to set these to recalc everything
    public DirtySync ALLOCATED = new DirtySync("misc_player", x -> {
        recalcAllocated();
        EntityData unit = Load.Unit(p);
        unit.equipmentCache.STAT_CALC.setDirty();
        unit.getResources().capAll(p);
    }) {
        @Override
        public void setDirty() {
            super.setDirty();
        }

    };

    // one full stat calculation per equipped skill gem plus a simulated hit per damage component is
    // far too much to redo every time a buff ticks, and nothing but a tooltip ever reads the result,
    // so it is allowed to lag a second behind the stats it is built from
    public static final int SPELL_TOOLTIP_CALC_INTERVAL = 20;
    private boolean calcedSpellTooltipsOnce = false;
    private int lastSpellTooltipTick = 0;
    private boolean forceSpellTooltipSend = false;

    // the client throws its copy away on a dimension change while the server's stays the same, so
    // the next result has to go out even if nothing in it changed
    public void resendSpellTooltips() {
        forceSpellTooltipSend = true;
        SPELL_TOOLTIPS.setDirty();
    }

    // the numbers a spell tooltip shows that only the server can work out. recalculated after the
    // stats they are built from changed, then pushed to the client - see SpellTooltipCalculation
    public DirtySync SPELL_TOOLTIPS = new DirtySync("spell_tooltips", x -> {
        if (x instanceof ServerPlayer sp) {
            int now = sp.tickCount;
            // now < last means this is a different entity than the one that last calculated (respawn),
            // which has no client side numbers at all and has to be done straight away
            if (calcedSpellTooltipsOnce && now >= lastSpellTooltipTick && now - lastSpellTooltipTick < SPELL_TOOLTIP_CALC_INTERVAL) {
                this.SPELL_TOOLTIPS.setDirty(); // too soon, come back next tick
                return;
            }
            calcedSpellTooltipsOnce = true;
            lastSpellTooltipTick = now;

            var next = SpellTooltipCalculation.calcAll(sp);
            var pd = Load.player(sp);
            // a stat recalc fires on every buff tick in combat and most of them change nothing a
            // tooltip shows, so only send when the numbers actually moved
            if (forceSpellTooltipSend || !next.equals(pd.spellTooltips)) {
                forceSpellTooltipSend = false;
                pd.spellTooltips = next;
                Packets.sendToClient(sp, new SyncSpellTooltipsPacket(next));
            }
        }
    });

    public void setAllDirty() {
        ALLOCATED.setDirty();
        ENCHANT_COMPAT.setDirty();
        STAT_COMPAT.setDirty();
        SPELL_TOOLTIPS.setDirty();
    }

    public void tick() {

        ALLOCATED.onTickTrySync(p);
        ENCHANT_COMPAT.onTickTrySync(p);
        STAT_COMPAT.onTickTrySync(p);

        // SPELL_TOOLTIPS is not ticked here on purpose - it is built out of the stats the recalc that
        // runs after this produces, so CachedEntityStats ticks it once that has happened
    }

    public CachedPlayerStats(Player p) {
        this.p = p;
    }

    private void recalcAllocated() {

        if (false) {
            p.sendSystemMessage(Component.literal("Re calcing player stuff"));
        }
        statContexts = new ArrayList<>();

        var playerData = Load.player(p);

        playerData.aurasOn = new ArrayList<>();
        for (SkillGemData aura : playerData.getSkillGemInventory().getAurasGems()) {
            playerData.aurasOn.add(aura.id);
        }


        statContexts.addAll(PlayerStatUtils.addToolStats(p)); // todo this needs fixing

        statContexts.add(PlayerStatUtils.addBonusExpPerCharacters(p));

        statContexts.addAll(playerData.buff.getStatAndContext(p));

        statContexts.addAll(playerData.getSkillGemInventory().getAuraStats(p));
        statContexts.addAll(playerData.jewelData.getStatAndContext(p));
        statContexts.addAll(playerData.statPoints.getStatAndContext(p));

        statContexts.addAll(PlayerStatUtils.addNewbieElementalResists(Load.Unit(p)));
        statContexts.addAll(playerData.talents.getStatAndContext(p));
        statContexts.addAll(playerData.ascClass.getStatAndContext(p));
        statContexts.addAll(playerData.prophecy.getStatAndContext(p));


    }
}
