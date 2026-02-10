package com.logica.components.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.vars.Orientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility helpers for neighbor traversal used across components.
 */
public final class NeighborScanner {

    private NeighborScanner() {
    }

    /**
     * Returns the 6 orthogonal neighbor positions around {@code pos}.
     */
    public static List<Vector3i> sixWay(Vector3i pos) {
        if (pos == null) return Collections.emptyList();
        List<Vector3i> result = new ArrayList<>(6);
        for (Orientation o : Orientation.ALL) {
            Vector3i dir = o.getDirection();
            result.add(new Vector3i(pos.x + dir.x, pos.y + dir.y, pos.z + dir.z));
        }
        return result;
    }

    /**
     * Returns the vertical diagonal positions used by pipes (horizontal +/- one in Y).
     */
    public static List<Vector3i> pipeVerticalDiagonals(Vector3i pos) {
        if (pos == null) return Collections.emptyList();
        int[][] horizontal = new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        List<Vector3i> result = new ArrayList<>(horizontal.length * 2);
        for (int[] h : horizontal) {
            result.add(new Vector3i(pos.x + h[0], pos.y + 1, pos.z + h[1]));
            result.add(new Vector3i(pos.x + h[0], pos.y - 1, pos.z + h[1]));
        }
        return result;
    }
}
