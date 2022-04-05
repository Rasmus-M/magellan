package com.dreamcodex.ti.util;

public enum ColorMode {
    COLOR_MODE_GRAPHICS_1("Graphics 1 Color Mode"),
    COLOR_MODE_BITMAP("Bitmap Color Mode"),
    COLOR_MODE_ECM_2("Enhanced Color Mode - 2 bpp"),
    COLOR_MODE_ECM_3("Enhanced Color Mode - 3 bpp");

    private String label;

    private ColorMode(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }
}
