package com.robertx22.mine_and_slash.aoe_data.database.entity_configs;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.database.data.EntityConfig;
import com.robertx22.mine_and_slash.database.data.EntityConfig.SpecialMobStats;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityTypeUtils;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;

public class EntityConfigs implements ExileRegistryInit {

    void setupBroadClasses() {

        new EntityConfig(EntityTypeUtils.EntityClassification.MOB.name()
                .toLowerCase(Locale.ROOT), 1).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        EntityConfig animal = new EntityConfig(EntityTypeUtils.EntityClassification.ANIMAL.name()
                .toLowerCase(Locale.ROOT), 0);
        animal.hp_multi -= 0.5F;
        animal.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        EntityConfig npc = new EntityConfig(EntityTypeUtils.EntityClassification.NPC.name()
                .toLowerCase(Locale.ROOT), 0);
        npc.hp_multi += 0.2F;
        npc.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityTypeUtils.EntityClassification.OTHER.name()
                .toLowerCase(Locale.ROOT), 0).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityTypeUtils.EntityClassification.AMBIENT.name()
                .toLowerCase(Locale.ROOT), 0).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityTypeUtils.EntityClassification.PLAYER.name()
                .toLowerCase(Locale.ROOT), 0).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);


    }

    void setupSpecificMobs() {

        EntityConfig irongolem = new EntityConfig(EntityType.IRON_GOLEM, 0);
        irongolem.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

        EntityConfig polarbear = new EntityConfig(EntityType.POLAR_BEAR, 0);
        polarbear.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

        new EntityConfig(EntityType.ZOMBIFIED_PIGLIN, 0.5F).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        var zombie = new EntityConfig(EntityType.ZOMBIE, 0.75F);
        zombie.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityType.DROWNED, 0.75F).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityType.BEE, 0.8F).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        new EntityConfig(EntityType.WOLF, 0.5F).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

        EntityConfig enderman = new EntityConfig(EntityType.ENDERMAN, 1);
        enderman.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

        EntityConfig wither = mob(EntityType.WITHER, new SpecialMobStats());
        wither.min_lvl = 30;

        EntityConfig dragon = mob(EntityType.ENDER_DRAGON, new SpecialMobStats());
        dragon.min_lvl = 40;

    }

    void setupWholeMods() {


        new EntityConfig("lycanite_mobs", 1.2F).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

        EntityConfig blue = new EntityConfig("blue_skies", 1);
        blue.set_health_damage_override = true;
        blue.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

    }

    void setupMyMobs() {

    }

    @Override
    public void registerAll() {
        this.setupBroadClasses();
        this.setupMyMobs();
        this.setupSpecificMobs();
        this.setupWholeMods();
    }

    private EntityConfig mob(EntityType type, SpecialMobStats stats) {
        EntityConfig c = new EntityConfig(type, 1);
        c.stats = stats;
        c.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        return c;
    }

}
