package com.dreamcodex.ti.actions.exporting;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.exporters.ScrollFileExporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ExportScrollFileAction extends FileAction {

    public ExportScrollFileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(
            MagellanExportDialog.TYPE_SCROLL,
            parent,
            parent,
            preferences.isExportComments(),
            preferences.getDefStartChar(),
            preferences.getDefEndChar(),
            TIGlobals.MIN_CHAR,
            preferences.getCharacterSetEnd(),
            preferences.getDefStartSprite(),
            preferences.getDefEndSprite(),
            preferences.getSpriteSetEnd(),
            preferences.isCurrentMapOnly(),
            preferences.isExcludeBlank(),
            preferences.isIncludeCharNumbers(),
            preferences.isWrap(),
            true,
            true,
            preferences.isIncludeSpriteData(),
            true,
            preferences.getCompression(),
            preferences.getTransitionType(),
            preferences.getScrollFrames()
        );
        if (exporter.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, ASMEXTS, "Assembler Source Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < ASMEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + ASMEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + ASMEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                preferences.setExportComments(exporter.includeComments());
                preferences.setIncludeCharNumbers(exporter.includeCharNumbers());
                preferences.setCurrentMapOnly(exporter.currentMapOnly());
                preferences.setWrap(exporter.isWrap());
                preferences.setDefStartChar(Math.min(sChar, eChar));
                preferences.setDefEndChar(Math.max(sChar, eChar));
                preferences.setCompression(exporter.getCompression());
                preferences.setTransitionType(exporter.getScrollOrientation());
                preferences.setScrollFrames(exporter.getFrames());
                ScrollFileExporter magIO = new ScrollFileExporter(mapEditor, dataSet, preferences);
                try {
                    magIO.writeScrollFile(file, preferences.getTransitionType(), preferences.isWrap(), preferences.getCompression(), preferences.isExportComments(), preferences.isCurrentMapOnly(), preferences.isIncludeCharNumbers(), preferences.getScrollFrames(), false);
                } catch (Exception ee) {
                    ee.printStackTrace();
                    showError("Export failed", ee.getMessage());
                }
            }
        }
        exporter.dispose();

    }
}
