package com.dreamcodex.ti.util;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dreamcodex.ti.util.ColorMode.*;

/**
 * Keeps the colors of the characters when the color mode is switched: take a snapshot of what
 * each character looks like before the switch, then rebuild the character data for the new mode
 * from the snapshot.
 */
public class CharColorConverter {

    /**
     * What the defined characters look like: a color per pixel (null = transparent), and whether
     * the pixel was a background pixel (value 0).
     */
    public static class Snapshot {
        private final ColorMode colorMode;
        private final HashMap<Integer, Color[][]> colors = new HashMap<>();
        private final HashMap<Integer, boolean[][]> back = new HashMap<>();

        private Snapshot(ColorMode colorMode) {
            this.colorMode = colorMode;
        }
    }

    /**
     * Records the look of the characters. Call before the color mode or the ECM palettes are changed.
     */
    public static Snapshot capture(DataSet dataSet) {
        ColorMode colorMode = dataSet.getColorMode();
        Snapshot snapshot = new Snapshot(colorMode);
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int[][] grid = dataSet.getCharGrids().get(ch);
            if (grid == null) {
                continue;
            }
            Color[][] colors = new Color[grid.length][grid[0].length];
            boolean[][] back = new boolean[grid.length][grid[0].length];
            int[][] charColors = colorMode == COLOR_MODE_BITMAP ? dataSet.getCharColors().get(ch) : null;
            int[] clrSet = dataSet.getClrSets()[ch / TIGlobals.COLOR_SET_SIZE];
            ECMPalette ecmPalette = dataSet.getEcmCharPalettes()[ch];
            boolean transparent = dataSet.getEcmCharTransparency()[ch];
            for (int y = 0; y < grid.length; y++) {
                for (int x = 0; x < grid[y].length; x++) {
                    int value = grid[y][x];
                    back[y][x] = value == 0;
                    if (colorMode == COLOR_MODE_GRAPHICS_1) {
                        colors[y][x] = getTIColor(clrSet[value == 0 ? Globals.INDEX_CLR_BACK : Globals.INDEX_CLR_FORE]);
                    }
                    else if (colorMode == COLOR_MODE_BITMAP) {
                        colors[y][x] = charColors != null ? getTIColor(charColors[y][value == 0 ? Globals.INDEX_CLR_BACK : Globals.INDEX_CLR_FORE]) : null;
                    }
                    else {
                        colors[y][x] = value == 0 && transparent ? null : ecmPalette.getColor(value);
                    }
                }
            }
            snapshot.colors.put(ch, colors);
            snapshot.back.put(ch, back);
        }
        return snapshot;
    }

    /**
     * Rebuilds the character data for the current color mode from the snapshot. Call after the
     * color mode has been set and the ECM palettes have been rebuilt. Grids and color sets are
     * changed in place, but bitmap row colors are new arrays.
     */
    public static void apply(DataSet dataSet, Snapshot snapshot) {
        switch (dataSet.getColorMode()) {
            case COLOR_MODE_GRAPHICS_1:
                applyGraphicsMode(dataSet, snapshot);
                break;
            case COLOR_MODE_BITMAP:
                applyBitmapMode(dataSet, snapshot);
                break;
            case COLOR_MODE_ECM_2:
            case COLOR_MODE_ECM_3:
                applyECMMode(dataSet, snapshot);
                break;
        }
    }

    // TI colors ------------------------------------------------------------------------------/

    private static Color getTIColor(int index) {
        return index == 0 ? null : TIGlobals.TI_PALETTE_OPAQUE[index];
    }

    /**
     * The TI color index of a color. Index 0 (transparent) only for transparent pixels and for the
     * color used to show transparency, so light colors don't turn transparent.
     */
    public static int getTIColorIndex(Color color) {
        if (color == null || color.equals(TIGlobals.TI_COLOR_TRANSOPAQUE)) {
            return 0;
        }
        return Globals.getClosestColorIndex(color, TIGlobals.TI_PALETTE_OPAQUE, 0);
    }

    /**
     * Picks a back and fore TI color for some pixels: the back color is the most used color of
     * the background pixels, the fore color the most used other color. Falls back to the given
     * colors if there are no pixels to decide from.
     */
    private static int[] pickColors(ArrayList<Color> pixels, ArrayList<Boolean> backFlags, int defaultBack, int defaultFore) {
        int[] backCounts = new int[16];
        int[] foreCounts = new int[16];
        for (int i = 0; i < pixels.size(); i++) {
            int index = getTIColorIndex(pixels.get(i));
            if (backFlags.get(i)) {
                backCounts[index]++;
            }
            else {
                foreCounts[index]++;
            }
        }
        int back = getMostUsed(backCounts, -1);
        if (back == -1) {
            back = defaultBack;
        }
        int fore = getMostUsed(foreCounts, back);
        if (fore == -1) {
            fore = getMostUsed(backCounts, back);
        }
        if (fore == -1) {
            fore = defaultFore;
        }
        return new int[] {back, fore};
    }

    private static int getMostUsed(int[] counts, int exclude) {
        int best = -1;
        int max = 0;
        for (int i = 0; i < counts.length; i++) {
            if (i != exclude && counts[i] > max) {
                max = counts[i];
                best = i;
            }
        }
        return best;
    }

    /**
     * 0 if a pixel color is closer to the back color than to the fore color, otherwise 1.
     */
    private static int getPixelValue(Color color, int back, int fore) {
        int index = getTIColorIndex(color);
        if (index == back) {
            return 0;
        }
        if (index == fore) {
            return 1;
        }
        return getTIDistance(color, back) <= getTIDistance(color, fore) ? 0 : 1;
    }

    private static double getTIDistance(Color color, int index) {
        if (color == null || index == 0) {
            return color == null && index == 0 ? 0 : Double.MAX_VALUE / 4;
        }
        return Globals.colorDistance(color, TIGlobals.TI_PALETTE_OPAQUE[index]);
    }

    // Graphics 1 -----------------------------------------------------------------------------/

    private static void applyGraphicsMode(DataSet dataSet, Snapshot snapshot) {
        int[][] clrSets = dataSet.getClrSets();
        for (int set = 0; set < clrSets.length; set++) {
            ArrayList<Color> pixels = new ArrayList<>();
            ArrayList<Boolean> backFlags = new ArrayList<>();
            boolean defined = false;
            for (int ch = set * TIGlobals.COLOR_SET_SIZE; ch < (set + 1) * TIGlobals.COLOR_SET_SIZE; ch++) {
                if (snapshot.colors.containsKey(ch)) {
                    defined = true;
                    addPixels(snapshot, ch, -1, pixels, backFlags);
                }
            }
            if (!defined) {
                continue;
            }
            int[] colors = pickColors(pixels, backFlags, clrSets[set][Globals.INDEX_CLR_BACK], clrSets[set][Globals.INDEX_CLR_FORE]);
            clrSets[set][Globals.INDEX_CLR_BACK] = colors[0];
            clrSets[set][Globals.INDEX_CLR_FORE] = colors[1];
            for (int ch = set * TIGlobals.COLOR_SET_SIZE; ch < (set + 1) * TIGlobals.COLOR_SET_SIZE; ch++) {
                Color[][] charPixels = snapshot.colors.get(ch);
                if (charPixels != null) {
                    int[][] grid = dataSet.getCharGrids().get(ch);
                    for (int y = 0; y < grid.length; y++) {
                        for (int x = 0; x < grid[y].length; x++) {
                            grid[y][x] = getPixelValue(charPixels[y][x], colors[0], colors[1]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds the pixels of a character, or of one row if row isn't -1.
     */
    private static void addPixels(Snapshot snapshot, int ch, int row, ArrayList<Color> pixels, ArrayList<Boolean> backFlags) {
        Color[][] colors = snapshot.colors.get(ch);
        boolean[][] back = snapshot.back.get(ch);
        for (int y = 0; y < colors.length; y++) {
            if (row == -1 || y == row) {
                for (int x = 0; x < colors[y].length; x++) {
                    pixels.add(colors[y][x]);
                    backFlags.add(back[y][x]);
                }
            }
        }
    }

    // Bitmap ---------------------------------------------------------------------------------/

    private static void applyBitmapMode(DataSet dataSet, Snapshot snapshot) {
        HashMap<Integer, int[][]> charColors = new HashMap<>();
        dataSet.setCharColors(charColors);
        int[][] clrSets = dataSet.getClrSets();
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int[] clrSet = clrSets[ch / TIGlobals.COLOR_SET_SIZE];
            int[][] rowColors = new int[8][2];
            Color[][] charPixels = snapshot.colors.get(ch);
            if (snapshot.colorMode == COLOR_MODE_GRAPHICS_1 || charPixels == null) {
                // Exact: every row gets the colors of the color set
                for (int[] row : rowColors) {
                    row[Globals.INDEX_CLR_BACK] = clrSet[Globals.INDEX_CLR_BACK];
                    row[Globals.INDEX_CLR_FORE] = clrSet[Globals.INDEX_CLR_FORE];
                }
            }
            else {
                // Colors of the whole character, used for rows that only have one kind of pixel
                ArrayList<Color> pixels = new ArrayList<>();
                ArrayList<Boolean> backFlags = new ArrayList<>();
                addPixels(snapshot, ch, -1, pixels, backFlags);
                int[] charColorPair = pickColors(pixels, backFlags, clrSet[Globals.INDEX_CLR_BACK], clrSet[Globals.INDEX_CLR_FORE]);
                int[][] grid = dataSet.getCharGrids().get(ch);
                for (int y = 0; y < rowColors.length && y < grid.length; y++) {
                    pixels.clear();
                    backFlags.clear();
                    addPixels(snapshot, ch, y, pixels, backFlags);
                    int[] colors = pickColors(pixels, backFlags, charColorPair[0], charColorPair[1]);
                    rowColors[y][Globals.INDEX_CLR_BACK] = colors[0];
                    rowColors[y][Globals.INDEX_CLR_FORE] = colors[1];
                    for (int x = 0; x < grid[y].length; x++) {
                        grid[y][x] = getPixelValue(charPixels[y][x], colors[0], colors[1]);
                    }
                }
            }
            charColors.put(ch, rowColors);
        }
    }

    // ECM ------------------------------------------------------------------------------------/

    /**
     * A palette being built. Transparent palettes have a transparent first entry, and only
     * transparent characters use them.
     */
    private static class PaletteBuilder {
        private final boolean transparent;
        private final ArrayList<Color> colors = new ArrayList<>();

        PaletteBuilder(boolean transparent) {
            this.transparent = transparent;
            if (transparent) {
                colors.add(null);
            }
        }

        int getMissing(ArrayList<Color> needed) {
            int missing = 0;
            for (Color color : needed) {
                if (!colors.contains(color)) {
                    missing++;
                }
            }
            return missing;
        }

        void addAll(ArrayList<Color> needed, int size) {
            for (Color color : needed) {
                if (!colors.contains(color) && colors.size() < size) {
                    colors.add(color);
                }
            }
        }

        /**
         * The index of the closest color. Transparent pixels use index 0.
         */
        int getClosestIndex(Color color) {
            int index = colors.indexOf(color);
            if (index != -1) {
                return index;
            }
            if (color == null) {
                return 0;
            }
            int best = 0;
            double minDist = Double.MAX_VALUE;
            for (int i = 0; i < colors.size(); i++) {
                if (colors.get(i) != null) {
                    double dist = Globals.colorDistance(color, colors.get(i));
                    if (dist < minDist) {
                        minDist = dist;
                        best = i;
                    }
                }
            }
            return best;
        }

        double getDistance(ArrayList<Color> needed) {
            double distance = 0;
            for (Color color : needed) {
                if (color == null) {
                    distance += transparent ? 0 : 1000;
                }
                else {
                    Color closest = colors.get(getClosestIndex(color));
                    distance += closest != null ? Globals.colorDistance(color, closest) : 1000;
                }
            }
            return distance;
        }
    }

    private static void applyECMMode(DataSet dataSet, Snapshot snapshot) {
        ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
        ECMPalette[] ecmCharPalettes = dataSet.getEcmCharPalettes();
        boolean[] ecmCharTransparency = dataSet.getEcmCharTransparency();
        int maxPalettes = ecmPalettes.length;
        int size = ecmPalettes[0].getSize();
        ArrayList<PaletteBuilder> palettes = new ArrayList<>();
        HashMap<Integer, PaletteBuilder> charPalettes = new HashMap<>();
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            Color[][] charPixels = snapshot.colors.get(ch);
            if (charPixels == null) {
                continue;
            }
            ArrayList<Color> needed = getECMColors(charPixels, size);
            boolean transparent = needed.contains(null);
            PaletteBuilder found = null;
            // A palette that has all the colors
            for (PaletteBuilder palette : palettes) {
                if (found == null && palette.transparent == transparent && palette.getMissing(needed) == 0) {
                    found = palette;
                }
            }
            // A palette with room for the missing colors
            for (PaletteBuilder palette : palettes) {
                if (found == null && palette.transparent == transparent && palette.colors.size() + palette.getMissing(needed) <= size) {
                    found = palette;
                }
            }
            // A new palette
            if (found == null && palettes.size() < maxPalettes) {
                found = new PaletteBuilder(transparent);
                palettes.add(found);
            }
            // Out of palettes: the closest one
            if (found == null) {
                double minDist = Double.MAX_VALUE;
                for (PaletteBuilder palette : palettes) {
                    double dist = palette.getDistance(needed);
                    if (dist < minDist) {
                        minDist = dist;
                        found = palette;
                    }
                }
            }
            found.addAll(needed, size);
            charPalettes.put(ch, found);
        }
        // Store the palettes. The transparent entry keeps its current color.
        for (int p = 0; p < palettes.size(); p++) {
            ArrayList<Color> colors = palettes.get(p).colors;
            for (int i = 0; i < colors.size(); i++) {
                if (colors.get(i) != null) {
                    ecmPalettes[p].setColor(i, colors.get(i));
                }
            }
        }
        // Store the characters
        for (Map.Entry<Integer, PaletteBuilder> entry : charPalettes.entrySet()) {
            int ch = entry.getKey();
            PaletteBuilder palette = entry.getValue();
            Color[][] charPixels = snapshot.colors.get(ch);
            int[][] grid = dataSet.getCharGrids().get(ch);
            for (int y = 0; y < grid.length; y++) {
                for (int x = 0; x < grid[y].length; x++) {
                    Color color = charPixels[y][x];
                    grid[y][x] = palette.getClosestIndex(color != null ? Globals.getECMSafeColor(color) : null);
                }
            }
            ecmCharPalettes[ch] = ecmPalettes[palettes.indexOf(palette)];
            ecmCharTransparency[ch] = palette.transparent;
        }
    }

    /**
     * The ECM-safe colors of a character, most used first, at most size colors.
     * A transparent color (null) is always kept.
     */
    private static ArrayList<Color> getECMColors(Color[][] charPixels, int size) {
        LinkedHashMap<Color, Integer> counts = new LinkedHashMap<>();
        for (Color[] row : charPixels) {
            for (Color color : row) {
                Color safeColor = color != null ? Globals.getECMSafeColor(color) : null;
                Integer count = counts.get(safeColor);
                counts.put(safeColor, count != null ? count + 1 : 1);
            }
        }
        ArrayList<Map.Entry<Color, Integer>> entries = new ArrayList<>(counts.entrySet());
        // Stable sort, so equally used colors keep their order of appearance
        entries.sort((e1, e2) -> {
            if (e1.getKey() == null || e2.getKey() == null) {
                return e1.getKey() == e2.getKey() ? 0 : e1.getKey() == null ? -1 : 1;
            }
            return e2.getValue() - e1.getValue();
        });
        ArrayList<Color> colors = new ArrayList<>();
        for (Map.Entry<Color, Integer> entry : entries) {
            if (colors.size() < size) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }
}
