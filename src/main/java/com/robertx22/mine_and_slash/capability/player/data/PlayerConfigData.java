package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.UniqueSalvageHelper;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.database.data.profession.Profession;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ISalvagable;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage.AutoSalvageGenericConfigure;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Optional;

public class PlayerConfigData {

    public enum Config {

        CAST_FAIL("cast_fail_messages", true, Words.TITLE_FEATURE_CAST_FAIL, Words.CAST_FAIL_MSGS, false),
        MOB_DEATH_MESSAGES("mob_death_messages", false, Words.TITLE_FEATURE_MOB_KILL_LOOT, Words.MOB_DEATH_MESSAGES, false),
        EXP_CHAT_MESSAGES("exp_chat_messages", false, Words.TITLE_FEATURE_EXP_MSG, Words.EXP_CHAT_MESSAGES, false),
        DAMAGE_MESSAGES("damage_messages", false, Words.TITLE_FEATURE_DAMAGE_LOG, Words.DAMAGE_MESSAGES, false),
        PROFESSION_MESSAGES("profession_exp_messages", false, Words.TITLE_FEATURE_PROF_EXP, Words.PROFESSION_MESSAGES, false),
        AUTO_PVE("auto_pve", false, Words.TITLE_FEATURE_AUTO_TEAM, Words.AUTOMATIC_PVE, false),
        AGGRESSIVE_SUMMONS("aggressive_summons", true, Words.TITLE_FEATURE_AGGRO_SUMMONS, Words.AGGRESIVE_SUMMONS, false),
        ENABLE_EXP_GAIN("enable_exp_gain", true, Words.TITLE_FEATURE_ENABLE_EXP_GAIN, Words.ENABLE_EXP_GAIN, false),
        AUTO_SALVAGE_DROP("auto_salvage_drop", false, Words.TITLE_FEATURE_AUTO_SALVAGE_DROP, Words.AUTO_SALVAGE_DROP, false),
        AUTO_FIRE_BOWS("auto_fire_bows", true, Words.TITLE_FEATURE_AUTO_FIRE_BOWS, Words.AUTO_FIRE_BOWS, false),
        STAT_ORDER_TEST("stat_order_test", false, Words.TITLE_FEATURE_STAT_ORDER_DEBUG, Words.STAT_ORDER_TEST, true),
        DAMAGE_CONFLICT_MSG("damage_conflict_check", false, Words.TITLE_FEATURE_DMG_CONFLICT_DEBUG, Words.DMG_CONFLICT_CHECK, true),
        //EVERYONE_IS_ALLY("everyone_is_ally", false, Words.TITLE_FEATURE_EVERYONE_ALLY, Words.EVERYONE_IS_ALLY, false),
        //DROP_MAP_CHEST_CONTENTS_ON_GROUND("drop_map_chest_contents_on_ground", false, Words.TITLE_FEATURE_DROP_MAP_CHEST_ITEMS, Words.DROP_MAP_CHEST_CONTENTS_ON_GROUND, false)
        ;

        public String id;
        public Words title;

        public Words word;
        public boolean isDebug;
        public boolean enabledByDefault;


        Config(String id, boolean enabledByDefault, Words title, Words word, boolean isdebug) {
            this.id = id;
            this.title = title;
            this.word = word;
            this.isDebug = isdebug;
            this.enabledByDefault = enabledByDefault;
        }

    }


    public AutoSalvage salvage = new AutoSalvage();

    public HashMap<String, Boolean> configs = new HashMap<>();

    public boolean isConfigEnabled(Config id) {
        return configs.getOrDefault(id.id, false);
    }

    public void onLoginFillDefaults() {
        for (Config value : Config.values()) {
            if (!configs.containsKey(value.id)) {
                configs.put(value.id, ServerContainer.get().defaultFeatureConfigs.get(value).get());
            }
        }
    }


    public class AutoSalvage {

