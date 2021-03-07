package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.MagellanAction;
import com.dreamcodex.ti.component.MagellanImportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.MapImageFileImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ImportMapImageAction extends MagellanAction {

    public ImportMapImageAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            MagellanImportDialog importer = new MagellanImportDialog(MagellanImportDialog.TYPE_MAP_IMAGE, parent, parent, preferences.getColorMode(), preferences.getCharacterSetStart(), preferences.getCharacterSetEnd(), preferences.getSpriteSetEnd(), dataSet.getEcmPalettes());
            if (importer.isOkay()) {
                try {
                    MapImageFileImporter magIO = new MapImageFileImporter(mapEditor, dataSet, preferences);
                    magIO.readMapImageFile(file, importer.getStartChar(), importer.getEndChar(), importer.getStartPalette(), importer.getTolerance());
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    showError("Error importing file", ex.getMessage());
                }
                importer.dispose();
            }
            parent.setModified(true);
        }
        parent.updateCharButtons();
        parent.updateComponents();
        parent.editDefault();
    }
}
