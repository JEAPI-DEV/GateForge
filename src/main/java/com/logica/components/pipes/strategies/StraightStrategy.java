package com.logica.components.pipes.strategies;

import com.hypixel.hytale.logger.HytaleLogger;
import com.logica.components.core.PipeConnectionContext;
import com.logica.components.pipes.PipeShape;
import com.logica.components.pipes.PipeShapeStrategy;
import com.logica.components.pipes.ShapeResult;
import com.logica.vars.Orientation;

public class StraightStrategy implements PipeShapeStrategy {
    private static final HytaleLogger LOG = HytaleLogger.getLogger();

    @Override
    public boolean matches(PipeConnectionContext context) {
        boolean valid2Way = context.getHorizontalConnectionCount() == 2 &&
                ((context.n && context.s) || (context.e && context.w));
        boolean vertical = context.up || context.down;
        return valid2Way || vertical;
    }

    @Override
    public ShapeResult calculate(PipeConnectionContext context) {
        int rotation;

        if (context.n && context.s) {
            rotation = 0;
        } else if (context.e && context.w) {
            rotation = 1;
        } else if (context.up || context.down) {
            rotation = (context.e || context.w) ? 1 : 0;
        } else {
            rotation = (context.e || context.w) ? 1 : 0;
        }

        HytaleLogger.getLogger().atWarning().log("Using Straight Strategy");
//        if(rotation == 1 && context.vw) {
//            rotation = 3;
//        }
//        if(rotation == 0 && context.ve) {
//            rotation = 2;
//        }

        java.util.Set<Orientation> relative = context.getRelativeVerticals(rotation);
        boolean localVN = relative.contains(Orientation.NORTH);
        boolean localVS = relative.contains(Orientation.SOUTH);

        if (localVS && !localVN) {
            // Flip rotation
            int oldRot = rotation;
            rotation = (rotation == 0) ? 2 : 3;
            localVN = true;
            localVS = false;
            LOG.atInfo().log("[Logica][Pipe] Normalizing Straight: rot %d -> %d", oldRot, rotation);
        }

        ShapeResult result = new ShapeResult(PipeShape.STRAIGHT, rotation);
        StringBuilder stateName = new StringBuilder(PipeShape.STRAIGHT.toString());

        if (localVN) {
            stateName.append("_VN");
        }
        if (localVS) {
            stateName.append("_VS");
        }

        result.setState(stateName.toString());
        return result;
    }
}
