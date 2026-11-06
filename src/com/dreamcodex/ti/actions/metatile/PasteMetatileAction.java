package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;

/**
 * Sets the active metatile from the clipboard: hex character numbers in row order,
 * separated by spaces, commas or line breaks, as written by CopyMetatileAction.
 */
public class PasteMetatileAction extends EditorAction {

    public PasteMetatileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
    }

    @Override
    protected void performAction(ActionEvent e) throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String text = clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) ? clipboard.getData(DataFlavor.stringFlavor).toString().trim() : "";
        int metatileNum = parent.getActiveMetatile();
        int[][] metatile = dataSet.getMetatiles().get(metatileNum);
        int size = metatile.length;
        String[] tokens = text.isEmpty() ? new String[0] : text.split("[\\s,]+");
        if (tokens.length != size * size) {
            showError("Paste Metatile", "The clipboard doesn't hold a " + size + "x" + size + " metatile (" + size * size + " hex character numbers).");
            return;
        }
        int[] chars = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].startsWith(">") ? tokens[i].substring(1) : tokens[i];
            try {
                chars[i] = Integer.parseInt(token, 16);
            } catch (NumberFormatException ex) {
                chars[i] = -1;
            }
            if (chars[i] < TIGlobals.MIN_CHAR || chars[i] > TIGlobals.MAX_CHAR) {
                showError("Paste Metatile", "'" + tokens[i] + "' is not a valid character number.");
                return;
            }
        }
        // Changes the metatile in place, so the editor and the project data stay the same array
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                metatile[y][x] = chars[y * size + x];
            }
        }
        parent.updateMetatile(metatileNum);
    }
}
