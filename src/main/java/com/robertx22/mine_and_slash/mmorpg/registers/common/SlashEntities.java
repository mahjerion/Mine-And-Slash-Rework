package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.entities.*;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SkeletonSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SpiderPet;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.WolfSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.ZombieSummon;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.ColdGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.FireGolem;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems.LightningGolem;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class SlashEntities {


    public static RegObj<EntityType<SimpleProjectileEntity>> SIMPLE_PROJECTILE = projectile(SimpleProjectileEntity::new, "spell_projectile");
    public static RegObj<EntityType<SimpleArrowEntity>> SIMPLE_ARROW = projectile(SimpleArrowEntity::new, "spell_arrow");
    public static RegObj<EntityType<StationaryFallingBlockEntity>> SIMPLE_BLOCK_ENTITY = projectile(StationaryFallingBlockEntity::new, "spell_block_entity", false);
    public static RegObj<EntityType<SimpleTridentEntity>> SIMPLE_TRIDENT = projectile(SimpleTridentEntity::new, "spell_trident", false);
    public static RegObj<EntityType<AutoAimingProj>> AUTO_AIMING_SKELETON_SKULL = projectile(AutoAimingProj::new, "auto_aim_skull", false);


    // summons
    public static RegObj<EntityType<WolfSummon>> SPIRIT_WOLF = mob(WolfSummon::new, EntityType.WOLF, "spirit_wolf");
    public static RegObj<EntityType<ZombieSummon>> ZOMBIE = mob(ZombieSummon::new, EntityType.SKELETON, "zombie");
    public static RegObj<EntityType<SkeletonSummon>> SKELETON = mob(SkeletonSummon::new, EntityType.SKELETON, "skeleton");
    public static RegObj<EntityType<SpiderPet>> SPIDER = mob(SpiderPet::new, EntityType.CAVE_SPIDER, "spider");

    // hired companions. sized off the zombie because that is the model the renderer bakes, so the
    // hitbox and the thing you see line up.
    public static RegObj<EntityType<MercenaryEntity>> MERCENARY = mob(MercenaryEntity::new, EntityType.ZOMBIE, "mercenary");

    public static RegObj<EntityType<FireGolem>> FIRE_GOLEM = mob(FireGolem::new, EntityType.WOLF, "fire_golem");
    public static RegObj<EntityType<ColdGolem>> COLD_GOLEM = mob(ColdGolem::new, EntityType.WOLF, "cold_golem");
    public static RegObj<EntityType<LightningGolem>> LIGHTNING_GOLEM = mob(LightningGolem::new, EntityType.WOLF, "lightning_golem");

    // elemental caster monsters. four types rather than one with a variant field because map spawn
    // pools weight by entity id - library_of_exile_mob_list names mobs, not variants - so this is
    // the only way a map can ask for an even split across the four elements. everything that makes
    // them different is datapack: see WizardType, keyed on the id below.
    public static RegObj<EntityType<WizardEntity>> FIRE_WIZARD = monster(WizardEntity::new, EntityType.WITCH, "fire_wizard");
    public static RegObj<EntityType<WizardEntity>> ICE_WIZARD = monster(WizardEntity::new, EntityType.WITCH, "ice_wizard");
    public static RegObj<EntityType<WizardEntity>> LIGHTNING_WIZARD = monster(WizardEntity::new, EntityType.WITCH, "lightning_wizard");
    public static RegObj<EntityType<WizardEntity>> CHAOS_WIZARD = monster(WizardEntity::new, EntityType.WITCH, "chaos_wizard");


    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory, String id) {
        return projectile(factory, id, true);

    }

    /**
     * A genuinely hostile mob, unlike {@link #mob} above - which is MISC because everything it
     * registers is a summon or a companion that is never naturally spawned.
     * <p>
     * MONSTER is the honest category and it is what vanilla's despawn rules read, but it is not what
     * makes these spawn: nothing puts them in a biome's spawner list, and the map dimension denies
     * natural spawning outright. A map's mob list places them explicitly, which is exactly why they
     * can only ever appear in one.
     * <p>
     * Tracking range is wider than the summons' 10 as well - a wizard is a ranged attacker that
     * fights from nine blocks and throws things twenty, so at 10 a player would watch a fireball
     * arrive from an entity that hadn't been sent to them yet.
     */
    private static <T extends Entity> RegObj<EntityType<T>> monster(EntityType.EntityFactory<T> factory, EntityType like, String id) {
        return Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(like.getDimensions().width, like.getDimensions().height)
                .setTrackingRange(32)
                .build(id));
    }

    private static <T extends Entity> RegObj<EntityType<T>> mob(EntityType.EntityFactory<T> factory, EntityType like, String id) {

        RegObj<EntityType<T>> def = Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(like.getDimensions().width, like.getDimensions().height)
                .setTrackingRange(10)
                .build(id));


        return def;
    }

    private static <T extends Entity> RegObj<EntityType<T>> projectile(EntityType.EntityFactory<T> factory,
                                                                       String id, boolean itemRender) {

        RegObj<EntityType<T>> def = Def.entity(id, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .setUpdateInterval(10)
                .setTrackingRange(4)
                .build(id));

        return def;
    }

    public static void init() {

    }

}


