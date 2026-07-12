package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;

public class ShiftUpCharAction extends EditorAction {

    public ShiftUpCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        charCanvas.setGrid(Globals.cycleGridUp(charCanvas.getGridData()));
        dataSet.getCharGrids().put(parent.getActiveChar(), charCanvas.getGridData());
        if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
            charCanvas.setColors(Globals.cycleGridUp(charCanvas.getGridColors()));
            dataSet.getCharColors().put(parent.getActiveChar(), charCanvas.getGridColors());
        }
        parent.updateCharButton(parent.getActiveChar());
        parent.updateComponents();
    }
}
