package com.logica.components.providers;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.core.PowerProvider;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.List;

/**
 * Lever implementation.
 * Manually toggled power source.
 */
public class Lever extends PowerProvider {

    public Lever(Vector3i position) {
        super(position, LogicaConstants.BlockId.PROVIDER_LEVER);
    }

    @Override
    public void onInteract(World world) {
        updateOutput(world, !isActive());
        notifyNeighbors(world);
        render(world);
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        // Levers don't update based on neighbors, they are toggled manually.
    }

    @Override
    public void render(World world) {
        try {
            BlockType blockType = world.getBlockType(getPosition());
            if (blockType != null) {
                String state = isActive() ? "Active" : "default";
                LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);

                accessor.setBlockInteractionState(getPosition(), blockType, state, false);
                world.performBlockUpdate(getPosition().getX(), getPosition().getY(), getPosition().getZ());
            }
        } catch (Exception e) {
            LogicaLogger.warn("[GateForge][Lever] Error updating block state: " + e.getMessage());
        }
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {

        if (neighborPos == null) return false;
        int dx = Math.abs(neighborPos.x - getPosition().x);
        int dy = Math.abs(neighborPos.y - getPosition().y);
        int dz = Math.abs(neighborPos.z - getPosition().z);
        return (dx + dy + dz) == 1;
    }


    @Override
    public List<Vector3i> getInputs(World world) {
        return List.of();
    }

    @Override
    public List<Vector3i> getOutputs(World world) {
        return NeighborScanner.sixWay(getPosition());
    }

    @Override
    public void onRecover(World world) {
        try {
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            if (accessor != null) {
                int rot = accessor.getRotationIndex(getPosition());
                this.state = state.withRotation(rot);
                LogicaLogger.info("[GateForge][Lever] rotation=%d facing=%s pos=%s",
                        rot, Orientation.fromRotationIndex(rot), getPosition());
            }
        } catch (Exception ignored) {
        }
    }
}
