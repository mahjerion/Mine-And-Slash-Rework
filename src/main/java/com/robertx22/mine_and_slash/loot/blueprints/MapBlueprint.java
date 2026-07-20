package com.robertx22.mine_and_slash.loot.blueprints;


import com.robertx22.dungeon_realm.item.DungeonMapGenSettings;
import com.robertx22.dungeon_realm.item.DungeonMapItem;
import com.robertx22.mine_and_slash.database.data.map_affix.MapAffix;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.maps.MapAffixData;
import com.robertx22.mine_and_slash.maps.MapItemData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// todo when i generate a dungeon map from the vanilla mod, it should also generate mns map data
public class MapBlueprint extends RarityItemBlueprint {


    public MapBlueprint(LootInfo info) {
        super(info);
    }

    boolean uberMap = false;


    @Override
    public ItemStack generate() {

        return DungeonMapItem.newRandomMapItemStack(new DungeonMapGenSettings(), this.info.player);

        // todo need to fix this

        /*
        MapItemData data = createData();

        ItemStack stack = new ItemStack(DungeonEntries.DUNGEON_MAP_ITEM.get());

        var vanillaData = DungeonMapItem.randomNewMapData();

        if (uberMap) {
            vanillaData.uber = true;
        }

        StackSaving.MAP.saveTo(stack, data);
        DungeonItemNbt.DUNGEON_MAP.saveTo(stack, vanillaData);
        return stack;

         */

    }

    public MapItemData createData() {
        MapItemData data = new MapItemData();

        GearRarity rarity = (GearRarity) this.rarity.get();

        data.rar = rarity.GUID();

        data.tier = rarity.getPossibleMapTiers().random();

        genAffixes(data, rarity);

        return data;
    }

    public static void genAffixes(MapItemData map, GearRarity rarity) {

        map.affixes = new ArrayList<>();

        int amount = rarity.getAffixAmount();

        List<String> affixes = new ArrayList<String>();

        for (int i = 0; i < amount; i++) {

            MapAffix affix = ExileDB.MapAffixes().getFilterWrapped(x -> x.req.isEmpty()).random();

            while (affixes.contains(affix.GUID()) /*|| affix.isBeneficial()*/) {
                affix = ExileDB.MapAffixes().getFilterWrapped(x -> x.req.isEmpty()).random();
            }
            int percent = rarity.stat_percents.random();
            map.affixes.add(new MapAffixData(affix, percent));
            affixes.add(affix.GUID());

        }

    }

}
