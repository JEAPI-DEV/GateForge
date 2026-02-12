package com.logica.network;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.ILogicaComponent;
import com.logica.system.LogicTicker;
import com.logica.vars.LogicaConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class LogicaNetworkManager implements LogicTicker {
    private static final HytaleLogger LOG = HytaleLogger.getLogger();
    private static LogicaNetworkManager instance;
    private static ComponentRegistry registry;
    private static List<ILogicaComponent> storage = new ArrayList<>();
    private static Deque<ILogicaComponent> updateDeque = new ArrayDeque<>();

    public static LogicaNetworkManager getInstance() {
        if (instance == null) {
            instance = new LogicaNetworkManager();
            registry = new DefaultComponentRegistry();
        }
        return instance;
    }

    @Override
    public void tick(World world) {
        if (world == null)
            return;
        if (updateDeque.isEmpty())
            return;

        int processed = 0;
        int maxPerTick = 512;
        int initialSize = updateDeque.size();
        int toProcess = Math.min(initialSize, maxPerTick);

        while (processed < toProcess) {
            ILogicaComponent comp = updateDeque.poll();
            if (comp == null)
                break;
            if (!storage.contains(comp))
                continue;
            comp.updateOutput(world, null);
            processed++;
        }
    }

    public ILogicaComponent createComponentForId(Vector3i pos, String blockId, World world) {
        if (pos == null)
            return null;

        LogicaConstants.BlockId resolvedId = LogicaConstants.BlockId.from(blockId);
        if (resolvedId == null)
            return null;

        // defensive avoidance of creating components that already exist
        ILogicaComponent testIfExists = getComponentAt(pos);
        if (testIfExists != null)
            return testIfExists;

        ILogicaComponent comp = registry.create(resolvedId, pos, world);
        if (comp == null)
            return null;
        storage.add(comp);
        LOG.atInfo().log("[Logica][NM] Created/Recovered " + comp.getClass().getSimpleName() + " at " + pos);
        comp.onRecover(world);
        comp.onPlace(world);
        enqueueUpdate(comp);
        return comp;
    }

    public void removeComponent(ILogicaComponent comp) {
        if (comp == null)
            return;
        storage.remove(comp);
        unqueue(comp);
    }

    public void enqueueUpdate(ILogicaComponent comp) {
        if (comp != null && !updateDeque.contains(comp)) {
            if (comp.getClass().getSimpleName().equalsIgnoreCase("Lamp")) {
                // Log stack trace to find who is enqueuing the Lamp
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    if (ste.getClassName().contains("com.logica")) {
                        sb.append("\n\t at ").append(ste);
                    }
                }
                LOG.atInfo().log("[Logica][NM] Enqueued Lamp update at %s from: %s", comp.getPosition(), sb.toString());
            }
            updateDeque.add(comp);
        }
    }

    public void unqueue(ILogicaComponent delist) {
        updateDeque.stream().filter(comp -> comp == delist).forEach(comp -> updateDeque.remove(comp));
    }

    public ILogicaComponent getComponentAt(Vector3i pos) {
        for (ILogicaComponent comp : storage) {
            Vector3i compPos = comp.getPosition();
            if (compPos != null && pos != null
                    && compPos.x == pos.x
                    && compPos.y == pos.y
                    && compPos.z == pos.z) {
                return comp;
            }
        }
        return null;
    }

    public boolean doesComponentExist(ILogicaComponent comp) {
        return storage.contains(comp);
    }

}
