package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

/**
 * Shifts the characters of the active metatile down, wrapping around.
 */
public class ShiftDownMetatileAction extends EditorAction {

    public ShiftDownMetatileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        int metatileNum = parent.getActiveMetatile();
        // Shifts the metatile in place, so the editor and the project data stay the same array
        Globals.cycleGridDown(dataSet.getMetatiles().get(metatileNum));
        parent.updateMetatile(metatileNum);
    }
}
