package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.DataFileImporter;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.dreamcodex.ti.util.TIGlobals.SUPER_LAST_CHAR;

public class OpenDataFileAction extends FileAction {

    private final String filePath;

    public OpenDataFileAction(String name, String filePath, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.filePath = filePath;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file;
        if (filePath != null) {
            file = new File(filePath);
        }
        else {
            file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, FILEEXTS, "Map Data Files");
        }
        if (file != null && file.exists()) {
            try {
                if (!checkColorModeAndCharacterRange(file)) {
                    return;
                }
                parent.newProject();
                DataFileImporter importer = new DataFileImporter(mapEditor, dataSet, preferences);
                importer.readDataFile(file);
                parent.setMapDataFile(file);
                preferences.addRecentFile(file.getAbsolutePath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
                showError("Error opening data file", ioException.getMessage());
            }
        }
        mapEditor.goToMap(0);
        parent.updateAll();
        parent.editDefault();
        parent.setModified(false);
    }

    private boolean checkColorModeAndCharacterRange(File file) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            int linesToProcess = 2;
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    if (line.startsWith(Globals.KEY_COLOR_MODE)) {
                        ColorMode fileColorMode = ColorMode.values()[Integer.parseInt(line.substring(Globals.KEY_COLOR_MODE.length()))];
                        if (fileColorMode != preferences.getColorMode()) {
                            int reply = showConfirmation("Confirm Color Mode Change", "This file was saved in " + fileColorMode + ". Do you want to switch to that mode before loading the file?", true);
                            if (reply == JOptionPane.YES_OPTION) {
                                parent.setColorModeOption(fileColorMode);
                            }
                            else if (reply == JOptionPane.CANCEL_OPTION) {
                                return false;
                            }
                        }
                        linesToProcess--;
                    } else if (line.startsWith(Globals.KEY_CHARRANG)) {
                        int endChar = Integer.parseInt(line.substring(Globals.KEY_CHARRANG.length()).split("\\|")[1]);
                        if (endChar == SUPER_LAST_CHAR && preferences.getCharacterSetEnd() != endChar) {
                            parent.setSuperCharacterSetOption();
                        }
                        linesToProcess--;
                    }
                }
            } while (line != null && linesToProcess > 0);
            return true;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex1) {
                    showError("Error opening data file", ex1.getMessage());
                    ex1.printStackTrace(System.err);
                }
            }
        }
    }
}
