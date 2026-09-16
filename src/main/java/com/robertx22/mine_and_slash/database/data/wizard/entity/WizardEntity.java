package com.robertx22.mine_and_slash.database.data.wizard.entity;

import com.robertx22.mine_and_slash.database.data.wizard.WizardCastState;
import com.robertx22.mine_and_slash.database.data.wizard.WizardSpellShapes.TelegraphKind;
import com.robertx22.mine_and_slash.database.data.wizard.WizardTelegraphParticles;
import com.robertx22.mine_and_slash.database.data.wizard.WizardType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
     * Public and plain rather than synched: the client never needs the wait between casts, only the
     * cast itself, which {@link #publishCast} sends.
     */
    public int nextCastTicks = 0;

    /**
     * The wind up of a spell in progress. Created lazily, and every reader below is null safe,
     * because {@code registerGoals()} runs from the {@code Mob} constructor BEFORE this class's own
     * field initializers - anything assigned here would be wiped straight back out afterwards.
     */
    private WizardCastState castState;

    /** the longest projectile telegraph beam drawn, whatever the projectile's range. here rather than in the
     * renderer so this common class never references a client one */
    public static final double TELEGRAPH_BEAM_MAX_LENGTH = 20D;

    // ------------------------------------------------------------------ the telegraph, as clients see it
    //
    // Everything a client needs to draw the icon and the ground telegraph, set once when a cast starts
    // and once when it ends - never per tick. Progress is a start TIME rather than a counter, so a
    // player who starts tracking the wizard halfway through a cast still sees the right fill.
    //
    // The shape numbers arrive already scaled by the wizard's stats: the client never reads the
    // spell's components itself, see WizardSpellShapes. Not saved - a cast isn't.

    /** spell GUID, empty when idle. every client reader gates on this, so it is set last */
    private static final EntityDataAccessor<String> CAST_SPELL =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.STRING);
    /** game time the telegraph started */
    private static final EntityDataAccessor<Integer> CAST_START =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.INT);
    /** telegraph + cast time, in ticks */
    private static final EntityDataAccessor<Integer> CAST_TOTAL =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.INT);
    /** ticks the icon fills over. shorter than the total for a multicast, which fires while lit */
    private static final EntityDataAccessor<Integer> CAST_FILL =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.INT);
    /** {@link TelegraphKind} ordinal */
    private static final EntityDataAccessor<Integer> CAST_SHAPE =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.INT);
    /** circle radius or line length, in blocks */
    private static final EntityDataAccessor<Float> CAST_SIZE =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.FLOAT);
    /** how many projectiles a line telegraph fans out into */
    private static final EntityDataAccessor<Integer> CAST_PROJ_COUNT =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.INT);
    /** the fan's spread, the same {@code proj_apart} ProjectileCastHelper spreads by */
    private static final EntityDataAccessor<Float> CAST_PROJ_APART =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.FLOAT);
    /** where an at-sight skill will land. only meaningful for {@link TelegraphKind#AT_TARGET_CIRCLE} */
    private static final EntityDataAccessor<Vector3f> CAST_ANCHOR =
            SynchedEntityData.defineId(WizardEntity.class, EntityDataSerializers.VECTOR3);

    public WizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // a wizard spends the fight walking backwards away from whoever it is fighting, without
        // looking where it is going, and half of them stand in their own fire. vanilla monsters
        // path straight through both by default.
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1F);
    }

    // runs from the Entity constructor, before this class's field initialisers - like registerGoals.
    // safe because it only touches entityData and the static accessors; never read castState here.
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_SPELL, "");
        this.entityData.define(CAST_START, 0);
        this.entityData.define(CAST_TOTAL, 0);
        this.entityData.define(CAST_FILL, 0);
        this.entityData.define(CAST_SHAPE, 0);
        this.entityData.define(CAST_SIZE, 0F);
        this.entityData.define(CAST_PROJ_COUNT, 0);
        this.entityData.define(CAST_PROJ_APART, 0F);
        this.entityData.define(CAST_ANCHOR, new Vector3f());
    }

    public WizardCastState getCastState() {
        if (castState == null) {
            castState = new WizardCastState();
        }
        return castState;
    }

    /** server side: show every client tracking this wizard the cast that just started */
    public void publishCast(String spellGuid, int totalTicks, int fillTicks, TelegraphKind kind, float size,
                            int projCount, float projApart, @Nullable Vec3 anchor) {
        if (level().isClientSide) {
            return;
        }
        this.entityData.set(CAST_START, (int) level().getGameTime());
        this.entityData.set(CAST_TOTAL, Math.max(1, totalTicks));
        this.entityData.set(CAST_FILL, Math.max(1, fillTicks));
        this.entityData.set(CAST_SHAPE, kind.ordinal());
        this.entityData.set(CAST_SIZE, size);
        this.entityData.set(CAST_PROJ_COUNT, projCount);
        this.entityData.set(CAST_PROJ_APART, projApart);
        this.entityData.set(CAST_ANCHOR, anchor == null ? new Vector3f()
                : new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
        // last: a client never sees a live spell next to the previous cast's shape
        this.entityData.set(CAST_SPELL, spellGuid);
    }

    /**
     * The one place a cast ends - forgets it on the server and takes the telegraph down on clients.
     * <p>
     * Everything that ends a cast goes through here rather than {@code getCastState().clear()}: a
     * cast cleared without this leaves the client drawing a full icon and a telegraph forever.
     */
    public void endCast() {
        getCastState().clear();
        if (!level().isClientSide) {
            this.entityData.set(CAST_SPELL, "");
        }
    }

    // ------------------------------------------------------------------ client readers

    /** the spell being cast, or empty when idle */
    public String getTelegraphSpell() {
        return this.entityData.get(CAST_SPELL);
    }

    /** ticks since the telegraph started, with partial tick for smooth rendering */
    public float getTelegraphElapsed(float partialTick) {
        return (level().getGameTime() - (long) this.entityData.get(CAST_START)) + partialTick;
    }

    public int getTelegraphTotalTicks() {
        return this.entityData.get(CAST_TOTAL);
    }

    public int getTelegraphFillTicks() {
        return this.entityData.get(CAST_FILL);
    }

    public TelegraphKind getTelegraphKind() {
        int i = this.entityData.get(CAST_SHAPE);
        TelegraphKind[] all = TelegraphKind.values();
        return i >= 0 && i < all.length ? all[i] : TelegraphKind.NONE;
    }

    public float getTelegraphSize() {
        return this.entityData.get(CAST_SIZE);
    }

    public int getTelegraphProjCount() {
        return this.entityData.get(CAST_PROJ_COUNT);
    }

    public float getTelegraphProjApart() {
        return this.entityData.get(CAST_PROJ_APART);
    }

    public Vector3f getTelegraphAnchor() {
        return this.entityData.get(CAST_ANCHOR);
    }

    /**
     * Grown while a projectile telegraph is up. The beam is drawn from this wizard's render call, which
     * only happens when this box is in view - so without this a wizard just off screen would drop a
     * beam that crosses the middle of it. Rendering is the only use of this box here - the Neat plate focus check reads the real box for wizards.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling();
        if (!getTelegraphSpell().isEmpty() && getTelegraphKind() == TelegraphKind.PROJECTILE_LINE) {
            return box.inflate(Math.min(getTelegraphSize(), TELEGRAPH_BEAM_MAX_LENGTH));
        }
        return box;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            WizardTelegraphParticles.tick(this);
        }
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
        endCast();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        endCast();
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
