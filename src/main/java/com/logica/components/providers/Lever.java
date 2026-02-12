package com.logica.components.providers;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.components.core.PowerProvider;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.utils.NetCompHelper;
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
        super(position);
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
            LogicaLogger.warn("[Logica][Lever] Error updating block state: " + e.getMessage());
        }
    }

    @Override
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return isProvidingPowerTo(neighborPos);
    }

    @Override
    public void notifyNeighbors(World world) {
        super.notifyNeighbors(world);

        // Notify ALL adjacent solid blocks (Through-Block notification)
        // Since we can't rely on Rotation to detect Floor/Wall placement,
        // we notify any solid block we are touching.
        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        for (Orientation o : Orientation.ALL) {
            Vector3i offset = o.getDirection();
            Vector3i adjacentPos = new Vector3i(getPosition().x + offset.x, getPosition().y + offset.y,
                    getPosition().z + offset.z);
            BlockType adjacentBlock = world.getBlockType(adjacentPos);

            if (NetCompHelper.isBlockSolid(adjacentBlock)) {
                for (Orientation subDir : Orientation.ALL) {
                    Vector3i subOffset = subDir.getDirection();
                    Vector3i target = new Vector3i(adjacentPos.x + subOffset.x, adjacentPos.y + subOffset.y,
                            adjacentPos.z + subOffset.z);

                    if (target.equals(getPosition()))
                        continue;

                    ILogicaComponent neighbor = nm.getComponentAt(target);
                    if (neighbor != null) {
                        nm.enqueueUpdate(neighbor);
                    } else {
                        BlockType t = world.getBlockType(target);
                        if (LogicaConstants.isLogicaComponent(t)) {
                            neighbor = nm.createComponentForId(target, t.getId(), world);
                            if (neighbor != null) {
                                nm.enqueueUpdate(neighbor);
                            }
                        }
                    }
                }
            }
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
                LogicaLogger.info("[Logica][Lever] rotation=%d facing=%s pos=%s",
                        rot, Orientation.fromRotationIndex(rot), getPosition());
            }
        } catch (Exception ignored) {
        }
    }
}
