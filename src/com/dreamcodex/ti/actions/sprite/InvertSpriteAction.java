package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class InvertSpriteAction extends EditorAction {

    public InvertSpriteAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        spriteCanvas.setGrid(Globals.invertGrid(spriteCanvas.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
        dataSet.getSpriteGrids().put(parent.getActiveSprite(), spriteCanvas.getGridData());
        parent.updateSpriteButton(parent.getActiveSprite());
        parent.updateComponents();
    }
}
