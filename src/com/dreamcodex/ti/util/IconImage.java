package com.dreamcodex.ti.util;

import java.awt.*;

public class IconImage {

    private final Image image;
    private final boolean empty;

    public IconImage(Image image, boolean empty) {
        this.image = image;
        this.empty = empty;
    }

    public Image getImage() {
        return image;
    }

    public boolean isEmpty() {
        return empty;
    }
}
