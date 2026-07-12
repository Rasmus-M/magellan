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

public class FlipVerticalCharAction extends EditorAction {

    public FlipVerticalCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        boolean isBitmap = dataSet.getColorMode() == COLOR_MODE_BITMAP;
        charCanvas.setGridAndColors(Globals.flipGrid(charCanvas.getGridData(), true), isBitmap ? Globals.flipGrid(charCanvas.getGridColors(), true) : null);
        dataSet.getCharGrids().put(parent.getActiveChar(), charCanvas.getGridData());
        if (isBitmap) {
            dataSet.getCharColors().put(parent.getActiveChar(), charCanvas.getGridColors());
        }
        parent.updateCharButton(parent.getActiveChar());
        parent.updateComponents();
    }
}
