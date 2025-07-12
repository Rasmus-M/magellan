package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.util.ColorMode.*;

public class CVBasicDataFileExporter extends Exporter {
    private final StringBuilder lineBuffer = new StringBuilder();

    public CVBasicDataFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeCVBasicDataFile(File mapDataFile, Preferences preferences) throws Exception {
        writeCVBasicDataFile(
            mapDataFile,
            preferences.getDefStartChar(),
            preferences.getDefEndChar(),
            preferences.getDefStartSprite(),
            preferences.getDefEndSprite(),
            preferences.getCompression(),
            preferences.isExportComments(),
            preferences.isCurrentMapOnly(),
            preferences.isIncludeCharNumbers(),
            preferences.isIncludeCharData(),
            preferences.isIncludeSpriteData(),
            preferences.isIncludeColorData(),
            preferences.isIncludeMapData()
        );
    }

    public void writeCVBasicDataFile(
        File mapDataFile,
        int startChar, int endChar,
        int startSprite, int endSprite,
        int compression,
        boolean includeComments,
        boolean currMapOnly,
        boolean includeCharNumbers,
        boolean includeCharData,
        boolean includeSpriteData,
        boolean includeColorData,
        boolean includeMapData
    ) throws Exception {
        // Validate RLE constraints
        if ((compression == MagellanExportDialog.COMPRESSION_RLE_BYTE ||
             compression == MagellanExportDialog.COMPRESSION_RLE_WORD) &&
            endChar > 127) {
            throw new Exception("RLE Compression not supported for characters > 127.");
        }

        mapEditor.storeCurrentMap();
        Map<ECMPalette, Integer> paletteMap = buildECMPaletteMap();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mapDataFile))) {
            if (includeCharData) {
                writeCharacterPatterns(startChar, endChar, includeComments, includeCharNumbers, bw);
            }
            if (includeColorData) {
                writeColors(startChar, endChar, includeComments, includeCharNumbers, paletteMap, bw);
            }
            if (includeSpriteData) {
                writeSpritePatterns(startSprite, endSprite, includeComments, bw);
            }
            if (includeMapData) {
                writeMapData(compression, includeComments, currMapOnly, includeSpriteData, paletteMap, bw);
            }

            bw.flush();
        }
    }

    // Build palette index map
    private Map<ECMPalette, Integer> buildECMPaletteMap() {
        Map<ECMPalette, Integer> map = new HashMap<>(ecmPalettes.length);
        int idx = 0;
        for (ECMPalette p : ecmPalettes) {
            map.put(p, idx++);
        }
        return map;
    }

    // ---------- Generic Print Helpers ----------

    private void printLine(BufferedWriter bw, String text) throws IOException {
        printLine(bw, text, true);
    }

    private void printLine(BufferedWriter bw, String text, String comment) throws IOException {
        if (comment == null || comment.isEmpty()) {
            printLine(bw, text, true);
        } else {
            printLine(bw, text + " ' " + comment, true);
        }
    }

    private void printLine(BufferedWriter bw, String text, boolean isCommand) throws IOException {
        if (!isCommand) bw.write("' ");
        bw.write(text);
        bw.newLine();
    }

    private void printSectionHeader(BufferedWriter bw, String title) throws IOException {
        printLine(bw, "----------------------------------------", false);
        printLine(bw, title, false);
        printLine(bw, "----------------------------------------", false);
    }

    /**
     * Appends one byte to the current line buffer.
     * If label!=null, flushes any existing buffer and starts a fresh line.
     * Returns updated count of bytes on this line.
     */
    @Override
    protected int printByte(BufferedWriter bw, StringBuilder buf, int countOnLine, int value, String label) throws IOException {
        if (label != null) {
            // flush prior
            if (buf.length() > 0) {
                printLine(bw, buf.toString(), false);
                buf.setLength(0);
            }
            buf.append(label)
               .append(":  DATA BYTE $")
               .append(Globals.toHexString(value & 0xFF, 2));
            countOnLine = 1;
        } else {
            if (countOnLine == 0) {
                buf.append("       DATA BYTE $")
                   .append(Globals.toHexString(value & 0xFF, 2));
            } else {
                buf.append(", $")
                   .append(Globals.toHexString(value & 0xFF, 2));
            }
            countOnLine++;
        }
        return countOnLine;
    }

    // ---------- Color Data ----------

