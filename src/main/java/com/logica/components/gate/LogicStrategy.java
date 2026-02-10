package com.logica.components.gate;

import java.util.List;

/**
 * Strategy interface for evaluating logic gates.
 */
@FunctionalInterface
public interface LogicStrategy {
    boolean evaluate(List<Boolean> inputs);
}
