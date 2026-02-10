 package com.logica.workarounds.playerinteraction.packets;

import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.logica.workarounds.playerinteraction.PlayerInteractionEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public final class PlayerInteractionPacketHandler implements PacketWatcher {
   private final SubmissionPublisher<PlayerInteractionEvent> publisher;
   private final Map<PlayerInteractionPacketHandler.InteractionKey, PlayerInteractionPacketHandler.PendingInteraction> pending = new ConcurrentHashMap();
   private final Set<PlayerInteractionPacketHandler.InteractionKey> finishedChains = ConcurrentHashMap.newKeySet();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private static final long CLEANUP_DELAY_MS = 1500L;

   public PlayerInteractionPacketHandler(SubmissionPublisher<PlayerInteractionEvent> publisher) {
      this.publisher = publisher;
   }

   public void accept(PacketHandler handler, Packet packet) {
      if (packet.getId() == 290) {
         PlayerAuthentication auth = handler.getAuth();
         if (auth != null) {
            UUID uuid = auth.getUuid();
            SyncInteractionChains chains = (SyncInteractionChains)packet;
            SyncInteractionChain[] var6 = chains.updates;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               SyncInteractionChain update = var6[var8];
               PlayerInteractionPacketHandler.InteractionKey key = new PlayerInteractionPacketHandler.InteractionKey(uuid, update.chainId);
               PlayerInteractionPacketHandler.PendingInteraction p = (PlayerInteractionPacketHandler.PendingInteraction)this.pending.computeIfAbsent(key, (k) -> {
                  return new PlayerInteractionPacketHandler.PendingInteraction();
               });
               p.merge(update);
               if (update.state == InteractionState.Finished && this.finishedChains.add(key)) {
                  this.publisher.submit(new PlayerInteractionEvent(p.merged.interactionType, uuid.toString(), p.merged.itemInHandId, p.merged));
                  this.scheduleCleanup(key);
               }
            }

         }
      }
   }

   private void scheduleCleanup(PlayerInteractionPacketHandler.InteractionKey key) {
      this.scheduler.schedule(() -> {
         this.pending.remove(key);
         this.finishedChains.remove(key);
      }, 1500L, TimeUnit.MILLISECONDS);
   }

   public void onPlayerDisconnect(UUID uuid) {
      this.pending.keySet().removeIf((k) -> {
         return k.uuid.equals(uuid);
      });
      this.finishedChains.removeIf((k) -> {
         return k.uuid.equals(uuid);
      });
   }

   public void registerListeners() {
      PacketAdapters.registerInbound(this);
   }

   private static record InteractionKey(UUID uuid, int chainId) {
      private InteractionKey(UUID uuid, int chainId) {
         this.uuid = uuid;
         this.chainId = chainId;
      }

      public UUID uuid() {
         return this.uuid;
      }

      public int chainId() {
         return this.chainId;
      }
   }

   private static final class PendingInteraction {
      SyncInteractionChain merged;

      void merge(SyncInteractionChain source) {
         if (this.merged == null) {
            this.merged = new SyncInteractionChain(source);
         } else {
            this.mergeSingleChain(this.merged, source);
         }
      }

      private void mergeSingleChain(SyncInteractionChain target, SyncInteractionChain source) {
         if (source.itemInHandId != null && target.itemInHandId == null) {
            target.itemInHandId = source.itemInHandId;
         }

         if (source.utilityItemId != null && target.utilityItemId == null) {
            target.utilityItemId = source.utilityItemId;
         }

         if (source.toolsItemId != null && target.toolsItemId == null) {
            target.toolsItemId = source.toolsItemId;
         }

         if (source.data != null) {
            if (target.data == null) {
               target.data = source.data;
            } else {
               if (source.data.entityId != -1) {
                  target.data.entityId = source.data.entityId;
               }

               target.data.proxyId = source.data.proxyId;
               if (source.data.targetSlot != Integer.MIN_VALUE) {
                  target.data.targetSlot = source.data.targetSlot;
               }

               if (source.data.blockPosition != null) {
                  target.data.blockPosition = source.data.blockPosition;
               }

               if (source.data.hitLocation != null) {
                  target.data.hitLocation = source.data.hitLocation;
               }

               if (source.data.hitNormal != null) {
                  target.data.hitNormal = source.data.hitNormal;
               }

               if (source.data.hitDetail != null) {
                  target.data.hitDetail = source.data.hitDetail;
               }
            }
         }

         if (source.interactionData != null && target.interactionData == null) {
            target.interactionData = source.interactionData;
         }

         if (source.forkedId != null && target.forkedId == null) {
            target.forkedId = source.forkedId;
         }

         if (source.newForks != null && target.newForks == null) {
            target.newForks = source.newForks;
         }

         target.operationBaseIndex = source.operationBaseIndex;
         target.desync |= source.desync;
         target.initial |= source.initial;
         target.state = source.state;
      }
   }
}
    