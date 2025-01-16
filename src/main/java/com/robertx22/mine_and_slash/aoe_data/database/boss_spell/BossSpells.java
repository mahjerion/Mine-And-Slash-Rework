package com.robertx22.mine_and_slash.aoe_data.database.boss_spell;

import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class BossSpells {

    public static void init() {

        new BossDealCloseAoe().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new SummonExplodyMobs().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);

    }
}
