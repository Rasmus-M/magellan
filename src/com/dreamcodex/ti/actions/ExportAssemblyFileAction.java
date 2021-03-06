package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.AssemblyDataFileExporter;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ExportAssemblyFileAction extends MagellanAction {

    public ExportAssemblyFileAction(JFrame parent, IconProvider iconProvider, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super("Assembly Data", parent, iconProvider, mapEditor, dataSet, preferences);

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_ASM, parent, iconProvider, preferences);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, ASMEXTS, "Assembler Source Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (String extension : ASMEXTS) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + extension)) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + ASMEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                int sSprite = exporter.getStartSprite();
                int eSprite = exporter.getEndSprite();
                preferences.setExportComments(exporter.includeComments());
                preferences.setIncludeCharNumbers(exporter.includeCharNumbers());
                preferences.setCurrentMapOnly(exporter.currentMapOnly());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                preferences.setIncludeSpriteData(exporter.includeSpritedata());
                preferences.setDefStartSprite(Math.min(sSprite, eSprite));
                preferences.setDefEndSprite(Math.max(sSprite, eSprite));
                preferences.setCompression(exporter.getCompression());
                AssemblyDataFileExporter magIO = new AssemblyDataFileExporter(mapEditor, dataSet, preferences.getColorMode());
                try {
                    magIO.writeAssemblyDataFile(file, preferences.getDefStartChar(), preferences.getDefEndChar(), preferences.getDefStartSprite(), preferences.getDefEndSprite(), preferences.getCompression(), preferences.isExportComments(), preferences.isCurrentMapOnly(), preferences.isIncludeCharNumbers(), preferences.isIncludeSpriteData());
                } catch (Exception e) {
                    showError("Export failed", e.getMessage());
                }
            }
        }
        exporter.dispose();
    }
}
