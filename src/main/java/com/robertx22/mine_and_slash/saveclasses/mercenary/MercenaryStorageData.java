package com.robertx22.mine_and_slash.saveclasses.mercenary;

import com.robertx22.mine_and_slash.aoe_data.database.mercenaries.Mercenaries;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.registry.ExileDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Every mercenary a character has, keyed by class id, plus which one is currently out.
 * <p>
 * Loadouts are per mercenary (the design's ESO style companions), so switching class swaps the whole
 * gear + gem set rather than carrying one shared loadout across.
 */
public class MercenaryStorageData {

    public HashMap<String, MercenaryData> map = new HashMap<>();

    /** class id of the mercenary currently summoned, or empty for none */
    public String active = "";

    // whether this character has already been told mercenaries unlocked. persisted rather than
    // derived so the line is said exactly once no matter how the level was reached - levelling,
    // a command, an import, or the unlock level being lowered under a character that already
    // qualified. CharacterData swaps the whole MercenaryStorageData, so this is per character.
    public boolean unlock_announced = false;

    // the player turned mercenaries off entirely. not on MercenaryData, which is per mercenary
    // class - the switch has to put every one of them away, not just whichever is active. per
    // character for the same reason as the flag above.
    public boolean disabled = false;

    // the live entity and the countdown to its return. transient on purpose: the mercenary entity is
    // never written to the world save, so neither of these means anything across a reload - it is
    // respawned from the data above instead, and an orphan can't be left behind.
    public transient UUID spawnedId = null;
    public transient int respawnTimer = 0;

    // how long the mercenary has been owed but absent. one int per player per tick, and the only
    // reason it exists is that a mercenary which never arrives used to produce no log line at all -
    // MercenaryManager warns once when this passes its threshold.
    public transient int blockedTicks = 0;

    /**
     * Mercenaries are created on demand rather than seeded, so a datapack adding a class doesn't
     * need a migration and a character that never opened the screen carries no data.
     */
    public MercenaryData getOrCreate(String classId) {
        if (classId == null || classId.isEmpty()) {
            classId = Mercenaries.FIGHTER;
        }
        MercenaryData data = map.get(classId);
        if (data == null) {
            data = new MercenaryData(classId);
            map.put(classId, data);
        }
        // a save written before class_id existed, or one hand edited - keep the key authoritative
        data.class_id = classId;
        return data;
    }

    public MercenaryData getActive() {
        return getOrCreate(getActiveId());
    }

    public String getActiveId() {
        if (active == null || active.isEmpty() || !ExileDB.Mercenaries().isRegistered(active)) {
            // the stored class can vanish when a datapack drops it. fall back rather than handing
            // back a dangling id that every caller would then have to null check.
            return firstRegisteredId();
        }
        return active;
    }

    public void setActive(String classId) {
        this.active = classId == null ? "" : classId;
    }

    /**
     * What a character gets before it has ever chosen: the fighter, matching {@link #getOrCreate}'s
     * own fallback. Not simply the first of {@link #getAllClasses()} - that list is sorted by id, so
     * "elementalist" won and a brand new character was quietly handed a squishy ranged companion.
     * The sorted order stays as the fallback for a datapack that removed the fighter.
     */
    private String firstRegisteredId() {
        if (ExileDB.Mercenaries().isRegistered(Mercenaries.FIGHTER)) {
            return Mercenaries.FIGHTER;
        }
        List<MercenaryClass> all = getAllClasses();
        return all.isEmpty() ? Mercenaries.FIGHTER : all.get(0).GUID();
    }

    /** every registered class, in a stable order, so the gui's left/right arrows don't jump around */
    public static List<MercenaryClass> getAllClasses() {
        List<MercenaryClass> list = new ArrayList<>(ExileDB.Mercenaries().getList());
        list.sort((a, b) -> a.GUID().compareTo(b.GUID()));
        return list;
    }
}
