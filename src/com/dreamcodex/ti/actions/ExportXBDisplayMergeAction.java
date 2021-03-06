package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.component.MagellanExportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;

public class ExportXBDisplayMergeAction extends MagellanAction {

    public ExportXBDisplayMergeAction(String name, JFrame parent, IconProvider iconProvider, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, iconProvider, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_XBSCRMER, parent, iconProvider, preferences.isExportComments(), preferences.getDefStartChar(), preferences.getDefEndChar(), TIGlobals.MIN_CHAR, preferences.getCharacterSetEnd(), preferences.getSpriteSetEnd(), preferences.isCurrentMapOnly(), preferences.isExcludeBlank());
        if (exporter.isOkay()) {
            File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.SAVE_DIALOG, ANYS, "Screen Merge Files");
            if (file != null) {
                int lineNo = exporter.getCodeLineStart();
                int dispWidth = exporter.getCharLineStart();

                if (lineNo >= 0 && lineNo <= 32710 && (dispWidth == 28 || dispWidth == 32)) {
                    int currentMap = mapEditor.getCurrentMapId();
                    int[][] map = mapEditor.getMapData(currentMap);

                    byte[] init = {7, 'T', 'I', 'F', 'I', 'L', 'E', 'S', 0, 6, (byte) 0x80, 1, (byte) 0x8B, (byte) 0xA3, 6};
                    byte[] tifiles = new byte[1664];
                    System.arraycopy(init, 0, tifiles, 0, init.length);
                    int sectorPosition = 0x80;
                    byte lineSize = 0x76;
                    int mapLine = 0;
                    int outputPos = 0;
                    int colStart = 2;
                    int colEnd = 29;
                    byte stringSize = (byte) 0x70;
                    if (dispWidth == 32) {
                        lineSize = (byte) 0x86;
                        colStart = 0;
                        colEnd = 31;
                        stringSize = (byte) 0x80;
                    }

                    for (int chunk = 0; chunk < 6; chunk++) {
                        outputPos = sectorPosition;
                        tifiles[outputPos++] = lineSize;
                        tifiles[outputPos++] = (byte) (lineNo >> 8);
                        tifiles[outputPos++] = (byte) (lineNo & 0xFF);
                        tifiles[outputPos++] = (byte) 0x93; // DATA "
                        tifiles[outputPos++] = (byte) 0xC7; //
                        tifiles[outputPos++] = stringSize;
                        if (dispWidth == 28) {
                            for (int line = 0; line < 4; line++)
                                for (int column = colStart; column <= colEnd; column++)
                                    tifiles[outputPos++] = (byte) map[mapLine + line][column];
                        } else {
                            for (int line = 0; line < 4; line++)
                                for (int column = colStart; column <= colEnd; column++)
                                    tifiles[outputPos++] = (byte) (0x60 + map[mapLine + line][column]);
                        }
                        tifiles[outputPos++] = 0; // end of string
                        lineNo += 10;
                        mapLine += 4;
                        tifiles[outputPos] = (byte) 0xFF; // end of line and sector
                        sectorPosition += 0x100;
                    }
                    tifiles[outputPos++] = (byte) 0x02; // end of file
                    tifiles[outputPos++] = (byte) 0xFF;
                    tifiles[outputPos++] = (byte) 0xFF;
                    tifiles[outputPos] = (byte) 0xFF;

                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(tifiles);
                        fos.flush();
                        fos.close();
                    } catch (Exception ee) {
                        showError("Export failed", ee.getMessage());
                    }
                }
            }
        }
        exporter.dispose();
    }
}
