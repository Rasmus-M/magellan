package com.dreamcodex.ti.actions.map;

import com.dreamcodex.ti.component.MapCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;

public class ShiftMapRightAction extends MapAction {

    public ShiftMapRightAction(Icon icon, MapEditor mapEditor, UndoManager undoManager) {
        super(null, icon, mapEditor, undoManager);
    }

    public String getName() {
        return "Shift map right with wrap";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction();
        undoManager.addEdit(new MapCanvas.ActionEdit(this, new ShiftMapLeftAction(null, mapEditor, undoManager)));
    }

    @Override
    public void performAction() {
        int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
        Globals.cycleGridRight(mapData);
        mapEditor.redrawModifiedCanvas();
    }
}
