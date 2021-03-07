package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.Magellan.COLOR_MODE_ECM_2;

public class MapImageFileImporter extends Importer {

    public MapImageFileImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readMapImageFile(File mapImageFile, int startIndex, int endIndex, int startPalette, int tolerance) throws Exception {
        startIndex &= 0xf8;
        int maxIndex = endIndex - startIndex;
        BufferedImage bufferedImage = ImageIO.read(mapImageFile);
        int width = bufferedImage.getWidth() / 8;
        int height = bufferedImage.getHeight() / 8;
        mapdMain.setGridWidth(width);
        mapdMain.setGridHeight(height);
        int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
        // Find unique patterns based on RGB values
        // Produces a list of pairs
        // First element of the pair is the RGB grid
        // Second element of the pair is the palette, which is only used for the ECM cases
        ArrayList<Pair<int[][], Pair<Set<Integer>, Integer>>> patterns = new ArrayList<Pair<int[][], Pair<Set<Integer>, Integer>>>();
        for (int o = 0; o < 2; o++) {
            for (int n = 0; n + o < height; n += 2) {
                int y1 = (n + o) * 8;
                for (int m = 0; m < width; m++) {
                    int x1 = m * 8;
                    int[][] rgbGrid = new int[8][8];
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            rgbGrid[y][x] = bufferedImage.getRGB(x + x1, y + y1);
                        }
                    }
                    // Find exact match in the existing characters
                    int ch = -1;
                    for (int i = 0; i < patterns.size() && ch == -1; i++) {
                        if (Globals.gridEquals(rgbGrid, patterns.get(i).getFirst())) {
                            ch = i;
                        }
                    }
                    // If no exact match, find the best match, and match if within tolerance
                    int chBest = 0;
                    if (ch == -1) {
                        double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < patterns.size(); i++) {
                            double dist = Globals.gridColorDistance(rgbGrid, patterns.get(i).getFirst());
                            if (dist < minDist) {
                                minDist = dist;
                                chBest = i;
                                if (minDist < tolerance) {
                                    ch = i;
                                }
                            }
                        }
                    }
                    // If still no match, try to add it
                    if (ch == -1) {
                        // Is there room?
                        if (patterns.size() <= maxIndex) {
                            // At this point only the grid is initialized, the palette is set to null
                            patterns.add(new Pair<int[][], Pair<Set<Integer>, Integer>>(rgbGrid, null));
                            ch = patterns.size() - 1;
                        }
                        // else use best match
                        else {
                            ch = chBest;
                        }
                    }
                    mapData[n + o][m] = Math.min(ch, maxIndex);
                }
            }
        }
        System.out.println("Number of unique patterns: " + patterns.size());
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                // Divide into char/color sets
                // Each charSet is stored as a pair
                // The first element is an integer key composed of the background and foreground colors
                // The second element is a list of indexes of characters that are members of the character set
                ArrayList<Pair<Integer, ArrayList<Integer>>> charSets = new ArrayList<Pair<Integer, java.util.ArrayList<Integer>>>();
                int[][][] charGrids = new int[256][8][8];
                for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
                    // Build a palette of the pattern grid
                    // Each entry in the palette consists of a RGB color and a count
                    int[][] rgbGrid = patterns.get(i).getFirst();
                    int[][] palette = new int[64][2]; // Max 64 unique colors in a 8x8 grid
                    int iMax = -1;
                    for (int[] row : rgbGrid) {
                        for (int color : row) {
                            boolean found = false;
                            for (int p = 0; p <= iMax && !found; p++) {
                                if (palette[p][0] == color) {
                                    palette[p][1]++;
                                    found = true;
                                }
                            }
                            if (!found) {
                                iMax++;
                                palette[iMax][0] = color;
                                palette[iMax][1] = 1;
                            }
                        }
                    }
                    // Background and foreground are set to the two most used colors of the palette
                    Globals.sortGrid(palette);
                    int backColorIndex = Globals.getClosestColorIndex(new Color(palette[0][0], true), TIGlobals.TI_PALETTE);
                    int foreColorIndex = iMax > 0 ? Globals.getClosestColorIndex(new Color(palette[1][0], true), TIGlobals.TI_PALETTE, backColorIndex) : backColorIndex;
                    int screenColor = mapdMain.getColorScreen();
                    boolean mono = backColorIndex == foreColorIndex;
                    if (!mono && foreColorIndex == screenColor) {
                        int tmp = foreColorIndex;
                        foreColorIndex = backColorIndex;
                        backColorIndex = tmp;
                    }
                    int key = (backColorIndex * 16) | foreColorIndex;
                    ArrayList<Integer> charSet = null;
                    boolean foundButNoRoom = false;
                    for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                        if (charSet == null) {
                            int otherKey = charSetPair.getFirst();
                            int otherBackColorIndex = otherKey / 16;
                            int otherForeColorIndex = otherKey % 16;
                            boolean otherMono = otherBackColorIndex == otherForeColorIndex;
                            // Match to key of existing character color set
                            // if monochrome it's enough that one of the colors (background or foreground) matches
                            if (
                                    otherKey == key
                                            ||
                                            (mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)
                                            ||
                                            otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex
                            ) {
                                charSet = charSetPair.getSecond();
                                if (charSet.size() < 8) {
                                    charSet.add(i);
                                    if ((mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)) {
                                        if (mono) {
                                            foreColorIndex = otherForeColorIndex;
                                            backColorIndex = otherBackColorIndex;
                                        } else {
                                            // Update foreground color of the other character set
                                            if (otherBackColorIndex == backColorIndex) {
                                                charSetPair.setFirst((otherBackColorIndex * 16) | foreColorIndex);
                                            } else {
                                                charSetPair.setFirst((otherBackColorIndex * 16) | backColorIndex);
                                                foreColorIndex = backColorIndex;
                                            }
                                            backColorIndex = otherBackColorIndex;
                                        }
                                    } else if (otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex) {
                                        // Inverse match
                                        foreColorIndex = otherForeColorIndex;
                                        backColorIndex = otherBackColorIndex;
                                    }
                                } else {
                                    charSet = null;
                                    foundButNoRoom = true;
                                }
                            }
                        }
                    }
                    if (charSet == null) {
                        // An exact match with an existing character set was not found
                        if (charSets.size() < (maxIndex + 1) / 8) {
                            // Make a new one if room
                            charSet = new ArrayList<Integer>(8);
                            charSet.add(i);
                            charSets.add(new Pair<Integer, ArrayList<Integer>>(key, charSet));
                        } else {
                            if (foundButNoRoom) {
                                // Remap the character
                                rgbGrid = patterns.get(i).getFirst();
                                int ch = -1;
                                double minDist = Double.MAX_VALUE;
                                for (int j = 0; j < i; j++) {
                                    if (patterns.get(j) != null) {
                                        double dist = Globals.gridColorDistance(rgbGrid, patterns.get(j).getFirst());
                                        if (dist < minDist) {
                                            minDist = dist;
                                            ch = j;
                                        }
                                    }
                                }
                                System.out.println("Char " + i + " remapped to " + ch);
                                for (int n = 0; n < height; n++) {
                                    for (int m = 0; m < width; m++) {
                                        if (mapData[n][m] == i) {
                                            mapData[n][m] = ch;
                                        }
                                    }
                                }
                                patterns.set(i, null);
                            } else {
                                // Find the best matching set, i.e. the one with the smallest color distance
                                double minDist = Double.MAX_VALUE;
                                int backIndex1 = backColorIndex;
                                int foreIndex1 = foreColorIndex;
                                for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                                    if (charSetPair.getSecond().size() < 8) {
                                        int k = charSetPair.getFirst();
                                        int foreIndex2 = k / 16;
                                        int backIndex2 = k % 16;
                                        double dist =
                                                Globals.colorDistance(TIGlobals.TI_PALETTE[backIndex2], TIGlobals.TI_PALETTE[backIndex1]) +
                                                        Globals.colorDistance(TIGlobals.TI_PALETTE[foreIndex2], TIGlobals.TI_PALETTE[foreIndex1]);
                                        if (dist < minDist) {
                                            minDist = dist;
                                            charSet = charSetPair.getSecond();
                                            backColorIndex = foreIndex2;
                                            foreColorIndex = backIndex2;
                                        }
                                    }
                                }
                                charSet.add(i);
                            }
                        }
                    }
                    if (charSet != null) {
                        // Update pattern to match character color set
                        int[][] charGrid = charGrids[i];
                        for (int y = 0; y < rgbGrid.length; y++) {
                            int[] row = rgbGrid[y];
                            for (int x = 0; x < row.length; x++) {
                                Color color = new Color(row[x], true);
                                charGrid[y][x] = Globals.colorDistance(color, TIGlobals.TI_PALETTE[backColorIndex]) <= Globals.colorDistance(color, TIGlobals.TI_PALETTE[foreColorIndex]) ? 0 : 1;
                            }
                        }
                        // Check if pattern is now a duplicate of another pattern in the character color set
                        boolean found = false;
                        for (int j = 0; j < charSet.size() - 2 && !found; j++) {
                            int k = charSet.get(j);
                            if (Globals.gridEquals(charGrid, charGrids[k])) {
                                // Found the same pattern - remove this one
                                charSet.remove(charSet.size() - 1);
                                for (int n = 0; n < height; n++) {
                                    for (int m = 0; m < width; m++) {
                                        if (mapData[n][m] == i) {
                                            mapData[n][m] = k;
                                        }
                                    }
                                }
                                patterns.set(i, null);
                                found = true;
                            }
                        }
                    }
                }
                // Maps old char numbers to new
                Map<Integer, Integer> charMap = new HashMap<Integer, Integer>();
                int colSet = startIndex / 8;
                int ch = 0;
                for (Pair<Integer, ArrayList<Integer>> charSetPair : charSets) {
                    ArrayList<Integer> charSet = charSetPair.getSecond();
                    for (Integer n : charSet) {
                        charMap.put(n, ch);
                        hmCharGrids.put(ch + startIndex, charGrids[n]);
                        ch++;
                    }
                    // Fill up character color set
                    while (ch % 8 != 0) {
                        hmCharGrids.put(ch + startIndex, new int[8][8]);
                        ch++;
                    }
                    int key = charSetPair.getFirst();
                    clrSets[colSet++] = new int[] {key / 16, key % 16};
                }
                // Remap chars
                for (int n = 0; n < height; n++) {
                    for (int m = 0; m < width; m++) {
                        Integer c = charMap.get(mapData[n][m]);
                        if (c != null) {
                            mapData[n][m] = c;
                        }
                        else {
                            System.out.println(mapData[n][m] + " at (" + m + "," + n + ") not found in charmap");
                        }
                    }
                }
                break;
            case COLOR_MODE_BITMAP:
                for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
                    int[][] rgbGrid = patterns.get(i).getFirst();
                    int[][] charGrid = hmCharGrids.get(i + startIndex);
                    if (charGrid == null) {
                        charGrid = new int[8][8];
                        hmCharGrids.put(i + startIndex, charGrid);
                    }
                    int[][] charColors = hmCharColors.get(i + startIndex);
                    if (charColors == null) {
                        charColors = new int[8][2];
                        hmCharColors.put(i + startIndex, charColors);
                    }
                    int screenColor = mapdMain.getColorScreen();
                    for (int y = 0; y < rgbGrid.length; y++) {
                        int[] row = rgbGrid[y];
                        int[][] palette = new int[8][2];
                        int iMax = -1;
                        for (Integer color : row) {
                            boolean found = false;
                            for (int p = 0; p <= iMax && !found; p++) {
                                if (palette[p][0] == color) {
                                    palette[p][1]++;
                                    found = true;
                                }
                            }
                            if (!found) {
                                iMax++;
                                palette[iMax][0] = color;
                                palette[iMax][1] = 1;
                            }
                        }
                        Globals.sortGrid(palette);
                        int backColor = Globals.getClosestColorIndex(new Color(palette[0][0]), TIGlobals.TI_PALETTE);
                        int foreColor = iMax > 0 ? Globals.getClosestColorIndex(new Color(palette[1][0]), TIGlobals.TI_PALETTE, backColor) : backColor;
                        if (backColor == foreColor && foreColor != screenColor) {
                            backColor = screenColor;
                        }
                        else if (foreColor == screenColor) {
                            int tmp = foreColor;
                            foreColor = backColor;
                            backColor = tmp;
                        }
                        charColors[y][Globals.INDEX_CLR_BACK] = backColor;
                        charColors[y][Globals.INDEX_CLR_FORE] = foreColor;
                        // Pattern
                        for (int x = 0; x < row.length; x++) {
                            Color color = new Color(row[x]);
                            charGrid[y][x] = backColor != 0 && Globals.colorDistance(color, TIGlobals.TI_PALETTE[backColor]) <= Globals.colorDistance(color, TIGlobals.TI_PALETTE[foreColor]) ? 0 : 1;
                        }
                    }
                }
                break;
            case COLOR_MODE_ECM_2:
            case COLOR_MODE_ECM_3:
                int maxPaletteSize = colorMode == COLOR_MODE_ECM_2 ? 4 : 8;
                int maxPalettes = (colorMode == COLOR_MODE_ECM_2 ? 16 : 8) - startPalette;
                // Build palettes
                // Each palette consist of a set of RGB values and a count of how many characters use the palette
                ArrayList<Pair<Set<Integer>, Integer>> palettes = new ArrayList<Pair<Set<Integer>, Integer>>();
                for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                    int[][] rgbGrid = pattern.getFirst();
                    HashSet<Integer> rgbPalette = new HashSet<Integer>();
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            rgbPalette.add(rgbGrid[y][x]);
                        }
                    }
                    for (Pair<Set<Integer>, Integer> palette : palettes) {
                        if (rgbPalette.equals(palette.getFirst())) {
                            pattern.setSecond(palette);
                            palette.setSecond(palette.getSecond() + 1);
                        }
                    }
                    if (pattern.getSecond() == null) {
                        Pair<Set<Integer>, Integer> newPalette = new Pair<Set<Integer>, Integer>(rgbPalette, 1);
                        pattern.setSecond(newPalette);
                        palettes.add(newPalette);
                    }
                }
                System.out.println("1. Number of unique palettes: " + palettes.size());
                // Sort with most used palettes first
                Collections.sort(palettes, new Comparator<Pair<Set<Integer>, Integer>>() {
                    public int compare(Pair<Set<Integer>, Integer> p1, Pair<Set<Integer>, Integer> p2) {
                        return p2.getSecond() - p1.getSecond();
                    }
                });
                // Eliminate subset palettes
                boolean found;
                do {
                    found = false;
                    for (int i = 0; i < palettes.size() && !found; i++) {
                        for (int j = 0; j < palettes.size() && !found; j++) {
                            if (i != j) {
                                if (palettes.get(i).getFirst().containsAll(palettes.get(j).getFirst())) {
                                    for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                                        if (pattern.getSecond() == palettes.get(j)) {
                                            pattern.setSecond(palettes.get(i));
                                            palettes.get(i).setSecond(palettes.get(i).getSecond() + 1);
                                        }
                                    }
                                    palettes.remove(j);
                                    found = true;
                                }
                            }
                        }
                    }
                }
                while (found);
                System.out.println("2. Number of unique palettes: " + palettes.size());
                // Sort with largest palettes first
                Collections.sort(palettes, new Comparator<Pair<Set<Integer>, Integer>>() {
                    public int compare(Pair<Set<Integer>, Integer> p1, Pair<Set<Integer>, Integer> p2) {
                        return p2.getFirst().size() - p1.getFirst().size();
                    }
                });
                // Combine palettes
                do {
                    found = false;
                    for (int i = 0; i < palettes.size() && !found; i++) {
                        for (int j = 0; j < palettes.size() && !found; j++) {
                            if (i != j) {
                                Set<Integer> combinedPalette = new HashSet<Integer>(palettes.get(i).getFirst());
                                combinedPalette.addAll(palettes.get(j).getFirst());
                                if (combinedPalette.size() <= maxPaletteSize) {
                                    palettes.get(i).getFirst().clear();
                                    palettes.get(i).getFirst().addAll(combinedPalette);
                                    for (Pair<int[][], Pair<Set<Integer>, Integer>> pattern : patterns) {
                                        if (pattern.getSecond() == palettes.get(j)) {
                                            pattern.setSecond(palettes.get(i));
                                            palettes.get(i).setSecond(palettes.get(i).getSecond() + 1);
                                        }
                                    }
                                    palettes.remove(j);
                                    found = true;
                                }
                            }
                        }
                    }
                }
                while (found);
                System.out.println("3. Number of unique palettes: " + palettes.size());
                // Keep the most used colors of the palette
                for (int i = 0; i < Math.min(palettes.size(), maxPalettes); i++) {
                    Set<Integer> palette = palettes.get(i).getFirst();
                    Integer[] palArray = palette.toArray(new Integer[palette.size()]);

                    if (palette.size() > maxPaletteSize) {
                        int[] colorCounts = new int[palette.size()];
                        for (int j = 0; j < patterns.size(); j++) {
                            if (patterns.get(j).getSecond().getFirst() == palette) {
                                int[][] grid = patterns.get(j).getFirst();
                                for (int y = 0; y < 8; y++) {
                                    for (int x = 0; x < 8; x++) {
                                        int color = grid[y][x];
                                        for (int c = 0; c < palArray.length; c++) {
                                            if (palArray[c] != null && palArray[c] == color) {
                                                colorCounts[c]++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        for (int k = 0; k < palArray.length; k++) {
                            for (int l = k + 1; l < palArray.length; l++) {
                                if (colorCounts[l] > colorCounts[k]) {
                                    int tmp = colorCounts[l];
                                    colorCounts[l] = colorCounts[k];
                                    colorCounts[k] = tmp;
                                    tmp = palArray[l];
                                    palArray[l] = palArray[k];
                                    palArray[k] = tmp;
                                }
                            }
                        }
                        palette.clear();
                        palette.addAll(Arrays.asList(palArray).subList(0, maxPaletteSize));
                    }
                }
                for (int i = 0; i < patterns.size(); i++) {
                    // Convert to indexed colors
                    int[][] grid = patterns.get(i).getFirst();
                    Pair<Set<Integer>, Integer> palette = patterns.get(i).getSecond();
                    int paletteIndex = palettes.indexOf(palette);
                    if (paletteIndex >= maxPalettes) {
                        // Find best matching palette
                        double minPalDist = Double.MAX_VALUE;
                        for (int n = 0; n < maxPalettes; n++) {
                            double dist = 0;
                            for (Integer color1 : palettes.get(n).getFirst()) {
                                for (Integer color2 : palettes.get(n).getFirst()) {
                                    dist += Globals.colorDistance(color1, color2);
                                }
                            }
                            if (dist < minPalDist) {
                                minPalDist = dist;
                                paletteIndex = n;
                            }
                        }
                        palette = palettes.get(paletteIndex);
                    }
                    Integer[] palArray = palette.getFirst().toArray(new Integer[palette.getFirst().size()]);
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int color = grid[y][x];
                            found = false;
                            for (int n = 0; n < palArray.length && n < maxPaletteSize && !found; n++) {
                                if (palArray[n] != null && color == palArray[n]) {
                                    grid[y][x] = n;
                                    found = true;
                                }
                            }
                            if (!found) {
                                // Find best matching color
                                double minColDist = Double.MAX_VALUE;
                                int colorIndex = 0;
                                for (int n = 0; n < palArray.length && n < maxPaletteSize; n++) {
                                    if (palArray[n] != null) {
                                        double dist = Globals.colorDistance(color, palArray[n]);
                                        if (dist < minColDist) {
                                            minColDist = dist;
                                            colorIndex = n;
                                        }
                                    }
                                }
                                grid[y][x] = colorIndex;
                            }
                        }
                    }
                    // Build data structures
                    ecmPalettes[paletteIndex + startPalette].setSafeColors(palArray);
                    hmCharGrids.put(i + startIndex, grid);
                    ecmCharPalettes[i + startIndex] = ecmPalettes[paletteIndex + startPalette];
                }
                break;
        }
        // Adjust map for startIndex
        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                mapData[n][m] += startIndex;
            }
        }

    }
}
