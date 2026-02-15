package com.logica.components.gate;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.utils.NetCompHelper;
import com.logica.utils.PowerUtil;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;
import java.util.ArrayList;
import java.util.List;

import static com.logica.utils.NetCompHelper.updateBlockState;

/**
 * Class for components that perform logic operations.
 */
public abstract class Gate extends NetComp {
    protected LogicStrategy strategy;

    public Gate(Vector3i position, LogicaConstants.BlockId blockId) {
        super(position, blockId);
    }

    // public List<Vector3i> getConnections(World world) {
    // List<Vector3i> connections = new ArrayList<>();
    // LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
    // for (Vector3i neighborPos : NeighborScanner.sixWay(getPosition())) {
    // ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
    // if (neighbor != null)
    // connections.add(neighborPos);
    // }
    // return connections;
    // }

    protected void setStrategy(LogicStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        List<Boolean> inputs = calculateLogicInputs(world);
        boolean shouldBeActive = evaluateLogic(inputs);

        boolean previous = state.isOn();
        // Efficient state update using existing object
        this.state.setOn(shouldBeActive);
        if (previous != shouldBeActive) {
            Vector3i outputDir = getOutputDirection();
            Vector3i outputPos = new Vector3i(getPosition().x + outputDir.x,
                    getPosition().y + outputDir.y,
                    getPosition().z + outputDir.z);
            LogicaLogger.debug("[GateForge][Gate] %s state=%s outputDir=%s outputPos=%s",
                    getClass().getSimpleName(), shouldBeActive, outputDir, outputPos);
            notifyNeighbors(world);
        }
    }

    @Override
    public void render(World world) {
        updateBlockState(world, state);
    }

    @Override
    public void notifyNeighbors(World world) {
        super.notifyNeighbors(world);
        notifyOutputFaceNeighbors(world);
    }

