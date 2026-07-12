package com.dreamcodex.ti.actions.sprite;

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
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class UpdateSpriteAction extends EditorAction {

    public UpdateSpriteAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        int activeSprite = parent.getActiveSprite();
        String hexString = "";
        JTextField spriteTextField = parent.getUI().getSpriteTextField();
        switch (colorMode) {
            case COLOR_MODE_BITMAP:
            case COLOR_MODE_GRAPHICS_1:
                hexString = Globals.padHexString(spriteTextField.getText(), 64);
                break;
            case COLOR_MODE_ECM_2:
                hexString = Globals.padHexString(spriteTextField.getText(), 128 + 4);
                break;
            case COLOR_MODE_ECM_3:
                hexString = Globals.padHexString(spriteTextField.getText(), 192 + 4);
                break;
        }
        spriteTextField.setText(hexString);
        spriteTextField.setCaretPosition(0);
        // Plane 0
        int[][] spriteGrid = Globals.getSpriteIntGrid(hexString.substring(0, 64));
        // Plane 1
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            Globals.orGrid(Globals.getSpriteIntGrid(hexString.substring(64, 128)), spriteGrid, 1);
        }
        // Plane 2
        if (colorMode == COLOR_MODE_ECM_3) {
            Globals.orGrid(Globals.getSpriteIntGrid(hexString.substring(128, 192)), spriteGrid, 2);
        }
        // Palette
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            int palette = Integer.parseInt(colorMode == COLOR_MODE_ECM_2 ? hexString.substring(128, 132) : hexString.substring(192, 196), 16);
            dataSet.getEcmSpritePalettes()[activeSprite] = dataSet.getEcmPalettes()[palette];
            parent.getUI().getSpriteECMPaletteComboBox().setSelectedItem(dataSet.getEcmSpritePalettes()[activeSprite]);
        }
        dataSet.getSpriteGrids().put(activeSprite, spriteGrid);
        spriteCanvas.setGrid(dataSet.getSpriteGrids().get(activeSprite));
        parent.updateSpriteButton(activeSprite);
    }
}
