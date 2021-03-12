package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class IsometricFileExporter extends Exporter {

    public IsometricFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeIsometricFile(File mapDataFile) throws Exception {
        mapEditor.storeCurrentMap();
        FileWriter writer = null;
        try {
            writer = new FileWriter(mapDataFile);
            int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
            int[][] transMapData = new int[mapData.length - 1][mapData[0].length - 2];
            Map<String, Integer> transCharLookup = new HashMap<String, Integer>();
            int[][][] transCharGrids = new int[256][16][24];
            int iMax = 0;
            for (int y = 0; y < mapData.length - 1; y++) {
                for (int x = 0; x < mapData[y].length - 2; x++) {
                    String key =
                            mapData[y][x] + "-" + mapData[y][x + 1] + "-" + mapData[y][x + 2] + "-" +
                                    mapData[y + 1][x] + "-" + mapData[y + 1][x + 1] + "-" + mapData[y + 1][x + 2];
                    Integer i = transCharLookup.get(key);
                    if (i == null) {
                        // Found a new transition
                        i = iMax++;
                        transCharLookup.put(key, i);
                        Globals.copyGrid(charGrids.get(mapData[y][x]), transCharGrids[i], 0, 0);
                        Globals.copyGrid(charGrids.get(mapData[y][x + 1]), transCharGrids[i], 8, 0);
                        Globals.copyGrid(charGrids.get(mapData[y][x + 2]), transCharGrids[i], 16, 0);
                        Globals.copyGrid(charGrids.get(mapData[y + 1][x]), transCharGrids[i], 0, 8);
                        Globals.copyGrid(charGrids.get(mapData[y + 1][x + 1]), transCharGrids[i], 8, 8);
                        Globals.copyGrid(charGrids.get(mapData[y + 1][x + 2]), transCharGrids[i], 16, 8);
                        // Globals.printGrid(transCharGrids[i]);
                    }
                    transMapData[y][x] = i;
                }
            }
            for (int f = 0; f < 8; f++) {
                for (int i = 0; i < iMax; i++) {
                    int[][] scrollGrid = new int[8][8];
                    int[][] transCharGrid = transCharGrids[i];
                    for (int y = 8 - f, y1 = 0; y < 16 - f; y++, y1++) {
                        System.arraycopy(transCharGrid[y], 2 * f, scrollGrid[y1], 0, 8);
                    }
                    // System.out.println("Char " + i + " frame " + f);
                    // Globals.printGrid(scrollGrid);
                    writer.write(i == 0 ? "PATFR" + f : "      ");
                    writer.write(" DATA ");
                    String hexString = Globals.getHexString(scrollGrid);
                    writer.write(">" + hexString.substring(0, 4) + ",");
                    writer.write(">" + hexString.substring(4, 8) + ",");
                    writer.write(">" + hexString.substring(8, 12) + ",");
                    writer.write(">" + hexString.substring(12, 16) + "\n");
                }
            }
            for (int yStart = transMapData.length - 40; yStart < transMapData.length; yStart++) {
                int y = yStart;
                for (int x = 0; x < transMapData[0].length; x+= 2) {
                    if (x % 8 == 0) {
                        writer.write(x == 0 && y == transMapData.length - 40 ? "MAP    " : "       ");
                        writer.write("DATA ");
                    }
                    // System.out.println("(x,y)=(" + x + "," + y + ")");
                    writer.write(">" + Globals.toHexString(transMapData[y][x], 2) + Globals.toHexString(transMapData[y][x + 1], 2));
                    writer.write(x % 8 < 6 ? "," : "\n");
                    y--;
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
