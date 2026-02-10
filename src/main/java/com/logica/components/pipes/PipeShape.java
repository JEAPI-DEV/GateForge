package com.logica.components.pipes;

public enum PipeShape {
    STRAIGHT("Straight"),
    CORNER("Corner"),
    THREE_WAY("ThreeWay"),
    FOUR_WAY("FourWay");

    private final String name;

    PipeShape(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
