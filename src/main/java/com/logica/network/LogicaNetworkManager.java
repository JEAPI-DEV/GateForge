package com.logica.network;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.ILogicaComponent;
import com.logica.system.LogicTicker;
import com.logica.vars.LogicaConstants;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogicaNetworkManager implements LogicTicker {

    private static LogicaNetworkManager instance;
    private static ComponentRegistry registry;
    private static Map<Vector3i, ILogicaComponent> storage = new ConcurrentHashMap<>();
    private static Deque<ILogicaComponent> updateDeque = new ArrayDeque<>();
    private boolean recovered = false;

    public void setAllComponents(Map<Vector3i, ILogicaComponent> newComponents) {
        storage.clear();
        updateDeque.clear();
        if (newComponents != null) {
            storage.putAll(newComponents);
            // Defer onRecover to first tick when world is available
            recovered = false;
        }
    }

    public Map<Vector3i, ILogicaComponent> getAllComponents() {
        return new ConcurrentHashMap<>(storage);
    }

    public static LogicaNetworkManager getInstance() {
        if (instance == null) {
            instance = new LogicaNetworkManager();
            registry = new DefaultComponentRegistry();
        }
        return instance;
    }

    public void tick(World world) {
        if (world == null)
            return;

        if (!recovered) {
            LogicaLogger.info("[GateForge][NM] Recovering " + storage.size() + " components...");
            for (ILogicaComponent comp : storage.values()) {
                comp.onRecover(world);
                // Force refresh of activeSources to ensure graph connectivity is restored
                comp.updateOutput(world, null);
            }
            recovered = true;
        }

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
            if (!storage.containsValue(comp))
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
        storage.put(pos, comp);
        LogicaLogger.info("[GateForge][NM] Created/Recovered " + comp.getClass().getSimpleName() + " at " + pos);
        comp.onRecover(world);
        comp.onPlace(world);
        enqueueUpdate(comp);
        return comp;
    }

    public void removeComponent(ILogicaComponent comp) {
        if (comp == null)
            return;
        storage.remove(comp.getPosition());
        unqueue(comp);
    }

    public void moveComponent(Vector3i oldPos, Vector3i newPos) {
        if (oldPos == null || newPos == null)
            return;

        ILogicaComponent comp = storage.remove(oldPos);
        if (comp != null) {
            if (storage.containsKey(newPos)) {
                LogicaLogger.warn("[GateForge][NM] Force overwriting component at %s due to move from %s", newPos,
                        oldPos);
                storage.remove(newPos);
            }
            storage.put(newPos, comp);
        }
    }

    public void enqueueUpdate(ILogicaComponent comp) {
        if (comp != null && !updateDeque.contains(comp)) {
            if (comp.getClass().getSimpleName().equalsIgnoreCase("Lamp")) {
                LogicaLogger.debug("[GateForge][NM] Enqueued Lamp update at %s", comp.getPosition());
            }
            updateDeque.add(comp);
        }
    }

    public void unqueue(ILogicaComponent delist) {
        updateDeque.stream().filter(comp -> comp == delist).forEach(comp -> updateDeque.remove(comp));
    }

    public ILogicaComponent getComponentAt(Vector3i pos) {
        if (pos == null)
            return null;
        return storage.get(pos);
    }

    public boolean doesComponentExist(ILogicaComponent comp) {
        return storage.containsValue(comp);
    }

}
