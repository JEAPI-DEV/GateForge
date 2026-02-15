package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;

import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * OR Gate implementation.
 */
public class OrGate extends Gate {
    public OrGate(Vector3i position) {
        super(position, LogicaConstants.BlockId.GATE_OR);
        setStrategy(inputs -> {
            if (inputs == null)
                return false;
            for (boolean input : inputs)
                if (input)
                    return true;
            return false;
        });
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.WEST.getDirection(), Orientation.EAST.getDirection());
    }
}
