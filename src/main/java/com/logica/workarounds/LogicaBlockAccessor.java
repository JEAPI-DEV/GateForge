package com.logica.workarounds;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.IChunkAccessorSync;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Lightweight helper for accessing block data (rotation, interaction states,
 * block placement)
 * using the underlying {@link BlockAccessor} / chunk APIs. It also bridges
 * thread boundaries by
 * scheduling work onto the world executor when callers are off-thread.
 */
@SuppressWarnings("deprecation")
public final class LogicaBlockAccessor {
    private final World world;
    private final IChunkAccessorSync<? extends BlockAccessor> accessor;
    @NonNullDecl
    private final Executor executor;

    private LogicaBlockAccessor(@Nonnull World world,
            @Nonnull IChunkAccessorSync<? extends BlockAccessor> accessor) {
        this.world = world;
        this.accessor = accessor;
        this.executor = world;
    }

    /**
     * Build a helper for the provided world instance if it exposes chunk access.
     */
    @Nullable
    public static LogicaBlockAccessor forWorld(@Nullable World world) {
        if (world == null) {
            return null;
        }
        return new LogicaBlockAccessor(world, world);
    }

    private boolean isWorldThread() {
        try {
            // World implements isInThread(); use reflection to avoid compile-time
            // dependency.
            Object value = world.getClass().getMethod("isInThread").invoke(world);
            return value instanceof Boolean && (Boolean) value;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void runSync(Runnable runnable) {
        if (isWorldThread()) {
            runnable.run();
        } else {
            CompletableFuture.runAsync(runnable, executor).join();
        }
    }

    private <T> T callSync(Supplier<T> supplier, T defaultValue) {
        if (isWorldThread())
            try {
                return supplier.get();
            } catch (Exception ex) {
                return defaultValue;
            }
        try {
            return CompletableFuture.supplyAsync(supplier, executor).join();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * Fetch the rotation index of the block at the given position.
     */
    public int getRotationIndex(@Nonnull Vector3i pos) {
        return callSync(() -> {
            return accessor.getBlockRotationIndex(pos.getX(), pos.getY(), pos.getZ());
        }, 0);
    }

    /**
     * Update a block's interaction state via the chunk accessor, mirroring the
     * engine behavior.
     */
    public void setBlockInteractionState(@Nonnull Vector3i pos,
            @Nonnull BlockType blockType,
            @Nonnull String state,
            boolean force) {
        runSync(() -> {
            BlockAccessor chunk = accessor.getChunk(ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ()));
            if (chunk != null) {
                chunk.setBlockInteractionState(pos.getX(), pos.getY(), pos.getZ(), blockType, state, force);
            }
        });
    }

    /**
     * Safe setBlock wrapper that runs on the world executor when needed.
     */
    public boolean setBlock(int x, int y, int z, int typeId, @Nonnull BlockType blockType,
            int rotation, int filler, int settings) {
        return callSync(() -> {
            BlockAccessor chunk = accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) return false;

            //if(!val) HytaleLogger.getLogger().atWarning().log("%d|%d|%d , ID:%s, TYPE:%s," +
            //      " ROT:%d, FILLER:%d, SETTINGS:%d", x,y,z, typeId, blockType.toString(), rotation, filler, settings);
            return chunk.setBlock(x, y, z, typeId, blockType, rotation, filler, settings);
        }, false);
    }
}
