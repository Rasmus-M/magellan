package com.dreamcodex.ti.actions;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.component.ImagePreview;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.MutableFilter;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public abstract class FileAction extends MagellanAction {

    protected final String FILEEXT = "mag";
    protected final String[] FILEEXTS = {FILEEXT};
    protected final String XBEXT = "xb";
    protected final String[] XBEXTS = {XBEXT, "bas", "txt"};
    protected final String ASMEXT = "a99";
    protected final String[] ASMEXTS = {ASMEXT, "asm"};
    protected final String IMGEXT = "png";
    protected final String[] IMGEXTS = {IMGEXT, "gif"};
    protected final String BINEXT = "mgb";
    protected final String[] BINEXTS = {BINEXT, "bin"};
    protected final String VDPEXT = "vdp";
    protected final String[] VDPEXTS = {VDPEXT, "vram", "bin"};
    protected final String ANY = "";
    protected final String[] ANYS = {ANY};

    public FileAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    protected File getFileFromChooser(String startDir, int dialogType, String[] exts, String desc) {
        return getFileFromChooser(startDir, dialogType, exts, desc, false);
    }

    protected File getFileFromChooser(String startDir, int dialogType, String[] exts, String desc, boolean hasImagePreview) {
        JFileChooser jfileDialog = new JFileChooser(startDir);
        jfileDialog.setDialogType(dialogType);
        if (!exts[0].equals(ANY)) {
            jfileDialog.setFileFilter(new MutableFilter(exts, desc));
        }
        if (hasImagePreview) {
            jfileDialog.setAccessory(new ImagePreview(jfileDialog));
        }
        int optionSelected = JFileChooser.CANCEL_OPTION;
        if (dialogType == JFileChooser.OPEN_DIALOG) {
            optionSelected = jfileDialog.showOpenDialog(this.parent);
        } else if (dialogType == JFileChooser.SAVE_DIALOG) {
            optionSelected = jfileDialog.showSaveDialog(this.parent);
        } else // default to an OPEN_DIALOG
        {
            optionSelected = jfileDialog.showOpenDialog(this.parent);
        }
        if (optionSelected == JFileChooser.APPROVE_OPTION) {
            File file = jfileDialog.getSelectedFile();
            if (file != null) {
                preferences.setCurrentDirectory(file.getParent());
            }
            return file;
        }
        return null;
    }
}
