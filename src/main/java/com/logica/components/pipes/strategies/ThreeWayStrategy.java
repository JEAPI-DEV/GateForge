package com.logica.components.pipes.strategies;

import com.hypixel.hytale.logger.HytaleLogger;
import com.logica.components.core.PipeConnectionContext;
import com.logica.components.pipes.PipeShape;
import com.logica.components.pipes.PipeShapeStrategy;
import com.logica.components.pipes.ShapeResult;
import com.logica.vars.Orientation;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;

public class ThreeWayStrategy implements PipeShapeStrategy {

    @Override
    public boolean matches(PipeConnectionContext context) {
        return context.getHorizontalConnectionCount() == 3;
    }

    @Override
    public ShapeResult calculate(PipeConnectionContext context) {
        int rotation;
        // Logic:
        // if !w -> 0 (NES)
        // if !n -> 3 (ESW)
        // if !e -> 2 (SWN)
        // else -> 1 (WNE)

        if (!context.w)
            rotation = 0;
        else if (!context.n)
            rotation = 1;
        else if (!context.e)
            rotation = 2;
        else
            rotation = 3;

        // 3. Calculate Base Shape & State (with internal post-processing)
        HytaleLogger.getLogger().atInfo().log(
                "[Logica][ContexThreeWayStrategy]: H(N:%b S:%b E:%b W:%b) V(N:%b S:%b E:%b W:%b)",
                context.n, context.s, context.e, context.w,
                context.vn, context.vs, context.ve, context.vw);

        // Tagging
        Set<Orientation> relative = context.getRelativeVerticals(rotation);

        return getShapeResult(context, relative, rotation);
    }

    @NonNullDecl
    private static ShapeResult getShapeResult(PipeConnectionContext context, Set<Orientation> relative, int rotation) {
        StringBuilder stateName = new StringBuilder(PipeShape.THREE_WAY.toString());

        boolean dontskip = true;
        if (!context.n && context.s && context.e && context.w
                && !context.vn && context.vs && !context.ve && context.vw) {
            stateName.append("_VS");
            stateName.append("_VW");
            dontskip = false;
        }

        if (relative.contains(Orientation.NORTH) && dontskip)
            stateName.append("_VN");
        if (relative.contains(Orientation.EAST) && dontskip)
            stateName.append("_VE");
        if (relative.contains(Orientation.SOUTH) && dontskip)
            stateName.append("_VS");

        ShapeResult result = new ShapeResult(PipeShape.THREE_WAY, rotation);
        result.setState(stateName.toString());
        return result;
    }
}
