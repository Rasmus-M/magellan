package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;
import static com.dreamcodex.ti.util.Globals.isGridEmpty;

public class SpriteImageExporter extends Exporter {

    public SpriteImageExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences){
        super(mapEditor, dataSet, preferences);
    }

    public void writeSpriteImage(File imageOut, int spriteCols) throws IOException {
        int spriteCount = spriteGrids.size();
        while (spriteCount > 64 && (spriteGrids.get(spriteCount - 1) == null || isGridEmpty(spriteGrids.get(spriteCount - 1)))) {
            spriteCount--;
        }
        int spriteRows = (int) Math.ceil((double) spriteCount / spriteCols);
        BufferedImage bufferCharImage = new BufferedImage(spriteCols * 16, spriteRows * 16, BufferedImage.TYPE_INT_ARGB);
        int drawRow = 0;
        int drawCol = 0;
        Graphics2D gf = (Graphics2D) (bufferCharImage.getGraphics());
        gf.setComposite(AlphaComposite.Clear);
        gf.setColor(TIGlobals.TI_COLOR_TRANSPARENT);
        gf.fillRect(0, 0, bufferCharImage.getWidth(), bufferCharImage.getHeight());
        gf.setColor(TIGlobals.TI_COLOR_BLACK);
        for (int i = 0; i < spriteCount; i++) {
            gf.setComposite(AlphaComposite.SrcOver);
            if (spriteGrids.containsKey(i)) {
                int[][] spriteGrid = spriteGrids.get(i);
                for (int y = 0; y < spriteGrid.length; y++) {
                    for (int x = 0; x < spriteGrid[y].length; x++) {
                        if (spriteGrid[y][x] != 0) {
                            if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                                gf.setColor(TIGlobals.TI_PALETTE[spriteColors[i]]);
                            }
                            else {
                                gf.setColor(ecmSpritePalettes[i].getColor(spriteGrid[y][x]));
                            }
                            gf.fillRect((drawCol * 16) + x, (drawRow * 16) + y, 1, 1);
                        }
                    }
                }
            }
            drawCol++;
            if (drawCol >= spriteCols) {
                drawCol = 0;
                drawRow++;
            }
        }
        gf.dispose();
        String formatName = imageOut.getName().toLowerCase().endsWith("gif") ? "gif" : "png";
        ImageIO.write(bufferCharImage, formatName, imageOut);
    }
}
