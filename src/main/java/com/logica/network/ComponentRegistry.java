package com.logica.network;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.components.core.ILogicaComponent;
import com.logica.vars.LogicaConstants;

import java.util.function.BiFunction;

/**
 * Abstraction for mapping block IDs to component factories.
 */
public interface ComponentRegistry {
    void register(LogicaConstants.BlockId blockId, BiFunction<Vector3i, World, ILogicaComponent> factory);

    BiFunction<Vector3i, World, ILogicaComponent> resolve(LogicaConstants.BlockId blockId);

    ILogicaComponent create(LogicaConstants.BlockId blockId, Vector3i pos, World world);
}
