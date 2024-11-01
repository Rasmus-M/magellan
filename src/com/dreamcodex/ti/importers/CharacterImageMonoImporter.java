package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import java.awt.image.BufferedImage;

public class CharacterImageMonoImporter extends Importer {

    private Preferences preferences;

    public CharacterImageMonoImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
        this.preferences = preferences;
    }

    public void readCharImageMono(BufferedImage buffImg, boolean skipBlank) {
        // get character glyphs
        int rowOffset = 0;
        int colOffset = 0;
        for (int charNum = TIGlobals.MIN_CHAR; charNum <= preferences.getCharacterSetEnd(); charNum++) {
            int[][] newCharArray = new int[8][8];
            if (colOffset * 8 + 7 < buffImg.getWidth() && rowOffset * 8 + 7 < buffImg.getHeight()) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        newCharArray[y][x] = (buffImg.getRGB((colOffset * 8) + x, (rowOffset * 8) + y) != -1 ? 1 : 0);
                    }
                }
                if (!(skipBlank && Globals.isGridEmpty(newCharArray))) {
                    charGrids.put(charNum, newCharArray);
                }
            }
            colOffset++;
            if (colOffset >= 8) {
                colOffset = 0;
                rowOffset++;
            }
        }
    }
}
