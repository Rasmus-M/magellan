package com.dreamcodex.ti.util;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

import static com.dreamcodex.ti.util.Globals.getECMSafeColor;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 02-11-13
 * Time: 22:06
 */
public class ECMPalette {

    private class ColorComparator implements Comparator<Color> {
        public int compare(Color o1, Color o2) {
            return o1.getRGB() - o2.getRGB();
        }
    }

    private int size;
    private Color[] palette;

    public ECMPalette(int size) {
        this(size, 0);
    }

    public ECMPalette(int size, int baseColorIndex) {
        this.size = size;
        palette = new Color[size];
        for (int i = 0; i < size; i++) {
            setColor(i, TIGlobals.TI_PALETTE_OPAQUE[i + baseColorIndex]);
        }
    }

    public int getSize() {
        return size;
    }

    public Color getColor(int index) {
        return palette[index];
    }

    public void setColor(int index, Color color) {
        palette[index] = color;
    }

    public Color[] getColors() {
        return palette;
    }

    public void setColors(Color[] palette) {
        this.palette = palette;
    }

    public int[] getIntColors() {
        int[] colors = new int[size];
        for (int i = 0; i < size; i++) {
            colors[i] = palette[i].getRGB();
        }
        return colors;
    }

    public void setIntColors(int[] colors) {
        for (int i = 0; i < size; i++) {
            palette[i] = new Color(colors[i]);
        }
    }

    public void setSafeColors(Integer[] colors) {
        for (int i = 0; i < size; i++) {
            if (i < colors.length && colors[i] != null) {
                setSafeColor(i, colors[i]);
            }
        }
        if (colors.length < size) {
            for (int i = colors.length; i < size; i++) {
                palette[i] = new Color(0, 0, 0);
            }
        }
    }

    public void setSafeColor(int index, int c) {
        Color color = new Color(c);
        palette[index] = getECMSafeColor(color);
    }

    public boolean startsWith(int[] colors, int n) {
        for (int i = 0; i < n; i++) {
            if (palette[i].getRGB() != colors[i]) {
               return false;
            }
        }
        return true;
    }

    public void sort() {
        Arrays.sort(palette, new ColorComparator());
    }

    public boolean contains(ECMPalette ecmPalette) {
        for (int i = 0; i < ecmPalette.size; i++) {
            if (!contains(ecmPalette.getColor(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Color color) {
        return getColorIndex(color) != -1;
    }

    public int getColorIndex(Color color) {
        for (int i = 0; i < size; i++) {
            if (color.equals(palette[i])) {
                return i;
            }
        }
        return -1;
    }

    public int getClosestColorIndex(Color color) {
        return Globals.getClosestColorIndex(color, palette);
    }
}
