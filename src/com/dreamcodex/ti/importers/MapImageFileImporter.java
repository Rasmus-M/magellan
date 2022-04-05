package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;

public class MapImageFileImporter extends Importer {

    private static class Palette {

        private Set<Integer> rgbValues;
        private int count;

        public Palette(Set<Integer> rgbValues, int count) {
            this.rgbValues = rgbValues;
            this.count = count;
        }

        public Set<Integer> getRgbValues() {
            return rgbValues;
        }

        public void setRgbValues(Set<Integer> rgbValues) {
            this.rgbValues = rgbValues;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    private static class Pattern {

        private int[][] rgbGrid;
        private Palette palette;

        public Pattern(int[][] rgbGrid, Palette palette) {
            this.rgbGrid = rgbGrid;
            this.palette = palette;
        }

        public int[][] getRgbGrid() {
            return rgbGrid;
        }

        public void setRgbGrid(int[][] rgbGrid) {
            this.rgbGrid = rgbGrid;
        }

        public Palette getPalette() {
            return palette;
        }

        public void setPalette(Palette palette) {
            this.palette = palette;
        }
    }

    private static class CharSet {

        private int color;
        private ArrayList<Integer> charIndexes;

        public CharSet(int color, ArrayList<Integer> charIndexes) {
            this.color = color;
            this.charIndexes = charIndexes;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public ArrayList<Integer> getCharIndexes() {
            return charIndexes;
        }

        public void setCharIndexes(ArrayList<Integer> charIndexes) {
            this.charIndexes = charIndexes;
        }
    }

    private int startIndex;
    private int endIndex;
    private int maxIndex;
    private int startPalette;
    private int[][] mapData;
    private int width;
    private int height;

    public MapImageFileImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readMapImageFile(File mapImageFile, int startIndex, int endIndex, int startPalette, int tolerance) throws Exception {
        this.startIndex = startIndex & 0xf8;
        this.endIndex = endIndex;
        this.maxIndex = endIndex - startIndex;
        this.startPalette = startPalette;
        BufferedImage bufferedImage = ImageIO.read(mapImageFile);
        width = bufferedImage.getWidth() / 8;
        height = bufferedImage.getHeight() / 8;
        mapEditor.setGridWidth(width);
        mapEditor.setGridHeight(height);
        mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
        // Find unique patterns based on RGB values
        ArrayList<Pattern> patterns = new ArrayList<>();
        for (int o = 0; o < 2; o++) {
            for (int n = 0; n + o < height; n += 2) {
                int y1 = (n + o) * 8;
                for (int m = 0; m < width; m++) {
                    int x1 = m * 8;
                    int[][] rgbGrid = new int[8][8];
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            rgbGrid[y][x] = bufferedImage.getRGB(x + x1, y + y1) | 0xff000000;
                        }
                    }
                    // Find exact match in the existing characters
                    int ch = -1;
                    for (int i = 0; i < patterns.size() && ch == -1; i++) {
                        if (Globals.gridEquals(rgbGrid, patterns.get(i).getRgbGrid())) {
                            ch = i;
                        }
                    }
                    // If no exact match, find the best match, and match if within tolerance
                    int chBest = 0;
                    if (ch == -1) {
                        double minDist = Double.MAX_VALUE;
                        for (int i = 0; i < patterns.size(); i++) {
                            double dist = Globals.gridColorDistance(rgbGrid, patterns.get(i).getRgbGrid());
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
                            patterns.add(new Pattern(rgbGrid, null));
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
                importGraphicsMode(patterns);
                break;
            case COLOR_MODE_BITMAP:
                importBitmapMode(patterns);
                break;
            case COLOR_MODE_ECM_2:
            case COLOR_MODE_ECM_3:
                importECMMode(patterns);
                break;
        }
        // Adjust map for startIndex
        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                mapData[n][m] += startIndex;
            }
        }
    }

    private void importGraphicsMode(ArrayList<Pattern> patterns) {
        // Divide into char/color sets
        ArrayList<CharSet> charSets = new ArrayList<>();
        int[][][] charGrids = new int[maxIndex + 1][8][8];
        for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
            // Build a palette of the pattern grid
            // Each entry in the palette consists of a RGB color and a count
            int[][] rgbGrid = patterns.get(i).getRgbGrid();
            int[][] palette = new int[64][2]; // Max 64 unique colors in a 8x8 grid
            int iMax = -1;
            for (int[] row : rgbGrid) {
                for (int color : row) {
                    iMax = updatePaletteWithColor(palette, iMax, color);
                }
            }
            // Background and foreground are set to the two most used colors of the palette
            Globals.sortGrid(palette);
            int backColorIndex = Globals.getClosestColorIndex(new Color(palette[0][0], true), TIGlobals.TI_PALETTE);
            int foreColorIndex = iMax > 0 ? Globals.getClosestColorIndex(new Color(palette[1][0], true), TIGlobals.TI_PALETTE, backColorIndex) : backColorIndex;
            int screenColor = mapEditor.getColorScreen();
            boolean mono = backColorIndex == foreColorIndex;
            if (!mono && foreColorIndex == screenColor) {
                int tmp = foreColorIndex;
                foreColorIndex = backColorIndex;
                backColorIndex = tmp;
            }
            int key = (backColorIndex * 16) | foreColorIndex;
            ArrayList<Integer> charIndexes = null;
            boolean foundButNoRoom = false;
            for (CharSet charSet : charSets) {
                if (charIndexes == null) {
                    int otherKey = charSet.getColor();
                    int otherBackColorIndex = otherKey / 16;
                    int otherForeColorIndex = otherKey % 16;
                    boolean otherMono = otherBackColorIndex == otherForeColorIndex;
                    // Match to key of existing character color set
                    // if monochrome, it's enough that one of the colors (background or foreground) matches
                    if (
                        otherKey == key
                            ||
                            (mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)
                            ||
                            otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex
                    ) {
                        charIndexes = charSet.getCharIndexes();
                        if (charIndexes.size() < 8) {
                            charIndexes.add(i);
                            if ((mono || otherMono) && (otherBackColorIndex == backColorIndex || otherForeColorIndex == foreColorIndex)) {
                                if (mono) {
                                    foreColorIndex = otherForeColorIndex;
                                } else {
                                    // Update foreground color of the other character set
                                    if (otherBackColorIndex == backColorIndex) {
                                        charSet.setColor((otherBackColorIndex * 16) | foreColorIndex);
                                    } else {
                                        charSet.setColor((otherBackColorIndex * 16) | backColorIndex);
                                        foreColorIndex = backColorIndex;
                                    }
                                }
                                backColorIndex = otherBackColorIndex;
                            } else if (otherBackColorIndex == foreColorIndex && otherForeColorIndex == backColorIndex) {
                                // Inverse match
                                foreColorIndex = otherForeColorIndex;
                                backColorIndex = otherBackColorIndex;
                            }
                        } else {
                            charIndexes = null;
                            foundButNoRoom = true;
                        }
                    }
                }
            }
            if (charIndexes == null) {
                // An exact match with an existing character set was not found
                if (charSets.size() < (maxIndex + 1) / 8) {
                    // Make a new one if room
                    charIndexes = new ArrayList<>(8);
                    charIndexes.add(i);
                    charSets.add(new CharSet(key, charIndexes));
                } else {
                    if (foundButNoRoom) {
                        // Remap the character
                        rgbGrid = patterns.get(i).getRgbGrid();
                        int ch = -1;
                        double minDist = Double.MAX_VALUE;
                        for (int j = 0; j < i; j++) {
                            if (patterns.get(j) != null) {
                                double dist = Globals.gridColorDistance(rgbGrid, patterns.get(j).getRgbGrid());
                                if (dist < minDist) {
                                    minDist = dist;
                                    ch = j;
                                }
                            }
                        }
                        System.out.println("Char " + i + " remapped to " + ch);
                        Globals.gridReplace(mapData, i, ch);
                        patterns.set(i, null);
                    } else {
                        // Find the best matching set, i.e. the one with the smallest color distance
                        double minDist = Double.MAX_VALUE;
                        int backIndex1 = backColorIndex;
                        int foreIndex1 = foreColorIndex;
                        for (CharSet charSet : charSets) {
                            if (charSet.getCharIndexes().size() < 8) {
                                int k = charSet.getColor();
                                int foreIndex2 = k / 16;
                                int backIndex2 = k % 16;
                                double dist =
                                        Globals.colorDistance(TIGlobals.TI_PALETTE[backIndex2], TIGlobals.TI_PALETTE[backIndex1]) +
                                                Globals.colorDistance(TIGlobals.TI_PALETTE[foreIndex2], TIGlobals.TI_PALETTE[foreIndex1]);
                                if (dist < minDist) {
                                    minDist = dist;
                                    charIndexes = charSet.getCharIndexes();
                                    backColorIndex = foreIndex2;
                                    foreColorIndex = backIndex2;
                                }
                            }
                        }
                        if (charIndexes != null) {
                            charIndexes.add(i);
                        }
                    }
                }
            }
            if (charIndexes != null) {
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
                for (int j = 0; j < charIndexes.size() - 2 && !found; j++) {
                    int k = charIndexes.get(j);
                    if (Globals.gridEquals(charGrid, charGrids[k])) {
                        // Found the same pattern - remove this one
                        charIndexes.remove(charIndexes.size() - 1);
                        Globals.gridReplace(mapData, i, k);
                        patterns.set(i, null);
                        found = true;
                    }
                }
            }
        }
        // Maps old char numbers to new
        Map<Integer, Integer> charMap = new HashMap<>();
        int colSet = startIndex / 8;
        int ch = 0;
        for (CharSet charSet : charSets) {
            ArrayList<Integer> charIndexes = charSet.getCharIndexes();
            for (Integer n : charIndexes) {
                charMap.put(n, ch);
                this.charGrids.put(ch + startIndex, charGrids[n]);
                ch++;
            }
            // Fill up character color set
            while (ch % 8 != 0) {
                this.charGrids.put(ch + startIndex, new int[8][8]);
                ch++;
            }
            int key = charSet.getColor();
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
    }

    private void importBitmapMode(ArrayList<Pattern> patterns) {
        for (int i = 0; i < patterns.size() && i <= maxIndex; i++) {
            int[][] rgbGrid = patterns.get(i).getRgbGrid();
            int[][] charGrid = this.charGrids.computeIfAbsent(i + startIndex, k -> new int[8][8]);
            int[][] charColors = this.charColors.computeIfAbsent(i + startIndex, k -> new int[8][2]);
            int screenColor = mapEditor.getColorScreen();
            for (int y = 0; y < rgbGrid.length; y++) {
                int[] row = rgbGrid[y];
                int[][] palette = new int[8][2];
                int iMax = -1;
                for (Integer color : row) {
                    iMax = updatePaletteWithColor(palette, iMax, color);
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
    }

    private void importECMMode(ArrayList<Pattern> patterns) {
        int maxPaletteSize = colorMode == COLOR_MODE_ECM_2 ? 4 : 8;
        int maxPalettes = (colorMode == COLOR_MODE_ECM_2 ? 16 : 8) - startPalette;
        // Build palettes
        // Each palette consist of a set of RGB values and a count of how many characters use the palette
        ArrayList<Palette> palettes = new ArrayList<>();
        for (Pattern pattern : patterns) {
            int[][] rgbGrid = pattern.getRgbGrid();
            HashSet<Integer> rgbPalette = new HashSet<>();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    rgbPalette.add(rgbGrid[y][x]);
                }
            }
            for (Palette palette : palettes) {
                if (rgbPalette.equals(palette.getRgbValues())) {
                    pattern.setPalette(palette);
                    palette.setCount(palette.getCount() + 1);
                }
            }
            if (pattern.getPalette() == null) {
                Palette newPalette = new Palette(rgbPalette, 1);
                pattern.setPalette(newPalette);
                palettes.add(newPalette);
            }
        }
        System.out.println("1. Number of unique palettes: " + palettes.size());
        // Sort with most used palettes first
        palettes.sort((p1, p2) -> p2.getCount() - p1.getCount());
        // Eliminate subset palettes
        boolean found;
        do {
            found = false;
            for (int i = 0; i < palettes.size() && !found; i++) {
                Palette palette1 = palettes.get(i);
                for (int j = 0; j < palettes.size() && !found; j++) {
                    Palette palette2 = palettes.get(j);
                    if (i != j) {
                        if (palette1.getRgbValues().containsAll(palette2.getRgbValues())) {
                            for (Pattern pattern : patterns) {
                                if (pattern.getPalette() == palette2) {
                                    pattern.setPalette(palette1);
                                    palette1.setCount(palette1.getCount() + 1);
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
        palettes.sort((p1, p2) -> p2.getRgbValues().size() - p1.getRgbValues().size());
        // Combine palettes
        do {
            found = false;
            for (int i = 0; i < palettes.size() && !found; i++) {
                Palette palette1 = palettes.get(i);
                for (int j = 0; j < palettes.size() && !found; j++) {
                    if (i != j) {
                        Palette palette2 = palettes.get(j);
                        Set<Integer> combinedPalette = new HashSet<>(palette1.getRgbValues());
                        combinedPalette.addAll(palette2.getRgbValues());
                        if (combinedPalette.size() <= maxPaletteSize) {
                            palette1.getRgbValues().clear();
                            palette1.getRgbValues().addAll(combinedPalette);
                            for (Pattern pattern : patterns) {
                                if (pattern.getPalette() == palette2) {
                                    pattern.setPalette(palette1);
                                    palette1.setCount(palette1.getCount() + 1);
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
            Set<Integer> rgbValues = palettes.get(i).getRgbValues();
            Integer[] palArray = rgbValues.toArray(new Integer[0]);
            if (rgbValues.size() > maxPaletteSize) {
                int[] colorCounts = new int[rgbValues.size()];
                for (Pattern pattern : patterns) {
                    if (pattern.getPalette().getRgbValues() == rgbValues) {
                        int[][] grid = pattern.getRgbGrid();
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
                rgbValues.clear();
                rgbValues.addAll(Arrays.asList(palArray).subList(0, maxPaletteSize));
            }
        }
        for (int i = 0; i < patterns.size(); i++) {
            // Convert to indexed colors
            int[][] grid = patterns.get(i).getRgbGrid();
            Palette palette = patterns.get(i).getPalette();
            int paletteIndex = palettes.indexOf(palette);
            if (paletteIndex >= maxPalettes) {
                // Find best matching palette
                double minPalDist = Double.MAX_VALUE;
                for (int n = 0; n < maxPalettes; n++) {
                    double dist = 0;
                    for (Integer color1 : palettes.get(n).getRgbValues()) {
                        for (Integer color2 : palettes.get(n).getRgbValues()) {
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
            Integer[] palArray = palette.getRgbValues().toArray(new Integer[0]);
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
            this.charGrids.put(i + startIndex, grid);
            ecmCharPalettes[i + startIndex] = ecmPalettes[paletteIndex + startPalette];
        }
    }

    private int updatePaletteWithColor(int[][] palette, int iMax, Integer color) {
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
        return iMax;
    }
}
