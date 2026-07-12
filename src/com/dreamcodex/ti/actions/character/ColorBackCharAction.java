package com.dreamcodex.ti.actions.character;

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

public class ColorBackCharAction extends EditorAction {

    private final int index;

    public ColorBackCharAction(int index, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.index = index;
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        int activeChar = parent.getActiveChar();
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
            // Mark the selected background color
            for (DualClickButton colorButton : parent.getUI().getCharColorDockButtons()) {
                if ("B".equals(colorButton.getText())) {
                    colorButton.setText("");
                }
            }
            parent.getUI().getCharColorDockButtons()[index].setText("B");
            if (colorMode != COLOR_MODE_BITMAP) {
                int cset = activeChar / 8;
                dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK] = index;
                for (int c = 0; c < Magellan.FONT_COLS; c++) {
                    parent.updateCharButton((cset * 8) + c, false);
                }
                parent.updateCharButton(activeChar);
            }
            charCanvas.setColorBack(index);
            charCanvas.redrawCanvas();
        }
        else {
            if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                charCanvas.setColorBack(index);
            }
            else {
                // Swap two colors of the palette and of the character grids
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[parent.getUI().getCharECMPaletteComboBox().getSelectedIndex()];
                int index2 = parent.getUI().getCharECMPaletteComboBox().getIndexFore();
                for (int i = 0; i < dataSet.getEcmCharPalettes().length; i++) {
                    if (dataSet.getEcmCharPalettes()[i] == ecmPalette) {
                        Globals.swapGridValues(dataSet.getCharGrids().get(i), index, index2);
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
