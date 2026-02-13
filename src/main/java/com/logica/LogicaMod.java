package com.logica;

import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.logica.components.managers.LogicaPressurePlateManager;
import com.logica.eventhandlers.LogicaEventListener;
import com.logica.eventhandlers.LogicaInteractEventSystem;
import com.logica.eventhandlers.hytaleIntegration.LogicaBreakEventSystem;
import com.logica.eventhandlers.hytaleIntegration.LogicaPlaceEventSystem;
import com.logica.workarounds.playerinteraction.PlayerInteractLib;
import com.logica.workarounds.playerinteraction.PlayerInteractLibHandler;
import com.logica.system.LogicaLogicTicker;
import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogicaMod extends JavaPlugin {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static LogicaMod instance;
    private LogicaEventListener eventListener;

    public LogicaMod(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        LogicaLogger.info("Logica mod loaded!");
    }

    public static LogicaMod getInstance() {
        return instance;
    }

    public ScheduledExecutorService getScheduler() {
        return this.scheduler;
    }

    @Override
    protected void setup() {
        LogicaLogger.setDebug(false);
        try {
            registerEvents();
            PlayerInteractLib playerInteractLib = new PlayerInteractLib();
            playerInteractLib.initialize(this);

            PlayerInteractLibHandler interactHandler = new PlayerInteractLibHandler();
            interactHandler.initialize();

            LogicaPressurePlateManager pressurePlateManager = new LogicaPressurePlateManager();
            getScheduler().scheduleAtFixedRate(pressurePlateManager, 0L, 50L, TimeUnit.MILLISECONDS);

            // Ticker for logical propagation (20 TPS)
            LogicaLogicTicker logicTicker = new LogicaLogicTicker();
            getScheduler().scheduleAtFixedRate(logicTicker, 0L, 50L, TimeUnit.MILLISECONDS);

            LogicaLogger.info("Logica mod setup complete!");
        } catch (Exception e) {
            LogicaLogger.warn("Failed to setup Logica mod: " + e.getMessage());
        }
    }

    private void registerEvents() {
        try {
            eventListener = new LogicaEventListener();
            EntityModule.get().getEntityStoreRegistry().registerSystem(new LogicaPlaceEventSystem());
            EntityModule.get().getEntityStoreRegistry().registerSystem(new LogicaBreakEventSystem());
            EntityModule.get().getEntityStoreRegistry().registerSystem(new LogicaInteractEventSystem());
            LogicaLogger.info("[GateForge] Event systems registered via EntityModule");
        } catch (Exception e) {
            LogicaLogger.warn("Failed to register event system: " + e.getMessage());
        }
    }

    public LogicaEventListener getEventListener() {
        return eventListener;
    }
}
