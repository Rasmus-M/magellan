package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.util.Globals.getECMSafeColor;

public class SpriteImageImporter extends Importer {

    int firstPalette;
    int lastPalette;
    int nColors;

    public SpriteImageImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readSpriteFile(File file, int spriteIndex, int minPalette, int maxPalette, int gap) throws Exception {
        int size = 16 + gap;
        BufferedImage image = ImageIO.read(file);
        ColorModel colorModel = image.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel indexColorModel = (IndexColorModel) colorModel;
            if (indexColorModel.getMapSize() <= 256) {
                nColors = colorMode == COLOR_MODE_ECM_2 ? 4 : 8;
                firstPalette = minPalette;
                lastPalette = minPalette;
                Raster raster = image.getRaster();
                int xSprites = image.getWidth() / size;
                int ySprites = image.getHeight() / size;
                for (int sy = 0; sy < ySprites; sy++) {
                    int y0 = sy * size;
                    for (int sx = 0; sx < xSprites; sx++) {
                        int x0 = sx * size;
                        if (spriteIndex <= TIGlobals.MAX_SPRITE) {
                            if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                                spriteIndex = importSprite(raster, indexColorModel, x0, y0, spriteIndex);
                            } else {
                                spriteIndex = importECMSprite(raster, indexColorModel, x0, y0, spriteIndex, minPalette, maxPalette);
                            }
                        }
                    }
                }
            }
            else {
                throw new Exception("Palette size must be max 256 colors.");
            }
        }
        else {
            throw new Exception("Image must be using an indexed color model.");
        }
    }

    private int importSprite(Raster raster, IndexColorModel indexColorModel, int x0, int y0, int spriteIndex) {
        Map<Integer, int[][]> colorLayers = new TreeMap<Integer, int[][]>();
        int[] pixel = new int[1];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                raster.getPixel(x + x0, y + y0, pixel);
                int colorIndex = Math.min(pixel[0], 15);
                if (colorIndex != 0) { // 0 is transparent
                    int[][] colorLayer = colorLayers.computeIfAbsent(colorIndex, k -> new int[16][16]);
                    colorLayer[y][x] = 1;
                }
            }
        }
        for (int colorIndex : colorLayers.keySet()) {
            hmSpriteGrids.put(spriteIndex, colorLayers.get(colorIndex));
            spriteColors[spriteIndex] = colorIndex;
            spriteIndex++;
        }
        return spriteIndex;
    }

    private int importECMSprite(Raster raster, IndexColorModel indexColorModel, int x0, int y0, int spriteIndex, int minPalette, int maxPalette) {
        Color[][] colorGrid = new Color[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int[] pixel = new int[1];
                raster.getPixel(x + x0, y + y0, pixel);
                int colorIndex = pixel[0];
                colorGrid[y][x] = colorIndex != 0 ? getECMSafeColor(new Color(indexColorModel.getRGB(colorIndex))) : null;
            }
        }
        ECMPalette optimalPalette = getPaletteForGrid(colorGrid, nColors);
        ECMPalette palette = findExistingPalette(optimalPalette);
        if (palette == null && lastPalette <= maxPalette) {
            palette = optimalPalette;
            lastPalette++;
            ecmPalettes[lastPalette] = palette;
        }
        if (palette != null) {
            int[][] grid = getGridForPalette(colorGrid, palette);
            hmSpriteGrids.put(spriteIndex, grid);
            ecmSpritePalettes[spriteIndex] = palette;
            spriteIndex++;
        }
        return spriteIndex;
    }

    private ECMPalette findExistingPalette(ECMPalette newPalette) {
        for (int i = firstPalette; i < lastPalette; i++) {
            ECMPalette existingPalette = ecmPalettes[i];
            if (existingPalette.contains(newPalette)) {
                return existingPalette;
            }
        }
        return null;
    }

    private ECMPalette getPaletteForGrid(Color[][] colorGrid, int nColors) {
        ArrayList<Map.Entry<Color, Integer>> colorCounts = countColors(colorGrid);
        ECMPalette palette = new ECMPalette(nColors);
        for (int i = 1; i < Math.min(nColors, colorCounts.size()); i++) {
            palette.setColor(i, colorCounts.get(i).getKey());
        }
        return palette;
    }

    private ArrayList<Map.Entry<Color, Integer>> countColors(Color[][] colorGrid) {
        HashMap<Color, Integer> colorCounts = new HashMap<Color, Integer>();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Color color = colorGrid[y][x];
                if (color != null) {
                    Integer count = colorCounts.get(color);
                    colorCounts.put(color, count == null ? 1 : count + 1);
                }
            }
        }
        ArrayList<Map.Entry<Color, Integer>> colorList = new ArrayList<>(colorCounts.entrySet());
        colorList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return colorList;
    }

    private int[][] getGridForPalette(Color[][] colorGrid, ECMPalette palette) {
        int[][] grid = new int[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Color color = colorGrid[y][x];
                grid[y][x] = color != null ? palette.getColorIndex(color) : 0;
            }
        }
        return grid;
    }
}
