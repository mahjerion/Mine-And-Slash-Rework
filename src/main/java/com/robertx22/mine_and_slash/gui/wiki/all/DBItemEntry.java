package com.robertx22.mine_and_slash.gui.wiki.all;

import com.robertx22.mine_and_slash.gui.wiki.BestiaryEntry;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryGroup;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class DBItemEntry<T> extends BestiaryGroup<T> {

    ExileRegistryType type;
    Words word;
    String id;
    public Function<T, BestiaryEntry> maker;

    // optional, null means every registry entry is listed
    private Predicate<T> visibleIf = null;

    public DBItemEntry(ExileRegistryType type, Words word, String id, Function<T, BestiaryEntry> maker) {
        this.type = type;
        this.word = word;
        this.id = id;
        this.maker = maker;
    }

    public DBItemEntry<T> visibleIf(Predicate<T> pred) {
        this.visibleIf = pred;
        return this;
    }

    @Override
    public List<BestiaryEntry> getAll(int lvl) {
        List<BestiaryEntry> list = new ArrayList<>();
        for (Object o : Database.getRegistry(type).getList()) {
            T t = (T) o;
            if (visibleIf != null && !visibleIf.test(t)) {
                continue;
            }
            list.add(maker.apply(t));
        }
        return list;
    }

    @Override
    public Component getName() {
        return word.locName();
    }

    
    @Override
    public String texName() {
        return id;
    }


}