private void writeColors(
    int startChar,
    int endChar,
    boolean includeComments,
    boolean includeCharNumbers,
    Map<ECMPalette,Integer> paletteMap,
    BufferedWriter bw
) throws IOException {
    // Section header
    if (includeComments) {
        printSectionHeader(bw, " Colorset Definitions");
    }

    // Graphics Mode 1: single combined foreground/background sets
    if (colorMode == COLOR_MODE_GRAPHICS_1) {
        // Count of color‐sets
        printLine(bw, "CLRNUM:  DATA BYTE " + clrSets.length, includeComments);

        // Dump in groups of four
        int cnt = 0;
        for (int i = 0; i < clrSets.length; i++) {
            if (cnt == 0) {
                lineBuffer.setLength(0);
                lineBuffer.append(i == 0 ? "CLRSET BYTE " : "       BYTE ");
            }
            if (cnt > 0) lineBuffer.append(", ");
            // two nibbles: fore, back
            lineBuffer.append('$')
                      .append(Integer.toHexString(
                          clrSets[i][Globals.INDEX_CLR_FORE]).toUpperCase())
                      .append(Integer.toHexString(
                          clrSets[i][Globals.INDEX_CLR_BACK]).toUpperCase());
            cnt++;
            if (cnt > 3) {
                printLine(bw, lineBuffer.toString(), includeComments);
                cnt = 0;
            }
        }
        // flush any remainder
        if (cnt > 0) {
            printLine(bw, lineBuffer.toString(), includeComments);
        }
    }

    // Bitmap mode: per-character two-row color pairs
    else if (colorMode == COLOR_MODE_BITMAP) {
        for (int ci = startChar; ci <= endChar; ci++) {
            int[][] cols = charColors.get(ci);
            if (cols == null) continue;

            if (includeCharNumbers) {
                printLine(bw,
                    "CCH" + ci + ":  DATA BYTE " + ci,
                    includeComments);
            }

            lineBuffer.setLength(0);
            lineBuffer.append("COL").append(ci).append(":  DATA BYTE ");
            for (int row = 0; row < 8; row += 2) {
                int[] a = cols[row], b = cols[row + 1];
                // each pair is $backFore
                lineBuffer.append('$')
                          .append(Integer.toHexString(a[1]).toUpperCase())
                          .append(Integer.toHexString(a[0]).toUpperCase())
                          .append(", $")
                          .append(Integer.toHexString(b[1]).toUpperCase())
                          .append(Integer.toHexString(b[0]).toUpperCase());
                if (row < 6) lineBuffer.append(", ");
            }
            printLine(bw, lineBuffer.toString(), null);
        }
    }

    // ECM-2 / ECM-3: multi-plane palette blocks + per-char tile attributes
    else if (colorMode == COLOR_MODE_ECM_2
          || colorMode == COLOR_MODE_ECM_3) {

        // 1) Emit each palette in 4-color chunks
        for (int pi = 0; pi < ecmPalettes.length; pi++) {
            ECMPalette pal = ecmPalettes[pi];
            int planes = pal.getSize() / 4;
            for (int block = 0; block < planes; block++) {
                lineBuffer.setLength(0);
                lineBuffer.append(
                    block == 0
                      ? "PAL" + pi + ":  DATA BYTE "
                      : "       DATA BYTE "
                );
                for (int c = 0; c < 4; c++) {
                    Color col = pal.getColor(block * 4 + c);
                    int r = (int)Math.round(col.getRed() / 17.0);
                    int g = (int)Math.round(col.getGreen() / 17.0);
                    int b = (int)Math.round(col.getBlue() / 17.0);
                    lineBuffer.append("$0")
                              .append(Integer.toHexString(r).toUpperCase())
                              .append(Integer.toHexString(g).toUpperCase())
                              .append(Integer.toHexString(b).toUpperCase())
                              .append(c < 3 ? ", " : "");
                }
                printLine(bw, lineBuffer.toString(), null);
            }
        }

        // 2) Emit per-char palette index + transparency
        if (includeComments) {
            printSectionHeader(bw, " Tile Attributes");
        }
        for (int ci = startChar; ci <= endChar; ci++) {
            int palIdx = paletteMap.get(ecmCharPalettes[ci]);
            int attr = (palIdx << (colorMode == COLOR_MODE_ECM_3 ? 1 : 0))
                       | (ecmCharTransparency[ci] ? 0x10 : 0);
            printLine(bw,
                "TAT" + ci + ":  DATA BYTE $"
                + Globals.toHexString(attr, 2),
                null);
        }
    }

    printLine(bw, "", null);
}

    // ---------- Character Patterns ----------

    private void writeCharacterPatterns(
        int startChar, int endChar,
        boolean includeComments, boolean includeCharNumbers,
        BufferedWriter bw
    ) throws IOException {
        String header = " Character Patterns"
                        + ((colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)
                           ? " Plane 0" : "");
        if (includeComments) printSectionHeader(bw, header);

        // Plane 0 (or only plane)
        for (int ci = startChar; ci <= endChar; ci++) {
            String hex = charGrids.get(ci) == null
                         ? Globals.BLANKCHAR
                         : Globals.getHexString(charGrids.get(ci)).toUpperCase();
            if (includeCharNumbers) {
                printLine(bw, "PCH" + ci + ":  DATA BYTE " + ci, null);
            }
            String label = (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP)
                           ? "PAT" + ci : "P0_" + ci;
            writeHexChunks(bw, label, hex, includeComments, 16, 16, null);
        }

        // Plane 1 (ECM 2+)
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            if (includeComments) printSectionHeader(bw, " Character Patterns Plane 1");
            for (int ci = startChar; ci <= endChar; ci++) {
                String hex = charGrids.get(ci) == null
                             ? Globals.BLANKCHAR
                             : Globals.getHexString(charGrids.get(ci), 2).toUpperCase();
                writeHexChunks(bw, "P1_" + ci, hex, includeComments, 16, 16, null);
            }
        }

        // Plane 2 (ECM3)
        if (colorMode == COLOR_MODE_ECM_3) {
            if (includeComments) printSectionHeader(bw, " Character Patterns Plane 2");
            for (int ci = startChar; ci <= endChar; ci++) {
                String hex = charGrids.get(ci) == null
                             ? Globals.BLANKCHAR
                             : Globals.getHexString(charGrids.get(ci), 4).toUpperCase();
                writeHexChunks(bw, "P2_" + ci, hex, includeComments, 16, 16, null);
            }
        }
        printLine(bw, "", null);
    }

    // ---------- Sprite Patterns ----------

    private void writeSpritePatterns(
        int startSprite, int endSprite,
        boolean includeComments,
        BufferedWriter bw
    ) throws IOException {
        int planes = (colorMode == COLOR_MODE_ECM_3) ? 3
                   : (colorMode == COLOR_MODE_ECM_2) ? 2
                   : 1;
        String[] prefixes = { "SPR", "S1_", "S2_" };

        for (int pl = 0; pl < planes; pl++) {
            String title = " Sprite Patterns"
                           + (pl > 0 ? " Plane " + pl : "");
            if (includeComments) printSectionHeader(bw, title);

            for (int si = startSprite; si <= endSprite; si++) {
                int[][] grid = spriteGrids.get(si);
                if (grid == null) continue;
                String hex = Globals.getSpriteHexString(grid, (int)Math.pow(2, pl))
                                     .toUpperCase();
                String label = prefixes[pl] + si;
                writeHexChunks(bw, label, hex, includeComments, 64, 16,
                               (pl == 0 && includeComments) ? "Color " + spriteColors[si] : null);
            }
        }
        printLine(bw, "", null);
    }

    // ---------- Map Data ----------

    private void writeMapData(
        int compression,
        boolean includeComments,
        boolean currMapOnly,
        boolean includeSpriteData,
        Map<ECMPalette, Integer> paletteMap,
        BufferedWriter bw
    ) throws Exception {
        if (includeComments) printSectionHeader(bw, "Map Data");
        int mapCount = currMapOnly ? 1 : mapEditor.getMapCount();
        printLine(bw, "MCOUNT:  DATA BYTE " + mapCount, null);

        for (int mid = 0; mid < mapEditor.getMapCount(); mid++) {
            if (currMapOnly && mid != mapEditor.getCurrentMapId()) continue;

            int[][] mapGrid = mapEditor.getMapData(mid);
            if (includeComments) {
                printLine(bw, " == Map #" + mid + " ==", false);
            }
            printLine(bw, "MC" + mid + ":  DATA BYTE " + mapEditor.getScreenColor(mid),
                      null);

            // dimensions
            lineBuffer.setLength(0);
            lineBuffer.append("MS").append(mid).append(":  DATA $")
                      .append(Globals.toHexString(mapGrid[0].length, 4))
                      .append(", $").append(Globals.toHexString(mapGrid.length, 4))
                      .append(", $").append(Globals.toHexString(
                          mapGrid.length * mapGrid[0].length, 4));
            printLine(bw, lineBuffer.toString(),
                      new String(includeComments ? "Width, Height, Size" : ""));
            lineBuffer.setLength(0);

            // data
            if (compression == MagellanExportDialog.COMPRESSION_NONE) {
                writeMapRows(mapGrid, mid, bw, includeComments);
            }
            else if (compression == MagellanExportDialog.COMPRESSION_RLE_BYTE) {
                writeRleByteData(mapGrid, mid, bw);
            }
            else if (compression == MagellanExportDialog.COMPRESSION_RLE_WORD) {
                writeRleWordData(mapGrid, mid, bw);
            }
            else if (compression == MagellanExportDialog.COMPRESSION_META_2
                  || compression == MagellanExportDialog.COMPRESSION_META_4
                  || compression == MagellanExportDialog.COMPRESSION_META_8) {
                int size = (compression == MagellanExportDialog.COMPRESSION_META_2) ? 2
                        : (compression == MagellanExportDialog.COMPRESSION_META_4) ? 4 : 8;
                writeMetaTileData(size, mapGrid, mid, bw, includeComments);
            }
            else if (compression == MagellanExportDialog.COMPRESSION_NYBBLES) {
                writeNybblesData(mapGrid, mid, bw, includeComments);
            }
            else {
                throw new RuntimeException("Unsupported compression mode: " + compression);
            }
            printLine(bw, "", null);

            // sprite locations
            if (includeSpriteData) {
                writeSpriteLocations(includeComments, paletteMap, bw, mid);
            }
        }
    }

    private void writeMapRows(
        int[][] grid, int mapId,
        BufferedWriter bw, boolean includeComments
    ) throws IOException {
        for (int y = 0; y < grid.length; y++) {
            int rowLen = grid[y].length;
            for (int block = 0; block < Math.ceil((double) rowLen / 8); block++) {
                lineBuffer.setLength(0);
                String label = (y == 0 && block == 0) ? "MD" + mapId : "";
                lineBuffer.append(label)
                          .append(label.isEmpty() ? "    " : ":")
                          .append("  DATA BYTE ");
                for (int x = block * 8; x < Math.min((block + 1) * 8, rowLen); x++) {
                    int v = grid[y][x];
                    if (v == MapCanvas.NOCHAR) v = 0;
                    lineBuffer.append("$")
                              .append(Globals.toHexString(v & 0xFF, 2))
                              .append(x < (block + 1) * 8 - 1 ? ", " : "");
                }
                if (includeComments && (block % 4) == 0) {
                    lineBuffer.append(" ' Map Row " + y);
                }
                printLine(bw, lineBuffer.toString(), null);
            }
        }
    }

    // ---------- RLE-BYTE ----------

    private void writeRleByteData(int[][] grid, int mapId, BufferedWriter bw) throws IOException {
        int last = -1, run = 0, countOnLine = 0;
        for (int[] row : grid) {
            for (int cur : row) {
                if (last >= 0) {
                    if (cur == last && run < 255) {
                        run++;
                    } else {
                        countOnLine = printRlePair(bw, lineBuffer, countOnLine, last | 0x80, run,
                                                   countOnLine == 0 ? "MD" + mapId : null);
                        run = 0;
                    }
                }
                last = cur;
            }
        }
        // final flush
        if (run > 0) {
            countOnLine = printRlePair(bw, lineBuffer, countOnLine, last | 0x80, run,
                                       countOnLine == 0 ? "MD" + mapId : null);
        } else if (last >= 0) {
            countOnLine = printByte(bw, lineBuffer, countOnLine, last,
                                    countOnLine == 0 ? "MD" + mapId : null);
        }
        // end marker
        countOnLine = printByte(bw, lineBuffer, countOnLine, 0x80, null);
        countOnLine = printByte(bw, lineBuffer, countOnLine, 0x00, null);
        printLine(bw, lineBuffer.toString(), null);
    }

    private int printRlePair(
        BufferedWriter bw, StringBuilder buf,
        int countOnLine, int value, int repeat, String label
    ) throws IOException {
        countOnLine = printByte(bw, buf, countOnLine, value, label);
        return printByte(bw, buf, countOnLine, repeat, null);
    }

    // ---------- RLE-WORD ----------

    private void writeRleWordData(int[][] grid, int mapId, BufferedWriter bw) throws IOException {
        int last = -1, run = 0, countOnLine = 0;
        for (int[] row : grid) {
            for (int x = 0; x < row.length - 1; x += 2) {
                int cur = (row[x] << 8) | row[x + 1];
                if (last >= 0) {
                    if (cur == last && run < 255) {
                        run++;
                    } else {
                        // flush run
                        countOnLine = printRleWordPair(bw, lineBuffer, countOnLine, last, run,
                                                       countOnLine == 0 ? "MD" + mapId : null);
                        run = 0;
                    }
                }
                last = cur;
            }
        }
        // final flush
        if (run > 0) {
            countOnLine = printRleWordPair(bw, lineBuffer, countOnLine, last, run,
                                           countOnLine == 0 ? "MD" + mapId : null);
        } else if (last >= 0) {
            countOnLine = printByte(bw, lineBuffer, countOnLine, (last >> 8) & 0xFF,
                                    countOnLine == 0 ? "MD" + mapId : null);
            countOnLine = printByte(bw, lineBuffer, countOnLine, last & 0xFF, null);
        }
        // end marker (MSB + two zeros)
        countOnLine = printByte(bw, lineBuffer, countOnLine, 0x80, null);
        countOnLine = printByte(bw, lineBuffer, countOnLine, 0x00, null);
        countOnLine = printByte(bw, lineBuffer, countOnLine, 0x00, null);
        printLine(bw, lineBuffer.toString(), null);
    }

    private int printRleWordPair(
        BufferedWriter bw, StringBuilder buf,
        int countOnLine, int value, int repeat, String label
    ) throws IOException {
        // MSB of word + repeat
        countOnLine = printByte(bw, buf, countOnLine, ((value >> 8) & 0xFF) | 0x80, label);
        countOnLine = printByte(bw, buf, countOnLine, value & 0xFF, null);
        return printByte(bw, buf, countOnLine, repeat, null);
    }

    // ---------- Meta-Tile ----------