    private void notifyOutputFaceNeighbors(World world) {
        try {
            Vector3i dir = getOutputDirection();
            Vector3i outputBlock = new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y,
                    getPosition().z + dir.z);
            BlockType bt = world.getBlockType(outputBlock);

            if (!NetCompHelper.isBlockSolid(bt) || LogicaConstants.isLogicaBlock(bt.getId()))
                return;

            LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
            for (Vector3i target : NeighborScanner.sixWay(outputBlock)) {
                if (target.equals(getPosition()))
                    continue;

                BlockType t = world.getBlockType(target);
                if (!LogicaConstants.isLogicaComponent(t))
                    continue;

                ILogicaComponent neighbor = nm.getComponentAt(target);
                if (neighbor == null)
                    neighbor = nm.createComponentForId(target, t.getId(), world);

                if (neighbor != null)
                    nm.enqueueUpdate(neighbor);
            }
        } catch (Exception e) {
            LogicaLogger.warn("[GateForge][Gate] notifyNeighbors output-face propagation failed: %s", e);
        }
    }

    /**
     * Pull-Model Implementation:
     * This method is now less critical for power transfer between components,
     * as the receiver checks `getOutputs`. However, it's still good practice
     * for components to report where they *intend* to send power.
     */
    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        if (!isActive())
            return false;
        Vector3i outputDir = getOutputDirection();
        Vector3i outputPos = new Vector3i(getPosition().x + outputDir.x,
                getPosition().y + outputDir.y,
                getPosition().z + outputDir.z);
        LogicaLogger.debug(
                "[GateForge][GateDebug] %s isProvidingPowerTo pos=%s neighbor=%s rot=%d outputDir=%s outputPos=%s",
                getClass().getSimpleName(), getPosition(), neighborPos, state.rotation(),
                outputDir, outputPos);
        return NetCompHelper.samePos(neighborPos, outputPos);
    }

    @Override
    public boolean canAcceptInputFrom(Vector3i neighborPos, Orientation relativeDir) {
        if (relativeDir == null)
            return false;
        return isInputDirection(relativeDir);
    }

    @Override
    public boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        if (relativeDir == null)
            return false;
        Vector3i outputDir = getOutputDirection();
        return NetCompHelper.samePos(outputDir, relativeDir.getDirection());
    }

    @Override
    public List<Vector3i> getOutputs(World world) {
        Vector3i outputDir = getOutputDirection();
        Vector3i outputPos = new Vector3i(getPosition().x + outputDir.x,
                getPosition().y + outputDir.y,
                getPosition().z + outputDir.z);
        LogicaLogger.debug(
                "[GateForge][GateDebug] %s getOutputs pos=%s rot=%d outputDir=%s outputPos=%s",
                getClass().getSimpleName(), getPosition(), state.rotation(),
                outputDir, outputPos);
        return List.of(outputPos);
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i blockPos) {
        if (!isActive() || blockPos == null)
            return false;

        Vector3i outputDir = getOutputDirection();
        Vector3i outputBlock = new Vector3i(getPosition().x + outputDir.x,
                getPosition().y + outputDir.y,
                getPosition().z + outputDir.z);
        // Gates only strong-power through the block on their output face.
        return NetCompHelper.samePos(outputBlock, blockPos);
    }

    @Override
    public void onRecover(World world) {
        try {
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            if (accessor != null) {
                int rot = accessor.getRotationIndex(getPosition());
                this.state = state.withRotation(rot);
                Orientation worldFacing = Orientation.fromRotationIndex(rot);
                LogicaLogger.info("[GateForge][Gate] %s rotation=%d facing=%s pos=%s",
                        getClass().getSimpleName(), rot, worldFacing, getPosition());
            }
        } catch (Exception ignored) {
        }
    }

    public List<Boolean> calculateLogicInputs(World world) {
        List<Boolean> inputs = new ArrayList<>();
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        for (Vector3i dir : getInputDirections()) {
            Vector3i neighborPos = offsetByWorldDir(dir);
            ILogicaComponent neighbor = nm.getComponentAt(neighborPos);

            if (neighbor == null) {
                BlockType bt = world.getBlockType(neighborPos);
                if (LogicaConstants.isLogicaComponent(bt))
                    neighbor = nm.createComponentForId(neighborPos, bt.getId(), world);
            }

            boolean powered = false;

            if (neighbor != null && neighbor.isActive()) {
                // Pull Model: Check if neighbor is outputting to THIS position
                List<Vector3i> neighborOutputs = neighbor.getOutputs(world);
                if (neighborOutputs != null) {
                    for (Vector3i output : neighborOutputs) {
                        if (NetCompHelper.samePos(output, this.getPosition())) {
                            powered = true;
                            break;
                        }
                    }
                }
            } else {
                if (PowerUtil.isSolidBlockReceivingStrongPower(world, neighborPos, getPosition(), true)) {
                    powered = true;
                }
            }
            inputs.add(powered);
        }
        return inputs;
    }

    /**
     * NetComp implementation: returns the positions of input blocks.
     */
    @Override
    public List<Vector3i> getInputs(World world) {
        // TODO: implement strong-power-aware input positions when block-through rules
        // are finalized.
        return new ArrayList<>();
    }

    private boolean isInputDirection(Orientation relativeDir) {
        Vector3i rel = relativeDir.getDirection();
        for (Vector3i dir : getInputDirections()) {
            Vector3i worldDir = rotateLocalDir(dir);
            if (worldDir.equals(rel))
                return true;
        }
        return false;
    }

    protected Vector3i getOutputDirection() {
        Orientation out = Orientation.toWorld(Orientation.NORTH, state.rotation());
        return out.getDirection();
    }

    protected abstract List<Vector3i> getInputDirections();

    private Vector3i offsetByWorldDir(Vector3i localDir) {
        Vector3i worldDir = rotateLocalDir(localDir);
        return new Vector3i(getPosition().x + worldDir.x, getPosition().y + worldDir.y, getPosition().z + worldDir.z);
    }

    private Vector3i rotateLocalDir(Vector3i localDir) {
        Orientation local = Orientation.fromDirection(localDir);
        Orientation world = Orientation.toWorld(local, state.rotation());
        return world != null ? world.getDirection() : localDir;
    }

    public boolean evaluateLogic(List<Boolean> inputs) {
        if (strategy != null) {
            return strategy.evaluate(inputs);
        }
        return false;
    }
}
