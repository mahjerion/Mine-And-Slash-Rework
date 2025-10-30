package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.entities.IDatapackProjectileEntity;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class ModifyProjectileAction extends SpellAction {

    public ModifyProjectileAction() {
        super(Arrays.asList());
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        ((IDatapackProjectileEntity) ctx.sourceEntity).handleModifyProjectileAction(data);
    }

    public MapHolder create(Optional<Double> projSpeed, Optional<Double> projAccel, Optional<Double> pitch, Optional<Double> pitchOffset,
                            Optional<Double> yawOffset, Optional<Double> yawVelocity, Optional<Double> yawAccel) {
        MapHolder c = new MapHolder();
        c.type = GUID();
        if (projSpeed.isPresent()) {
            c.put(MapField.PROJECTILE_SPEED, projSpeed.get());
        }
        if (projAccel.isPresent()) {
            c.put(MapField.PROJECTILE_ACCELERATION, projAccel.get());
        }
        if (pitch.isPresent()) {
            c.put(MapField.PITCH, pitch.get());
        }
        if (pitchOffset.isPresent()) {
            c.put(MapField.PITCH_OFFSET, pitchOffset.get());
        }
        if (yawOffset.isPresent()) {
            c.put(MapField.YAW_OFFSET, yawOffset.get());
        }
        if (yawVelocity.isPresent()) {
            c.put(MapField.YAW_VELOCITY, yawVelocity.get());
        }
        if (yawAccel.isPresent()) {
            c.put(MapField.YAW_ACCELERATION, yawAccel.get());
        }
        return c;
    }

    @Override
    public String GUID() {
        return "modify_proj";
    }

}
