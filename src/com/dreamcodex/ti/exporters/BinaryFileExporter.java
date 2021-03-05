package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static com.dreamcodex.ti.Magellan.COLOR_MODE_BITMAP;

public class BinaryFileExporter extends Exporter {

    public BinaryFileExporter(MapEditor mapdMain, ECMPalette[] ecmPalettes, int[][] clrSets, HashMap<Integer, int[][]> hmCharGrids, HashMap<Integer, int[][]> hmCharColors, ECMPalette[] ecmCharPalettes, boolean[] ecmCharTransparency, HashMap<Integer, int[][]> hmSpriteGrids, int[] spriteColors, ECMPalette[] ecmSpritePalettes, int colorMode) {
        super(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
    }

    public void writeBinaryFile(File mapDataFile, byte chunkFlags, int startChar, int endChar, boolean currMapOnly) throws IOException {
        // store working map first
        mapdMain.storeCurrentMap();
        // get file output buffer
        FileOutputStream fos = new FileOutputStream(mapDataFile);

        // write File Header
        fos.write(BIN_HEADER_MAG);
        fos.write(BIN_HEADER_VER);
        fos.write(chunkFlags);
        byte mapCount = (byte) (currMapOnly ? 1 : mapdMain.getMapCount());
        fos.write(mapCount);

        // write Colorset Chunk (if present)
        if ((chunkFlags & BIN_CHUNK_COLORS) == BIN_CHUNK_COLORS) {
            if (colorMode == COLOR_MODE_BITMAP) {
                byte charCount = (byte) ((endChar - startChar) + 1);
                fos.write(charCount);
                for (int bc = startChar; bc <= endChar; bc++) {
                    fos.write(getColorBytes(bc));
                }
            }
            else {
                for (int i = 0; i < clrSets.length; i++) {
                    fos.write((byte) (clrSets[i][Globals.INDEX_CLR_FORE] << 4 | clrSets[i][Globals.INDEX_CLR_BACK]));
                }
            }
        }

        // write Character Chunk (if present)
        if ((chunkFlags & BIN_CHUNK_CHARS) == BIN_CHUNK_CHARS) {
            byte charCount = (byte) ((endChar - startChar) + 1);
            fos.write(charCount);
            for (int bc = startChar; bc <= endChar; bc++) {
                fos.write(getCharBytes(bc));
            }
        }

        // write Maps
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            if (!currMapOnly || m == mapdMain.getCurrentMapId()) {
                int[][] mapToSave = mapdMain.getMapData(m);
                int mapCols = mapToSave[0].length;
                int mapRows = mapToSave.length;
                int mapSize = mapCols * mapRows;
                int mapScreenColor = mapdMain.getScreenColor(m);

                // write Map Header
                //   reserved bytes for Magellan use
                fos.write(BIN_MAP_HEADER_RESERVED1);
                fos.write(BIN_MAP_HEADER_RESERVED2);
                fos.write(BIN_MAP_HEADER_RESERVED3);
                fos.write(BIN_MAP_HEADER_RESERVED4);
                //   map size as a series of bytes
                int mapSizeChunk = mapSize;
                for (int i = 0; i < 8; i++) {
                    if (mapSizeChunk > 255) {
                        fos.write(255);
                        mapSizeChunk -= 255;
                    }
                    else if (mapSizeChunk > 0) {
                        fos.write((byte) mapSizeChunk);
                        mapSizeChunk = 0;
                    }
                    else {
                        fos.write(0);
                    }
                }
                //   map columns as a byte pair
                if (mapCols > 255) {
                    fos.write(255);
                    fos.write(mapCols - 255);
                }
                else {
                    fos.write(mapCols);
                    fos.write(0);
                }
                //   map rows as a byte pair
                if (mapRows > 255) {
                    fos.write(255);
                    fos.write(mapRows - 255);
                }
                else {
                    fos.write(mapRows);
                    fos.write(0);
                }
                //   map screen color
                fos.write(mapScreenColor);

                // write Map Data
                for (int y = 0; y < mapRows; y++) {
                    for (int x = 0; x < mapCols; x++) {
                        fos.write(mapToSave[y][x]);
                    }
                }

                // write Sprite Chunk (if present)
                if ((chunkFlags & BIN_CHUNK_SPRITES) == BIN_CHUNK_SPRITES) {
                    // not yet implemented
                }
            }
        }

        fos.flush();
        fos.close();
    }

    protected byte[] getColorBytes(int charnum) {
        byte[] colorbytes = new byte[8];
        int[][] colorarray = hmCharColors.get(charnum);
        for (int y = 0; y < 8; y++) {
            byte background = (byte) colorarray[y][0];
            byte foreground = (byte) (colorarray[y][1] << 4);
            colorbytes[y] = (byte) (background | foreground);
        }
        return colorbytes;
    }

    protected byte[] getCharBytes(int charnum) {
        byte[] charbytes = new byte[8];
        int[][] chararray = hmCharGrids.get(charnum);
        if (chararray != null) {
            int bcount = 0;
            byte byteval = (byte) 0;
            int bytepos = 0;
            boolean goHigh = true;
            for (int y = 0; y < 8; y++) {
                bytepos = 0;
                for (int x = 0; x < 8; x++) {
                    if (chararray[y][x] > 0) {
                        byteval = (byte) (byteval | (bytepos == 0 ? 8 : (bytepos == 1 ? 4 : (bytepos == 2 ? 2 : 1))));
                    }
                    bytepos++;
                    if (bytepos > 3) {
                        charbytes[bcount] = (byte) (charbytes[bcount] | (goHigh ? byteval << 4 : byteval));
                        if (!goHigh) {
                            bcount++;
                        }
                        goHigh = !goHigh;
                        byteval = (byte) 0;
                        bytepos = 0;
                    }
                }
            }
        }
        else {
            for (int i = 0; i < charbytes.length; i++) {
                charbytes[i] = (byte) 0;
            }
        }
        return charbytes;
    }
}
