package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static com.dreamcodex.ti.util.ColorMode.*;

/**
 * Imports a C64 CharPad project (.ctm, format version 6 or 7).
 * CharPad character n becomes character startChar + n, and the map (with tiles expanded
 * into characters) replaces the current map. Colors are converted for the current color mode.
 */
public class CharPadImporter extends Importer {

    // C64 colors (Pepto palette)
    private static final Color[] C64_PALETTE = {
        new Color(0x000000), new Color(0xFFFFFF), new Color(0x68372B), new Color(0x70A4B2),
        new Color(0x6F3D86), new Color(0x588D43), new Color(0x352879), new Color(0xB8C76F),
        new Color(0x6F4F25), new Color(0x433900), new Color(0x9A6759), new Color(0x444444),
        new Color(0x6C6C6C), new Color(0x9AD284), new Color(0x6C5EB5), new Color(0x959595)
    };

    // Closest TI color for each C64 color, picked by hand because the TI palette is much more
    // saturated than the C64 one, so nearest RGB would turn several C64 colors black
    private static final int[] C64_TO_TI = {
        1,  // black
        15, // white
        6,  // red -> dark red
        7,  // cyan
        13, // purple -> magenta
        12, // green -> dark green
        4,  // blue -> dark blue
        11, // yellow -> light yellow
        10, // orange -> dark yellow
        6,  // brown -> dark red
        9,  // light red
        14, // dark grey -> grey
        14, // grey
        3,  // light green
        5,  // light blue
        14  // light grey -> grey
    };

    public static final int SCREEN_MODE_HIRES = 0;
    public static final int SCREEN_MODE_MULTICOLOR = 1;
    public static final int SCREEN_MODE_ECM = 2;

    public static final int COLORING_PER_MAP = 0;
    public static final int COLORING_PER_TILE = 1;
    public static final int COLORING_PER_CHAR = 2;

    /**
     * The contents of a CTM file.
     */
    public static class CtmData {
        public int version;
        // BG1, BG2/MC1, BG3/MC2, BG4, char color
        public int[] colors = new int[5];
        public int coloringMethod;
        public int screenMode;
        public boolean tileSystem;
        public int charCount;
        public int[][] charImages; // [char][row] bytes
        public int[] charAttributes; // low nybble color, high nybble material
        public int tileCount;
        public int tileWidth;
        public int tileHeight;
        public int[][] tiles; // [tile][cell], cells left to right, top to bottom
        public int[] tileColors;
        public int mapWidth;
        public int mapHeight;
        public int[] map; // cells left to right, top to bottom

        /**
         * The width of the map in characters, with tiles expanded.
         */
        public int getCharMapWidth() {
            return tileSystem ? mapWidth * tileWidth : mapWidth;
        }

        public int getCharMapHeight() {
            return tileSystem ? mapHeight * tileHeight : mapHeight;
        }

        /**
         * The character at a position of the map, with tiles expanded, or -1 if undefined.
         */
        public int getCharAt(int x, int y) {
            if (tileSystem) {
                int tile = map[(y / tileHeight) * mapWidth + x / tileWidth];
                if (tile >= tileCount) {
                    return -1;
                }
                int ch = tiles[tile][(y % tileHeight) * tileWidth + x % tileWidth];
                return ch < charCount ? ch : -1;
            }
            int ch = map[y * mapWidth + x];
            return ch < charCount ? ch : -1;
        }
    }

    public CharPadImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    // Parsing ----------------------------------------------------------------------------/

    private static class Reader {
        private final byte[] data;
        private int pos;

        Reader(byte[] data, int pos) {
            this.data = data;
            this.pos = pos;
        }

        int readByte() throws IOException {
            if (pos >= data.length) {
                throw new IOException("Unexpected end of CharPad file.");
            }
            return data[pos++] & 0xFF;
        }

        int readWord() throws IOException {
            return readByte() | (readByte() << 8);
        }

