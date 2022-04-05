package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;
import static java.lang.Math.floorMod;

public class ScrollFileExporter extends Exporter {

    public ScrollFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeScrollFile(File mapDataFile, TransitionType transitionType, boolean wrap, int compression, boolean includeComments, boolean currMapOnly, boolean includeCharNumbers, int frames, boolean animate) throws Exception {
        if (transitionType == TransitionType.TWO_DIMENSIONAL || transitionType == TransitionType.ISOMETRIC) {
            throw new Exception("Export not implemented for " + transitionType);
        }
        mapEditor.storeCurrentMap();
        ArrayList<int[][]> transMaps = new ArrayList<>();
        Map<String, TransChar> transChars = new HashMap<>();
        Map<Integer, ArrayList<TransChar>> colorSets = new TreeMap<>();
        TransChar[] transCharSet = new TransChar[256];
        boolean[] usedChars = new boolean[256];
        int[] startAndEndChar = {255, 0};
        // Find transitions
        int imax = findCharacterTransitions(transMaps, transCharSet, transChars, usedChars, colorSets, startAndEndChar, currMapOnly, transitionType, wrap);
        int startChar = startAndEndChar[0];
        int endChar = startAndEndChar[1];
        // Remap
        ArrayList<Integer> remappedChars = new ArrayList<>();
        TransChar[] remappedTransCharSet = new TransChar[transCharSet.length];
        remapOriginalCharacters(remappedChars, remappedTransCharSet, transCharSet, usedChars, startChar, endChar, animate);
        // Write out result
        BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile));
        writeOriginalCharacterPatterns(bw, remappedChars, includeCharNumbers, includeComments);
        writeColors(bw, colorSets, usedChars, startChar, endChar, animate, includeCharNumbers, includeComments);
        writeTransitionCharacters(bw, remappedTransCharSet, imax, includeComments);
        writeInvertedCharacters(bw, remappedTransCharSet, imax, includeComments);
        writeMap(bw, transMaps, compression, includeComments);
        writeScrolledPatterns(bw, remappedChars, remappedTransCharSet, imax, transitionType, includeComments, frames, animate);
        writeScrolledColors(bw, remappedChars, remappedTransCharSet, imax, transitionType, includeComments, frames, animate);
        bw.flush();
        bw.close();
    }

    private int findCharacterTransitions(ArrayList<int[][]> transMaps, TransChar[] transCharSet, Map<String, TransChar> transChars, boolean[] usedChars, Map<Integer, ArrayList<TransChar>> colorSets, int[] startAndEndChar, boolean currMapOnly, TransitionType transitionType, boolean wrap) throws Exception {
        int imax = 0;
        boolean allColorsOK = true;
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                int[][] mapData = mapEditor.getMapData(m);
                if (mapData.length > 1 && mapData[0].length > 1) {
                    int i = 0;
                    int height = mapData.length;
                    int yStart = transitionType.getyOffset() < 0 && !wrap ? 1 : 0;
                    int yEnd = height - (transitionType.getyOffset() > 0 && !wrap ? 1 : 0);
                    int width = mapData[0].length;
                    int xStart = transitionType.getxOffset() < 0 && !wrap ? 1 : 0;
                    int xEnd = width - (transitionType.getxOffset() > 0 && !wrap ? 1 : 0);
                    for (int y = yStart; y < yEnd; y++) {
                        for (int x = xStart; x < xEnd; x++) {
                            int fromChar = mapData[y][x];
                            if (fromChar < startAndEndChar[0]) {
                                startAndEndChar[0] = fromChar;
                            }
                            if (fromChar > startAndEndChar[1]) {
                                startAndEndChar[1] = fromChar;
                            }
                            usedChars[fromChar] = true;
                            int toChar = mapData[floorMod(y + transitionType.getyOffset(), height)][floorMod(x + transitionType.getxOffset(), width)];
                            if (toChar < startAndEndChar[0]) {
                                startAndEndChar[0] = toChar;
                            }
                            if (toChar > startAndEndChar[1]) {
                                startAndEndChar[1] = toChar;
                            }
                            usedChars[toChar] = true;
                            String key = fromChar + "-" + toChar;
                            TransChar transChar = transChars.get(key);
                            if (transChar != null) {
                                transChar.incCount();
                            } else {
                                boolean colorsOK = true;
                                boolean invert = false;
                                if (colorMode == COLOR_MODE_BITMAP) {
                                    int[][] charColors = new int[8][8];
                                    if (transitionType.getxOffset() != 0) {
                                        int[][] fromColorGrid = this.charColors.get(fromChar);
                                        int[][] toColorGrid = this.charColors.get(toChar);
                                        for (int r = 0; r < 8 && colorsOK; r++) {
                                            int[] fromColorRow = fromColorGrid[r];
                                            int[] toColorRow = toColorGrid[r];
                                            int screenColor = mapEditor.getColorScreen();
                                            int fromForeColor = fromColorRow[Globals.INDEX_CLR_FORE] != 0 ? fromColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                            int toForeColor = toColorRow[Globals.INDEX_CLR_FORE] != 0 ? toColorRow[Globals.INDEX_CLR_FORE] : screenColor;
                                            if (fromForeColor == toForeColor) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                            } else if (!Globals.arrayContains(charGrids.get(fromChar)[r], Globals.INDEX_CLR_FORE)) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = toForeColor;
                                            } else if (!Globals.arrayContains(charGrids.get(toChar)[r], Globals.INDEX_CLR_FORE)) {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                            } else {
                                                charColors[r][Globals.INDEX_CLR_FORE] = fromForeColor;
                                                colorsOK = false;
                                                allColorsOK = false;
                                            }
                                            int fromBackColor = fromColorRow[Globals.INDEX_CLR_BACK] != 0 ? fromColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                            int toBackColor = toColorRow[Globals.INDEX_CLR_BACK] != 0 ? toColorRow[Globals.INDEX_CLR_BACK] : screenColor;
                                            if (fromBackColor == toBackColor) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                            } else if (!Globals.arrayContains(charGrids.get(fromChar)[r], Globals.INDEX_CLR_BACK)) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = toBackColor;
                                            } else if (!Globals.arrayContains(charGrids.get(toChar)[r], Globals.INDEX_CLR_BACK)) {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                            } else {
                                                charColors[r][Globals.INDEX_CLR_BACK] = fromBackColor;
                                                colorsOK = false;
                                                allColorsOK = false;
                                            }
                                        }
                                    }
                                    transChar = new TransChar(fromChar, toChar, i, colorsOK, charColors);
                                    transChars.put(key, transChar);
                                    transCharSet[i] = transChar;
                                    imax = i++;
                                    if (imax > 255) {
                                        throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                    }
                                } else {
                                    int screenColor = mapEditor.getColorScreen();
                                    int[] fromClrSet = clrSets[fromChar / 8];
                                    int[] toClrSet = clrSets[toChar / 8];
                                    int foreColor;
                                    int backColor;
                                    int fromForeColor = fromClrSet[Globals.INDEX_CLR_FORE] != 0 ? fromClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                    int toForeColor = toClrSet[Globals.INDEX_CLR_FORE] != 0 ? toClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                    if (fromForeColor == toForeColor) {
                                        foreColor = fromForeColor;
                                    } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_FORE)) {
                                        foreColor = toForeColor;
                                    } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_FORE)) {
                                        foreColor = fromForeColor;
                                    } else {
                                        foreColor = fromForeColor;
                                        colorsOK = false;
                                        // allColorsOK = false;
                                        System.out.println("Colors not OK: fromChar=" + fromChar + " toChar=" + toChar + " fromForeColor=" + fromForeColor + " toForeColor=" + toForeColor);
                                    }
                                    int fromBackColor = fromClrSet[Globals.INDEX_CLR_BACK] != 0 ? fromClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                    int toBackColor = toClrSet[Globals.INDEX_CLR_BACK] != 0 ? toClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                    if (fromBackColor == toBackColor) {
                                        backColor = fromBackColor;
                                    } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_BACK)) {
                                        backColor = toBackColor;
                                    } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_BACK)) {
                                        backColor = fromBackColor;
                                    } else {
                                        backColor = fromBackColor;
                                        colorsOK = false;
                                        // allColorsOK = false;
                                        System.out.println("Colors not OK: fromChar=" + fromChar + " toChar=" + toChar + " fromBackColor=" + fromBackColor + " toBackColor=" + toBackColor);
                                    }
                                    if (!colorsOK) {
                                        // Invert color set and pattern of to character
                                        colorsOK = true;
                                        toForeColor = toClrSet[Globals.INDEX_CLR_BACK] != 0 ? toClrSet[Globals.INDEX_CLR_BACK] : screenColor;
                                        if (fromForeColor == toForeColor) {
                                            foreColor = fromForeColor;
                                        } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_FORE)) {
                                            foreColor = toForeColor;
                                        } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_BACK)) {
                                            foreColor = fromForeColor;
                                        } else {
                                            foreColor = fromForeColor;
                                            colorsOK = false;
                                            allColorsOK = false;
                                        }
                                        toBackColor = toClrSet[Globals.INDEX_CLR_FORE] != 0 ? toClrSet[Globals.INDEX_CLR_FORE] : screenColor;
                                        if (fromBackColor == toBackColor) {
                                            backColor = fromBackColor;
                                        } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_BACK)) {
                                            backColor = toBackColor;
                                        } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_FORE)) {
                                            backColor = fromBackColor;
                                        } else {
                                            backColor = fromBackColor;
                                            colorsOK = false;
                                            allColorsOK = false;
                                        }
                                        invert = colorsOK;
                                    }
                                    transChar = new TransChar(fromChar, toChar, -1, colorsOK, foreColor, backColor);
                                    transChar.setInvert(invert);
                                    transChars.put(key, transChar);
                                    int ckey = backColor + (foreColor << 4);
                                    ArrayList<TransChar> colorSet = colorSets.computeIfAbsent(ckey, k -> new ArrayList<>());
                                    colorSet.add(transChar);
                                }
                            }
                        }
                    }
                    if (colorMode != COLOR_MODE_BITMAP) {
                        // Organize into color sets
                        i = 0;
                        for (int ckey : colorSets.keySet()) {
                            ArrayList<TransChar> colorSet = colorSets.get(ckey);
                            for (TransChar transChar : colorSet) {
                                transChar.setIndex(i);
                                transCharSet[i] = transChar;
                                imax = i++;
                                if (i > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
                                }
                                if (imax > 255) {
                                    throw new Exception("Character Set Full: Scrolling this map requires more than 256 characters.");
                                }
                            }
                            while (i % 8 != 0) {
                                i++;
                            }
                            if (i > 256) {
                                throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
                            }
                        }
                    }
                    int newHeight = height - (transitionType.getyOffset() != 0 && !wrap ? 1 : 0);
                    int newWidth = width - (transitionType.getxOffset() != 0 && !wrap ? 1 : 0);
                    int[][] transMap = new int[newHeight][newWidth];
                    transMaps.add(transMap);
                    for (int y = yStart; y < yEnd; y++) {
                        for (int x = xStart; x < xEnd; x++) {
                            int fromChar = mapData[y][x];
                            int toChar = mapData[floorMod(y + transitionType.getyOffset(), height)][floorMod(x + transitionType.getxOffset(), width)];;
                            String key = fromChar + "-" + toChar;
                            TransChar transChar = transChars.get(key);
                            transMap[y - yStart][x - xStart] = transChar.getIndex();
                        }
                    }
                }
            }
        }
        if (!allColorsOK) {
            JOptionPane.showMessageDialog(null, "Warning - Some character transitions have incompatible colors. This may cause color spills when the map is scrolled.", "Invalid Color Transitions", JOptionPane.INFORMATION_MESSAGE);
        }
        return imax;
    }

    private void remapOriginalCharacters(ArrayList<Integer> remappedChars, TransChar[] remappedTransCharSet, TransChar[] transCharSet, boolean[] usedChars, int startChar, int endChar, boolean animate) {
        for (int i = 0; i < transCharSet.length; i++) {
            TransChar transChar = transCharSet[i];
            remappedTransCharSet[i] = transChar != null ? new TransChar(transChar) : null;
        }
        int mapTo = 0;
        for (int mapFrom = startChar; mapFrom <= (animate ? charGrids.size() - 1 : endChar); mapFrom++) {
            if (usedChars[mapFrom]) {
                remappedChars.add(mapFrom);
                for (TransChar transChar : remappedTransCharSet) {
                    if (transChar != null) {
                        if (transChar.getFromChar() == mapFrom) {
                            transChar.setFromChar(mapTo);
                        }
                        if (transChar.getToChar() == mapFrom) {
                            transChar.setToChar(mapTo);
                        }
                    }
                }
                mapTo++;
            }
        }
    }

    private void writeOriginalCharacterPatterns(BufferedWriter bw, ArrayList<Integer> remappedChars, boolean includeCharNumbers, boolean includeComments) throws Exception {
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Original Character Patterns", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int j = 0; j < remappedChars.size(); j++) {
            int i = remappedChars.get(j);
            String hexstr = Globals.BLANKCHAR;
            if (charGrids.get(i) != null) {
                hexstr = Globals.getHexString(charGrids.get(i)).toUpperCase();
            }
            if (includeCharNumbers) {
                printPaddedLine(bw, "PCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA >" + Globals.toHexString(i, 2), includeComments);
            }
            StringBuilder sbLine = new StringBuilder();
            sbLine.append("PAT").append(j).append(j < 10 ? "  " : (j < 100 ? " " : "")).append(" DATA ");
            sbLine.append(">").append(hexstr, 0, 4).append(",");
            sbLine.append(">").append(hexstr, 4, 8).append(",");
            sbLine.append(">").append(hexstr, 8, 12).append(",");
            sbLine.append(">").append(hexstr, 12, 16);
            printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(j, 2) + (i != j ? " (" + Globals.toHexString(i, 2) + ")" : "") : null);
        }
    }

    private void writeColors(BufferedWriter bw, Map<Integer, ArrayList<TransChar>> colorSets, boolean[] usedChars, int startChar, int endChar, boolean animate, boolean includeCharNumbers, boolean includeComments) throws Exception {
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Colorset Definitions", false);
            printPaddedLine(bw, "****************************************", false);
        }
        if (colorMode == COLOR_MODE_BITMAP) {
            for (int i = startChar; i <= (animate ? charColors.size() - 1 : endChar); i++) {
                int[][] charColors = this.charColors.get(i);
                if (charColors != null && !Globals.isColorGridEmpty(charColors)) {
                    if (includeCharNumbers) {
                        printPaddedLine(bw, "CCH" + i + (i < 10 ? "  " : (i < 100 ? " " : "")) + " DATA " + Globals.toHexString(i, 2), includeComments);
                    }
                    StringBuilder sbLine = new StringBuilder();
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
                    printPaddedLine(bw, sbLine.toString(), includeComments ? (usedChars[i] ? "" : "unused") : null);
                }
            }
        } else {
            int nColorSets = 0;
            for (int ckey : colorSets.keySet()) {
                int size = colorSets.get(ckey).size();
                nColorSets += size / 8 + (size % 8 == 0 ? 0 : 1);
            }
            printPaddedLine(bw, "CLRNUM DATA " + nColorSets, includeComments);
            boolean first = true;
            StringBuilder sbLine = new StringBuilder();
            int itemCount = 0;
            for (int ckey : colorSets.keySet()) {
                int size = colorSets.get(ckey).size();
                for (int n = 0; n < size / 8 + (size % 8 == 0 ? 0 : 1); n++) {
                    if (itemCount == 0) {
                        if (first) {
                            sbLine.append("CLRSET BYTE ");
                            first = false;
                        } else {
                            sbLine.append("       BYTE ");
                        }
                    }
                    if (itemCount > 0) {
                        sbLine.append(",");
                    }
                    sbLine.append(">");
                    sbLine.append(Globals.toHexString(ckey, 2));
                    itemCount++;
                    if (itemCount > 3) {
                        printPaddedLine(bw, sbLine.toString(), includeComments);
                        sbLine = new StringBuilder();
                        itemCount = 0;
                    }
                }
            }
            if (sbLine.length() > 0) {
                printPaddedLine(bw, sbLine.toString(), includeComments);
            }
        }
    }

    private void writeTransitionCharacters(BufferedWriter bw, TransChar[] remappedTransCharSet, int imax, boolean includeComments) throws Exception {
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Character Pairs (from, to) ", false);
            printPaddedLine(bw, "****************************************", false);
        }
        printPaddedLine(bw, "TCHNUM DATA " + (imax + 1), includeComments);
        for (int i = 0; i <= imax; i++) {
            TransChar transChar = remappedTransCharSet[i];
            if (transChar != null) {
                printPaddedLine(bw,
                        (i == 0 ? "TCHARS" : "      ") + " BYTE >" + Globals.toHexString(transChar.getFromChar(), 2) + ",>" + Globals.toHexString(transChar.getToChar(), 2),
                        !includeComments ? null :
                                "#" + Globals.toHexString(transChar.getIndex(), 2) +
                                        (colorMode != COLOR_MODE_BITMAP ? " color " + Globals.toHexString(transChar.getForeColor(), 1) + "/" + Globals.toHexString(transChar.getBackColor(), 1) : "") +
                                        (transChar.isInvert() ? " invert" : "") +
                                        (transChar.isColorsOK() ? "" : " ERROR")
                );
            } else {
                printPaddedLine(bw, (i == 0 ? "TCHARS" : "      ") + " BYTE >FF,>FF", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
            }
        }
    }

    private void writeInvertedCharacters(BufferedWriter bw, TransChar[] transCharSet, int imax, boolean includeComments) throws Exception {
        if (colorMode == COLOR_MODE_GRAPHICS_1) {
            boolean found = false;
            for (int i = 0; i <= imax && !found; i++) {
                TransChar transChar = transCharSet[i];
                if (transChar != null && transChar.isInvert()) {
                    found = true;
                }
            }
            if (found) {
                if (includeComments) {
                    printPaddedLine(bw, "*************************************************", false);
                    printPaddedLine(bw, "* Transition chars with inverted 'to' characters ", false);
                    printPaddedLine(bw, "*************************************************", false);
                }
                for (int i = 0; i <= imax; i++) {
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        printPaddedLine(bw,
                                (i == 0 ? "ICHARS" : "      ") + " BYTE >" + (transChar.isInvert() ? "FF" : "00"),
                                !includeComments ? null : "#" + Globals.toHexString(transChar.getIndex(), 2)
                        );
                    } else {
                        printPaddedLine(bw, (i == 0 ? "ICHARS" : "      ") + " BYTE >00", !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
                    }
                }
            }
        }
    }

    private void writeMap(BufferedWriter bw, ArrayList<int[][]> transMaps, int compression, boolean includeComments) throws Exception {
        if (includeComments) {
            printPaddedLine(bw, "****************************************", false);
            printPaddedLine(bw, "* Transition Map Data", false);
            printPaddedLine(bw, "****************************************", false);
        }
        for (int m = 0; m < transMaps.size(); m++) {
            StringBuilder sbLine = new StringBuilder();
            int[][] mapToSave = transMaps.get(m);
            if (includeComments) {
                printPaddedLine(bw, "* == Map #" + m + " == ", false);
            }
            printPaddedLine(bw, "MC" + m + (m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))) + " DATA " + mapEditor.getScreenColor(m), includeComments);
            sbLine.append("MS").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA >");
            sbLine.append(Globals.toHexString(mapToSave[0].length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave.length, 4));
            sbLine.append(",>").append(Globals.toHexString(mapToSave[0].length * mapToSave.length, 4));
            printPaddedLine(bw, sbLine.toString(), "Width, Height, Size");
            sbLine.delete(0, sbLine.length());
            if (compression == MagellanExportDialog.COMPRESSION_NONE) {
                boolean isFirstByte = true;
                for (int y = 0; y < mapToSave.length; y++) {
                    if (includeComments) {
                        printPaddedLine(bw, "* -- Map Row " + y + " -- ", false);
                    }
                    for (int cl = 0; cl < Math.ceil((double) mapToSave[y].length / 8); cl++) {
                        if (y == 0 && cl == 0) {
                            sbLine.append("MD").append(m).append(m < 10 ? "   " : (m < 100 ? "  " : (m < 1000 ? " " : ""))).append(" DATA ");
                        } else {
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
                            isFirstByte = true;
                        }
                        printPaddedLine(bw, sbLine.toString(), includeComments);
                        sbLine.delete(0, sbLine.length());
                    }
                }
                ArrayList<int[]> uniqueRows = new ArrayList<>();
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
                // System.out.println("Unique rows: " + uniqueRows.size());
            } else if (compression == MagellanExportDialog.COMPRESSION_RLE_BYTE) {
                // RLE compression (byte)
                // We assume all characters are < 128. If msb is set, the next byte determines
                // how many times (2 - 256) the current byte (with msb cleared) should be repeated.
                // A repeat count of 0 is used as end marker.
                // System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                int i = 0;
                int n = 0;
                int current;
                int last = -1;
                int count = 0;
                for (int[] row : mapToSave) {
                    for (int x = 0; x < mapToSave[0].length; x++) {
                        current = row[x];
                        if (last != -1) {
                            if (current == last && count < 255) {
                                // Same byte, increment count
                                count++;
                            } else if (count > 0) {
                                // End of run of bytes
                                i = printByte(bw, sbLine, i, last | 128);
                                i = printByte(bw, sbLine, i, count);
                                count = 0;
                                n += 2;
                            } else {
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
                } else {
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
                // System.out.println("Compressed size: " + n);
            } else if (compression == MagellanExportDialog.COMPRESSION_RLE_WORD) {
                // RLE compression (word)
                // We assume all characters are < 128. If msb of the MSB is set, the byte following the
                // current word determines how many times (2 - 256) the current word (with msb cleared)
                // should be repeated. A repeat count of 0 is used as end marker.
                // System.out.println("Uncompressed size: " + (mapToSave.length * mapToSave[0].length));
                int i = 0;
                int n = 0;
                int current;
                int last = -1;
                int count = 0;
                for (int[] row : mapToSave) {
                    for (int x = 0; x < mapToSave[0].length - 1; x += 2) {
                        current = row[x] << 8 | row[x + 1];
                        if (last != -1) {
                            if (current == last && count < 255) {
                                // Same word, increment count
                                count++;
                            } else if (count > 0) {
                                // End of run of words
                                i = printByte(bw, sbLine, i, (last & 0xFF00) >> 8 | 128);
                                i = printByte(bw, sbLine, i, last & 0x00FF);
                                i = printByte(bw, sbLine, i, count);
                                count = 0;
                                n += 3;
                            } else {
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
                } else {
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
                // System.out.println("Compressed size: " + n);
            } else {
                throw new RuntimeException("Compression mode not yet supported.");
            }
        }
    }

    private void writeScrolledPatterns(BufferedWriter bw, ArrayList<Integer> charMap, TransChar[] transCharSet, int imax, TransitionType transitionType, boolean includeComments, int frames, boolean animate) throws Exception {
        if (frames > 0 || transitionType.getyOffset() != 0 && frames == -1) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Scrolled Character Patterns", false);
                printPaddedLine(bw, "****************************************", false);
            }
            if (frames > 0) {
                for (int f = 0; f < frames; f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(transitionType.getyOffset() != 0 ? "V" : "H").append("PFRM").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSet[i];
                        if (transChar != null) {
                            int[][] fromGrid = charGrids.get(charMap.get(transChar.getFromChar()) + (animate ? f * 32 : 0));
                            int[][] toGrid = charGrids.get(charMap.get(transChar.getToChar()) + (animate ? f * 32 : 0));
                            if (transChar.isInvert()) {
                                toGrid = Globals.cloneGrid(toGrid);
                                Globals.invertGrid(toGrid, 1);
                            }
                            int[][] scrollGrid = new int[8][8];
                            if (transitionType.getyOffset() != 0) {
                                int y1 = 0;
                                if (transitionType.getyOffset() > 0) {
                                    for (int y = offset; y < 8; y++) {
                                        System.arraycopy(fromGrid[y], 0, scrollGrid[y1], 0, 8);
                                        y1++;
                                    }
                                    for (int y = 0; y < offset; y++) {
                                        System.arraycopy(toGrid[y], 0, scrollGrid[y1], 0, 8);
                                        y1++;
                                    }
                                } else {
                                    for (int y = 8 - offset; y < 8; y++) {
                                        System.arraycopy(toGrid[y], 0, scrollGrid[y1], 0, 8);
                                        y1++;
                                    }
                                    for (int y = 0; y < 8 - offset; y++) {
                                        System.arraycopy(fromGrid[y], 0, scrollGrid[y1], 0, 8);
                                        y1++;
                                    }
                                }
                            }
                            else {
                                int x1 = 0;
                                if (transitionType.getxOffset() > 0) {
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
                                } else {
                                    for (int x = 8 - offset; x < 8; x++) {
                                        for (int y = 0; y < 8; y++) {
                                            scrollGrid[y][x1] = toGrid[y][x];
                                        }
                                        x1++;
                                    }
                                    for (int x = 0; x < 8 - offset; x++) {
                                        for (int y = 0; y < 8; y++) {
                                            scrollGrid[y][x1] = fromGrid[y][x];
                                        }
                                        x1++;
                                    }
                                }
                            }
                            hexstr = Globals.getHexString(scrollGrid).toUpperCase();
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr, 0, 4).append(",");
                        sbLine.append(">").append(hexstr, 4, 8).append(",");
                        sbLine.append(">").append(hexstr, 8, 12).append(",");
                        sbLine.append(">").append(hexstr, 12, 16);
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            // Pattern strips
            else {
                for (int i = 0; i <= imax; i++) {
                    String hexstr;
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        hexstr =
                            Globals.getHexString(charGrids.get(charMap.get(transChar.getToChar()))).toUpperCase() +
                            Globals.getHexString(charGrids.get(charMap.get(transChar.getFromChar()))).toUpperCase();
                    }
                    else {
                        hexstr = Globals.BLANKCHAR + Globals.BLANKCHAR;
                    }
                    StringBuilder sbLine = new StringBuilder();
                    sbLine.append(i == 0 ? "PSTRIP" : "      ").append(" DATA ");
                    sbLine.append(">").append(hexstr, 0, 4).append(",");
                    sbLine.append(">").append(hexstr, 4, 8).append(",");
                    sbLine.append(">").append(hexstr, 8, 12).append(",");
                    sbLine.append(">").append(hexstr, 12, 16);
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? "unused" : "- " + Globals.toHexString(transChar.getToChar(), 2)) : null);
                    sbLine = new StringBuilder();
                    sbLine.append("       DATA ");
                    sbLine.append(">").append(hexstr, 16, 20).append(",");
                    sbLine.append(">").append(hexstr, 20, 24).append(",");
                    sbLine.append(">").append(hexstr, 24, 28).append(",");
                    sbLine.append(">").append(hexstr, 28, 32);
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? " unused" : "- " + Globals.toHexString(transChar.getFromChar(), 2)) : null);
                }
            }
        }
    }

    private void writeScrolledColors(BufferedWriter bw, ArrayList<Integer> charMap, TransChar[] transCharSet, int imax, TransitionType transitionType, boolean includeComments, int frames, boolean animate) throws Exception {
        boolean vertical = transitionType.getyOffset() != 0;
        if ((frames > 0 || vertical && frames == -1) && colorMode == COLOR_MODE_BITMAP) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Scrolled Character Colors", false);
                printPaddedLine(bw, "****************************************", false);
            }
            if (frames > 0) {
                for (int f = 0; f < (vertical ? frames : 1); f++) {
                    int offset = f * 8 / frames;
                    for (int i = 0; i <= imax; i++) {
                        StringBuilder sbLine = new StringBuilder();
                        if (i == 0) {
                            sbLine.append(vertical ? "V" : "H").append("CFRM").append(f);
                        }
                        else {
                            sbLine.append("      ");
                        }
                        sbLine.append(" DATA ");
                        String hexstr;
                        TransChar transChar = transCharSet[i];
                        if (transChar != null) {
                            int[][] fromGrid = charColors.get(charMap.get(transChar.getFromChar()) + (animate ? f * 32 : 0));
                            int[][] toGrid = charColors.get(charMap.get(transChar.getToChar()) + (animate ? f * 32 : 0));
                            hexstr = "";
                            if (vertical) {
                                for (int y = 8 - offset; y < 8; y++) {
                                    hexstr += Integer.toHexString(toGrid[y][1]).toUpperCase();
                                    hexstr += Integer.toHexString(toGrid[y][0]).toUpperCase();
                                }
                                for (int y = 0; y < 8 - offset; y++) {
                                    hexstr += Integer.toHexString(fromGrid[y][1]).toUpperCase();
                                    hexstr += Integer.toHexString(fromGrid[y][0]).toUpperCase();
                                }
                            }
                            else {
                                for (int y = 0; y < 8; y++) {
                                    int foreColor = 0;
                                    int backColor = 0;
                                    int fromForeColor = fromGrid[y][Globals.INDEX_CLR_FORE] != 0 ? fromGrid[y][Globals.INDEX_CLR_FORE] : mapEditor.getColorScreen();
                                    int toForeColor = toGrid[y][Globals.INDEX_CLR_FORE] != 0 ? toGrid[y][Globals.INDEX_CLR_FORE] : mapEditor.getColorScreen();
                                    if (fromForeColor == toForeColor) {
                                        foreColor = fromForeColor;
                                    }
                                    else if (!Globals.arrayContains(charGrids.get(charMap.get(transChar.getFromChar()))[y], Globals.INDEX_CLR_FORE)) {
                                        foreColor = toForeColor;
                                    }
                                    else if (!Globals.arrayContains(charGrids.get(charMap.get(transChar.getToChar()))[y], Globals.INDEX_CLR_FORE)) {
                                        foreColor = fromForeColor;
                                    }
                                    int fromBackColor = fromGrid[y][Globals.INDEX_CLR_BACK] != 0 ? fromGrid[y][Globals.INDEX_CLR_BACK] : mapEditor.getColorScreen();
                                    int toBackColor = toGrid[y][Globals.INDEX_CLR_BACK] != 0 ? toGrid[y][Globals.INDEX_CLR_BACK] : mapEditor.getColorScreen();
                                    if (fromBackColor == toBackColor) {
                                        backColor = fromBackColor;
                                    }
                                    else if (!Globals.arrayContains(charGrids.get(charMap.get(transChar.getFromChar()))[y], Globals.INDEX_CLR_BACK)) {
                                        backColor = toBackColor;
                                    }
                                    else if (!Globals.arrayContains(charGrids.get(charMap.get(transChar.getToChar()))[y], Globals.INDEX_CLR_BACK)) {
                                        backColor = fromBackColor;
                                    }
                                    hexstr += Integer.toHexString(foreColor).toUpperCase();
                                    hexstr += Integer.toHexString(backColor).toUpperCase();
                                }
                            }
                        }
                        else {
                            hexstr = Globals.BLANKCHAR;
                        }
                        sbLine.append(">").append(hexstr, 0, 4).append(",");
                        sbLine.append(">").append(hexstr, 4, 8).append(",");
                        sbLine.append(">").append(hexstr, 8, 12).append(",");
                        sbLine.append(">").append(hexstr, 12, 16);
                        printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + (transChar == null ? " unused" : "") : null);
                    }
                }
            }
            // Color strips
            else {
                for (int i = 0; i <= imax; i++) {
                    String hexstr = "";
                    TransChar transChar = transCharSet[i];
                    if (transChar != null) {
                        int[][] toColors = charColors.get(charMap.get(transChar.getToChar()));
                        for (int row = 0; row < 8; row++) {
                            hexstr += Integer.toHexString(toColors[row][1]) + Integer.toHexString(toColors[row][0]);
                        }
                        int[][] fromColors = charColors.get(charMap.get(transChar.getFromChar()));
                        for (int row = 0; row < 8; row++) {
                            hexstr += Integer.toHexString(fromColors[row][1]) + Integer.toHexString(fromColors[row][0]);
                        }
                    }
                    else {
                        hexstr = Globals.BLANKCHAR + Globals.BLANKCHAR;
                    }
                    hexstr = hexstr.toUpperCase();
                    StringBuilder sbLine = new StringBuilder();
                    sbLine.append(i == 0 ? "CSTRIP" : "      ").append(" DATA ");
                    sbLine.append(">").append(hexstr, 0, 4).append(",");
                    sbLine.append(">").append(hexstr, 4, 8).append(",");
                    sbLine.append(">").append(hexstr, 8, 12).append(",");
                    sbLine.append(">").append(hexstr, 12, 16);
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? "unused" : "- " + Globals.toHexString(transChar.getToChar(), 2)) : null);
                    sbLine = new StringBuilder();
                    sbLine.append("       DATA ");
                    sbLine.append(">").append(hexstr, 16, 20).append(",");
                    sbLine.append(">").append(hexstr, 20, 24).append(",");
                    sbLine.append(">").append(hexstr, 24, 28).append(",");
                    sbLine.append(">").append(hexstr, 28, 32);
                    printPaddedLine(bw, sbLine.toString(), includeComments ? "#" + Globals.toHexString(i, 2) + " " + (transChar == null ? " unused" : "- " + Globals.toHexString(transChar.getFromChar(), 2)) : null);
                }
            }
        }
    }
}
