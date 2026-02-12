package com.logica.utils;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.Orientation;

import java.util.List;

public class PowerUtil {

    /**
     * Checks if a solid block at {@code target} is receiving strong power from any
     * neighbor
     * EXCEPT the one at {@code ignoredSource}.
     * 
     * Strong power mimics Minecraft behavior: a solid block is powered if a
     * component
     * is explicitly outputting INTO it.
     */
    public static boolean isSolidBlockReceivingStrongPower(World world, Vector3i target, Vector3i ignoredSource,
            boolean ignoreSource) {
        if (world == null)
            return false;

        BlockType targetBt = world.getBlockType(target);
        if (!NetCompHelper.isBlockSolid(targetBt))
            return false;
        if (targetBt != null && com.logica.vars.LogicaConstants.isLogicaBlock(targetBt.getId()))
            return false;

        LogicaNetworkManager nm = LogicaNetworkManager.getInstance();

        for (Orientation o : Orientation.ALL) {
            Vector3i dir = o.getDirection();
            Vector3i neighborPos = new Vector3i(target.x + dir.x, target.y + dir.y, target.z + dir.z);

            if (ignoreSource && NetCompHelper.samePos(neighborPos, ignoredSource))
                continue;

            ILogicaComponent component = nm.getComponentAt(neighborPos);
            if (component != null && component.isActive()) {
                // Pull Model: Only allow strong power when the component can power through the block.
                if (component.canProvidePowerThroughBlock(target)) {
                    List<Vector3i> outputs = component.getOutputs(world);
                    if (outputs != null) {
                        for (Vector3i output : outputs) {
                            if (NetCompHelper.samePos(output, target)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
