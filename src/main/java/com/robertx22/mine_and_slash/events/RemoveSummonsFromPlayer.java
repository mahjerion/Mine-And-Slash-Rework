package com.robertx22.mine_and_slash.events;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;

public class RemoveSummonsFromPlayer extends EventConsumer<MineAndSlashEvents.OnPerkUnlearnedAndRemoved> {
    @Override
    public void accept(MineAndSlashEvents.OnPerkUnlearnedAndRemoved event) {
        var perk = ExileDB.Perks().get(event.perk);
        Spell spell = perk.getSpell();

        if (spell == null || !spell.getConfig().tags.contains(SpellTags.has_pet_ability)) {
            return;
        }

        Load.player(event.player).removeSummonType(spell.identifier);
    }

    @Override
    public int callOrder() {
        return 1;
    }
}
