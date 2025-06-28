package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static com.dreamcodex.ti.util.ColorMode.*;

public class CharacterImageColorImporter extends Importer {

    private final Preferences preferences;

    public CharacterImageColorImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
        this.preferences = preferences;
    }

    public void readCharImageColor(BufferedImage buffImg, int startIndex, int endIndex, int gap, boolean skipBlank) {
        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        int size = 8 + gap;
        int cols = (width + gap) / size;
        int rows = (height + gap) / size;
        // get colorsets
        if (colorMode == COLOR_MODE_GRAPHICS_1) {
            cols = Math.min(cols, 8);
            rows = Math.min(rows, 32);
            width = cols * size - gap;
            height = rows * size - gap;
            for (int cs = 0; cs < rows; cs++) {
                int colorFore = -1;
                int colorBack = -1;
                int testY = 0;
                int testX = 0;
                while (testY < 8 && (colorFore == -1 || colorBack == -1)) {
                    int pixelColor = buffImg.getRGB(testX, (cs * 8) + testY);
                    int tiColor = getTIColorForPixel(pixelColor);
                    if ((tiColor > 0) && (colorFore == -1)) {
                        colorFore = tiColor;
                    } else if ((tiColor > 0) && (tiColor != colorFore)) {
                        colorBack = tiColor;
                    }
                    testX++;
                    if (testX % size == size - 1) {
                        testX += gap;
                    }
                    if (testX >= width) {
                        testX = 0;
                        testY++;
                        if (testY % size == size - 1) {
                            testY += gap;
                        }
                    }
                }
                if (colorFore == -1) {
                    colorFore = 1;
                }
                if (colorBack == -1) {
                    colorBack = 0;
                }
                clrSets[cs][Globals.INDEX_CLR_FORE] = colorFore;
                clrSets[cs][Globals.INDEX_CLR_BACK] = colorBack;
            }
        }
        // Global ECM palette
        ECMPalette ecmGlobalPalette = null;
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            ECMPalette basePalette = ecmPalettes[0];
            int ecmColorIndex = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = buffImg.getRGB(x, y);
                    boolean found = false;
                    for (int i = 0; i < ecmColorIndex && !found; i++) {
                        if (basePalette.getColor(i).getRGB() == color) {
                            found = true;
                        }
                    }
                    if (!found && ecmColorIndex < basePalette.getSize()) {
                        basePalette.setColor(ecmColorIndex++, new Color(color));
                    }
                }
            }
            if (ecmColorIndex < basePalette.getSize()) {
                for (int i = ecmColorIndex; i < basePalette.getSize(); i++) {
                    basePalette.setColor(i, Color.WHITE);
                }
                basePalette.sort();
                ecmGlobalPalette = basePalette;
            }
        }
        // get character glyphs
        int row = 0;
        int col = 0;
        int cSet = 0;
        int ecmPaletteIndex = 1;
        for (int charNum = startIndex; charNum <= endIndex; charNum++) {
            if (col < cols && row < rows) {
                int[][] newCharArray = new int[8][8];
                int[][] newColorArray = new int[8][2];
                // ECM palette for character
                int[] ecmColors = colorMode == COLOR_MODE_ECM_2 ? new int[4] : (colorMode == COLOR_MODE_ECM_3 ? new int[8] : null);
                int ecmColorIndex = 0;
                if (ecmGlobalPalette == null && (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3)) {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int color = buffImg.getRGB((col * size) + x, (row * size) + y);
                            boolean found = false;
                            for (int i = 0; i < Math.min(ecmColorIndex, ecmColors.length) && !found; i++) {
                                if (ecmColors[i] == color) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                if (ecmColorIndex < ecmColors.length) {
                                    ecmColors[ecmColorIndex++] = color;
                                }
                            }
                        }
                    }
                    for (int i = ecmColorIndex; i < ecmColors.length; i++) {
                        ecmColors[i] = Color.WHITE.getRGB();
                    }
                    Arrays.sort(ecmColors);
                }
                // Colors
                for (int y = 0; y < 8; y++) {
                    int[] newColors = newColorArray[y];
                    newColors[0] = -1;
                    newColors[1] = -1;
                    for (int x = 0; x < 8; x++) {
                        int color = buffImg.getRGB((col * size) + x, (row * size) + y) | 0xff000000;
                        if (colorMode == COLOR_MODE_GRAPHICS_1) {
                            newCharArray[y][x] = (color == TIGlobals.TI_PALETTE_OPAQUE[clrSets[cSet][Globals.INDEX_CLR_FORE]].getRGB() ? 1 : 0);
                        } else if (colorMode == COLOR_MODE_BITMAP) {
                            int tiColor = getTIColorForPixel(color);
                            if (newColors[0] == -1) {
                                newColors[0] = tiColor;
                            } else if (newColors[1] == -1 && newColors[0] != tiColor) {
                                newColors[1] = tiColor;
                            }
                            newCharArray[y][x] = tiColor == newColors[0] ? 0 : 1;
                        } else {
                            boolean found = false;
                            if (ecmGlobalPalette == null) {
                                for (int i = 0; i < ecmColors.length && !found; i++) {
                                    if (ecmColors[i] == color) {
                                        newCharArray[y][x] = i;
                                        found = true;
                                    }
                                }
                            } else {
                                for (int i = 0; i < ecmGlobalPalette.getSize() && !found; i++) {
                                    if (ecmGlobalPalette.getColor(i).getRGB() == color) {
                                        newCharArray[y][x] = i;
                                        found = true;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!(skipBlank && Globals.isGridEmpty(newCharArray))) {
                    charGrids.put(charNum, newCharArray);
                    if (colorMode == COLOR_MODE_BITMAP) {
                        charColors.put(charNum, newColorArray);
                    }
                    if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                        if (ecmGlobalPalette == null) {
                            // Match char palette to existing palettes
                            boolean found = false;
                            for (int i = 0; i < Math.min(ecmPaletteIndex, ecmPalettes.length); i++) {
                                if (ecmPalettes[i].startsWith(ecmColors, ecmColorIndex)) {
                                    ecmCharPalettes[charNum] = ecmPalettes[i];
                                    found = true;
                                }
                            }
                            if (!found) {
                                if (ecmPaletteIndex < ecmPalettes.length - 1) {
                                    ecmPalettes[ecmPaletteIndex].setIntColors(ecmColors);
                                    ecmCharPalettes[charNum] = ecmPalettes[ecmPaletteIndex];
                                    ecmPaletteIndex++;
                                } else {
                                    ecmCharPalettes[charNum] = ecmPalettes[ecmPaletteIndex];
                                }
                            }
                        } else {
                            ecmCharPalettes[charNum] = ecmGlobalPalette;
                        }
                    }
                }
            }
            col++;
            if (col >= cols) {
                col = 0;
                row++;
                cSet++;
            }
        }
    }

    protected int getTIColorForPixel(int pixelRBG) {
        return Globals.getClosestColorIndex(new Color(pixelRBG), TIGlobals.TI_PALETTE_OPAQUE);
    }
}
