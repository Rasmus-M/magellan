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

    public int getXStart(boolean wrap) {
        switch (this) {
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
            case TWO_DIMENSIONAL:
                return wrap ? 0 : 1;
            case ISOMETRIC:
                return wrap ? 0 : 2;
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            default:
                return 0;
        }
    }

    public int getYStart(boolean wrap) {
        switch (this) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            case TWO_DIMENSIONAL:
            case ISOMETRIC:
                return wrap ? 0 : 1;
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
            default:
                return 0;
        }
    }

}
