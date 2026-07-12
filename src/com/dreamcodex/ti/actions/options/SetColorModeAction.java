package com.dreamcodex.ti.actions.options;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class SetColorModeAction extends EditorAction {

    private final ColorMode colorMode;

    public SetColorModeAction(ColorMode colorMode, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.colorMode = colorMode;
    }

    @Override
    protected void performAction(ActionEvent e) {
        parent.setColorModeOption(colorMode);
    }
}
