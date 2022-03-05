package com.dreamcodex.ti.component;

import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.NamedIcon;
import com.dreamcodex.ti.util.Preferences;
import com.dreamcodex.ti.util.TIGlobals;
import com.dreamcodex.ti.util.TransitionType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MagellanExportDialog extends JDialog implements PropertyChangeListener, ActionListener {

    public static final int TYPE_BASIC = 0;
    public static final int TYPE_ASM = 1;
    public static final int TYPE_BINARY = 2;
    public static final int TYPE_XBSCRMER = 3;
    public static final int TYPE_SCROLL = 4;

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
    private JCheckBox jchkIncludeColorData;
    private JCheckBox jchkIncludeMapData;
    private JCheckBox jchkIncludeCharData;
    private JCheckBox jchkIncludeSpriteData;
    private JCheckBox jchkIncludeComments;
    private JCheckBox jchkIncludeCharNumbers;
    private JCheckBox jchkCurrentMapOnly;
    private JCheckBox jchkExcludeBlank;
    private JComboBox transitionTypeComboBox;
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
                preferences.isIncludeMapData(),
                preferences.isIncludeCharData(),
                preferences.isIncludeSpriteData(),
                preferences.isIncludeColorData(),
                preferences.getCompression(),
                preferences.getTransitionType(),
                preferences.getScrollFrames()
        );
    }

    public MagellanExportDialog(int type, JFrame parent, IconProvider ip, boolean setCommentsOn, int startChar, int endChar, int minc, int maxc, int maxSprite, boolean currentMapOnly, boolean excludeBlank) {
        this(type, parent, ip, setCommentsOn, startChar, endChar, minc, maxc, TIGlobals.MIN_SPRITE, maxSprite, maxSprite, currentMapOnly, excludeBlank, false, false, true, true, false, true, 0, TransitionType.BOTTOM_TO_TOP, -1);
    }

    public MagellanExportDialog(int type, JFrame parent, IconProvider ip, boolean setCommentsOn, int startChar, int endChar, int minc, int maxc, int startSprite, int endsprite, int maxSprite, boolean currentMapOnly, boolean excludeBlank, boolean includeCharNumbers, boolean wrap, boolean includeMapData, boolean includeCharData, boolean includeSpriteData, boolean includeColorData, int compression, TransitionType transitionType, int scrollFrames) {
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

        jchkIncludeColorData = new JCheckBox("Include Color Sets", includeColorData);

        jchkIncludeMapData = new JCheckBox("Include Map Data", includeMapData);
        jchkIncludeMapData.addActionListener(this);

        jchkIncludeCharData = new JCheckBox("Include Character Data", includeCharData);
        jchkIncludeCharData.addActionListener(this);

        jchkIncludeSpriteData = new JCheckBox("Include Sprite Data", includeSpriteData);
        jchkIncludeSpriteData.addActionListener(this);

        jchkIncludeComments = new JCheckBox("Include Comments", setCommentsOn);

        jchkIncludeCharNumbers = new JCheckBox("Include Character Numbers", includeCharNumbers);
        jchkIncludeCharNumbers.setEnabled(includeCharData);

        jchkCurrentMapOnly = new JCheckBox("Current Map Only", currentMapOnly);
        jchkCurrentMapOnly.setEnabled(includeMapData);

        jchkExcludeBlank = new JCheckBox("Exclude Blank Characters", excludeBlank);

        transitionTypeComboBox = new JComboBox(TransitionType.values());
        transitionTypeComboBox.setSelectedItem(transitionType);
        transitionTypeComboBox.addActionListener(this);

        jchkWrap = new JCheckBox("Wrap Edges", wrap);

        compressComboBox = new JComboBox(new String[] {"No compression", "RLE Compress Maps (bytes)", "RLE Compress Maps (words)", "2x2 Meta tiles", "4x4 Meta tiles", "Pack in nybbles (16 characters max)"});
        compressComboBox.setSelectedIndex(Math.min(compression, compressComboBox.getItemCount() - 1));
        compressComboBox.setEnabled(includeMapData);

        frameComboBox = new JComboBox(new String[] {"0", "2", "4", "8"});
        if (transitionType == TransitionType.BOTTOM_TO_TOP) {
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
        jcmbStartChar.setEnabled(includeCharData);
        jcmbEndChar.setEnabled(includeCharData);

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

        List<Object> objForm = new ArrayList<>();
        switch (type) {
            case TYPE_BASIC:
                objForm.add(new JLabel("From Char #"));
                objForm.add(jcmbStartChar);
                objForm.add(new JLabel("To Char #"));
                objForm.add(jcmbEndChar);
                objForm.add(new JLabel("Code Line Number Start"));
                objForm.add(jtxtCodeLineStart);
                objForm.add(new JLabel("Character Data Line Number Start"));
                objForm.add(jtxtCharLineStart);
                objForm.add(new JLabel("Map Data Line Number Start"));
                objForm.add(jtxtMapLineStart);
                objForm.add(new JLabel("Line Number Interval"));
                objForm.add(jtxtLineInterval);
                objForm.add(jchkExcludeBlank);
                objForm.add(jchkCurrentMapOnly);
                objForm.add(jchkIncludeComments);
                break;
            case TYPE_ASM:
                objForm.add(jchkIncludeMapData);
                objForm.add(jchkCurrentMapOnly);
                objForm.add(new JLabel("Map Compression"));
                objForm.add(compressComboBox);
                objForm.add(jchkIncludeCharData);
                objForm.add(new JLabel("From Char #"));
                objForm.add(jcmbStartChar);
                objForm.add(new JLabel("To Char #"));
                objForm.add(jcmbEndChar);
                objForm.add(jchkIncludeCharNumbers);
                objForm.add(jchkIncludeSpriteData);
                objForm.add(new JLabel("From Sprite #"));
                objForm.add(jcmbStartSprite);
                objForm.add(new JLabel("To Sprite #"));
                objForm.add(jcmbEndSprite);
                objForm.add(jchkIncludeColorData);
                objForm.add(jchkIncludeComments);
                break;
            case TYPE_BINARY:
                objForm.add(jchkCurrentMapOnly);
                objForm.add(jchkIncludeCharData);
                objForm.add(new JLabel("From Char #"));
                objForm.add(jcmbStartChar);
                objForm.add(new JLabel("To Char #"));
                objForm.add(jcmbEndChar);
                objForm.add(jchkIncludeSpriteData);
                objForm.add(new JLabel("From Sprite #"));
                objForm.add(jcmbStartSprite);
                objForm.add(new JLabel("To Sprite #"));
                objForm.add(jcmbEndSprite);
                objForm.add(jchkIncludeColorData);
                break;
            case TYPE_XBSCRMER:
                objForm.add(new JLabel("Code Line Number Start (0-32710)"));
                objForm.add(jtxtCodeLineStart);
                objForm.add(new JLabel("Display width (28 or 32)"));
                objForm.add(jtxtCharLineStart);
                break;
            case TYPE_SCROLL:
                objForm.add(new JLabel("Transition Type"));
                objForm.add(transitionTypeComboBox);
                objForm.add(jchkWrap);
                objForm.add(jchkCurrentMapOnly);
                objForm.add(new JLabel("Map Compression"));
                objForm.add(compressComboBox);
                objForm.add(new JLabel("Generate Scrolled Character Frames"));
                objForm.add(frameComboBox);
                objForm.add(jchkIncludeCharNumbers);
                objForm.add(jchkIncludeComments);
                break;
        }

        Object[] objButtons = {OK_TEXT, CANCEL_TEXT};

        JOptionPane joptMain = new JOptionPane(objForm.toArray(), JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, objButtons, objButtons[0]);
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

    public boolean includeColorData() {
        return jchkIncludeColorData.isSelected();
    }

    public boolean includeMapData() {
        return jchkIncludeMapData.isSelected();
    }

    public boolean includeCharData() {
        return jchkIncludeCharData.isSelected();
    }

    public boolean includeSpriteData() {
        return jchkIncludeSpriteData.isSelected();
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

    public TransitionType getScrollOrientation() {
        return (TransitionType) transitionTypeComboBox.getSelectedItem();
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
        if (e.getSource() == transitionTypeComboBox) {
            TransitionType transitionType = (TransitionType) transitionTypeComboBox.getSelectedItem();
            if (transitionType == TransitionType.BOTTOM_TO_TOP) {
                frameComboBox.addItem("2-character Strips");
            }
            else if (transitionType == TransitionType.LEFT_TO_RIGHT) {
                frameComboBox.removeItemAt(4);
            }
        }
        else if (e.getSource() == jchkIncludeMapData) {
            compressComboBox.setEnabled(jchkIncludeMapData.isSelected());
            jchkCurrentMapOnly.setEnabled(jchkIncludeMapData.isSelected());
        }
        else if (e.getSource() == jchkIncludeCharData) {
            jcmbStartChar.setEnabled(jchkIncludeCharData.isSelected());
            jcmbEndChar.setEnabled(jchkIncludeCharData.isSelected());
            jchkIncludeCharNumbers.setEnabled(jchkIncludeCharData.isSelected());
        }
        else if (e.getSource() == jchkIncludeSpriteData) {
            jcmbStartSprite.setEnabled(jchkIncludeSpriteData.isSelected());
            jcmbEndSprite.setEnabled(jchkIncludeSpriteData.isSelected());
        }
    }
}

