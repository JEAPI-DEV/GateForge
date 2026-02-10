package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * NOR Gate implementation.
 */
public class NORGate extends Gate {
    public NORGate(Vector3i position) {
        super(position);
        setStrategy(inputs -> {
            if (inputs == null)
                return true;
            for (boolean input : inputs)
                if (input)
                    return false;
            return true;
        });
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
