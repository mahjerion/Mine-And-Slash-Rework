package com.robertx22.mine_and_slash.aoe_data.database.mercenaries;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.spells.schools.MercenarySpells;
import com.robertx22.mine_and_slash.aoe_data.database.stats.old.DatapackStats;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.saveclasses.PointData;

public class Mercenaries implements ExileRegistryInit {

    public static String FIGHTER = "fighter";
    public static String ELEMENTALIST = "elementalist";
    public static String HUNTER = "hunter";

    @Override
    public void registerAll() {

        MercenaryClass fighter = new MercenaryClass();
        fighter.id = FIGHTER;
        fighter.locname = "Fighter";
        fighter.icon = "fighter";
        fighter.texture = "mmorpg:textures/entity/fighter.png";

        fighter.base_stats.put(DatapackStats.STR.GUID(), 5F);
        fighter.base_stats.put(DatapackStats.DEX.GUID(), 3F);
        fighter.base_stats.put(DatapackStats.INT.GUID(), 3F);

        fighter.stats_per_level.put(DatapackStats.STR.GUID(), 1F);

        fighter.stats_per_10_lvl.put(DatapackStats.STR.GUID(), 5F);
        fighter.stats_per_10_lvl.put(DatapackStats.DEX.GUID(), 3F);
        fighter.stats_per_10_lvl.put(DatapackStats.INT.GUID(), 3F);

        // the y row picks the unlock level out of lvl_reqs: row 0 is level 1, row 2 is level 10.
        fighter.skills.put(MercenarySpells.MERC_GONG_STRIKE, new PointData(0, 0));
        fighter.skills.put(MercenarySpells.MERC_PULL, new PointData(1, 0));
        fighter.skills.put(MercenarySpells.MERC_TAUNT, new PointData(1, 1));

        MercenaryClass elementalist = new MercenaryClass();
        elementalist.id = ELEMENTALIST;
        elementalist.locname = "Elementalist";
        elementalist.icon = "elementalist";
        elementalist.texture = "mmorpg:textures/entity/elementalist.png";
        // stays at range and kites instead of closing to melee
        elementalist.ai_behavior = MercenaryClass.AiBehavior.RANGED;

        elementalist.base_stats.put(DatapackStats.STR.GUID(), 3F);
        elementalist.base_stats.put(DatapackStats.DEX.GUID(), 3F);
        elementalist.base_stats.put(DatapackStats.INT.GUID(), 5F);

        elementalist.stats_per_level.put(DatapackStats.INT.GUID(), 1F);

        elementalist.stats_per_10_lvl.put(DatapackStats.STR.GUID(), 3F);
        elementalist.stats_per_10_lvl.put(DatapackStats.DEX.GUID(), 3F);
        elementalist.stats_per_10_lvl.put(DatapackStats.INT.GUID(), 5F);

        // the y row picks the unlock level out of lvl_reqs: row 0 is level 1, row 2 is level 10.
        elementalist.skills.put(MercenarySpells.MERC_FIREBALL, new PointData(0, 0));
        elementalist.skills.put(MercenarySpells.MERC_METEOR, new PointData(0, 2));
        elementalist.skills.put(MercenarySpells.MERC_FROST_NOVA, new PointData(2, 1));
        elementalist.skills.put(MercenarySpells.MERC_MAGE_CIRCLE, new PointData(3, 2));

        MercenaryClass hunter = new MercenaryClass();
        hunter.id = HUNTER;
        hunter.locname = "Hunter";
        hunter.icon = "hunter";
        hunter.texture = "mmorpg:textures/entity/hunter.png";
        // a bow class holds its distance, same as the elementalist
        hunter.ai_behavior = MercenaryClass.AiBehavior.RANGED;

        hunter.base_stats.put(DatapackStats.STR.GUID(), 3F);
        hunter.base_stats.put(DatapackStats.DEX.GUID(), 5F);
        hunter.base_stats.put(DatapackStats.INT.GUID(), 3F);

        hunter.stats_per_level.put(DatapackStats.DEX.GUID(), 1F);

        hunter.stats_per_10_lvl.put(DatapackStats.STR.GUID(), 3F);
        hunter.stats_per_10_lvl.put(DatapackStats.DEX.GUID(), 5F);
        hunter.stats_per_10_lvl.put(DatapackStats.INT.GUID(), 3F);

        // the y row picks the unlock level out of lvl_reqs: row 0 is level 1, row 2 is level 10.
        hunter.skills.put(MercenarySpells.MERC_ARROW_BARRAGE, new PointData(0, 0));
        hunter.skills.put(MercenarySpells.MERC_FIRE_TRAP, new PointData(2, 1));
        hunter.skills.put(MercenarySpells.MERC_SUMMON_WOLF, new PointData(1, 2));

        fighter.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        elementalist.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        hunter.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }
}
