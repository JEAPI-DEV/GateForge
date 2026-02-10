package com.logica.components.consumers;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NetComp;

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
        } else {
            state.setOn(newState);
        }
    }
}
