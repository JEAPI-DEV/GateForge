package com.logica.components.pipes;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.Connector;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.interfaces.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.ArrayList;
import java.util.List;

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
    protected void calculateNewState(World world, NetComp caller) {
        state.setOn(!state.activeSources().isEmpty());
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

        Orientation rel = Orientation.fromDirection(new Vector3i(
                neighborPos.x - getPosition().x,
                neighborPos.y - getPosition().y,
                neighborPos.z - getPosition().z));

        if (rel == null)
            return false;

        return !state.activeSources().containsValue(rel);
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    @Override
    public boolean canAcceptInputFrom(Vector3i neighborPos, Orientation relativeDir) {
        return true;
    }

    @Override
    public boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        if (relativeDir == null)
            return false;
        return !state.activeSources().containsValue(relativeDir);
    }

    @Override
    public void onRecover(World world) {
        com.hypixel.hytale.logger.HytaleLogger.getLogger().atInfo().log(
                "[Logica][Pipe] rotation=%d facing=%s pos=%s",
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
        super.notifyNeighbors(world);

        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
        for (Vector3i pos : NeighborScanner.pipeVerticalDiagonals(getPosition())) {
            BlockType bt = world.getBlockType(pos);
            if (!LogicaConstants.isLogicaComponent(bt))
                continue;

            ILogicaComponent neighbor = nm.getComponentAt(pos);
            if (neighbor == null) {
                neighbor = nm.createComponentForId(pos, bt.getId(), world);
            }
            if (neighbor != null) {
                nm.enqueueUpdate(neighbor);
            }
        }
    }
}
