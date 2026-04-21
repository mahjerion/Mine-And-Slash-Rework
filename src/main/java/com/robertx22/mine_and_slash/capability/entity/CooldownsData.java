package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.database.registry.ExileDB;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CooldownsData {

    public static String IN_COMBAT = "in_combat";

    private HashMap<String, Data> map = new HashMap<>();

    public static class Data {
        public int ticks = 0;

        public int need = 0;

        public Data() {
        }

        public Data(int ticks, int need) {
            this.ticks = ticks;
            this.need = need;
        }
    }

    public void tickSpellCooldowns(int ticks) {
        map.values().removeIf(data -> {
            data.ticks -= ticks;
            return data.ticks < 1;
        });
    }

    public void tickDownCooldown(String id, int ticks) {
        Data data = map.get(id);
        if (data != null) {
            data.ticks -= ticks;

            if (data.ticks < 1) {
                map.remove(id);
            }
        }
    }

    public void onTicksPass(int ticks) {
        map.values().removeIf(data -> {
            data.ticks -= ticks;
            return data.ticks < 1;
        });
    }

    public List<String> getAllSpellsOnCooldown() {
        return map.keySet()
                .stream()
                .filter(x -> ExileDB.Spells()
                        .isRegistered(x))
                .collect(Collectors.toList());
    }

    public void setOnCooldown(String id, int ticks) {
        map.put(id, new Data(ticks, ticks));
    }

    public int getCooldownTicks(String id) {
        return map.getOrDefault(id, new Data(0, 0)).ticks;
    }

    public int getNeededTicks(String id) {
        return map.getOrDefault(id, new Data(0, 0)).need;
    }

    public boolean isOnCooldown(String id) {
        return getCooldownTicks(id) > 0;
    }

    public boolean runIfNoCooldownAndSet(String id, int cdticks, Runnable run) {
        if (!isOnCooldown(id)) {
            run.run();
            setOnCooldown(id, cdticks);
            return true;
        }
        return false;
    }

}
