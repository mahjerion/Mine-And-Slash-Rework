package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AttachedSpell {

    public List<ComponentPart> on_cast = new ArrayList<>();

    public HashMap<String, List<ComponentPart>> entity_components = new HashMap<>();

    public void onCast(SpellCtx ctx) {
        on_cast.forEach(x -> x.tryActivate(ctx));
    }

    public List<ComponentPart> getDataForEntity(String en) {
        return entity_components.get(en);
    }

    public void tryActivate(String entity_name, SpellCtx ctx) {
        try {
            if (entity_components.containsKey(entity_name)) {
                for (ComponentPart entry : entity_components.get(entity_name)) {
                    entry.tryActivate(ctx);
                }
            } else {
                //ExileLog.get().log("Spell doesn't have data for spell entity called: " + entity_name + ". Spell id: " + ctx.calculatedSpellData.getSpell()
                //   .GUID());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // every part of the spell, including the per_entity_hit sub-parts nested under a part.
    // tooltips scan this for applied effects, so an effect given only "per entity hit" (pack
    // spells like timewinder) must be visible here too, one level deep like runtime executes it
    public List<ComponentPart> getAllComponents() {
        List<ComponentPart> top = new ArrayList<>();
        top.addAll(this.on_cast);
        this.entity_components.entrySet()
            .forEach(x -> top.addAll(x.getValue()));

        List<ComponentPart> list = new ArrayList<>();
        for (ComponentPart part : top) {
            list.add(part);
            list.addAll(part.getPerEntityHit());
        }
        return list;
    }

}
