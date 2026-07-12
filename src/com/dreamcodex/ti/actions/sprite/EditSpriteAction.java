package com.dreamcodex.ti.actions.sprite;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.DualClickButton;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_3;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class EditSpriteAction extends EditorAction {

    private final int spriteNum;

    public EditSpriteAction(int spriteNum, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.spriteNum = spriteNum;
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas spriteCanvas = parent.getUI().getSpriteGridCanvas();
        int oldActiveSprite = parent.getActiveSprite();
        parent.setActiveSprite(spriteNum);
        HashMap<Integer, int[][]> spriteGrids = dataSet.getSpriteGrids();
        int[] spriteColors = dataSet.getSpriteColors();
        ECMPalette[] ecmSpritePalettes = dataSet.getEcmSpritePalettes();
        if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
            parent.swapSprites(spriteNum, oldActiveSprite);
        }
        if (spriteGrids.get(spriteNum) == null) {
            spriteCanvas.clearGrid();
            spriteGrids.put(spriteNum, spriteCanvas.getGridData());
        }
        spriteCanvas.resetUndoRedo();
        spriteCanvas.setGrid(spriteGrids.get(spriteNum));
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
            spriteCanvas.setColorDraw(spriteColors[spriteNum]);
            DualClickButton[] spriteColorDockButtons = parent.getUI().getSpriteColorDockButtons();
            for (int i = 0; i < spriteColorDockButtons.length; i++) {
                spriteColorDockButtons[i].setText(i == spriteCanvas.getColorBack() ? "B" : (i == spriteCanvas.getColorDraw() ? "F" : ""));
            }
        }
        else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            spriteCanvas.setPalette(ecmSpritePalettes[spriteNum].getColors());
            parent.getUI().getSpriteECMPaletteComboBox().setSelectedItem(ecmSpritePalettes[spriteNum]);
        }
        spriteCanvas.redrawCanvas();
        mapEditor.setActiveSprite(spriteNum);
        parent.updateComponents();
    }
}
