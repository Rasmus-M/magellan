package com.dreamcodex.ti.actions.metatile;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.EditorAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 * Copies the active metatile to the clipboard as hex character numbers, one line per row.
 */
public class CopyMetatileAction extends EditorAction {

    public CopyMetatileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
    }

    @Override
    protected void performAction(ActionEvent e) {
        int[][] metatile = dataSet.getMetatiles().get(parent.getActiveMetatile());
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < metatile.length; y++) {
            if (y > 0) {
                sb.append("\n");
            }
            for (int x = 0; x < metatile[y].length; x++) {
                if (x > 0) {
                    sb.append(" ");
                }
                String hex = Integer.toHexString(metatile[y][x]).toUpperCase();
                sb.append(hex.length() < 2 ? "0" + hex : hex);
            }
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }
}
