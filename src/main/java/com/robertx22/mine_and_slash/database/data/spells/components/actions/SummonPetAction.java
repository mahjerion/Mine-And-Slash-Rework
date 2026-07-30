package com.robertx22.mine_and_slash.database.data.spells.components.actions;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class SummonPetAction extends SpellAction {
    public SummonPetAction() {
        super(Arrays.asList());
    }

    public static int INFINITE_DURATION = -1;

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        if (ctx.world.isClientSide) {
            return;
        }


        int amount = data.getOrDefault(MapField.COUNT, 1D).intValue();


        for (int i = 0; i < amount; i++) {

            Optional<EntityType<?>> type = EntityType.byString(data.get(MapField.SUMMONED_PET_ID));

            TamableAnimal en = (TamableAnimal) type.get().create(ctx.world);

            en.finalizeSpawn((ServerLevel) ctx.world, ctx.world.getCurrentDifficultyAt(ctx.getBlockPos()), MobSpawnType.MOB_SUMMONED, null, null);

            en.tame((Player) ctx.caster);

            var pos = ctx.caster.position(); // todo

            en.setPos(pos.x(), pos.y(), pos.z());

            int duration = getDuration(ctx, data);

            float aggroRadius = ctx.calculatedSpellData.data.getNumber(EventData.AGGRO_RADIUS, 15).number;
            aggroRadius *= ctx.calculatedSpellData.data.getNumber(EventData.AGGRO_RADIUS_MULTI, 1).number;


            boolean counts = data.getOrDefault(MapField.COUNTS_TOWARDS_MAX_SUMMONS, true);
            Load.Unit(en).summonedPetData.setup(ctx.calculatedSpellData.getSpell(), duration, (int) aggroRadius, counts);


            Load.Unit(en).SetMobLevelAtSpawn((Player) ctx.caster);

            Load.Unit(en).setLevel(Load.Unit(ctx.caster).getLevel());

            Load.Unit(en).setRarity(IRarity.SUMMON_ID);


            ctx.world.addFreshEntity(en);
        }

        // BONUS_TOTAL_SUMMONS is the cap of this spell's summon type, the max_x_summons stats are
        // conditioned on EventData.SUMMON_TYPE so only the matching type's stats added into it
        int typeCap = (int) ctx.calculatedSpellData.data.getNumber(EventData.BONUS_TOTAL_SUMMONS, 0).number;
        updatePlayerSummons(ctx.caster, ctx.calculatedSpellData.getSpell().config.summonType, typeCap);
    }

    private static int getDuration(SpellCtx ctx, MapHolder data) {
        int duration = data.get(MapField.LIFESPAN_TICKS).intValue();
        if (duration == INFINITE_DURATION) {
            return duration;
        }

        return (int) (duration * ctx.calculatedSpellData.data.getNumber(EventData.DURATION_MULTI, 1).number);
    }

    public static void updatePlayerSummons(LivingEntity caster, SummonType cappedType, int typeCap) {
        ArrayList<NearbySummon> summonsNearby = new ArrayList<>();
        ArrayList<NearbySummon> ofCappedType = new ArrayList<>();

        for (SummonEntity en : EntityFinder.start(caster, SummonEntity.class, caster.blockPosition()).searchFor(AllyOrEnemy.all).radius(100).build()) {
            if (en.getOwner() != caster) {
                continue;
            }

            var data = Load.Unit(en).summonedPetData;
            var nearby = new NearbySummon(en, data);

            summonsNearby.add(nearby);

            // a summon can be of the capped type but exempt from it, ie burst summons
            // limited by their duration and cooldown instead of by a cap slot
            if (data.counts_towards_max_summons && data.getSummonType() == cappedType) {
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
