package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.CharacterImageExporter;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportCharImageAction extends MagellanAction {

    private final boolean color;

    public ExportCharImageAction(boolean color, String name, JFrame parent, IconProvider iconProvider, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, iconProvider, mapEditor, dataSet, preferences);
        this.color = color;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, IMGEXTS, "Image Files");
        if (file != null) {
            boolean isExtensionAdded = false;
            for (int ex = 0; ex < IMGEXTS.length; ex++) {
                if (file.getAbsolutePath().toLowerCase().endsWith("." + IMGEXTS[ex])) {
                    isExtensionAdded = true;
                }
            }
            if (!isExtensionAdded) {
                file = new File(file.getAbsolutePath() + "." + IMGEXT);
            }
            CharacterImageExporter magIO = new CharacterImageExporter(mapEditor, dataSet.getEcmPalettes(), dataSet.getClrSets(), dataSet.getCharGrids(), dataSet.getCharColors(), dataSet.getEcmCharPalettes(), dataSet.getEcmCharTransparency(), dataSet.getSpriteGrids(), dataSet.getSpriteColors(), dataSet.getEcmSpritePalettes(), preferences.getColorMode());
            try {
                magIO.writeCharImage(file, 8, color);
            } catch (IOException ee) {
                showError("Export failed", ee.getMessage());
            }
        }

    }
}
