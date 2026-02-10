package com.logica.components.pipes;

public class ShapeResult {
    private String state;
    private final PipeShape shape;
    private final int rotation;

    // Connectivity flags
    public boolean n, s, e, w, u, d;

    public ShapeResult(PipeShape shape, int rotation) {
        this.shape = shape;
        this.state = shape.toString();
        this.rotation = rotation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public PipeShape getShape() {
        return shape;
    }

    public int getRotation() {
        return rotation;
    }
}
