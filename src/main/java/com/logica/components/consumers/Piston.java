package com.logica.components.consumers;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.ILogicaComponent;
import com.logica.components.core.NeighborScanner;
import com.logica.components.core.NetComp;
import com.logica.network.LogicaNetworkManager;
import com.logica.utils.PowerUtil;
import com.logica.vars.LogicaConstants;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Piston implementation.
 * Pushes up to 10 blocks when powered.
 */
public class Piston extends Consumer {

    public Piston(Vector3i position) {
        super(position);
    }

    @Override
    protected void calculateNewState(World world, com.logica.components.core.NetComp caller) {
        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        if (accessor != null) {
            int rot = accessor.getRotationIndex(getPosition());
            if (rot != -1) {
                state.setRotation(rot);
            }
        }

        boolean powered = isPowered(world);
        if (powered && !state.isOn()) {
            if (tryPush(world)) {
                applyStateChange(world, true);
            }
        } else if (!powered && state.isOn()) {
            applyStateChange(world, false);
            onRetract(world);
        }
    }

    protected void onRetract(World world) {
        // Default behavior: do nothing
    }

    private boolean tryPush(World world) {
        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        if (accessor == null)
            return false;

        Orientation pushDir = Orientation.fromRotationIndex(state.rotation());
        Vector3i dir = pushDir.getDirection();

        List<BlockInfo> blocksToMove = new ArrayList<>();
        boolean foundGap = false;

        for (int i = 1; i <= 11; i++) {
            Vector3i pos = new Vector3i(getPosition().x + dir.x * i, getPosition().y + dir.y * i,
                    getPosition().z + dir.z * i);
            BlockType bt = world.getBlockType(pos);

            // Robust Air/Gap check
            if (bt == null || bt.getId() == null || bt.getId().equalsIgnoreCase("empty")
                    || bt.getId().equalsIgnoreCase("hytale:empty")) {
                foundGap = true;
                break;
            }

            if (i > 10) return false;
            if (isUnpushable(pos, bt)) return false;
            blocksToMove.add(new BlockInfo(pos, bt, accessor.getRotationIndex(pos)));
        }

        if (!foundGap) return false;

        for (int i = blocksToMove.size() - 1; i >= 0; i--) {
            BlockInfo bi = blocksToMove.get(i);
            Vector3i target = new Vector3i(bi.pos().x + dir.x, bi.pos().y + dir.y, bi.pos().z + dir.z);
            int typeIndex = BlockType.getAssetMap().getIndex(bi.type().getId());
            accessor.setBlock(target.x, target.y, target.z, typeIndex, bi.type(), bi.rotation(), 0, 198);

            if (LogicaConstants.isLogicaComponent(bi.type())) {
                ILogicaComponent comp = LogicaNetworkManager.getInstance().getComponentAt(bi.pos());
                if (comp instanceof NetComp netComp) {
                    netComp.setPosition(target);
                }
            }
        }

        Vector3i front = new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y, getPosition().z + dir.z);
        accessor.setBlock(front.x, front.y, front.z, 0, null, 0, 0, 198);

        return true;
    }

    protected boolean isUnpushable(Vector3i pos, BlockType bt) {
        if (bt == null || bt.getId() == null)
            return false;

        String id = bt.getId();
        if (id.equals(LogicaConstants.KEY_PISTON) ||
                id.equals(LogicaConstants.KEY_STICKY_PISTON)) {

            ILogicaComponent comp = LogicaNetworkManager.getInstance()
                    .getComponentAt(pos);

            if (comp != null && comp.isActive()) return true;
        }
        return false;
    }

    @Override
    public void render(World world) {
        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        BlockType blockType = world.getBlockType(getPosition());
        if (blockType != null && accessor != null) {
            String stateName = isActive() ? "Extended" : "default";
            accessor.setBlockInteractionState(getPosition(), blockType, stateName, false);
            world.performBlockUpdate(getPosition().x, getPosition().y, getPosition().z);
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
    public boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    @Override
    public void onRecover(World world) {
        render(world);
    }

    private record BlockInfo(Vector3i pos, BlockType type, int rotation) {
    }
}
