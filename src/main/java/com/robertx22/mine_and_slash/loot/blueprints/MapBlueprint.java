package com.robertx22.mine_and_slash.loot.blueprints;


import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.item.DungeonMapGenSettings;
import com.robertx22.dungeon_realm.item.DungeonMapItem;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.map_affix.MapAffix;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MapRarityBias;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.maps.MapAffixData;
import com.robertx22.mine_and_slash.maps.MapItemData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// todo when i generate a dungeon map from the vanilla mod, it should also generate mns map data
public class MapBlueprint extends RarityItemBlueprint {


    public MapBlueprint(LootInfo info) {
        super(info);
    }

    boolean uberMap = false;

    // whether this map may inherit a tier floor from the map the player is standing in. Loot is the
    // default consumer so this defaults to true; non-loot generators (the Map Creator item) turn it
    // off so a crafted item can't be used inside a high tier map to mint a high tier map.
    public boolean inheritMapTier = true;

    // the map boss's reward drop, which uses the stricter floor band.
    public boolean fromMapBoss = false;


    @Override
    public ItemStack generate() {

        // createData() is re-run by the dungeon mod's ON_GENERATE_NEW_MAP_ITEM handler on a *fresh*
        // blueprint, so fields set on this instance don't survive into it - they have to ride along
        // on the settings object. generate() itself does run on our own instance (ItemBlueprint.createStack).
        DungeonMapGenSettings settings = new DungeonMapGenSettings();
        settings.inheritMapTier = this.inheritMapTier;
        settings.fromMapBoss = this.fromMapBoss || isFinalMapBossKill();

        return DungeonMapItem.newRandomMapItemStack(settings, this.info.player);

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

    // The map boss's ordinary loot gets the same tight ascending band as the dedicated
    // BONUS_MAP_ITEM_FROM_BOSS drop. Without this the only map in the whole run that can exceed the
    // run's own tier is that one drop, which is a 25% roll - so three quarters of completed maps
    // would produce nothing above their own tier and the ladder would barely move.
    // The capability lookup allocates on a miss, so it's guarded to the rare case of a map actually
    // dropping inside a map, not run per mob kill.
    private boolean isFinalMapBossKill() {

        if (!inheritMapTier || info.map_tier <= 0 || info.mobKilled == null) {
            return false;
        }

        try {
            return DungeonEntityCapability.get(info.mobKilled).data.isFinalMapBoss;
        } catch (Exception e) {
            return false;
        }
    }

    public MapItemData createData() {
        MapItemData data = new MapItemData();

        // Atlas map_rarity_bias raises the chance the rolled map upgrades to the next rarity tier
        // (GearRarityPart's higher-rarity roll). Only the dedicated stat applies here - not general
        // magic find - so map rarity stays gated behind the Atlas rather than scaling with gear.
        if (info.playerEntityData != null) {
            this.rarity.chanceForHigherRarity += info.playerEntityData.getUnit().getCalculatedStat(MapRarityBias.getInstance()).getValue();
        }

        GearRarity rarity = (GearRarity) this.rarity.get();

        data.rar = rarity.GUID();

        data.tier = rarity.getPossibleMapTiers().random();

        genAffixes(data, rarity);

        applyTierFloor(data);

        return data;
    }

    // Without this, tier is a pure dependent of a heavily weighted rarity roll (Common weight 5000 vs
    // Mythic 25), so clearing a T90 map drops the same T0-T10 junk a fresh character finds in the
    // overworld. The floor makes a map run sustain its own tier, and the boss drop pushes it upward.
    // A lucky natural roll still wins, so the rare Mythic jackpot survives untouched.
    private void applyTierFloor(MapItemData data) {

        if (!inheritMapTier || info.map_tier <= 0) {
            return;
        }

        var config = ServerContainer.get();

        int falloff = fromMapBoss ? config.MAP_BOSS_TIER_FALLOFF.get() : config.MAP_TIER_DROP_FALLOFF.get();
        int rise = fromMapBoss ? config.MAP_BOSS_TIER_RISE.get() : config.MAP_TIER_DROP_RISE.get();

        int low = Math.max(0, info.map_tier - falloff);
        int high = Math.max(low, info.map_tier + rise);

        int floor = RandomUtils.RandomRange(low, high);

        if (floor > data.tier) {
            // setTier re-derives rarity from the band containing the new tier and tops the affixes up,
            // so the map stays internally consistent for everything that reads getRarity().
            data.setTier(floor);
        }
    }

    public static void genAffixes(MapItemData map, GearRarity rarity) {
        map.affixes = new ArrayList<>();
        reconcileAffixes(map, rarity);
    }

    // Bring the affix count in line with the rarity without disturbing affixes that are already
    // rolled. Used whenever a map's rarity changes after generation (the tier floor snapping it into
    // a higher band, or the Orb of Map Rarity) - a straight reroll would throw away the affixes the
    // player already read off the item.
    public static void reconcileAffixes(MapItemData map, GearRarity rarity) {

        if (map.affixes == null) {
            map.affixes = new ArrayList<>();
        }

        int amount = rarity.getAffixAmount();

        while (map.affixes.size() > amount) {
            map.affixes.remove(map.affixes.size() - 1);
        }

        List<String> affixes = map.affixes.stream().map(x -> x.id).collect(Collectors.toList());

        var possible = ExileDB.MapAffixes().getFilterWrapped(x -> x.req.isEmpty()).list;

        while (map.affixes.size() < amount) {

            // bail out instead of spinning forever if the pool is smaller than the required count
            if (affixes.size() >= possible.size()) {
                break;
            }

            MapAffix affix = RandomUtils.weightedRandom(possible);

            while (affixes.contains(affix.GUID()) /*|| affix.isBeneficial()*/) {
                affix = RandomUtils.weightedRandom(possible);
            }
            int percent = rarity.stat_percents.random();
            map.affixes.add(new MapAffixData(affix, percent));
            affixes.add(affix.GUID());
        }
    }

}
