package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TIGlobals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import static com.dreamcodex.ti.Magellan.COLOR_MODE_BITMAP;

public class VRAMDumpImporter extends Importer {

    public VRAMDumpImporter(MapEditor mapdMain, ECMPalette[] ecmPalettes, int[][] clrSets, HashMap<Integer, int[][]> hmCharGrids, HashMap<Integer, int[][]> hmCharColors, ECMPalette[] ecmCharPalettes, boolean[] ecmCharTransparency, HashMap<Integer, int[][]> hmSpriteGrids, int[] spriteColors, ECMPalette[] ecmSpritePalettes, int colorMode) {
        super(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
    }

    public void readVramDumpFile(File vramDumpFile, int charTableOffset, int mapTableOffset, int colorTableOffset, int spriteTableOffset, int spriteAttrOffset, boolean bitmapMode) throws IOException {
        FileInputStream fib = new FileInputStream(vramDumpFile);
        boolean basicOffset = false;
        if (charTableOffset == mapTableOffset) {
            basicOffset = true;
        }
        int readPos = 0;
        int readInt;
        int charStart = TIGlobals.MIN_CHAR;
        int charEnd = TIGlobals.MAX_CHAR;
        StringBuilder sbChar = new StringBuilder();
        int charByte = 0;
        int charRead = charStart;
        int charTableLength = 2048;
        int colorTableLength = bitmapMode ? 2048 : 32;
        int mapCols = 32;
        int mapRows = 24;
        int mapTableLength = (mapCols * mapRows);
        int spriteTableLength = 2048;
        int spriteAttrLength = 128;
        int spritePatternByte = 0;
        while ((readInt = fib.read()) != -1) {
            if ((readPos >= charTableOffset) && (readPos < (charTableOffset + charTableLength))) {
                if (charRead <= charEnd) {
                    sbChar.append(readInt < 16 ? "0" : "").append(Integer.toHexString(readInt));
                    charByte++;
                    if (charByte >= 8) {
                        int mapChar = charRead;
                        if (basicOffset)
                            mapChar = (mapChar + 0xA0) & 0xFF;
                        hmCharGrids.put(mapChar, Globals.getIntGrid(sbChar.toString(), 8));
                        charRead++;
                        charByte = 0;
                        sbChar.delete(0, sbChar.length());
                    }
                }
            }
            if (readPos >= mapTableOffset && readPos < (mapTableOffset + mapTableLength)) {
                int mapCell = readPos - mapTableOffset;
                int mapRow = (int) (Math.floor(mapCell / mapCols));
                int mapCol = mapCell % mapCols;
                int mapChar = readInt;
                if (basicOffset)
                    mapChar = (mapChar + 0xA0) & 0xFF;
                mapdMain.setGridAt(mapCol, mapRow, mapChar);
            }
            if (readPos >= colorTableOffset && readPos < (colorTableOffset + colorTableLength)) {
                if (bitmapMode) {
                    if (colorMode == COLOR_MODE_BITMAP) {
                        int colorByte = readPos - colorTableOffset;
                        int colorChar = colorByte / 8;
                        int[][] colorGrid = hmCharColors.get(colorChar);
                        if (colorGrid == null) {
                            colorGrid = new int[8][2];
                            hmCharColors.put(colorChar, colorGrid);
                        }
                        int row = colorByte % 8;
                        colorGrid[row][0] = readInt & 0x0F;
                        colorGrid[row][1] = (readInt & 0xF0) >> 4;
                    }
                }
                else {
                    int setNum = readPos - colorTableOffset;
                    int colorFore = (readInt & 0xF0) >> 4;
                    int colorBack = (readInt & 0x0F);
                    if (basicOffset)
                        setNum = (setNum + 20) & 0x1F;
                    clrSets[setNum][Globals.INDEX_CLR_FORE] = colorFore;
                    clrSets[setNum][Globals.INDEX_CLR_BACK] = colorBack;
                    if (colorMode == COLOR_MODE_BITMAP) {
                        for (int colorChar = setNum * 8; colorChar < setNum * 8 + 8; colorChar++) {
                            int[][] colorGrid = hmCharColors.get(colorChar);
                            if (colorGrid == null) {
                                colorGrid = new int[8][2];
                                hmCharColors.put(colorChar, colorGrid);
                            }
                            for (int row = 0; row < 8; row++) {
                                colorGrid[row][0] = colorBack;
                                colorGrid[row][1] = colorFore;
                            }
                        }
                    }
                }
            }
            if (readPos >= spriteTableOffset && readPos < spriteTableOffset + spriteTableLength) {
                int offset = readPos - spriteTableOffset;
                int spriteNum = offset / 32;
                int byteOffset = offset % 32;
                int x0 = byteOffset >= 16 ? 8 : 0;
                int y0 = byteOffset >= 16 ? byteOffset - 16 : byteOffset;
                int mask = 0x80;
                int[][] grid = hmSpriteGrids.get(spriteNum);
                int[] row = grid[y0];
                for (int x = x0; x < x0 + 8; x++) {
                    row[x] = (readInt & mask) != 0 ? 1 : 0;
                    mask >>= 1;
                }
            }
            if (readPos >= spriteAttrOffset && readPos < spriteAttrOffset + spriteAttrLength) {
                int offset = readPos - spriteAttrOffset;
                if (offset % 4 == 2) {
                    spritePatternByte = readInt;
                }
                if (offset % 4 == 3) {
                    spriteColors[spritePatternByte / 4] = readInt & 0x0f;
                }
            }
            readPos++;
        }
        fib.close();
    }
}
