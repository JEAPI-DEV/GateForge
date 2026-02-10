package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * AND Gate implementation.
 */
public class AndGate extends Gate {
    public AndGate(Vector3i position) {
        super(position);
        setStrategy(inputs -> inputs != null && inputs.size() >= 2 && inputs.getFirst() && inputs.get(1));
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
