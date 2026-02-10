package com.logica.components.consumers;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.vars.Orientation;
import com.logica.workarounds.LogicaBlockAccessor;

public class StickyPiston extends Piston {

    public StickyPiston(Vector3i position) {
        super(position);
    }

    @Override
    protected void onRetract(World world) {
        super.onRetract(world);

        LogicaBlockAccessor accessor = LogicaBlockAccessor.forWorld(world);
        if (accessor == null)
            return;

        Orientation pushDir = Orientation.fromRotationIndex(state.rotation());
        Vector3i dir = pushDir.getDirection();

        // The block that might be pulled is 2 blocks away (since piston head was at 1
        // block away)
    Vector3i targetPos = new Vector3i(getPosition().x + dir.x * 2, getPosition().y + dir.y * 2,
        getPosition().z + dir.z * 2);

        BlockType blockToPull = world.getBlockType(targetPos);

        // Check if there is a valid block to pull
        if (blockToPull != null && !isUnpushable(blockToPull)) {

            int rot = accessor.getRotationIndex(targetPos);
            int typeIndex = BlockType.getAssetMap().getIndex(blockToPull.getId());
            Vector3i destPos = new Vector3i(getPosition().x + dir.x, getPosition().y + dir.y, getPosition().z + dir.z);
            accessor.setBlock(destPos.x, destPos.y, destPos.z, typeIndex, blockToPull, rot, 0, 198);
            accessor.setBlock(targetPos.x, targetPos.y, targetPos.z, 0, null, 0, 0, 198);
        }
    }
}