        void readBlockMarker() throws IOException {
            int b1 = readByte();
            int b2 = readByte();
            if (b1 != 0xDA || (b2 & 0xF0) != 0xB0) {
                throw new IOException("Invalid CharPad file: block marker expected at offset " + (pos - 2) + ".");
            }
        }
    }

    public static CtmData parse(byte[] data) throws IOException {
        if (data.length < 10 || data[0] != 'C' || data[1] != 'T' || data[2] != 'M') {
            throw new IOException("Not a CharPad file.");
        }
        CtmData ctm = new CtmData();
        ctm.version = data[3] & 0xFF;
        Reader reader;
        if (ctm.version == 7) {
            reader = new Reader(data, 4);
            for (int i = 0; i < 5; i++) {
                ctm.colors[i] = reader.readByte() & 0x0F;
            }
            ctm.coloringMethod = reader.readByte();
            ctm.screenMode = reader.readByte();
            ctm.tileSystem = (reader.readByte() & 0x01) != 0;
        }
        else if (ctm.version == 6) {
            reader = new Reader(data, 4);
            ctm.colors[0] = reader.readByte() & 0x0F;
            ctm.colors[1] = reader.readByte() & 0x0F;
            ctm.colors[2] = reader.readByte() & 0x0F;
            ctm.colors[4] = reader.readByte() & 0x0F;
            ctm.coloringMethod = reader.readByte();
            int flags = reader.readByte();
            ctm.screenMode = (flags & 0x01) != 0 ? SCREEN_MODE_MULTICOLOR : SCREEN_MODE_HIRES;
            ctm.tileSystem = (flags & 0x02) != 0;
        }
        else {
            throw new IOException("CharPad file format version " + ctm.version + " is not supported. Please save the file with CharPad 2.6 or later (version 6 or 7).");
        }
        // Characters
        reader.readBlockMarker();
        ctm.charCount = reader.readWord() + 1;
        ctm.charImages = new int[ctm.charCount][8];
        for (int ch = 0; ch < ctm.charCount; ch++) {
            for (int row = 0; row < 8; row++) {
                ctm.charImages[ch][row] = reader.readByte();
            }
        }
        reader.readBlockMarker();
        ctm.charAttributes = new int[ctm.charCount];
        for (int ch = 0; ch < ctm.charCount; ch++) {
            ctm.charAttributes[ch] = reader.readByte();
        }
        // Tiles
        if (ctm.tileSystem) {
            reader.readBlockMarker();
            ctm.tileCount = reader.readWord() + 1;
            ctm.tileWidth = reader.readByte();
            ctm.tileHeight = reader.readByte();
            if (ctm.tileWidth == 0 || ctm.tileHeight == 0) {
                throw new IOException("Invalid CharPad file: tile size is " + ctm.tileWidth + "x" + ctm.tileHeight + ".");
            }
            ctm.tiles = new int[ctm.tileCount][ctm.tileWidth * ctm.tileHeight];
            for (int t = 0; t < ctm.tileCount; t++) {
                for (int i = 0; i < ctm.tiles[t].length; i++) {
                    ctm.tiles[t][i] = reader.readWord();
                }
            }
            if (ctm.coloringMethod == COLORING_PER_TILE) {
                reader.readBlockMarker();
                ctm.tileColors = new int[ctm.tileCount];
                for (int t = 0; t < ctm.tileCount; t++) {
                    ctm.tileColors[t] = reader.readByte() & 0x0F;
                }
            }
            // Tile tags
            reader.readBlockMarker();
            for (int t = 0; t < ctm.tileCount; t++) {
                reader.readByte();
            }
            // Tile names
            reader.readBlockMarker();
            for (int t = 0; t < ctm.tileCount; t++) {
                while (reader.readByte() != 0) {
                    // Skip name
                }
            }
        }
        // Map
        reader.readBlockMarker();
        ctm.mapWidth = reader.readWord();
        ctm.mapHeight = reader.readWord();
        ctm.map = new int[ctm.mapWidth * ctm.mapHeight];
        for (int i = 0; i < ctm.map.length; i++) {
            ctm.map[i] = reader.readWord();
        }
        return ctm;
    }

