package com.logica.eventhandlers.hytaleIntegration;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.logica.utils.LogicaLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.logica.LogicaMod;
import com.logica.eventhandlers.LogicaEventListener;
import com.logica.vars.LogicaConstants;

import javax.annotation.Nonnull;

public class LogicaBreakEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public LogicaBreakEventSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        try {
            Vector3i targetPos = event.getTargetBlock();
            World world = Universe.get().getDefaultWorld();
            if (world == null)
                return;

            int x = targetPos.getX(), y = targetPos.getY(), z = targetPos.getZ();

            BlockType bt = event.getBlockType();
            String id = bt.getId();

            if (LogicaConstants.isLogicaComponent(bt)) {
                world.execute(() -> {
                    LogicaLogger.debug("[GateForge] Block broken: " + id + " at (" + x + ", " + y + ", " + z + ")");
                    LogicaEventListener listener = LogicaMod.getInstance().getEventListener();
                    if (listener != null)
                        listener.onBlockBreak(world, targetPos);
                });
            }
        } catch (Exception e) {
            LogicaLogger.warn("Error happened in BreakEventSystem: " + e);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
