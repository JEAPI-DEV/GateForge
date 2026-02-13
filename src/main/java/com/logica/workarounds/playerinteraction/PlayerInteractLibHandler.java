package com.logica.workarounds.playerinteraction;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.LogicaMod;
import com.logica.eventhandlers.LogicaEventListener;

import java.util.concurrent.Flow;

/**
 * Handles PlayerInteractionEvents from the PlayerInteractLib and forwards them
 * to the LogicaEventListener for processing lever interactions.
 */
public class PlayerInteractLibHandler implements Flow.Subscriber<PlayerInteractionEvent> {
    // private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final LogicaEventListener eventListener;

    public PlayerInteractLibHandler() {
        this.eventListener = LogicaMod.getInstance().getEventListener();
    }

    /**
     * Initializes the handler by subscribing to the PlayerInteractLib event
     * publisher.
     */
    public void initialize() {
        PlayerInteractLib interactLib = getPlayerInteractLib();
        if (interactLib != null) {
            interactLib.getPublisher().subscribe(this);
        }
    }

    /**
     * Gets the PlayerInteractLib instance using the static accessor.
     * Returns null if not yet initialized.
     */
    private PlayerInteractLib getPlayerInteractLib() {
        try {
            PlayerInteractLib lib = PlayerInteractLib.getInstance();
            return lib;
        } catch (Exception e) {
            // LOGGER.atWarning().log("[GateForge] Error accessing PlayerInteractLib: " +
            // e.getMessage());
            return null;
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
        // LOGGER.atInfo().log("[GateForge] Subscribed to PlayerInteractionEvent stream");
    }

    @Override
    public void onNext(PlayerInteractionEvent event) {
        try {
            handleInteractionEvent(event);
        } catch (Exception e) {
            // LOGGER.atWarning().log("[GateForge] Error handling interaction event: " +
            // e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        // LOGGER.atWarning().log("[GateForge] Error in PlayerInteractionEvent stream: " +
        // throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        // LOGGER.atInfo().log("[GateForge] PlayerInteractionEvent stream completed");
    }

    /**
     * Handles a PlayerInteractionEvent by extracting the block position and
     * forwarding it to the LogicaEventListener.
     */
    private void handleInteractionEvent(PlayerInteractionEvent event) {
        SyncInteractionChain chain = event.interaction();

        // Check if this is a lever interaction
        String itemInHand = event.itemInHandId();
        InteractionType interactionType = event.interactionType();

        // ONLY respond to Use (F) interactions to prevent scrolling/wheeling from
        // triggering it
        if (interactionType != InteractionType.Use) {
            return;
        }

        // Get the block position from the interaction chain data
        if (chain != null && chain.data != null && chain.data.blockPosition != null) {
            // Convert BlockPosition to Vector3i
            var blockPos = chain.data.blockPosition;
            Vector3i pos = new Vector3i(blockPos.x, blockPos.y, blockPos.z);

            // LOGGER.atInfo().log("[GateForge] Interaction at position: " + pos);

            // Get the world and forward to event listener
            World world = Universe.get().getDefaultWorld();
            if (world != null && eventListener != null) {
                // Check if it's a lever block at this position
                world.execute(() -> {
                    try {
                        // HytaleLogger.getLogger().atInfo().log("[GateForge][PlayerInteractLib] Handling
                        // interaction at " + pos);
                        eventListener.onBlockInteract(world, pos);
                    } catch (Exception e) {
                        // LOGGER.atWarning().log("[GateForge] Error in onBlockInteract: " +
                        // e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
