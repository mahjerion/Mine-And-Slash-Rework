package com.robertx22.mine_and_slash.aoe_data.database.prophecies;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.database.data.prophecy.starts.*;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class ProphecyStarts implements ExileRegistryInit {
    @Override
    public void registerAll() {
        new ProphecyUniqueGearProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new GearProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new RuneProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new JewelProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new AuraGemProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new SupportGemProphecy().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
    }
}
