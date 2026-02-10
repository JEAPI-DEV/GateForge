package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * NOT Gate implementation.
 */
public class NotGate extends Gate {
    public NotGate(Vector3i position) {
        super(position);
        setStrategy(inputs -> inputs == null || inputs.isEmpty() || !inputs.getFirst());
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.SOUTH.getDirection());
    }
}