    // Importing --------------------------------------------------------------------------/

    /**
     * Sets characters startChar to startChar + charCount - 1 and replaces the current map.
     * The caller must check that the characters fit.
     */
    public void importCharPad(CtmData ctm, int startChar) {
        int[] charColor = getCharColors(ctm);
        // Each character as a grid of C64 colors
        int[][][] c64Grids = new int[ctm.charCount][][];
        int[] backColors = new int[ctm.charCount];
        for (int ch = 0; ch < ctm.charCount; ch++) {
            backColors[ch] = ctm.screenMode == SCREEN_MODE_ECM ? ctm.colors[(ch >> 6) & 3] : ctm.colors[0];
            c64Grids[ch] = getC64Grid(ctm, ch, charColor[ch], backColors[ch]);
        }
        int screenColor = C64_TO_TI[ctm.colors[0]];
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                importGraphicsMode(ctm, c64Grids, backColors, charColor, startChar);
                break;
            case COLOR_MODE_BITMAP:
                importBitmapMode(ctm, c64Grids, backColors, charColor, startChar);
                break;
            case COLOR_MODE_ECM_2:
            case COLOR_MODE_ECM_3:
                importECMMode(ctm, c64Grids, backColors, startChar);
                break;
        }
        // Map
        int width = ctm.getCharMapWidth();
        int height = ctm.getCharMapHeight();
        mapEditor.setGridWidth(width);
        mapEditor.setGridHeight(height);
        int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int ch = ctm.getCharAt(x, y);
                mapData[y][x] = startChar + (ch != -1 ? ch : 0);
            }
        }
        mapEditor.setColorScreen(screenColor);
    }

    /**
     * The C64 color (0-15, including the multicolor bit) of each character. With per-tile colors,
     * a character gets the color of the first tile that uses it.
     */
    private int[] getCharColors(CtmData ctm) {
        int[] charColor = new int[ctm.charCount];
        for (int ch = 0; ch < ctm.charCount; ch++) {
            charColor[ch] = ctm.coloringMethod == COLORING_PER_CHAR ? ctm.charAttributes[ch] & 0x0F : ctm.colors[4];
        }
        if (ctm.coloringMethod == COLORING_PER_TILE && ctm.tileSystem) {
            boolean[] colored = new boolean[ctm.charCount];
            for (int t = 0; t < ctm.tileCount; t++) {
                for (int ch : ctm.tiles[t]) {
                    if (ch < ctm.charCount && !colored[ch]) {
                        charColor[ch] = ctm.tileColors[t];
                        colored[ch] = true;
                    }
                }
            }
        }
        return charColor;
    }

    /**
     * A character as an 8x8 grid of C64 colors. Multicolor pixels are two pixels wide.
     */
    private int[][] getC64Grid(CtmData ctm, int ch, int color, int backColor) {
        // In ECM mode there are only 64 images, repeated in each background color bank
        int[] image = ctm.charImages[ctm.screenMode == SCREEN_MODE_ECM ? ch & 63 : ch];
        int[][] grid = new int[8][8];
        boolean multicolor = ctm.screenMode == SCREEN_MODE_MULTICOLOR && (color & 0x08) != 0;
        int foreColor = getForeColor(ctm, color);
        int[] pairColors = {backColor, ctm.colors[1], ctm.colors[2], foreColor};
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (multicolor) {
                    grid[y][x] = pairColors[(image[y] >> (6 - (x & 6))) & 3];
                }
                else {
                    grid[y][x] = (image[y] & (0x80 >> x)) != 0 ? foreColor : backColor;
                }
            }
        }
        return grid;
    }

    /**
     * The C64 fore color of a character color: in multicolor mode bit 3 selects multicolor.
     */
    private int getForeColor(CtmData ctm, int color) {
        return ctm.screenMode == SCREEN_MODE_MULTICOLOR ? color & 0x07 : color;
    }

    /**
     * The most used color in the grids other than the back color, or the default color if there is none.
     */
    private int getMostUsedColor(int backColor, int defaultColor, int[]... rows) {
        int[] counts = new int[16];
        for (int[] row : rows) {
            for (int c : row) {
                if (c != backColor) {
                    counts[c]++;
                }
            }
        }
        int best = defaultColor;
        int max = 0;
        for (int c = 0; c < 16; c++) {
            if (counts[c] > max) {
                max = counts[c];
                best = c;
            }
        }
        return best;
    }

    /**
     * 1 if a C64 color is closer to the fore color than to the back color.
     */
    private int getPixel(int c, int backColor, int foreColor) {
        if (c == backColor) {
            return 0;
        }
        return Globals.colorDistance(C64_PALETTE[c], C64_PALETTE[foreColor]) <= Globals.colorDistance(C64_PALETTE[c], C64_PALETTE[backColor]) ? 1 : 0;
    }

    /**
     * The TI back color: transparent when the C64 back color is the screen color.
     */
    private int getTIBackColor(CtmData ctm, int backColor) {
        return backColor == ctm.colors[0] ? 0 : C64_TO_TI[backColor];
    }

    private void importGraphicsMode(CtmData ctm, int[][][] c64Grids, int[] backColors, int[] charColor, int startChar) {
        // Characters in a color set share one fore and back color: use the back color of the first
        // character in the set and the most used other color of the set
        for (int first = 0; first < ctm.charCount; first += 8) {
            int last = Math.min(first + 8, ctm.charCount) - 1;
            int backColor = backColors[first];
            ArrayList<int[]> rows = new ArrayList<>();
            for (int ch = first; ch <= last; ch++) {
                for (int[] row : c64Grids[ch]) {
                    rows.add(row);
                }
            }
            int foreColor = getMostUsedColor(backColor, getForeColor(ctm, charColor[first]), rows.toArray(new int[0][]));
            for (int ch = first; ch <= last; ch++) {
                int[][] grid = new int[8][8];
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        grid[y][x] = getPixel(c64Grids[ch][y][x], backColor, foreColor);
                    }
                }
                charGrids.put(startChar + ch, grid);
            }
            int colorSet = (startChar + first) / 8;
            clrSets[colorSet][Globals.INDEX_CLR_FORE] = C64_TO_TI[foreColor];
            clrSets[colorSet][Globals.INDEX_CLR_BACK] = getTIBackColor(ctm, backColor);
        }
    }

    private void importBitmapMode(CtmData ctm, int[][][] c64Grids, int[] backColors, int[] charColor, int startChar) {
        // Each pixel row has a fore and back color: use the character's back color and the most used other color in the row
        for (int ch = 0; ch < ctm.charCount; ch++) {
            int backColor = backColors[ch];
            int[][] grid = new int[8][8];
            int[][] colors = new int[8][2];
            for (int y = 0; y < 8; y++) {
                int[] row = c64Grids[ch][y];
                int foreColor = getMostUsedColor(backColor, getForeColor(ctm, charColor[ch]), row);
                for (int x = 0; x < 8; x++) {
                    grid[y][x] = getPixel(row[x], backColor, foreColor);
                }
                colors[y][Globals.INDEX_CLR_FORE] = C64_TO_TI[foreColor];
                colors[y][Globals.INDEX_CLR_BACK] = getTIBackColor(ctm, backColor);
            }
            charGrids.put(startChar + ch, grid);
            charColors.put(startChar + ch, colors);
        }
    }

    private void importECMMode(CtmData ctm, int[][][] c64Grids, int[] backColors, int startChar) {
        // Palettes as lists of C64 colors, with the back color first. Characters share a palette
        // if it has the same back color and holds, or has room for, all their colors.
        int maxPalettes = ecmPalettes.length;
        int paletteSize = ecmPalettes[0].getSize();
        ArrayList<ArrayList<Integer>> palettes = new ArrayList<>();
        int[] charPalette = new int[ctm.charCount];
        for (int ch = 0; ch < ctm.charCount; ch++) {
            ArrayList<Integer> needed = new ArrayList<>();
            needed.add(backColors[ch]);
            for (int[] row : c64Grids[ch]) {
                for (int c : row) {
                    if (!needed.contains(c)) {
                        needed.add(c);
                    }
                }
            }
            int found = -1;
            // A palette that holds all the colors
            for (int p = 0; p < palettes.size() && found == -1; p++) {
                ArrayList<Integer> palette = palettes.get(p);
                if (palette.get(0).equals(needed.get(0)) && palette.containsAll(needed)) {
                    found = p;
                }
            }
            // A palette with room for the missing colors
            for (int p = 0; p < palettes.size() && found == -1; p++) {
                ArrayList<Integer> palette = palettes.get(p);
                if (palette.get(0).equals(needed.get(0))) {
                    int missing = 0;
                    for (int c : needed) {
                        if (!palette.contains(c)) {
                            missing++;
                        }
                    }
                    if (palette.size() + missing <= paletteSize) {
                        for (int c : needed) {
                            if (!palette.contains(c)) {
                                palette.add(c);
                            }
                        }
                        found = p;
                    }
                }
            }
            // A new palette
            if (found == -1 && palettes.size() < maxPalettes) {
                ArrayList<Integer> palette = new ArrayList<>();
                for (int i = 0; i < needed.size() && i < paletteSize; i++) {
                    palette.add(needed.get(i));
                }
                palettes.add(palette);
                found = palettes.size() - 1;
            }
            // Out of palettes: the closest one with the same back color, or any
            if (found == -1) {
                double minDist = Double.MAX_VALUE;
                for (int p = 0; p < palettes.size(); p++) {
                    ArrayList<Integer> palette = palettes.get(p);
                    double dist = palette.get(0).equals(needed.get(0)) ? 0 : 1000;
                    for (int c : needed) {
                        dist += Globals.colorDistance(C64_PALETTE[c], C64_PALETTE[getClosestPaletteColor(palette, c)]);
                    }
                    if (dist < minDist) {
                        minDist = dist;
                        found = p;
                    }
                }
            }
            charPalette[ch] = found;
        }
        // Store the palettes, keeping the existing colors in unused entries
        for (int p = 0; p < palettes.size(); p++) {
            ArrayList<Integer> palette = palettes.get(p);
            for (int i = 0; i < palette.size(); i++) {
                ecmPalettes[p].setColor(i, Globals.getECMSafeColor(C64_PALETTE[palette.get(i)]));
            }
        }
        // Store the characters
        for (int ch = 0; ch < ctm.charCount; ch++) {
            ArrayList<Integer> palette = palettes.get(charPalette[ch]);
            int[][] grid = new int[8][8];
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    grid[y][x] = palette.indexOf(getClosestPaletteColor(palette, c64Grids[ch][y][x]));
                }
            }
            charGrids.put(startChar + ch, grid);
            ecmCharPalettes[startChar + ch] = ecmPalettes[charPalette[ch]];
            ecmCharTransparency[startChar + ch] = false;
        }
    }

    /**
     * The color itself if it's in the palette, otherwise the closest palette color.
     */
    private int getClosestPaletteColor(ArrayList<Integer> palette, int c) {
        if (palette.contains(c)) {
            return c;
        }
        int best = palette.get(0);
        double minDist = Double.MAX_VALUE;
        for (int pc : palette) {
            double dist = Globals.colorDistance(C64_PALETTE[c], C64_PALETTE[pc]);
            if (dist < minDist) {
                minDist = dist;
                best = pc;
            }
        }
        return best;
    }
}
