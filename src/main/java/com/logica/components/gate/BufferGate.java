package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;

import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * Buffer Gate implementation.
 */
public class BufferGate extends Gate {
    public BufferGate(Vector3i position) {
        super(position, LogicaConstants.BlockId.GATE_BUFFER);
        setStrategy(inputs -> inputs != null && !inputs.isEmpty() && inputs.getFirst());
    }

    @Override
    protected List<Vector3i> getInputDirections() {
        return List.of(Orientation.SOUTH.getDirection());
    }
}
