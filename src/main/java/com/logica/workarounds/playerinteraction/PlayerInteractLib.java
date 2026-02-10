package com.logica.workarounds.playerinteraction;


import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.logica.workarounds.playerinteraction.packets.PlayerInteractionPacketHandler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

/**
 * PlayerInteractLib handles player interaction packets and publishes PlayerInteractionEvents.
 * Can be used as a standalone JavaPlugin or as a component within another plugin.
 */
public class PlayerInteractLib {
  //private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static PlayerInteractLib instance;
  private final SubmissionPublisher<PlayerInteractionEvent> publisher = new SubmissionPublisher();
  private ScheduledFuture<?> scheduledFuture;
  private PlayerInteractionPacketHandler packetHandler;
  private boolean initialized = false;

  public static PlayerInteractLib getInstance() {
    return instance;
  }

  public SubmissionPublisher<PlayerInteractionEvent> getPublisher() {
    return this.publisher;
  }

  public PlayerInteractLib() {
    instance = this;
  }

  /**
   * Initializes the library with the given plugin context.
   * This should be called from the parent plugin's setup() method.
   *
   * @param plugin The parent plugin providing context
   */
  public void initialize(JavaPlugin plugin) {
    if (initialized) {
      //LOGGER.atInfo().log("PlayerInteractLib already initialized, skipping");
      return;
    }
    
    //LOGGER.atInfo().log("Setting up PlayerInteractLib");
    this.packetHandler = new PlayerInteractionPacketHandler(this.publisher);
    this.packetHandler.registerListeners();
    
    // Register disconnect handler
    plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    
    // Start cleanup task
    this.scheduledFuture = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::clearData, 0L, 1L, TimeUnit.SECONDS);
    
    initialized = true;
    //LOGGER.atInfo().log("PlayerInteractLib initialized successfully");
  }

  private void clearData() {
    // Cleanup logic if needed
  }

  private void onPlayerDisconnect(PlayerDisconnectEvent playerDisconnectEvent) {
    if (this.packetHandler != null) {
      this.packetHandler.onPlayerDisconnect(playerDisconnectEvent.getPlayerRef().getUuid());
    }
  }
  
  /**
   * Shuts down the library and cleans up resources.
   */
  public void shutdown() {
    if (this.scheduledFuture != null) {
      this.scheduledFuture.cancel(false);
    }
    this.publisher.close();
    initialized = false;
    //LOGGER.atInfo().log("PlayerInteractLib shut down");
  }
}