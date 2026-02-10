package com.logica.components.pipes;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.interfaces.ILogicaComponent;
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
            Orientation relativeDir = Orientation.fromDirection(new Vector3i(
                    origin.x - neighborPos.x,
                    origin.y - neighborPos.y,
                    origin.z - neighborPos.z));
            if (relativeDir != null && neighbor.canAcceptInputFrom(origin, relativeDir) && !connections.contains(neighborPos)) {
                connections.add(neighborPos);
            }
        }
    }

    private static boolean isVerticalAlignment(Vector3i origin, Vector3i neighbor) {
        return origin.x == neighbor.x && origin.z == neighbor.z && origin.y != neighbor.y;
    }

    private static boolean isPipeOrGate(LogicaConstants.BlockId id) {
        if (id == null) return false;
        switch (id) {
            case PIPE:
            case GATE_AND:
            case GATE_OR:
            case GATE_NOT:
            case GATE_NAND:
            case GATE_NOR:
            case GATE_XOR:
            case GATE_BUFFER:
                return true;
            default:
                return false;
        }
    }
}
