package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiConfigToggle;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.PickSpellAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.OpenGearSubFilterAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.OpenMapLayoutFilterAction;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ResetGearTypeSalvage;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleGearTypeSalvage;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellCastingData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuiInventoryGrids {


    public static InvGuiGrid ofSelectableSpells(Player p, int slot) {
        GuiAction.regenActionMap(); //  todo find better way of ensuring

        List<GuiItemData> list = new ArrayList<>();

        PickSpellAction.SLOT = slot;

        /*
        for (Spell spell : ExileDB.Spells().getFilterWrapped(x -> x.config.hotbarUsable && x.getLevelOf(p) > 0).list) {
            list.add(new GuiItemData(new PickSpellAction(spell)));
        }
        */

        // todo stopped this feature because then i'd be using up hotbar slots with spells that cant be casted by themselves..
        for (SpellCastingData.InsertedSpell spell : Load.player(p).spellCastingData.spells) {
            list.add(new GuiItemData(new PickSpellAction(spell.getData().getSpell())));
        }


        return InvGuiGrid.ofList(list);
    }


    // the rarity columns, leaving the last grid column free for the per type filter / row header button
    private static List<GearRarity> filterRarities() {
        var rarities = ExileDB.GearRarities().getList();
        rarities.sort(Comparator.comparingInt(x -> x.item_tier));

        // ofYRowLists truncates silently, so cut here instead of losing a column without a word
        if (rarities.size() > InvGuiGrid.X_MAX - 1) {
            return rarities.subList(0, InvGuiGrid.X_MAX - 1);
        }
        return rarities;
    }

    public static InvGuiGrid ofSalvageConfig() {
        GuiAction.regenActionMap(); //  todo find better way of ensuring

        // linked, enum hash codes are identity based so a HashMap would shuffle the rows every launch
        Map<ToggleAutoSalvageRarity.SalvageType, List<GuiItemData>> map = new LinkedHashMap<>();

        var rarities = filterRarities();

        for (ToggleAutoSalvageRarity.SalvageType type : ToggleAutoSalvageRarity.SalvageType.values()) {

            if (!map.containsKey(type)) {
                map.put(type, new ArrayList<>());
            }
            for (GearRarity rar : rarities) {
                map.get(type).add(new GuiItemData(new ToggleAutoSalvageRarity(type, rar)));
            }
            // gear backed rows get a button into their per gear type filters
            if (OpenGearSubFilterAction.TYPES.contains(type)) {
                map.get(type).add(new GuiItemData(new OpenGearSubFilterAction(type)));
            }
            // maps get their own page, keyed by layout rather than by rarity
            else if (type == ToggleAutoSalvageRarity.SalvageType.MAP) {
                map.get(type).add(new GuiItemData(new OpenMapLayoutFilterAction()));
            }
        }
        List<List<GuiItemData>> lists = new ArrayList<>();

        for (Map.Entry<ToggleAutoSalvageRarity.SalvageType, List<GuiItemData>> en : map.entrySet()) {
            lists.add(en.getValue());
        }

        return InvGuiGrid.ofYRowLists(lists);
    }

    // the gear types shown under one salvage type row, grouped the same way the pickup check groups them.
    // sorted by id so cloth_/leather_/plate_ variants sit together
    public static List<BaseGearType> subFilterGearTypes(ToggleAutoSalvageRarity.SalvageType type) {
        return ExileDB.GearTypes().getList().stream()
                .filter(x -> GearItemData.salvageTypeOf(x) == type)
                .sorted(Comparator.comparing(BaseGearType::GUID))
                .collect(Collectors.toList());
    }

    public static int subFilterPageCount(ToggleAutoSalvageRarity.SalvageType type) {
        int size = subFilterGearTypes(type).size();
        return Math.max(1, (int) Math.ceil(size / (double) InvGuiGrid.Y_MAX));
    }

    public static InvGuiGrid ofGearSubFilter(ToggleAutoSalvageRarity.SalvageType type, int page) {
        GuiAction.regenActionMap(); //  todo find better way of ensuring

        var rarities = filterRarities();
        var types = subFilterGearTypes(type);

        List<List<GuiItemData>> lists = new ArrayList<>();

        int from = page * InvGuiGrid.Y_MAX;

        for (int i = from; i < Math.min(from + InvGuiGrid.Y_MAX, types.size()); i++) {
            BaseGearType gt = types.get(i);

            List<GuiItemData> row = new ArrayList<>();
            row.add(new GuiItemData(new ResetGearTypeSalvage(gt)));

            for (GearRarity rar : rarities) {
                row.add(new GuiItemData(new ToggleGearTypeSalvage(gt, rar)));
            }
            lists.add(row);
        }

        return InvGuiGrid.ofYRowLists(lists);
    }

    public static InvGuiGrid ofConfigs() {
        GuiAction.regenActionMap(); //  todo find better way of ensuring

        List<GuiItemData> lists = new ArrayList<>();

        for (PlayerConfigData.Config v : Arrays.stream(PlayerConfigData.Config.values()).filter(x -> !x.isDebug).collect(Collectors.toList())) {
            lists.add(new GuiItemData(new GuiConfigToggle(v)));
        }

        return InvGuiGrid.ofYRowLists(Arrays.asList(lists));
    }
}
