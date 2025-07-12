package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.CVBasicDataFileExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ExportCVBasicDataFileAction extends FileAction {

    public ExportCVBasicDataFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        MagellanExportDialog exportDialog = new MagellanExportDialog(MagellanExportDialog.TYPE_ASM, parent, parent, preferences);
        if (exportDialog.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, CVBEXTS, "CVBasic Source Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (String extension : CVBEXTS) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + extension)) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + CVBEXT);
                }
                int sChar = exportDialog.getStartChar();
                int eChar = exportDialog.getEndChar();
                int sSprite = exportDialog.getStartSprite();
                int eSprite = exportDialog.getEndSprite();
                preferences.setExportComments(exportDialog.includeComments());
                preferences.setIncludeCharNumbers(exportDialog.includeCharNumbers());
                preferences.setCurrentMapOnly(exportDialog.currentMapOnly());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                preferences.setIncludeCharData(exportDialog.includeCharData());
                preferences.setIncludeSpriteData(exportDialog.includeSpriteData());
                preferences.setIncludeColorData(exportDialog.includeColorData());
                preferences.setIncludeMapData(exportDialog.includeMapData());
                preferences.setDefStartSprite(Math.min(sSprite, eSprite));
                preferences.setDefEndSprite(Math.max(sSprite, eSprite));
                preferences.setCompression(exportDialog.getCompression());
                CVBasicDataFileExporter exporter = new CVBasicDataFileExporter(mapEditor, dataSet, preferences);
                try {
                    exporter.writeCVBasicDataFile(file, preferences);
                } catch (Exception e) {
                    showError("Export failed", e.getMessage());
                }
            }
        }
        exportDialog.dispose();
    }
}
