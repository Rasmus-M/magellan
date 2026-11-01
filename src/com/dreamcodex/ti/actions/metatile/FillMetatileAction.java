package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.util.Arrays;

/**
 * Sets every cell of the active metatile to the active character.
 */
public class FillMetatileAction extends EditorAction {

    public FillMetatileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        int metatileNum = parent.getActiveMetatile();
        for (int[] row : dataSet.getMetatiles().get(metatileNum)) {
            Arrays.fill(row, parent.getActiveChar());
        }
        parent.updateMetatile(metatileNum);
    }
}
