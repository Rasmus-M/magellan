package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.DataFileExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class SaveDataFileAction extends SaveDataFileAsAction {

    public SaveDataFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = parent.getMapDataFile();
        if (file != null && file.isFile()) {
            DataFileExporter magIO = new DataFileExporter(mapEditor, dataSet, preferences);
            try {
                magIO.writeDataFile(file, preferences.getCharacterSetCapacity());
            } catch (IOException ioException) {
                ioException.printStackTrace();
                showError("Error saving file", ioException.getMessage());
            }
            parent.setModified(false);
            parent.updateComponents();
        }
        else {
            super.actionPerformed(e);
        }
    }
}
