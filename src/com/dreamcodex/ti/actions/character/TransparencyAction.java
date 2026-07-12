package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class TransparencyAction extends EditorAction {

    public TransparencyAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        int activeChar = parent.getActiveChar();
        dataSet.getEcmCharTransparency()[activeChar] = parent.getUI().getTransparencyCheckBox().isSelected();
        charCanvas.setECMTransparency(dataSet.getEcmCharTransparency()[activeChar]);
        charCanvas.redrawCanvas();
        parent.updateCharButton(activeChar, true);
    }
}
