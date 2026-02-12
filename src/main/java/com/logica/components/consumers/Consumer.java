package com.logica.components.consumers;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NetComp;
import com.logica.utils.PowerUtil;
import com.logica.vars.Orientation;

/**
 * Base class for consumers that need to update their visual state when power changes.
 */
public abstract class Consumer extends NetComp {
    protected Consumer(Vector3i position) {
        super(position);
    }

    /**
     * Update the on/off state and render if it changed.
     */
    protected void applyStateChange(World world, boolean newState) {
        boolean oldState = state.isOn();
        if (oldState != newState) {
            state.setOn(newState);
            render(world);
        }
    }

    protected boolean isPowered(World world) {
        if (!state.activeSources().isEmpty())
            return true;

        for (Orientation o : Orientation.ALL) {
            Vector3i neighborPos = new Vector3i(getPosition().x + o.getDirection().x,
                    getPosition().y + o.getDirection().y,
                    getPosition().z + o.getDirection().z);
            if (PowerUtil.isSolidBlockReceivingStrongPower(world, neighborPos, getPosition(), true))
                return true;
        }
        return false;
    }

    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        return false;
    }
}


