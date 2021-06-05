package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanImportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.SpriteImageImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ImportSpriteImageAction extends FileAction {

    public ImportSpriteImageAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            MagellanImportDialog importDialog = new MagellanImportDialog(MagellanImportDialog.TYPE_SPRITE_IMAGE, parent, parent, preferences, dataSet);
            if (importDialog.isOkay()) {
                try {
                    SpriteImageImporter importer = new SpriteImageImporter(mapEditor, dataSet, preferences);
                    importer.readSpriteFile(file, importDialog.getStartSprite(), importDialog.getStartPalette(), importDialog.getEndPalette(), importDialog.getGap(), importDialog.useExistingPalettes());
                } catch (Exception ee) {
                    ee.printStackTrace(System.err);
                    showError("Error importing file", ee.getMessage());
                }
                importDialog.dispose();
            }
            parent.setModified(true);
        }
        parent.updateAll();
        parent.editDefault();
    }
}
