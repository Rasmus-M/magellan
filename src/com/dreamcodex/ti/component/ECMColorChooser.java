package com.dreamcodex.ti.component;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 29-11-13
 * Time: 16:25
 */
public class ECMColorChooser extends JColorChooser {

    public static Color showDialog(Component component, String title, Color initialColor) throws HeadlessException {

        final JColorChooser pane = new ECMColorChooser(initialColor != null ? initialColor : Color.white);

        ColorTracker ok = new ColorTracker(pane);
        JDialog dialog = createDialog(component, title, true, pane, ok, null);

        dialog.setVisible(true);

        return ok.getColor();
    }

    public ECMColorChooser(Color initialColor) {
        this(new ECMColorSelectionModel(initialColor));
    }

    public ECMColorChooser(ColorSelectionModel model) {
        super(model);
        AbstractColorChooserPanel[] oldPanels = getChooserPanels();
        AbstractColorChooserPanel[] newPanels = new AbstractColorChooserPanel[oldPanels.length + 1];
        System.arraycopy(oldPanels, 0, newPanels, 1, oldPanels.length);
        newPanels[0] = new ECMColorChooserPanel();
        setChooserPanels(newPanels);
    }

    static class ColorTracker implements ActionListener, Serializable {
        JColorChooser chooser;
        Color color;

        public ColorTracker(JColorChooser c) {
            chooser = c;
        }

        public void actionPerformed(ActionEvent e) {
            color = chooser.getColor();
        }

        public Color getColor() {
            return color;
        }
    }
}
