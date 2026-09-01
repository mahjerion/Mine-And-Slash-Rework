package com.robertx22.mine_and_slash.database.data.wizard.entity;

import com.robertx22.mine_and_slash.database.data.wizard.WizardCastState;
import com.robertx22.mine_and_slash.database.data.wizard.WizardType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * An elemental caster monster - Fire, Ice, Lightning or Chaos Wizard.
 * <p>
 * One class behind all four. What separates them is entirely datapack: the registered entity type's
 * path is the id of a {@link WizardType} entry, which carries the skill list, the texture and the
 * cast interval. Four entity types rather than one with a variant field because the spawn pools are
 * weighted per entity id - {@code library_of_exile_mob_list} names mobs, not variants - so a map
 * could not otherwise ask for "a quarter of these, evenly split by element".
 * <p>
 * {@code extends Monster} is load bearing rather than cosmetic: {@code EntityTypeUtils.isMob} keys
 * on {@code instanceof Enemy}, and that is what earns the wizard a level rolled off the map's tier,
 * a rarity, mob affixes and loot. Nothing here sets any of that up - the normal
 * {@code OnMobSpawn.setupNewMobOnSpawn} path does, and it must not be short circuited the way
 * {@code MercenaryManager} does with {@code mobStatsAreSet()}.
 * <p>
 * These only ever appear in maps, and that falls out of the data rather than being enforced here:
 * they are in no biome spawner anywhere, and the map dimension denies natural spawning outright, so
 * the only route into the world is a map's mob list.
 */
public class WizardEntity extends Monster {

    /**
     * Ticks until the next cast, counted down by {@link com.robertx22.mine_and_slash.database.data.wizard.WizardSpellCaster}.
     * <p>
     * Public and plain rather than synched: casting is entirely server side, and the client only
     * ever needs to know the arm swung, which {@code swing()} already broadcasts.
     */
    public int nextCastTicks = 0;

    /**
     * The wind up of a spell in progress. Created lazily, and every reader below is null safe,
     * because {@code registerGoals()} runs from the {@code Mob} constructor BEFORE this class's own
     * field initializers - anything assigned here would be wiped straight back out afterwards.
     */
    private WizardCastState castState;

    public WizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // a wizard spends the fight walking backwards away from whoever it is fighting, without
        // looking where it is going, and half of them stand in their own fire. vanilla monsters
        // path straight through both by default.
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1F);
    }

    public WizardCastState getCastState() {
        if (castState == null) {
            castState = new WizardCastState();
        }
        return castState;
    }

    /**
     * This wizard's datapack entry, or null if a pack removed it.
     * <p>
     * Resolved from the entity type's path every call rather than cached: a datapack reload replaces
     * the registry's entries while the mob is still standing there, and a cached reference would go
     * on casting the pre-reload skill list. Both sides can answer - the registry syncs on login -
     * which is what lets the renderer read the texture from it.
     */
    @Nullable
    public WizardType getWizardType() {
        try {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
            if (key == null) {
                return null;
            }
            return ExileDB.Wizards().get(key.getPath());
        } catch (Exception e) {
            return null;
        }
    }

    /** true while a skill is winding up - the combat goal holds its distance instead of repositioning */
    public boolean isCastingSpell() {
        return castState != null && castState.isCasting();
    }

    /** how close the wizard wants to be for the skill it is casting, or 0 when it has none */
    public double getActiveEngageRange() {
        return castState == null ? 0 : castState.activeEngageRange();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WizardCombatGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        // required. Only Player.tick() calls this in 1.20.1, so without it a mob's arm never
        // animates and every cast is silent visually - MercenaryEntity and SummonEntity do the same.
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    public void die(DamageSource source) {
        // whatever it was winding up is over. the spell entities it already placed look after
        // themselves with no caster, so this is all the cleanup there is.
        getCastState().clear();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        getCastState().clear();
        super.remove(reason);
    }

    // pillager rather than witch, even though it wears the witch's model: the witch's noises are
    // drinking and cackling, which read as a mob doing something it isn't.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    /**
     * A wizard has no melee attack at all - its skills are the whole of its damage.
     * <p>
     * Refused here as well as left out of the combat goal, because the goal is not the only thing
     * that can ask for a swing: anything that pushes a mob into contact damage would otherwise give
     * a caster a free physical hit, which is the exact thing these mobs exist to stop being the
     * only damage in a map.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // never turn on another monster. a map full of wizards that aggro each other stops being a
        // threat to anyone, and their skills are area effects that would chew through their own side
        return !(target instanceof Monster) && super.canAttack(target);
    }
}
