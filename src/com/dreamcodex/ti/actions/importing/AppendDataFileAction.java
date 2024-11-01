package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.AppendDataFileImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class AppendDataFileAction extends FileAction {

    public AppendDataFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, FILEEXTS, "Map Data Files");
        if (file != null) {
            AppendDataFileImporter importer = new AppendDataFileImporter(mapEditor, dataSet, preferences);
            try {
                importer.readAppendDataFile(file);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                showError("Error appending data file", e.getMessage());
            }
            parent.updateAll();
            parent.setModified(true);
        }
    }
}
