package com.robertx22.mine_and_slash.database.data.spells.entities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.AoeSelector;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.IMyRenderAsItem;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.Utilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector3f;

public class SimpleProjectileEntity extends AbstractArrow implements IMyRenderAsItem, IDatapackProjectileEntity {

    CalculatedSpellData spellData;

    private int xTile;
    private int yTile;
    private int zTile;

    protected boolean inGround;

    private int ticksInGround = 0;

    public boolean moveTowardsEnemies = false;
    public boolean moveTowardsCaster = false;

    private static final EntityDataAccessor<CompoundTag> SPELL_DATA = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> ENTITY_NAME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_ENTITY_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIT_ALLIES = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PIERCE = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEATH_TIME = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHAINS = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPIRE_ON_BLOCK_HIT = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ACCELERATION = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_VELOCITY = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_ACCELERATION = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> FORWARD_VECTOR = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> UP_VECTOR = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.VECTOR3);
    // orbit mode: the projectile keeps a fixed distance around its caster and follows them around,
    // instead of flying free. synced so the client can place it itself against the local player
    private static final EntityDataAccessor<Boolean> ORBITING = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ORBIT_RADIUS = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_SPEED = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_Y_OFFSET = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_START_ANGLE = SynchedEntityData.defineId(SimpleProjectileEntity.class, EntityDataSerializers.FLOAT);

    public Entity ignoreEntity;

    boolean collidedAlready = false;

    private boolean motionDirty = false;

    private Float cachedSpeedMultiplier = null;
    private Float cachedYawSpeedMultiplier = null;

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    protected boolean onExpireProc(LivingEntity caster) {
        return true;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return new ArrayList<>();
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {

    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override // seems to help making it hit easier?
    public float getPickRadius() {
        return 1.0F;
    }

    public int getTicksInGround() {
        return this.ticksInGround;
    }

    public int getDeathTime() {
        return entityData.get(DEATH_TIME);
    }

    public void setDeathTime(int newVal) {
        this.entityData.set(DEATH_TIME, newVal);
    }

    public float getSpeedMultiplier() {
        if (cachedSpeedMultiplier == null) {
            cachedSpeedMultiplier = getSpellData().data.getNumber(EventData.PROJECTILE_SPEED_MULTI, 1F).number;
        }
        return cachedSpeedMultiplier;
    }

    public float getYawSpeedMultiplier() {
        if (cachedYawSpeedMultiplier == null) {
            cachedYawSpeedMultiplier = getSpellData().data.getNumber(EventData.PROJECTILE_YAW_SPEED_MULTI, 1F).number;
        }
        return cachedYawSpeedMultiplier;
    }

    public SimpleProjectileEntity(EntityType<? extends Entity> type, Level worldIn) {
        super((EntityType<? extends AbstractArrow>) type, worldIn);
        this.xTile = -1;
        this.yTile = -1;
        this.zTile = -1;

        this.setSoundEvent(SoundEvents.EMPTY);
    }

    protected void moveToImpactPosition(HitResult result) {

        if (result instanceof EntityHitResult enres) {
            // the result just contains entity position, so we must clip against the AABB ourselves
            AABB aabb = enres.getEntity().getBoundingBox().inflate(0.3D);
            Vec3 traceEnd = this.position().add(this.getDeltaMovement());
            Optional<Vec3> clipped = aabb.clip(this.position(), traceEnd);

            if (clipped.isPresent()) {
                // move to where we hit the entity
                this.setPos(clipped.get());
            }
        } else {
            // move all the way to where we impacted
            this.setPos(result.getLocation());
        }

    }

    public Entity getEntityHit(HitResult result, double radius) {

        EntityHitResult enres = null;

        if (result instanceof EntityHitResult) {
            enres = (EntityHitResult) result;
        }

        if (enres == null) {
            return null;
        }

        if (enres.getEntity() instanceof Entity) {
            if (enres.getEntity() != this.getCaster()) {
                return enres.getEntity();
            }
        }

        if (getCaster() != null) {
            List<LivingEntity> entities = EntityFinder.start(getCaster(), LivingEntity.class, position())
                    .radius(radius)
                    .build();

            if (entities.size() > 0) {

                LivingEntity closest = entities.get(0);

                for (LivingEntity en : entities) {
                    if (en != closest) {
                        if (this.distanceTo(en) < this.distanceTo(closest)) {
                            closest = en;
                        }
                    }
                }

                return closest;
            }
        }

        return null;

    }

    public void onTick() {

        if (entityData.get(ORBITING)) {
            // orbit mode replaces free flight entirely, so the acceleration/yaw/tracking steering
            // below must not run and fight it for control of the motion
            applyOrbit();
        } else {
            applyAcceleration();
            applyYawVelocity();
        }

        if (getCaster() != null) {

            if (!entityData.get(ORBITING)) {
                tryMoveTowardsTargets();
            }

            if (!level().isClientSide) {
                this.getSpellData()
                        .getSpell()
                        .getAttached()
                        .tryActivate(getScoreboardName(), SpellCtx.onTick(getCaster(), this, getSpellData()));

            }
        }
    }

    boolean exploded = false;

    @Override
    public void remove(RemovalReason r) {

        if (!exploded) {
            exploded = true;

            LivingEntity caster = getCaster();

            if (caster != null) {
                if (!level().isClientSide) {
                    this.getSpellData()
                            .getSpell()
                            .getAttached()
                            .tryActivate(getScoreboardName(), SpellCtx.onExpire(caster, this, getSpellData()));
                }
            }
        }

        super.remove(r);
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public final void tick() {

        if (this.removeNextTick) {
            this.remove(RemovalReason.KILLED);
            return;
        }

        try {
            super.tick();
        } catch (Exception e) {
            e.printStackTrace();
            this.scheduleRemoval();
        }

        if (this.getSpellData() == null || getCaster() == null) {
            if (tickCount > 100) {
                this.scheduleRemoval();
            }
            return;
        }

        if (motionDirty) {
            syncMotion();
        }

        try {
            onTick();

            if (this.inGround) {
                ticksInGround++;
            }


            if (this.tickCount >= this.getDeathTime()) {
                onExpireProc(this.getCaster());
                this.scheduleRemoval();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.scheduleRemoval();
        }

    }

    private void syncMotion() {
        if (level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(this, new ClientboundSetEntityMotionPacket(this));
        }
    }

    protected void setMotionDirty() {
        motionDirty = true;
    }

    private void setSpeed(double newSpeed) {
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();

        if (speed >= 1e-4) {
            double factor = newSpeed / speed;
            setDeltaMovement(velocity.scale(factor));
        } else if (newSpeed != 0.0) {
            // handle accelerating from an initial speed of 0
            Vector3f forward = entityData.get(FORWARD_VECTOR);
            setDeltaMovement(new Vec3(forward.mul((float) newSpeed)));
        }
    }

    private void applyAcceleration() {
        float acceleration = entityData.get(ACCELERATION);

        if (acceleration == 0f) {
            return;
        }

        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        setSpeed(Math.max(speed + acceleration, 0.0));
    }

    private void setPitch(float pitch) {
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);

        Vector3f velocity = getDeltaMovement().toVector3f();
        float speed = velocity.length();

        // try to determine yaw from velocity
        float speedXY = (float) Math.sqrt(Math.fma(velocity.x, velocity.x, velocity.z * velocity.z));

        if (speedXY < 1e-4f) {
            // try to determine yaw from forward vector
            velocity = entityData.get(FORWARD_VECTOR);
            speedXY = (float) Math.sqrt(Math.fma(velocity.x, velocity.x, velocity.z * velocity.z));
            if (speedXY < 1e-4f) {
                // determine yaw from up vector
                if (velocity.y > 0f) {
                    // head tilted back, invert
                    velocity = entityData.get(UP_VECTOR);
                    velocity.x *= -1f;
                    velocity.z *= -1f;
                } else {
                    velocity = entityData.get(UP_VECTOR);
                }
                speedXY = (float) Math.sqrt(Math.fma(velocity.x, velocity.x, velocity.z * velocity.z));
            }
        }

        float scale = cosPitch / speedXY;
        velocity.x *= scale;
        velocity.z *= scale;

        velocity.y = -sinPitch;

        velocity.mul(speed);

        setDeltaMovement(velocity.x, velocity.y, velocity.z);
    }

    private void adjustPitch(float angle) {
        Vector3f velocity = getDeltaMovement().toVector3f();
        Vector3f axis = velocity.cross(new Vector3f(0f, 1f, 0f)).normalize();
        velocity.rotateAxis(angle * Mth.DEG_TO_RAD, axis.x, axis.y, axis.z);
        setDeltaMovement(velocity.x, velocity.y, velocity.z);
    }

    private void adjustYaw(float angle) {
        Vector3f velocity = getDeltaMovement().toVector3f();
        Vector3f axis = entityData.get(UP_VECTOR);
        velocity.rotateAxis(-angle * Mth.DEG_TO_RAD, axis.x, axis.y, axis.z);
        setDeltaMovement(velocity.x, velocity.y, velocity.z);
    }

    private void applyYawVelocity() {
        float yawVelocity = entityData.get(YAW_VELOCITY);
        float yawAcceleration = entityData.get(YAW_ACCELERATION);

        if (yawVelocity == 0f && yawAcceleration == 0f) {
            return;
        }

        yawVelocity += yawAcceleration;
        entityData.set(YAW_VELOCITY, yawVelocity);

        adjustYaw(yawVelocity);
    }

    private void applyOrbit() {

        LivingEntity caster = getCaster();

        if (caster == null) {
            return;
        }

        // derived from tickCount rather than accumulated, so both sides land on the exact same
        // angle every tick without any per tick sync traffic, and a resynced client can't drift
        float angle = entityData.get(ORBIT_START_ANGLE) + tickCount * entityData.get(ORBIT_SPEED);
        double rad = angle * Mth.DEG_TO_RAD;
        double radius = entityData.get(ORBIT_RADIUS);

        Vec3 want = caster.position()
                .add(Math.cos(rad) * radius, entityData.get(ORBIT_Y_OFFSET), Math.sin(rad) * radius);

        if (level().isClientSide) {
            // the client owns its own player's position, so placing the orb here instead of waiting
            // for server position packets keeps the ring glued to you with no lag.
            // the delta is zeroed because vanilla's own movement step runs before this method every
            // tick, and a leftover velocity would drag the orb off the ring before we correct it
            setDeltaMovement(Vec3.ZERO);
            setPos(want);
        } else {
            // moving via the delta rather than setPos keeps a real movement vector for the next
            // tick's hit trace, which is what vanilla raycasts the projectile along.
            // deliberately no setMotionDirty(): the client ignores motion for orbiting projectiles,
            // so broadcasting it every tick for every orb would be pure bandwidth
            setDeltaMovement(want.subtract(position()));
        }
    }

    /**
     * Where this orb should really be drawn for a given partial tick, expressed as a delta from
     * where the renderer would otherwise put it. Ticking only produces 20 positions a second, and
     * an orbiting projectile's position is derived from the caster's, so lerping between its last
     * two ticked positions always trails the player by up to a full tick and cuts the circle into
     * chords. Recomputing against the caster's own interpolated render position removes both.
     */
    @Override
    public Vec3 getSmoothRenderOffset(float partialTicks) {

        if (!entityData.get(ORBITING)) {
            return Vec3.ZERO;
        }

        LivingEntity caster = getCaster();

        if (caster == null) {
            return Vec3.ZERO;
        }

        // tickCount - 1 + partial, because at partialTicks 0 every entity renders at its previous
        // tick's position, so that is the angle this offset has to line up with
        float angle = entityData.get(ORBIT_START_ANGLE) + (tickCount - 1 + partialTicks) * entityData.get(ORBIT_SPEED);
        double rad = angle * Mth.DEG_TO_RAD;
        double radius = entityData.get(ORBIT_RADIUS);

        Vec3 want = caster.getPosition(partialTicks)
                .add(Math.cos(rad) * radius, entityData.get(ORBIT_Y_OFFSET), Math.sin(rad) * radius);

        return want.subtract(this.getPosition(partialTicks));
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {

        if (entityData.get(ORBITING)) {
            // an orbiting projectile is fully reproducible on the client from the caster it can
            // already see, so the server's position is redundant here - and worse, it was computed
            // from where the server thought the caster was a few ticks ago. applying it snaps the
            // orb backwards, then the next tick pulls it forwards again, which is what jitters
            return;
        }

        super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {

        if (entityData.get(ORBITING)) {
            // same reasoning as lerpTo: a synced velocity would only fight applyOrbit
            return;
        }

        super.lerpMotion(x, y, z);
    }

    Entity target = null;

    public void tryMoveTowardsTargets() {

        if (moveTowardsCaster) {
            var speed = getDeltaMovement().length();
            var direction = ProjectileCastHelper.positionToVelocity(new MyPosition(position()), new MyPosition(getCaster().getEyePosition()));
            setDeltaMovement(direction.scale(speed));
            setMotionDirty();
            return;
        }

        if (moveTowardsEnemies) {

            if (target == null || !target.isAlive() || this.tickCount % 20 == 0) {

                int radius = getSpellData().getSpell().config.tracking_radius;

                var b = EntityFinder.start(getCaster(), LivingEntity.class, position())
                        .finder(EntityFinder.SelectionType.RADIUS)
                        .searchFor(getSpellData().getSpell().config.tracks)
                        .predicate(x -> AoeSelector.canHit(this.position(), x))
                        .radius(radius);

                target = b.getClosest();
            }

            if (target != null) {
                var speed = getDeltaMovement().length();
                var direction = ProjectileCastHelper.positionToVelocity(new MyPosition(position()), new MyPosition(target.getEyePosition()));
                setDeltaMovement(direction.scale(speed));
                setMotionDirty();
            }
        }
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 pos, Vec3 posPlusMotion) {

        EntityHitResult res = ProjectileUtil.getEntityHitResult(
                this.level(), this, pos, posPlusMotion, this.getBoundingBox()
                        .expandTowards(this.getDeltaMovement())
                        .inflate(1D), (e) -> {
                    return !e.isSpectator() && e.isPickable() && e instanceof Entity && e != this.getCaster() && e != this.ignoreEntity;
                });

        if (res != null && res.getEntity() instanceof LivingEntity le) {
            if (ServerContainer.get().isMnsDamageBlacklisted(le)) {
                return null; // blacklisted entities are invisible to spells, let the projectile fly through
            }
        }

        if (!this.entityData.get(HIT_ALLIES)) {
            if (res != null && getCaster() != null && res.getEntity() instanceof LivingEntity) {
                if (AllyOrEnemy.allies.is(getCaster(), (LivingEntity) res.getEntity())) {
                    return null; // don't hit allies with spells, let them pass
                }
            }
        }
        return res;
    }

    @Override
    protected void onHit(HitResult raytraceResultIn) {

        // super.onHit(raytraceResultIn); // adding this back seemed to fix proj a bit

        HitResult.Type raytraceresult$type = raytraceResultIn.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {

            this.onImpact(raytraceResultIn);

        } else if (raytraceresult$type == HitResult.Type.BLOCK) {

            if (collidedAlready) {
                return;
            }
            this.onImpact(raytraceResultIn);

            collidedAlready = true;

            this.inGround = true;

        }

    }

    protected void onImpact(HitResult result) {

        this.moveToImpactPosition(result);

        Entity entityHit = getEntityHit(result, 0.3D);

        if (entityHit != null) {
            if (level().isClientSide) {
                SoundUtils.playSound(this, SoundEvents.GENERIC_HURT, 1F, 0.9F);
            }

            LivingEntity caster = getCaster();

            LivingEntity en = null;

            if (entityHit instanceof LivingEntity == false) {
                // HARDCODED support for dumb ender dragon non living entity dragon parts
                if (entityHit instanceof EnderDragonPart) {
                    EnderDragonPart part = (EnderDragonPart) entityHit;
                    if (!part.isInvulnerableTo(this.damageSources().mobAttack(caster))) {
                        en = part.parentMob;
                    }
                }
            } else if (entityHit instanceof LivingEntity) {
                en = (LivingEntity) entityHit;
            }

            if (en == null) {
                return;
            }

            if (caster != null) {
                if (!Load.Unit(caster)
                        .alreadyHit(this, en)) {
                    if (!level().isClientSide) {
                        var ctx = SpellCtx.onHit(caster, this, en, getSpellData());

                        this.getSpellData()
                                .getSpell()
                                .getAttached()
                                .tryActivate(getScoreboardName(), ctx);
                    }
                }
            }

        } else {

            if (level().isClientSide) {
                SoundUtils.playSound(this, SoundEvents.STONE_HIT, 0.7F, 0.9F);
            }

        }

        if (entityHit != null) {
            if (!entityData.get(EXPIRE_ON_ENTITY_HIT)) {
                return;
            } else {
                scheduleRemoval();
            }
        }

        if (result instanceof BlockHitResult && entityData.get(EXPIRE_ON_BLOCK_HIT)) {
            scheduleRemoval();
        }


        if (!level().isClientSide) {


            if (getCaster() != null) {


                int chains = this.entityData.get(CHAINS).intValue();

                if (chains > 0) {
                    chains--;

                    if (entityHit == null) {
                        chains = 0;
                    }


                    var radius = getSpellData().data.getNumber(EventData.AREA_MULTI, 1F).number;

                    var b = EntityFinder.start(getCaster(), LivingEntity.class, position())
                            .finder(EntityFinder.SelectionType.RADIUS)
                            .searchFor(AllyOrEnemy.enemies)
                            .radius(5 * radius);

                    if (entityHit instanceof LivingEntity hit) {
                        b.excludeEntity(hit);
                    }
                    var target = b.getClosest();

                    if (target != null) {

                        SimpleProjectileEntity en = (SimpleProjectileEntity) getType().create(level());
                        en.setPos(position());
                        var sd = this.getSpellDataCopy(); // important so it doesnt affect old ones
                        sd.chains_did++; // when upping chain count
                        en.init(caster, sd, holder);
                        en.entityData.set(CHAINS, chains);
                        var vel = ProjectileCastHelper.positionToVelocity(new MyPosition(position()), new MyPosition(target.getEyePosition()));
                        en.setDeltaMovement(vel.normalize().multiply(speed, speed, speed));
                        level().addFreshEntity(en);

                    }
                }
            }
        }

    }

    boolean removeNextTick = false;

    public void scheduleRemoval() {
        if (!this.isRemoved()) {
            this.discard();
            removeNextTick = true;
        }
    }

    static Gson GSON = new Gson();

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {

        try {

            // super.writeCustomDataToTag(nbt);

            nbt.putInt("xTile", this.xTile);
            nbt.putInt("yTile", this.yTile);
            nbt.putInt("zTile", this.zTile);

            nbt.putByte("inGround", (byte) (this.inGround ? 1 : 0));

            nbt.putInt("deathTime", this.getDeathTime());

            nbt.putString("data", GSON.toJson(spellData));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {

        try {

//            super.readCustomDataFromTag(nbt);

            this.xTile = nbt.getInt("xTile");
            this.yTile = nbt.getInt("yTile");
            this.zTile = nbt.getInt("zTile");

            this.inGround = nbt.getByte("inGround") == 1;

            this.setDeathTime(nbt.getInt("deathTime"));

            this.spellData = GSON.fromJson(nbt.getString("data"), CalculatedSpellData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    LivingEntity caster;

    public LivingEntity getCaster() {
        if (caster == null) {
            try {
                this.caster = Utilities.getLivingEntityByUUID(level(), UUID.fromString(getSpellData().caster_uuid));
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        return caster;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SPELL_DATA, new CompoundTag());
        this.entityData.define(ENTITY_NAME, "");
        this.entityData.define(EXPIRE_ON_ENTITY_HIT, true);
        this.entityData.define(EXPIRE_ON_BLOCK_HIT, true);
        this.entityData.define(HIT_ALLIES, false);
        this.entityData.define(PIERCE, false);
        this.entityData.define(DEATH_TIME, 100);
        this.entityData.define(CHAINS, 0);
        this.entityData.define(ACCELERATION, 0f);
        this.entityData.define(YAW_VELOCITY, 0f);
        this.entityData.define(YAW_ACCELERATION, 0f);
        this.entityData.define(FORWARD_VECTOR, new Vector3f());
        this.entityData.define(UP_VECTOR, new Vector3f());
        this.entityData.define(ORBITING, false);
        this.entityData.define(ORBIT_RADIUS, 0f);
        this.entityData.define(ORBIT_SPEED, 0f);
        this.entityData.define(ORBIT_Y_OFFSET, 0f);
        this.entityData.define(ORBIT_START_ANGLE, 0f);
        super.defineSynchedData();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public CalculatedSpellData getSpellData() {
        try {
            if (level().isClientSide) {
                if (spellData == null) {
                    CompoundTag nbt = entityData.get(SPELL_DATA);
                    if (nbt != null) {
                        this.spellData = GSON.fromJson(nbt.getString("spell"), CalculatedSpellData.class);
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return spellData;
    }

    public CalculatedSpellData getSpellDataCopy() {
        return GSON.fromJson(GSON.toJson(getSpellData()), CalculatedSpellData.class);
    }

    @Override
    public ItemStack getItem() {
        try {
            Item item = VanillaUTIL.REGISTRY.items().get(new ResourceLocation(getSpellData().data.getString(EventData.ITEM_ID)));
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return new ItemStack(Items.AIR);
    }

    public String getScoreboardName() {
        return entityData.get(ENTITY_NAME);
    }

    @Override
    public void playerTouch(Player player) {
        // don't allow player to pickup lol
    }

    MapHolder holder;
    float speed = 0;

    @Override
    public void init(LivingEntity caster, CalculatedSpellData data, MapHolder holder) {
        this.holder = holder;
        this.spellData = data;

        this.pickup = Pickup.DISALLOWED;

        this.setNoGravity(!holder.getOrDefault(MapField.GRAVITY, true));

        int lifespan = holder.get(MapField.LIFESPAN_TICKS)
                .intValue();
        // opt in only: a projectile that IS the summon (summon_wisp) scales with summon duration,
        // while delivery projectiles and novas keep their fixed flight time
        if (!holder.getOrDefault(MapField.UNAFFECTED_BY_DURATION, true)) {
            lifespan *= data.data.getNumber(EventData.DURATION_MULTI, 1).number;
        }
        this.setDeathTime(lifespan);

        this.entityData.set(EXPIRE_ON_ENTITY_HIT, holder.getOrDefault(MapField.EXPIRE_ON_ENTITY_HIT, true));
        this.entityData.set(EXPIRE_ON_BLOCK_HIT, holder.getOrDefault(MapField.EXPIRE_ON_BLOCK_HIT, true));
        this.entityData.set(HIT_ALLIES, holder.getOrDefault(MapField.HITS_ALLIES, false));
        this.entityData.set(CHAINS, holder.getOrDefault(MapField.CHAIN_COUNT, 0D).intValue() + (int) data.data.getNumber(EventData.BONUS_CHAINS).number);

        this.checkInsideBlocks();

        if (data.data.getBoolean(EventData.PIERCE)) {
            this.entityData.set(EXPIRE_ON_ENTITY_HIT, false);
        }

        this.moveTowardsEnemies = holder.getOrDefault(MapField.TRACKS_ENEMIES, false);
        this.moveTowardsCaster = holder.getOrDefault(MapField.TRACKS_CASTER, false);
        this.speed = holder.getOrDefault(MapField.PROJECTILE_SPEED, 1D).floatValue();

        this.entityData.set(ACCELERATION, holder.getOrDefault(MapField.PROJECTILE_ACCELERATION, 0D).floatValue() * getSpeedMultiplier());

        this.entityData.set(YAW_VELOCITY, holder.getOrDefault(MapField.YAW_VELOCITY, 0D).floatValue() * getYawSpeedMultiplier());
        this.entityData.set(YAW_ACCELERATION, holder.getOrDefault(MapField.YAW_ACCELERATION, 0D).floatValue() * getYawSpeedMultiplier());

        // orbit speed is deliberately NOT scaled by the projectile speed stats, otherwise a fast
        // projectile build would spin the ring apart instead of keeping it in formation
        this.entityData.set(ORBITING, holder.getOrDefault(MapField.ORBITS_CASTER, false));
        this.entityData.set(ORBIT_RADIUS, holder.getOrDefault(MapField.ORBIT_RADIUS, 2D).floatValue());
        this.entityData.set(ORBIT_SPEED, holder.getOrDefault(MapField.ORBIT_SPEED, 3D).floatValue());
        this.entityData.set(ORBIT_Y_OFFSET, holder.getOrDefault(MapField.ORBIT_Y_OFFSET, 1D).floatValue());

        if (this.entityData.get(ORBITING)) {
            // vanilla AbstractArrow latches its own inGround flag whenever the arrow sits inside a
            // block collision shape and then stops moving it entirely, which would freeze an orbit
            // the moment the caster walks past a wall. no physics skips that whole branch.
            // it also skips onHit, which is fine: orbiting projectiles damage from their attached
            // spell tick, not from collision
            this.setNoPhysics(true);
        }

        data.data.setString(EventData.ITEM_ID, holder.get(MapField.ITEM));
        CompoundTag nbt = new CompoundTag();
        nbt.putString("spell", GSON.toJson(spellData));
        entityData.set(SPELL_DATA, nbt);
        this.setOwner(caster);

        String name = holder.get(MapField.ENTITY_NAME);
        entityData.set(ENTITY_NAME, name);

    }

    @Override
    public void setVectors(Vector3f forward, Vector3f up) {
        this.entityData.set(FORWARD_VECTOR, forward);
        this.entityData.set(UP_VECTOR, up);

        // ProjectileCastHelper calls this after init(), so this is the first point where the per
        // projectile spawn direction is known. a nova cast spaces the directions evenly, which is
        // what spreads an orbiting ring out evenly around the caster
        if (this.entityData.get(ORBITING)) {
            this.entityData.set(ORBIT_START_ANGLE, (float) Mth.atan2(forward.z, forward.x) * Mth.RAD_TO_DEG);
        }
    }

    @Override
    public void handleModifyProjectileAction(MapHolder data) {
        if (data.has(MapField.PROJECTILE_SPEED)) {
            setSpeed(data.get(MapField.PROJECTILE_SPEED) * getSpeedMultiplier());
            setMotionDirty();
        }
        if (data.has(MapField.PROJECTILE_ACCELERATION)) {
            entityData.set(ACCELERATION, data.get(MapField.PROJECTILE_ACCELERATION).floatValue() * getSpeedMultiplier());
        }
        if (data.has(MapField.PITCH)) {
            setPitch(data.get(MapField.PITCH).floatValue());
            setMotionDirty();
        }
        if (data.has(MapField.PITCH_OFFSET)) {
            adjustPitch(data.get(MapField.PITCH_OFFSET).floatValue());
            setMotionDirty();
        }
        if (data.has(MapField.YAW_OFFSET)) {
            adjustYaw(data.get(MapField.YAW_OFFSET).floatValue());
            setMotionDirty();
        }
        if (data.has(MapField.YAW_VELOCITY)) {
            entityData.set(YAW_VELOCITY, data.get(MapField.YAW_VELOCITY).floatValue() * getYawSpeedMultiplier());
        }
        if (data.has(MapField.YAW_ACCELERATION)) {
            entityData.set(YAW_ACCELERATION, data.get(MapField.YAW_ACCELERATION).floatValue() * getYawSpeedMultiplier());
        }
    }
}
