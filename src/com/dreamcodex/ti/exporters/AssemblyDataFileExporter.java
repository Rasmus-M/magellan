package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static com.dreamcodex.ti.Magellan.*;

public class AssemblyDataFileExporter extends Exporter {

    public AssemblyDataFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeAssemblyDataFile(File mapDataFile, Preferences preferences) throws Exception{
        writeAssemblyDataFile(mapDataFile, preferences.getDefStartChar(), preferences.getDefEndChar(), preferences.getDefStartSprite(), preferences.getDefEndSprite(), preferences.getCompression(), preferences.isExportComments(), preferences.isCurrentMapOnly(), preferences.isIncludeCharNumbers(), preferences.isIncludeSpriteData());
    }

    public void writeAssemblyDataFile(File mapDataFile, int startChar, int endChar, int startSprite, int endSprite, int compression, boolean includeComments, boolean currMapOnly, boolean includeCharNumbers, boolean includeSpriteData) throws Exception {
        if ((compression == MagellanExportDialog.COMPRESSION_RLE_BYTE || compression == MagellanExportDialog.COMPRESSION_RLE_WORD) && endChar > 127) {
            throw new Exception("RLE Compression not supported for characters > 127.");
        }
        // Build ECM palette map
        Map<ECMPalette, Integer> paletteMap = new HashMap<ECMPalette, Integer>(16);
        {
            int n = 0;
            for (ECMPalette ecmPalette : ecmPalettes) {
                paletteMap.put(ecmPalette, n++);
            }
        }
        StringBuilder sbLine = new StringBuilder();
        BufferedWriter bw = null;
        mapdMain.storeCurrentMap();
        bw = new BufferedWriter(new FileWriter(mapDataFile));
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Colorset Definitions", false);
            printPaddedLine(bw, "****************************************", false);
        }
        if (colorMode == COLOR_MODE_GRAPHICS_1) {
            int itemCount = 0;
            printPaddedLine(bw, "CLRNUM DATA " + (clrSets.length), includeComments);
            for (int i = 0; i < clrSets.length; i++) {
                if (itemCount == 0) {
                    if (i == 0) {
                        sbLine.append("CLRSET BYTE ");
                    }
                    else {
                        sbLine.append("       BYTE ");
                    }
                }
                if (itemCount > 0) {
                    sbLine.append(",");
                }
                sbLine.append(">");
                sbLine.append(Integer.toHexString(clrSets[i][Globals.INDEX_CLR_FORE]).toUpperCase());
                sbLine.append(Integer.toHexString(clrSets[i][Globals.INDEX_CLR_BACK]).toUpperCase());
                itemCount++;
                if (itemCount > 3) {
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                    itemCount = 0;
                }
            }
        }
        else if (colorMode == COLOR_MODE_BITMAP) {
            sbLine.delete(0, sbLine.length());
            for (int i = startChar; i <= endChar; i++) {
                int[][] charColors = hmCharColors.get(i);
                if (charColors != null) {
                    if (includeCharNumbers) {
                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + i, includeComments);
                    }
                    sbLine.append("COL").append(i).append(i < 10 ? "  " : (i < 100 ? " " : "")).append(" DATA ");
                    for (int row = 0; row < 8; row += 2) {
                        sbLine.append(">");
                        int[] rowColors = charColors[row];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        rowColors = charColors[row + 1];
                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
                        if (row < 6) {
                            sbLine.append(",");
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
        }
        else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            sbLine.delete(0, sbLine.length());
            for (int i = 0; i < ecmPalettes.length; i++) {
                ECMPalette ecmPalette = ecmPalettes[i];
                for (int l = 0; l < ecmPalette.getSize() / 4; l++) {
                    if (l == 0) {
                        sbLine.append("PAL").append(i).append(i < 10 ? " " : "").append("  DATA ");
                    }
                    else {
                        sbLine.append("       DATA ");
                    }
                    for (int c = 0; c < 4; c++) {
                        Color color = ecmPalette.getColor(c + l * 4);
                        sbLine.append(">0");
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getRed() / 17)).toUpperCase());
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getGreen() / 17)).toUpperCase());
                        sbLine.append(Integer.toHexString((int) Math.round((double) color.getBlue() / 17)).toUpperCase());
                        if (c < 3) {
                            sbLine.append(",");
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Tile Attributes", false);
                printPaddedLine(bw, "****************************************", false);
            }
            sbLine.delete(0, sbLine.length());
            for (int i = startChar; i <= endChar; i++) {
                sbLine.append("TAT").append(i).append(i < 10 ? "   " : (i < 100 ? "  " : " "));
                sbLine.append("BYTE >").append(Globals.toHexString((paletteMap.get(ecmCharPalettes[i]) << (colorMode == COLOR_MODE_ECM_3 ? 1 : 0)) | (ecmCharTransparency[i] ? 0x10 : 0), 2));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (sbLine.length() > 0) {
            printPaddedLine(bw, sbLine.toString(), includeComments);
            sbLine.delete(0, sbLine.length());
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Character Patterns" + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? " Plane 0" : ""), false);
            printPaddedLine(bw, "****************************************", false);
        }
        sbLine.delete(0, sbLine.length());
        for (int i = startChar; i <= endChar; i++) {
            String hexstr = Globals.BLANKCHAR;
            if (hmCharGrids.get(i) != null) {
                hexstr = Globals.getHexString(hmCharGrids.get(i)).toUpperCase();
            }
            if (includeCharNumbers) {
                printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + i, includeComments);
            }
            sbLine.append((colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? "PAT" : "P0_") + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
            sbLine.append(">" + hexstr.substring(0, 4) + ",");
            sbLine.append(">" + hexstr.substring(4, 8) + ",");
            sbLine.append(">" + hexstr.substring(8, 12) + ",");
            sbLine.append(">" + hexstr.substring(12, 16));
            printPaddedLine(bw, sbLine.toString(), includeComments);
            sbLine.delete(0, sbLine.length());
        }
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Patterns Plane 1", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = startChar; i <= endChar; i++) {
                String hexstr = Globals.BLANKCHAR;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i), 2).toUpperCase();
                }
                sbLine.append("P1_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                sbLine.append(">" + hexstr.substring(0, 4) + ",");
                sbLine.append(">" + hexstr.substring(4, 8) + ",");
                sbLine.append(">" + hexstr.substring(8, 12) + ",");
                sbLine.append(">" + hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (colorMode == COLOR_MODE_ECM_3) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Patterns Plane 2", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = startChar; i <= endChar; i++) {
                String hexstr = Globals.BLANKCHAR;
                if (hmCharGrids.get(i) != null) {
                    hexstr = Globals.getHexString(hmCharGrids.get(i), 4).toUpperCase();
                }
                sbLine.append("P2_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                sbLine.append(">" + hexstr.substring(0, 4) + ",");
                sbLine.append(">" + hexstr.substring(4, 8) + ",");
                sbLine.append(">" + hexstr.substring(8, 12) + ",");
                sbLine.append(">" + hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments);
                sbLine.delete(0, sbLine.length());
            }
        }
        if (includeSpriteData) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Sprite Patterns" + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? " Plane 0" : ""), false);
                printPaddedLine(bw, "****************************************", false);
            }
            sbLine.delete(0, sbLine.length());
            for (int i = startSprite; i <= endSprite; i++) {
                if (hmSpriteGrids.get(i) != null) {
                    String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i)).toUpperCase();
                    sbLine.append((colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? "SPR" : "S0_") + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                    for (int pos = 0; pos < 64; pos += 4) {
                        if (pos > 0 && pos % 16 == 0) {
                            sbLine.append("       DATA ");
                        }
                        sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                        if (pos % 16 == 12) {
                            printPaddedLine(bw, sbLine.toString(), includeComments ? (pos == 12 ? "Color " + spriteColors[i] : "") : null);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
            }
            if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Sprite Patterns Plane 1", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                sbLine.delete(0, sbLine.length());
                for (int i = startSprite; i <= endSprite; i++) {
                    if (hmSpriteGrids.get(i) != null) {
                        String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i), 2).toUpperCase();
                        sbLine.append("S1_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                        for (int pos = 0; pos < 64; pos += 4) {
                            if (pos > 0 && pos % 16 == 0) {
                                sbLine.append("       DATA ");
                            }
                            sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                            if (pos % 16 == 12) {
                                printPaddedLine(bw, sbLine.toString(), includeComments);
                                sbLine.delete(0, sbLine.length());
                            }
                        }
                    }
                }
            }
            if (colorMode == COLOR_MODE_ECM_3) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Sprite Patterns Plane 2", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                sbLine.delete(0, sbLine.length());
                for (int i = startSprite; i <= endSprite; i++) {
                    if (hmSpriteGrids.get(i) != null) {
                        String hexstr = Globals.getSpriteHexString(hmSpriteGrids.get(i), 4).toUpperCase();
                        sbLine.append("S2_" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA ");
                        for (int pos = 0; pos < 64; pos += 4) {
                            if (pos > 0 && pos % 16 == 0) {
                                sbLine.append("       DATA ");
                            }
                            sbLine.append(">" + hexstr.substring(pos, pos + 4) + (pos % 16 != 12 ? "," : ""));
                            if (pos % 16 == 12) {
                                printPaddedLine(bw, sbLine.toString(), includeComments);
                                sbLine.delete(0, sbLine.length());
                            }
                        }
                    }
                }
            }
        }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        printPaddedLine(bw, "MCOUNT DATA " + (currMapOnly ? 1 : mapdMain.getMapCount()), includeComments);
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                int[][] mapToSave = mapdMain.getMapData(m);
                if (includeComments) {
                    printPaddedLine(bw, "* == Map #" + m + " == ", false);
                }
                printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapdMain.getScreenColor(m), includeComments);
                sbLine.delete(0, sbLine.length());
                sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
                sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
                sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
                sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
                printPaddedLine(bw, sbLine.toString(), includeComments ? "Width, Height, Size" : null);
                sbLine.delete(0, sbLine.length());
                if (compression == MagellanExportDialog.COMPRESSION_NONE) {
                    boolean isFirstByte;
                    String hexChunk;
                    isFirstByte = true;
                    for (int y = 0; y < mapToSave.length; y++) {
                        if (includeComments) {
                            printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                        }
                        int rowLength = mapToSave[y].length;
                        boolean useBytes = rowLength % 2 == 1;
                        String directive = useBytes ? "BYTE" : "DATA";
                        for (int cl = 0; cl < Math.ceil((double) rowLength / 8); cl++) {
                            if (y == 0 && cl == 0) {
                                sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" " + directive + " ");
                            }
                            else {
                                sbLine.append("       " + directive + " ");
                            }
                            for (int colpos = (cl * 8); colpos < Math.min((cl + 1) * 8, rowLength); colpos++) {
                                if (isFirstByte) {
                                    if (colpos > (cl * 8)) {
                                        sbLine.append(",");
                                    }
                                    sbLine.append(">");
                                }
                                hexChunk = Integer.toHexString(mapToSave[y][colpos]).toUpperCase();
                                if (mapToSave[y][colpos] == MapCanvas.NOCHAR) {
                                    hexChunk = "00";
                                }
                                sbLine.append(hexChunk.length() < 1 ? "00" : (hexChunk.length() < 2 ? "0" : "")).append(hexChunk);
                                if (!useBytes) {
                                    isFirstByte = !isFirstByte;
                                }
                            }
                            printPaddedLine(bw, sbLine.toString(), includeComments);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
                else if (compression == MagellanExportDialog.COMPRESSION_RLE_BYTE) {
                    // RLE compression (byte)
                    // We assume all characters are < 128. If msb is set, the next byte determines
                    // how many times (2 - 256) the current byte (with msb cleared) should be repeated.
                    // A repeat count of 0 is used as end marker.
                    System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                    int i = 0;
                    int n = 0;
                    int current;
                    int last = -1;
                    int count = 0;
                    for (int y = 0; y < mapToSave.length; y++) {
                        for (int x = 0; x < mapToSave[0].length; x++) {
                            current = mapToSave[y][x];
                            if (last != -1) {
                                if (current == last && count < 255) {
                                    // Same byte, increment count
                                    count++;
                                }
                                else if (count > 0) {
                                    // End of run of bytes
                                    i = printByte(bw, sbLine, i, last | 128);
                                    i = printByte(bw, sbLine, i, count);
                                    count = 0;
                                    n += 2;
                                }
                                else {
                                    // Different byte
                                    i = printByte(bw, sbLine, i, last);
                                    n++;
                                }
                            }
                            last = current;
                        }
                    }
                    if (count > 0) {
                        i = printByte(bw, sbLine, i, last | 128);
                        i = printByte(bw, sbLine, i, count);
                        n += 2;
                    }
                    else {
                        // Different byte
                        i = printByte(bw, sbLine, i, last);
                        n++;
                    }
                    // End marker
                    i = printByte(bw, sbLine, i, 128);
                    i = printByte(bw, sbLine, i, 0);
                    n += 2;
                    printPaddedLine(bw, sbLine.toString(), false);
                    n += i;
                    System.out.println("Compressed size: " + n);
                }
                else if (compression == MagellanExportDialog.COMPRESSION_RLE_WORD) {
                    // RLE compression (word)
                    // We assume all characters are < 128. If msb of the MSB is set, the byte following the
                    // current word determines how many times (2 - 256) the current word (with msb cleared)
                    // should be repeated. A repeat count of 0 is used as end marker.
                    System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                    int i = 0;
                    int n = 0;
                    int current;
                    int last = -1;
                    int count = 0;
                    for (int y = 0; y < mapToSave.length; y++) {
                        for (int x = 0; x < mapToSave[0].length - 1; x += 2) {
                            current = mapToSave[y][x] << 8 | mapToSave[y][x + 1];
                            if (last != -1) {
                                if (current == last && count < 255) {
                                    // Same word, increment count
                                    count++;
                                }
                                else if (count > 0) {
                                    // End of run of words
                                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                                    i = printByte(bw, sbLine, i, last & 0x00FF);
                                    i = printByte(bw, sbLine, i, count);
                                    count = 0;
                                    n += 3;
                                }
                                else {
                                    // Different byte
                                    i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                                    i = printByte(bw, sbLine, i, last & 0x00FF);
                                    n += 2;
                                }
                            }
                            last = current;
                        }
                    }
                    if (count > 0) {
                        i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                        i = printByte(bw, sbLine, i, last & 0x00FF);
                        i = printByte(bw, sbLine, i, count);
                        n += 2;
                    }
                    else {
                        // Different byte
                        i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8);
                        i = printByte(bw, sbLine, i, last & 0x00FF);
                        n++;
                    }
                    // End marker
                    i = printByte(bw, sbLine, i, 128);
                    i = printByte(bw, sbLine, i, 0);
                    i = printByte(bw, sbLine, i, 0);
                    n += 3;
                    printPaddedLine(bw, sbLine.toString(), false);
                    n += i;
                    System.out.println("Compressed size: " + n);
                }
                else if (compression == MagellanExportDialog.COMPRESSION_META_2 || compression == MagellanExportDialog.COMPRESSION_META_4) {
                    int size = compression == MagellanExportDialog.COMPRESSION_META_2 ? 2 : 4;
                    int height = mapToSave.length / size;
                    int width = mapToSave[0].length / size;
                    Map<String, MetaTile> metaTileLookup = new HashMap<String, MetaTile>();
                    MetaTile[][] metaTileMap = new MetaTile[height][width];
                    int n = 0;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            StringBuilder keyBuffer = new StringBuilder();
                            int[] tiles = new int[size * size];
                            for (int y1 = 0; y1 < size; y1++) {
                                for (int x1 = 0; x1 < size; x1++) {
                                    int tile = mapToSave[y * size + y1][x * size + x1];
                                    keyBuffer.append(Globals.toHexString(tile, 2));
                                    tiles[y1 * size + x1] = tile;
                                }
                            }
                            String key = keyBuffer.toString();
                            MetaTile metaTile = metaTileLookup.get(key);
                            if (metaTile == null) {
                                metaTile = new MetaTile(n++, tiles);
                                metaTileLookup.put(key, metaTile);
                            }
                            metaTileMap[y][x] = metaTile;
                        }
                    }
                    System.out.println("Number of meta tiles: " + metaTileLookup.size());
                    if (metaTileLookup.size() > 256) {
                        throw new Exception("Cannot support more than 256 meta tiles (" + metaTileLookup.size() + " found)");
                    }
                    boolean first = true;
                    n = 0;
                    for (MetaTile[] row : metaTileMap) {
                        for (MetaTile metaTile : row) {
                            n = printByte(bw, sbLine, n, metaTile.getNumber(), first ? "MD" + m : null);
                            first = false;
                        }
                    }
                    printPaddedLine(bw, sbLine.toString(), false);
                    if (includeComments) {
                        printPaddedLine(bw, "****************************************", false);
                        printPaddedLine(bw, "* Meta Tiles", false);
                        printPaddedLine(bw, "****************************************", false);
                    }
                    ArrayList<MetaTile> metaTiles = new ArrayList<MetaTile>(metaTileLookup.values());
                    Collections.sort(metaTiles, new Comparator<MetaTile>() {
                        public int compare(MetaTile m1, MetaTile m2) {
                            return m1.getNumber() - m2.getNumber();
                        }
                    });
                    for (MetaTile metaTile : metaTiles) {
                        sbLine.delete(0, sbLine.length());
                        n = 0;
                        first = true;
                        int[] tiles = metaTile.getTiles();
                        for (int tile : tiles) {
                            n = printByte(bw, sbLine, n, tile, first ? "MT" + m + Globals.padl(metaTile.getNumber(), 3) : null);
                            first = false;
                        }
                        if (metaTile.getTiles().length < 8) {
                            printPaddedLine(bw, sbLine.toString(), false);
                        }
                    }
                }
                else if (compression == MagellanExportDialog.COMPRESSION_NYBBLES) {
                    boolean isFirstByte;
                    String hexChunk;
                    for (int y = 0; y < mapToSave.length; y++) {
                        if (includeComments) {
                            printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                        }
                        for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 16); cl++) {
                            if (y == 0 && cl == 0) {
                                sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA ");
                            }
                            else {
                                sbLine.append("       DATA ");
                            }
                            for (int colpos = (cl * 16); colpos < Math.min((cl + 1) * 16, mapToSave[y].length); colpos++) {
                                if (colpos % 4 == 0) {
                                    if (colpos > (cl * 16)) {
                                        sbLine.append(",");
                                    }
                                    sbLine.append(">");
                                }
                                int value = mapToSave[y][colpos];
                                if (value >= 16) {
                                    throw new RuntimeException("Compression mode not supported for characters >= 16");
                                }
                                hexChunk = value != MapCanvas.NOCHAR ? Integer.toHexString(value).toUpperCase() : "0";
                                sbLine.append(hexChunk);
                            }
                            printPaddedLine(bw, sbLine.toString(), includeComments);
                            sbLine.delete(0, sbLine.length());
                        }
                    }
                }
                else {
                    throw new RuntimeException("Compression mode not yet supported.");
                }
                if (includeSpriteData) {
                    HashMap<Point, ArrayList<Integer>> spriteMap = mapdMain.getSpriteMap(m);
                    if (spriteMap.size() > 0) {
                        if (includeComments) {
                            printPaddedLine(bw, "* Sprite Locations", false);
                        }
                        boolean smallMap = mapdMain.getGridWidth() <= 32 && mapdMain.getGridHeight() <= 32;
                        boolean first = true;
                        for (Point p : spriteMap.keySet()) {
                            ArrayList<Integer> spriteList = spriteMap.get(p);
                            for (Integer spriteNum : spriteList) {
                                int color = (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) ? spriteColors[spriteNum] : paletteMap.get(ecmSpritePalettes[spriteNum]) * (colorMode == COLOR_MODE_ECM_3 ? 2 : 1);
                                printPaddedLine(bw, (first ? "SL" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) : "      ") +
                                        (smallMap ? " BYTE " + ((p.y * 8 - 1) & 0xFF) : " DATA " + (p.y * 8)) + "," + (p.x * 8) + "," + spriteNum * 4 + "," + color, includeComments ? (first ? "y, x, pattern#, color#" : "") : null);
                                first = false;
                            }
                        }
                    }
                }
            }
        }
        bw.flush();
        bw.close();
    }
}
