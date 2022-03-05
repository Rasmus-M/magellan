package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.BinaryFileExporter;
import com.dreamcodex.ti.exporters.Exporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportBinaryFileAction extends FileAction {

    public ExportBinaryFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(
            MagellanExportDialog.TYPE_BINARY,
            parent,
            parent,
            preferences.isExportComments(),
            preferences.getDefStartChar(),
            preferences.getDefEndChar(),
            TIGlobals.MIN_CHAR,
            preferences.getCharacterSetEnd(),
            preferences.getSpriteSetEnd(),
            preferences.isCurrentMapOnly(),
            preferences.isExcludeBlank()
        );
        if (exporter.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, BINEXTS, "Binary Data Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < BINEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + BINEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + BINEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                int sSprite = exporter.getStartSprite();
                int eSprite = exporter.getEndSprite();
                boolean bIncludeColorSets = exporter.includeColorData();
                boolean bIncludeCharData = exporter.includeCharData();
                boolean includeSpriteData = exporter.includeSpriteData();
                byte chunkByte = (byte) ((bIncludeColorSets ? Exporter.BIN_CHUNK_COLORS : 0) | (bIncludeCharData ? Exporter.BIN_CHUNK_CHARS : 0) | (includeSpriteData ? Exporter.BIN_CHUNK_SPRITES : 0));
                preferences.setCurrentMapOnly(exporter.currentMapOnly());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                preferences.setDefStartSprite(Math.min(sSprite, eSprite));
                preferences.setDefEndSprite(Math.max(sSprite, eSprite));
                BinaryFileExporter magIO = new BinaryFileExporter(mapEditor, dataSet, preferences);
                try {
                    magIO.writeBinaryFile(file, chunkByte, preferences.getDefStartChar(), preferences.getDefEndChar(), preferences.getDefStartSprite(), preferences.getDefEndSprite(), preferences.isCurrentMapOnly());
                } catch (IOException ioException) {
                    showError("Export failed", ioException.getMessage());
                }
            }
        }
        exporter.dispose();
    }
}
