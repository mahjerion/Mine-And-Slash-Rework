package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;

public class TpToCaster extends SpellAction {
    public TpToCaster() {
        super(new ArrayList<>());
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        for (LivingEntity target : targets) {
            // enemies land in front of the caster rather than inside them. allies (return summons)
            // keep arriving at the exact position.
            Vec3 dest = AllyOrEnemy.enemies.is(ctx.caster, target) ?
                    TeleportTargetToSourceAction.pullDestination(ctx.caster, target) :
                    ctx.caster.position();

            // this used to setLoc straight to the destination with no collision check at all, which
            // could stuff a mob inside a wall
            TeleportTargetToSourceAction.teleportEntitySafe(target, dest);
        }
    }

    public MapHolder create() {
        MapHolder c = new MapHolder();
        c.type = GUID();
        this.validate(c);
        return c;
    }

    @Override
    public String GUID() {
        return "tp_to_caster";
    }
}
