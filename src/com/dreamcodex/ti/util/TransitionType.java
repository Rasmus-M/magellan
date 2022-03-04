package com.dreamcodex.ti.util;

public enum TransitionType {

    TOP_TO_BOTTOM("Top to Bottom", 0, 1),
    BOTTOM_TO_TOP("Bottom to top", 0, -1),
    LEFT_TO_RIGHT("Left to right", 1, 0),
    RIGHT_TO_LEFT("Right to left", -1, 0),
    TWO_DIMENSIONAL("Two dimensional", 1, -1),
    ISOMETRIC("Isometric", 0, 0);

    private final String label;
    private final int xOffset;
    private final int yOffset;

    TransitionType(String label, int xOffset, int yOffset) {
        this.label = label;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public String toString() {
        return label;
    }

    public int getxOffset() {
        return xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }
}
