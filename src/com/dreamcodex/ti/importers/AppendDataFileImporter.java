package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class AppendDataFileImporter extends Importer {

    public AppendDataFileImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readAppendDataFile(File mapDataFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int mapY = 0;
        int mapX = 0;
        int mapCount = 1;
        int mapColor = 15;
        int mapWidth = 32;
        int mapHeight = 24;
        int currMap = mapEditor.getMapCount();
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_COLORSET)) {
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                }
                else if (lineIn.startsWith(Globals.KEY_SCRBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_SCRBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPCOUNT)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPCOUNT.length());
                    mapCount = Integer.parseInt(lineIn);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPSIZE)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPSIZE.length());
                    mapWidth = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    mapHeight = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                }
                else if (lineIn.equals(Globals.KEY_MAPSTART)) {
                    if (mapY > 0) {
                        mapEditor.addBlankMap(mapWidth, mapHeight);
                        currMap++;
                        mapEditor.setCurrentMapId(currMap);
                        mapY = 0;
                    }
                }
                else if (lineIn.equals(Globals.KEY_MAPEND)) {
                    mapEditor.setColorScreen(mapColor);
                    mapEditor.storeCurrentMap();
                }
                else if (lineIn.startsWith(Globals.KEY_MAPBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapEditor.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPDATA)) {
                    if (currMap == mapEditor.getMapCount()) {
                        mapEditor.storeCurrentMap();
                        mapEditor.addBlankMap(mapWidth, mapHeight);
                        mapEditor.setCurrentMapId(currMap);
                        mapY = 0;
                        mapEditor.setColorScreen(mapColor);
                    }
                    else if (mapY >= mapHeight) {
                        mapEditor.storeCurrentMap();
                        mapEditor.addBlankMap();
                        currMap++;
                        mapEditor.setCurrentMapId(currMap);
                        mapY = 0;
                        mapEditor.setColorScreen(mapColor);
                    }
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    StringTokenizer stParse = new StringTokenizer(lineIn, "|", false);
                    while (stParse.hasMoreTokens()) {
                        String sVal = stParse.nextToken();
                        mapEditor.setGridAt(mapX, mapY, Integer.parseInt(sVal));
                        mapX++;
                    }
                    mapX = 0;
                    mapY++;
                }
            }
        } while (lineIn != null);
        br.close();
        mapEditor.updateComponents();
    }
}
