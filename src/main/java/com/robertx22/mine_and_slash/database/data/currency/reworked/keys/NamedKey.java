package com.robertx22.mine_and_slash.database.data.currency.reworked.keys;

public class NamedKey extends IdKey {

    public String name;


    public NamedKey(String id, String name) {
        super(id);
        this.name = name;
    }
}
