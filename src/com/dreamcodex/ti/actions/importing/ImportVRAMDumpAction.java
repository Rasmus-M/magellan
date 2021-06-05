package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.component.VramImportDialog;
import com.dreamcodex.ti.importers.VRAMDumpImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImportVRAMDumpAction extends FileAction {

    public ImportVRAMDumpAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, VDPEXTS, "VRAM Dump Files", false);
        if (file != null) {
            int charOffset = 0;
            int mapOffset = 0;
            int colorOffset = 0;
            int spriteOffset = 0;
            int spriteAttrOffset = 0;
            boolean bitmapMode = false;
            boolean textMode = false;
            int textColor = 0;
            int screenColor = 0;
            try {
                if (file.length() == 0x4008) {
                    FileInputStream fib = new FileInputStream(file);
                    if (fib.skip(0x4000) == 0x4000) {
                        int[] vdpRegs = new int[8];
                        for (int i = 0; i < 8; i++) {
                            vdpRegs[i] = fib.read();
                        }
                        bitmapMode = (vdpRegs[0] & 0x02) != 0;
                        textMode = (vdpRegs[1] & 0x10) != 0;
                        mapOffset = (vdpRegs[2] & 0x0F) * 0x400;
                        if (bitmapMode) {
                            colorOffset = (vdpRegs[3] & 0x80) != 0 ? 0x2000 : 0;
                            charOffset = (vdpRegs[4] & 0x04) != 0 ? 0x2000 : 0;
                        } else {
                            colorOffset = (vdpRegs[3] & 0xFF) * 0x40;
                            charOffset = (vdpRegs[4] & 0x07) * 0x800;
                        }
                        spriteAttrOffset = (vdpRegs[5] & 0x7F) * 0x80;
                        spriteOffset = (vdpRegs[6] & 0x07) * 0x800;
                        textColor = (vdpRegs[7] & 0xF0) >> 4;
                        screenColor = vdpRegs[7] & 0x0F;
                    }
                    fib.close();
                } else {
                    VramImportDialog importDialog = new VramImportDialog(parent);
                    if (importDialog.isOkay()) {
                        charOffset = importDialog.getCharDataOffset();
                        mapOffset = importDialog.getMapDataOffset();
                        colorOffset = importDialog.getColorDataOffset();
                        spriteOffset = importDialog.getSpriteDataOffset();
                        spriteAttrOffset = importDialog.getSpriteAttrDataOffset();
                        bitmapMode = importDialog.isBitmapMode();
                    }
                }
                VRAMDumpImporter importer = new VRAMDumpImporter(mapEditor, dataSet, preferences);
                importer.readVRAMDumpFile(file, charOffset, mapOffset, colorOffset, spriteOffset, spriteAttrOffset, bitmapMode, textMode, textColor, screenColor);
                parent.updateAll();
                parent.setModified(true);
            } catch (IOException ioException) {
                showError("Error importing file", ioException.getMessage());
            }
        }
    }
}
