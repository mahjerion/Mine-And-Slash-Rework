package com.robertx22.mine_and_slash.database.data.spells.entities;

import org.joml.Vector3f;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;

public interface IDatapackProjectileEntity extends IDatapackSpellEntity {

    public void setVectors(Vector3f forward, Vector3f up);
    public void handleModifyProjectileAction(MapHolder data);

}
