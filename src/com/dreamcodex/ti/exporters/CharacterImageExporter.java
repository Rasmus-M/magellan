package com.dreamcodex.ti.exporters;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;
import static com.dreamcodex.ti.util.Globals.isGridEmpty;

public class CharacterImageExporter extends Exporter {

    public CharacterImageExporter(MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(mapEditor, dataSet, preferences);
    }

    public void writeCharImage(File imageOut, int tileCols, boolean isColor) throws IOException {
        int charCount = charGrids.size();
        while (charCount > 256 && (charGrids.get(charCount - 1) == null || isGridEmpty(charGrids.get(charCount - 1)))) {
            charCount--;
        }
        int tileRows = (int) Math.ceil((double) charCount / tileCols);
        BufferedImage bufferCharImage = new BufferedImage(tileCols * 8, tileRows * 8, BufferedImage.TYPE_INT_ARGB);
        int[][] charGrid;
        int[][] charColors;
        int drawRow = 0;
        int drawCol = 0;
        int setNum = 0;
        boolean shouldDraw;
        Graphics2D gf = (Graphics2D) (bufferCharImage.getGraphics());
        if (isColor) {
            gf.setComposite(AlphaComposite.Clear);
            gf.setColor(TIGlobals.TI_COLOR_TRANSPARENT);
        }
        else {
            gf.setComposite(AlphaComposite.SrcOver);
            gf.setColor(TIGlobals.TI_COLOR_WHITE);
        }
        gf.fillRect(0, 0, bufferCharImage.getWidth(), bufferCharImage.getHeight());
        gf.setColor(TIGlobals.TI_COLOR_BLACK);
        for (int ch = 0; ch < charCount; ch++) {
            gf.setComposite(AlphaComposite.SrcOver);
            if (charGrids.containsKey(ch)) {
                charGrid = charGrids.get(ch);
                charColors = colorMode == COLOR_MODE_BITMAP ? this.charColors.get(ch) : null;
                for (int y = 0; y < charGrid.length; y++) {
                    for (int x = 0; x < charGrid[y].length; x++) {
                        shouldDraw = true;
                        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                            if (charGrid[y][x] == 1) {
                                if (isColor) {
                                    if (charColors == null) {
                                        if (clrSets[setNum][Globals.INDEX_CLR_FORE] == 0) {
                                            shouldDraw = false;
                                        }
                                        gf.setColor(TIGlobals.TI_PALETTE[clrSets[setNum][Globals.INDEX_CLR_FORE]]);
                                    }
                                    else {
                                        shouldDraw = charColors[y][1] != 0;
                                        gf.setColor(TIGlobals.TI_PALETTE[charColors[y][1]]);
                                    }
                                }
                                else {
                                    gf.setColor(TIGlobals.TI_COLOR_BLACK);
                                }
                            }
                            else {
                                if (isColor) {
                                    if (charColors == null) {
                                        if (clrSets[setNum][Globals.INDEX_CLR_BACK] == 0) {
                                            shouldDraw = false;
                                        }
                                        gf.setColor(TIGlobals.TI_PALETTE[clrSets[setNum][Globals.INDEX_CLR_BACK]]);
                                    }
                                    else {
                                        shouldDraw = charColors[y][0] != 0;
                                        gf.setColor(TIGlobals.TI_PALETTE[charColors[y][0]]);
                                    }
                                }
                                else {
                                    gf.setColor(TIGlobals.TI_COLOR_WHITE);
                                }
                            }
                        }
                        else {
                            if (isColor) {
                                gf.setColor(ecmCharPalettes[ch].getColor(charGrid[y][x]));
                            }
                            else {
                                gf.setColor(charGrid[y][x] != 0 ? TIGlobals.TI_COLOR_BLACK : TIGlobals.TI_COLOR_WHITE);
                            }
                        }
                        if (shouldDraw) {
                            gf.fillRect((drawCol * 8) + x, (drawRow * 8) + y, 1, 1);
                        }
                    }
                }
            }
            drawCol++;
            if (drawCol >= tileCols) {
                drawCol = 0;
                drawRow++;
                setNum++;
            }
        }
        gf.dispose();
        String formatName = imageOut.getName().toLowerCase().endsWith("gif") ? "gif" : "png";
        ImageIO.write(bufferCharImage, formatName, imageOut);
    }
}
