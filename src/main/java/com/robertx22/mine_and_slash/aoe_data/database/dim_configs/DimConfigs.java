package com.robertx22.mine_and_slash.aoe_data.database.dim_configs;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.database.data.DimensionConfig;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class DimConfigs implements ExileRegistryInit {

    @Override
    public void registerAll() {

        DimensionConfig.Overworld().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        DimensionConfig.Nether().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        DimensionConfig.End().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        DimensionConfig.DefaultExtra().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

    }
}
