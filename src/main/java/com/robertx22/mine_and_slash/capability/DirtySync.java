package com.robertx22.mine_and_slash.capability;

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
            sync.accept(p);

            onSynced(p);

            if (p instanceof Player pl) {
                //  pl.sendSystemMessage(Component.literal(id));
            }
        }
    }

    public void onSynced(Entity p) {

    }

}
