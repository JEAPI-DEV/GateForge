package com.logica.eventhandlers;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;

public class LogicaEventListener {
    private static final HytaleLogger LOG = HytaleLogger.getLogger();
    private final LogicaNetworkManager networkManager = LogicaNetworkManager.getInstance();

    public void onBlockPlace(World world, Vector3i pos, String blockId) {
        try {
            ILogicaComponent comp = networkManager.createComponentForId(pos, blockId, world);
            if (comp == null) return;
        } catch (Exception e) {
            LOG.atWarning().log("[Logica] Problem with the Eventlistener: " + e);
        }
    }

    public void onBlockBreak(World world, Vector3i pos) {
        ILogicaComponent comp = networkManager.getComponentAt(pos);
        if (comp != null) comp.onBreak(world);
        networkManager.removeComponent(comp);

        for (Vector3i neighborPos : NeighborScanner.sixWay(pos)) {
            ILogicaComponent neighbor = networkManager.getComponentAt(neighborPos);
            if (neighbor != null) {
                networkManager.enqueueUpdate(neighbor);
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
            LOG.atInfo().log("[Logica] Interacting with component at " + pos + " (type: "
                    + comp.getClass().getSimpleName() + ")");
            comp.onInteract(world);
        }
    }
}
