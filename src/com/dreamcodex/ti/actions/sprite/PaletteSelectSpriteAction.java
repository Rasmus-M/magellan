package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class PaletteSelectSpriteAction extends EditorAction {

    public PaletteSelectSpriteAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        int activeSprite = parent.getActiveSprite();
        int selectedIndex = parent.getUI().getSpriteECMPaletteComboBox().getSelectedIndex();
        if (selectedIndex != -1) {
            ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
            dataSet.getEcmSpritePalettes()[activeSprite] = ecmPalette;
            spriteCanvas.setPalette(ecmPalette.getColors());
        }
        spriteCanvas.redrawCanvas();
        parent.updateSpriteButton(activeSprite);
    }
}
