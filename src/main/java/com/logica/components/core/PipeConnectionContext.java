package com.logica.components.core;

import com.logica.vars.Orientation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PipeConnectionContext {
    private final Set<Orientation> horizontalConnections = EnumSet.noneOf(Orientation.class);
    private final Set<Orientation> verticalConnections = EnumSet.noneOf(Orientation.class); // "Climbing" connections

    // Flags for easy access
    public boolean n, s, e, w;
    public boolean vn, vs, ve, vw;
    public boolean up, down;

    public PipeConnectionContext(Map<Orientation, NeighborInfo> neighbors,
                                 Set<Orientation> activeConnections, Set<Orientation> climbingConnections) {

        if (activeConnections != null) {
            this.horizontalConnections.addAll(activeConnections);
        }
        if (climbingConnections != null) {
            this.verticalConnections.addAll(climbingConnections);
        }

        calculateFlags();
    }

    private void calculateFlags() {
        n = horizontalConnections.contains(Orientation.NORTH);
        s = horizontalConnections.contains(Orientation.SOUTH);
        e = horizontalConnections.contains(Orientation.EAST);
        w = horizontalConnections.contains(Orientation.WEST);

        vn = verticalConnections.contains(Orientation.NORTH);
        vs = verticalConnections.contains(Orientation.SOUTH);
        ve = verticalConnections.contains(Orientation.EAST);
        vw = verticalConnections.contains(Orientation.WEST);
        up = horizontalConnections.contains(Orientation.UP);
        down = horizontalConnections.contains(Orientation.DOWN);
    }

    public int getHorizontalConnectionCount() {
        return (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);
    }

    public int getVerticalClimbCount() {
        return (vn ? 1 : 0) + (vs ? 1 : 0) + (ve ? 1 : 0) + (vw ? 1 : 0);
    }

    public Set<Orientation> getRelativeVerticals(int rotation) {
        // Calculate Local Vertical Connections based on rotation
        boolean localVN = false, localVE = false, localVS = false, localVW = false;

        switch (rotation) {
            case 0:
                localVN = vn;
                localVE = ve;
                localVS = vs;
                localVW = vw;
                break;
            case 3:
                localVN = ve;
                localVE = vs;
                localVS = vw;
                localVW = vn;
                break;
            case 2:
                localVN = vs;
                localVE = vw;
                localVS = vn;
                localVW = ve;
                break;
            case 1:
                localVN = vw;
                localVE = vn;
                localVS = ve;
                localVW = vs;
                break;
        }

        Set<Orientation> relative = EnumSet.noneOf(Orientation.class);
        if (localVN)
            relative.add(Orientation.NORTH);
        if (localVE)
            relative.add(Orientation.EAST);
        if (localVS)
            relative.add(Orientation.SOUTH);
        if (localVW)
            relative.add(Orientation.WEST);
        return relative;
    }
}
