package com.dreamcodex.ti.actions.map;

import com.dreamcodex.ti.component.MapEditor;

import javax.swing.*;
import javax.swing.undo.UndoManager;

public abstract class MapAction extends AbstractAction {

    protected final MapEditor mapEditor;
    protected final UndoManager undoManager;

    public MapAction(String name, Icon icon, MapEditor mapEditor, UndoManager undoManager) {
        super(name, icon);
        this.mapEditor = mapEditor;
        this.undoManager = undoManager;
    }

    public abstract  String getName();

    public abstract void performAction();
}
