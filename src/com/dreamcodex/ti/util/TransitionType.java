package com.dreamcodex.ti.util;

public enum TransitionType {

    TOP_TO_BOTTOM("Top to Bottom", 1, 2, new int[] {0}, new int[] {1}),
    BOTTOM_TO_TOP("Bottom to top", 1, 2, new int[] {0}, new int[] {-1}),
    LEFT_TO_RIGHT("Left to right", 2, 1, new int[] {1}, new int[] {0}),
    RIGHT_TO_LEFT("Right to left", 2, 1, new int[] {-1}, new int[] {0}),
    TWO_DIMENSIONAL("Two dimensional", 2, 2, new int[] {1, 0, 1}, new int[] {0, 1, 1}),
    ISOMETRIC("Isometric", 3, 2, new int[] {1, 2, 0, 1, 2}, new int[] {0, 0, -1, -1, -1});

    private final String label;
    private int width;
    private int height;
    private final int[] xOffsets;
    private final int[] yOffsets;

    TransitionType(String label, int width, int height, int[] xOffsets, int[] yOffsets) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.xOffsets = xOffsets;
        this.yOffsets = yOffsets;
    }

    public String toString() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getXOffsets() {
        return xOffsets;
    }

    public int[] getYOffsets() {
        return yOffsets;
    }

    public int getXOffset() {
        return xOffsets[0];
    }

    public int getYOffset() {
        return yOffsets[0];
    }

    public int getSize() {
        return xOffsets.length;
    }

    public int getXStart(boolean wrap) {
        if (wrap) {
            return 0;
        }
        switch (this) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            case LEFT_TO_RIGHT:
            case TWO_DIMENSIONAL:
            case ISOMETRIC:
            default:
                return 0;
            case RIGHT_TO_LEFT:
                return 1;
        }
    }

    public int getXEnd(boolean wrap) {
        if (wrap) {
            return 0;
        }
        switch (this) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            case RIGHT_TO_LEFT:
            default:
                return 0;
            case LEFT_TO_RIGHT:
            case TWO_DIMENSIONAL:
                return 1;
            case ISOMETRIC:
                return 2;
        }
    }

    public int getYStart(boolean wrap) {
        if (wrap) {
            return 0;
        }
        switch (this) {
            case TOP_TO_BOTTOM:
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
            case TWO_DIMENSIONAL:
            default:
                return 0;
            case BOTTOM_TO_TOP:
            case ISOMETRIC:
                return 1;
        }
    }

    public int getYEnd(boolean wrap) {
        if (wrap) {
            return 0;
        }
        switch (this) {
            case BOTTOM_TO_TOP:
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
            case ISOMETRIC:
            default:
                return 0;
            case TOP_TO_BOTTOM:
            case TWO_DIMENSIONAL:
                return 1;
        }
    }

    public int getBaseX() {
        switch (this) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            case LEFT_TO_RIGHT:
            case TWO_DIMENSIONAL:
            case ISOMETRIC:
            default:
                return 0;
            case RIGHT_TO_LEFT:
                return 1;
        }
    }

    public int getBaseY() {
        switch (this) {
            case TOP_TO_BOTTOM:
            case LEFT_TO_RIGHT:
            case TWO_DIMENSIONAL:
            case RIGHT_TO_LEFT:
            default:
                return 0;
            case BOTTOM_TO_TOP:
            case ISOMETRIC:
                return 1;
        }
    }
}
