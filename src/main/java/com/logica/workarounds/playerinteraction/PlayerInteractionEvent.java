package com.logica.workarounds.playerinteraction;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;

public record PlayerInteractionEvent(InteractionType interactionType, String uuid, String itemInHandId, SyncInteractionChain interaction) {
  public PlayerInteractionEvent(InteractionType interactionType, String uuid, String itemInHandId, SyncInteractionChain interaction) {
    this.interactionType = interactionType;
    this.uuid = uuid;
    this.itemInHandId = itemInHandId;
    this.interaction = interaction;
  }

  public InteractionType interactionType() {
    return this.interactionType;
  }

  public String uuid() {
    return this.uuid;
  }

  public String itemInHandId() {
    return this.itemInHandId;
  }

  public SyncInteractionChain interaction() {
    return this.interaction;
  }
}