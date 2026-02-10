package com.logica.components.pipes;

import com.logica.components.core.PipeConnectionContext;

public interface PipeShapeStrategy {
    boolean matches(PipeConnectionContext context);

    ShapeResult calculate(PipeConnectionContext context);
}
