package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import static com.dreamcodex.ti.util.ColorMode.*;

public class DataFileImporter extends Importer {

    public DataFileImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public int readDataFile(File mapDataFile) throws IOException {
        charGrids.clear();
        BufferedReader br = new BufferedReader(new FileReader(mapDataFile));
        String lineIn = "";
        int mapY = 0;
        int mapX = 0;
        int mapCount = 1;
        int mapColor = 15;
        int mapWidth = 32;
        int mapHeight = 24;
        int charStart = TIGlobals.BASIC_FIRST_CHAR;
        int charEnd = TIGlobals.BASIC_LAST_CHAR;
        int charRead = charStart;
        int charRead1 = charStart;
        int charRead2 = charStart;
        int charColorRead = charStart;
        int cset = (int) (Math.floor(charStart / 8));
        int palNo = 0;
        int charPalNo = charStart;
        int charTransNo = charStart;
        int currMap = 0;
        int spriteRead = TIGlobals.MIN_SPRITE;
        int spriteRead1 = TIGlobals.MIN_SPRITE;
        int spriteRead2 = TIGlobals.MIN_SPRITE;
        int spritePalNo = 0;
        int spriteColNo = 0;
        int spriteCoordinateScale = 8;
        do {
            lineIn = br.readLine();
            if (lineIn == null) {
                break;
            }
            else {
                if (lineIn.startsWith(Globals.KEY_COLORSET)) {
                    lineIn = lineIn.substring(Globals.KEY_COLORSET.length());
                    clrSets[cset][Globals.INDEX_CLR_FORE] = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    clrSets[cset][Globals.INDEX_CLR_BACK] = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    cset++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA.length()), 16);
                    charGrids.put(charRead, Globals.getIntGrid(lineIn, 8));
                    charRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA1) && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA1.length()), 16);
                    int[][] charGrid = charGrids.get(charRead1++);
                    int[][] charGrid1 = Globals.getIntGrid(lineIn, 8);
                    Globals.orGrid(charGrid1, charGrid, 1);
                }
                else if (lineIn.startsWith(Globals.KEY_CHARDATA2) && (colorMode == COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_CHARDATA2.length()), 16);
                    int[][] charGrid = charGrids.get(charRead2++);
                    int[][] charGrid2 = Globals.getIntGrid(lineIn, 8);
                    Globals.orGrid(charGrid2, charGrid, 2);
                }
                else if (lineIn.startsWith(Globals.KEY_CHARCOLOR) && colorMode == COLOR_MODE_BITMAP) {
                    lineIn = lineIn.substring(Globals.KEY_CHARCOLOR.length());
                    if (lineIn.length() > 16) {
                        lineIn = lineIn.substring(0, 16);
                    }
                    else if (lineIn.length() < 16) {
                        lineIn = lineIn + "1010101010101010";
                        lineIn = lineIn.substring(0, 16);
                    }
                    int[][] charColors = new int[8][2];
                    for (int y = 0; y < 8; y++) {
                        charColors[y][1] = Integer.parseInt(Character.toString(lineIn.charAt(y * 2)), 16);
                        charColors[y][0] = Integer.parseInt(Character.toString(lineIn.charAt(y * 2 + 1)), 16);
                    }
                    this.charColors.put(charColorRead, charColors);
                    charColorRead++;
                }
                else if (lineIn.startsWith(Globals.KEY_PALETTE) && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    lineIn = lineIn.substring(Globals.KEY_PALETTE.length());
                    ECMPalette ecmPalette = ecmPalettes[palNo++];
                    String[] hexCols = lineIn.split("\\|");
                    for (int i = 0; i < Math.min(hexCols.length, ecmPalette.getSize()); i++) {
                        Color color;
                        try {
                            color = new Color(Integer.parseInt(hexCols[i].substring(2), 16));
                        } catch (NumberFormatException e) {
                            color = TIGlobals.TI_COLOR_TRANSOPAQUE;
                        }
                        ecmPalette.setColor(i, color);
                    }
                }
                else if (lineIn.startsWith(Globals.KEY_CHARPALS) && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    ECMPalette ecmPalette;
                    try {
                        ecmPalette = ecmPalettes[Integer.parseInt(lineIn.substring(Globals.KEY_CHARPALS.length()))];
                    } catch (NumberFormatException e) {
                        ecmPalette = ecmPalettes[0];
                    }
                    ecmCharPalettes[charPalNo++] = ecmPalette;
                }
                else if (lineIn.startsWith(Globals.KEY_CHARTRANS) && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    ecmCharTransparency[charTransNo++] = "1".equals(lineIn.substring(Globals.KEY_CHARTRANS.length()));
                }
                else if (lineIn.startsWith(Globals.KEY_CHARRANG)) {
                    lineIn = lineIn.substring(Globals.KEY_CHARRANG.length());
                    charStart = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    charEnd = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    charRead = charStart;
                    charRead1 = charStart;
                    charRead2 = charStart;
                    charColorRead = charStart;
                    cset = (int) (Math.floor(charStart / 8));
                    charPalNo = charStart;
                    charTransNo = charStart;
                }
                else if (lineIn.startsWith(Globals.KEY_SCRBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_SCRBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapEditor.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPCOUNT)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPCOUNT.length());
                    mapCount = Integer.parseInt(lineIn);
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
                else if (lineIn.startsWith(Globals.KEY_MAPSIZE)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPSIZE.length());
                    mapWidth = Integer.parseInt(lineIn.substring(0, lineIn.indexOf("|")));
                    mapHeight = Integer.parseInt(lineIn.substring(lineIn.indexOf("|") + 1));
                    mapEditor.setGridWidth(mapWidth);
                    mapEditor.setGridHeight(mapHeight);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPBACK)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPBACK.length());
                    mapColor = Integer.parseInt(lineIn);
                    mapEditor.setColorScreen(mapColor);
                }
                else if (lineIn.startsWith(Globals.KEY_MAPDATA)) {
                    if (mapY >= mapHeight) {
                        mapEditor.setColorScreen(mapColor);
                        mapEditor.storeCurrentMap();
                        mapEditor.addBlankMap(mapWidth, mapHeight);
                        currMap++;
                        mapEditor.setCurrentMapId(currMap);
                        mapY = 0;
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
                else if (lineIn.startsWith(Globals.KEY_SPRITE_LOCATION_PIXELS)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    spriteCoordinateScale = Integer.parseInt(lineIn) == 1 ? 1 : 8;
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_LOCATION)) {
                    lineIn = lineIn.substring(Globals.KEY_MAPDATA.length());
                    String[] lineParts = lineIn.split("\\|");
                    if (lineParts.length == 3) {
                        HashPoint p = new HashPoint(spriteCoordinateScale * Integer.parseInt(lineParts[0]), spriteCoordinateScale * Integer.parseInt(lineParts[1]));
                        int spriteNum = Integer.parseInt(lineParts[2]);
                        HashMap<Point, ArrayList<Integer>> spriteMap = mapEditor.getSpriteMap();
                        ArrayList<Integer> spriteList = spriteMap.get(p);
                        if (spriteList == null) {
                            spriteList = new ArrayList<Integer>();
                            spriteMap.put(p, spriteList);
                        }
                        spriteList.add(spriteNum);
                    }
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN.length()), 64);
                    spriteGrids.put(spriteRead++, Globals.getIntGrid(lineIn, 16));
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN1) && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN1.length()), 64);
                    int[][] spriteGrid = spriteGrids.get(spriteRead1++);
                    int[][] spriteGrid1 = Globals.getIntGrid(lineIn, 16);
                    Globals.orGrid(spriteGrid1, spriteGrid, 1);
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_PATTERN2) && (colorMode == COLOR_MODE_ECM_3)) {
                    lineIn = Globals.trimHex(lineIn.substring(Globals.KEY_SPRITE_PATTERN2.length()), 64);
                    int[][] spriteGrid = spriteGrids.get(spriteRead2++);
                    int[][] spriteGrid2 = Globals.getIntGrid(lineIn, 16);
                    Globals.orGrid(spriteGrid2, spriteGrid, 2);
                }
                else if (lineIn.startsWith(Globals.KEY_SPRITE_COLOR)) {
                    int colorIndex;
                    try {
                        colorIndex = Integer.parseInt(lineIn.substring(Globals.KEY_SPRITE_COLOR.length()));
                    } catch (NumberFormatException e) {
                        colorIndex = 0;
                    }
                    if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                        ecmSpritePalettes[spritePalNo++] = ecmPalettes[colorIndex];
                    }
                    else {
                        spriteColors[spriteColNo++] = colorIndex;
                    }
                }
            }
        } while (lineIn != null);
        br.close();
        if (colorMode == COLOR_MODE_BITMAP && charColorRead == charStart) {
            // Bitmap color mode but no bitmap colors found - use color sets
            for (int i = charStart; i <= charEnd; i++) {
                if (charGrids.get(i) != null) {
                    int[][] colorGrid = charColors.get(i);
                    if (colorGrid == null) {
                        colorGrid = new int[8][2];
                        charColors.put(i, colorGrid);
                    }
                    for (int row = 0; row < 8; row++) {
                        colorGrid[row][0] = clrSets[i / 8][Globals.INDEX_CLR_BACK];
                        colorGrid[row][1] = clrSets[i / 8][Globals.INDEX_CLR_FORE];
                    }
                }
            }
        }
        return charStart;
    }
}
