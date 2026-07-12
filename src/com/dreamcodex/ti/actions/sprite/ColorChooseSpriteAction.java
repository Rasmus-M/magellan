package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.ECMColorChooser;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.awt.Color;
import java.awt.event.ActionEvent;

public class ColorChooseSpriteAction extends EditorAction {

    private final int index;

    public ColorChooseSpriteAction(int index, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.index = index;
    }

    @Override
    protected void performAction(ActionEvent e) {
        ECMPalette ecmPalette = dataSet.getEcmPalettes()[parent.getUI().getSpriteECMPaletteComboBox().getSelectedIndex()];
        Color color = ECMColorChooser.showDialog(parent, "Select Color", ecmPalette.getColor(index));
        if (color != null) {
            parent.setECMPaletteColor(ecmPalette, index, color);
        }
    }
}
