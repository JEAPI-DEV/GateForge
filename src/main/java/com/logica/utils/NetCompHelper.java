package com.logica.utils;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import com.logica.components.misc.ComponentState;
import com.logica.vars.LogicaConstants;
import com.logica.workarounds.LogicaBlockAccessor;

public class NetCompHelper {
    public static boolean samePos(Vector3i a, Vector3i b) {
        if (a == null || b == null)
            return false;
        return a.x == b.x && a.y == b.y && a.z == b.z;
    }

    public static boolean isBlockSolid(BlockType bt) {
        return bt != null && "Solid".equalsIgnoreCase(String.valueOf(bt.getMaterial()));
    }

    public static boolean isLogicaPipe(BlockType bt) {
        return bt != null && bt.getId() != null
                && LogicaConstants.BlockId.PIPE.id().equalsIgnoreCase(bt.getId());
    }

    public static void updateBlockState(World world, ComponentState state) {
        try {
            BlockType blockType = world.getBlockType(state.pos());
            if (blockType != null) {
                Vector3i pos = state.pos();
                LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
                int typeIndex = BlockType.getAssetMap().getIndex(blockType.getId());

                boolean updated = accessor.setBlock(pos.x,
                        pos.y, pos.z, typeIndex,
                        blockType,
                        state.rotation(),
                        0, 198);
                if (!updated)
                    return;
                try {
                    world.performBlockUpdate(pos.getX(), pos.getY(), pos.getZ());
                } catch (Throwable ignored) {
                }
            }
        } catch (Exception e) {
            LogicaLogger.warn("[Logica][Gate] Error in BlockState Handler: " + e);
        }
    }
}
