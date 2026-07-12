package com.dreamcodex.ti.actions.character;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.DualClickButton;
import com.dreamcodex.ti.component.GridCanvas;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_3;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;

public class EditCharAction extends EditorAction {

    private final int charNum;

    public EditCharAction(int charNum, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.charNum = charNum;
    }

    @Override
    protected void performAction(ActionEvent e) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas charCanvas = parent.getUI().getCharGridCanvas();
        int oldActiveChar = parent.getActiveChar();
        parent.setActiveChar(charNum);
        HashMap<Integer, int[][]> charGrids = dataSet.getCharGrids();
        if ((e.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
            parent.swapCharacters(charNum, oldActiveChar, 0, true, true, true);
        }
        if (charGrids.get(charNum) == null) {
            charCanvas.clearGrid();
            charGrids.put(charNum, charCanvas.getGridData());
            if (colorMode == COLOR_MODE_BITMAP) {
                dataSet.getCharColors().put(charNum, charCanvas.getGridColors());
            }
        }
        charCanvas.resetUndoRedo();
        charCanvas.setGridAndColors(charGrids.get(charNum), colorMode == COLOR_MODE_BITMAP ? dataSet.getCharColors().get(charNum) : null);
        if (colorMode == COLOR_MODE_GRAPHICS_1) {
            int cset = charNum / 8;
            charCanvas.setColorBack(dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK]);
            charCanvas.setColorDraw(dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE]);
            DualClickButton[] charColorDockButtons = parent.getUI().getCharColorDockButtons();
            for (int i = 0; i < charColorDockButtons.length; i++) {
                charColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == charCanvas.getColorDraw() ? "F" : ""));
            }
        }
        else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            ECMPalette ecmPalette = dataSet.getEcmCharPalettes()[charNum];
            charCanvas.setPalette(ecmPalette.getColors());
            parent.getUI().getCharECMPaletteComboBox().setSelectedItem(ecmPalette);
        }
        charCanvas.setECMTransparency(dataSet.getEcmCharTransparency()[charNum]);
        charCanvas.redrawCanvas();
        mapEditor.setActiveChar(charNum);
        mapEditor.setCloneModeOn(false);
        parent.updateComponents();
    }
}
