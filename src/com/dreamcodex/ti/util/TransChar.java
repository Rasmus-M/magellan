package com.dreamcodex.ti.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import static java.lang.Math.floorMod;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 13-06-13
 * Time: 21:33
 */
public class TransChar {

    private int fromChar;
    private int[] toChars;
    private int index;
    private int count;
    private boolean colorsOK;
    private int foreColor;
    private int backColor;
    private int[][] colorGrid;
    private boolean invert;

    public TransChar(TransChar transChar) {
        this.fromChar = transChar.fromChar;
        this.toChars = transChar.toChars;
        this.index = transChar.index;
        this.count = transChar.count;
        this.colorsOK = transChar.colorsOK;
        this.foreColor = transChar.foreColor;
        this.backColor = transChar.backColor;
        this.colorGrid = transChar.colorGrid;
        this.invert = transChar.invert;
    }

    public TransChar(TransitionType transitionType, int x, int y, int[][] mapData) {
        int height = mapData.length;
        int width = mapData[0].length;
        switch (transitionType) {
            case TOP_TO_BOTTOM:
            case BOTTOM_TO_TOP:
            case LEFT_TO_RIGHT:
            case RIGHT_TO_LEFT:
                this.fromChar = mapData[y][x];
                this.toChars = new int[] {
                    mapData[floorMod(y + transitionType.getyOffset(), height)][floorMod(x + transitionType.getxOffset(), width)]
                };
                break;
            case TWO_DIMENSIONAL: {
                this.fromChar = mapData[y][x];
                this.toChars = new int[] {
                    mapData[y][floorMod(x + 1, width)],
                    mapData[floorMod(y - 1, height)][x],
                    mapData[floorMod(y - 1, height)][floorMod(x + 1, width)]
                };
                break;
            }
            case ISOMETRIC: {
                this.fromChar = mapData[y][x];
                this.toChars = new int[] {
                    mapData[y][floorMod(x + 1, width)],
                    mapData[y][floorMod(x + 2, width)],
                    mapData[floorMod(y - 1, height)][x],
                    mapData[floorMod(y - 1, height)][floorMod(x + 1, width)],
                    mapData[floorMod(y - 1, height)][floorMod(x + 2, width)]
                };
                break;
            }
        }
    }

    public TransChar(int fromChar, int toChar, boolean colorsOK) {
        this(fromChar, new int[] {toChar}, colorsOK);
    }

    public TransChar(int fromChar, int[] toChars, boolean colorsOK) {
        this.fromChar = fromChar;
        this.toChars = toChars;
        this.colorsOK = colorsOK;
        count = 1;
    }

    public TransChar(int fromChar, int toChar, int index, boolean colorsOK, int[][] colorGrid) {
        this(fromChar, toChar, colorsOK);
        this.index = index;
        this.colorGrid = colorGrid;
    }

    public String getKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(fromChar);
        for (int toChar : toChars) {
            sb.append("-").append(toChar);
        }
        return sb.toString();
    }

    public int getFromChar() {
        return fromChar;
    }

    public void setFromChar(int fromChar) {
        this.fromChar = fromChar;
    }

    public int[] getToChars() {
        return toChars;
    }

    public void setToChars(int[] toChars) {
        this.toChars = toChars;
    }

    public int getToChar() {
        return toChars[0];
    }

    public void setToChar(int toChar) {
        this.toChars[0] = toChar;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getCount() {
        return count;
    }

    public boolean isColorsOK() {
        return colorsOK;
    }

    public void setColorsOK(boolean colorsOK) {
        this.colorsOK = colorsOK;
    }

    public int getForeColor() {
        return foreColor;
    }

    public void setForeColor(int foreColor) {
        this.foreColor = foreColor;
    }

    public int getBackColor() {
        return backColor;
    }

    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    public int[][] getColorGrid() {
        return colorGrid;
    }

    public void setColorGrid(int[][] colorGrid) {
        this.colorGrid = colorGrid;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public void incCount() {
        count++;
    }

    public boolean equals(Object o) {
        return this == o ||
            o instanceof TransChar &&
            ((TransChar) o).getFromChar() == fromChar &&
            Arrays.equals(((TransChar) o).getToChars(), toChars);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(">" + Globals.toHexString(this.getFromChar(), 2));
        for (int toChar : toChars) {
            sb.append(",>").append(Globals.toHexString(toChar, 2));
        }
        return sb.toString();
    }

    public static class TransCharCountComparator implements Comparator<TransChar> {
        public int compare(TransChar tc1, TransChar tc2) {
            return tc2.getCount() - tc1.getCount();
        }
    }

    public static class TransCharFromComparator implements Comparator<TransChar> {
        public int compare(TransChar tc1, TransChar tc2) {
            return tc2.getFromChar() - tc1.getFromChar();
        }
    }

    public static class TransCharToComparator implements Comparator<TransChar> {
        public int compare(TransChar tc1, TransChar tc2) {
            return tc2.getToChar() - tc1.getToChar();
        }
    }
}
