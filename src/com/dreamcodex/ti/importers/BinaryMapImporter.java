package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BinaryMapImporter extends Importer {

    public BinaryMapImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readBinaryMap(File binaryMapFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(binaryMapFile);
        for (int y = 0; y < mapEditor.getGridHeight(); y++) {
            for (int x = 0; x < mapEditor.getGridWidth(); x++) {
                int ch = inputStream.read();
                if (ch >= 0 && charGrids.get(ch) != null) {
                    mapEditor.setGridAt(x, y, ch);
                }
            }
        }
        inputStream.close();
    }
}
