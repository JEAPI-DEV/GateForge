package com.logica.components.pipes;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines valid pipe connections using shared neighbor rules.
 */
public final class PipeConnectionFinder {

    private PipeConnectionFinder() {
    }

    public static List<Vector3i> findConnections(World world, Vector3i position, Pipe pipe) {
        if (world == null || position == null || pipe == null) {
            return List.of();
        }

        List<Vector3i> all = new ArrayList<>();
        for (Vector3i pos : NeighborScanner.sixWay(position)) {
            addIfConnectable(world, pos, pipe, all, position);
        }
        for (Vector3i pos : NeighborScanner.pipeVerticalDiagonals(position)) {
            addIfConnectable(world, pos, pipe, all, position);
        }

        // Preserve legacy behavior: if both straight vertical neighbors exist, return only them
        Vector3i upPos = null;
        Vector3i downPos = null;
        for (Vector3i p : all) {
            if (p.x == position.x && p.z == position.z) {
                if (p.y > position.y)
                    upPos = p;
                if (p.y < position.y)
                    downPos = p;
            }
        }
        if (upPos != null && downPos != null) {
            return List.of(upPos, downPos);
        }

        return all;
    }

    private static void addIfConnectable(World world, Vector3i neighborPos, Pipe pipe, List<Vector3i> connections, Vector3i origin) {
        int dx = neighborPos.x - origin.x;
        int dy = neighborPos.y - origin.y;
        int dz = neighborPos.z - origin.z;
        if (dy != 0 && dx != 0 && dz != 0) {
            return;
        }
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
        BlockType bt = world.getBlockType(neighborPos);
        if (bt == null)
            return;

        String id = bt.getId();
        LogicaConstants.BlockId blockId = LogicaConstants.BlockId.from(id);

        if (isVerticalAlignment(origin, neighborPos) && isPipeOrGate(blockId)) {
            return;
        }

        if (blockId == null)
            return;

        ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
        if (neighbor == null)
            neighbor = nm.createComponentForId(neighborPos, id, world);

        if (neighbor != null && pipe.canConnectTo(neighbor)) {
            Orientation relativeDir;
            if (dy != 0) {
                relativeDir = Orientation.fromDelta(-dx, 0, -dz);
            } else {
                relativeDir = Orientation.fromDelta(-dx, -dy, -dz);
            }
            if (relativeDir != null) {
                boolean accepts = neighbor.canAcceptInputFrom(origin, relativeDir);
                boolean provides = neighbor.canProvideOutputTo(origin, relativeDir);
                if ((accepts || provides) && !connections.contains(neighborPos)) {
                    connections.add(neighborPos);
                }
            }
        }
    }

    private static boolean isVerticalAlignment(Vector3i origin, Vector3i neighbor) {
        return origin.x == neighbor.x && origin.z == neighbor.z && origin.y != neighbor.y;
    }

    private static boolean isPipeOrGate(LogicaConstants.BlockId id) {
        if (id == null) return false;
        return switch (id) {
            case PIPE, GATE_AND, GATE_OR, GATE_NOT, GATE_NAND, GATE_NOR, GATE_XOR, GATE_BUFFER -> true;
            default -> false;
        };
    }
}
