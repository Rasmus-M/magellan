package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import java.awt.image.BufferedImage;

public class CharacterImageMonoImporter extends Importer {

    private final Preferences preferences;

    public CharacterImageMonoImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
        this.preferences = preferences;
    }

    public void readCharImageMono(BufferedImage buffImg, int startIndex, int endIndex, int gap, boolean skipBlank) {
        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        int size = 8 + gap;
        int cols = (width + gap) / size;
        int rows = (height + gap) / size;
        int col = 0;
        int row = 0;
        for (int charNum = startIndex; charNum <= endIndex; charNum++) {
            int[][] newCharArray = new int[8][8];
            if (col < cols && row < rows) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        newCharArray[y][x] = (buffImg.getRGB((col * size) + x, (row * size) + y) != -1 ? 1 : 0);
                    }
                }
                if (!(skipBlank && Globals.isGridEmpty(newCharArray))) {
                    charGrids.put(charNum, newCharArray);
                }
            }
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }
}
