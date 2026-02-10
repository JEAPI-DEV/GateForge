package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * XOR Gate implementation.
 */
public class XORGate extends Gate {
    public XORGate(Vector3i position) {
        super(position);
        setStrategy(inputs -> {
            if (inputs == null)
                return false;
            int count = 0;
            for (boolean input : inputs)
                if (input)
                    count++;
            return count == 1;
        });
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
