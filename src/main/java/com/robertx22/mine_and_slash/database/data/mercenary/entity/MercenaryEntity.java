package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryCastState;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.entities.AutoAimingProj;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A hired companion. Deliberately a sibling of
 * {@link com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity} rather than a
 * subclass: {@code EntityData.isSummon()} keys off a non empty {@code summonedPetData}, and a
 * mercenary must not read as a summon - it isn't spawned by a spell, "explode your minions" style
 * effects must not consume it, and it doesn't inherit summon stats from a source spell.
 * <p>
 * It is still a {@link TamableAnimal}, which makes it an {@code OwnableEntity}. That single fact is
 * what gives it ally-ness for free everywhere: {@code EntityFinder.isTamedByAlly} resolves the owner
 * and checks their team, so allied players' heals and buffs reach it and nobody on the team targets it.
 */
public class MercenaryEntity extends TamableAnimal implements RangedAttackMob {

    // the class id has to reach the client so the renderer can pick the datapack texture. sent as the
    // id rather than the texture string so a datapack texture change takes effect without a resync.
    private static final EntityDataAccessor<String> CLASS_ID =
            SynchedEntityData.defineId(MercenaryEntity.class, EntityDataSerializers.STRING);

    private static final String NBT_CLASS_ID = "merc_class";

    /** how far the mercenary will look for something to fight on its own */
    public static final int AGGRO_RADIUS = 10;

