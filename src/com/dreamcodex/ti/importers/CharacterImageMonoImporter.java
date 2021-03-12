package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import java.awt.image.BufferedImage;

public class CharacterImageMonoImporter extends Importer {

    public CharacterImageMonoImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readCharImageMono(BufferedImage buffImg) {
        // get character glyphs
        int rowOffset = 0;
        int colOffset = 0;
        for (int charNum = TIGlobals.MIN_CHAR; charNum <= TIGlobals.MAX_CHAR; charNum++) {
            int[][] newCharArray = new int[8][8];
            if (charGrids.containsKey(charNum)) {
                charGrids.remove(charNum);
            }
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    newCharArray[y][x] = (buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y) != -1 ? 1 : 0);
                }
            }
            charGrids.put(charNum, newCharArray);
            colOffset++;
            if (colOffset >= 8) {
                colOffset = 0;
                rowOffset++;
            }
        }
    }
}
