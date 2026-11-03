package com.dreamcodex.ti.ui;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.app.*;
import com.dreamcodex.ti.actions.character.*;
import com.dreamcodex.ti.actions.clipboard.CopyCharAction;
import com.dreamcodex.ti.actions.clipboard.CopySpriteAction;
import com.dreamcodex.ti.actions.clipboard.PasteCharAction;
import com.dreamcodex.ti.actions.clipboard.PasteSpriteAction;
import com.dreamcodex.ti.actions.exporting.*;
import com.dreamcodex.ti.actions.importing.*;
import com.dreamcodex.ti.actions.metatile.*;
import com.dreamcodex.ti.actions.options.*;
import com.dreamcodex.ti.actions.sprite.*;
import com.dreamcodex.ti.actions.tools.*;
import com.dreamcodex.ti.component.*;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    private static final int[] METATILE_SIZES = {0, 2, 4};
    private ButtonGroup metatileModeButtonGroup;
    private final JMenuItem[] metatileModeMenuItems = new JMenuItem[METATILE_SIZES.length];
    private JMenuItem[] charMapMenuItems = new JMenuItem[0];

    // Character editor
    private GridCanvas charGridCanvas;
    private JTextField charTextField;
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

    // Metatile editor
    private JPanel metatileEditorPanel;
    private MetatileCanvas metatileCanvas;
    private JLabel metatileIntLabel;
    private JLabel metatileHexLabel;
    private CharacterPicker characterPicker;
    private JPanel metatileDockPanel;
    private JButton[] metatileButtons = new JButton[0];

    // Tabbed pane
    private JTabbedPane editorTabbedPane;
    private JPanel spriteEditorPanel;

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

    public MetatileCanvas getMetatileCanvas() {
        return metatileCanvas;
    }

    public JLabel getMetatileIntLabel() {
        return metatileIntLabel;
    }

    public JLabel getMetatileHexLabel() {
        return metatileHexLabel;
    }

    public CharacterPicker getCharacterPicker() {
        return characterPicker;
    }

    public JButton[] getMetatileButtons() {
        return metatileButtons;
    }

    public JTabbedPane getEditorTabbedPane() {
        return editorTabbedPane;
    }

    // Identify tabs by component, so inserting tabs doesn't break the checks
    public boolean isSpriteTabSelected() {
        return editorTabbedPane.getSelectedComponent() == spriteEditorPanel;
    }

    public JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem jmitNew = new JMenuItem();
        jmitNew.setAction(new NewProjectAction("New Map Project", parent, mapEditor, dataSet, preferences));
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
        JMenuItem jmitExit = new JMenuItem();
        jmitExit.setAction(new ExitAction("Exit", parent, mapEditor, dataSet, preferences));
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
        JMenuItem jmitExportCVB = new JMenuItem();
        jmitExportCVB.setAction(new ExportCVBasicDataFileAction("CVBasic Data", parent, mapEditor, dataSet, preferences));
        exportMenu.add(jmitExportCVB);
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
        JMenuItem jmitSwapChars = new JMenuItem();
        jmitSwapChars.setAction(new SwapCharactersAction("Replace Characters", parent, mapEditor, dataSet, preferences));
        toolsMenu.add(jmitSwapChars);
        toolsMenu.addSeparator();
        JMenuItem jmitAnalyzeCharUsage = new JMenuItem();
        jmitAnalyzeCharUsage.setAction(new AnalyzeCharUsageAction("Analyze Character Usage", parent, mapEditor, dataSet, preferences));
        toolsMenu.add(jmitAnalyzeCharUsage);
        JMenuItem jmitAnalyzeCharTrans = new JMenuItem();
        jmitAnalyzeCharTrans.setAction(new AnalyzeCharTransAction("Analyze Character Transitions", parent, mapEditor, dataSet, preferences));
        toolsMenu.add(jmitAnalyzeCharTrans);
        toolsMenu.addSeparator();
        toolsMenu.add(new LoadOverlayImageAction("Load Overlay Image", parent, mapEditor, dataSet, preferences));
        toolsMenu.add(new ClearOverlayImageAction("Clear Overlay Image", parent, mapEditor, dataSet, preferences));
        // Add menu
        menuBar.add(toolsMenu);

        JMenu optionsMenu = new JMenu("Options");
        JMenuItem jmitShowPos = new JCheckBoxMenuItem(new ToggleShowPositionAction("Show Position", parent, mapEditor, dataSet, preferences));
        jmitShowPos.setSelected(mapEditor.showPosIndic());
        optionsMenu.add(jmitShowPos);
        JMenuItem jmitBase0Pos = new JCheckBoxMenuItem(new ToggleBase0PositionAction("Base 0 for Position", parent, mapEditor, dataSet, preferences));
        jmitBase0Pos.setSelected(mapEditor.base0Position());
        optionsMenu.add(jmitBase0Pos);

        optionsMenu.addSeparator();

        characterSetSizeButtonGroup = new ButtonGroup();

        characterSetBasicMenuItem = new JRadioButtonMenuItem(new SetCharacterSetSizeAction(CHARACTER_SET_BASIC, CHARACTER_SET_SIZES[CHARACTER_SET_BASIC], parent, mapEditor, dataSet, preferences));
        characterSetBasicMenuItem.setSelected(preferences.getCharacterSetCapacity() == CHARACTER_SET_BASIC);
        characterSetSizeButtonGroup.add(characterSetBasicMenuItem);
        optionsMenu.add(characterSetBasicMenuItem);

        characterSetExpandedMenuItem = new JRadioButtonMenuItem(new SetCharacterSetSizeAction(CHARACTER_SET_EXPANDED, CHARACTER_SET_SIZES[CHARACTER_SET_EXPANDED], parent, mapEditor, dataSet, preferences));
        characterSetExpandedMenuItem.setSelected(preferences.getCharacterSetCapacity() == CHARACTER_SET_EXPANDED);
        characterSetSizeButtonGroup.add(characterSetExpandedMenuItem);
        optionsMenu.add(characterSetExpandedMenuItem);

        characterSetSuperMenuItem = new JRadioButtonMenuItem(new SetCharacterSetSizeAction(CHARACTER_SET_SUPER, CHARACTER_SET_SIZES[CHARACTER_SET_SUPER], parent, mapEditor, dataSet, preferences));
        characterSetSuperMenuItem.setSelected(preferences.getCharacterSetCapacity() == CHARACTER_SET_SUPER);
        characterSetSizeButtonGroup.add(characterSetSuperMenuItem);
        optionsMenu.add(characterSetSuperMenuItem);

        optionsMenu.addSeparator();

        metatileModeButtonGroup = new ButtonGroup();
        for (int i = 0; i < METATILE_SIZES.length; i++) {
            int size = METATILE_SIZES[i];
            String name = size == 0 ? "No Metatiles" : size + "x" + size + " Metatiles";
            metatileModeMenuItems[i] = new JRadioButtonMenuItem(new SetMetatileModeAction(size, name, parent, mapEditor, dataSet, preferences));
            metatileModeMenuItems[i].setSelected(dataSet.getMetatileSize() == size);
            metatileModeButtonGroup.add(metatileModeMenuItems[i]);
            optionsMenu.add(metatileModeMenuItems[i]);
        }

        optionsMenu.addSeparator();

        colorModeButtonGroup = new ButtonGroup();
        graphicsColorModeMenuItem = new JRadioButtonMenuItem(new SetColorModeAction(COLOR_MODE_GRAPHICS_1, COLOR_MODE_GRAPHICS_1.toString(), parent, mapEditor, dataSet, preferences));
        graphicsColorModeMenuItem.setSelected(dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1);
        colorModeButtonGroup.add(graphicsColorModeMenuItem);
        optionsMenu.add(graphicsColorModeMenuItem);
        bitmapColorModeMenuItem = new JRadioButtonMenuItem(new SetColorModeAction(COLOR_MODE_BITMAP, COLOR_MODE_BITMAP.toString(), parent, mapEditor, dataSet, preferences));
        bitmapColorModeMenuItem.setSelected(dataSet.getColorMode() == COLOR_MODE_BITMAP);
        colorModeButtonGroup.add(bitmapColorModeMenuItem);
        optionsMenu.add(bitmapColorModeMenuItem);
        ecm2ColorModeMenuItem = new JRadioButtonMenuItem(new SetColorModeAction(COLOR_MODE_ECM_2, COLOR_MODE_ECM_2.toString(), parent, mapEditor, dataSet, preferences));
        ecm2ColorModeMenuItem.setSelected(dataSet.getColorMode() == COLOR_MODE_ECM_2);
        colorModeButtonGroup.add(ecm2ColorModeMenuItem);
        optionsMenu.add(ecm2ColorModeMenuItem);
        ecm3ColorModeMenuItem = new JRadioButtonMenuItem(new SetColorModeAction(COLOR_MODE_ECM_3, COLOR_MODE_ECM_3.toString(), parent, mapEditor, dataSet, preferences));
        ecm3ColorModeMenuItem.setSelected(dataSet.getColorMode() == COLOR_MODE_ECM_3);
        colorModeButtonGroup.add(ecm3ColorModeMenuItem);
        optionsMenu.add(ecm3ColorModeMenuItem);

        optionsMenu.addSeparator();

        JMenuItem jmitViewCharLayer = new JCheckBoxMenuItem(new ToggleViewCharLayerAction("View Character Layer", parent, mapEditor, dataSet, preferences));
        jmitViewCharLayer.setSelected(mapEditor.getViewCharLayer());
        optionsMenu.add(jmitViewCharLayer);
        JMenuItem jmitViewSpriteLayer = new JCheckBoxMenuItem(new ToggleViewSpriteLayerAction("View Sprite Layer", parent, mapEditor, dataSet, preferences));
        jmitViewSpriteLayer.setSelected(mapEditor.getViewSpriteLayer());
        optionsMenu.add(jmitViewSpriteLayer);

        optionsMenu.addSeparator();

        JMenuItem jmitMagnifySprites = new JCheckBoxMenuItem(new ToggleMagnifySpritesAction("Magnify Sprites", parent, mapEditor, dataSet, preferences));
        jmitMagnifySprites.setSelected(mapEditor.getMagnifySprites());
        optionsMenu.add(jmitMagnifySprites);
        JMenuItem jmitSnapSpritesToGrid = new JCheckBoxMenuItem(new ToggleSnapSpritesToGridAction("Snap Sprites to Grid", parent, mapEditor, dataSet, preferences));
        jmitSnapSpritesToGrid.setSelected(mapEditor.getSnapSpritesToGrid());
        optionsMenu.add(jmitSnapSpritesToGrid);
        JCheckBoxMenuItem showSpritePerLineMenuItem = new JCheckBoxMenuItem(new ShowSpritesPerLineAction("Show Number of Sprites per Line", parent, mapEditor, dataSet, preferences));
        showSpritePerLineMenuItem.setSelected(mapEditor.getShowSpritesPerLine());
        optionsMenu.add(showSpritePerLineMenuItem);

        // Add menu
        menuBar.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem jmitHelpAbout = new JMenuItem();
        jmitHelpAbout.setAction(new AboutAction("About Magellan", parent, mapEditor, dataSet, preferences));
        helpMenu.add(jmitHelpAbout);
        // Add menu
        menuBar.add(helpMenu);

        // These read or write maps as character numbers, so they are disabled in metatile mode
        // until they support metatiles
        charMapMenuItems = new JMenuItem[] {
            jmitAppend,
            jmitImportVramDump, jmitImportBinaryMap, jmitImportMapImage,
            jmitExportData, jmitExportBasic, jmitExportExec, jmitExportXB256, jmitExportXBDisMer,
            jmitExportScrollMap, jmitExportBin, jmitExportBinMap,
            jmitAnalyzeCharUsage, jmitAnalyzeCharTrans
        };

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

    public void setMetatileModeOption(int size) {
        for (int i = 0; i < METATILE_SIZES.length; i++) {
            if (METATILE_SIZES[i] == size) {
                metatileModeButtonGroup.setSelected(metatileModeMenuItems[i].getModel(), true);
            }
        }
    }

    /**
     * Shows or hides the Metatiles tab and enables the menu items that only work with character maps.
     * Size is 0 for no metatiles, otherwise the metatile width/height in characters.
     */
    public void setMetatileMode(int size) {
        boolean enabled = size != 0;
        for (JMenuItem menuItem : charMapMenuItems) {
            menuItem.setEnabled(!enabled);
        }
        setMetatileModeOption(size);
        int tabIndex = editorTabbedPane.indexOfComponent(metatileEditorPanel);
        if (enabled) {
            buildMetatileDock(size);
            if (tabIndex < 0) {
                editorTabbedPane.insertTab("Metatiles", null, metatileEditorPanel, null, 1);
            }
            editorTabbedPane.setSelectedComponent(metatileEditorPanel);
        }
        else if (tabIndex >= 0) {
            // Removing the selected tab would leave the Sprites tab selected, so go to Characters first
            if (editorTabbedPane.getSelectedIndex() == tabIndex) {
                editorTabbedPane.setSelectedIndex(0);
            }
            editorTabbedPane.remove(tabIndex);
        }
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
        jpnlToolButtons.add(getToolButton(Globals.CMD_FILL_CHR, "Fill", new FillCharAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(Globals.CMD_CLEAR_CHR, "Clear", new ClearCharAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(Globals.CMD_INVERT_CHR, "Invert Image", new InvertCharAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(CMD_PROPERTIES_CHR, "Character Properties", new CharacterPropertiesAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(Globals.CMD_GRID_CHR, "Toggle Grid", new ToggleGridCharAction("", parent, mapEditor, dataSet, preferences)));
        charUndoButton = getToolButton(Globals.CMD_UNDO_CHR, "Undo Edit", new UndoCharAction("", parent, mapEditor, dataSet, preferences));
        charUndoButton.setEnabled(false);
        jpnlToolButtons.add(charUndoButton);
        charRedoButton = getToolButton(Globals.CMD_REDO_CHR, "Redo Edit", new RedoCharAction("", parent, mapEditor, dataSet, preferences));
        charRedoButton.setEnabled(false);
        jpnlToolButtons.add(charRedoButton);
        jpnlCharTools.add(jpnlToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create character editor grid and surrounding buttons
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATEL_CHR, "Rotate Left", Globals.CLR_BUTTON_TRANS, new RotateLeftCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        charIntLabel = getLabel();
        charIntLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(charIntLabel, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTU_CHR, "Shift Up", Globals.CLR_BUTTON_SHIFT, new ShiftUpCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        charHexLabel = getLabel();
        charHexLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(charHexLabel, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATER_CHR, "Rotate Right", Globals.CLR_BUTTON_TRANS, new RotateRightCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTL_CHR, "Shift Left", Globals.CLR_BUTTON_SHIFT, new ShiftLeftCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTR_CHR, "Shift Right", Globals.CLR_BUTTON_SHIFT, new ShiftRightCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPH_CHR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS, new FlipHorizontalCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTD_CHR, "Shift Down", Globals.CLR_BUTTON_SHIFT, new ShiftDownCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPV_CHR, "Flip Vertical", Globals.CLR_BUTTON_TRANS, new FlipVerticalCharAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
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
        transparencyCheckBox.addActionListener(new TransparencyAction("", parent, mapEditor, dataSet, preferences));
        jpnlToolButtons.add(transparencyCheckBox);
        jpnlCharTool.add(transparencyCheckBox, BorderLayout.WEST);
        charTextField = new JTextField();
        jpnlCharTool.add(charTextField, BorderLayout.CENTER);
        JPanel jpnlCharToolbar = getPanel(new GridLayout(1, 3));
        JButton jbtnUpdateChar = getToolButton(Globals.CMD_UPDATE_CHR, "Set Char", new UpdateCharAction("", parent, mapEditor, dataSet, preferences));
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
            charButtons[ch] = getDockButton(((ch >= TIGlobals.CHARMAPSTART) && (ch <= TIGlobals.CHARMAPEND) ? "" + TIGlobals.CHARMAP[ch - TIGlobals.CHARMAPSTART] : "?"), TIGlobals.TI_PALETTE_OPAQUE[dataSet.getClrSets()[rowNum][Globals.INDEX_CLR_BACK]], Globals.DM_TOOL, new EditCharAction(ch, "", parent, mapEditor, dataSet, preferences));
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
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_FILL_SPR, "Fill", new FillSpriteAction("", parent, mapEditor, dataSet, preferences)));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_CLEAR_SPR, "Clear", new ClearSpriteAction("", parent, mapEditor, dataSet, preferences)));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_INVERT_SPR, "Invert Image", new InvertSpriteAction("", parent, mapEditor, dataSet, preferences)));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_GRID_SPR, "Toggle Grid", new ToggleGridSpriteAction("", parent, mapEditor, dataSet, preferences)));
        spriteUndoButton = getToolButton(Globals.CMD_UNDO_SPR, "Undo Edit", new UndoSpriteAction("", parent, mapEditor, dataSet, preferences));
        spriteUndoButton.setEnabled(false);
        jpnlSpriteToolButtons.add(spriteUndoButton);
        spriteRedoButton = getToolButton(Globals.CMD_REDO_SPR, "Redo Edit", new RedoSpriteAction("", parent, mapEditor, dataSet, preferences));
        spriteRedoButton.setEnabled(false);
        jpnlSpriteToolButtons.add(spriteRedoButton);
        jpnlSpriteTools.add(jpnlSpriteToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create sprite editor grid and surrounding buttons
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATEL_SPR, "Rotate Left", Globals.CLR_BUTTON_TRANS, new RotateLeftSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        spriteIntLabel = getLabel();
        spriteIntLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(spriteIntLabel, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTU_SPR, "Shift Up", Globals.CLR_BUTTON_SHIFT, new ShiftUpSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        spriteHexLabel = getLabel();
        spriteHexLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(spriteHexLabel, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATER_SPR, "Rotate Right", Globals.CLR_BUTTON_TRANS, new RotateRightSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTL_SPR, "Shift Left", Globals.CLR_BUTTON_SHIFT, new ShiftLeftSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTR_SPR, "Shift Right", Globals.CLR_BUTTON_SHIFT, new ShiftRightSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPH_SPR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS, new FlipHorizontalSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTD_SPR, "Shift Down", Globals.CLR_BUTTON_SHIFT, new ShiftDownSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPV_SPR, "Flip Vertical", Globals.CLR_BUTTON_TRANS, new FlipVerticalSpriteAction("", parent, mapEditor, dataSet, preferences)), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
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
        JButton jbtnUpdateSprite = getToolButton(Globals.CMD_UPDATE_SPR, "Set Sprite", new UpdateSpriteAction("", parent, mapEditor, dataSet, preferences));
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
            spriteButtons[i] = getDockButton(Integer.toString(i), TIGlobals.TI_PALETTE_OPAQUE[0], Globals.DM_SPRITE, new EditSpriteAction(i, "", parent, mapEditor, dataSet, preferences));
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

        // Metatile editor, only shown as a tab in metatile mode
        metatileEditorPanel = createMetatileEditorPanel(insets, insets2);

        // Tabs
        spriteEditorPanel = jpnlSpriteEdit;
        editorTabbedPane.add("Characters", jpnlCharEdit);
        editorTabbedPane.add("Sprites", jpnlSpriteEdit);

        // Main
        jpnlMain.add(mapEditor, BorderLayout.CENTER);
        jpnlMain.add(editorTabbedPane, BorderLayout.WEST);

        return jpnlMain;
    }

    private JPanel createMetatileEditorPanel(Insets insets, Insets insets2) {
        JPanel jpnlMetatileTools = getPanel(new GridBagLayout());

        // Toolbar on the left side of the metatile editor
        JPanel jpnlToolButtons = getPanel(new GridLayout(3, 1, 0, 2));
        jpnlToolButtons.add(getToolButton(Globals.CMD_FILL_CHR, "Fill with active character", new FillMetatileAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(Globals.CMD_CLEAR_CHR, "Clear", new ClearMetatileAction("", parent, mapEditor, dataSet, preferences)));
        jpnlToolButtons.add(getToolButton(Globals.CMD_GRID_CHR, "Toggle Grid", new ToggleGridMetatileAction("", parent, mapEditor, dataSet, preferences)));
        jpnlMetatileTools.add(jpnlToolButtons, new GridBagConstraints(1, 1, 1, 2, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));

        // Metatile number
        JPanel jpnlLabels = getPanel(new FlowLayout(FlowLayout.CENTER));
        metatileIntLabel = getLabel();
        metatileIntLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlLabels.add(metatileIntLabel);
        metatileHexLabel = getLabel();
        metatileHexLabel.setPreferredSize(Globals.DM_TEXT);
        jpnlLabels.add(metatileHexLabel);
        jpnlMetatileTools.add(jpnlLabels, new GridBagConstraints(2, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets, 2, 2));

        // Editor: left click sets a cell to the active character, right click picks the character from a cell
        metatileCanvas = new MetatileCanvas(dataSet.getCharImages(), (x, y, e) -> {
            if (SwingUtilities.isRightMouseButton(e)) {
                int charNum = dataSet.getMetatiles().get(parent.getActiveMetatile())[y][x];
                new EditCharAction(charNum, "", parent, mapEditor, dataSet, preferences).actionPerformed(new ActionEvent(metatileCanvas, ActionEvent.ACTION_PERFORMED, ""));
            }
            else {
                parent.setMetatileCell(x, y, parent.getActiveChar());
            }
        });
        metatileCanvas.setToolTipText("Left click: set character, right click: pick character");
        Dimension editorDimension = new Dimension(EDITOR_GRID_SIZE, EDITOR_GRID_SIZE);
        metatileCanvas.setPreferredSize(editorDimension);
        metatileCanvas.setMinimumSize(editorDimension);
        jpnlMetatileTools.add(metatileCanvas, new GridBagConstraints(2, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));

        // Character picker, in place of the color dock, drawn like the character dock
        characterPicker = new CharacterPicker(charButtons, this::getCharacterRowLabel, preferences.getCharacterSetStart(), preferences.getCharacterSetEnd(), (charNum, e) ->
            new EditCharAction(charNum, "", parent, mapEditor, dataSet, preferences).actionPerformed(new ActionEvent(characterPicker, ActionEvent.ACTION_PERFORMED, ""))
        );
        JScrollPane jsclCharacterPicker = new JScrollPane(characterPicker, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsclCharacterPicker.getVerticalScrollBar().setUnitIncrement(CharacterPicker.CELL_HEIGHT);
        // Width comes from the layout; show 6 rows
        Dimension pickerDimension = new Dimension(characterPicker.getPreferredSize().width, 6 * CharacterPicker.CELL_HEIGHT + 4);
        jsclCharacterPicker.setPreferredSize(pickerDimension);
        jsclCharacterPicker.setMinimumSize(pickerDimension);

        // Metatile dock, filled when metatile mode is switched on
        metatileDockPanel = getPanel(new GridLayout(1, 1));
        JScrollPane jsclMetatileDock = new JScrollPane(metatileDockPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsclMetatileDock.getVerticalScrollBar().setUnitIncrement(Globals.DM_TOOL.height);

        // Assemble Metatile Editor Panel
        JPanel jpnlMetatileEdit = getPanel(new GridBagLayout());
        jpnlMetatileEdit.add(jpnlMetatileTools, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlMetatileEdit.add(jsclCharacterPicker, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlMetatileEdit.add(jsclMetatileDock, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets2, 1, 1));
        return jpnlMetatileEdit;
    }

    /**
     * Rebuilds the metatile dock. Characters keep the same size as in the character dock,
     * so a row holds 4 metatiles of 2x2 or 2 metatiles of 4x4.
     */
    private void buildMetatileDock(int size) {
        int cols = FONT_COLS / size;
        Dimension buttonSize = new Dimension(Globals.DM_TOOL.width * size, Globals.DM_TOOL.height * size);
        metatileButtons = new JButton[TIGlobals.N_METATILES];
        metatileDockPanel.removeAll();
        metatileDockPanel.setLayout(new GridLayout(TIGlobals.N_METATILES / cols, cols));
        for (int i = TIGlobals.MIN_METATILE; i <= TIGlobals.MAX_METATILE; i++) {
            metatileButtons[i] = getDockButton("", TIGlobals.TI_PALETTE_OPAQUE[0], buttonSize, new EditMetatileAction(i, "", parent, mapEditor, dataSet, preferences));
            metatileButtons[i].setToolTipText("Metatile " + i + " (>" + Integer.toHexString(i).toUpperCase() + ")");
            metatileButtons[i].setBorder(Globals.bordButtonNormal);
            metatileDockPanel.add(metatileButtons[i]);
        }
        metatileDockPanel.revalidate();
        metatileDockPanel.repaint();
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
                DualClickButton dbtnColorButton = getPaletteButton(
                    new ColorForeCharAction(cd, "", parent, mapEditor, dataSet, preferences),
                    new ColorBackCharAction(cd, "", parent, mapEditor, dataSet, preferences),
                    TIGlobals.TI_PALETTE_OPAQUE[cd]
                );
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
            charECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), charGridCanvas.getColorDraw(), charGridCanvas.getColorBack(), new PaletteHandler(true));
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
                DualClickButton dbtnColorButton = getPaletteButton(
                    new ColorForeSpriteAction(cd, "", parent, mapEditor, dataSet, preferences),
                    new ColorBackSpriteAction(cd, "", parent, mapEditor, dataSet, preferences),
                    TIGlobals.TI_PALETTE_OPAQUE[cd]
                );
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
            spriteECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), spriteGridCanvas.getColorDraw(), spriteGridCanvas.getColorBack(), new PaletteHandler(false));
            jPanel.add(spriteECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
    }

    public void buildDocks() {
        characterDockPanel = buildCharacterDock(characterDockPanel);
        spriteDockPanel = buildSpriteDock(spriteDockPanel);
        characterPicker.setRange(preferences.getCharacterSetStart(), preferences.getCharacterSetEnd());
    }

    protected JPanel buildCharacterDock(JPanel jPanel) {
        int dockFontRows = preferences.getCharacterSetSize() / FONT_COLS;
        if (jPanel != null) {
            jPanel.removeAll();
            jPanel.setLayout(new GridLayout(dockFontRows, FONT_COLS + 1));
        } else {
            jPanel = getPanel(new GridLayout(dockFontRows, FONT_COLS + 1));
        }
        int capacity = preferences.getCharacterSetCapacity();
        int col = FONT_COLS;
        for (int c = TIGlobals.MIN_CHAR; c <= MAX_CHAR; c++) {
            boolean shown;
            if (c < TIGlobals.BASIC_FIRST_CHAR) {
                shown = capacity >= CHARACTER_SET_EXPANDED;
            } else if (c <= TIGlobals.BASIC_LAST_CHAR) {
                shown = true;
            } else if (c <= EXP_LAST_CHAR) {
                shown = capacity >= CHARACTER_SET_EXPANDED;
            } else {
                shown = capacity >= CHARACTER_SET_SUPER;
            }
            if (shown) {
                if (col >= FONT_COLS) {
                    jPanel.add(getCharacterRowLabel(c));
                    col = 0;
                }
                jPanel.add(charButtons[c]);
                col++;
            }
        }
        jPanel.revalidate();
        return jPanel;
    }

    /**
     * Label for the character dock row starting at character c: L1, L2 ... below the basic
     * character set, 1, 2 ... in it (shaded differently above the last XB character), and
     * U1, U2 ... above it.
     */
    private JLabel getCharacterRowLabel(int c) {
        if (c < TIGlobals.BASIC_FIRST_CHAR) {
            return getLabel("L" + (c / FONT_COLS + 1) + " ", JLabel.RIGHT, CLR_CHARS_LOWER);
        }
        else if (c <= TIGlobals.BASIC_LAST_CHAR) {
            int row = (c - TIGlobals.BASIC_FIRST_CHAR) / FONT_COLS + 1;
            return getLabel(row + " ", JLabel.RIGHT, c > TIGlobals.FINALXBCHAR ? CLR_CHARS_BASE2 : CLR_CHARS_BASE1);
        }
        else {
            int row = (c - TIGlobals.BASIC_LAST_CHAR - 1) / FONT_COLS + 1;
            return getLabel((row < 100 ? "U" : "") + row + " ", JLabel.RIGHT, CLR_CHARS_UPPER);
        }
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

    protected JButton getToolButton(String iconKey, String tooltip, Action action) {
        return getToolButton(iconKey, tooltip, Globals.CLR_BUTTON_NORMAL, action);
    }

    protected JButton getToolButton(String iconKey, String tooltip, Color bgcolor, Action action) {
        JButton jbtnTool = getToolButton(getIcon(iconKey), bgcolor);
        jbtnTool.setToolTipText(tooltip);
        jbtnTool.addActionListener(action);
        return jbtnTool;
    }

    private JButton getToolButton(ImageIcon imageIcon, Color bgcolor) {
        JButton jbtnTool = new JButton(imageIcon);
        jbtnTool.setMargin(new Insets(0, 0, 0, 0));
        jbtnTool.setBackground(bgcolor);
        jbtnTool.setPreferredSize(Globals.DM_TOOL);
        return jbtnTool;
    }

    protected JButton getDockButton(String buttonlabel, Color bgcolor, Dimension size, ActionListener listener) {
        JButton jbtnDock = new JButton(buttonlabel);
        jbtnDock.addActionListener(listener);
        jbtnDock.setOpaque(true);
        jbtnDock.setBackground(bgcolor);
        jbtnDock.setMargin(new Insets(0, 0, 0, 0));
        jbtnDock.setPreferredSize(size);
        return jbtnDock;
    }

    protected DualClickButton getPaletteButton(Action foreAction, Action backAction, Color bgcolor) {
        DualClickButton dbtnPal = new DualClickButton("", foreAction, backAction);
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

    /**
     * Maps the ECM palette dock gestures onto the character or the sprite actions.
     */
    private class PaletteHandler implements ECMPaletteComboBox.PaletteHandler {

        private final boolean chr;

        PaletteHandler(boolean chr) {
            this.chr = chr;
        }

        public void foreColorSelected(int index, ActionEvent e) {
            (chr
                ? new ColorForeCharAction(index, "", parent, mapEditor, dataSet, preferences)
                : new ColorForeSpriteAction(index, "", parent, mapEditor, dataSet, preferences)
            ).actionPerformed(e);
        }

        public void backColorSelected(int index, ActionEvent e) {
            (chr
                ? new ColorBackCharAction(index, "", parent, mapEditor, dataSet, preferences)
                : new ColorBackSpriteAction(index, "", parent, mapEditor, dataSet, preferences)
            ).actionPerformed(e);
        }

        public void colorChosen(int index, ActionEvent e) {
            (chr
                ? new ColorChooseCharAction(index, "", parent, mapEditor, dataSet, preferences)
                : new ColorChooseSpriteAction(index, "", parent, mapEditor, dataSet, preferences)
            ).actionPerformed(e);
        }

        public void paletteSelected() {
            ActionEvent e = new ActionEvent(MagellanUI.this, ActionEvent.ACTION_PERFORMED, null);
            (chr
                ? new PaletteSelectCharAction("", parent, mapEditor, dataSet, preferences)
                : new PaletteSelectSpriteAction("", parent, mapEditor, dataSet, preferences)
            ).actionPerformed(e);
        }
    }
}
