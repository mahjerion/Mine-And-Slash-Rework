package com.robertx22.mine_and_slash.database.data.spells.summons.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.entities.AutoAimingProj;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class SummonEntity extends TamableAnimal implements RangedAttackMob {

    public SummonEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }


    protected AbstractArrow getArrow(ItemStack pArrowStack, float pVelocity) {
        return ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity);


    }

    Goal aggroGoal = null;

    /**
     * Advances the swing timer, which is the only reason a summon's arm ever moves.
     * <p>
     * {@code LivingEntity.swing()} sets {@code swinging = true} and {@code swingTime = -1} and
     * broadcasts the animate packet, but the thing that walks {@code swingTime} forward into
     * {@code attackAnim} - the float {@code HumanoidModel.setupAttackAnimation} reads - is
     * {@code LivingEntity.updateSwingTime()}, and in 1.20.1 the only caller of it anywhere in the
     * entity tree is {@code Player.tick()}. Neither LivingEntity nor Mob calls it, so a summon could
     * swing all it liked: swingTime stayed at -1, attackAnim stayed at 0, and the arm never moved.
     * <p>
     * Runs on both sides, and the client side is the one that matters - that is where the model reads
     * attackAnim. aiStep is called from LivingEntity.tick on client and server alike, unlike
     * serverAiStep. Harmlessly a no-op for the spider and wolf summons, whose models have no arm
     * swing to drive.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        updateSwingTime();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {

            if (this.tickCount == 20) {
                var owner = getOwner();
                if (owner instanceof Player p) {
                    if (Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.AGGRESSIVE_SUMMONS)) {
                        if (aggroGoal == null) {
                            aggroGoal = new NearestAttackableTargetGoal<>(this, Monster.class, false);
                            this.targetSelector.addGoal(2, aggroGoal);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {

        if (true) {
            autoAimingRangedAttack(pTarget);
        } else {

            ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem)));
            AbstractArrow abstractarrow = this.getArrow(itemstack, pDistanceFactor);
            if (this.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
                abstractarrow = ((net.minecraft.world.item.BowItem) this.getMainHandItem().getItem()).customArrow(abstractarrow);
            double d0 = pTarget.getX() - this.getX();
            double d1 = pTarget.getY(0.3333333333333333D) - abstractarrow.getY();
            double d2 = pTarget.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            abstractarrow.shoot(d0, d1 + d3 * (double) 0.2F, d2, 5, 0);
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(abstractarrow);
        }
    }


    /**
     * Launches a Wither skull toward (par2, par4, par6)
     */
    private void autoAimingRangedAttack(LivingEntity target) {

        // melee already swings - MeleeAttackGoal does it before doHurtTarget - but nothing swung for a
        // shot, so a ranged summon fired with a completely still arm. same reason MercenaryEntity
        // swings in its own performRangedAttack.
        this.swing(InteractionHand.MAIN_HAND);

        SoundUtils.playSound(this, SoundEvents.ARROW_SHOOT, 1, 0.2F);


        AutoAimingProj en = SlashEntities.AUTO_AIMING_SKELETON_SKULL.get().create(level());

        en.setOwner(this);

        en.setPosRaw(getX(), getEyeY(), getZ());

        en.setDeltaMovement(ProjectileCastHelper.positionToVelocity(new MyPosition(getEyePosition()), new MyPosition(target.getEyePosition())));

        en.target = target;
        en.speed = 2;

        this.level().addFreshEntity(en);
    }

    // cooldown between the shot and starting to draw again. vanilla's RangedBowAttackGoal always
    // spends another 20 ticks drawing the bow before it fires, so the real cycle is this + 20 and
    // 0 here would still cap the summon at one arrow per second
    private static final int RANGED_ATTACK_COOLDOWN = 10;

    // where the summon plants itself instead of walking closer, and the band it kites in: vanilla
    // backs away inside half of this and re-approaches past ~0.87 of it, so 10 holds ~5-8.7 blocks.
    // it does not gate shooting - the bow fires at any range once there's a target and line of sight
    private static final float RANGED_ATTACK_RADIUS = 10F;

    public boolean usesMelee() {
        return true;
    }

    public boolean usesRanged() {
        return false;
    }

    @Override
    protected void registerGoals() {


        if (usesMelee()) {
            this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        }
        if (usesRanged()) {
            this.goalSelector.addGoal(5, new RangedBowAttackGoal<>(this, 1.0D, RANGED_ATTACK_COOLDOWN, RANGED_ATTACK_RADIUS));
        }

        this.goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1, 1));
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.0D, 6.0F, 1.0F, false));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        //this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, x -> canAttack(x)));
    }

    public Entity focusEntity = null;

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        LivingEntity owner = getOwner();

        if (owner == null) {
            return false;
        }
        if (!pTarget.isAlive()) {
            return false;
        }
        if (!AllyOrEnemy.summonShouldAttack.is(owner, pTarget)) {
            return false;
        }
        // aggro the focus target, otherwise find something else
        if (focusEntity != null && focusEntity.isAlive() && isInAggroRadius((LivingEntity) focusEntity)) {
            return focusEntity == pTarget;
        } else {
            return isInAggroRadius(pTarget);
        }

    }

    // todo test this
    private boolean isInAggroRadius(LivingEntity target) {
        var aggroRadius = Load.Unit(this).summonedPetData.aggro_radius;

        int distance = (int) target.distanceTo(this);

        return aggroRadius >= distance;

    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean canRide(Entity pVehicle) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }
}
