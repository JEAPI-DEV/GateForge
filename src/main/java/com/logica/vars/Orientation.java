package com.logica.vars;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.List;

/**
 * Utility for handling directions and rotations in the Logica mod.
 * Unifies directional logic to prevent duplication and errors.
 */
public enum Orientation {
    NORTH(0, 0, -1, 0),
    EAST(1, 0, 0, 1),
    SOUTH(0, 0, 1, 2),
    WEST(-1, 0, 0, 3),
    UP(0, 1, 0, 4),
    DOWN(0, -1, 0, 5);

    private final int dx, dy, dz;
    private final int rotationIndex;

    /**
     * Maps engine rotation indices (0..3) to logical quarter-turn steps.
     * Adjust this mapping if in-game rotation order differs.
     */
    private static final int[] ROTATION_STEPS_BY_INDEX = {0, 3, 2, 1};

    Orientation(int dx, int dy, int dz, int rotationIndex) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rotationIndex = rotationIndex;
    }

    public Vector3i getDirection() {
        return new Vector3i(dx, dy, dz);
    }

    public static Orientation fromRotation(int index) {
        for (Orientation o : values())
            if (o.rotationIndex == index) return o;
        return NORTH;
    }

    /**
     * Converts an engine rotation index into a world-facing orientation.
     */
    public static Orientation fromRotationIndex(int index) {
        int steps = rotationStepsForIndex(index);
        return NORTH.rotateY(steps);
    }

    public static Orientation fromDelta(int dx, int dy, int dz) {
        for (Orientation o : values())
            if (o.dx == dx && o.dy == dy && o.dz == dz) return o;
        return null;
    }

    public static Orientation fromDirection(Vector3i dir) {
        return fromDelta(dir.x, dir.y, dir.z);
    }

    public Orientation getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    public Orientation rotateY(int amount) {
        if (this == UP || this == DOWN)
            return this;
        int newIndex = (this.rotationIndex + amount) % 4;
        if (newIndex < 0)
            newIndex += 4;
        return fromRotation(newIndex);
    }

    /**
     * Converts a local orientation to world orientation using the given rotation index.
     * Rotation index follows the same convention as {@link #fromRotation(int)}.
     */
    public static Orientation toWorld(Orientation local, int rotationIndex) {
        if (local == null)
            return null;
        int steps = rotationStepsForIndex(rotationIndex);
        return local.rotateY(steps);
    }

    private static int rotationStepsForIndex(int index) {
        int normalized = Math.floorMod(index, ROTATION_STEPS_BY_INDEX.length);
        return ROTATION_STEPS_BY_INDEX[normalized];
    }

    /**
     * Converts logical rotation steps (0..3) into engine rotation indices.
     */
    public static int toEngineRotationIndex(int steps) {
        int normalized = Math.floorMod(steps, ROTATION_STEPS_BY_INDEX.length);
        for (int i = 0; i < ROTATION_STEPS_BY_INDEX.length; i++) {
            if (ROTATION_STEPS_BY_INDEX[i] == normalized) {
                return i;
            }
        }
        return 0;
    }

    public Orientation getLeft() {
        return rotateY(-1);
    }

    public Orientation getRight() {
        return rotateY(1);
    }

    public Orientation getBack() {
        return rotateY(2);
    }

    public static final List<Orientation> ALL = List.of(NORTH, SOUTH, EAST, WEST, UP, DOWN);
}
