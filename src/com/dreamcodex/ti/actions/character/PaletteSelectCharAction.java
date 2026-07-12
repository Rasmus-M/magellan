package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class PaletteSelectCharAction extends EditorAction {

    public PaletteSelectCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        int activeChar = parent.getActiveChar();
        int selectedIndex = parent.getUI().getCharECMPaletteComboBox().getSelectedIndex();
        if (selectedIndex != -1) {
            ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
            dataSet.getEcmCharPalettes()[activeChar] = ecmPalette;
            charCanvas.setPalette(ecmPalette.getColors());
        }
        charCanvas.redrawCanvas();
        parent.updateCharButton(activeChar);
    }
}
