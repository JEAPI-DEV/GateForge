package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * NAND Gate implementation.
 * Outputs OFF only if all active inputs are ON and there is at least one active
 * input.
 * Otherwise outputs ON.
 */
public class NANDGate extends Gate {
    public NANDGate(Vector3i position) {
        super(position);
        // Robust NAND: only OFF if we have inputs and all are true.
        setStrategy(inputs -> {
            if (inputs.size() < 2)
                return true;
            return !(inputs.get(0) && inputs.get(1));
        });
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
