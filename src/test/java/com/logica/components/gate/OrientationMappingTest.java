package com.logica.components.gate;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrientationMappingTest {

    @Test
    void toWorldRotationZeroMatchesWorldAxes() {
        assertEquals(new Vector3i(0, 0, -1), Orientation.toWorld(Orientation.NORTH, 0).getDirection());
        assertEquals(new Vector3i(1, 0, 0), Orientation.toWorld(Orientation.EAST, 0).getDirection());
        assertEquals(new Vector3i(0, 0, 1), Orientation.toWorld(Orientation.SOUTH, 0).getDirection());
        assertEquals(new Vector3i(-1, 0, 0), Orientation.toWorld(Orientation.WEST, 0).getDirection());
        assertEquals(new Vector3i(0, 1, 0), Orientation.toWorld(Orientation.UP, 0).getDirection());
        assertEquals(new Vector3i(0, -1, 0), Orientation.toWorld(Orientation.DOWN, 0).getDirection());
    assertEquals(Orientation.NORTH, Orientation.fromRotationIndex(0));
    assertEquals(Orientation.WEST, Orientation.fromRotationIndex(1));
    }

    @Test
    void toWorldRotationQuarterTurns() {
        Orientation north = Orientation.toWorld(Orientation.NORTH, 1);
        Orientation east = Orientation.toWorld(Orientation.EAST, 1);
        Orientation south = Orientation.toWorld(Orientation.SOUTH, 1);
        Orientation west = Orientation.toWorld(Orientation.WEST, 1);

        assertNotNull(north);
        assertNotNull(east);
        assertNotNull(south);
        assertNotNull(west);

        assertEquals(new Vector3i(-1, 0, 0), north.getDirection());
        assertEquals(new Vector3i(0, 0, -1), east.getDirection());
        assertEquals(new Vector3i(1, 0, 0), south.getDirection());
        assertEquals(new Vector3i(0, 0, 1), west.getDirection());
    }

    @Test
    void notGateLocalInputIsSouth() {
        NotGate gate = new NotGate(new Vector3i(0, 0, 0));
        Vector3i localInput = gate.getInputDirections().getFirst();
        assertEquals(Orientation.SOUTH.getDirection(), localInput);
    }
}
