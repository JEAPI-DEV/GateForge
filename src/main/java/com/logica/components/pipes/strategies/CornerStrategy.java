package com.logica.components.pipes.strategies;

import com.logica.components.core.PipeConnectionContext;
import com.logica.components.pipes.PipeShape;
import com.logica.components.pipes.PipeShapeStrategy;
import com.logica.components.pipes.ShapeResult;
import com.logica.vars.Orientation;

public class CornerStrategy implements PipeShapeStrategy {

    @Override
    public boolean matches(PipeConnectionContext context) {
        // Matches if 2 horizontal connections but NOT straight
        if (context.getHorizontalConnectionCount() != 2)
            return false;

        boolean straight = (context.n && context.s) || (context.e && context.w);
        return !straight;
    }

    @Override
    public ShapeResult calculate(PipeConnectionContext context) {
        int rotation;
        if (context.n && context.e)
            rotation = 0;
        else if (context.e && context.s)
            rotation = 3;
        else if (context.s && context.w)
            rotation = 2;
        else
            rotation = 1; // WN

        // Tagging
        java.util.Set<Orientation> relative = context.getRelativeVerticals(rotation);

        StringBuilder stateName = new StringBuilder(PipeShape.CORNER.toString());

        if (relative.contains(Orientation.NORTH))
            stateName.append("_VN");
        if (relative.contains(Orientation.EAST))
            stateName.append("_VE");

        ShapeResult result = new ShapeResult(PipeShape.CORNER, rotation);
        result.setState(stateName.toString());
        return result;
    }
}
