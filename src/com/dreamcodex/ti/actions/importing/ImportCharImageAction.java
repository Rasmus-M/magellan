package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.FileAction;
import com.dreamcodex.ti.component.MagellanImportDialog;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.CharacterImageColorImporter;
import com.dreamcodex.ti.importers.CharacterImageMonoImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImportCharImageAction extends FileAction {

    private final boolean color;

    public ImportCharImageAction(boolean color, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.color = color;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            MagellanImportDialog importDialog = new MagellanImportDialog(MagellanImportDialog.TYPE_CHAR_IMAGE, parent, parent, preferences, dataSet);
            if (importDialog.isOkay()) {
                int gap = importDialog.getGap();
                BufferedImage buffImg;
                try {
                    BufferedImage charImg = ImageIO.read(file);
                    int width = charImg.getWidth();
                    int height = charImg.getHeight();
                    buffImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                    if (color) {
                        Graphics2D g2d = ((Graphics2D) (buffImg.getGraphics()));
                        g2d.setColor(TIGlobals.TI_COLOR_TRANSOPAQUE);
                        g2d.fillRect(0, 0, width, height);
                        g2d.setComposite(AlphaComposite.SrcOver);
                        g2d.drawImage(charImg, 0, 0, parent);
                        g2d.setComposite(AlphaComposite.Src);
                    } else {
                        buffImg.getGraphics().drawImage(charImg, 0, 0, parent);
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                    showError("Error importing file", e.getMessage());
                    return;
                }
                try {
                    if (color) {
                        CharacterImageColorImporter importer = new CharacterImageColorImporter(mapEditor, dataSet, preferences);
                        importer.readCharImageColor(buffImg, importDialog.getStartChar(), importDialog.getEndChar(), gap, importDialog.skipBlank());
                    } else {
                        CharacterImageMonoImporter importer = new CharacterImageMonoImporter(mapEditor, dataSet, preferences);
                        importer.readCharImageMono(buffImg, importDialog.getStartChar(), importDialog.getEndChar(), importDialog.getGap(), importDialog.skipBlank());
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    showError("Error importing file", e.getMessage());
                }
                parent.updateAll();
                parent.setModified(true);
            }
        }
    }
}
