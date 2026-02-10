package com.logica.components.consumers;

import com.hypixel.hytale.logger.HytaleLogger;
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
        boolean shouldBeActive = !state.activeSources().isEmpty();
        boolean strongPower = false;
        if (!shouldBeActive && world != null) {
            for (Orientation orientation : Orientation.ALL) {
                Vector3i offset = orientation.getDirection();
                Vector3i neighborPos = new Vector3i(getPosition().x + offset.x, getPosition().y + offset.y,
                        getPosition().z + offset.z);
                if (PowerUtil.isSolidBlockReceivingStrongPower(world, neighborPos, getPosition(), true)) {
                    strongPower = true;
                    shouldBeActive = true;
                    break;
                }
            }
        }
        HytaleLogger.getLogger().atInfo().log(
                "[Logica][LampDebug] calc pos=%s sources=%d strongPower=%s shouldBeActive=%s",
                getPosition(), state.activeSources().size(), strongPower, shouldBeActive);
        applyStateChange(world, shouldBeActive);
    }

    @Override
    public void render(World world) {
        updateBlockState(world);
    }

    @Override
    public void onRecover(World world) {
        render(world);
    }

    @Override
    public boolean isProvidingPowerTo(Vector3i neighborPos) {
        return false; // Lamps are consumers only
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    private void updateBlockState(World world) {
        BlockType blockType = world.getBlockType(getPosition());
        if (blockType != null) {
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
            String stateName = isActive() ? "Active" : "default";
            accessor.setBlockInteractionState(getPosition(), blockType, stateName, false);
            world.performBlockUpdate(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        }
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
