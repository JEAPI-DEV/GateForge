package com.logica.components.pipes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.logica.components.core.NeighborInfo;
import com.logica.components.core.PipeConnectionContext;
import com.logica.components.pipes.strategies.CornerStrategy;
import com.logica.components.pipes.strategies.FourWayStrategy;
import com.logica.components.pipes.strategies.StraightStrategy;
import com.logica.components.pipes.strategies.ThreeWayStrategy;
import com.logica.vars.Orientation;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PipeShapeLogic {
    private static final HytaleLogger LOG = HytaleLogger.getLogger();

    private static final List<PipeShapeStrategy> STRATEGIES = List.of(
            new FourWayStrategy(),
            new ThreeWayStrategy(),
            new StraightStrategy(),
            new CornerStrategy());

    /**
     * Calculates the shape of a pipe based on its neighbors.
     * 
     * @param position            The world position of the pipe.
     * @param neighborContext     Context of the surrounding 6 blocks.
     * @param activeConnections   The directions considered as valid 'connections'
     *                            (pipes usually).
     * @param climbingConnections The directions considered as vertical 'climbing'
     *                            connections.
     * @return Calculated ShapeResult.
     */
    public static ShapeResult calculateShape(Vector3i position,
            Map<Orientation, NeighborInfo> neighborContext,
            Set<Orientation> activeConnections,
            Set<Orientation> climbingConnections) {

        LOG.atInfo().log("[Logica][Pipe] Calculating shape for %s", position);

        // 1. Build Context
        PipeConnectionContext context = new PipeConnectionContext(neighborContext, activeConnections,
                climbingConnections);

        // 2. Select Strategy
        PipeShapeStrategy selectedStrategy = new StraightStrategy();
        for (PipeShapeStrategy strategy : STRATEGIES) {
            if (strategy.matches(context)) {
                selectedStrategy = strategy;
                break;
            }
        }

        // 3. Calculate Base Shape & State (with internal post-processing)
        LOG.atInfo().log("[Logica][Pipe] Context Flags for %s: H(N:%b S:%b E:%b W:%b) V(N:%b S:%b E:%b W:%b)",
                position,
                context.n, context.s, context.e, context.w,
                context.vn, context.vs, context.ve, context.vw);

        ShapeResult result = selectedStrategy.calculate(context);

        // Copy flags to result (compatibility)
        result.n = context.n;
        result.s = context.s;
        result.e = context.e;
        result.w = context.w;
        result.u = context.up;
        result.d = context.down;

        LOG.atInfo().log("[Logica][Pipe] Final: %s (rot:%d)", result.getState(), result.getRotation());
        return result;
    }

    // Legacy support wrapper
    public static ShapeResult calculateShape(Vector3i position, List<Vector3i> connections) {
        Set<Orientation> active = EnumSet.noneOf(Orientation.class);
        Set<Orientation> climbing = EnumSet.noneOf(Orientation.class);
        Map<Orientation, NeighborInfo> neighbors = java.util.Collections.emptyMap();

        if (connections != null) {
            for (Vector3i c : connections) {
                int dx = c.getX() - position.getX();
                int dy = c.getY() - position.getY();
                int dz = c.getZ() - position.getZ();

                // Use Orientation helper to determine direction
                if (dy == 0) {
                    Orientation o = Orientation.fromDelta(dx, dy, dz);
                    if (o != null)
                        active.add(o);
                } else if (dx == 0 && dz == 0) {
                    // Vertical direct (UP/DOWN)
                    Orientation o = Orientation.fromDelta(dx, dy, dz);
                    if (o != null)
                        active.add(o);
                } else {
                    Orientation hDir = Orientation.fromDelta(dx, 0, dz);
                    if (hDir != null) {
                        active.add(hDir);
                        if (dy == 1) { // Only mark as 'climbing' if going UP?
                            climbing.add(hDir);
                        }
                    }
                }
            }
        }

        return calculateShape(position, neighbors, active, climbing);
    }

    // The applyPostProcessing method is removed as per instructions.
}
