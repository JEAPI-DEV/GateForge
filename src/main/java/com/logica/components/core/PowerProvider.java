package com.logica.components.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;

public abstract class PowerProvider extends NetComp {

    protected PowerProvider(Vector3i position) {
        super(position);
    }

    public void updateOutput(World world, boolean state){
        if(state == this.state.isOn()) return;
        this.state.setOn(state);
        notifyNeighbors(world);
    }

    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        if (!isActive() || neighborPos == null)
            return false;
        int dx = Math.abs(neighborPos.x - getPosition().x);
        int dy = Math.abs(neighborPos.y - getPosition().y);
        int dz = Math.abs(neighborPos.z - getPosition().z);
        return (dx + dy + dz) == 1;
    }
}
