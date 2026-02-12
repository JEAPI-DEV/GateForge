package com.logica.system;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.network.LogicaNetworkManager;

/**
 * Periodically triggers logical propagation updates.
 * Submits work to the world thread to ensure thread-safety with Hytale APIs.
 */
public class LogicaLogicTicker implements Runnable {

    private final LogicTicker ticker;

    public LogicaLogicTicker() {
        this(LogicaNetworkManager.getInstance());
    }

    public LogicaLogicTicker(LogicTicker ticker) {
        this.ticker = ticker;
    }

    @Override
    public void run() {
        try {
            World world = Universe.get().getDefaultWorld();
            if (world == null)
                return;

            world.execute(() -> {
                try {
                    ticker.tick(world);
                } catch (Exception e) {
                    LogicaLogger.warn("[Logica][Ticker] Error in processUpdates: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            LogicaLogger.warn("[Logica][Ticker] Critical error in Ticker thread: " + e.getMessage());
        }
    }
}
