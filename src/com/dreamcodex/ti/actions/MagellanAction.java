package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.*;

public abstract class MagellanAction extends AbstractAction {

    protected final Magellan parent;
    protected final MapEditor mapEditor;
    protected final DataSet dataSet;
    protected final Preferences preferences;

    public MagellanAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name);
        this.parent = parent;
        this.mapEditor = mapEditor;
        this.dataSet = dataSet;
        this.preferences = preferences;
    }


    protected void showError(String title, String message) {
        parent.showError(title, message);
    }

    protected int showConfirmation(String title, String message, boolean includeCancel) {
        return parent.showConfirmation(title, message, includeCancel);
    }

    protected Image getImage(String name) {
        return Toolkit.getDefaultToolkit().getImage(name);
    }
}
