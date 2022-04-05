package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_2;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_ECM_3;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 29-01-2015
 * Time: 21:12
 */
public class MagellanImportDialog extends JDialog implements PropertyChangeListener, ActionListener, ItemListener {

    public static final int TYPE_MAP_IMAGE = 0;
    public static final int TYPE_SPRITE_IMAGE = 1;

    private static final String OK_TEXT = "Import";
    private static final String CANCEL_TEXT = "Cancel";

    private final JComboBox jcmbStartChar;
    private final JComboBox jcmbEndChar;
    private final JComboBox jcmbStartSprite;
    private final JComboBox jcmbStartPalette;
    private final JComboBox jcmbEndPalette;
    private final JSpinner jspnGap;
    private final JSlider jsldTolerance;
    private final JCheckBox useExistingPalettes;

    private boolean clickedOkay = false;

    public MagellanImportDialog(int type, JFrame parent, IconProvider ip, Preferences preferences, DataSet dataSet) {
        this(type, parent, ip, preferences.getColorMode(), preferences.getCharacterSetStart(), preferences.getCharacterSetEnd(), preferences.getSpriteSetEnd(), dataSet.getEcmPalettes());
    }

    public MagellanImportDialog(int type, JFrame parent, IconProvider ip, ColorMode colorMode, int minc, int maxc, int maxSprite, ECMPalette[] palettes) {
        super(parent, "Import Settings", true);

        jcmbStartChar = new JComboBox();
        jcmbStartChar.setRenderer(new CharListCellRenderer());
        for (int i = minc; i <= maxc; i += 8) {
            Icon icon = ip.getIconForChar(i);
            int chardex = i - TIGlobals.CHARMAPSTART;
            NamedIcon namedIcon = new NamedIcon(icon, (icon == null && chardex >= 0 && chardex < TIGlobals.CHARMAP.length ? TIGlobals.CHARMAP[chardex] + " " : "") + Integer.toString(i));
            jcmbStartChar.addItem(namedIcon);
        }
        jcmbStartChar.setSelectedIndex(0);
        jcmbStartChar.addItemListener(this);

        jcmbEndChar = new JComboBox();
        jcmbEndChar.setRenderer(new CharListCellRenderer());
        for (int i = minc; i <= maxc; i++) {
            Icon icon = ip.getIconForChar(i);
            int chardex = i - TIGlobals.CHARMAPSTART;
            NamedIcon namedIcon = new NamedIcon(icon, (icon == null && chardex >= 0 && chardex < TIGlobals.CHARMAP.length ? TIGlobals.CHARMAP[chardex] + " " : "") + Integer.toString(i));
            jcmbEndChar.addItem(namedIcon);
        }
        jcmbEndChar.setSelectedIndex(maxc - minc);

        jcmbStartSprite = new JComboBox();
        jcmbStartSprite.setRenderer(new CharListCellRenderer());
        for (int i = TIGlobals.MIN_SPRITE; i <= maxSprite; i++) {
            Icon icon = ip.getIconForSprite(i);
            NamedIcon namedIcon = new NamedIcon(icon, Integer.toString(i));
            jcmbStartSprite.addItem(namedIcon);
        }
        jcmbStartSprite.setSelectedIndex(0);

        jcmbStartPalette = colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? new ECMPaletteComboBox(palettes, -1, -1, this, type == TYPE_MAP_IMAGE) : null;
        jcmbEndPalette = colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? new ECMPaletteComboBox(palettes, -1, -1, this, type == TYPE_MAP_IMAGE) : null;

        jspnGap = new JSpinner(new SpinnerNumberModel(0, 0, 8, 1));
        jsldTolerance = new JSlider(0, 16, 0);
        jsldTolerance.setMajorTickSpacing(2);
        jsldTolerance.setMinorTickSpacing(1);
        jsldTolerance.setSnapToTicks(true);
        jsldTolerance.setPaintTicks(true);
        jsldTolerance.setPaintLabels(true);

        useExistingPalettes = new JCheckBox("Use existing palettes");

        Object[] objForm = new Object[15];
        int objCount = 0;
        switch (type) {
            case TYPE_MAP_IMAGE:
                objForm[objCount++] = new JLabel("Start Import at Char #");
                objForm[objCount++] = jcmbStartChar;
                objForm[objCount++] = new JLabel("End/Max Import Char #");
                objForm[objCount++] = jcmbEndChar;
                objForm[objCount++] = new JLabel("Tolerance");
                objForm[objCount++] = jsldTolerance;
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    objForm[objCount++] = new JLabel("Start Palette");
                    objForm[objCount++] = jcmbStartPalette;
                }
                break;
            case TYPE_SPRITE_IMAGE:
                objForm[objCount++] = new JLabel("Start Import at Sprite #");
                objForm[objCount++] = jcmbStartSprite;
                objForm[objCount++] = new JLabel("Pixel gap between Sprites");
                objForm[objCount++] = jspnGap;
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    objForm[objCount++] = new JLabel("Start Palette");
                    objForm[objCount++] = jcmbStartPalette;
                    objForm[objCount++] = new JLabel("End Palette");
                    objForm[objCount++] = jcmbEndPalette;
                    objForm[objCount] = useExistingPalettes;
                }
                break;
        }
        Object[] objButtons = {OK_TEXT, CANCEL_TEXT};
        JOptionPane joptMain = new JOptionPane(objForm, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, objButtons, objButtons[0]);
        joptMain.addPropertyChangeListener(this);
        this.setContentPane(joptMain);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(this);
        this.setVisible(true);
    }

    public boolean isOkay() {
        return clickedOkay;
    }

    /* PropertyChangeListener method */
    public void propertyChange(PropertyChangeEvent pce) {
        if (pce != null && pce.getNewValue() != null) {
            if (pce.getNewValue().equals(OK_TEXT)) {
                clickedOkay = true;
                this.setVisible(false);
            }
            else if (pce.getNewValue().equals(CANCEL_TEXT)) {
                clickedOkay = false;
                this.setVisible(false);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
    }

    public int getStartChar() {
        return jcmbStartChar.getSelectedIndex() * 8;
    }

    public int getEndChar() {
        return Math.max(jcmbEndChar.getSelectedIndex(), getStartChar());
    }

    public int getStartSprite() {
        return jcmbStartSprite.getSelectedIndex();
    }

    public int getGap() {
        return (Integer) jspnGap.getValue();
    }

    public int getStartPalette() {
        return jcmbStartPalette != null ? jcmbStartPalette.getSelectedIndex() : 0;
    }

    public int getEndPalette() {
        return jcmbEndPalette != null ? jcmbEndPalette.getSelectedIndex() : 0;
    }

    public int getTolerance() {
        int value = jsldTolerance.getValue();
        return value == 0 ? 0 : (int) Math.pow(2, value);
    }

    public boolean useExistingPalettes() {
        return this.useExistingPalettes.isSelected();
    }

    public void itemStateChanged(ItemEvent e) {
        if (getStartChar() > jcmbEndChar.getSelectedIndex()) {
            jcmbEndChar.setSelectedIndex(getStartChar());
        }
    }
}