    public MercenaryEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CLASS_ID, "");
    }

    public String getClassId() {
        return this.entityData.get(CLASS_ID);
    }

    public void setClassId(String id) {
        this.entityData.set(CLASS_ID, id == null ? "" : id);
        // the class - and with it ai_behavior - isn't known yet when registerGoals() runs at
        // construction (this is called after the entity already exists), so the combat goal picked
        // there is only ever a placeholder default. this is the one real point it can be corrected.
        refreshCombatGoal();
    }

    @Nullable
    public MercenaryClass getMercClass() {
        String id = getClassId();
        if (id.isEmpty() || !ExileDB.Mercenaries().isRegistered(id)) {
            return null;
        }
        return ExileDB.Mercenaries().get(id);
    }

    /** the owner's stored data for this mercenary, or null if the owner is gone */
    @Nullable
    public MercenaryData getMercData() {
        if (getOwner() instanceof Player p) {
            return Load.player(p).mercs.getOrCreate(getClassId());
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(NBT_CLASS_ID, getClassId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setClassId(tag.getString(NBT_CLASS_ID));
    }

    // a mercenary is summoned and dismissed explicitly, never by distance. without this the game
    // despawns it the moment the owner walks far enough away.
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // deliberately never written to the world save. everything worth keeping lives in the owner's
    // MercenaryData, and respawning from that on login is both cheaper and safe - a crash, a reload
    // or a dimension change can't strand an orphan mercenary in a chunk somewhere.
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ------------------------------------------------------------------ combat mode

    // canAttack() is called once per candidate entity per targeting tick, and resolving the owner's
    // MercenaryData every time was the hottest thing this class did. refreshed once per tick instead.
    private MercenaryData.CombatMode cachedMode = MercenaryData.CombatMode.AGGRESSIVE;

    public MercenaryData.CombatMode getMode() {
        return cachedMode;
    }

    private void refreshMode() {
        MercenaryData data = getMercData();
        if (data != null) {
            cachedMode = data.mode;
        }
    }

    // cooldown between the shot and starting to draw again. vanilla's RangedBowAttackGoal always
    // spends another 20 ticks drawing the bow before it fires, so the real cycle is this + 20 and
    // 0 here would still cap the mercenary at one arrow per second
    private static final int RANGED_ATTACK_COOLDOWN = 10;

    private static final float RANGED_ATTACK_RADIUS = 10F;

    // the combat goals currently held in goalSelector at priority 5, swapped out by refreshCombatGoal()
    // when the mercenary's actual class - and so its ai_behavior - becomes known. deliberately plain
    // fields with no initializer: registerGoals() runs from the Mob constructor, BEFORE this class's
    // own field initializers, so anything assigned an initial value here would be wiped straight back
    // out afterwards - orphaning the goals it had already put in the selector.
    private Goal combatGoal;
    private Goal bowGoal;

    // ------------------------------------------------------------------ spell casting

    // the wind up of a spell in progress, driven by MercenarySpellCaster. created lazily for the same
    // reason the two goal fields above have no initializer - registerGoals() runs before field
    // initializers do - and every reader below is null safe so a call from that window can't NPE.
    private MercenaryCastState castState;

    public MercenaryCastState getCastState() {
        if (this.castState == null) {
            this.castState = new MercenaryCastState();
        }
        return this.castState;
    }

    /**
     * True while a skill's cast time is running.
     * <p>
     * A mercenary keeps walking, kiting and pathing through a wind up - a caster that froze in place
     * for a second would just be shot - but it throws nothing while it casts: no melee swing, no bow
     * shot, and no second skill (MercenarySpellCaster returns early on its own for that one).
     */
    public boolean isCastingSpell() {
        return this.castState != null && this.castState.isCasting();
    }

    /**
     * True while the mercenary is walking into range of a skill it has already picked.
     * <p>
     * Unlike {@link #isCastingSpell()} this does NOT suppress attacks - the mercenary is still
     * fighting on the way in. What it does suppress is the combat goals' own movement, because
     * MercenarySpellCaster owns the navigation for the duration; without that,
     * {@code MercenaryRangedGoal.fleeFrom} would undo every step of the approach on the tick after
     * it was taken, and a kiting elementalist could never reach anything with Frost Nova.
     */
    public boolean isApproachingForCast() {
        return this.castState != null && this.castState.isApproaching();
    }

    @Override
    protected void registerGoals() {
        refreshCombatGoal();

        this.goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1, 1));
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.0D, 6.0F, 2.0F, false));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        // in Idle these still run, but canAttack rejects everything, so they never produce a target
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::canAttack));
    }

    /** picks the priority 5 combat goals to match the mercenary's class - melee closes in, ranged kites */
    private void refreshCombatGoal() {
        if (this.combatGoal != null) {
            this.goalSelector.removeGoal(this.combatGoal);
            this.combatGoal = null;
        }
        if (this.bowGoal != null) {
            this.goalSelector.removeGoal(this.bowGoal);
            this.bowGoal = null;
        }

        MercenaryClass mc = getMercClass();

        if (mc != null && mc.ai_behavior == MercenaryClass.AiBehavior.RANGED) {
            // one goal that both kites and shoots, so there is nothing for a bow goal to add
            this.combatGoal = new MercenaryRangedGoal(this, 1.0D);
        } else {
            // melee keeps the pair it always had. MeleeAttackGoal wins the flags whenever it can path
            // to the target, but it gives up for a full second at a time when it can't (its own
            // COOLDOWN_BETWEEN_CAN_USE_CHECKS) - and that gap is the only window in which a bow in a
            // melee mercenary's hand ever fired. nothing here forbids equipping one.
            this.combatGoal = new MercenaryMeleeAttackGoal(this, 1.0D, true);
            this.bowGoal = new RangedBowAttackGoal<>(this, 1.0D, RANGED_ATTACK_COOLDOWN, RANGED_ATTACK_RADIUS);
        }

        // melee first: goals are kept in insertion order and both want MOVE and LOOK, so this is what
        // decides that melee, not the bow, gets first refusal on them every tick.
        this.goalSelector.addGoal(5, this.combatGoal);
        if (this.bowGoal != null) {
            this.goalSelector.addGoal(5, this.bowGoal);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = getOwner();

        if (owner == null || !target.isAlive()) {
            return false;
        }
        // ask from the owner's point of view - that is what keeps the owner, their teammates and
        // everyone's pets off the list
        if (!AllyOrEnemy.summonShouldAttack.is(owner, target)) {
            return false;
        }

        switch (getMode()) {
            case IDLE:
                return false;
            case DEFENSIVE:
                // only things that have actually swung at us or at the owner
                return target == owner.getLastHurtByMob() || target == this.getLastHurtByMob();
            case AGGRESSIVE:
            default:
                // the owner's current target always wins, so the mercenary focuses what you focus
                if (owner.getLastHurtMob() == target) {
                    return true;
                }
                return target.distanceTo(this) <= AGGRO_RADIUS;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }
        refreshMode();

        // Idle has to actively drop a target - the goals above only decide whether a NEW one is
        // picked, and a target acquired before the mode flipped would otherwise stick.
        if (getMode() == MercenaryData.CombatMode.IDLE && this.getTarget() != null) {
            this.setTarget(null);
        }
    }

    // ------------------------------------------------------------------ name

    /**
     * "Dev's Fighter Mercenary". Overriding getName() rather than setting a custom name means it is
     * always live - it follows an owner rename or a class switch - and everything downstream picks it
     * up for free, because Entity.getDisplayName() routes through here.
     */
    @Override
    public Component getName() {
        try {
            LivingEntity owner = getOwner();
            MercenaryClass mc = getMercClass();

            if (owner != null && mc != null) {
                return Words.MercenaryOwnedName.locName(owner.getName(), mc.locName());
            }
        } catch (Exception e) {
            // a datapack class that went missing, or an owner who logged out mid-render. the plain
            // type name is a fine answer and much better than a broken name render.
        }
        return super.getName();
    }

    // ------------------------------------------------------------------ ranged

    private boolean holdsRangedWeapon() {
        ItemStack main = getMainHandItem();
        return main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!holdsRangedWeapon()) {
            return;
        }
        // the mercenary is mid cast - it doesn't get to shoot as well. covers RangedBowAttackGoal on a
        // melee mercenary too, which has no idea a spell is going off
        if (isCastingSpell()) {
            return;
        }
        // melee already swings - MeleeAttackGoal does it before doHurtTarget - but nothing swings for
        // a bow shot, so the mercenary fired with a completely still arm.
        this.swing(InteractionHand.MAIN_HAND);

        SoundUtils.playSound(this, SoundEvents.ARROW_SHOOT, 1, 0.2F);

        AutoAimingProj en = SlashEntities.AUTO_AIMING_SKELETON_SKULL.get().create(level());
        if (en == null) {
            return;
        }
        en.setOwner(this);
        en.setPosRaw(getX(), getEyeY(), getZ());
        en.setDeltaMovement(ProjectileCastHelper.positionToVelocity(new MyPosition(getEyePosition()), new MyPosition(target.getEyePosition())));
        en.target = target;
        en.speed = 2;

        this.level().addFreshEntity(en);
    }

    protected AbstractArrow getArrow(ItemStack arrowStack, float velocity) {
        return ProjectileUtil.getMobArrow(this, arrowStack, velocity);
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }
}
