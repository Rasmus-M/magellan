package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BinaryMapImporter extends Importer {

    private final boolean metatileMode;

    public BinaryMapImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
        metatileMode = dataSet.isMetatileMode();
    }

    /**
     * Reads one byte per map cell into the current map. In metatile mode the bytes are
     * metatile numbers, otherwise character numbers (undefined characters are skipped).
     */
    public void readBinaryMap(File binaryMapFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(binaryMapFile);
        for (int y = 0; y < mapEditor.getGridHeight(); y++) {
            for (int x = 0; x < mapEditor.getGridWidth(); x++) {
                int value = inputStream.read();
                if (value >= 0 && (metatileMode || charGrids.get(value) != null)) {
                    mapEditor.setGridAt(x, y, value);
                }
            }
        }
        inputStream.close();
    }
}