        //  private HashMap<ToggleAutoSalvageRarity.SalvageType, HashMap<String, Boolean>> map = new HashMap<>();
        // Salvage Type -> <rarity, enabled>
        private HashMap<ToggleAutoSalvageRarity.SalvageType, HashMap<String, Boolean>> map = new HashMap<>();

        // Salvage Type -> <ID, enabled>
        // Ex:
        // SalvageType.SPELL, <plus_aoe, disabled>
        // SalvageType.GEAR, <shield, enabled>

        // this configuration should take precedence over the rarity config because it's more specific
        private HashMap<ToggleAutoSalvageRarity.SalvageType, HashMap<String, Boolean>> tmap = new HashMap<>();

        // Gear Type id (BaseGearType GUID) -> <rarity, enabled>
        // Ex:
        // plate_chest, <rare, disabled>
        // sword, <common, enabled>
        //
        // this is the most specific configuration of them all, an absent entry means "inherit",
        // aka fall through to the less specific configs below it.
        private HashMap<String, HashMap<String, Boolean>> gtmap = new HashMap<>();

        // minimum sockets a runed base needs to survive auto salvage. 0 == filter off.
        // a primitive, so a save written before this field existed loads as 0, aka off,
        // with none of the null dance getGtMap() needs
        private int runed_min_sockets = 0;

        // map layout (dungeon_realm Dungeon GUID) -> filtered out. absent or false == keep.
        // deliberately not keyed by rarity, unlike gtmap: a filtered layout goes at any rarity
        private HashMap<String, Boolean> maplayouts = new HashMap<>();

        public HashMap<ToggleAutoSalvageRarity.SalvageType, HashMap<String, Boolean>> getTMap() {
            if (tmap == null) {
                tmap = new HashMap<>();
            }
            return tmap;
        }

        // old saves predate this field, gson leaves it null
        public HashMap<String, HashMap<String, Boolean>> getGtMap() {
            if (gtmap == null) {
                gtmap = new HashMap<>();
            }
            return gtmap;
        }

        public int getRunedMinSockets() {
            return runed_min_sockets;
        }

        public void setRunedMinSockets(int sockets) {
            this.runed_min_sockets = Mth.clamp(sockets, 0, GearRarity.maxRunedSockets());
        }

        // off -> 2 -> 3 -> ... -> the highest a runed rarity can roll -> off.
        // starts at 2 because the runed rarity's socket floor is 2, so a bar of 1 would cull nothing
        public void cycleRunedMinSockets() {
            int max = GearRarity.maxRunedSockets();

            if (max < 2) {
                return; // no rarity rolls enough sockets for the filter to mean anything
            }
            int next = runed_min_sockets < 2 ? 2 : runed_min_sockets + 1;

            runed_min_sockets = next > max ? 0 : next;
        }

        // a runed base under the socket threshold is culled before any other filter gets a say
        public boolean failsRunedSocketFilter(ISalvagable data) {
            if (runed_min_sockets < 1) {
                return false;
            }
            int count = data.getSocketFilterCount();

            return count > -1 && count < runed_min_sockets;
        }

        // old saves predate this field, gson leaves it null
        public HashMap<String, Boolean> getMapLayoutMap() {
            if (maplayouts == null) {
                maplayouts = new HashMap<>();
            }
            return maplayouts;
        }

        // a filtered layout is salvaged at every rarity, which is the whole point of the page
        public boolean isMapLayoutFiltered(String layoutId) {
            if (layoutId == null) {
                return false;
            }
            return getMapLayoutMap().getOrDefault(layoutId, false);
        }

        public void toggleMapLayoutFilter(String layoutId) {
            setMapLayoutFilter(layoutId, !isMapLayoutFiltered(layoutId));
        }

        public void setMapLayoutFilter(String layoutId, boolean filtered) {
            if (layoutId == null) {
                return;
            }
            if (filtered) {
                getMapLayoutMap().put(layoutId, true);
            } else {
                getMapLayoutMap().remove(layoutId); // keep is the default, so don't store it
            }
        }

