package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.SpriteImageExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportSpriteImageAction extends FileAction {

    private final boolean color;

    public ExportSpriteImageAction(boolean color, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.color = color;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, IMGEXTS, "Image Files");
        if (file != null) {
            boolean isExtensionAdded = false;
            for (String imgext : IMGEXTS) {
                if (file.getAbsolutePath().toLowerCase().endsWith("." + imgext)) {
                    isExtensionAdded = true;
                }
            }
            if (!isExtensionAdded) {
                file = new File(file.getAbsolutePath() + "." + IMGEXT);
            }
            SpriteImageExporter exporter = new SpriteImageExporter(mapEditor, dataSet, preferences);
            try {
                exporter.writeSpriteImage(file, 4);
            } catch (IOException ee) {
                showError("Export failed", ee.getMessage());
            }
        }
    }
}
