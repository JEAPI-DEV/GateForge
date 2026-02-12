package com.logica.components.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.logica.vars.Orientation;

import java.util.List;

/**
 * Common interface for all logical components.
 */
public interface ILogicaComponent {
    Vector3i getPosition();

    boolean isActive();

    void updateOutput(World world, NetComp caller);

    void notifyNeighbors(World world);

    List<Vector3i> getInputs(World world);
    List<Vector3i> getOutputs(World world);

    boolean isProvidingPowerTo(Vector3i neighborPos);

    default boolean canAcceptInputFrom(Vector3i neighborPos, Orientation relativeDir) {
        return true;
    }

    default boolean canProvideOutputTo(Vector3i neighborPos, Orientation relativeDir) {
        return true;
    }

    default boolean canProvidePowerThroughBlock(Vector3i neighborPos) {
        return false;
    }

    default void onInteract(World world){}

    void onRecover(World world);

    void onPlace(World world);

    void onBreak(World world);

    void render(World world);
}