        // todo test this
        public boolean trySalvageOnPickup(Player player, ItemStack stack) {


            ExileStack ex = ExileStack.of(stack);

            if (stack.isEnchanted()) {
                return false; // we don't want to auto salvage gear that is likely to have been worn or important
            }

            if (ex.get(StackKeys.DROPPED).hasAndTrue(x -> x.forcedDrop)) {
                return false; // don't auto salvage items that we forced the player to drop
            }

            ICommonDataItem<GearRarity> data = ICommonDataItem.load(stack);
            boolean doSalvage = false;

            if (data != null) {
                if (data.isSalvagable(ex)) {

                    if (failsRunedSocketFilter(data)) {
                        // too few sockets to ever be worth a runeword, so nothing else gets a vote
                        doSalvage = true;
                    } else if (isMapLayoutFiltered(data.getMapLayoutId(ex))) {
                        // a layout the player crossed off, at any rarity
                        doSalvage = true;
                    } else {
                        // most specific first: gear type + rarity, then the per id override, then the plain rarity config
                        Optional<Boolean> subFilterEnabled = checkGearTypeSalvageConfig(data.getSubFilterId(), data.getRarityId());

                        if (subFilterEnabled.isPresent()) {
                            doSalvage = subFilterEnabled.get();
                        } else {
                            Optional<Boolean> typeSalvageEnabled = checkTypeSalvageConfig(data.getSalvageType(), data.getSalvageConfigurationId());

                            if (typeSalvageEnabled.isEmpty()) {
                                if (checkRaritySalvageConfig(data.getSalvageType(), data.getRarityId())) {
                                    doSalvage = true;
                                }
                            } else {
                                doSalvage = typeSalvageEnabled.get();
                            }
                        }
                    }
                }

                if (doSalvage) {
                    SoundUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.75F, 1.25F);

                    // Give salvage experience to the player's salvaging profession (without rested exp bonus)
                    Profession salvagingProfession = ExileDB.Professions().get("salvaging"); // Adjust profession ID as needed
                    if (salvagingProfession != null) {
                        Load.player(player).professions.addExp(player, salvagingProfession.GUID(), data.getAutoSalvageExpReward(), false);
                    }

                    // consume the input BEFORE paying out. the other way round, anything that throws
                    // in the payout (the helper syncs and sends a packet) escapes before the shrink
                    // and leaves the player holding the item AND the shards.
                    boolean shouldAutoSalvageDrop = Load.player(player).config.isConfigEnabled(Config.AUTO_SALVAGE_DROP);
                    stack.shrink(stack.getCount() + 100);

                    // auto salvage pays full shards, but silently - bulk pickup would flood chat. only
                    // a first time unlock is worth interrupting the player for, and that is announced
                    // inside the helper regardless of this flag.
                    if (data instanceof GearItemData gear) {
                        UniqueSalvageHelper.onUniqueSalvaged(player, ex, gear, false);
                    }

                    data.getSalvageResult(ex).forEach(e -> {
                        Backpacks backpacks = Load.backpacks(player).getBackpacks();

                        if (shouldAutoSalvageDrop) {
                            PlayerUtils.spawnAtPlayer(e, player);
                            return;
                        }

                        if (backpacks.tryAutoPickup(player, e, false)) {
                            return;
                        }

                        PlayerUtils.giveItem(e, player);
                    });
                    return true;
                }

            }

            return false;
        }


        public boolean checkRaritySalvageConfig(ToggleAutoSalvageRarity.SalvageType type, String rar) {
            return map.getOrDefault(type, new HashMap<>()).getOrDefault(rar, false);
        }

        public Optional<Boolean> checkTypeSalvageConfig(ToggleAutoSalvageRarity.SalvageType type, String id) {
            if (id == null) {
                return Optional.empty();
            }

            if (!getTMap().containsKey(type)) {
                return Optional.empty();
            }

            if (getTMap().containsKey(type) && !getTMap().get(type).containsKey(id)) {
                return Optional.empty();
            }

            return Optional.of(tmap.get(type).get(id));
        }

