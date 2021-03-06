package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.BinaryFileExporter;
import com.dreamcodex.ti.exporters.Exporter;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportBinaryFileAction extends MagellanAction {

    public ExportBinaryFileAction(String name, JFrame parent, IconProvider iconProvider, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, iconProvider, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_BINARY, parent, iconProvider, preferences.isExportComments(), preferences.getDefStartChar(), preferences.getDefEndChar(), TIGlobals.MIN_CHAR, preferences.getCharacterSetEnd(), preferences.getSpriteSetEnd(), preferences.isCurrentMapOnly(), preferences.isExcludeBlank());
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
                boolean bIncludeColorsets = exporter.includeColorsets();
                boolean bIncludeChardata = exporter.includeChardata();
                boolean bIncludeSpritedata = exporter.includeSpritedata();
                byte chunkByte = (byte) (0 | (bIncludeColorsets ? Exporter.BIN_CHUNK_COLORS : 0) | (bIncludeChardata ? Exporter.BIN_CHUNK_CHARS : 0) | (bIncludeSpritedata ? Exporter.BIN_CHUNK_SPRITES : 0));
                preferences.setCurrentMapOnly(exporter.currentMapOnly());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                BinaryFileExporter magIO = new BinaryFileExporter(mapEditor, dataSet.getEcmPalettes(), dataSet.getClrSets(), dataSet.getCharGrids(), dataSet.getCharColors(), dataSet.getEcmCharPalettes(), dataSet.getEcmCharTransparency(), dataSet.getSpriteGrids(), dataSet.getSpriteColors(), dataSet.getEcmSpritePalettes(), preferences.getColorMode());
                try {
                    magIO.writeBinaryFile(file, chunkByte, preferences.getDefStartChar(), preferences.getDefEndChar(), preferences.isCurrentMapOnly());
                } catch (IOException ioException) {
                    showError("Export failed", ioException.getMessage());
                }
            }
        }
        exporter.dispose();
    }
}
