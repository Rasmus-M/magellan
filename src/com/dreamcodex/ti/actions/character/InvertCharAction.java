package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class InvertCharAction extends EditorAction {

    public InvertCharAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0 || colorMode != COLOR_MODE_BITMAP) {
            charCanvas.setGrid(Globals.invertGrid(charCanvas.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
        }
        else {
            charCanvas.setGridAndColors(Globals.invertGrid(charCanvas.getGridData(), 1), Globals.flipGrid(charCanvas.getGridColors(), false));
            dataSet.getCharColors().put(parent.getActiveChar(), charCanvas.getGridColors());
        }
        dataSet.getCharGrids().put(parent.getActiveChar(), charCanvas.getGridData());
        parent.updateCharButton(parent.getActiveChar());
        parent.updateComponents();
    }
}
