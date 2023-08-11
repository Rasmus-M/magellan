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
        if (transitionType == TransitionType.ISOMETRIC) {
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
        boolean allColorsOK = findCharacterTransitions(transMaps, transCharSet, transChars, usedChars, colorSets, startAndEndChar, currMapOnly, transitionType, wrap);
        if (!allColorsOK) {
            JOptionPane.showMessageDialog(null, "Warning - Some character transitions have incompatible colors. This may cause color spills when the map is scrolled.", "Invalid Color Transitions", JOptionPane.INFORMATION_MESSAGE);
        }
        int imax = getMaxIndex(transCharSet);
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
        writeTransitionCharacters(bw, remappedTransCharSet, imax, includeComments, transitionType);
        writeInvertedCharacters(bw, remappedTransCharSet, imax, includeComments);
        writeMap(bw, transMaps, compression, includeComments);
        writeScrolledPatterns(bw, remappedChars, remappedTransCharSet, imax, transitionType, includeComments, frames, animate);
        writeScrolledColors(bw, remappedChars, remappedTransCharSet, imax, transitionType, includeComments, frames, animate);
        bw.flush();
        bw.close();
    }

    private boolean findCharacterTransitions(ArrayList<int[][]> transMaps, TransChar[] transCharSet, Map<String, TransChar> transChars, boolean[] usedChars, Map<Integer, ArrayList<TransChar>> colorSets, int[] startAndEndChar, boolean currMapOnly, TransitionType transitionType, boolean wrap) throws Exception {
        boolean allColorsOK = true;
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                int[][] mapData = mapEditor.getMapData(m);
                if (mapData.length > 1 && mapData[0].length > 1) {
                    allColorsOK &= findCharacterTransitionsForMap(mapData, transChars, usedChars, colorSets, startAndEndChar, transitionType, wrap);
                }
            }
        }
        // Add to transCharSet
        int i = getMaxIndex(transCharSet) + 1;
        if (colorMode == COLOR_MODE_BITMAP) {
            for (TransChar transChar : transChars.values()) {
                addTransCharToSet(transChar, transCharSet, i++);
            }
        } else {
            // Organize into color sets
            for (ArrayList<TransChar> colorSet : colorSets.values()) {
                for (TransChar transChar : colorSet) {
                    addTransCharToSet(transChar, transCharSet, i++);
                }
                while (i % 8 != 0) {
                    i++;
                }
                if (i > 255) {
                    throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
                }
            }
        }
        // Create maps
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                int[][] mapData = mapEditor.getMapData(m);
                if (mapData.length > 1 && mapData[0].length > 1) {
                    createTransitionMap(mapData, transChars, transMaps, transitionType, wrap);
                }
            }
        }
        return allColorsOK;
    }

    private boolean findCharacterTransitionsForMap(int[][] mapData, Map<String, TransChar> transChars, boolean[] usedChars, Map<Integer, ArrayList<TransChar>> colorSets, int[] startAndEndChar, TransitionType transitionType, boolean wrap) throws Exception {
        boolean allColorsOK = true;
        int width = mapData[0].length;
        int height = mapData.length;
        for (int y = transitionType.getYStart(wrap); y < height - transitionType.getYEnd(wrap); y++) {
            for (int x = transitionType.getXStart(wrap); x < width - transitionType.getXEnd(wrap); x++) {
                TransChar newTransChar = new TransChar(transitionType, x, y, mapData);
                updateUsedChars(newTransChar, usedChars, startAndEndChar);
                TransChar transChar = transChars.get(newTransChar.getKey());
                if (transChar != null) {
                    transChar.incCount();
                } else {
                    determineColorsForTransChar(newTransChar, transitionType, colorSets);
                    transChars.put(newTransChar.getKey(), newTransChar);
                    if (!newTransChar.isColorsOK()) {
                        allColorsOK = false;
                    }
                }
            }
        }
        return allColorsOK;
    }

    private void createTransitionMap(int[][] mapData, Map<String, TransChar> transChars, ArrayList<int[][]> transMaps, TransitionType transitionType, boolean wrap) {
        int width = mapData[0].length;
        int height = mapData.length;
        int xStart = transitionType.getXStart(wrap);
        int yStart = transitionType.getYStart(wrap);
        int xEnd = transitionType.getXEnd(wrap);
        int yEnd = transitionType.getYEnd(wrap);
        int newWidth = width - xEnd - xStart;
        int newHeight = height - yEnd - yStart;
        int[][] transMap = new int[newHeight][newWidth];
        transMaps.add(transMap);
        for (int y = yStart; y < height - yEnd; y++) {
            for (int x = xStart; x < width - xEnd; x++) {
                TransChar testTransChar = new TransChar(transitionType, x, y, mapData);
                TransChar transChar = transChars.get(testTransChar.getKey());
                transMap[y - yStart][x - xStart] = transChar.getIndex();
            }
        }
    }

    private int getMaxIndex(TransChar[] transCharSet) {
        for (int i = transCharSet.length - 1; i >= 0; i--) {
            if (transCharSet[i] != null) {
                return i;
            }
        }
        return -1;
    }

    private void updateUsedChars(TransChar transChar, boolean[] usedChars, int[] startAndEndChar) {
        int fromChar = transChar.getFromChar();
        if (fromChar < startAndEndChar[0]) {
            startAndEndChar[0] = fromChar;
        }
        if (fromChar > startAndEndChar[1]) {
            startAndEndChar[1] = fromChar;
        }
        usedChars[fromChar] = true;
        int toChar = transChar.getToChar();
        if (toChar < startAndEndChar[0]) {
            startAndEndChar[0] = toChar;
        }
        if (toChar > startAndEndChar[1]) {
            startAndEndChar[1] = toChar;
        }
        usedChars[toChar] = true;
    }

    private void addTransCharToSet(TransChar transChar, TransChar[] transCharSet, int index) throws Exception {
        if (index < transCharSet.length) {
            transChar.setIndex(index);
            transCharSet[index] = transChar;
        } else {
            throw new Exception("Character Set Full: Scrolling this map requires more than 32 color sets.");
        }
    }

    private void determineColorsForTransChar(TransChar transChar, TransitionType transitionType, Map<Integer, ArrayList<TransChar>> colorSets) {
        int fromChar = transChar.getFromChar();
        if (colorMode == COLOR_MODE_BITMAP) {
            determineColorsForTransCharInBitmapMode(transChar, fromChar, transChar.getToChar(), transitionType.getXOffset() != 0);
        } else {
            for (int toChar : transChar.getToChars()) {
                determineColorsForTransCharInGraphicsMode(transChar, fromChar, toChar);
                if (!transChar.isColorsOK()) {
                    break;
                }
            }
            int ckey = transChar.getBackColor() + (transChar.getForeColor() << 4);
            ArrayList<TransChar> colorSet = colorSets.computeIfAbsent(ckey, k -> new ArrayList<>());
            colorSet.add(transChar);
        }
    }

    private void determineColorsForTransCharInBitmapMode(TransChar transChar, int fromChar, int toChar, boolean horizontal) {
        int[][] charColors = new int[8][8];
        boolean colorsOK = true;
        if (horizontal) {
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
                }
            }
        }
        transChar.setColorsOK(colorsOK);
        transChar.setColorGrid(charColors);
    }

    private void determineColorsForTransCharInGraphicsMode(TransChar transChar, int fromChar, int toChar) {
        boolean colorsOK = true;
        boolean invert = false;
        int screenColor = mapEditor.getColorScreen();
        int[] fromClrSet = clrSets[fromChar / 8];
        int[] toClrSet = clrSets[toChar / 8];
        int foreColor;
        int backColor;
        // Fore color
        int fromForeColor = fromClrSet[Globals.INDEX_CLR_FORE] != 0 ? fromClrSet[Globals.INDEX_CLR_FORE] : screenColor;
        int toForeColor = toClrSet[Globals.INDEX_CLR_FORE] != 0 ? toClrSet[Globals.INDEX_CLR_FORE] : screenColor;
        if (fromForeColor == toForeColor) {
            foreColor = fromForeColor; // Same fore colors
        } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_FORE)) {
            foreColor = toForeColor; // From grid empty - use to color
        } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_FORE)) {
            foreColor = fromForeColor; // To grid empty - user from color
        } else {
            foreColor = fromForeColor;
            colorsOK = false;
            System.out.println("Colors not OK: fromChar=" + fromChar + " toChar=" + toChar + " fromForeColor=" + fromForeColor + " toForeColor=" + toForeColor);
        }
        // Back color
        int fromBackColor = fromClrSet[Globals.INDEX_CLR_BACK] != 0 ? fromClrSet[Globals.INDEX_CLR_BACK] : screenColor;
        int toBackColor = toClrSet[Globals.INDEX_CLR_BACK] != 0 ? toClrSet[Globals.INDEX_CLR_BACK] : screenColor;
        if (fromBackColor == toBackColor) {
            backColor = fromBackColor; // Same back colors
        } else if (!Globals.arrayContains(charGrids.get(fromChar), Globals.INDEX_CLR_BACK)) {
            backColor = toBackColor; // From grid full - use to color
        } else if (!Globals.arrayContains(charGrids.get(toChar), Globals.INDEX_CLR_BACK)) {
            backColor = fromBackColor; // To grid full - use from color
        } else {
            backColor = fromBackColor;
            colorsOK = false;
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
            }
            invert = colorsOK;
        }
        boolean initialized = transChar.getForeColor() != -1 || transChar.getBackColor() != -1;
        if (initialized && (
                transChar.getForeColor() != foreColor && transChar.isForeColorUsed() ||
                transChar.getBackColor() != backColor && transChar.isBackColorUsed() ||
                transChar.isInvert() != invert
        )) {
            transChar.setColorsOK(false);
        } else {
            transChar.setColorsOK(colorsOK);
            transChar.setForeColor(foreColor);
            transChar.setForeColorUsed(Globals.arrayContains(charGrids.get(fromChar), foreColor) || Globals.arrayContains(charGrids.get(toChar), foreColor));
            transChar.setBackColor(backColor);
            transChar.setBackColorUsed(Globals.arrayContains(charGrids.get(fromChar), backColor) || Globals.arrayContains(charGrids.get(toChar), backColor));
            transChar.setInvert(invert);
        }
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
                        int[] toChars = transChar.getToChars();
                        int[] remappedToChars = new int[toChars.length];
                        for (int i = 0; i < toChars.length; i++) {
                            if (toChars[i] == mapFrom) {
                                remappedToChars[i] = mapTo;
                            } else {
                                remappedToChars[i] = toChars[i];
                            }
                        }
                        transChar.setToChars(remappedToChars);
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

    private void writeTransitionCharacters(BufferedWriter bw, TransChar[] remappedTransCharSet, int imax, boolean includeComments, TransitionType transitionType) throws Exception {
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
                    (i == 0 ? "TCHARS" : "      ") + " BYTE " + transChar,
                    !includeComments ? null :
                        "#" + Globals.toHexString(transChar.getIndex(), 2) +
                            (colorMode != COLOR_MODE_BITMAP ? " color " + Globals.toHexString(transChar.getForeColor(), 1) + "/" + Globals.toHexString(transChar.getBackColor(), 1) : "") +
                            (transChar.isInvert() ? " invert" : "") +
                            (transChar.isColorsOK() ? "" : " ERROR")
                );
            } else {
                printPaddedLine(bw, (i == 0 ? "TCHARS" : "      ") + " BYTE >FF" + Globals.repeat(",>FF", transitionType.getSize()), !includeComments ? null : "#" + Globals.toHexString(i, 2) + " unused");
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
        if (transitionType == TransitionType.ISOMETRIC) {
            return;
        }
        if ((frames > 0 || transitionType.getYOffset() != 0 && frames == -1)) {
            if (includeComments) {
                printPaddedLine(bw, "****************************************", false);
                printPaddedLine(bw, "* Scrolled Character Patterns", false);
                printPaddedLine(bw, "****************************************", false);
            }
            if (transitionType.getSize() == 1) {
                if (frames > 0) {
                    for (int f = 0; f < frames; f++) {
                        int offset = f * 8 / frames;
                        for (int i = 0; i <= imax; i++) {
                            StringBuilder sbLine = new StringBuilder();
                            if (i == 0) {
                                sbLine.append(transitionType.getYOffset() != 0 ? "V" : "H").append("PFRM").append(f);
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
                                if (transitionType.getYOffset() != 0) {
                                    int y1 = 0;
                                    if (transitionType.getYOffset() > 0) {
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
                                    if (transitionType.getXOffset() > 0) {
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
            } else {
                int hFrames = transitionType.getWidth() > 1 ? frames : 1;
                int vFrames = transitionType.getHeight() > 1 ? frames : 1;
                for (int hFrame = 0; hFrame < hFrames; hFrame++) {
                    int hOffset = hFrame * 8 / hFrames;
                    for (int vFrame = 0; vFrame < vFrames; vFrame++) {
                        int vOffset = vFrame * 8 / vFrames;
                        for (int i = 0; i <= imax; i++) {
                            StringBuilder sbLine = new StringBuilder();
                            if (i == 0) {
                                sbLine.append("PFRM").append(hFrame).append(vFrame);
                            } else {
                                sbLine.append("      ");
                            }
                            sbLine.append(" DATA ");
                            String hexstr;
                            TransChar transChar = transCharSet[i];
                            if (transChar != null) {
                                int[][] grid = new int[transitionType.getHeight() * 8][transitionType.getWidth() * 8];
                                Globals.copyGrid(charGrids.get(charMap.get(transChar.getFromChar())), grid, transitionType.getBaseX() * 8, transitionType.getBaseY() * 8);
                                for (int t = 0; t < transitionType.getSize(); t++) {
                                    Globals.copyGrid(charGrids.get(charMap.get(transChar.getToChars()[t])), grid, (transitionType.getBaseX() + transitionType.getXOffsets()[t]) * 8, (transitionType.getBaseY() + transitionType.getYOffsets()[t]) * 8);
                                }
                                int[][] scrollGrid = new int[8][8];
                                Globals.copyGrid(grid, scrollGrid, transitionType.getBaseX() * 8 + hOffset * (transitionType.getBaseX() == 0 ? 1 : -1), transitionType.getBaseY() * 8 + vOffset * (transitionType.getBaseY() == 0 ? 1 : -1), 0, 0, 8, 8);
                                hexstr = Globals.getHexString(scrollGrid).toUpperCase();
                            } else {
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
            }
        }
    }

    private void writeScrolledColors(BufferedWriter bw, ArrayList<Integer> charMap, TransChar[] transCharSet, int imax, TransitionType transitionType, boolean includeComments, int frames, boolean animate) throws Exception {
        if (transitionType.getSize() > 1) {
            return;
        }
        boolean vertical = transitionType.getYOffset() != 0;
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
