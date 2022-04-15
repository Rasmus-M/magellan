package com.dreamcodex.ti.ui;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.clipboard.CopyCharAction;
import com.dreamcodex.ti.actions.clipboard.CopySpriteAction;
import com.dreamcodex.ti.actions.clipboard.PasteCharAction;
import com.dreamcodex.ti.actions.clipboard.PasteSpriteAction;
import com.dreamcodex.ti.actions.exporting.*;
import com.dreamcodex.ti.actions.importing.*;
import com.dreamcodex.ti.actions.tools.ClearOverlayImageAction;
import com.dreamcodex.ti.actions.tools.LoadOverlayImageAction;
import com.dreamcodex.ti.actions.tools.ShowSpritesPerLineAction;
import com.dreamcodex.ti.component.*;
import com.dreamcodex.ti.util.*;

import javax.swing.*;

import java.awt.*;
import java.net.URL;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.util.ColorMode.*;
import static com.dreamcodex.ti.util.Globals.*;
import static com.dreamcodex.ti.util.Globals.CMD_SNAP_SPRITES_TO_GRID;
import static com.dreamcodex.ti.util.TIGlobals.EXP_LAST_CHAR;
import static com.dreamcodex.ti.util.TIGlobals.MAX_CHAR;

public class MagellanUI {

    private static final Color CLR_CHARS_BASE1 = new Color(232, 232, 232);
    private static final Color CLR_CHARS_BASE2 = new Color(196, 196, 196);
    private static final Color CLR_CHARS_LOWER = new Color(222, 242, 255);
    private static final Color CLR_CHARS_UPPER = new Color(255, 222, 242);

    private static final int EDITOR_GRID_SIZE = 192;

    private final Magellan parent;
    private final MapEditor mapEditor;
    private final DataSet dataSet;
    private final Preferences preferences;

    private ButtonGroup colorModeButtonGroup;
    private JMenuItem graphicsColorModeMenuItem;
    private JMenuItem bitmapColorModeMenuItem;
    private JMenuItem ecm2ColorModeMenuItem;
    private JMenuItem ecm3ColorModeMenuItem;

    private ButtonGroup characterSetSizeButtonGroup;
    private JMenuItem characterSetBasicMenuItem;
    private JMenuItem characterSetExpandedMenuItem;
    private JMenuItem characterSetSuperMenuItem;

    // Character editor
    private GridCanvas charGridCanvas;
    private JTextField charTextField;
    private JButton lookButton;
    private JButton charUndoButton;
    private JButton charRedoButton;
    private JLabel charIntLabel;
    private JLabel charHexLabel;
    private JCheckBox transparencyCheckBox;
    private JPanel charColorDockPanel;
    private DualClickButton[] charColorDockButtons = new DualClickButton[0];
    private ECMPaletteComboBox charECMPaletteComboBox;
    private JPanel characterDockPanel;
    private JButton[] charButtons;

    // Sprite editor
    private GridCanvas spriteGridCanvas;
    private JTextField spriteTextField;
    private JButton spriteUndoButton;
    private JButton spriteRedoButton;
    private JLabel spriteIntLabel;
    private JLabel spriteHexLabel;
    private JPanel spriteColorDockPanel;
    private DualClickButton[] spriteColorDockButtons = new DualClickButton[0];
    private ECMPaletteComboBox spriteECMPaletteComboBox;
    private JPanel spriteDockPanel;
    private JButton[] spriteButtons;

    // Tabbed pane
    private JTabbedPane editorTabbedPane;

    public MagellanUI(Magellan parent, MapEditor mapEditor, DataSet dataSet, Preferences preferences) {

        this.parent = parent;
        this.mapEditor = mapEditor;
        this.dataSet = dataSet;
        this.preferences = preferences;
    }

    public GridCanvas getCharGridCanvas() {
        return charGridCanvas;
    }

    public JTextField getCharTextField() {
        return charTextField;
    }

    public JButton getLookButton() {
        return lookButton;
    }

    public JButton getCharUndoButton() {
        return charUndoButton;
    }

    public JButton getCharRedoButton() {
        return charRedoButton;
    }

    public JLabel getCharIntLabel() {
        return charIntLabel;
    }

    public JLabel getCharHexLabel() {
        return charHexLabel;
    }

    public JCheckBox getTransparencyCheckBox() {
        return transparencyCheckBox;
    }

    public DualClickButton[] getCharColorDockButtons() {
        return charColorDockButtons;
    }

    public ECMPaletteComboBox getCharECMPaletteComboBox() {
        return charECMPaletteComboBox;
    }

    public JButton[] getCharButtons() {
        return charButtons;
    }

    public GridCanvas getSpriteGridCanvas() {
        return spriteGridCanvas;
    }

    public JTextField getSpriteTextField() {
        return spriteTextField;
    }

    public JButton getSpriteUndoButton() {
        return spriteUndoButton;
    }

    public JButton getSpriteRedoButton() {
        return spriteRedoButton;
    }

    public JLabel getSpriteIntLabel() {
        return spriteIntLabel;
    }

    public JLabel getSpriteHexLabel() {
        return spriteHexLabel;
    }

    public DualClickButton[] getSpriteColorDockButtons() {
        return spriteColorDockButtons;
    }

    public ECMPaletteComboBox getSpriteECMPaletteComboBox() {
        return spriteECMPaletteComboBox;
    }

    public JButton[] getSpriteButtons() {
        return spriteButtons;
    }

