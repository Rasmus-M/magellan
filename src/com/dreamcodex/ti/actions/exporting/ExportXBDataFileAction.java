package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.MagellanAction;
import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.XBDataFileExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import static com.dreamcodex.ti.Magellan.CHARACTER_SET_BASIC;

public class ExportXBDataFileAction extends MagellanAction {

    private final int exportType;

    public ExportXBDataFileAction(int exportType, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.exportType = exportType;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_BASIC, parent, parent, preferences.isExportComments(), preferences.getDefStartChar(), preferences.getDefEndChar(), preferences.getCharacterSetStart(), preferences.getCharacterSetCapacity() != CHARACTER_SET_BASIC || exportType == Globals.XB256_PROGRAM ? preferences.getCharacterSetEnd() : (exportType == Globals.XB_PROGRAM ? TIGlobals.FINALXBCHAR : TIGlobals.BASIC_LAST_CHAR), preferences.getSpriteSetEnd(), preferences.isCurrentMapOnly(), preferences.isExcludeBlank());
        if (exporter.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, XBEXTS, "XB Data Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < XBEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + XBEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + XBEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                int aLine = exporter.getCodeLineStart();
                int cLine = exporter.getCharLineStart();
                int mLine = exporter.getMapLineStart();
                int iLine = exporter.getLineInterval();
                preferences.setExportComments(exporter.includeComments());
                preferences.setCurrentMapOnly(exporter.currentMapOnly());
                preferences.setExcludeBlank(exporter.excludeBlank());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                XBDataFileExporter magIO = new XBDataFileExporter(mapEditor, dataSet, preferences);
                try {
                    magIO.writeXBDataFile(file, preferences.getDefStartChar(), preferences.getDefEndChar(), aLine, cLine, mLine, iLine, exportType, preferences.isExportComments(), preferences.isCurrentMapOnly(), preferences.isExcludeBlank());
                } catch (IOException ee) {
                    showError("Export failed", ee.getMessage());
                }
            }
        }
        exporter.dispose();

    }
}
