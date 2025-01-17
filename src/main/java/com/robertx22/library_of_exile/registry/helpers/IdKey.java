package com.robertx22.library_of_exile.registry.helpers;

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
