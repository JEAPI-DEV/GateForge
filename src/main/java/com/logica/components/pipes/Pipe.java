package com.logica.components.pipes;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.Connector;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pull-model pipe implementation with multi-source tracking.
 */
public class Pipe extends Connector {
    private String lastState = null;
    private int lastRotation = -1;

    public Pipe(Vector3i position) {
        super(position);
    }

    @Override
    public void render(World world) {
        // Pipes use shape/rotation only; no on/off visual state.
    }

    @Override
    public void updateShape(World world) {
        List<Vector3i> connections = getConnections(world);
        ShapeResult result = PipeShapeLogic.calculateShape(getPosition(), connections);

        if (!result.getState().equals(lastState) || result.getRotation() != lastRotation) {
            if (PipeShapeApplier.apply(world, getPosition(), result)) {
                lastState = result.getState();
                lastRotation = result.getRotation();
                this.state = state.withRotation(result.getRotation());
            }
        }
    }

    @Override
    public void updateOutput(World world, NetComp caller) {
        if (caller == null) {
            // Full refresh: must check BOTH Cardinals (super logic) and Diagonals (Pipe
            // logic)
            // atomically to prevent state flickering (OFF -> ON).
            refreshAllSources(world);
        } else {
            // Incremental update: NetComp handles Cardinals, we handle Diagonals
            super.updateOutput(world, caller);
            handleDiagonalUpdate(world, caller);
        }
    }

    private void refreshAllSources(World world) {
        Map<NetComp, Orientation> oldSources = new HashMap<>(state.activeSources());
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        // 1. Check Cardinal Neighbors (Logic from NetComp)
        Map<NetComp, Orientation> newSources = getCardinalSources(world);

        // 2. Check Diagonal Neighbors (Pipe Step-up Logic)
        for (Vector3i neighborPos : NeighborScanner.pipeVerticalDiagonals(getPosition())) {
            ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
            // STRICT RULE: Only accept power from other PIPES via diagonal (step-up)
            // connections.
            if (neighbor instanceof Pipe pipe && pipe.isActive()) {
                Vector3i delta = new Vector3i(
                        neighborPos.x - getPosition().x,
                        neighborPos.y - getPosition().y,
                        neighborPos.z - getPosition().z);

                // Map diagonal to horizontal orientation
                if (delta.y != 0) {
                    Orientation horizontal = Orientation.fromDelta(delta.x, 0, delta.z);
                    if (horizontal != null && pipe.isProvidingPowerTo(getPosition())) {
                        newSources.put(pipe, horizontal);
                    }
                }
            }
        }

        boolean oldOn = state.isOn();
        state.setActiveSources(newSources);
        calculateNewState(world, null);
        boolean sourcesChanged = !oldSources.equals(newSources);
        boolean stateChanged = oldOn != state.isOn();

        if (sourcesChanged || stateChanged) {
            notifyNeighbors(world);
        }
    }

    private void handleDiagonalUpdate(World world, NetComp caller) {
        Vector3i delta = new Vector3i(
                caller.getPosition().x - getPosition().x,
                caller.getPosition().y - getPosition().y,
                caller.getPosition().z - getPosition().z);

        // Only handle if it's a diagonal (NetComp handles cardinals)
        if (delta.y != 0 && (delta.x != 0 || delta.z != 0)) {
            // STRICT RULE: Ignore diagonal updates from non-Pipes (e.g. Gates)
            if (!(caller instanceof Pipe)) {
                return;
            }

            Orientation horizontal = Orientation.fromDelta(delta.x, 0, delta.z);
            if (horizontal != null) {
                boolean shouldBeSource = caller.isActive() && caller.isProvidingPowerTo(getPosition());

                boolean changed = false;
                if (shouldBeSource) {
                    if (!state.activeSources().containsKey(caller)) {
                        state.addSource(caller, horizontal);
                        changed = true;
                    }
                } else {
                    if (state.activeSources().containsKey(caller)) {
                        state.removeSource(caller);
                        changed = true;
                    }
                }

                if (changed) {
                    calculateNewState(world, caller);
                    notifyNeighbors(world);
                }
            }
        }
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        boolean powered = !state.activeSources().isEmpty();
        if (!powered && world != null) {
            for (Orientation orientation : Orientation.ALL) {
                Vector3i offset = orientation.getDirection();
                Vector3i neighborPos = new Vector3i(getPosition().x + offset.x,
                        getPosition().y + offset.y,
                        getPosition().z + offset.z);
                if (com.logica.utils.PowerUtil.isSolidBlockReceivingStrongPower(world, neighborPos, getPosition(),
                        true)) {
                    powered = true;
                    break;
                }
            }
        }
        state.setOn(powered);
    }

    @Override
    public List<Vector3i> getInputs(World world) {
        return getConnections(world);
    }

