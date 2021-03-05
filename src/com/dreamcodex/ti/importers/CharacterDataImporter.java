package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TIGlobals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class CharacterDataImporter extends Importer {

    public CharacterDataImporter(MapEditor mapdMain, ECMPalette[] ecmPalettes, int[][] clrSets, HashMap<Integer, int[][]> hmCharGrids, HashMap<Integer, int[][]> hmCharColors, ECMPalette[] ecmCharPalettes, boolean[] ecmCharTransparency, HashMap<Integer, int[][]> hmSpriteGrids, int[] spriteColors, ECMPalette[] ecmSpritePalettes, int colorMode) {
        super(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
    }

    public void readCharacterData(File mapDataFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int charStart = TIGlobals.BASIC_FIRST_CHAR;
        int charEnd = TIGlobals.BASIC_LAST_CHAR;
        int charRead = charStart;
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARDATA.length());
                    hmCharGrids.put(charRead, Globals.getIntGrid(lineIn, 8));
                    charRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARRANG)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARRANG.length());
                    charStart = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    charEnd = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    charRead = charStart;
                }
            }
        } while (lineIn != null);
        br.close();
    }
}
