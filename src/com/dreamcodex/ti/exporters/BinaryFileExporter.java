package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;

public class BinaryFileExporter extends Exporter {

    public BinaryFileExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeBinaryFile(File mapDataFile, byte chunkFlags, int startChar, int endChar, int startSprite, int endSprite, boolean currMapOnly) throws IOException {
        // store working map first
        mapEditor.storeCurrentMap();
        // get file output buffer
        FileOutputStream fos = new FileOutputStream(mapDataFile);

        // write File Header
        fos.write(BIN_HEADER_MAG);
        fos.write(BIN_HEADER_VER);
        fos.write(chunkFlags);
        byte mapCount = (byte) (currMapOnly ? 1 : mapEditor.getMapCount());
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

        // write Sprite Chunk (if present)
        if ((chunkFlags & BIN_CHUNK_SPRITES) == BIN_CHUNK_SPRITES) {
            byte spriteCount = (byte) ((endSprite - startSprite) + 1);
            fos.write(spriteCount);
            for (int bc = startSprite; bc <= endSprite; bc++) {
                fos.write(getSpriteBytes(bc));
            }
        }

        // write Maps
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            if (!currMapOnly || m == mapEditor.getCurrentMapId()) {
                int[][] mapToSave = mapEditor.getMapData(m);
                int mapCols = mapToSave[0].length;
                int mapRows = mapToSave.length;
                int mapSize = mapCols * mapRows;
                int mapScreenColor = mapEditor.getScreenColor(m);

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
            }
        }

        fos.flush();
        fos.close();
    }

    protected byte[] getColorBytes(int charnum) {
        byte[] colorbytes = new byte[8];
        int[][] colorarray = charColors.get(charnum);
        for (int y = 0; y < 8; y++) {
            byte background = (byte) colorarray[y][0];
            byte foreground = (byte) (colorarray[y][1] << 4);
            colorbytes[y] = (byte) (background | foreground);
        }
        return colorbytes;
    }

    protected byte[] getCharBytes(int charnum) {
        byte[] charbytes = new byte[8];
        int[][] chararray = charGrids.get(charnum);
        if (chararray != null) {
            writeCharBytes(chararray, 0, 0, charbytes, 0);
        }
        return charbytes;
    }

    protected byte[] getSpriteBytes(int spritenum) {
        byte[] spritebytes = new byte[32];
        int[][] spritearray = spriteGrids.get(spritenum);
        if (spritearray != null) {
            writeCharBytes(spritearray, 0, 0, spritebytes, 0);
            writeCharBytes(spritearray, 0, 8, spritebytes, 8);
            writeCharBytes(spritearray, 8, 0, spritebytes, 16);
            writeCharBytes(spritearray, 8, 8, spritebytes, 24);
        }
        return spritebytes;
    }

    protected void writeCharBytes(int[][] grid, int x0, int y0, byte[] result, int i) {
        for (int y = y0; y < y0 + 8; y++) {
            int bit = 0x80;
            int b = 0;
            for (int x = x0; x < x0 + 8; x++) {
                if (grid[y][x] != 0) {
                    b |= bit;
                }
                bit >>= 1;
            }
            result[i++] = (byte) b;
        }
    }
}
