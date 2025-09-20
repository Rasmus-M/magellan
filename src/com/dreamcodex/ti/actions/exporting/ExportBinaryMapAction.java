package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.BinaryMapExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportBinaryMapAction extends FileAction {

    public ExportBinaryMapAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_BINARY_MAP, parent, parent, preferences);
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
                preferences.setOffset(exporter.getOffset());
                BinaryMapExporter magIO = new BinaryMapExporter(mapEditor, dataSet, preferences);
                try {
                    magIO.writeBinaryMap(file);
                } catch (IOException ioException) {
                    showError("Export failed", ioException.getMessage());
                }
            }
        }
    }
}
