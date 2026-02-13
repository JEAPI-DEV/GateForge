package com.logica.eventhandlers;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;

public class LogicaEventListener {

    private final LogicaNetworkManager networkManager = LogicaNetworkManager.getInstance();

    public void onBlockPlace(World world, Vector3i pos, String blockId) {
        try {
            ILogicaComponent comp = networkManager.createComponentForId(pos, blockId, world);
            if (comp == null)
                return;
        } catch (Exception e) {
            LogicaLogger.warn("[GateForge] Problem with the Eventlistener: " + e);
        }
    }

    public void onBlockBreak(World world, Vector3i pos) {
        ILogicaComponent comp = networkManager.getComponentAt(pos);
        if (comp != null)
            comp.onBreak(world);
        networkManager.removeComponent(comp);

        for (Vector3i neighborPos : NeighborScanner.sixWay(pos)) {
            ILogicaComponent neighbor = networkManager.getComponentAt(neighborPos);
            if (neighbor != null) {
                networkManager.enqueueUpdate(neighbor);
            }
        }
        // Also notify diagonal neighbors (for pipes)
        for (Vector3i neighborPos : NeighborScanner.pipeVerticalDiagonals(pos)) {
            ILogicaComponent neighbor = networkManager.getComponentAt(neighborPos);
            if (neighbor != null) {
                networkManager.enqueueUpdate(neighbor);
                if (neighbor instanceof com.logica.components.pipes.Pipe pipe) {
                    pipe.updateShape(world);
                }
            }
        }
    }

    public void onBlockInteract(World world, Vector3i pos) {
        ILogicaComponent comp = networkManager.getComponentAt(pos);

        if (comp == null) {
            BlockType bt = world.getBlockType(pos);
            if (LogicaConstants.isLogicaComponent(bt)) {
                comp = networkManager.createComponentForId(pos, bt.getId(), world);
            }
        }

        if (comp != null) {
            LogicaLogger.debug("[GateForge] Interacting with component at " + pos + " (type: "
                    + comp.getClass().getSimpleName() + ")");
            comp.onInteract(world);
        }
    }
}
