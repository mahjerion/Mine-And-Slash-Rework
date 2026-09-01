package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.capability.entity.SummonedPetData;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Predicate;

public class SummonPetAction extends SpellAction {
    public SummonPetAction() {
        super(Arrays.asList());
    }

    public static int INFINITE_DURATION = -1;

    // how far out a summon is still found by the cap check and by the despawns below. a pet that
    // wandered further than this is out of reach either way, and dies off the registry check instead
    public static final int SEARCH_RADIUS = 100;

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        if (ctx.world.isClientSide) {
            return;
        }

        // the owner has to be a Player - vanilla resolves an owner uuid through getPlayerByUUID and
        // nothing else - so a mercenary's pet is owned by the mercenary's owner and merely summoned
        // by the mercenary. resolveOwner already encodes exactly that walk.
        if (!(AllyOrEnemy.resolveOwner(ctx.caster) instanceof Player ownerPlayer)) {
            return; // an unowned caster has nobody to hand the pet to
        }

        int amount = data.getOrDefault(MapField.COUNT, 1D).intValue();


        for (int i = 0; i < amount; i++) {

            Optional<EntityType<?>> type = EntityType.byString(data.get(MapField.SUMMONED_PET_ID));

            TamableAnimal en = (TamableAnimal) type.get().create(ctx.world);

            en.finalizeSpawn((ServerLevel) ctx.world, ctx.world.getCurrentDifficultyAt(ctx.getBlockPos()), MobSpawnType.MOB_SUMMONED, null, null);

            en.tame(ownerPlayer);

            var pos = ctx.caster.position(); // todo

            en.setPos(pos.x(), pos.y(), pos.z());

            int duration = getDuration(ctx, data);

            float aggroRadius = ctx.calculatedSpellData.data.getNumber(EventData.AGGRO_RADIUS, 15).number;
            aggroRadius *= ctx.calculatedSpellData.data.getNumber(EventData.AGGRO_RADIUS_MULTI, 1).number;


            boolean counts = data.getOrDefault(MapField.COUNTS_TOWARDS_MAX_SUMMONS, true);
            Load.Unit(en).summonedPetData.setup(ctx.calculatedSpellData.getSpell(), duration, (int) aggroRadius, counts, ctx.caster, ownerPlayer);


            Load.Unit(en).SetMobLevelAtSpawn(ownerPlayer);

            Load.Unit(en).setLevel(Load.Unit(ctx.caster).getLevel());

            Load.Unit(en).setRarity(IRarity.SUMMON_ID);


            ctx.world.addFreshEntity(en);
        }

        // BONUS_TOTAL_SUMMONS is the cap of this spell's summon type, the max_x_summons stats are
        // conditioned on EventData.SUMMON_TYPE so only the matching type's stats added into it
        int typeCap = (int) ctx.calculatedSpellData.data.getNumber(EventData.BONUS_TOTAL_SUMMONS, 0).number;
        SummonType cappedType = ctx.calculatedSpellData.getSpell().config.summonType;

        if (ctx.caster != ownerPlayer) {
            // a mercenary's cast must never enforce a cap on its OWNER's pets. typeCap above is read
            // off the mercenary's own BONUS_TOTAL_SUMMONS, so passing the real type here would cull
            // a summoner player's wolves down to whatever budget their mercenary happens to have -
            // three wolves out, hire a Hunter, lose two of them on its first summon. NONE skips the
            // culling entirely and leaves the call doing only the registry rebuild, which is the
            // part a mercenary's pet actually needs (see SummonedPetData.registeredWithOwner).
            cappedType = SummonType.NONE;
        }

