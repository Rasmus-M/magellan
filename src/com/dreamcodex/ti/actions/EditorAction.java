package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public abstract class EditorAction extends MagellanAction {

    public EditorAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        try {
            performAction(e);
            mapEditor.redrawCanvas();
        } catch (Exception ex) {
            showError("Program error", ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    protected abstract void performAction(ActionEvent e) throws Exception;
}
