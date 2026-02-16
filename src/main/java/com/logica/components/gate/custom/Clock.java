package com.logica.components.gate.custom;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NetComp;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.Collections;
import java.util.List;

public class Clock extends NetComp {

    private int tickCount = 0;

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    private static final int TOGGLE_TICKS = 20; // Approx 1 second assuming 20 TPS

    public Clock(Vector3i position) {
        super(position, LogicaConstants.BlockId.COMP_CLOCK);
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        // Clock logic handled in update() override to support counting
    }

    @Override
    public void updateOutput(World world, NetComp caller) {
        if (world == null) return;

        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        int rot = accessor.getRotationIndex(getPosition());
        if (rot != state.rotation()) {
            state.setRotation(rot);
        }
        tickCount++;
        if (tickCount >= TOGGLE_TICKS) {
            tickCount = 0;
            boolean newState = !state.isOn();
            state.setOn(newState);
            render(world);
            notifyNeighbors(world);
        }

        // Re-queue to keep the clock running
        LogicaNetworkManager.getInstance().enqueueUpdate(this);
    }

    @Override
    public void onRecover(World world) {
        // Ensure the clock starts running when loaded
        LogicaNetworkManager.getInstance().enqueueUpdate(this);
    }

    @Override
    public void render(World world) {
        BlockType blockType = world.getBlockType(getPosition());
        if (blockType != null) {
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            String stateName = isActive() ? "Active" : "default";
            // Use force=false to avoid spam
            accessor.setBlockInteractionState(getPosition(), blockType, stateName, false);
            world.performBlockUpdate(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        }
    }

    @Override
    public List<Vector3i> getInputs(World world) {
        return Collections.emptyList();
    }

    @Override
    public List<Vector3i> getOutputs(World world) {
        Vector3i dir = Orientation.fromRotationIndex(state.rotation()).getDirection();
        return List.of(new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y, getPosition().z + dir.z));
    }

    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        if (!isActive()) return false;
        Vector3i dir = Orientation.fromRotationIndex(state.rotation()).getDirection();
        Vector3i target = new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y, getPosition().z + dir.z);
        return neighborPos.equals(target);
    }

    @Override
    public boolean canAcceptInputFrom(Vector3i neighborPos, Orientation relativeDir) {
        return false;
    }

    @Override
    public boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        if (relativeDir == null) return false;
        Vector3i dir = Orientation.fromRotationIndex(state.rotation()).getDirection();
        return dir.equals(relativeDir.getDirection());
    }
}
