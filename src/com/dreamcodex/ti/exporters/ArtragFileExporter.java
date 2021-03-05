package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TransChar;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArtragFileExporter extends Exporter {

    public ArtragFileExporter(MapEditor mapdMain, ECMPalette[] ecmPalettes, int[][] clrSets, HashMap<Integer, int[][]> hmCharGrids, HashMap<Integer, int[][]> hmCharColors, ECMPalette[] ecmCharPalettes, boolean[] ecmCharTransparency, HashMap<Integer, int[][]> hmSpriteGrids, int[] spriteColors, ECMPalette[] ecmSpritePalettes, int colorMode) {
        super(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
    }

    public void writeArtragFile(File mapDataFile, boolean wrap, boolean includeComments, boolean includeCharNumbers, int frames) throws Exception {

        Map<String, String> colorLinkHints = new HashMap<String, String>();
        colorLinkHints.put("0-73", "0-23");
        colorLinkHints.put("73-0", "24-0");
        colorLinkHints.put("0-74", "9-10");
        colorLinkHints.put("74-0", "10-11");
        colorLinkHints.put("0-75", "0-18");
        colorLinkHints.put("75-0", "18-0");
        colorLinkHints.put("0-73", "0-23");

        // store working map first
        mapdMain.storeCurrentMap();
        BufferedWriter bw = null;
        int[][][] transMaps = new int[2][][];
        Map<String, TransChar>[] transCharsArr = new Map[2];
        TransChar[][] transCharSetArr = new TransChar[2][];
        boolean[] usedChars = new boolean[256];
        int startChar = 255;
        int endChar = 0;
        int imax = 0;
        boolean allColorsOK = true;
        for (int m = 0; m < 2; m++) {
            Map<String, TransChar> transChars = new HashMap<String, TransChar>();
            transCharsArr[m] = transChars;
            TransChar[] transCharSet = new TransChar[256];
            transCharSetArr[m] = transCharSet;
            int yOffset = m == 0 ? 8 : 0;
            int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
            if (mapData.length > 1 && mapData[0].length > 1) {
                int i = 0;
                for (int y = yOffset; y < yOffset + 8; y++) {
                    for (int x = 0; x < mapData[0].length - (wrap ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        if (fromChar < startChar) {
                            startChar = fromChar;
                        }
                        if (fromChar > endChar) {
                            endChar = fromChar;
                        }
                        usedChars[fromChar] = true;
                        int toChar = mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                        if (toChar < startChar) {
                            startChar = toChar;
                        }
                        if (toChar > endChar) {
                            endChar = toChar;
                        }
                        usedChars[toChar] = true;
                        String key = fromChar + "-" + toChar;
                        TransChar transChar = transChars.get(key);
                        if (transChar != null) {
                            transChar.incCount();
                        }
                        else {
                            boolean colorsOK = true;
                            int[][] charColors = new int[8][8];
                            int[][] fromCharGrid = hmCharGrids.get(fromChar);
                            int[][] toCharGrid = hmCharGrids.get(toChar);
                            int[][] fromColorGrid = hmCharColors.get(fromChar);
                            int[][] toColorGrid = hmCharColors.get(toChar);
                            for (int r = 0; r < 8 && colorsOK; r++) {
                                int[] fromColorRow = fromColorGrid[r];
                                int[] toColorRow = toColorGrid[r];
                                int screenColor = mapdMain.getColorScreen();
                                int fromForeColor = fromColorRow[Globals.INDEX_CLR_FORE] != 0 ? fromColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                int toForeColor = toColorRow[Globals.INDEX_CLR_FORE] != 0 ? toColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_FORE) && !Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = 0; // don't care
                                }
                                else if (fromForeColor == toForeColor) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                }
                                else if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = toForeColor;
                                }
                                else if (!Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_FORE)) {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                }
                                else {
                                    charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                    colorsOK = false;
                                    allColorsOK = false;
                                }
                                int fromBackColor = fromColorRow[Globals.INDEX_CLR_BACK] != 0 ? fromColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                int toBackColor = toColorRow[Globals.INDEX_CLR_BACK] != 0 ? toColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_BACK) && !Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = 0; // don't care
                                }
                                else if (fromBackColor == toBackColor) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                }
                                else if (!Globals.arrayContains(fromCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = toBackColor;
                                }
                                else if (!Globals.arrayContains(toCharGrid[r], Globals.INDEX_CLR_BACK)) {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                }
                                else {
                                    charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                    colorsOK = false;
                                    allColorsOK = false;
                                }
                            }
                            if (m == 0) {
                                transChar = new TransChar(fromChar, toChar, i, colorsOK, charColors);
                                transChars.put(key, transChar);
                                transCharSet[i] = transChar;
                                imax = i++;
                                if (imax > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                }
                            }
                            else {
                                String hexString = Globals.getColorHexString(charColors);
                                // Second half of the map - this has a separate character set from the first part
                                // but both parts share the same color set, so we must try to match the colors
                                int l = transCharsArr[0].size();
                                boolean matchFound = false;
                                int s = 0;
                                if (colorLinkHints.containsKey(key)) {
                                    s = transCharsArr[0].get(colorLinkHints.get(key)).getIndex();
                                }
                                for (int t = s; t < l && !matchFound; t++) {
                                    TransChar tc = transCharSetArr[0][t];
                                    if (transCharSet[tc.getIndex()] == null) {
                                        boolean colorsMatch = true;
                                        int[][] tColors = tc.getColorGrid();
                                        int[][] newColors = new int[8][2];
                                        for (int r = 0; r < 8 && colorsMatch; r++) {
                                            for (int c = 0; c < 2; c++) {
                                                int color1 = tColors[r][c];
                                                int color2 = charColors[r][c];
                                                if (color1 != color2) {
                                                    if (color1 != 0 && color2 != 0) {
                                                        colorsMatch = false;
                                                    }
                                                    else if (color1 == 0) {
                                                        newColors[r][c] = color2;
                                                    }
                                                    else {
                                                        newColors[r][c] = color1;
                                                    }
                                                }
                                                else {
                                                    newColors[r][c] = color1;
                                                }
                                            }
                                        }
                                        if (colorsMatch) {
                                            String hexString2 = Globals.getColorHexString(tColors);
                                            tc.setColorGrid(newColors);
                                            transChar = new TransChar(fromChar, toChar, tc.getIndex(), colorsOK, newColors);
                                            transChars.put(key, transChar);
                                            transCharSet[tc.getIndex()] = transChar;
                                            matchFound = true;
                                            System.out.println("Match found for " + key + " (" + hexString + "): " + tc.getIndex() + " " + tc.getFromChar() + "-" + tc.getToChar() + " before: " + hexString2 + " now: " + Globals.getColorHexString(newColors));
                                        }
                                    }
                                }
                                if (!matchFound) {
                                    System.out.println("No matching colors found for " + key + " (" + hexString + ")");
                                    throw new Exception("No matching colors found for " + key + " (" + hexString+ ")");
                                }
                            }
                        }
                    }
                }
                int[][] transMap = new int[mapData.length][mapData[0].length - (wrap ? 0 : 1)];
                transMaps[m] = transMap;
                for (int y = yOffset; y < yOffset + 8; y++) {
                    for (int x = 0; x < mapData[0].length - (wrap ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        int toChar = mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                        String key = fromChar + "-" + toChar;
                        TransChar transChar = transChars.get(key);
                        transMap[y][x] = transChar.getIndex();
                    }
                }
            }
        }
        if (!allColorsOK) {
            JOptionPane.showMessageDialog(null, "Warning - Some character transitions have incompatible colors. This may cause color spills when the map is scrolled.", "Invalid Color Transitions", JOptionPane.INFORMATION_MESSAGE);
        }
        bw = new BufferedWriter(new FileWriter(mapDataFile));
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Original Character Patterns", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            for (int i = startChar; i <= endChar; i++) {
//                String hexstr = Globals.BLANKCHAR;
//                if (hmCharGrids.get(i) != null) {
//                    hexstr = Globals.getByteString(hmCharGrids.get(i)).toUpperCase();
//                }
//                if (includeCharNumbers) {
//                    printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA >" + Globals.toHexString(i, 2), includeComments);
//                }
//                printPaddedLine(bw, "PAT" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + ">" +
//                    hexstr.substring(0, 4) + "," + ">" +
//                    hexstr.substring(4, 8) + "," + ">" +
//                    hexstr.substring(8, 12) + "," + ">" +
//                    hexstr.substring(12, 16), includeComments ? (usedChars[i] ? "" : "unused") : null);
//            }
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Colorset Definitions", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            for (int i = startChar; i <= endChar; i++) {
//                int[][] charColors = hmCharColors.get(i);
//                if (charColors != null && !Globals.isColorGridEmpty(charColors)) {
//                    if (includeCharNumbers) {
//                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + Globals.toHexString(i, 2), includeComments);
//                    }
//                    StringBuilder sbLine = new StringBuilder();
//                    sbLine.append("COL").append(i).append(i < 10 ? "  " : (i < 100 ? " " : "")).append(" DATA ");
//                    for (int row = 0; row < 8; row += 2) {
//                        sbLine.append(">");
//                        int[] rowColors = charColors[row];
//                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
//                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
//                        rowColors = charColors[row + 1];
//                        sbLine.append(Integer.toHexString(rowColors[1]).toUpperCase());
//                        sbLine.append(Integer.toHexString(rowColors[0]).toUpperCase());
//                        if (row < 6) {
//                            sbLine.append(",");
//                        }
//                    }
//                    printPaddedLine(bw, sbLine.toString(), includeComments ? (usedChars[i] ? "" : "unused") : null);
//                }
//            }
//            if (includeComments) {
//                printPaddedLine(bw, "****************************************", false);
//                printPaddedLine(bw, "* Transition Character Pairs (from, to) ", false);
//                printPaddedLine(bw, "****************************************", false);
//            }
//            printPaddedLine(bw, "TCHNUM DATA " + (imax + 1), includeComments);
//            for (int m = 0; m < 2; m++) {
//                for (int i = 0; i <= imax; i++) {
//                    TransChar transChar = transCharSetArr[m][i];
//                    if (transChar != null) {
//                        printPaddedLine(bw,
//                                (i == 0 ? "TCHRS" + m : "      ") + " BYTE >" + Globals.toHexString(transChar.getFromChar(), 2) + ",>" + Globals.toHexString(transChar.getToChar(), 2),
//                                !includeComments ? null :
//                                        "#" + Globals.toHexString(transChar.getIndex(), 2) +
//                                                (colorMode != Magellan.COLOR_MODE_BITMAP ? " color " + Globals.toHexString(transChar.getForeColor(), 1) + "/" + Globals.toHexString(transChar.getBackColor(), 1) : "") +
//                                                (transChar.isColorsOK() ? "" : " ERROR")
//                        );
//                    }
//                    else {
//                        printPaddedLine(bw, (i == 0 ? "TCHRS0" : "      ") + " BYTE >FF,>FF", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
//                    }
//                }
//            }
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int m = 1; m >= 0; m--) {
            StringBuilder sbLine = new StringBuilder();
            int[][] mapToSave = transMaps[m];
            if (includeComments) {
                printPaddedLine(bw, "* == Map #" + m + " == ", false);
            }
            printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapdMain.getScreenColor(m), includeComments);
            sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
            sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
            printPaddedLine(bw, sbLine.toString(), "Width, Height, Size");
            sbLine.delete(0, sbLine.length());

            boolean isFirstByte = true;
            int yOffset = m == 0 ? 8 : 0;
            for (int y = yOffset; y < yOffset + 8; y++) {
                if (includeComments) {
                    printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                }
                for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 8); cl++) {
                    if (y == 0 && cl == 0) {
                        sbLine.append(m == 0 ? "MAPBOT" : "MAPTOP").append(" DATA ");
                    }
                    else {
                        sbLine.append("       DATA ");
                    }
                    for (int colpos = (cl * 8); colpos < Math.min((cl + 1) * 8, mapToSave[y].length); colpos++) {
                        if (isFirstByte) {
                            if (colpos > (cl * 8)) {
                                sbLine.append(",");
                            }
                            sbLine.append(">");
                        }
                        sbLine.append(mapToSave[y][colpos] == MapCanvas.NOCHAR ? "00" : Globals.toHexString(mapToSave[y][colpos], 2));
                        isFirstByte = !isFirstByte;
                    }
                    if (!isFirstByte) {
                        sbLine.append("XX"); // If odd, pad with an illegal value
                        isFirstByte = !isFirstByte;
                    }
                    printPaddedLine(bw, sbLine.toString(), includeComments);
                    sbLine.delete(0, sbLine.length());
                }
            }
            ArrayList<int[]> uniqueRows = new ArrayList<int[]>();
            for (int[] row : mapToSave) {
                boolean found = false;
                for (int y = 0; y < uniqueRows.size() && !found; y++) {
                    if (Arrays.equals(row, uniqueRows.get(y))) {
                        found = true;
                    }
                }
                if (!found) {
                    uniqueRows.add(row);
                }
            }
        }
        if (frames > 0) {
            for (int m = 1; m >= 0; m--) {
                if (includeComments) {
                    printPaddedLine(bw, "****************************************", false);
                    printPaddedLine(bw, "* Scrolled Character Patterns", false);
                    printPaddedLine(bw, "****************************************", false);
                }
                for (int f = 0; f < frames; f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(m == 0 ? "PTBOT" : "PTTOP").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSetArr[m][i];
                        if (transChar != null) {
                            int[][] fromGrid = hmCharGrids.get(transChar.getFromChar());
                            int[][] toGrid = hmCharGrids.get(transChar.getToChar());
                            if (transChar.isInvert()) {
                                toGrid = Globals.cloneGrid(toGrid);
                                Globals.invertGrid(toGrid, 1);
                            }
                            int[][] scrollGrid = new int[8][8];
                            int x1 = 0;
                            for (int x = offset; x < 8; x++) {
                                for (int y = 0; y < 8; y++) {
                                    scrollGrid[y][x1] = fromGrid[y][x];
                                }
                                x1++;
                            }
                            for (int x = 0; x < offset; x++) {
                                for (int y = 0; y < 8; y++) {
                                    scrollGrid[y][x1] = toGrid[y][x];
                                }
                                x1++;
                            }
                            hexstr = Globals.getHexString(scrollGrid).toUpperCase();
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                        sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                        sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                        sbLine.append(">").append(hexstr.substring(12, 16));
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Character Colors", false);
                printPaddedLine(bw, "****************************************", false);
            }
            for (int i = 0; i <= imax; i++) {
                StringBuilder sbLine = new StringBuilder();
                if (i == 0) {
                    sbLine.append("COLORS");
                }
                else {
                    sbLine.append("      ");
                }
                sbLine.append(" DATA ");
                String hexstr;
                TransChar transChar = transCharSetArr[0][i];
                if (transChar != null) {
                    hexstr = Globals.getColorHexString(transChar.getColorGrid()).toUpperCase();
                }
                else {
                    hexstr = Globals.BLANKCHAR;
                }
                sbLine.append(">").append(hexstr.substring(0, 4)).append(",");
                sbLine.append(">").append(hexstr.substring(4, 8)).append(",");
                sbLine.append(">").append(hexstr.substring(8, 12)).append(",");
                sbLine.append(">").append(hexstr.substring(12, 16));
                printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
            }
        }
        bw.flush();
        bw.close();
    }
}
