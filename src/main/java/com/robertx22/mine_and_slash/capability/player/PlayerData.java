package com.robertx22.mine_and_slash.capability.player;

import com.robertx22.library_of_exile.components.ICap;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.packets.SyncPlayerCapToClient;
import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.a_libraries.curios.RefCurios;
import com.robertx22.mine_and_slash.capability.CapNbtCache;
import com.robertx22.mine_and_slash.capability.DirtySync;
import com.robertx22.mine_and_slash.capability.entity.SummonedData;
import com.robertx22.mine_and_slash.capability.player.data.*;
import com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.characters.CharStorageData;
import com.robertx22.mine_and_slash.database.data.omen.OmenData;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.event_hooks.my_events.CachedPlayerStats;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatCalcInfoData;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.prophecy.PlayerProphecies;
import com.robertx22.mine_and_slash.saveclasses.atlas.AtlasData;
import com.robertx22.mine_and_slash.saveclasses.perks.TalentsData;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellCastingData;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellSchoolsData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_calc.StatCalculation;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class PlayerData implements ICap {


    public static final ResourceLocation RESOURCE = new ResourceLocation(SlashRef.MODID, "player_data");
    public static Capability<PlayerData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static PlayerData get(LivingEntity entity) {
        return entity.getCapability(INSTANCE)
                .orElse(null);
    }

    transient final LazyOptional<PlayerData> supp = LazyOptional.of(() -> this);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INSTANCE) {
            return supp.cast();
        }
        return LazyOptional.empty();

    }


    @Override
    public void syncToClient(Player player) {

    }


    private static final String TEAM_DATA = "teams";
    private static final String PROPHECY = "proph";
    private static final String TALENTS_DATA = "tals";
    private static final String STAT_POINTS = "stats";
    private static final String DEATH_STATS = "death";
    private static final String GEMS = "gems";
    private static final String AURAS = "auras";
    private static final String MAP = "map";
    private static final String ASC = "asc";
    private static final String CAST = "casting";
    private static final String CONFIG = "config";
    private static final String JEWELS = "jewels";
    private static final String FAVOR = "favor";
    private static final String PROFESSIONS = "profs";
    private static final String BUFFS = "buffs";
    private static final String RESTED_XP = "rxp";
    private static final String NAME = "name";
    private static final String CHARACTERS = "chars";
    private static final String BONUS_TALENTS = "btal";
    private static final String POINTS = "points";
    private static final String MISC_INFO = "minfo";
    private static final String OMENS_FILLED = "ofi";
    private static final String SUMMONED = "summoned";
    private static final String ATLAS_DATA = "atlas";
    private static final String CHAR_EQUIPMENT = "chareq";

    public DirtySync playerDataSync = new DirtySync("playerdata_sync", x -> syncData());

    public transient Player player;


    public transient StatCalcInfoData ctxs = new StatCalcInfoData();

    // so players know where their stats come from in the future gui
    //ublic SavedStatCtxList ctxStats = new SavedStatCtxList();

    public TeamData team = new TeamData();
    public TalentsData talents = new TalentsData();
    public StatPointsData statPoints = new StatPointsData();
    public SpellSchoolsData ascClass = new SpellSchoolsData();
    public PlayerProphecies prophecy = new PlayerProphecies();
    public SpellCastingData spellCastingData = new SpellCastingData();
    public PlayerConfigData config = new PlayerConfigData();
    public DeathFavorData favor = new DeathFavorData();
    public PlayerProfessionsData professions = new PlayerProfessionsData();
    public PlayerBuffData buff = new PlayerBuffData();
    public RestedExpData rested_xp = new RestedExpData();
    public PlayerPointsData points = new PlayerPointsData();
    public MiscSyncData miscInfo = new MiscSyncData();
    public AtlasData atlas = new AtlasData();
    public JewelData jewelData;

    private MyInventory skillGemInv = new MyInventory(GemInventoryHelper.TOTAL_SLOTS);
    private MyInventory auraInv = new MyInventory(GemInventoryHelper.TOTAL_AURAS);

    public CharStorageData characters = new CharStorageData();

    public List<String> aurasOn = new ArrayList<>();
    SummonedData summonedData = new SummonedData();


    public int bonusTalents = 0;


    public int omensFilled = 0;

    public PlayerData(Player player) {
        this.player = player;
        this.cachedStats = new CachedPlayerStats(player);
        this.jewelData = new JewelData(player);
    }


    public CachedPlayerStats cachedStats;

    public JewelData getJewels() {
        return jewelData;
    }

    // 17 gson passes plus 4 inventory createTag calls, which anything reading the player's nbt was
    // paying for every tick. keyed off playerDataSync rather than a second set of dirty hooks - the
    // jewel and character equipment inventories get swapped out at runtime, so listeners on them
    // would silently detach. OnServerTick force marks playerDataSync dirty every 3 seconds anyway,
    // so anything this misses can only be stale for that long, and saves force a rebuild outright.
    private transient final CapNbtCache nbtCache = new CapNbtCache();

    public CapNbtCache getNbtCache() {
        return nbtCache;
    }

    @Override
    public CompoundTag serializeNBT() {
        return nbtCache.get(player, playerDataSync.getVersion(), this::buildNBT);
    }

    private CompoundTag buildNBT() {

        CompoundTag nbt = new CompoundTag();

        LoadSave.Save(team, nbt, TEAM_DATA);
        LoadSave.Save(talents, nbt, TALENTS_DATA);
        LoadSave.Save(prophecy, nbt, PROPHECY);
        LoadSave.Save(statPoints, nbt, STAT_POINTS);
        LoadSave.Save(ascClass, nbt, ASC);
        LoadSave.Save(spellCastingData, nbt, CAST);
        LoadSave.Save(config, nbt, CONFIG);
        LoadSave.Save(favor, nbt, FAVOR);
        LoadSave.Save(professions, nbt, PROFESSIONS);
        LoadSave.Save(buff, nbt, BUFFS);
        LoadSave.Save(rested_xp, nbt, RESTED_XP);
        LoadSave.Save(characters, nbt, CHARACTERS);
        LoadSave.Save(points, nbt, POINTS);
        LoadSave.Save(miscInfo, nbt, MISC_INFO);
        LoadSave.Save(summonedData, nbt, SUMMONED);
        LoadSave.Save(atlas, nbt, ATLAS_DATA);

        // stored character gear can't ride along in the CHARACTERS json - LoadSave is gson, and an
        // ItemStack won't survive that. keyed by character slot, but it's owned by the CharacterData
        // object, so deleting a character takes its gear with it and no orphan can be left at an index
        // that tryAddNewCharacter later reuses.
        CompoundTag charEquipment = new CompoundTag();
        characters.map.forEach((num, character) -> {
            if (character != null) {
                charEquipment.put(String.valueOf(num), character.getEquipment().createTag());
            }
        });
        nbt.put(CHAR_EQUIPMENT, charEquipment);

        // LoadSave.Save(ctxStats, nbt, "ctx");

        nbt.put(GEMS, skillGemInv.createTag());
        nbt.put(AURAS, auraInv.createTag());
        //nbt.put(JEWELS, jewelsInv.createTag());
        nbt.put(JEWELS, jewelData.jewelInventory.createTag());

        nbt.putInt(BONUS_TALENTS, bonusTalents);
        nbt.putInt(OMENS_FILLED, omensFilled);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        // anything cached was built before this data existed
        nbtCache.markDirty();

        this.team = loadOrBlank(TeamData.class, new TeamData(), nbt, TEAM_DATA, new TeamData());
        this.prophecy = loadOrBlank(PlayerProphecies.class, new PlayerProphecies(), nbt, PROPHECY, new PlayerProphecies());
        this.talents = loadOrBlank(TalentsData.class, new TalentsData(), nbt, TALENTS_DATA, new TalentsData());
        this.statPoints = loadOrBlank(StatPointsData.class, new StatPointsData(), nbt, STAT_POINTS, new StatPointsData());
        this.ascClass = loadOrBlank(SpellSchoolsData.class, new SpellSchoolsData(), nbt, ASC, new SpellSchoolsData());
        this.spellCastingData = loadOrBlank(SpellCastingData.class, new SpellCastingData(), nbt, CAST, new SpellCastingData());
        this.config = loadOrBlank(PlayerConfigData.class, new PlayerConfigData(), nbt, CONFIG, new PlayerConfigData());
        this.favor = loadOrBlank(DeathFavorData.class, new DeathFavorData(), nbt, FAVOR, new DeathFavorData());
        this.professions = loadOrBlank(PlayerProfessionsData.class, new PlayerProfessionsData(), nbt, PROFESSIONS, new PlayerProfessionsData());
        this.buff = loadOrBlank(PlayerBuffData.class, new PlayerBuffData(), nbt, BUFFS, new PlayerBuffData());
        this.rested_xp = loadOrBlank(RestedExpData.class, new RestedExpData(), nbt, RESTED_XP, new RestedExpData());
        this.points = loadOrBlank(PlayerPointsData.class, new PlayerPointsData(), nbt, POINTS, new PlayerPointsData());
        this.characters = loadOrBlank(CharStorageData.class, new CharStorageData(), nbt, CHARACTERS, new CharStorageData());
        this.miscInfo = loadOrBlank(MiscSyncData.class, new MiscSyncData(), nbt, MISC_INFO, new MiscSyncData());
        this.summonedData = loadOrBlank(SummonedData.class, new SummonedData(), nbt, SUMMONED, new SummonedData());
        this.atlas = loadOrBlank(AtlasData.class, new AtlasData(), nbt, ATLAS_DATA, new AtlasData());

        // must come after `characters` is assigned above - the stacks hang off those objects.
        // on the client this tag is absent (syncData strips it), so every inventory just ends up empty.
        CompoundTag charEquipment = nbt.getCompound(CHAR_EQUIPMENT);
        this.characters.map.forEach((num, character) -> {
            if (character != null) {
                character.getEquipment().fromTag(charEquipment.getList(String.valueOf(num), 10));
            }
        });

        //generate a container with mutable size
        // this.ctxStats = loadOrBlank(SavedStatCtxList.class, new SavedStatCtxList(), nbt, "ctx", new SavedStatCtxList());

        //todo this code sucks, we need Codec
        this.jewelData = new JewelData(this.player);
        this.jewelData.jewelInventory.fromTag(nbt.getList(JEWELS, 10));


        skillGemInv.fromTag(nbt.getList(GEMS, 10)); // todo
        auraInv.fromTag(nbt.getList(AURAS, 10)); // todo
        //jewelsInv.fromTag(nbt.getList(JEWELS, 10));


        this.bonusTalents = nbt.getInt(BONUS_TALENTS);
        if (bonusTalents < 0) {
            bonusTalents = 0;
        }
        this.omensFilled = nbt.getInt(OMENS_FILLED);

    }

    // the last tag we actually sent to the client, so unchanged data isn't resent.
    private transient CompoundTag lastSyncedNbt = null;

    private void syncData() {

        // serializeNBT hands out a cached instance now, so this must not edit it in place. removing
        // CHAR_EQUIPMENT from the shared tag would strip stored character gear from the cache and
        // then from the next save - every alt's equipment, gone silently.
        CompoundTag nbt = this.serializeNBT().copy();

        // stored character gear is server side only. the client has no use for it, and leaving it in
        // would put every alt's full gear nbt into this packet and into the comparison below.
        nbt.remove(CHAR_EQUIPMENT);

        // OnServerTick marks this dirty every 3 seconds no matter what, and most of the explicit
        // setDirty() callers fire far more often than the data actually changes. serializing is
        // unavoidable to know whether anything changed, but skipping the send saves the packet and
        // the client side deserializeNBT, which rebuilds every sub object and all of the gem, aura
        // and jewel inventory stacks.
        if (nbt.equals(lastSyncedNbt)) {
            return;
        }
        lastSyncedNbt = nbt;

        // build the packet from the tag we already have instead of using the (Player, String)
        // constructor, which would serialize the whole capability a second time
        SyncPlayerCapToClient packet = new SyncPlayerCapToClient();
        packet.capid = this.getCapIdForSyncing();
        packet.nbt = nbt;

        Packets.sendToClient(player, packet);
    }

    // the client's copy of this capability is built from scratch on login, respawn and dimension
    // change, so it has nothing regardless of what the server last sent. call this whenever the
    // client side entity is replaced, or the check above will skip the resend it needs.
    public void forceNextSync() {
        this.lastSyncedNbt = null;
        this.playerDataSync.setDirty();
    }

    transient HashMap<String, Unit> spellUnits = new HashMap<>();


    // todo cache this maybe too
    public void recalcOmensFilled() {
        try {
            omensFilled = 0;
            ItemStack stack = MyCuriosUtils.get(RefCurios.OMEN, player, 0);
            if (StackSaving.OMEN.has(stack)) {
                var omen = StackSaving.OMEN.loadFrom(stack);
                this.omensFilled = omen.calcPiecesEquipped(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OmenData getOmen() {
        try {
            ItemStack stack = MyCuriosUtils.get(RefCurios.OMEN, player, 0);
            if (StackSaving.OMEN.has(stack)) {
                var omen = StackSaving.OMEN.loadFrom(stack);
                return omen;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Unit getSpellUnitStats(Spell spell) {
        if (!spellUnits.containsKey(spell.GUID())) {
            int key = keyOf(spell);
            if (spell.config.usesSupportGemsFromAnotherSpell()) {
                key = keyOf(spell.config.getSpellUsedForSuppGems());
            }

            var unit = calcSpellUnit(spell, key);
            spellUnits.put(spell.GUID(), unit);
        }
        return spellUnits.get(spell.GUID());
    }

    public boolean canHaveSpellUnit(Spell spell) {
        int key = keyOf(spell);
        if (spell.config.usesSupportGemsFromAnotherSpell()) {
            key = keyOf(spell.config.getSpellUsedForSuppGems());
        }
        return key != SpellCastingData.SPELL_KEY_NOT_EXIST;
    }

    public void setSpellUnitsDirty() {
        spellUnits = new HashMap<>();
    }

    public int keyOf(Spell spell) {
        int key = this.spellCastingData.keyOfSpell(spell.GUID());
        return key;
    }

    private Unit calcSpellUnit(Spell spell, int key) {
        var unit = new Unit();
        StatCalculation.calc(unit, this.cachedStats.allStatsWithoutSuppGems, player, spell, key);
        return unit;
    }

    public GemInventoryHelper getSkillGemInventory() {
        return new GemInventoryHelper(player, skillGemInv, auraInv);
    }

    public SummonedData getSummonedData() {
        return summonedData;
    }

    public void setSummons(String spell, List<UUID> summons) {
        summonedData.setSummons(spell, summons);
        this.playerDataSync.setDirty();
    }

    public void removeSummon(String spell, UUID uuid) {
        if (summonedData.removeSummon(spell, uuid)) {
            this.playerDataSync.setDirty();
        }
    }

    public void removeSummonType(String spell) {
        if (summonedData.removeSummonType(spell)) {
            this.playerDataSync.setDirty();
        }
    }

    public static <OBJ> OBJ loadOrBlank(Class theclass, OBJ newobj, CompoundTag nbt, String loc, OBJ blank) {
        try {
            OBJ data = LoadSave.Load(theclass, newobj, nbt, loc);
            if (data == null) {
                return blank;
            } else {
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blank;
    }
    public static final String ID = "rpg_player_data";
    @Override
    public String getCapIdForSyncing() {
        return ID;
    }
}
