package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;

import java.util.HashMap;

public abstract class Importer {

    protected MapEditor mapdMain;
    protected int[][] clrSets;
    protected ECMPalette[] ecmPalettes;
    protected HashMap<Integer, int[][]> hmCharGrids;
    protected HashMap<Integer, int[][]> hmCharColors;
    protected ECMPalette[] ecmCharPalettes;
    protected boolean[] ecmCharTransparency;
    protected HashMap<Integer, int[][]> hmSpriteGrids;
    protected int[] spriteColors;
    protected ECMPalette[] ecmSpritePalettes;
    protected int colorMode;

    public Importer() {
    }

    public Importer(
            MapEditor mapdMain,
            ECMPalette[] ecmPalettes, int[][] clrSets,
            HashMap<Integer, int[][]> hmCharGrids,
            HashMap<Integer, int[][]> hmCharColors,
            ECMPalette[] ecmCharPalettes,
            boolean[] ecmCharTransparency,
            HashMap<Integer, int[][]> hmSpriteGrids,
            int[] spriteColors,
            ECMPalette[] ecmSpritePalettes,
            int colorMode
    ) {
        this.mapdMain = mapdMain;
        this.clrSets = clrSets;
        this.hmCharGrids = hmCharGrids;
        this.hmCharColors = hmCharColors;
        this.ecmPalettes = ecmPalettes;
        this.ecmCharPalettes = ecmCharPalettes;
        this.ecmCharTransparency = ecmCharTransparency;
        this.hmSpriteGrids = hmSpriteGrids;
        this.spriteColors = spriteColors;
        this.ecmSpritePalettes = ecmSpritePalettes;
        this.colorMode = colorMode;
    }
}
