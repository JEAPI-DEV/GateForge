package com.logica.system;

import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Simple ticker interface to allow swap/mocking of tick drivers.
 */
public interface LogicTicker {
    void tick(World world);
}