    public JTabbedPane getEditorTabbedPane() {
        return editorTabbedPane;
    }

    public JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem jmitNew = new JMenuItem("New Map Project");
        jmitNew.setActionCommand(Globals.CMD_NEW);
        jmitNew.addActionListener(parent);
        fileMenu.add(jmitNew);
        JMenuItem jmitOpen = new JMenuItem();
        jmitOpen.setAction(new OpenDataFileAction("Open Map Project", null, parent, mapEditor, dataSet, preferences));
        fileMenu.add(jmitOpen);
        JMenu jmenuOpenRecent = new RecentMenu(preferences.getRecentFiles(), parent, mapEditor, dataSet, preferences);
        fileMenu.add(jmenuOpenRecent);
        JMenuItem jmitSave = new JMenuItem();
        jmitSave.setAction(new SaveDataFileAction("Save Map Project", parent, mapEditor, dataSet, preferences));
        fileMenu.add(jmitSave);
        JMenuItem jmitSaveAs = new JMenuItem();
        jmitSaveAs.setAction(new SaveDataFileAsAction("Save Map Project as...", parent, mapEditor, dataSet, preferences));
        fileMenu.add(jmitSaveAs);
        fileMenu.addSeparator();
        JMenuItem jmitAppend = new JMenuItem();
        jmitAppend.setAction(new AppendDataFileAction("Append Maps", parent, mapEditor, dataSet, preferences));
        fileMenu.add(jmitAppend);
        fileMenu.addSeparator();
        JMenuItem jmitExit = new JMenuItem("Exit");
        jmitExit.setActionCommand(Globals.CMD_EXIT);
        jmitExit.addActionListener(parent);
        fileMenu.add(jmitExit);
        // Add menu
        menuBar.add(fileMenu);

