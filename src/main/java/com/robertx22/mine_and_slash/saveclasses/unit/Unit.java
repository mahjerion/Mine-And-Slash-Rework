package com.robertx22.mine_and_slash.saveclasses.unit;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.registry.IWeighted;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.rarities.MobRarity;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.blood.Blood;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.blood.BloodUser;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.database.data.stats.types.special.BattlemageUser;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.vanilla_mc.packets.EfficientMobUnitPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// this stores data that can be lost without issue, stats that are recalculated all the time

public class Unit {

    public static Unit EMPTY = new Unit();


    private StatContainer stats = new StatContainer();


    public void toNbt(CompoundTag main) {
        CompoundTag nbt = new CompoundTag();
        int i = 0;
        for (StatData stat : stats.stats.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("v", stat.getValue());
            tag.putFloat("m", stat.getMoreStatTypeMulti());
            tag.putString("i", stat.getId());
            nbt.put("" + i, tag);
            i++;
        }
        main.put(SlashRef.MODID + "_unit", nbt);
        main.putInt(SlashRef.MODID + "_unit_sn", stats.stats.size());

    }

    public void fromNbt(CompoundTag main) {

        this.stats = new StatContainer();

        CompoundTag nbt = main.getCompound(SlashRef.MODID + "_unit");

        int num = main.getInt(SlashRef.MODID + "_unit_sn");

        for (int i = 0; i < num; i++) {
            CompoundTag tag = nbt.getCompound(i + "");
            String id = tag.getString("i");
            float val = tag.getFloat("v");
            float mul = tag.getFloat("m");
            StatData data = new StatData(id, val, mul);
            stats.stats.put(data.getId(), data);
        }

    }

    public boolean isBloodMage() {
        return getCalculatedStat(BloodUser.getInstance()).getValue() > 0;
    }

    public boolean isBattlemage() {
        return getCalculatedStat(BattlemageUser.getInstance()).getValue() > 0;
    }

    public void clearStats() {
        this.stats = new StatContainer();
    }

    public StatContainer getStats() {
        if (stats == null) {
            stats = new StatContainer();
        }

        return stats;
    }

    public void setStats(StatContainer c) {
        this.stats = c;
    }

    public StatData getCalculatedStat(Stat stat) {
        return getCalculatedStat(stat.GUID());
    }

    public StatData getCalculatedStat(String guid) {
        if (getStats().stats == null) {
            this.initStats();
        }
        return getStats().stats.getOrDefault(guid, new StatData(guid, 0, 1));
    }

    /*
    public StatData getOrCreateCalculatedStat(String guid) {
        if (getStats().stats == null) {
            this.initStats();
        }
        var data = getStats().stats.getOrDefault(guid, new StatData(guid, 0, 1));
        getStats().stats.put(guid, data);
        return data;
    }

     */

    public Unit() {

    }

    public void initStats() {
        getStats().stats = new HashMap<String, StatData>();
    }


    // Stat shortcuts
    public Health health() {
        return Health.getInstance();
    }

    public Mana mana() {
        return Mana.getInstance();
    }

    public StatData healthData() {
        try {
            return getCalculatedStat(Health.GUID);
        } catch (Exception e) {
        }
        return StatData.empty();
    }

    public StatData bloodData() {
        try {
            return getCalculatedStat(Blood.GUID);
        } catch (Exception e) {
        }
        return StatData.empty();
    }

    public StatData energyData() {
        try {
            return getCalculatedStat(Energy.GUID);
        } catch (Exception e) {

        }
        return StatData.empty();
    }

    public StatData magicShieldData() {
        try {
            return getCalculatedStat(MagicShield.GUID);
        } catch (Exception e) {

        }
        return StatData.empty();
    }

    public StatData manaData() {
        try {
            return getCalculatedStat(Mana.GUID);
        } catch (Exception e) {

        }
        return StatData.empty();
    }

    public String randomRarity(int lvl, EntityData data) {
        return randomRarity(lvl, data, 0, Collections.emptyMap());
    }

    public String randomRarity(int lvl, EntityData data, float densityBonusPercent) {
        return randomRarity(lvl, data, densityBonusPercent, Collections.emptyMap());
    }

    // densityBonusPercent (e.g. from the Atlas passive tree's mob_modifier_density stat) biases
    // the weighted roll toward non-common rarities without mutating the MobRarity registry itself.
    // perRarityBonusPercent (rarity GUID -> percent, e.g. the Atlas tree's per-rarity monster chance
    // stats) additionally biases the weight of a single specific rarity, stacking with the global bonus.
    public String randomRarity(int lvl, EntityData data, float densityBonusPercent, Map<String, Float> perRarityBonusPercent) {
        // if it's already set
        if (!data.getRarity().equals(IRarity.COMMON_ID) && ExileDB.MobRarities().isRegistered(data.getRarity())) {
            return data.getRarity();
        }
        List<MobRarity> rarities = ExileDB.MobRarities()
                .getList()
                .stream()
                .filter(x -> data.getLevel() >= x.minMobLevelForRandomSpawns() || data.getLevel() >= GameBalanceConfig.get().MAX_LEVEL)
                .collect(Collectors.toList());


        if (rarities.isEmpty()) {
            rarities.add(ExileDB.MobRarities().get(IRarity.COMMON_ID));
        }

        List<WeightedMobRarity> weighted = rarities.stream()
                .map(x -> new WeightedMobRarity(x, densityBonusPercent, perRarityBonusPercent))
                .collect(Collectors.toList());

        MobRarity finalRarity = RandomUtils.weightedRandom(weighted).rarity;


        return finalRarity.GUID();

    }

    private static class WeightedMobRarity implements IWeighted {
        final MobRarity rarity;
        final float bonusPercent;
        final Map<String, Float> perRarityBonusPercent;

        WeightedMobRarity(MobRarity rarity, float bonusPercent, Map<String, Float> perRarityBonusPercent) {
            this.rarity = rarity;
            this.bonusPercent = bonusPercent;
            this.perRarityBonusPercent = perRarityBonusPercent;
        }

        @Override
        public int Weight() {
            if (rarity.GUID().equals(IRarity.COMMON_ID)) {
                return rarity.Weight();
            }
            float total = bonusPercent + perRarityBonusPercent.getOrDefault(rarity.GUID(), 0F);
            if (total <= 0) {
                return rarity.Weight();
            }
            return (int) (rarity.Weight() * (1F + total / 100F));
        }
    }


    public static boolean shouldSendUpdatePackets(LivingEntity en) {
        if (ServerContainer.get().DONT_SYNC_DATA_OF_AMBIENT_MOBS.get()) {
            if (en.getType().getCategory() == MobCategory.AMBIENT || en.getType().getCategory() == MobCategory.WATER_AMBIENT) {
                return false;
            }
        }
        return true;
    }

    public static MyPacket getUpdatePacketFor(LivingEntity en, EntityData data) {
        return new EfficientMobUnitPacket(en, data);
    }

}
