package com.dreamcodex.ti.importers;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static com.dreamcodex.ti.Magellan.*;

public class SpriteImageImporter extends Importer {

    public SpriteImageImporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void readSpriteFile(File file, int spriteIndex, int startPalette, int gap) throws Exception {
        int size = 16 + gap;
        BufferedImage image = ImageIO.read(file);
        // if (image.getWidth() % 16 == 0 && image.getHeight() % 16 == 0) {
        ColorModel colorModel = image.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel indexColorModel = (IndexColorModel) colorModel;
            if (indexColorModel.getMapSize() <= 256) {
                Raster raster = image.getRaster();
                int xSprites = image.getWidth() / size;
                int ySprites = image.getHeight() / size;
                for (int sy = 0; sy < ySprites; sy++) {
                    int y0 = sy * size;
                    for (int sx = 0; sx < xSprites; sx++) {
                        int x0 = sx * size;
                        if (spriteIndex <= TIGlobals.MAX_SPRITE) {
                            if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                                Map<Integer, int[][]> colorLayers = new TreeMap<Integer, int[][]>();
                                int[] pixel = new int[1];
                                for (int y = 0; y < 16; y++) {
                                    for (int x = 0; x < 16; x++) {
                                        raster.getPixel(x + x0, y + y0, pixel);
                                        int colorIndex = Math.min(pixel[0], 15);
                                        if (colorIndex != 0) { // 0 is transparent
                                            int[][] colorLayer = colorLayers.get(colorIndex);
                                            if (colorLayer == null) {
                                                colorLayer = new int[16][16];
                                                colorLayers.put(colorIndex, colorLayer);
                                            }
                                            colorLayer[y][x] = 1;
                                        }
                                    }
                                }
                                for (int colorIndex : colorLayers.keySet()) {
                                    hmSpriteGrids.put(spriteIndex, colorLayers.get(colorIndex));
                                    spriteColors[spriteIndex] = colorIndex;
                                    spriteIndex++;
                                }
                            }
                            // F18A ECM sprites
                            else {
                                int colors = colorMode == COLOR_MODE_ECM_2 ? 4 : 8;
                                int[][] grid = hmSpriteGrids.get(spriteIndex);
                                if (grid == null) {
                                    grid = new int[16][16];
                                    hmSpriteGrids.put(spriteIndex, grid);
                                }
                                for (int y = 0; y < 16; y++) {
                                    for (int x = 0; x < 16; x++) {
                                        int[] pixel = new int[1];
                                        raster.getPixel(x + x0, y + y0, pixel);
                                        int colorIndex = pixel[0];
                                        grid[y][x] = colorIndex % colors;
                                    }
                                }
                                // Palette
                                ECMPalette palette = ecmPalettes[startPalette];
                                for (int i = 0; i < colors; i++) {
                                    palette.setSafeColor(i, indexColorModel.getRGB(i));
                                }
                                ecmSpritePalettes[spriteIndex] = palette;
                                spriteIndex++;
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
        // }
        // else {
        //     throw new Exception("Image size must be 16 x 16 pixels.");
        // }
    }
}
