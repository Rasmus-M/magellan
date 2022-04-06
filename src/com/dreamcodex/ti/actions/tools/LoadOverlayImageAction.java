package com.dreamcodex.ti.actions.tools;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LoadOverlayImageAction extends FileAction {

    public LoadOverlayImageAction(String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            try {
                BufferedImage image = ImageIO.read(file);
                mapEditor.setOverlay(image);
            } catch (IOException ee) {
                ee.printStackTrace();
                showError("Error reading file", ee.getMessage());
            }
        }
    }
}
