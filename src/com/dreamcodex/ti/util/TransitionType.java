package com.dreamcodex.ti.util;

public enum TransitionType {

    BOTTOM_TO_TOP("Bottom to top"),
    LEFT_TO_RIGHT("Left to right"),
    TWO_DIMENSIONAL("Two dimensional"),
    ISOMETRIC("Isometric");

    private final String label;

    TransitionType(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }
}
