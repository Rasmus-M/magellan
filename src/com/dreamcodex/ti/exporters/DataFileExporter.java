package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.util.ColorMode.*;

public class DataFileExporter extends Exporter {

    public DataFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeDataFile(File mapDataFile, int characterSetSize) throws IOException {
        // store working map first
        mapEditor.storeCurrentMap();
        // get file output buffer
        BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile));
        // Build ECM palette map
        Map<ECMPalette, Integer> paletteMap = new HashMap<ECMPalette, Integer>(16);
        int n = 0;
        for (ECMPalette ecmPalette : ecmPalettes) {
            paletteMap.put(ecmPalette, n++);
        }
        // Color mode
        bw.write("* COLOR MODE");
        bw.newLine();
        bw.write(Globals.KEY_COLOR_MODE + colorMode.ordinal());
        bw.newLine();
        // output overall character range (this is for backwards compatibility with earlier Magellan releases, will be phased out)
        bw.write("* CHARACTER RANGE");
        bw.newLine();
        bw.write(Globals.KEY_CHARRANG + TIGlobals.MIN_CHAR + "|" + getCharacterSetEnd(characterSetSize));
        bw.newLine();
        // save colorsets
        bw.write("* COLORSET");
        bw.newLine();
        for (int i = 0; i <= TIGlobals.MAX_CHAR / 8; i++) {
            bw.write(Globals.KEY_COLORSET + clrSets[i][Globals.INDEX_CLR_FORE] + "|" + clrSets[i][Globals.INDEX_CLR_BACK]);
            bw.newLine();
        }
        // save palettes
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            bw.write("* PALETTES");
            bw.newLine();
            for (ECMPalette ecmPalette : ecmPalettes) {
                bw.write(Globals.KEY_PALETTE);
                Color[] colors = ecmPalette.getColors();
                for (int i = 0; i < colors.length; i++) {
                    bw.write(Integer.toHexString(colors[i].getRGB()));
                    if (i < colors.length - 1) {
                        bw.write("|");
                    }
                }
                bw.newLine();
            }
            // save character palette numbers
            bw.write("* CHAR PALETTE NUMBERS");
            bw.newLine();
            for (ECMPalette charPalette : ecmCharPalettes) {
                bw.write(Globals.KEY_CHARPALS);
                bw.write(Integer.toString(paletteMap.get(charPalette)));
                bw.newLine();
            }
            // save character transparency
            bw.write("* CHAR TRANSPARENCY");
            bw.newLine();
            for (boolean trans : ecmCharTransparency) {
                bw.write(Globals.KEY_CHARTRANS);
                bw.write(trans ? "1" : "0");
                bw.newLine();
            }
        }
        // save chardefs
        bw.write("* CHAR DEFS");
        bw.newLine();
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            bw.write(Globals.KEY_CHARDATA);
            if (charGrids.get(i) != null) {
                String hexstr = Globals.getHexString(charGrids.get(i));
                bw.write(hexstr, 0, hexstr.length());
            }
            else {
                bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
            }
            bw.newLine();
        }
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            bw.write("* CHAR DEFS PLANE 1");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARDATA1);
                if (charGrids.get(i) != null) {
                    String hexstr = Globals.getHexString(charGrids.get(i), 2);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        if (colorMode == COLOR_MODE_ECM_3) {
            bw.write("* CHAR DEFS PLANE 2");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARDATA2);
                if (charGrids.get(i) != null) {
                    String hexstr = Globals.getHexString(charGrids.get(i), 4);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        // Save char colors (bitmap mode)
        if (colorMode == COLOR_MODE_BITMAP) {
            bw.write("* CHAR COLORS");
            bw.newLine();
            for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
                bw.write(Globals.KEY_CHARCOLOR);
                int[][] charColors = this.charColors.get(i);
                if (charColors != null) {
                    String hexstr = Globals.getColorHexString(charColors);
                    bw.write(hexstr, 0, hexstr.length());
                }
                else {
                    bw.write(Globals.BLANKCHAR, 0, Globals.BLANKCHAR.length());
                }
                bw.newLine();
            }
        }
        // save map parameters
        bw.write("* MAPS");
        bw.newLine();
        bw.write(Globals.KEY_MAPCOUNT + mapEditor.getMapCount());
        bw.newLine();
        // save map(s)
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            bw.write("* MAP #" + (m + 1));
            bw.newLine();
            bw.write(Globals.KEY_MAPSTART);
            bw.newLine();
            int[][] mapToSave = mapEditor.getMapData(m);
            bw.write(Globals.KEY_MAPSIZE + mapToSave[0].length + "|" + mapToSave.length);
            bw.newLine();
            bw.write(Globals.KEY_MAPBACK + mapEditor.getScreenColor(m));
            bw.newLine();
            for (int y = 0; y < mapToSave.length; y++) {
                bw.write(Globals.KEY_MAPDATA);
                for (int x = 0; x < mapToSave[y].length; x++) {
                    bw.write((x > 0 ? "|" : "") + mapToSave[y][x]);
                }
                bw.newLine();
            }
            bw.write("* SPRITE LOCATIONS");
            bw.newLine();
            bw.write(Globals.KEY_SPRITE_LOCATION_PIXELS + "1");
            bw.newLine();
            HashMap<Point, ArrayList<Integer>> spriteMap = mapEditor.getSpriteMap(m);
            for (Point p : spriteMap.keySet()) {
                ArrayList<Integer> spriteList = spriteMap.get(p);
                for (Integer spriteNum : spriteList) {
                    bw.write(Globals.KEY_SPRITE_LOCATION + p.x + "|" + p.y + "|" + spriteNum);
                    bw.newLine();
                }
            }
            bw.write(Globals.KEY_MAPEND);
            bw.newLine();
        }
        // Save sprites
        bw.write("* SPRITES PATTERNS");
        bw.newLine();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            bw.write(Globals.KEY_SPRITE_PATTERN);
            if (spriteGrids.get(i) != null) {
                bw.write(Globals.getHexString(spriteGrids.get(i)));
            }
            else {
                bw.write(Globals.BLANKSPRITE);
            }
            bw.newLine();
        }
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            bw.write("* SPRITES PATTERNS PLANE 1");
            bw.newLine();
            for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
                bw.write(Globals.KEY_SPRITE_PATTERN1);
                if (spriteGrids.get(i) != null) {
                    bw.write(Globals.getHexString(spriteGrids.get(i), 2));
                }
                else {
                    bw.write(Globals.BLANKSPRITE);
                }
                bw.newLine();
            }
        }
        if (colorMode == COLOR_MODE_ECM_3) {
            bw.write("* SPRITES PATTERNS PLANE 2");
            bw.newLine();
            for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
                bw.write(Globals.KEY_SPRITE_PATTERN2);
                if (spriteGrids.get(i) != null) {
                    bw.write(Globals.getHexString(spriteGrids.get(i), 4));
                }
                else {
                    bw.write(Globals.BLANKSPRITE);
                }
                bw.newLine();
            }
        }
        bw.write("* SPRITE COLORS/PALETTES");
        bw.newLine();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            bw.write(Globals.KEY_SPRITE_COLOR);
            if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                bw.write(Integer.toString(spriteColors[i]));
            }
            else {
                bw.write(Integer.toString(paletteMap.get(ecmSpritePalettes[i])));
            }
            bw.newLine();
        }
        bw.flush();
        bw.close();
    }
}
