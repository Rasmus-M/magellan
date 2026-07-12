package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class ShiftLeftCharAction extends EditorAction {

    public ShiftLeftCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        charCanvas.setGrid(Globals.cycleGridLeft(charCanvas.getGridData()));
        dataSet.getCharGrids().put(parent.getActiveChar(), charCanvas.getGridData());
        parent.updateCharButton(parent.getActiveChar());
        parent.updateComponents();
    }
}
