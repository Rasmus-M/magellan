package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.TIGlobals;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public class CharacterImageMonoImporter extends Importer {

    public CharacterImageMonoImporter(MapEditor mapdMain, ECMPalette[] ecmPalettes, int[][] clrSets, HashMap<Integer, int[][]> hmCharGrids, HashMap<Integer, int[][]> hmCharColors, ECMPalette[] ecmCharPalettes, boolean[] ecmCharTransparency, HashMap<Integer, int[][]> hmSpriteGrids, int[] spriteColors, ECMPalette[] ecmSpritePalettes, int colorMode) {
        super(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
    }

    public void readCharImageMono(BufferedImage buffImg) {
        // get character glyphs
        int rowOffset = 0;
        int colOffset = 0;
        for (int charNum = TIGlobals.MIN_CHAR; charNum <= TIGlobals.MAX_CHAR; charNum++) {
            int[][] newCharArray = new int[8][8];
            if (hmCharGrids.containsKey(charNum)) {
                hmCharGrids.remove(charNum);
            }
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    newCharArray[y][x] = (buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y) != -1 ? 1 : 0);
                }
            }
            hmCharGrids.put(charNum, newCharArray);
            colOffset++;
            if (colOffset >= 8) {
                colOffset = 0;
                rowOffset++;
            }
        }
    }
}
