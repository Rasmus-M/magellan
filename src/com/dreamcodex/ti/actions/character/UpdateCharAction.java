package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_3;

public class UpdateCharAction extends EditorAction {

    public UpdateCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        int activeChar = parent.getActiveChar();
        String hexString = "";
        JTextField charTextField = parent.getUI().getCharTextField();
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                hexString = Globals.padHexString(charTextField.getText(), 16);
                break;
            case COLOR_MODE_BITMAP:
                hexString = Globals.padHexString(charTextField.getText(), charTextField.getText().length() <= 16 ? 16 : 32);
                break;
            case COLOR_MODE_ECM_2:
                hexString = Globals.padHexString(charTextField.getText(), 32 + 4);
                break;
            case COLOR_MODE_ECM_3:
                hexString = Globals.padHexString(charTextField.getText(), 48 + 4);
                break;
        }
        charTextField.setText(hexString);
        charTextField.setCaretPosition(0);
        // Plane 0
        int[][] charGrid = Globals.getIntGrid(hexString.substring(0, 16), 8);
        // Bitmap colors
        int[][] charColors = dataSet.getCharColors() != null ? dataSet.getCharColors().get(activeChar) : null;
        if (colorMode == COLOR_MODE_BITMAP && hexString.length() == 32) {
            charColors = Globals.parseColorHexString(hexString.substring(16));
            dataSet.getCharColors().put(activeChar, charColors);
        }
        // Plane 1
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            Globals.orGrid(Globals.getIntGrid(hexString.substring(16, 32), 8), charGrid, 1);
        }
        // Plane 2
        if (colorMode == COLOR_MODE_ECM_3) {
            Globals.orGrid(Globals.getIntGrid(hexString.substring(32, 48), 8), charGrid, 2);
        }
        // Palette
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            int palette = Integer.parseInt(colorMode == COLOR_MODE_ECM_2 ? hexString.substring(32, 36) : hexString.substring(48, 52), 16);
            dataSet.getEcmCharPalettes()[activeChar] = dataSet.getEcmPalettes()[palette];
            parent.getUI().getCharECMPaletteComboBox().setSelectedItem(dataSet.getEcmCharPalettes()[activeChar]);
        }
        dataSet.getCharGrids().put(activeChar, charGrid);
        charCanvas.setGridAndColors(dataSet.getCharGrids().get(activeChar), colorMode == COLOR_MODE_BITMAP ? charColors : null);
        parent.updateCharButton(activeChar);
    }
}
