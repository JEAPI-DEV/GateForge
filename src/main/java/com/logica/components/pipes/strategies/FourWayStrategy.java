package com.logica.components.pipes.strategies;

import com.logica.components.core.PipeConnectionContext;
import com.logica.components.pipes.PipeShape;
import com.logica.components.pipes.PipeShapeStrategy;
import com.logica.components.pipes.ShapeResult;
import com.logica.vars.Orientation;

public class FourWayStrategy implements PipeShapeStrategy {

    @Override
    public boolean matches(PipeConnectionContext context) {
        return context.getHorizontalConnectionCount() >= 4;
    }

    @Override
    public ShapeResult calculate(PipeConnectionContext context) {
        int rotation;
        int vCount = context.getVerticalClimbCount();

        // Logic from original:
        // if vCount == 4 -> rot 0
        // if vCount == 3 -> check missing
        // if vCount == 2 -> check opposed/adjacent
        // if vCount == 1 -> check specific

        if (vCount == 4) {
            rotation = 0;
        } else if (vCount == 3) {
            if (!context.vw)
                rotation = 0; // Has VN, VE, VS
            else if (!context.vn)
                rotation = 1; // Has VE, VS, VW
            else if (!context.ve)
                rotation = 2; // Has VS, VW, VN
            else
                rotation = 3; // Has VW, VN, VE
        } else if (vCount == 2) {
            if (context.vn && context.vs) {
                rotation = 0; // VN, VS (Opposite)
            } else if (context.ve && context.vw) {
                rotation = 3; // VE, VW (Opposite)
            } else {
                // Adjacent
                if (context.vn && context.ve)
                    rotation = 0;
                else if (context.ve && context.vs)
                    rotation = 1;
                else if (context.vs && context.vw)
                    rotation = 2;
                else
                    rotation = 3; // VW, VN
            }
        } else if (vCount == 1) {
            if (context.vn)
                rotation = 0;
            else if (context.ve)
                rotation = 1;
            else if (context.vs)
                rotation = 2;
            else
                rotation = 3;
        } else {
            rotation = 0;
        }

        java.util.Set<Orientation> relative = context.getRelativeVerticals(rotation);

        StringBuilder stateName = new StringBuilder(PipeShape.FOUR_WAY.toString());
        if (relative.contains(Orientation.NORTH))
            stateName.append("_VN");
        if (relative.contains(Orientation.EAST))
            stateName.append("_VE");
        if (relative.contains(Orientation.SOUTH))
            stateName.append("_VS");
        if (relative.contains(Orientation.WEST))
            stateName.append("_VW");

        ShapeResult result = new ShapeResult(PipeShape.FOUR_WAY, rotation);
        result.setState(stateName.toString());
        return result;
    }
}