private void writeMetaTileData(
    int blockSize,
    int[][] mapGrid,
    int mapId,
    BufferedWriter bw,
    boolean includeComments
) throws Exception {
    // ensure dimensions are multiples of the block size
    if (mapGrid.length % blockSize != 0
        || mapGrid[0].length % blockSize != 0) {
        throw new Exception(
            "Map dimensions must be a multiple of meta-tile size "
            + blockSize
        );
    }

    int metaRows = mapGrid.length  / blockSize;
    int metaCols = mapGrid[0].length / blockSize;

    // Deduplication lookup: key = concatenated hex of each sub-tile
    Map<String,MetaTile> lookup = new LinkedHashMap<>();
    MetaTile[][] metaGrid   = new MetaTile[metaRows][metaCols];
    int nextMetaId = 0;

    // Build & dedupe meta-tiles
    for (int ry = 0; ry < metaRows; ry++) {
        for (int cx = 0; cx < metaCols; cx++) {
            StringBuilder keyBuilder = new StringBuilder(blockSize * blockSize * 2);
            int[] block = new int[blockSize * blockSize];

            for (int by = 0; by < blockSize; by++) {
                for (int bx = 0; bx < blockSize; bx++) {
                    int tile = mapGrid[ry*blockSize + by][cx*blockSize + bx];
                    block[by*blockSize + bx] = tile;
                    keyBuilder
                        .append(Globals.toHexString(tile, 2));
                }
            }

            String key = keyBuilder.toString();
            MetaTile mt = lookup.get(key);
            if (mt == null) {
                mt = new MetaTile(nextMetaId++, block);
                lookup.put(key, mt);
            }
            metaGrid[ry][cx] = mt;
        }
    }

    if (lookup.size() > 256) {
        throw new Exception(
            "Cannot support more than 256 meta-tiles (found "
            + lookup.size() + ")"
        );
    }

    // Emit the meta-tile map (indices into the deduped set)
    if (includeComments) {
        printSectionHeader(bw, " Meta Tile Map");
    }
    writeMetaTileMap(bw, metaGrid, mapId);

    // Emit the meta-tile definitions themselves
    if (includeComments) {
        printSectionHeader(bw, " Meta Tile Definitions");
    }
    writeMetaTileDefinitions(bw, lookup.values(), mapId, blockSize);
}

    private void writeMetaTileMap(
        BufferedWriter bw, MetaTile[][] grid, int mapId
    ) throws IOException {
        int lineCount = 0;
        boolean first = true;
        for (MetaTile[] row : grid) {
            for (MetaTile mt : row) {
                lineCount = printByte(bw, lineBuffer, lineCount,
                                      mt.getNumber(),
                                      first ? "MD" + mapId : null);
                first = false;
            }
        }
        printLine(bw, lineBuffer.toString(), null);
    }

    private void writeMetaTileDefinitions(
        BufferedWriter bw, Collection<MetaTile> tiles, int mapId, int blockSize
    ) throws IOException {
        List<MetaTile> list = new ArrayList<>(tiles);
        list.sort(Comparator.comparingInt(MetaTile::getNumber));
        for (MetaTile mt : list) {
            lineBuffer.setLength(0);
            int cnt = 0;
            boolean first = true;
            for (int t : mt.getTiles()) {
                cnt = printByte(bw, lineBuffer, cnt, t,
                                first ? "MT" + mapId + Globals.padl(mt.getNumber(), 3) : null);
                first = false;
            }
            // pad up to 8
            while (cnt < blockSize) {
                cnt = printByte(bw, lineBuffer, cnt, 0, null);
            }
            printLine(bw, lineBuffer.toString(), null);
        }
    }

    // ---------- NYBBLES ----------

    private void writeNybblesData(
        int[][] grid, int mapId,
        BufferedWriter bw, boolean includeComments
    ) throws IOException {
        for (int y = 0; y < grid.length; y++) {
            if (includeComments) {
                printLine(bw, " -- Map Row " + y + " -- ", null);
            }
            int rowLen = grid[y].length;
            for (int block = 0; block < Math.ceil((double) rowLen / 16); block++) {
                lineBuffer.setLength(0);
                String lbl = (y == 0 && block == 0) ? "MD" + mapId : "";
                lineBuffer.append(lbl)
                          .append(lbl.isEmpty() ? "" : ":")
                          .append("  DATA BYTE ");
                for (int x = block * 16; x < Math.min((block + 1) * 16, rowLen); x++) {
                    int v = grid[y][x];
                    if (v >= 16) {
                        throw new RuntimeException("NYBBLES only supports <16");
                    }
                    String hex = (v == MapCanvas.NOCHAR)
                                 ? "0"
                                 : Integer.toHexString(v).toUpperCase();
                    lineBuffer.append(hex)
                              .append((x + 1) % 4 == 0 && x + 1 < (block + 1) * 16 ? "," : "");
                }
                printLine(bw, lineBuffer.toString(), null);
            }
        }
    }

    // ---------- Sprite Locations ----------

  private void writeSpriteLocations(
      boolean includeComments,
      Map<ECMPalette,Integer> paletteMap,
      BufferedWriter bw,
      int mapId
  ) throws IOException {
      Map<Point,ArrayList<Integer>> spr = mapEditor.getSpriteMap(mapId);
      if (spr.isEmpty()) return;

      if (includeComments) printSectionHeader(bw, " Sprite Locations");

      boolean small = mapEditor.getGridWidth() <= 32
                  && mapEditor.getGridHeight() <= 32;

      // Build ordered points
      List<Point> pts = new ArrayList<>(spr.keySet());
      pts.sort(Comparator
        .comparingInt((Point p) -> p.y)
        .thenComparingInt(p -> p.x)
      );

      // inline comment only on first line
      String comment = includeComments ? " ' y, x, pattern#, color#" : "";

      // Prepare line-prefix strings
      String firstLabel   = String.format("SL%d:%s\n  DATA BYTE ", mapId, comment);
      int    prefixWidth  = firstLabel.length();
      String otherLabel   = String.format("  DATA BYTE ", mapId);
      String commaPattern = "%3d, %3d, %3d, %3d";  // y, x, pattern#, color#

      boolean first = true;
      for (Point p : pts) {
        for (int sid : spr.get(p)) {
          int yval   = small ? ((p.y - 1) & 0xFF) : p.y;
          int color  = (colorMode == COLOR_MODE_GRAPHICS_1
                    || colorMode == COLOR_MODE_BITMAP)
                    ? spriteColors[sid]
                    : paletteMap.get(ecmSpritePalettes[sid]) 
                      * (colorMode == COLOR_MODE_ECM_3 ? 2 : 1);

          String label = first ? firstLabel : otherLabel;
          String row   = String.format(commaPattern,
                            yval,
                            p.x,
                            sid * 4,
                            color
                          );

          printLine(bw, label + row, null);

          first = false;
        }
      }

      // final blank line for readability
      printLine(bw, "", null);
  }

    // ---------- Shared Chunk Writer ----------

    private void writeHexChunks(
        BufferedWriter bw,
        String label,
        String hex,
        boolean includeComments,
        int totalLength,
        int lineSize,
        String firstComment
    ) throws IOException {
        int len = totalLength;
        for (int pos = 0; pos < len; pos += 2) {
            if (pos % lineSize == 0) {
                if (pos != 0) {
                    printLine(bw, lineBuffer.toString(), includeComments ? firstComment : null);
                }
                lineBuffer.setLength(0);
                lineBuffer.append(pos == 0 ? label + ":  DATA BYTE " : "        DATA BYTE ");
            }
            lineBuffer.append('$')
                      .append(hex, pos, pos + 2)
                      .append((pos % lineSize) < lineSize - 2 ? ", " : "");
        }
        // flush last
        printLine(bw, lineBuffer.toString(), includeComments ? firstComment : null);
   }
}