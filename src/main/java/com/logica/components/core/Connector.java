package com.logica.components.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.interfaces.ILogicaComponent;

/**
 * Abstract class for components that primarily transport signals.
 * Inherits from LogicComponent.
 */
public abstract class Connector extends NetComp {
    public Connector(Vector3i position) {
        super(position);
    }

    public abstract void updateShape(World world);

    /**
     * Determines if this connector can connect to another network component.
     *
     * @param other The other network component.
     * @return True if a connection is possible.
     */
    public abstract boolean canConnectTo(ILogicaComponent other);
}
