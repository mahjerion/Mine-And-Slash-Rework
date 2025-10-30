package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.profession.Profession;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage.AutoSalvageGenericConfigure;
import net.minecraft.sounds.SoundEvents;
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

        public HashMap<ToggleAutoSalvageRarity.SalvageType, HashMap<String, Boolean>> getTMap() {
            if (tmap == null) {
                tmap = new HashMap<>();
            }
            return tmap;
        }

        // todo test this
        public boolean trySalvageOnPickup(Player player, ItemStack stack) {


            ExileStack ex = ExileStack.of(stack);

            if (stack.isEnchanted()) {
                return false; // we don't want to auto salvage gear that is likely to have been worn or important
            }

            ICommonDataItem<GearRarity> data = ICommonDataItem.load(stack);
            boolean doSalvage = false;

            if (data != null) {
                if (data.isSalvagable(ex)) {

                    Optional<Boolean> typeSalvageEnabled = checkTypeSalvageConfig(data.getSalvageType(), data.getSalvageConfigurationId());

                    if (typeSalvageEnabled.isEmpty()) {
                        if (checkRaritySalvageConfig(data.getSalvageType(), data.getRarityId())) {
                            doSalvage = true;
                        }
                    } else {
                        doSalvage = typeSalvageEnabled.get();
                    }
                }

                if (doSalvage) {
                    SoundUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.75F, 1.25F);

                    // Give salvage experience to the player's salvaging profession (without rested exp bonus)
                    Profession salvagingProfession = ExileDB.Professions().get("salvaging"); // Adjust profession ID as needed
                    if (salvagingProfession != null) {
                        Load.player(player).professions.addExp(player, salvagingProfession.GUID(), data.getAutoSalvageExpReward(), false);
                    }

                    boolean shouldAutoSalvageDrop = Load.player(player).config.isConfigEnabled(Config.AUTO_SALVAGE_DROP);
                    stack.shrink(stack.getCount() + 100);
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

    }

}
