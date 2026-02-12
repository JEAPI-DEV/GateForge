package com.logica.components.providers;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.core.PowerProvider;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.List;

public class PressurePlate extends PowerProvider {
    public PressurePlate(Vector3i position) {
        super(position);
    }

    @Override
    protected void calculateNewState(World world, NetComp caller) {
        // State is managed by LogicaPressurePlateManager
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return isProvidingPowerTo(neighborPos);
    }

    @Override
    public void render(World world) {
        var blockType = world.getBlockType(getPosition());
        if (blockType != null) {
            String state = isActive() ? "Active" : "default";
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            accessor.setBlockInteractionState(getPosition(), blockType, state, false);
            world.performBlockUpdate(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        }
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
                com.hypixel.hytale.logger.HytaleLogger.getLogger().atInfo().log(
                        "[Logica][PressurePlate] rotation=%d facing=%s pos=%s",
                        rot, com.logica.vars.Orientation.fromRotationIndex(rot), getPosition());
            }
        } catch (Exception ignored) { }
    }
}