    @Override
    public List<Vector3i> getOutputs(World world) {
        if (!isActive())
            return List.of();

        List<Vector3i> outputs = new ArrayList<>();
        for (Orientation orientation : Orientation.ALL) {
            if (state.activeSources().containsValue(orientation))
                continue;
            Vector3i dir = orientation.getDirection();
            outputs.add(new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y, getPosition().z + dir.z));
        }
        return outputs;
    }

    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        if (!isActive() || neighborPos == null)
            return false;

        int dx = neighborPos.x - getPosition().x;
        int dy = neighborPos.y - getPosition().y;
        int dz = neighborPos.z - getPosition().z;

        Orientation rel = Orientation.fromDelta(dx, dy, dz);

        // If direct cardinal failed, try horizontal component for diagonals
        if (rel == null && dy != 0) {
            rel = Orientation.fromDelta(dx, 0, dz);
        }

        if (rel == null)
            return false;

        return !isBlockedBySource(neighborPos, rel);
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    @Override
    public boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        if (relativeDir == null)
            return false;
        return !isBlockedBySource(neighborPos, relativeDir);
    }

    /**
     * Checks if the target output direction is allowed based on active sources.
     * Normally, we block outputting to the same direction as an input (backflow
     * prevention).
     * However, we allow "ZigZag" (Vertical Hairpin) connections where one is Up and
     * one is Down.
     */
    private boolean isBlockedBySource(Vector3i targetPos, Orientation targetDir) {
        boolean targetIsDiagonal = (targetPos.y != getPosition().y);

        for (java.util.Map.Entry<NetComp, Orientation> entry : state.activeSources().entrySet()) {
            NetComp sourceComp = entry.getKey();
            Orientation sourceDir = entry.getValue();

            if (sourceComp == null || sourceDir == null)
                continue;

            // 1. Direct Backflow: Always block outputting directly back to the source
            // component
            if (sourceComp.getPosition().equals(targetPos))
                return true;

            // 2. Directional Collision: If source and target share direction, check exact
            // geometry
            if (sourceDir == targetDir) {
                boolean sourceIsDiagonal = (sourceComp.getPosition().y != getPosition().y);

                if (targetIsDiagonal && sourceIsDiagonal) {
                    // ZigZag Exception: If they are on opposite vertical sides (Up vs Down), ALLOW
                    // it.
                    if (targetPos.y != sourceComp.getPosition().y) {
                        continue; // Not blocked by this source
                    }
                }

                // If not a ZigZag exception, it's a collision (Blocked)
                return true;
            }
        }

        return false;
    }

    @Override
    public void onRecover(World world) {
        com.logica.utils.LogicaLogger.info(
                "[GateForge][Pipe] rotation=%d facing=%s pos=%s",
                state.rotation(), com.logica.vars.Orientation.fromRotationIndex(state.rotation()), getPosition());
        updateShape(world);
    }

    public List<Vector3i> getConnections(World world) {
        return PipeConnectionFinder.findConnections(world, getPosition(), this);
    }

    @Override
    public boolean canConnectTo(ILogicaComponent other) {
        return other != null;
    }

    @Override
    public void notifyNeighbors(World world) {
        // Do NOT call super.notifyNeighbors(world) to avoid queuing pipes.
        // We handle propagation manually here for instant "wire" behavior.

        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        // 1. Cardinal Neighbors (Cardinals)
        for (Orientation orientation : Orientation.ALL) {
            Vector3i offset = orientation.getDirection();
            Vector3i neighborPos = new Vector3i(getPosition().x + offset.x,
                    getPosition().y + offset.y,
                    getPosition().z + offset.z);

            BlockType bt = world.getBlockType(neighborPos);
            // Must check logic component type before creating
            if (LogicaConstants.isLogicaComponent(bt)) {
                ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
                if (neighbor == null) {
                    neighbor = nm.createComponentForId(neighborPos, bt.getId(), world);
                }

                if (neighbor != null) {
                    if (neighbor instanceof Pipe pipe) {
                        // Instant propagation: Recurse immediately
                        // Update logical state
                        pipe.updateOutput(world, this);
                        // Update visual shape
                        pipe.updateShape(world);
                    } else {
                        // Gates/Lamps: Queue for next tick (keep delay)
                        nm.enqueueUpdate(neighbor);
                    }
                }
            }
        }

        // 2. Diagonal Neighbors (Step-up/down)
        for (Vector3i neighborPos : NeighborScanner.pipeVerticalDiagonals(getPosition())) {
            BlockType bt = world.getBlockType(neighborPos);
            if (!LogicaConstants.isLogicaComponent(bt))
                continue;

            ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
            if (neighbor == null) {
                neighbor = nm.createComponentForId(neighborPos, bt.getId(), world);
            }

            if (neighbor != null) {
                if (neighbor instanceof Pipe pipe) {
                    // Instant propagation for step-ups too
                    pipe.updateOutput(world, this);
                    pipe.updateShape(world);
                } else {
                    // Should not happen due to connection rules, but safe fallback
                    nm.enqueueUpdate(neighbor);
                }
            }
        }
    }
}
