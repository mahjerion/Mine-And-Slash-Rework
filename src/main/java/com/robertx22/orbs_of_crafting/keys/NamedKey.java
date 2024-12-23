package com.robertx22.orbs_of_crafting.keys;

public class NamedKey extends IdKey {

    public String name;


    public NamedKey(String id, String name) {
        super(id);
        this.name = name;
    }
}
