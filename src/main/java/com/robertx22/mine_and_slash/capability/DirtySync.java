package com.robertx22.mine_and_slash.capability;

import com.robertx22.mine_and_slash.uncommon.utilityclasses.ThrottledErrors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class DirtySync {

    private boolean dirty = false;

    // monotonic, unlike the dirty flag, which clears every time we sync. CapNbtCache keys off this
    // so the nbt cache can reuse the ~71 setDirty() call sites already maintained for client sync
    // instead of needing a second set of invalidation hooks.
    private long version = 0;

    private String id;
    private Consumer<Entity> sync;

    public DirtySync(String id, Consumer<Entity> sync) {
        this.sync = sync;
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setDirty() {
        this.dirty = true;
        this.version++;
    }

    // should only be used when you need instant sync
    public void setDirtyAndSync(Player p) {
        setDirty();
        onTickTrySync(p);
    }

    public void onTickTrySync(Entity p) {
        if (dirty) {
            dirty = false;

            try {
                sync.accept(p);

                onSynced(p);
            } catch (Exception e) {
                // the flag is cleared before the work so a sync queued BY the sync still lands, but
                // that also meant a single throw silently dropped the request for good. Nothing
                // re-marks an idle mob dirty, so it would keep serving whatever its cache held -
                // including an expired effect's stats, and including never running the stat calc
                // that cleans up leaked attribute modifiers. Re-arm and let it retry next tick; if
                // it is a permanent failure the repeating trace is the signal we want.
                dirty = true;
                ThrottledErrors.log("dirty_sync_" + id, "mns dirty sync failed, will retry: " + id, e);
            }

            if (p instanceof Player pl) {
                //  pl.sendSystemMessage(Component.literal(id));
            }
        }
    }

    public void onSynced(Entity p) {

    }

}
