package com.logica.components.pipes;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

/**
 * Applies computed pipe shapes to the world, handling rotation/state updates.
 */
public final class PipeShapeApplier {

    private PipeShapeApplier() {
    }

    /**
     * Apply the given shape result to the world. Returns true if the block was
     * updated.
     */
    public static boolean apply(World world, Vector3i position, ShapeResult result) {
        try {
            BlockType baseType = world.getBlockType(position);
            if (baseType == null)
                return false;

            BlockType stateType = baseType.getBlockForState(result.getState());
            if (stateType == null) {
                LogicaLogger.warn("[Logica][Pipe] No stateType for %s state=%s", position, result.getState());
                return false;
            }

            int typeIndex = BlockType.getAssetMap().getIndex(stateType.getId());
            LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);

            int engineRotation = Orientation.toEngineRotationIndex(result.getRotation());
            if (accessor.setBlock(position.x, position.y, position.z, typeIndex, stateType, engineRotation, 0,
                    198)) {
                try {
                    world.performBlockUpdate(position.getX(), position.getY(), position.getZ());
                } catch (Throwable ignored) {
                }
                LogicaLogger.debug("[Logica][Pipe] Physically updated %s to %s (rot:%d)", position, result.getState(),
                        engineRotation);
                return true;
            }
        } catch (Exception ex) {
            LogicaLogger.warn("Error updating pipe shape: " + ex);
        }
        return false;
    }
}
