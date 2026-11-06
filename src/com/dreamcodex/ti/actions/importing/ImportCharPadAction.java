package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.CharPadImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;

/**
 * Imports the characters and map of a C64 CharPad project (.ctm).
 */
public class ImportCharPadAction extends FileAction {

    public ImportCharPadAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, CTMEXTS, "CharPad Files", false);
        if (file == null) {
            return;
        }
        CharPadImporter.CtmData ctm;
        try {
            ctm = CharPadImporter.parse(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            showError("Error importing file", e.getMessage());
            return;
        }
        int startChar = preferences.getCharacterSetStart();
        int endChar = preferences.getCharacterSetEnd();
        if (startChar + ctm.charCount - 1 > endChar) {
            showError("Error importing file", "The file has " + ctm.charCount + " characters, but the current character set only has room for " + (endChar - startChar + 1) + ".\nPlease select a larger character set in the Options menu.");
            return;
        }
        // The map is imported as characters: in metatile mode, switch to no metatiles
        // while importing and back afterwards, which turns the imported map into metatiles
        int metatileSize = dataSet.getMetatileSize();
        if (metatileSize != 0) {
            parent.setMetatileMode(0);
        }
        try {
            CharPadImporter importer = new CharPadImporter(mapEditor, dataSet, preferences);
            importer.importCharPad(ctm, startChar);
            // The map has been replaced, possibly with a different size
            mapEditor.resetUndoManager();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            showError("Error importing file", e.getMessage());
        }
        parent.updateAll();
        if (metatileSize != 0) {
            // Shows an error and stays in no metatiles if the maps need too many metatiles
            parent.setMetatileMode(metatileSize);
        }
        parent.setModified(true);
    }
}