        JMenu importMenu = new JMenu("Import");
        JMenuItem jmitImportChrImgMono = new JMenuItem();
        jmitImportChrImgMono.setAction(new ImportCharImageAction(false, "Character Image (Mono)", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportChrImgMono);
        JMenuItem jmitImportChrImgColor = new JMenuItem();
        jmitImportChrImgColor.setAction(new ImportCharImageAction(true, "Character Image (Color)", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportChrImgColor);
        JMenuItem jmitImportVramDump = new JMenuItem();
        jmitImportVramDump.setAction(new ImportVRAMDumpAction("VRAM Dump", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportVramDump);
        JMenuItem jmitImportBinaryMap = new JMenuItem();
        jmitImportBinaryMap.setAction(new ImportBinaryMapAction("Binary Map", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportBinaryMap);
        JMenuItem jmitImportMapImage = new JMenuItem();
        jmitImportMapImage.setAction(new ImportMapImageAction("Map Image", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportMapImage);
        JMenuItem jmitImportSpriteImage = new JMenuItem();
        jmitImportSpriteImage.setAction(new ImportSpriteImageAction("Sprite Image", parent, mapEditor, dataSet, preferences));
        importMenu.add(jmitImportSpriteImage);
        // Add menu
        menuBar.add(importMenu);

        JMenu exportMenu = new JMenu("Export");
        JMenuItem jmitExportData = new JMenuItem();
        jmitExportData.setAction(new ExportXBDataFileAction(BASIC_DATA, "BASIC Data", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportData);
        JMenuItem jmitExportBasic = new JMenuItem();
        jmitExportBasic.setAction(new ExportXBDataFileAction(BASIC_PROGRAM, "BASIC Program", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportBasic);
        JMenuItem jmitExportExec = new JMenuItem();
        jmitExportExec.setAction(new ExportXBDataFileAction(XB_PROGRAM, "XB Program", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportExec);
        JMenuItem jmitExportXB256 = new JMenuItem();
        jmitExportXB256.setAction(new ExportXBDataFileAction(XB256_PROGRAM, "XB 256 Program", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportXB256);
        JMenuItem jmitExportXBDisMer = new JMenuItem();
        jmitExportXBDisMer.setAction(new ExportXBDisplayMergeAction("XB Display Merge", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportXBDisMer);
        exportMenu.addSeparator();
        JMenuItem jmitExportAsm = new JMenuItem();
        jmitExportAsm.setAction(new ExportAssemblyDataFileAction("Assembly Data", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportAsm);
        JMenuItem jmitExportScrollMap = new JMenuItem();
        jmitExportScrollMap.setAction(new ExportScrollFileAction("Assembly Scroll Data", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportScrollMap);
        exportMenu.addSeparator();
        JMenuItem jmitExportBin = new JMenuItem();
        jmitExportBin.setAction(new ExportBinaryFileAction("Binary Data", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportBin);
        JMenuItem jmitExportBinMap = new JMenuItem();
        jmitExportBinMap.setAction(new ExportBinaryMapAction("Binary Map (current)", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportBinMap);
        exportMenu.addSeparator();
        JMenuItem jmitExportChrImgMono = new JMenuItem();
        jmitExportChrImgMono.setAction(new ExportCharImageAction(false, "Character Image (Mono)", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportChrImgMono);
        JMenuItem jmitExportChrImgColor = new JMenuItem();
        jmitExportChrImgColor.setAction(new ExportCharImageAction(true, "Character Image (Color)", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportChrImgColor);
        JMenuItem jmitExportSpriteImg = new JMenuItem();
        jmitExportSpriteImg.setAction(new ExportSpriteImageAction(true, "Sprite Image", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportSpriteImg);
        JMenuItem jmitExportMapImg = new JMenuItem();
        jmitExportMapImg.setAction(new ExportMapImageAction("Map Image", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportMapImg);
        // Add menu
        menuBar.add(exportMenu);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem jmitSwapChars = new JMenuItem("Replace Characters");
        jmitSwapChars.setActionCommand(Globals.CMD_SWAPCHARS);
        jmitSwapChars.addActionListener(parent);
        toolsMenu.add(jmitSwapChars);
        toolsMenu.addSeparator();
        JMenuItem jmitAnalyzeCharUsage = new JMenuItem("Analyze Character Usage");
        jmitAnalyzeCharUsage.setActionCommand(Globals.CMD_ANALYZECHARUSAGE);
        jmitAnalyzeCharUsage.addActionListener(parent);
        toolsMenu.add(jmitAnalyzeCharUsage);
        JMenuItem jmitAnalyzeCharTrans = new JMenuItem("Analyze Character Transitions");
        jmitAnalyzeCharTrans.setActionCommand(Globals.CMD_ANALYZECHARTRANS);
        jmitAnalyzeCharTrans.addActionListener(parent);
        toolsMenu.add(jmitAnalyzeCharTrans);
        toolsMenu.addSeparator();
        toolsMenu.add(new LoadOverlayImageAction("Load Overlay Image", parent, mapEditor, dataSet, preferences));
        toolsMenu.add(new ClearOverlayImageAction("Clear Overlay Image", parent, mapEditor, dataSet, preferences));
        // Add menu
        menuBar.add(toolsMenu);

        JMenu optionsMenu = new JMenu("Options");
        JMenuItem jmitShowPos = new JCheckBoxMenuItem("Show Position", mapEditor.showPosIndic());
        jmitShowPos.setActionCommand(Globals.CMD_SHOWPOS);
        jmitShowPos.addActionListener(parent);
        optionsMenu.add(jmitShowPos);
        JMenuItem jmitBase0Pos = new JCheckBoxMenuItem("Base 0 for Position", mapEditor.base0Position());
        jmitBase0Pos.setActionCommand(Globals.CMD_BASE0POS);
        jmitBase0Pos.addActionListener(parent);
        optionsMenu.add(jmitBase0Pos);

        optionsMenu.addSeparator();

        characterSetSizeButtonGroup = new ButtonGroup();

        characterSetBasicMenuItem = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_BASIC], preferences.getCharacterSetCapacity() == CHARACTER_SET_BASIC);
        characterSetSizeButtonGroup.add(characterSetBasicMenuItem);
        characterSetBasicMenuItem.setActionCommand(Globals.CMD_BASICCHARSETSIZE);
        characterSetBasicMenuItem.addActionListener(parent);
        optionsMenu.add(characterSetBasicMenuItem);

        characterSetExpandedMenuItem = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_EXPANDED], preferences.getCharacterSetCapacity() == CHARACTER_SET_EXPANDED);
        characterSetSizeButtonGroup.add(characterSetExpandedMenuItem);
        characterSetExpandedMenuItem.setActionCommand(Globals.CMD_EXPANDEDCHARSETSIZE);
        characterSetExpandedMenuItem.addActionListener(parent);
        optionsMenu.add(characterSetExpandedMenuItem);

        characterSetSuperMenuItem = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_SUPER], preferences.getCharacterSetCapacity() == CHARACTER_SET_SUPER);
        characterSetSizeButtonGroup.add(characterSetSuperMenuItem);
        characterSetSuperMenuItem.setActionCommand(Globals.CMD_SUPERCHARSETSIZE);
        characterSetSuperMenuItem.addActionListener(parent);
        optionsMenu.add(characterSetSuperMenuItem);

        optionsMenu.addSeparator();

        colorModeButtonGroup = new ButtonGroup();
        graphicsColorModeMenuItem = new JRadioButtonMenuItem(COLOR_MODE_GRAPHICS_1.toString(), dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1);
        colorModeButtonGroup.add(graphicsColorModeMenuItem);
        graphicsColorModeMenuItem.setActionCommand(Globals.CMD_GRAPHICSCOLORMODE);
        graphicsColorModeMenuItem.addActionListener(parent);
        optionsMenu.add(graphicsColorModeMenuItem);
        bitmapColorModeMenuItem = new JRadioButtonMenuItem(COLOR_MODE_BITMAP.toString(), dataSet.getColorMode() == COLOR_MODE_BITMAP);
        colorModeButtonGroup.add(bitmapColorModeMenuItem);
        bitmapColorModeMenuItem.setActionCommand(Globals.CMD_BITMAPCOLORMODE);
        bitmapColorModeMenuItem.addActionListener(parent);
        optionsMenu.add(bitmapColorModeMenuItem);
        ecm2ColorModeMenuItem = new JRadioButtonMenuItem(COLOR_MODE_ECM_2.toString(), dataSet.getColorMode() == COLOR_MODE_ECM_2);
        colorModeButtonGroup.add(ecm2ColorModeMenuItem);
        ecm2ColorModeMenuItem.setActionCommand(Globals.CMD_ECM2COLORMODE);
        ecm2ColorModeMenuItem.addActionListener(parent);
        optionsMenu.add(ecm2ColorModeMenuItem);
        ecm3ColorModeMenuItem = new JRadioButtonMenuItem(COLOR_MODE_ECM_3.toString(), dataSet.getColorMode() == COLOR_MODE_ECM_3);
        colorModeButtonGroup.add(ecm3ColorModeMenuItem);
        ecm3ColorModeMenuItem.setActionCommand(Globals.CMD_ECM3COLORMODE);
        ecm3ColorModeMenuItem.addActionListener(parent);
        optionsMenu.add(ecm3ColorModeMenuItem);

        optionsMenu.addSeparator();

        JMenuItem jmitViewCharLayer = new JCheckBoxMenuItem("View Character Layer", mapEditor.getViewCharLayer());
        jmitViewCharLayer.setActionCommand(Globals.CMD_VIEW_CHAR_LAYER);
        jmitViewCharLayer.addActionListener(parent);
        optionsMenu.add(jmitViewCharLayer);
        JMenuItem jmitViewSpriteLayer = new JCheckBoxMenuItem("View Sprite Layer", mapEditor.getViewSpriteLayer());
        jmitViewSpriteLayer.setActionCommand(Globals.CMD_VIEW_SPRITE_LAYER);
        jmitViewSpriteLayer.addActionListener(parent);
        optionsMenu.add(jmitViewSpriteLayer);

        optionsMenu.addSeparator();

        JMenuItem jmitMagnifySprites = new JCheckBoxMenuItem("Magnify Sprites", mapEditor.getMagnifySprites());
        jmitMagnifySprites.setActionCommand(CMD_MAGNIFY_SPRITES);
        jmitMagnifySprites.addActionListener(parent);
        optionsMenu.add(jmitMagnifySprites);
        JMenuItem jmitSnapSpritesToGrid = new JCheckBoxMenuItem("Snap Sprites to Grid", mapEditor.getSnapSpritesToGrid());
        jmitSnapSpritesToGrid.setActionCommand(CMD_SNAP_SPRITES_TO_GRID);
        jmitSnapSpritesToGrid.addActionListener(parent);
        optionsMenu.add(jmitSnapSpritesToGrid);
        JCheckBoxMenuItem showSpritePerLineMenuItem = new JCheckBoxMenuItem(new ShowSpritesPerLineAction("Show Number of Sprites per Line", parent, mapEditor, dataSet, preferences));
        showSpritePerLineMenuItem.setSelected(mapEditor.getShowSpritesPerLine());
        optionsMenu.add(showSpritePerLineMenuItem);

        // Add menu
        menuBar.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem jmitHelpAbout = new JMenuItem("About Magellan");
        jmitHelpAbout.setActionCommand(Globals.CMD_ABOUT);
        jmitHelpAbout.addActionListener(parent);
        helpMenu.add(jmitHelpAbout);
        // Add menu
        menuBar.add(helpMenu);

        return menuBar;
    }

    public void setColorModeOption(ColorMode colorMode) {
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                colorModeButtonGroup.setSelected(graphicsColorModeMenuItem.getModel(), true);
                break;
            case COLOR_MODE_BITMAP:
                colorModeButtonGroup.setSelected(bitmapColorModeMenuItem.getModel(), true);
                break;
            case COLOR_MODE_ECM_2:
                colorModeButtonGroup.setSelected(ecm2ColorModeMenuItem.getModel(), true);
                break;
            case COLOR_MODE_ECM_3:
                colorModeButtonGroup.setSelected(ecm3ColorModeMenuItem.getModel(), true);
                break;
        }
    }

    public void setSuperCharacterSetOption() {
        characterSetSizeButtonGroup.setSelected(characterSetSuperMenuItem.getModel(), true);
    }

    public JPanel createMainPanel() {

        Insets insets = new Insets(1, 1, 1, 1);
        Insets insets2 = new Insets(4, 0, 0, 0);

        // Create the main panel
        JPanel jpnlMain = getPanel(new BorderLayout());

        // Create the tabbed pane
        editorTabbedPane = new JTabbedPane();

        // Create Character Editor
        JPanel jpnlCharTools = getPanel(new GridBagLayout());

        // Create toolbar on the left side of character editor
        JPanel jpnlToolButtons = getPanel(new GridLayout(7, 1, 0, 2));
        jpnlToolButtons.add(getToolButton(Globals.CMD_FILL_CHR, "Fill"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_CLEAR_CHR, "Clear"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_INVERT_CHR, "Invert Image"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_GRID_CHR, "Toggle Grid"));
        lookButton = getToolButton(Globals.CMD_LOOK, "Look At Character");
        jpnlToolButtons.add(lookButton);
        charUndoButton = getToolButton(Globals.CMD_UNDO_CHR, "Undo Edit");
        charUndoButton.setEnabled(false);
        jpnlToolButtons.add(charUndoButton);
        charRedoButton = getToolButton(Globals.CMD_REDO_CHR, "Redo Edit");
        charRedoButton.setEnabled(false);
        jpnlToolButtons.add(charRedoButton);
        jpnlCharTools.add(jpnlToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create character editor grid and surrounding buttons
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATEL_CHR, "Rotate Left", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        charIntLabel = getLabel();
        charIntLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(charIntLabel, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTU_CHR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        charHexLabel = getLabel();
        charHexLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(charHexLabel, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATER_CHR, "Rotate Right", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTL_CHR, "Shift Left", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTR_CHR, "Shift Right", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPH_CHR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTD_CHR, "Shift Down", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPV_CHR, "Flip Vertical", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        JPanel jpnlChar = getPanel(new BorderLayout());
        charGridCanvas = new GridCanvas(TIGlobals.TI_PALETTE_OPAQUE, 8, 8, 8, parent, parent, dataSet.getColorMode());
        charGridCanvas.setColorScreen(TIGlobals.TI_PALETTE_OPAQUE[mapEditor.getColorScreen()]);
        jpnlChar.add(charGridCanvas, BorderLayout.CENTER);
        Dimension jpnlCharDimension = new Dimension(EDITOR_GRID_SIZE, EDITOR_GRID_SIZE);
        jpnlChar.setPreferredSize(jpnlCharDimension);
        jpnlChar.setMinimumSize(jpnlCharDimension);
        jpnlCharTools.add(jpnlChar, new GridBagConstraints(3, 2, 3, 3, 2, 2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 2, 2));

        // Create character copy/paste tool
        JPanel jpnlCharTool = getPanel(new BorderLayout());
        transparencyCheckBox = new JCheckBox();
        transparencyCheckBox.setOpaque(false);
        transparencyCheckBox.setToolTipText("Toggle Transparency");
        transparencyCheckBox.setVisible(dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3);
        transparencyCheckBox.setActionCommand(Globals.CMD_TRANSPARENCY);
        transparencyCheckBox.addActionListener(parent);
        jpnlToolButtons.add(transparencyCheckBox);
        jpnlCharTool.add(transparencyCheckBox, BorderLayout.WEST);
        charTextField = new JTextField();
        jpnlCharTool.add(charTextField, BorderLayout.CENTER);
        JPanel jpnlCharToolbar = getPanel(new GridLayout(1, 3));
        JButton jbtnUpdateChar = getToolButton(Globals.CMD_UPDATE_CHR, "Set Char");
        jbtnUpdateChar.addActionListener(parent);
        jpnlCharToolbar.add(jbtnUpdateChar);

        Action copyCharAction = new CopyCharAction(getIcon(Globals.CMD_COPY_CHR), charTextField);
        JButton copyButton = getToolButton(copyCharAction, "Copy");
        copyButton.getActionMap().put(Globals.CMD_COPY_CHR, copyCharAction);
        copyButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) copyCharAction.getValue(Action.ACCELERATOR_KEY), Globals.CMD_COPY_CHR);
        jpnlCharToolbar.add(copyButton);

        Action pasteCharAction = new PasteCharAction(getIcon(Globals.CMD_PASTE_CHR), charTextField, jbtnUpdateChar);
        JButton pasteButton = getToolButton(pasteCharAction, "Paste and set");
        pasteButton.getActionMap().put(Globals.CMD_PASTE_CHR, pasteCharAction);
        pasteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) pasteCharAction.getValue(Action.ACCELERATOR_KEY), Globals.CMD_PASTE_CHR);
        jpnlCharToolbar.add(pasteButton);

        jpnlCharTool.add(jpnlCharToolbar, BorderLayout.EAST);
        jpnlCharTools.add(jpnlCharTool, new GridBagConstraints(1, 6, 6, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 2, 2));

        jpnlCharTools.setMinimumSize(jpnlCharTools.getPreferredSize());
        jpnlCharTools.setPreferredSize(jpnlCharTools.getPreferredSize());
        jpnlCharTools.setSize(jpnlCharTools.getPreferredSize());

        // Create character Color Dock
        charColorDockPanel = buildCharColorDock(null);

        // Create Character Dock Buttons
        charButtons = new JButton[(TIGlobals.MAX_CHAR - TIGlobals.MIN_CHAR) + 1];
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int rowNum = ch / 8;
            charButtons[ch] = getDockButton(((ch >= TIGlobals.CHARMAPSTART) && (ch <= TIGlobals.CHARMAPEND) ? "" + TIGlobals.CHARMAP[ch - TIGlobals.CHARMAPSTART] : "?"), Globals.CMD_EDIT_CHR + ch, TIGlobals.TI_PALETTE_OPAQUE[dataSet.getClrSets()[rowNum][Globals.INDEX_CLR_BACK]]);
            charButtons[ch].setForeground(TIGlobals.TI_COLOR_UNUSED);
        }

        // Initialise the border objects for button selection
        Globals.bordButtonNormal = charButtons[0].getBorder();

        // Create Character Dock
        characterDockPanel = buildCharacterDock(null);
        JScrollPane jsclCharacterDock = new JScrollPane(characterDockPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Assemble Character Editor Panel
        JPanel jpnlCharEdit = getPanel(new GridBagLayout());
        jpnlCharEdit.add(jpnlCharTools, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlCharEdit.add(charColorDockPanel, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlCharEdit.add(jsclCharacterDock, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets2, 1, 1));

        // Create Sprite Editor
        JPanel jpnlSpriteTools = getPanel(new GridBagLayout());

        // Create toolbar on the left side of sprite editor
        JPanel jpnlSpriteToolButtons = getPanel(new GridLayout(6, 1, 0, 2));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_FILL_SPR, "Fill"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_CLEAR_SPR, "Clear"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_INVERT_SPR, "Invert Image"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_GRID_SPR, "Toggle Grid"));
        spriteUndoButton = getToolButton(Globals.CMD_UNDO_SPR, "Undo Edit");
        spriteUndoButton.setEnabled(false);
        jpnlSpriteToolButtons.add(spriteUndoButton);
        spriteRedoButton = getToolButton(Globals.CMD_REDO_SPR, "Redo Edit");
        spriteRedoButton.setEnabled(false);
        jpnlSpriteToolButtons.add(spriteRedoButton);
        jpnlSpriteTools.add(jpnlSpriteToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create sprite editor grid and surrounding buttons
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATEL_SPR, "Rotate Left", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        spriteIntLabel = getLabel();
        spriteIntLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(spriteIntLabel, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTU_SPR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        spriteHexLabel = getLabel();
        spriteHexLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(spriteHexLabel, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATER_SPR, "Rotate Right", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTL_SPR, "Shift Left", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTR_SPR, "Shift Right", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPH_SPR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTD_SPR, "Shift Down", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPV_SPR, "Flip Vertical", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        JPanel jpnlSprite = getPanel(new BorderLayout());
        spriteGridCanvas = new GridCanvas(TIGlobals.TI_PALETTE_OPAQUE, 16, 16, 6, parent, parent, dataSet.getColorMode() == COLOR_MODE_BITMAP ? COLOR_MODE_GRAPHICS_1 : dataSet.getColorMode());
        spriteGridCanvas.setECMTransparency(true);
        spriteGridCanvas.setColorScreen(TIGlobals.TI_PALETTE_OPAQUE[mapEditor.getColorScreen()]);
        jpnlSprite.add(spriteGridCanvas, BorderLayout.CENTER);
        Dimension jpnlSpriteDimension = new Dimension(EDITOR_GRID_SIZE, EDITOR_GRID_SIZE);
        jpnlSprite.setPreferredSize(jpnlSpriteDimension);
        jpnlSprite.setMinimumSize(jpnlSpriteDimension);
        jpnlSpriteTools.add(jpnlSprite, new GridBagConstraints(3, 2, 3, 3, 2, 2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 2, 2));

        // Create sprite copy/paste tool
        JPanel jpnlSpriteTool = getPanel(new BorderLayout());
        spriteTextField = new JTextField();
        jpnlSpriteTool.add(spriteTextField, BorderLayout.CENTER);
        JPanel jpnlSpriteToolbar = getPanel(new GridLayout(1, 3));
        JButton jbtnUpdateSprite = getToolButton(Globals.CMD_UPDATE_SPR, "Set Sprite");
        jbtnUpdateSprite.addActionListener(parent);
        jpnlSpriteToolbar.add(jbtnUpdateSprite);

        Action copySpriteAction = new CopySpriteAction(getIcon(Globals.CMD_COPY_SPR), spriteTextField);
        JButton copySpriteButton = getToolButton(copySpriteAction, "Copy");
        copySpriteButton.getActionMap().put(CMD_COPY_SPR, copySpriteAction);
        copySpriteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) copySpriteAction.getValue(Action.ACCELERATOR_KEY), Globals.CMD_COPY_SPR);
        jpnlSpriteToolbar.add(copySpriteButton);

        Action pasteSpriteAction = new PasteSpriteAction(getIcon(Globals.CMD_PASTE_SPR), spriteTextField, jbtnUpdateSprite);
        JButton pasteSpriteButton = getToolButton(pasteSpriteAction, "Paste and set");
        pasteSpriteButton.getActionMap().put(CMD_PASTE_SPR, pasteSpriteAction);
        pasteSpriteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) pasteSpriteAction.getValue(Action.ACCELERATOR_KEY), CMD_PASTE_SPR);
        jpnlSpriteToolbar.add(pasteSpriteButton);

        jpnlSpriteTool.add(jpnlSpriteToolbar, BorderLayout.EAST);
        jpnlSpriteTools.add(jpnlSpriteTool, new GridBagConstraints(1, 6, 6, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 2, 2));

        jpnlSpriteTools.setMinimumSize(jpnlSpriteTools.getPreferredSize());
        jpnlSpriteTools.setPreferredSize(jpnlSpriteTools.getPreferredSize());
        jpnlSpriteTools.setSize(jpnlSpriteTools.getPreferredSize());

        // Create sprite Color Dock
        spriteColorDockPanel = buildSpriteColorDock(null);

        // Create Sprite Dock Buttons
        spriteButtons = new JButton[TIGlobals.MAX_SPRITE + 1];
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            spriteButtons[i] = getDockButton(Integer.toString(i), Globals.CMD_EDIT_SPR + i, TIGlobals.TI_PALETTE_OPAQUE[0], Globals.DM_SPRITE);
            spriteButtons[i].setForeground(TIGlobals.TI_COLOR_UNUSED);
        }

        // Create Sprite Dock
        spriteDockPanel = buildSpriteDock(null);
        JScrollPane jsclSpriteDock = new JScrollPane(spriteDockPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Assemble Sprite Editor Panel
        JPanel jpnlSpriteEdit = getPanel(new GridBagLayout());
        jpnlSpriteEdit.add(jpnlSpriteTools, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlSpriteEdit.add(spriteColorDockPanel, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlSpriteEdit.add(jsclSpriteDock, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets2, 1, 1));

        // Tabs
        editorTabbedPane.add("Characters", jpnlCharEdit);
        editorTabbedPane.add("Sprites", jpnlSpriteEdit);

        // Main
        jpnlMain.add(mapEditor, BorderLayout.CENTER);
        jpnlMain.add(editorTabbedPane, BorderLayout.WEST);

        return jpnlMain;
    }

    public void buildColorDocks() {
        charColorDockPanel = buildCharColorDock(charColorDockPanel);
        spriteColorDockPanel = buildSpriteColorDock(spriteColorDockPanel);
    }

    protected JPanel buildCharColorDock(JPanel jPanel) {
        if (dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1 || dataSet.getColorMode() == COLOR_MODE_BITMAP) {
            if (jPanel == null) {
                jPanel = getPanel(new GridLayout(2, 8, 0, 0));
            }
            else {
                jPanel.removeAll();
                jPanel.setLayout(new GridLayout(2, 8, 0, 0));
            }
            charColorDockButtons = new DualClickButton[16];
            for (int cd = 0; cd < 16; cd++) {
                DualClickButton dbtnColorButton = getPaletteButton(Globals.CMD_CLRFORE_CHR + cd, Globals.CMD_CLRBACK_CHR + cd, TIGlobals.TI_PALETTE_OPAQUE[cd]);
                if (cd == charGridCanvas.getColorBack()) {
                    dbtnColorButton.setText("B");
                }
                if (cd == charGridCanvas.getColorDraw()) {
                    dbtnColorButton.setText("F");
                }
                jPanel.add(dbtnColorButton);
                charColorDockButtons[cd] = dbtnColorButton;
            }
        }
        else {
            if (jPanel == null) {
                jPanel = getPanel(new BorderLayout());
            }
            else {
                jPanel.removeAll();
                jPanel.setLayout(new BorderLayout());
            }
            charECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), charGridCanvas.getColorDraw(), charGridCanvas.getColorBack(), parent, true);
            jPanel.add(charECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
    }

    protected JPanel buildSpriteColorDock(JPanel jPanel) {
        if (dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1 || dataSet.getColorMode() == COLOR_MODE_BITMAP) {
            if (jPanel == null) {
                jPanel = getPanel(new GridLayout(2, 8, 0, 0));
            }
            else {
                jPanel.removeAll();
                jPanel.setLayout(new GridLayout(2, 8, 0, 0));
            }
            spriteColorDockButtons = new DualClickButton[16];
            for (int cd = 0; cd < 16; cd++) {
                DualClickButton dbtnColorButton = getPaletteButton(Globals.CMD_CLRFORE_SPR + cd, Globals.CMD_CLRBACK_SPR + cd, TIGlobals.TI_PALETTE_OPAQUE[cd]);
                if (cd == spriteGridCanvas.getColorBack()) {
                    dbtnColorButton.setText("B");
                }
                if (cd == spriteGridCanvas.getColorDraw()) {
                    dbtnColorButton.setText("F");
                }
                jPanel.add(dbtnColorButton);
                spriteColorDockButtons[cd] = dbtnColorButton;
            }
        }
        else {
            if (jPanel == null) {
                jPanel = getPanel(new BorderLayout());
            }
            else {
                jPanel.removeAll();
                jPanel.setLayout(new BorderLayout());
            }
            spriteECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), spriteGridCanvas.getColorDraw(), spriteGridCanvas.getColorBack(), parent, false);
            jPanel.add(spriteECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
    }

    public void buildDocks() {
        characterDockPanel = buildCharacterDock(characterDockPanel);
        spriteDockPanel = buildSpriteDock(spriteDockPanel);
    }

    protected JPanel buildCharacterDock(JPanel jPanel) {
        int dockFontRows = preferences.getCharacterSetSize() / FONT_COLS;
        if (jPanel != null) {
            jPanel.removeAll();
            jPanel.setLayout(new GridLayout(dockFontRows, FONT_COLS + 1));
        } else {
            jPanel = getPanel(new GridLayout(dockFontRows, FONT_COLS + 1));
        }
        int col = FONT_COLS;
        int ccount = 1;
        int lcount = 1;
        int ucount = 1;
        for (int c = TIGlobals.MIN_CHAR; c <= MAX_CHAR; c++) {
            if (c < TIGlobals.BASIC_FIRST_CHAR) {
                if (preferences.getCharacterSetCapacity() >= CHARACTER_SET_EXPANDED) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel("L" + lcount + " ", JLabel.RIGHT, CLR_CHARS_LOWER));
                        lcount++;
                        col = 0;
                    }
                    jPanel.add(charButtons[c]);
                    col++;
                }
            } else if (c <= TIGlobals.BASIC_LAST_CHAR) {
                if (col >= 8) {
                    if (c > TIGlobals.FINALXBCHAR) {
                        jPanel.add(getLabel(ccount + " ", JLabel.RIGHT, CLR_CHARS_BASE2));
                    } else {
                        jPanel.add(getLabel(ccount + " ", JLabel.RIGHT, CLR_CHARS_BASE1));
                    }
                    ccount++;
                    col = 0;
                }
                jPanel.add(charButtons[c]);
                col++;
            } else if (c <= EXP_LAST_CHAR) {
                if (preferences.getCharacterSetCapacity() >= CHARACTER_SET_EXPANDED) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel("U" + ucount + " ", JLabel.RIGHT, CLR_CHARS_UPPER));
                        ucount++;
                        col = 0;
                    }
                    jPanel.add(charButtons[c]);
                    col++;
                }
            } else {
                if (preferences.getCharacterSetCapacity() >= CHARACTER_SET_SUPER) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel((ucount < 100 ? "U" : "") + ucount + " ", JLabel.RIGHT, CLR_CHARS_UPPER));
                        ucount++;
                        col = 0;
                    }
                    jPanel.add(charButtons[c]);
                    col++;
                }
            }
        }
        jPanel.revalidate();
        return jPanel;
    }

    protected JPanel buildSpriteDock(JPanel jPanel) {
        int dockSpriteRows = preferences.getSpriteSetSize() / SPRITE_COLS;
        if (jPanel != null) {
            jPanel.removeAll();
            jPanel.setLayout(new GridLayout(dockSpriteRows , 4));
        } else {
            jPanel = getPanel(new GridLayout(dockSpriteRows, 4));
        }
        for (int i = 0; i < preferences.getSpriteSetSize(); i++) {
            jPanel.add(spriteButtons[i]);
        }
        return jPanel;
    }

    protected JPanel getPanel(LayoutManager layout) {
        JPanel jpnlRtn = new JPanel(layout);
        jpnlRtn.setOpaque(true);
        jpnlRtn.setBackground(Globals.CLR_COMPONENTBACK);
        return jpnlRtn;
    }

    protected JButton getToolButton(Action action, String tooltip) {
        JButton jbtnTool = getToolButton(null, CLR_BUTTON_NORMAL);
        jbtnTool.setAction(action);
        jbtnTool.setToolTipText(tooltip);
        return jbtnTool;
    }

    protected JButton getToolButton(String buttonKey, String tooltip) {
        return getToolButton(buttonKey, tooltip, Globals.CLR_BUTTON_NORMAL);
    }

    protected JButton getToolButton(String buttonKey, String tooltip, Color bgcolor) {
        JButton jbtnTool = getToolButton(getIcon(buttonKey), bgcolor);
        jbtnTool.setToolTipText(tooltip);
        jbtnTool.setActionCommand(buttonKey);
        jbtnTool.addActionListener(parent);
        return jbtnTool;
    }

    private JButton getToolButton(ImageIcon imageIcon, Color bgcolor) {
        JButton jbtnTool = new JButton(imageIcon);
        jbtnTool.setMargin(new Insets(0, 0, 0, 0));
        jbtnTool.setBackground(bgcolor);
        jbtnTool.setPreferredSize(Globals.DM_TOOL);
        return jbtnTool;
    }

    protected JButton getDockButton(String buttonlabel, String actcmd, Color bgcolor) {
        return getDockButton(buttonlabel, actcmd, bgcolor, Globals.DM_TOOL);
    }

    protected JButton getDockButton(String buttonlabel, String actcmd, Color bgcolor, Dimension size) {
        JButton jbtnDock = new JButton(buttonlabel);
        jbtnDock.setActionCommand(actcmd);
        jbtnDock.addActionListener(parent);
        jbtnDock.setOpaque(true);
        jbtnDock.setBackground(bgcolor);
        jbtnDock.setMargin(new Insets(0, 0, 0, 0));
        jbtnDock.setPreferredSize(size);
        return jbtnDock;
    }

    protected DualClickButton getPaletteButton(String forecmd, String backcmd, Color bgcolor) {
        DualClickButton dbtnPal = new DualClickButton("", forecmd, backcmd, parent);
        dbtnPal.addActionListener(parent);
        dbtnPal.setOpaque(true);
        dbtnPal.setBackground(bgcolor);
        dbtnPal.setMargin(new Insets(0, 0, 0, 0));
        dbtnPal.setPreferredSize(Globals.DM_TOOL);
        dbtnPal.setFocusable(false);
        return dbtnPal;
    }

    protected ImageIcon getIcon(String name) {
        if (name.endsWith("spr") || name.endsWith("chr")) {
            name = name.substring(0, name.length() - 3);
        }
        URL imageURL = Magellan.class.getResource("images/icon_" + name + "_mono.png");
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(imageURL));
    }

    protected JLabel getLabel() {
        return getLabel("", SwingConstants.CENTER, Globals.CLR_COMPONENTBACK);
    }

    protected JLabel getLabel(String text, int align, Color clrback) {
        JLabel jlblRtn = new JLabel(text, align);
        jlblRtn.setOpaque(true);
        jlblRtn.setBackground(clrback);
        return jlblRtn;
    }

    public void setECMPaletteColor(ECMPalette ecmPalette, int index, Color color, Color screenColor) {
        charECMPaletteComboBox.setEditable(false);
        spriteECMPaletteComboBox.setEditable(false);
        ecmPalette.setColor(index, color);
        charECMPaletteComboBox.setEditable(true);
        spriteECMPaletteComboBox.setEditable(true);
        charGridCanvas.setPalette(ecmPalette.getColors());
        charGridCanvas.setColorScreen(screenColor);
        charGridCanvas.redrawCanvas();
        spriteGridCanvas.setPalette(ecmPalette.getColors());
        spriteGridCanvas.setColorScreen(screenColor);
        spriteGridCanvas.redrawCanvas();
    }

    public void updatePalettes(Color screenColor) {
        ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
        updateCharPaletteCombo(-1);
        updateSpritePaletteCombo(-1);
        charGridCanvas.setPalette(ecmPalettes[charECMPaletteComboBox.getSelectedIndex()].getColors());
        charGridCanvas.setColorScreen(screenColor);
        charGridCanvas.redrawCanvas();
        spriteGridCanvas.setPalette(ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()].getColors());
        spriteGridCanvas.setColorScreen(screenColor);
        spriteGridCanvas.redrawCanvas();
    }

    public void updateCharPaletteCombo(int activeChar) {
        charECMPaletteComboBox.setEditable(false);
        if (activeChar != -1) {
            charECMPaletteComboBox.setSelectedItem(dataSet.getEcmCharPalettes()[activeChar]);
        }
        charECMPaletteComboBox.setEditable(true);
    }

    public void updateSpritePaletteCombo(int activeSprite) {
        spriteECMPaletteComboBox.setEditable(false);
        if (activeSprite != -1) {
            spriteECMPaletteComboBox.setSelectedItem(dataSet.getEcmSpritePalettes()[activeSprite]);
        }
        spriteECMPaletteComboBox.setEditable(true);
    }
}
