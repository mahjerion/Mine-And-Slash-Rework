package com.robertx22.mine_and_slash.database.data.omen;

import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.blueprints.bases.BlueprintPart;

public class OmenPart extends BlueprintPart<Omen, OmenBlueprint> {

    public OmenPart(OmenBlueprint blueprint) {
        super(blueprint);
    }

    @Override
    protected Omen generateIfNull() {
        int lvl = this.blueprint.info.level;
        var omen = DroppableOmens(lvl).random();
        // the drop path guards this with OmenLootGen.condition(), but the prophecy reward roll has no
        // such hook - the coins are already spent by then, so never hand back a null to NPE on
        return omen != null ? omen : ExileDB.Omens().random();
    }

    public static FilterListWrap<Omen> DroppableOmens(int lvl) {
        return ExileDB.Omens().getFilterWrapped(x -> lvl >= GameBalanceConfig.get().MAX_LEVEL * x.lvl_req);
    }
}
