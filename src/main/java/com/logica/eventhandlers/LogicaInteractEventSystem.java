package com.logica.eventhandlers;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.logica.components.interfaces.ILogicaComponent;
import com.logica.network.LogicaNetworkManager;
import com.logica.vars.LogicaConstants;

import javax.annotation.Nonnull;

public class LogicaInteractEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    public LogicaInteractEventSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull UseBlockEvent.Pre event) {
        HytaleLogger.getLogger().atWarning().log("Called InteractEventHandler");
        try {
            if (LogicaConstants.isLogicaComponent(event.getBlockType())) {
                Vector3i targetPos = event.getTargetBlock();
                World world = Universe.get().getDefaultWorld();
                if (world == null) return;

                world.execute(() -> {
                    LogicaNetworkManager nm = LogicaNetworkManager
                            .getInstance();
                    ILogicaComponent comp = nm.getComponentAt(targetPos);

                    if (comp != null) {
                        HytaleLogger.getLogger().atInfo()
                                .log("[Logica] Interaction with " + comp.getClass().getSimpleName());
                        comp.onInteract(world);
                    }
                });
            }
        } catch (Exception e) {
            HytaleLogger.getLogger().atWarning().log("Exception happened at InteractEventSystem: " + e);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                PlayerRef.getComponentType());
    }
}
