package com.dreamcodex.ti.util;

import java.awt.*;
import java.util.HashMap;

import static com.dreamcodex.ti.util.TIGlobals.COLOR_SETS;
import static com.dreamcodex.ti.util.TIGlobals.N_CHARS;

public class DataSet {

    private ColorMode colorMode = ColorMode.COLOR_MODE_GRAPHICS_1;
    private int[][] clrSets = new int[COLOR_SETS][2];

    private HashMap<Integer, int[][]> charGrids;
    private HashMap<Integer, int[][]> charColors;
    private HashMap<Integer, Image> charImages;

    private ECMPalette[] ecmPalettes = null;
    private ECMPalette[] ecmCharPalettes = null;
    private ECMPalette[] ecmSpritePalettes = null;
    private boolean[] ecmCharTransparency = new boolean[N_CHARS];

    private HashMap<Integer, int[][]> spriteGrids;
    private HashMap<Integer, Image> spriteImages;
    private int[] spriteColors = new int[TIGlobals.MAX_SPRITE + 1];

    public ColorMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;
    }

    public int[][] getClrSets() {
        return clrSets;
    }

    public void setClrSets(int[][] clrSets) {
        this.clrSets = clrSets;
    }

    public HashMap<Integer, int[][]> getCharGrids() {
        return charGrids;
    }

    public void setCharGrids(HashMap<Integer, int[][]> charGrids) {
        this.charGrids = charGrids;
    }

    public HashMap<Integer, int[][]> getCharColors() {
        return charColors;
    }

    public void setCharColors(HashMap<Integer, int[][]> charColors) {
        this.charColors = charColors;
    }

    public HashMap<Integer, Image> getCharImages() {
        return charImages;
    }

    public void setCharImages(HashMap<Integer, Image> charImages) {
        this.charImages = charImages;
    }

    public ECMPalette[] getEcmPalettes() {
        return ecmPalettes;
    }

    public void setEcmPalettes(ECMPalette[] ecmPalettes) {
        this.ecmPalettes = ecmPalettes;
    }

    public ECMPalette[] getEcmCharPalettes() {
        return ecmCharPalettes;
    }

    public void setEcmCharPalettes(ECMPalette[] ecmCharPalettes) {
        this.ecmCharPalettes = ecmCharPalettes;
    }

    public ECMPalette[] getEcmSpritePalettes() {
        return ecmSpritePalettes;
    }

    public void setEcmSpritePalettes(ECMPalette[] ecmSpritePalettes) {
        this.ecmSpritePalettes = ecmSpritePalettes;
    }

    public boolean[] getEcmCharTransparency() {
        return ecmCharTransparency;
    }

    public void setEcmCharTransparency(boolean[] ecmCharTransparency) {
        this.ecmCharTransparency = ecmCharTransparency;
    }

    public HashMap<Integer, int[][]> getSpriteGrids() {
        return spriteGrids;
    }

    public void setSpriteGrids(HashMap<Integer, int[][]> spriteGrids) {
        this.spriteGrids = spriteGrids;
    }

    public HashMap<Integer, Image> getSpriteImages() {
        return spriteImages;
    }

    public void setSpriteImages(HashMap<Integer, Image> spriteImages) {
        this.spriteImages = spriteImages;
    }

    public int[] getSpriteColors() {
        return spriteColors;
    }

    public void setSpriteColors(int[] spriteColors) {
        this.spriteColors = spriteColors;
    }
}
