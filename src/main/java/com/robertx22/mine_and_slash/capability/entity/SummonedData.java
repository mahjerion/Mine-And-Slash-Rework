package com.robertx22.mine_and_slash.capability.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummonedData {
    private Map<String, List<UUID>> summonedTypes = new HashMap<>();

    public int getSummonedAmount(String spell) {
        if (!summonedTypes.containsKey(spell)) {
            return 0;
        }

        return summonedTypes.get(spell).size();
    }

    public void setSummons(String spell, List<UUID> summons) {
        summonedTypes.put(spell, summons);
    }

    public boolean isOwnBySpell(String spell, UUID uuid) {
        if (!summonedTypes.containsKey(spell)) {
            return false;
        }

        return summonedTypes.get(spell).contains(uuid);
    }

    public boolean removeSummon(String spell, UUID uuid) {
        if (!summonedTypes.containsKey(spell)) {
            return false;
        }

        summonedTypes.get(spell).remove(uuid);
        return true;
    }

    public boolean removeSummonType(String spell) {
        if (!summonedTypes.containsKey(spell)) {
            return false;
        }

        summonedTypes.remove(spell);
        return true;
    }
}
