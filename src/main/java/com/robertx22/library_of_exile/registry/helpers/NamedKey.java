package com.robertx22.library_of_exile.registry.helpers;

public class NamedKey extends IdKey {

    public String name;


    public NamedKey(String id, String name) {
        super(id);
        this.name = name;
    }
}
