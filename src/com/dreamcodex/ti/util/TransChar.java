package com.dreamcodex.ti.util;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 13-06-13
 * Time: 21:33
 */
public class TransChar {

    private int fromChar;
    private int toChar;
    private int index;
    private int count;
    private boolean colorsOK;
    private int foreColor;
    private int backColor;
    private int[][] colorGrid;
    private boolean invert;

    public TransChar(TransChar transChar) {
        this.fromChar = transChar.fromChar;
        this.toChar = transChar.toChar;
        this.index = transChar.index;
        this.count = transChar.count;
        this.colorsOK = transChar.colorsOK;
        this.foreColor = transChar.foreColor;
        this.backColor = transChar.backColor;
        this.colorGrid = transChar.colorGrid;
        this.invert = transChar.invert;
    }

    public TransChar(int fromChar, int toChar, boolean colorsOK) {
        this.fromChar = fromChar;
        this.toChar = toChar;
        this.colorsOK = colorsOK;
        count = 1;
    }

    public TransChar(int fromChar, int toChar, int index, boolean colorsOK, int foreColor, int backColor) {
        this(fromChar, toChar, colorsOK);
        this.index = index;
        this.foreColor = foreColor;
        this.backColor = backColor;
        count = 1;
    }

    public TransChar(int fromChar, int toChar, int index, boolean colorsOK, int[][] colorGrid) {
        this(fromChar, toChar, colorsOK);
        this.index = index;
        this.colorGrid = colorGrid;
        count = 1;
    }

    public int getFromChar() {
        return fromChar;
    }

    public void setFromChar(int fromChar) {
        this.fromChar = fromChar;
    }

    public int getToChar() {
        return toChar;
    }

    public void setToChar(int toChar) {
        this.toChar = toChar;
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

    public int getForeColor() {
        return foreColor;
    }

    public int getBackColor() {
        return backColor;
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
        return this == o || o instanceof TransChar && ((TransChar) o).getFromChar() == fromChar && ((TransChar) o).getToChar() == toChar;
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
