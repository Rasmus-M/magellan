package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.util.HashMap;

public abstract class Importer {

    protected MapEditor mapEditor;
    protected int[][] clrSets;
    protected ECMPalette[] ecmPalettes;
    protected HashMap<Integer, int[][]> charGrids;
    protected HashMap<Integer, int[][]> charColors;
    protected ECMPalette[] ecmCharPalettes;
    protected boolean[] ecmCharTransparency;
    protected HashMap<Integer, int[][]> spriteGrids;
    protected int[] spriteColors;
    protected ECMPalette[] ecmSpritePalettes;
    protected ColorMode colorMode;

    public Importer() {
    }

    public Importer(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        this(
            mapEditor,
            dataSet.getEcmPalettes(),
            dataSet.getClrSets(),
            dataSet.getCharGrids(),
            dataSet.getCharColors(),
            dataSet.getEcmCharPalettes(),
            dataSet.getEcmCharTransparency(),
            dataSet.getSpriteGrids(),
            dataSet.getSpriteColors(),
            dataSet.getEcmSpritePalettes(),
            preferences.getColorMode()
        );
    }

    public Importer(
            MapEditor mapEditor,
            ECMPalette[] ecmPalettes, int[][] clrSets,
            HashMap<Integer, int[][]> charGrids,
            HashMap<Integer, int[][]> charColors,
            ECMPalette[] ecmCharPalettes,
            boolean[] ecmCharTransparency,
            HashMap<Integer, int[][]> spriteGrids,
            int[] spriteColors,
            ECMPalette[] ecmSpritePalettes,
            ColorMode colorMode
    ) {
        this.mapEditor = mapEditor;
        this.clrSets = clrSets;
        this.charGrids = charGrids;
        this.charColors = charColors;
        this.ecmPalettes = ecmPalettes;
        this.ecmCharPalettes = ecmCharPalettes;
        this.ecmCharTransparency = ecmCharTransparency;
        this.spriteGrids = spriteGrids;
        this.spriteColors = spriteColors;
        this.ecmSpritePalettes = ecmSpritePalettes;
        this.colorMode = colorMode;
    }
}
