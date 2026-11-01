package com.dreamcodex.ti.actions.options;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class SetMetatileModeAction extends EditorAction {

    // 0 = no metatiles, otherwise the metatile width/height in characters
    private final int size;

    public SetMetatileModeAction(int size, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.size = size;
    }

    @Override
    protected void performAction(ActionEvent e) {
        if (!parent.setMetatileMode(size)) {
            // Conversion failed: put the menu selection back
            parent.getUI().setMetatileModeOption(dataSet.getMetatileSize());
        }
    }
}
