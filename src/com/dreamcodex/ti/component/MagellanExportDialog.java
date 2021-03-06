package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.NamedIcon;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MagellanExportDialog extends JDialog implements PropertyChangeListener, ActionListener {
    public static int TYPE_BASIC = 0;
    public static int TYPE_ASM = 1;
    public static int TYPE_BINARY = 2;
    public static int TYPE_XBSCRMER = 3;
    public static int TYPE_SCROLL = 4;

    public static int COMPRESSION_NONE = 0;
    public static int COMPRESSION_RLE_BYTE = 1;
    public static int COMPRESSION_RLE_WORD = 2;
    public static int COMPRESSION_META_2 = 3;
    public static int COMPRESSION_META_4 = 4;
    public static int COMPRESSION_NYBBLES = 5;

    private String OK_TEXT = "Export";
    private String CANCEL_TEXT = "Cancel";

    private JComboBox jcmbStartChar;
    private JComboBox jcmbEndChar;
    private JComboBox jcmbStartSprite;
    private JComboBox jcmbEndSprite;
    private JTextField jtxtCodeLineStart;
    private JTextField jtxtCharLineStart;
    private JTextField jtxtMapLineStart;
    private JTextField jtxtLineInterval;
    private JCheckBox jchkIncludeColorsets;
    private JCheckBox jchkIncludeChardata;
    private JCheckBox jchkIncludeSpritedata;
    private JCheckBox jchkIncludeComments;
    private JCheckBox jchkIncludeCharNumbers;
    private JCheckBox jchkCurrentMapOnly;
    private JCheckBox jchkExcludeBlank;
    private JPanel orientationRadioButtonPanel;
    private JRadioButton verticalButton;
    private JRadioButton horizontalButton;
    private JCheckBox jchkWrap;
    private JComboBox frameComboBox;
    private JComboBox compressComboBox;
    private boolean clickedOkay = false;
    private int minChar = 0;
    private int maxChar = 0;

    public MagellanExportDialog(int type, JFrame parent, IconProvider ip, Preferences preferences) {
        this(
            type,
                parent,
                ip,
                preferences.isExportComments(),
                preferences.getDefStartChar(),
                preferences.getDefEndChar(),
                preferences.getCharacterSetStart(),
                preferences.getCharacterSetEnd(),
                preferences.getDefStartSprite(),
                preferences.getDefEndSprite(),
                preferences.getSpriteSetEnd(),
                preferences.isCurrentMapOnly(),
                preferences.isExcludeBlank(),
                preferences.isIncludeCharNumbers(),
                preferences.isWrap(),
                preferences.isIncludeSpriteData(),
                preferences.getCompression(),
                preferences.getScrollOrientation(),
                preferences.getScrollFrames()
        );
    }

    public MagellanExportDialog(int type, JFrame parent, IconProvider ip, boolean setCommentsOn, int startChar, int endChar, int minc, int maxc, int maxSprite, boolean currentMapOnly, boolean excludeBlank) {
        this(type, parent, ip, setCommentsOn, startChar, endChar, minc, maxc, TIGlobals.MIN_SPRITE, maxSprite, maxSprite, currentMapOnly, excludeBlank, false, false, false, 0, 0, -1);
    }

    public MagellanExportDialog(int type, JFrame parent, IconProvider ip, boolean setCommentsOn, int startChar, int endChar, int minc, int maxc, int startSprite, int endsprite, int maxSprite, boolean currentMapOnly, boolean excludeBlank, boolean includeCharNumbers, boolean wrap, boolean includeSpriteData, int compression, int scrollOrientation, int scrollFrames) {
        super(parent, "Export Settings", true);
        minChar = minc;
        maxChar = maxc;
        if (type != TYPE_XBSCRMER) {
            jtxtCodeLineStart = new JTextField("100");
            jtxtCharLineStart = new JTextField("500");
        }
        else {
            jtxtCodeLineStart = new JTextField("30000");
            jtxtCharLineStart = new JTextField("28");
        }
        jtxtMapLineStart = new JTextField("900");
        jtxtLineInterval = new JTextField("10");
        jchkIncludeColorsets = new JCheckBox("Include Colorsets", true);
        jchkIncludeChardata = new JCheckBox("Include Character Data", true);
        jchkIncludeSpritedata = new JCheckBox("Include Sprite Data", includeSpriteData);
        jchkIncludeSpritedata.addActionListener(this);
        jchkIncludeComments = new JCheckBox("Include Comments", setCommentsOn);
        jchkIncludeCharNumbers = new JCheckBox("Include Character Numbers", includeCharNumbers);
        jchkCurrentMapOnly = new JCheckBox("Current Map Only", currentMapOnly);
        jchkExcludeBlank = new JCheckBox("Exclude Blank Characters", excludeBlank);

        verticalButton = new JRadioButton("Bottom to Top", true);
        verticalButton.addActionListener(this);
        horizontalButton = new JRadioButton("Left to Right");
        horizontalButton.addActionListener(this);
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(verticalButton);
        radioButtonGroup.add(horizontalButton);
        orientationRadioButtonPanel = new JPanel();
        orientationRadioButtonPanel.add(verticalButton);
        if (scrollOrientation == Magellan.SCROLL_ORIENTATION_VERTICAL) {
            verticalButton.setSelected(true);
        }
        orientationRadioButtonPanel.add(horizontalButton);
        if (scrollOrientation == Magellan.SCROLL_ORIENTATION_HORIZONTAL) {
            horizontalButton.setSelected(true);
        }

        jchkWrap = new JCheckBox("Wrap Edges", wrap);
        compressComboBox = new JComboBox(new String[] {"No compression", "RLE Compress Maps (bytes)", "RLE Compress Maps (words)", "2x2 Meta tiles", "4x4 Meta tiles", "Pack in nybbles (16 characters max)"});
        compressComboBox.setSelectedIndex(Math.min(compression, compressComboBox.getItemCount() - 1));
        frameComboBox = new JComboBox(new String[] {"0", "2", "4", "8"});
        if (scrollOrientation == Magellan.SCROLL_ORIENTATION_VERTICAL) {
            frameComboBox.addItem("2-character Strips");
        }

        frameComboBox.setSelectedIndex(Math.max(Math.min(scrollFrames != -1 ? (int) Math.floor(Math.log(scrollFrames) / Math.log(2)) : 4, frameComboBox.getItemCount() - 1), 0));

        jcmbStartChar = new JComboBox();
        jcmbStartChar.setRenderer(new CharListCellRenderer());
        jcmbEndChar = new JComboBox();
        jcmbEndChar.setRenderer(new CharListCellRenderer());

        for (int i = minChar; i <= maxChar; i++) {
            Icon icon = ip.getIconForChar(i);
            int chardex = i - TIGlobals.CHARMAPSTART;
            NamedIcon namedIcon = new NamedIcon(icon, (icon == null && chardex >= 0 && chardex < TIGlobals.CHARMAP.length ? TIGlobals.CHARMAP[chardex] + " " : "") + Integer.toString(i));
            jcmbStartChar.addItem(namedIcon);
            jcmbEndChar.addItem(namedIcon);
        }
        jcmbStartChar.setSelectedIndex(Math.min(Math.max(startChar - minChar, 0), jcmbStartChar.getItemCount() - 1));
        jcmbEndChar.setSelectedIndex(Math.max(Math.min(endChar - minChar, jcmbEndChar.getItemCount() - 1), 0));

        jcmbStartSprite = new JComboBox();
        jcmbStartSprite.setRenderer(new CharListCellRenderer());
        jcmbEndSprite = new JComboBox();
        jcmbEndSprite.setRenderer(new CharListCellRenderer());
        for (int i = TIGlobals.MIN_SPRITE; i <= maxSprite; i++) {
            Icon icon = ip.getIconForSprite(i);
            NamedIcon namedIcon = new NamedIcon(icon, Integer.toString(i));
            jcmbStartSprite.addItem(namedIcon);
            jcmbEndSprite.addItem(namedIcon);
        }
        jcmbStartSprite.setSelectedIndex(startSprite);
        jcmbEndSprite.setSelectedIndex(Math.min(endsprite, jcmbEndSprite.getItemCount() - 1));
        jcmbStartSprite.setEnabled(includeSpriteData);
        jcmbEndSprite.setEnabled(includeSpriteData);

        Object[] objForm = new Object[15];
        int objCount = 0;
        if (type != TYPE_XBSCRMER && type != TYPE_SCROLL) {
            objForm[objCount++] = new JLabel("From Char #");
            objForm[objCount++] = jcmbStartChar;
            objForm[objCount++] = new JLabel("To Char #");
            objForm[objCount++] = jcmbEndChar;
        }
        if (type == TYPE_BASIC) {
            objForm[objCount++] = new JLabel("Code Line Number Start");
            objForm[objCount++] = jtxtCodeLineStart;
            objForm[objCount++] = new JLabel("Character Data Line Number Start");
            objForm[objCount++] = jtxtCharLineStart;
            objForm[objCount++] = new JLabel("Map Data Line Number Start");
            objForm[objCount++] = jtxtMapLineStart;
            objForm[objCount++] = new JLabel("Line Number Interval");
            objForm[objCount++] = jtxtLineInterval;
            objForm[objCount++] = jchkExcludeBlank;
        }
        if (type == TYPE_BINARY) {
            objForm[objCount++] = jchkIncludeColorsets;
            objForm[objCount++] = jchkIncludeChardata;
            objForm[objCount++] = jchkIncludeSpritedata;
        }
        if (type == TYPE_XBSCRMER) {
            objForm[objCount++] = new JLabel("Code Line Number Start (0-32710)");
            objForm[objCount++] = jtxtCodeLineStart;
            objForm[objCount++] = new JLabel("Display width (28 or 32)");
            objForm[objCount++] = jtxtCharLineStart;
        }
        if (type == TYPE_ASM) {
            objForm[objCount++] = new JLabel("Map Compression");
            objForm[objCount++] = compressComboBox;
        }
        if (type == TYPE_SCROLL) {
            objForm[objCount++] = orientationRadioButtonPanel;
            objForm[objCount++] = jchkWrap;
            objForm[objCount++] = new JLabel("Map Compression");
            objForm[objCount++] = compressComboBox;
            objForm[objCount++] = new JLabel("Generate Scrolled Character Frames");
            objForm[objCount++] = frameComboBox;
        }
        if (type != TYPE_XBSCRMER) {
            objForm[objCount++] = jchkCurrentMapOnly;
        }
        if (type == TYPE_BASIC || type == TYPE_ASM || type == TYPE_SCROLL) {
            objForm[objCount++] = jchkIncludeComments;
        }
        if (type == TYPE_ASM || type == TYPE_SCROLL) {
            objForm[objCount++] = jchkIncludeCharNumbers;
        }
        if (type == TYPE_ASM) {
            objForm[objCount++] = jchkIncludeSpritedata;
            objForm[objCount++] = new JLabel("From Sprite #");
            objForm[objCount++] = jcmbStartSprite;
            objForm[objCount++] = new JLabel("To Sprite #");
            objForm[objCount++] = jcmbEndSprite;
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

    public int getStartChar() {
        return jcmbStartChar.getSelectedIndex() + minChar;
    }

    public int getEndChar() {
        return jcmbEndChar.getSelectedIndex() + minChar;
    }

    public int getStartSprite() {
        return jcmbStartSprite.getSelectedIndex();
    }

    public int getEndSprite() {
        return jcmbEndSprite.getSelectedIndex();
    }

    public int getCodeLineStart() {
        return Integer.parseInt(jtxtCodeLineStart.getText());
    }

    public int getCharLineStart() {
        return Integer.parseInt(jtxtCharLineStart.getText());
    }

    public int getMapLineStart() {
        return Integer.parseInt(jtxtMapLineStart.getText());
    }

    public int getLineInterval() {
        return Integer.parseInt(jtxtLineInterval.getText());
    }

    public boolean includeColorsets() {
        return jchkIncludeColorsets.isSelected();
    }

    public boolean includeChardata() {
        return jchkIncludeChardata.isSelected();
    }

    public boolean includeSpritedata() {
        return jchkIncludeSpritedata.isSelected();
    }

    public boolean includeComments() {
        return jchkIncludeComments.isSelected();
    }

    public boolean includeCharNumbers() {
        return jchkIncludeCharNumbers.isSelected();
    }

    public boolean currentMapOnly() {
        return jchkCurrentMapOnly.isSelected();
    }

    public boolean excludeBlank() {
        return jchkExcludeBlank.isSelected();
    }

    public int getScrollOrientation() {
        return verticalButton.isSelected() ? Magellan.SCROLL_ORIENTATION_VERTICAL : (horizontalButton.isSelected() ? Magellan.SCROLL_ORIENTATION_HORIZONTAL : Magellan.SCROLL_ORIENTATION_ISOMETRIC);
    }

    public boolean isWrap() {
        return jchkWrap.isSelected();
    }

    public int getCompression() {
        return compressComboBox.getSelectedIndex();
    }

    public int getFrames() {
        String frames = (String) frameComboBox.getSelectedItem();
        try {
            return Integer.parseInt(frames);
        } catch (NumberFormatException e) {
            return -1;
        }
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
        if (e.getSource() == verticalButton) {
            frameComboBox.addItem("2-character Strips");
        }
        else if (e.getSource() == horizontalButton) {
            frameComboBox.removeItemAt(4);
        }
        else if (e.getSource() == jchkIncludeSpritedata) {
            jcmbStartSprite.setEnabled(jchkIncludeSpritedata.isSelected());
            jcmbEndSprite.setEnabled(jchkIncludeSpritedata.isSelected());
        }
    }
}

