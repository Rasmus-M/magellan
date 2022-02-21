package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.BinaryMapImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ImportBinaryMapAction extends FileAction {

    public ImportBinaryMapAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, BINEXTS, "Binary Files", false);
        if (file != null) {
            try {
                BinaryMapImporter importer = new BinaryMapImporter(mapEditor, dataSet, preferences);
                importer.readBinaryMap(file);
                parent.updateComponents();
                parent.setModified(true);
            } catch (IOException ioException) {
                showError("Error importing file", ioException.getMessage());
            }
        }
    }
}
