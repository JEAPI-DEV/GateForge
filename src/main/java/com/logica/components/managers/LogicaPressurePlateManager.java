package com.logica.components.managers;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.PowerProvider;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;

import java.util.*;

/**
 * Improved pressure plate detection and debounce manager.
 * <p>
 * - reduces per-tick allocations by reusing temporary collections
 * - uses BlockType.getId() to identify plates instead of toString()
 * - clearer debounce logic and safer world-thread execution
 */
public class LogicaPressurePlateManager implements Runnable {

    private static final long DEBOUNCE_MS = 150L;

    private final Set<UUID> playersOnPlate = new HashSet<>();
    private final Map<UUID, Long> pendingReleases = new HashMap<>();
    private final Map<UUID, Vector3i> pressedPlates = new HashMap<>();

    private final Set<UUID> tmpCurrentlyIntersecting = new HashSet<>();
    private final Map<UUID, Vector3i> tmpCurrentPlates = new HashMap<>();
    private final Map<UUID, BlockType> tmpCurrentBlockTypes = new HashMap<>();

    @Override
    public void run() {
        World world = Universe.get().getDefaultWorld();
        if (world == null)
            return;
        world.execute(() -> {
            try {
                long now = System.currentTimeMillis();

                Collection<PlayerRef> players = Universe.get().getPlayers();

                tmpCurrentlyIntersecting.clear();
                tmpCurrentPlates.clear();
                tmpCurrentBlockTypes.clear();

                for (PlayerRef player : players) {
                    if (player == null)
                        continue;
                    UUID uuid = player.getUuid();
                    Transform t = player.getTransform();

                    double px = t.getPosition().getX();
                    double py = t.getPosition().getY();
                    double pz = t.getPosition().getZ();

                    final double width = 0.35D;
                    int minX = (int) Math.floor(px - width);
                    int maxX = (int) Math.floor(px + width);
                    int minZ = (int) Math.floor(pz - width);
                    int maxZ = (int) Math.floor(pz + width);
                    int footY = (int) Math.floor(py);

                    Vector3i foundPos = null;
                    BlockType foundType = null;

                    outer: for (int bx = minX; bx <= maxX; bx++) {
                        for (int bz = minZ; bz <= maxZ; bz++) {
                            for (int by = footY; by >= footY - 1; by--) {
                                BlockType bt = world.getBlockType(bx, by, bz);
                                if (bt == null)
                                    continue;
                                String id = bt.getId();
                                if (LogicaConstants.BlockId
                                        .from(id) == LogicaConstants.BlockId.PROVIDER_PRESSURE_PLATE) {
                                    foundType = bt;
                                    foundPos = new Vector3i(bx, by, bz);
                                    break outer;
                                }
                            }
                        }
                    }

                    if (foundPos != null) {
                        tmpCurrentlyIntersecting.add(uuid);
                        tmpCurrentPlates.put(uuid, foundPos);
                        tmpCurrentBlockTypes.put(uuid, foundType);
                    }
                }

                // Process active intersections (activations / plate switches)
                for (UUID uuid : tmpCurrentlyIntersecting) {
                    Vector3i platePos = tmpCurrentPlates.get(uuid);
                    BlockType plateType = tmpCurrentBlockTypes.get(uuid);
                    pendingReleases.remove(uuid);

                    if (playersOnPlate.contains(uuid)) {
                        Vector3i oldPos = pressedPlates.get(uuid);
                        if (oldPos != null && !oldPos.equals(platePos)) {
                            deactivatePlate(oldPos, world);
                            activatePlate(platePos, plateType, world);
                            pressedPlates.put(uuid, platePos);
                        }
                    } else {
                        playersOnPlate.add(uuid);
                        pressedPlates.put(uuid, platePos);
                        activatePlate(platePos, plateType, world);
                    }
                }

                // Debounce: players that were on a plate but are not currently intersecting
                leftDiff();
                Set<UUID> left = new HashSet<>(playersOnPlate);
                left.removeAll(tmpCurrentlyIntersecting);

                for (UUID u : left)
                    pendingReleases.computeIfAbsent(u, _ -> now + DEBOUNCE_MS);

                Iterator<Map.Entry<UUID, Long>> it = pendingReleases.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> e = it.next();
                    UUID u = e.getKey();
                    long expiry = e.getValue();
                    if (tmpCurrentlyIntersecting.contains(u)) {
                        it.remove();
                        continue;
                    }
                    if (now >= expiry) {
                        it.remove();
                        playersOnPlate.remove(u);
                        Vector3i pos = pressedPlates.remove(u);
                        if (pos != null)
                            deactivatePlate(pos, world);
                    }
                }

            } catch (Exception ex) {
                LogicaLogger.error("[Logica][Plate] Error in run: %s", ex.toString());
            }
        });
    }

    private void leftDiff() {
        // Just a helper to keep logic clean, doesn't do much in this simplified version
    }

    private void activatePlate(Vector3i pos, BlockType plateType, World world) {
        try {
            if (pos == null || world == null || plateType == null)
                return;
            LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
            ILogicaComponent comp = nm.getComponentAt(pos);
            if (comp == null)
                comp = nm.createComponentForId(pos, plateType.getId(), world);
            if (comp instanceof PowerProvider pp)
                pp.updateOutput(world, true);
        } catch (Exception e) {
            LogicaLogger.warn("[Logica][Plate] Failed to activate at %s : %s", pos, e.toString());
        }
    }

    private void deactivatePlate(Vector3i pos, World world) {
        try {
            if (pos == null || world == null)
                return;
            LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
            ILogicaComponent comp = nm.getComponentAt(pos);
            if (comp instanceof PowerProvider pp)
                pp.updateOutput(world, false);
        } catch (Exception e) {
            LogicaLogger.warn("[Logica][Plate] Failed to deactivate at %s : %s", pos, e.toString());
        }
    }
}