        updatePlayerSummons(ownerPlayer, cappedType, typeCap);
    }

    private static int getDuration(SpellCtx ctx, MapHolder data) {
        int duration = data.get(MapField.LIFESPAN_TICKS).intValue();
        if (duration == INFINITE_DURATION) {
            return duration;
        }

        return (int) (duration * ctx.calculatedSpellData.data.getNumber(EventData.DURATION_MULTI, 1).number);
    }

    // a skill that leaves the hotbar takes its minions with it, the same reason it takes its self
    // buffs - see SpellCastingData.setHotbar. otherwise you summon off one slot, swap the gem out
    // and summon again, ending up with every pet in the game running off a single hotbar slot.
    public static void despawnSummonsOfSpell(Player p, String spellId) {
        if (spellId == null || spellId.isEmpty() || p.level().isClientSide) {
            return;
        }

        despawn(p, x -> spellId.equals(x.spell));

        // the scan above is blind past its radius, so drop the registry entry too - any stray
        // outside it fails SummonedPetData.registeredWithOwner on its next check and self destructs
        Load.player(p).removeSummonType(spellId);
    }

    public static void despawnAllSummons(Player p) {
        if (p.level().isClientSide) {
            return;
        }

        despawn(p, x -> true);
        Load.player(p).clearSummons();
    }

    /**
     * A dismissed mercenary takes its pets with it - the same rule
     * {@link #despawnSummonsOfSpell} applies when a skill leaves the hotbar.
     * <p>
     * Searched from the summoner rather than the owner, because a mercenary that just died or was
     * dismissed is where its pets actually are; the owner may be anywhere. The pets are owned by
     * the owner, so the registry entry has to be cleaned off the owner, not off the mercenary.
     */
    public static void despawnSummonsOf(LivingEntity summoner) {
        if (summoner == null || summoner.level().isClientSide) {
            return;
        }

        boolean any = false;

        for (SummonEntity en : EntityFinder.start(summoner, SummonEntity.class, summoner.blockPosition()).searchFor(AllyOrEnemy.all).radius(SEARCH_RADIUS).build()) {
            var data = Load.Unit(en).summonedPetData;

            if (!data.isSummonedBy(summoner)) {
                continue;
            }
            if (en.getOwner() instanceof Player owner) {
                Load.player(owner).removeSummon(data.spell, en.getUUID());
            }

            data.discard(en);
            any = true;
        }

        if (any) {
            SoundUtils.playSound(summoner, SoundEvents.GENERIC_DEATH);
        }
    }

    private static void despawn(Player p, Predicate<SummonedPetData> predicate) {
        boolean any = false;

        for (SummonEntity en : EntityFinder.start(p, SummonEntity.class, p.blockPosition()).searchFor(AllyOrEnemy.all).radius(SEARCH_RADIUS).build()) {
            if (en.getOwner() != p) {
                continue;
            }

            var data = Load.Unit(en).summonedPetData;

            if (!predicate.test(data)) {
                continue;
            }

            data.discard(en);
            any = true;
        }

        if (any) {
            SoundUtils.playSound(p, SoundEvents.GENERIC_DEATH);
        }
    }

    public static void updatePlayerSummons(LivingEntity caster, SummonType cappedType, int typeCap) {
        ArrayList<NearbySummon> summonsNearby = new ArrayList<>();
        ArrayList<NearbySummon> ofCappedType = new ArrayList<>();

        for (SummonEntity en : EntityFinder.start(caster, SummonEntity.class, caster.blockPosition()).searchFor(AllyOrEnemy.all).radius(SEARCH_RADIUS).build()) {
            if (en.getOwner() != caster) {
                continue;
            }

            var data = Load.Unit(en).summonedPetData;
            var nearby = new NearbySummon(en, data);

            summonsNearby.add(nearby);

            // a summon can be of the capped type but exempt from it, ie burst summons
            // limited by their duration and cooldown instead of by a cap slot.
            // a pet somebody ELSE summoned - a mercenary's, which is owned by this player but cast
            // by the mercenary - never occupies this player's slots either, whatever the pack says
            // about counts_towards_max_summons.
            boolean summonedByCaster = data.summoner_uuid.isEmpty() || data.isSummonedBy(caster);

            if (data.counts_towards_max_summons && summonedByCaster && data.getSummonType() == cappedType) {
                ofCappedType.add(nearby);
            }
        }

        Set<String> spellsOfDiscarded = new HashSet<>();

        if (cappedType != SummonType.NONE) { // summons without a type are uncapped
            ofCappedType.sort(Comparator.comparingInt(x -> -x.summon.tickCount)); // oldest first

            int excess = ofCappedType.size() - typeCap;
            for (int i = 0; i < excess && i < ofCappedType.size(); i++) {
                NearbySummon summonToRemove = ofCappedType.get(i);
                spellsOfDiscarded.add(summonToRemove.data.spell);
                summonToRemove.data.discard(summonToRemove.summon);
                summonsNearby.remove(summonToRemove);
            }
        }

        HashMap<String, List<UUID>> summonedTypes = new HashMap<>();
        for (NearbySummon summon : summonsNearby) {
            summonedTypes.computeIfAbsent(summon.data.spell, x -> new ArrayList<>()).add(summon.summon.getUUID());
        }

        if (!(caster instanceof Player player)) {
            return;
        }

        // a spell whose last summon was just discarded has no survivors left to rebuild its entry from
        for (String spell : spellsOfDiscarded) {
            summonedTypes.putIfAbsent(spell, new ArrayList<>());
        }

        summonedTypes.forEach((spell, summons) -> Load.player(player).setSummons(spell, summons));
    }

    public MapHolder create(EntityType type, int lifespan, int amount, SummonType st, boolean counts) {
        MapHolder c = new MapHolder();
        c.put(MapField.SUMMON_TYPE, st.id);
        c.put(MapField.SUMMONED_PET_ID, EntityType.getKey(type).toString());
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.LIFESPAN_TICKS, (double) lifespan);
        c.put(MapField.COUNT, (double) amount);
        c.put(MapField.COUNTS_TOWARDS_MAX_SUMMONS, counts);
        c.type = GUID();
        return c;
    }

    @Override
    public String GUID() {
        return "summon_pet";
    }

    private static class NearbySummon {
        public SummonEntity summon;
        public SummonedPetData data;

        public NearbySummon(SummonEntity summon, SummonedPetData data) {
            this.summon = summon;
            this.data = data;
        }
    }
}
