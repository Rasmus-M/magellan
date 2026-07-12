package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.DualClickButton;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class ColorForeSpriteAction extends EditorAction {

    private final int index;

    public ColorForeSpriteAction(int index, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.index = index;
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        int activeSprite = parent.getActiveSprite();
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
            dataSet.getSpriteColors()[activeSprite] = index;
            // Mark the selected foreground color
            for (DualClickButton colorButton : parent.getUI().getSpriteColorDockButtons()) {
                if ("F".equals(colorButton.getText())) {
                    colorButton.setText("");
                }
            }
            parent.getUI().getSpriteColorDockButtons()[index].setText("F");
            parent.updateSpriteButton(activeSprite);
            spriteCanvas.setColorDraw(index);
            spriteCanvas.redrawCanvas();
        }
        else {
            if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                spriteCanvas.setColorDraw(index);
            }
            else {
                // Swap two colors of the palette and of the sprite grids
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[parent.getUI().getSpriteECMPaletteComboBox().getSelectedIndex()];
                int index2 = parent.getUI().getSpriteECMPaletteComboBox().getIndexBack();
                for (int i = 0; i < dataSet.getEcmSpritePalettes().length; i++) {
                    if (dataSet.getEcmSpritePalettes()[i] == ecmPalette) {
                        Globals.swapGridValues(dataSet.getSpriteGrids().get(i), index, index2);
                    }
                }
                Color color = ecmPalette.getColor(index);
                Color color2 = ecmPalette.getColor(index2);
                parent.setECMPaletteColor(ecmPalette, index2, color);
                parent.setECMPaletteColor(ecmPalette, index, color2);
            }
        }
    }
}
