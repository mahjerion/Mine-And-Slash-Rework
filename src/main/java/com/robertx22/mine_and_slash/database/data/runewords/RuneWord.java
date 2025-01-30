package com.robertx22.mine_and_slash.database.data.runewords;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.RuneItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class RuneWord implements IAutoGson<RuneWord>, JsonExileRegistry<RuneWord>, IAutoLocName {
    public static RuneWord SERIALIZER = new RuneWord();

    public String id = "";
    public transient String name = "";
    public List<StatMod> stats = new ArrayList<>();
    public List<String> runes = new ArrayList<>();
    public List<String> slots = new ArrayList<>();

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.RUNEWORDS;
    }

    @Override
    public String GUID() {
        return id;
    }


    // todo add this to api
    public boolean isEmpty() {
        return ExileDB.RuneWords().getDefault() != null && ExileDB.RuneWords().getDefault().GUID().equals(GUID());
    }

    @Override
    public Class<RuneWord> getClassForSerialization() {
        return RuneWord.class;
    }

    // todo
    public static String join(Iterator<?> iterator, String separator) {
        if (separator == null) {
            separator = "";
        }
        StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        while (iterator.hasNext()) {
            buf.append(iterator.next());
            if (iterator.hasNext()) {
                buf.append(separator);
            }
        }
        return buf.toString();
    }

    public boolean hasMatchingRunesToCreate(GearItemData gear) {

        var list = gear.sockets.getSocketed().stream().map(x -> x.g).collect(Collectors.toList());

        String reqString = getRunesString();
        String testString = join(list.listIterator(), "");

        return testString.contains(reqString);

    }

    public String getRunesString() {
        return join(runes.listIterator(), "");
    }


    boolean isRuneItem(String id, ItemStack stack) {
        return stack.getItem() instanceof RuneItem rune && rune.type.id.equals(id);
    }

    public boolean canApplyOnItem(ItemStack stack) {
        if (slots.stream().anyMatch(e -> {
            return GearSlot.isItemOfThisSlot(ExileDB.GearSlots().get(e), stack);
        })) {
            return true;
        }

        return false;
    }

    public boolean canApplyOnItem(GearItemData gear) {
        return slots.stream().anyMatch(x -> gear.GetBaseGearType().gear_slot.equals(x));
    }


    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Rune_Words;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".runeword." + id;
    }

    @Override
    public String locNameForLangFile() {
        return name;
    }
}
