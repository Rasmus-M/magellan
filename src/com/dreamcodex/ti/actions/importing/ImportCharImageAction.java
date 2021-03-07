package com.dreamcodex.ti.actions.importing;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.MagellanAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.importers.CharacterImageColorImporter;
import com.dreamcodex.ti.importers.CharacterImageMonoImporter;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImportCharImageAction extends MagellanAction {

    private final boolean color;

    public ImportCharImageAction(boolean color, String name, Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {
        super(name, parent, mapEditor, dataSet, preferences);
        this.color = color;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File file = getFileFromChooser(preferences.getCurrentDirectory(), JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            Image charImg = getImage(file.getAbsolutePath());
            BufferedImage buffImg = new BufferedImage(8 * 8, 8 * 32, BufferedImage.TYPE_3BYTE_BGR);
            if (color) {
                Graphics2D g2d = ((Graphics2D) (buffImg.getGraphics()));
                g2d.setColor(TIGlobals.TI_COLOR_TRANSOPAQUE);
                g2d.fillRect(0, 0, 8 * 8, 8 * 32);
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.drawImage(charImg, 0, 0, parent);
                g2d.setComposite(AlphaComposite.Src);
            } else {
                buffImg.getGraphics().drawImage(charImg, 0, 0, parent);
            }
            if (color) {
                CharacterImageColorImporter magIO = new CharacterImageColorImporter(mapEditor, dataSet, preferences);
                magIO.readCharImageColor(buffImg);
            } else {
                CharacterImageMonoImporter magIO = new CharacterImageMonoImporter(mapEditor, dataSet, preferences);
                magIO.readCharImageMono(buffImg);
            }
            if (preferences.getColorMode() == Magellan.COLOR_MODE_ECM_2 || preferences.getColorMode() == Magellan.COLOR_MODE_ECM_3) {
                parent.updateCharPaletteCombo(true);
            }
            parent.updateCharButtons();
            parent.setModified(true);
        }
        parent.updateComponents();
    }
}
