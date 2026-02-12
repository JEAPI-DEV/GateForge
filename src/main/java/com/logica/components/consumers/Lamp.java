package com.logica.components.consumers;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.utils.PowerUtil;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.List;

/**
 * Lamp implementation.
 * Visual indicator that lights up when powered.
 * Strict consumer - does not provide power to neighbors.
 */
public class Lamp extends Consumer {

    public Lamp(Vector3i position) {
        super(position);
    }

    @Override
    protected void calculateNewState(World world, com.logica.components.core.NetComp caller) {
        boolean shouldBeActive = isPowered(world);
        LogicaLogger.debug(
                "[Logica][LampDebug] calc pos=%s sources=%d shouldBeActive=%s",
                getPosition(), state.activeSources().size(), shouldBeActive);
        applyStateChange(world, shouldBeActive);
    }

    @Override
    public void render(World world) {
        BlockType blockType = world.getBlockType(getPosition());
        if (blockType != null) {
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            String stateName = isActive() ? "Active" : "default";
            accessor.setBlockInteractionState(getPosition(), blockType, stateName, false);
            world.performBlockUpdate(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        }
    }

    @Override
    public void onRecover(World world) {
        render(world);
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    @Override
    public List<Vector3i> getInputs(World world) {
        return NeighborScanner.sixWay(getPosition());
    }

    @Override
    public List<Vector3i> getOutputs(World world) {
        return List.of();
    }

    @Override
    public boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        return false;
    }
}