        public void toggleRaritySalvageConfig(ToggleAutoSalvageRarity.SalvageType type, String rarity) {
            if (!map.containsKey(type)) {
                map.put(type, new HashMap<>());
            }
            var m2 = map.get(type);

            if (!m2.containsKey(rarity)) {
                m2.put(rarity, false);
            }

            boolean bool = m2.get(rarity);

            if (bool) {
                m2.put(rarity, false);
            } else {
                m2.put(rarity, true);
            }
        }

        public void setAutoSalvageForTypeAndId(ToggleAutoSalvageRarity.SalvageType salvageType, String id, AutoSalvageGenericConfigure.AutoSalvageConfigAction action) {

            if (action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.CLEAR) {
                if (!getTMap().containsKey(salvageType)) {
                    return;
                }
                var idMap = tmap.get(salvageType);
                idMap.remove(id);
                return;
            }

            if (!tmap.containsKey(salvageType)) {
                getTMap().put(salvageType, new HashMap<>());
            }

            getTMap().get(salvageType).put(id, action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.ENABLE);

        }

        public HashMap<String, Boolean> getConfiguredMapForSalvageType(ToggleAutoSalvageRarity.SalvageType salvageType) {
            return getTMap().getOrDefault(salvageType, new HashMap<>());
        }

        // an empty Optional means "inherit", aka this gear type has no opinion for this rarity
        public Optional<Boolean> checkGearTypeSalvageConfig(String gearTypeId, String rarityId) {
            if (gearTypeId == null || rarityId == null) {
                return Optional.empty();
            }
            var rarities = getGtMap().get(gearTypeId);

            if (rarities == null || !rarities.containsKey(rarityId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(rarities.get(rarityId));
        }

        // inherit -> salvage -> keep -> inherit
        public void cycleGearTypeSalvageConfig(String gearTypeId, String rarityId) {
            var current = checkGearTypeSalvageConfig(gearTypeId, rarityId);

            if (current.isEmpty()) {
                setGearTypeSalvageConfig(gearTypeId, rarityId, AutoSalvageGenericConfigure.AutoSalvageConfigAction.ENABLE);
            } else if (current.get()) {
                setGearTypeSalvageConfig(gearTypeId, rarityId, AutoSalvageGenericConfigure.AutoSalvageConfigAction.DISABLE);
            } else {
                setGearTypeSalvageConfig(gearTypeId, rarityId, AutoSalvageGenericConfigure.AutoSalvageConfigAction.CLEAR);
            }
        }

        // a null rarityId applies the action to every registered rarity
        public void setGearTypeSalvageConfig(String gearTypeId, String rarityId, AutoSalvageGenericConfigure.AutoSalvageConfigAction action) {
            if (gearTypeId == null) {
                return;
            }
            if (rarityId == null) {
                if (action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.CLEAR) {
                    clearGearTypeSalvageConfig(gearTypeId);
                    return;
                }
                for (GearRarity rar : ExileDB.GearRarities().getList()) {
                    setGearTypeSalvageConfig(gearTypeId, rar.GUID(), action);
                }
                return;
            }

            if (action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.CLEAR) {
                var rarities = getGtMap().get(gearTypeId);
                if (rarities != null) {
                    rarities.remove(rarityId);
                    if (rarities.isEmpty()) {
                        getGtMap().remove(gearTypeId);
                    }
                }
                return;
            }

            getGtMap().computeIfAbsent(gearTypeId, x -> new HashMap<>())
                    .put(rarityId, action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.ENABLE);
        }

        public void clearGearTypeSalvageConfig(String gearTypeId) {
            getGtMap().remove(gearTypeId);
        }

        public HashMap<String, Boolean> getConfiguredRaritiesForGearType(String gearTypeId) {
            return getGtMap().getOrDefault(gearTypeId, new HashMap<>());
        }

    }

}
