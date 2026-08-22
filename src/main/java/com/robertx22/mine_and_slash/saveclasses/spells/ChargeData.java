package com.robertx22.mine_and_slash.saveclasses.spells;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.Cached;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChargeData {

    private HashMap<String, List<CdData>> cds = new HashMap<>();

    static class CdData {

        String id;
        Integer ticks;
        // what ticks started at, so a bar can be drawn against the real duration rather than the raw
        // config value - the stored one is stat modified. null on entries saved before this existed
        Integer need;

        public CdData(String id, Integer ticks) {
            this.id = id;
            this.ticks = ticks;
            this.need = ticks;
        }
    }

    public int getCurrentTicksChargingOf(String id) {
        if (cds.containsKey(id)) {
            if (cds.get(id).size() > 0) {
                return cds.get(id).get(0).ticks;
            }
        }
        return 0;
    }

    // how much of the wait for the next charge is left, 1 just spent down to 0 ready. same orientation
    // as a cooldown, so the hotbar can draw both with the one bar
    public float getRechargeFractionLeft(String id, int fallbackNeed) {
        if (!cds.containsKey(id) || cds.get(id).isEmpty()) {
            return 0F;
        }
        CdData first = cds.get(id).get(0);
        int need = first.need == null ? fallbackNeed : first.need;
        if (need < 1) {
            return 0F;
        }
        return MathHelper.clamp(first.ticks / (float) need, 0F, 1F);
    }

    public boolean hasCharge(String id) {
        return getCharges(id) > 0;
    }


    void add(String id, int cd) {
        addList(id);
        cds.get(id).add(new CdData(id, cd));
    }

    void addList(String id) {
        if (!cds.containsKey(id)) {
            cds.put(id, new ArrayList<>());
        }
    }

    public void spendCharge(Player player, Spell spell, int cd) {

        if (player.level().isClientSide) {
            return;
        }
        String id = spell.config.charge_name;

        int fn = MathHelper.clamp(cd, 0, 100000);
        
        add(id, fn);

        Load.player(player).playerDataSync.setDirty();
    }


    static List<CdData> empty = new ArrayList<>();

    public int getCharges(String id) {
        int oncd = (int) cds.getOrDefault(id, empty).stream().count();

        if (!Cached.MAX_SPELL_CHARGES.containsKey(id)) {
            ExileLog.get().log("Spell has no charges possible or the max spell charges aren't cached!");
        }

        int charges = Cached.MAX_SPELL_CHARGES.getOrDefault(id, 0);

        charges -= oncd;

        charges = MathHelper.clamp(charges, 0, 100);

        return charges;
    }


    public void addOneCharges() {

        for (Map.Entry<String, List<CdData>> en : cds.entrySet()) {
            if (en.getValue().size() > 0) {
                en.getValue().remove(0);
            }
        }

    }

    // runs on both sides. the client needs it or the recharge bar only moves when a sync happens to
    // land, which reads as the bar being frozen - cooldowns already tick locally for the same reason
    public void onTicks(Player player, int ticks) {

        boolean sync = false;

        for (Map.Entry<String, List<CdData>> en : cds.entrySet()) {
            List<CdData> list = en.getValue();

            // charges come back one at a time. only the one at the front of the queue is actually
            // regenerating - the rest sit at their full time until their turn, so spending three
            // charges costs three full regens instead of them overlapping into barely more than one.
            // the leftover is carried into the next charge so a coarse tick step loses nothing, and
            // so the reset command can still clear a whole queue in a single call
            int remaining = ticks;

            while (remaining > 0 && !list.isEmpty()) {
                CdData first = list.get(0);

                if (first.ticks > remaining) {
                    first.ticks -= remaining;
                    remaining = 0;
                } else {
                    remaining -= Math.max(0, first.ticks);
                    list.remove(0);
                    sync = true;
                }
            }
        }

        if (sync && !player.level().isClientSide) {

            Load.player(player).playerDataSync.setDirty();
        }
    }
}
