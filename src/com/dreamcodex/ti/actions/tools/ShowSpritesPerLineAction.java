package com.dreamcodex.ti.actions.tools;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.MagellanAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

public class ShowSpritesPerLineAction extends MagellanAction {

    public ShowSpritesPerLineAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mapEditor.setShowSpritesPerLine(!mapEditor.getShowSpritesPerLine());
    }
}
