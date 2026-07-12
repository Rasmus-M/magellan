package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class ShiftLeftSpriteAction extends EditorAction {

    public ShiftLeftSpriteAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        spriteCanvas.setGrid(Globals.cycleGridLeft(spriteCanvas.getGridData()));
        dataSet.getSpriteGrids().put(parent.getActiveSprite(), spriteCanvas.getGridData());
        parent.updateSpriteButton(parent.getActiveSprite());
        parent.updateComponents();
    }
}
