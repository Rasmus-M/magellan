package com.dreamcodex.ti.actions.map;

import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;

public class ShiftMapDownAction extends MapAction {

    public ShiftMapDownAction(Icon icon, MapEditor mapEditor, UndoManager undoManager) {
        super(null, icon, mapEditor, undoManager);
    }

    public String getName() {
        return "Shift map down with wrap";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction();
        undoManager.addEdit(new MapCanvas.ActionEdit(this, new ShiftMapUpAction(null, mapEditor, undoManager)));
    }

    @Override
    public void performAction() {
        int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
        Globals.cycleGridDown(mapData);
        mapEditor.redrawModifiedCanvas();
    }
}
