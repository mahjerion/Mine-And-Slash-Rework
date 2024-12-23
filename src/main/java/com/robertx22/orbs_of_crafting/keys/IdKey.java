package com.robertx22.orbs_of_crafting.keys;

public class IdKey extends KeyInfo {

    String id;

    public IdKey(String id) {
        this.id = id;
    }

    @Override
    public String GUID() {
        return id;
    }
}
