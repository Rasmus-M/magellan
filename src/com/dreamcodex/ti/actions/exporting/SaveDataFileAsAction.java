package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.DataFileExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class SaveDataFileAsAction extends FileAction {

    public SaveDataFileAsAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, FILEEXTS, "Map Data Files");
        if (file != null) {
            if (!file.getAbsolutePath().toLowerCase().endsWith("." + FILEEXT)) {
                file = new File(file.getAbsolutePath() + "." + FILEEXT);
            }
            parent.setMapDataFile(file);
            parent.setModified(false);
            DataFileExporter magIO = new DataFileExporter(mapEditor, dataSet, preferences);
            try {
                magIO.writeDataFile(file, preferences.getCharacterSetCapacity());
            } catch (IOException ioException) {
                ioException.printStackTrace();
                showError("Error saving file", ioException.getMessage());
            }
            parent.updateComponents();
            preferences.addRecentFile(file.getAbsolutePath());
        }

    }
}
