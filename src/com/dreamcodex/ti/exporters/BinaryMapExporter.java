package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class BinaryMapExporter extends Exporter {

    public BinaryMapExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeBinaryMap(File mapDataFile) throws IOException {
        // store working map first
        mapdMain.storeCurrentMap();
        // get file output buffer
        FileOutputStream fos = new FileOutputStream(mapDataFile);
        // write map
        int[][] mapToSave = mapdMain.getMapData(mapdMain.getCurrentMapId());
        boolean has16BitValues = false;
        for (int[] mapRow : mapToSave) {
            for (int mapChar : mapRow) {
                if (mapChar > 255) {
                    has16BitValues = true;
                }
            }
        }
        for (int[] mapRow : mapToSave) {
            for (int mapChar : mapRow) {
                if (has16BitValues) {
                    fos.write((mapChar & 0xff00) >>> 8);
                    fos.write(mapChar & 0xff);
                } else {
                    fos.write(mapChar);
                }
            }
        }
        fos.flush();
        fos.close();
    }
}
