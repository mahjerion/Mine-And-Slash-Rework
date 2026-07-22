package com.robertx22.mine_and_slash.database.data.shrine;

import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

// Default shrine buff options shipped as generated datapack JSON. Add more entries here (or drop new
// JSON into the shrine_buff datapack folder) and shrines will start picking them at weighted random.
// Any ExileEffect used here should carry EffectTags.shrine (see ModEffects.java) so the Atlas "Shrine
// buff effectiveness on you" stat (EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG) applies to it.
public class ShrineBuffs {

    public static void init() {

        // PLACEHOLDER: only Valor for now. Extend this list with whatever exile effects shrines should grant.
        new ShrineBuff("valor", ModEffects.VALOR.id, 1000, 20 * 60)
                .addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

    }
}
