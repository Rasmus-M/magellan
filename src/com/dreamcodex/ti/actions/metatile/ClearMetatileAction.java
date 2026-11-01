package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import java.awt.event.ActionEvent;
import java.util.Arrays;

/**
 * Sets every cell of the active metatile to a space.
 */
public class ClearMetatileAction extends EditorAction {

    public ClearMetatileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        int metatileNum = parent.getActiveMetatile();
        for (int[] row : dataSet.getMetatiles().get(metatileNum)) {
            Arrays.fill(row, TIGlobals.SPACECHAR);
        }
        parent.updateMetatile(metatileNum);
    }
}
