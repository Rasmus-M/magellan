package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class EditMetatileAction extends EditorAction {

    private final int metatileNum;

    public EditMetatileAction(int metatileNum, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.metatileNum = metatileNum;
    }

    @Override
    protected void performAction(ActionEvent e) {
        int oldActiveMetatile = parent.getActiveMetatile();
        parent.setActiveMetatile(metatileNum);
        if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
            parent.swapMetatiles(metatileNum, oldActiveMetatile);
        }
        parent.getUI().getMetatileCanvas().setMetatile(dataSet.getMetatiles().get(metatileNum));
        mapEditor.setPaintValue(metatileNum);
        mapEditor.setCloneModeOn(false);
        parent.updateComponents();
    }
}
