package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.MetatileConverter;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Appends the maps of a project file to the current project. Character and sprite definitions
 * are not read, and the map numbers are taken as they are when the file uses the same metatile
 * mode as the project. Otherwise the maps are converted through characters: metatiles in the file
 * are expanded with the file's metatile definitions, and in metatile mode the characters are
 * converted to the project's metatiles, adding new metatiles where needed.
 */
public class AppendDataFileImporter extends Importer {

    private final DataSet dataSet;

    public AppendDataFileImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
        this.dataSet = dataSet;
    }

    public void readAppendDataFile(File mapDataFile) throws IOException {
        List<int[][]> maps = new ArrayList<>();
        List<Integer> mapColors = new ArrayList<>();
        int fileMetatileSize = 0;
        HashMap<Integer, int[][]> fileMetatiles = new HashMap<>();
        int metatileRead = TIGlobals.MIN_METATILE;
        int[][] map = null;
        int mapY = 0;
        int mapColor = 15;
        int mapWidth = 32;
        int mapHeight = 24;
        try (BufferedReader br = new BufferedReader(new FileReader(mapDataFile))) {
            String lineIn;
            while ((lineIn = br.readLine()) != null) {
                if (lineIn.startsWith(Globals.KEY_METATILE_SIZE)) {
                    fileMetatileSize = DataFileImporter.parseMetatileSize(lineIn.substring(Globals.KEY_METATILE_SIZE.length()));
                }
                else if (lineIn.startsWith(Globals.KEY_METATILE)) {
                    if (fileMetatileSize != 0 && metatileRead <= TIGlobals.MAX_METATILE) {
                        fileMetatiles.put(metatileRead++, DataFileImporter.parseMetatile(lineIn.substring(Globals.KEY_METATILE.length()), fileMetatileSize));
                    }
                }
                else if (lineIn.startsWith(Globals.KEY_SCRBACK)) {
                    mapColor = Integer.parseInt(lineIn.substring(Globals.KEY_SCRBACK.length()));
                }
                else if (lineIn.startsWith(Globals.KEY_MAPSIZE)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPSIZE.length());
                    mapWidth = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    mapHeight = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                }
                else if (lineIn.equals(Globals.KEY_MAPSTART) || lineIn.equals(Globals.KEY_MAPEND)) {
                    map = null;
                }
                else if (lineIn.startsWith(Globals.KEY_MAPBACK)) {
                    mapColor = Integer.parseInt(lineIn.substring(Globals.KEY_MAPBACK.length()));
                }
                else if (lineIn.startsWith(Globals.KEY_MAPDATA)) {
                    // A new map starts at its first row, or when the previous map is full (older files)
                    if (map == null || mapY >= mapHeight) {
                        map = new int[mapHeight][mapWidth];
                        maps.add(map);
                        mapColors.add(mapColor);
                        mapY = 0;
                    }
                    StringTokenizer stParse = new StringTokenizer(lineIn.substring(Globals.KEY_MAPDATA.length()), "|", false);
                    for (int x = 0; x < mapWidth && stParse.hasMoreTokens(); x++) {
                        map[mapY][x] = Integer.parseInt(stParse.nextToken());
                    }
                    mapY++;
                }
            }
        }
        List<int[][]> newMaps = convertMaps(maps, fileMetatileSize, fileMetatiles);
        for (int i = 0; i < newMaps.size(); i++) {
            mapEditor.addMap(newMaps.get(i), mapColors.get(i), new HashMap<>());
        }
        mapEditor.updateComponents();
    }

    /**
     * Converts the maps from the file's metatile mode to the project's.
     */
    private List<int[][]> convertMaps(List<int[][]> maps, int fileMetatileSize, HashMap<Integer, int[][]> fileMetatiles) throws IOException {
        int metatileSize = dataSet.getMetatileSize();
        if (fileMetatileSize == metatileSize) {
            // Same mode: the numbers are taken as they are
            return maps;
        }
        // Through characters
        if (fileMetatileSize != 0) {
            for (int i = TIGlobals.MIN_METATILE; i <= TIGlobals.MAX_METATILE; i++) {
                if (!fileMetatiles.containsKey(i)) {
                    fileMetatiles.put(i, MetatileConverter.createBlankMetatile(fileMetatileSize, TIGlobals.SPACECHAR));
                }
            }
        }
        List<int[][]> charMaps = new ArrayList<>();
        for (int[][] map : maps) {
            charMaps.add(MetatileConverter.toCharacterMap(map, fileMetatileSize, fileMetatiles));
        }
        if (metatileSize == 0) {
            return charMaps;
        }
        // Metatiles used by the project's maps keep their definitions
        HashSet<Integer> usedMetatiles = new HashSet<>();
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            for (int[] row : mapEditor.getMapData(m)) {
                for (int metatileNum : row) {
                    usedMetatiles.add(metatileNum);
                }
            }
        }
        MetatileConverter.Result result = MetatileConverter.compress(charMaps, metatileSize, dataSet.getMetatiles(), TIGlobals.N_METATILES, TIGlobals.SPACECHAR, usedMetatiles);
        if (result == null) {
            throw new IOException("The appended maps need more than " + TIGlobals.N_METATILES + " different " + metatileSize + "x" + metatileSize + " metatiles.");
        }
        dataSet.setMetatiles(metatileSize, result.getMetatiles());
        return result.getMaps();
    }
}
