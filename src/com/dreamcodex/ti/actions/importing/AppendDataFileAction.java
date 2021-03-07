package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.MagellanAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.AppendDataFileImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class AppendDataFileAction extends MagellanAction {

    public AppendDataFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, FILEEXTS, "Map Data Files");
        if (file != null) {
            AppendDataFileImporter magIO = new AppendDataFileImporter(mapEditor, dataSet, preferences);
            try {
                magIO.readAppendDataFile(file);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                showError("Error appending data file", ioException.getMessage());
            }
            parent.updateComponents();
        }
    }
}
