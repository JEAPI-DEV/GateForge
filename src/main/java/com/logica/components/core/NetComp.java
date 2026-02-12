package com.logica.components.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.misc.ComponentState;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all logical components in the Logica mod.
 */
public abstract class NetComp implements ILogicaComponent {
    protected ComponentState state;

    protected NetComp(Vector3i position) {
        this.state = new ComponentState(false, 0, position);
    }

    @Override
    public Vector3i getPosition() {
        return state.pos();
    }

    @Override
    public boolean isActive() {
        return state.isOn();
    }

    @Override
    public void updateOutput(World world, NetComp caller) {
        if (world == null)
            return;
        if (caller == null) {
            refreshSources(world);
            return;
        }
        Map<NetComp, Orientation> oldSources = new HashMap<>(state.activeSources());

        Vector3i delta = new Vector3i(
                caller.getPosition().x - getPosition().x,
                caller.getPosition().y - getPosition().y,
                caller.getPosition().z - getPosition().z
        );
        Orientation callersDirection = Orientation.fromDirection(delta);
        if (callersDirection == null)
            return;

        boolean callerProviding = caller.isActive() && caller.isProvidingPowerTo(getPosition());
        boolean acceptsInput = canAcceptInputFrom(caller.getPosition(), callersDirection);

        if (callerProviding && acceptsInput) {
            state.addSource(caller, callersDirection);
        } else {
            state.removeSource(caller);
        }

        boolean sourcesChanged = !Objects.equals(oldSources, state.activeSources());
        if (sourcesChanged) {
            calculateNewState(world, caller);
            notifyNeighbors(world);
        }
    }

    private void refreshSources(World world) {
        Map<NetComp, Orientation> oldSources = new HashMap<>(state.activeSources());
        Map<NetComp, Orientation> newSources = new HashMap<>();
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        boolean logLamp = getClass().getSimpleName().equalsIgnoreCase("Lamp");
        if (logLamp) {
            HytaleLogger.getLogger().atInfo().log("[Logica][LampDebug] Refresh sources for %s", getPosition());
        }

        for (Orientation orientation : Orientation.ALL) {
            Vector3i offset = orientation.getDirection();
            Vector3i neighborPos = new Vector3i(getPosition().x + offset.x,
                    getPosition().y + offset.y,
                    getPosition().z + offset.z);

            ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
            if (neighbor == null) {
                BlockType bt = world.getBlockType(neighborPos);
                if (LogicaConstants.isLogicaComponent(bt)) {
                    neighbor = nm.createComponentForId(neighborPos, bt.getId(), world);
                }
            }

            if (neighbor instanceof NetComp netComp) {
                boolean providing = netComp.isActive() && netComp.isProvidingPowerTo(getPosition());
                boolean accepts = canAcceptInputFrom(neighborPos, orientation);
                if (logLamp && providing) {
                    HytaleLogger.getLogger().atInfo().log(
                            "[Logica][LampDebug] source=%s pos=%s dir=%s active=%s provides=%s accepts=%s",
                            netComp.getClass().getSimpleName(), neighborPos, orientation, netComp.isActive(),
                            providing, accepts);
                }
                if (providing && accepts) {
                    newSources.put(netComp, orientation);
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
        HytaleLogger.getLogger().atInfo().log("[Logica][NetComp] %s sources=%d pos=%s",
            getClass().getSimpleName(), newSources.size(), getPosition());
    }
    }

    protected abstract void calculateNewState(World world, NetComp caller);

    @Override
    public abstract List<Vector3i> getInputs(World world);

    @Override
    public abstract List<Vector3i> getOutputs(World world);

    @Override
    public abstract boolean isProvidingPowerTo(Vector3i neighborPos);

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return isProvidingPowerTo(neighborPos);
    }

    @Override
    public abstract void onRecover(World world);

    @Override
    public void notifyNeighbors(World world) {
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();
        for (Orientation orientation : Orientation.ALL) {
            Vector3i offset = orientation.getDirection();
        Vector3i neighborPos = new Vector3i(getPosition().x + offset.x,
            getPosition().y + offset.y,
            getPosition().z + offset.z);

        BlockType bt = world.getBlockType(neighborPos);

        HytaleLogger.getLogger().atInfo().log("Notifying neighbor at position: " + neighborPos
            + " direction: " + orientation + " (world-space)");

            if (LogicaConstants.isLogicaComponent(bt)) {
                ILogicaComponent neighbor = nm.getComponentAt(neighborPos);
                if (neighbor == null) {
                    neighbor = nm.createComponentForId(neighborPos, bt.getId(), world);
                }

                if (neighbor != null) {
                    nm.enqueueUpdate(neighbor);
                }
            }
        }
    }

    @Override
    public void onPlace(World world) {
        updateOutput(world, null);
    }

    @Override
    public void onBreak(World world) {

    }

    @Override
    public abstract void render(World world);

}
