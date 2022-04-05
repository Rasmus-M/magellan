package com.dreamcodex.ti.ui;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.actions.exporting.*;
import com.dreamcodex.ti.actions.importing.*;
import com.dreamcodex.ti.actions.tools.OverlayImageAction;
import com.dreamcodex.ti.component.MapEditor;
import com.dreamcodex.ti.component.RecentMenu;
import com.dreamcodex.ti.util.DataSet;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.Preferences;

import javax.swing.*;

import static com.dreamcodex.ti.Magellan.*;
import static com.dreamcodex.ti.util.Globals.*;
import static com.dreamcodex.ti.util.Globals.CMD_SNAP_SPRITES_TO_GRID;

public class MagellanUI {

    private final Magellan parent;
    private final MapEditor mapEditor;
    private final DataSet dataSet;
    private final int colorMode;
    private final Preferences preferences;

    private ButtonGroup colorModeButtonGroup;
    private JMenuItem jmitGraphicsColorMode;
    private JMenuItem jmitBitmapColorMode;
    private JMenuItem jmitECM2ColorMode;
    private JMenuItem jmitECM3ColorMode;

    private ButtonGroup characterSetSizeButtonGroup;
    JMenuItem jmitCharacterSetBasic;
    JMenuItem jmitCharacterSetExpanded;
    JMenuItem jmitCharacterSetSuper;


    public MagellanUI(Magellan parent, MapEditor mapEditor, DataSet dataSet, int colorMode, Preferences preferences) {

        this.parent = parent;
        this.mapEditor = mapEditor;
        this.dataSet = dataSet;
        this.colorMode = colorMode;
        this.preferences = preferences;
    }

    public JMenuBar createMenu() {
        JMenuBar jMenuBar = new JMenuBar();

        JMenu jmenFile = new JMenu("File");
        JMenuItem jmitNew = new JMenuItem("New Map Project");
        jmitNew.setActionCommand(Globals.CMD_NEW);
        jmitNew.addActionListener(parent);
        jmenFile.add(jmitNew);
        JMenuItem jmitOpen = new JMenuItem();
        jmitOpen.setAction(new OpenDataFileAction("Open Map Project", null, parent, mapEditor, dataSet, preferences));
        jmenFile.add(jmitOpen);
        JMenu jmenuOpenRecent = new RecentMenu(preferences.getRecentFiles(), parent, mapEditor, dataSet, preferences);
        jmenFile.add(jmenuOpenRecent);
        JMenuItem jmitSave = new JMenuItem();
        jmitSave.setAction(new SaveDataFileAction("Save Map Project", parent, mapEditor, dataSet, preferences));
        jmenFile.add(jmitSave);
        JMenuItem jmitSaveAs = new JMenuItem();
        jmitSaveAs.setAction(new SaveDataFileAsAction("Save Map Project as...", parent, mapEditor, dataSet, preferences));
        jmenFile.add(jmitSaveAs);
        jmenFile.addSeparator();
        JMenuItem jmitAppend = new JMenuItem();
        jmitAppend.setAction(new AppendDataFileAction("Append Maps", parent, mapEditor, dataSet, preferences));
        jmenFile.add(jmitAppend);
        jmenFile.addSeparator();
        JMenuItem jmitExit = new JMenuItem("Exit");
        jmitExit.setActionCommand(Globals.CMD_EXIT);
        jmitExit.addActionListener(parent);
        jmenFile.add(jmitExit);
        // Add menu
        jMenuBar.add(jmenFile);

        JMenu jmenImport = new JMenu("Import");
        JMenuItem jmitImportChrImgMono = new JMenuItem();
        jmitImportChrImgMono.setAction(new ImportCharImageAction(false, "Character Image (Mono)", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportChrImgMono);
        JMenuItem jmitImportChrImgColor = new JMenuItem();
        jmitImportChrImgColor.setAction(new ImportCharImageAction(true, "Character Image (Color)", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportChrImgColor);
        JMenuItem jmitImportVramDump = new JMenuItem();
        jmitImportVramDump.setAction(new ImportVRAMDumpAction("VRAM Dump", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportVramDump);
        JMenuItem jmitImportBinaryMap = new JMenuItem();
        jmitImportBinaryMap.setAction(new ImportBinaryMapAction("Binary Map", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportBinaryMap);
        JMenuItem jmitImportMapImage = new JMenuItem();
        jmitImportMapImage.setAction(new ImportMapImageAction("Map Image", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportMapImage);
        JMenuItem jmitImportSpriteImage = new JMenuItem();
        jmitImportSpriteImage.setAction(new ImportSpriteImageAction("Sprite Image", parent, mapEditor, dataSet, preferences));
        jmenImport.add(jmitImportSpriteImage);
        // Add menu
        jMenuBar.add(jmenImport);

        JMenu jmenExport = new JMenu("Export");
        JMenuItem jmitExportData = new JMenuItem();
        jmitExportData.setAction(new ExportXBDataFileAction(BASIC_DATA, "BASIC Data", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportData);
        JMenuItem jmitExportBasic = new JMenuItem();
        jmitExportBasic.setAction(new ExportXBDataFileAction(BASIC_PROGRAM, "BASIC Program", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportBasic);
        JMenuItem jmitExportExec = new JMenuItem();
        jmitExportExec.setAction(new ExportXBDataFileAction(XB_PROGRAM, "XB Program", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportExec);
        JMenuItem jmitExportXB256 = new JMenuItem();
        jmitExportXB256.setAction(new ExportXBDataFileAction(XB256_PROGRAM, "XB 256 Program", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportXB256);
        JMenuItem jmitExportXBDisMer = new JMenuItem();
        jmitExportXBDisMer.setAction(new ExportXBDisplayMergeAction("XB Display Merge", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportXBDisMer);
        jmenExport.addSeparator();
        JMenuItem jmitExportAsm = new JMenuItem();
        jmitExportAsm.setAction(new ExportAssemblyDataFileAction("Assembly Data", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportAsm);
        JMenuItem jmitExportScrollMap = new JMenuItem();
        jmitExportScrollMap.setAction(new ExportScrollFileAction("Assembly Scroll Data", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportScrollMap);
        jmenExport.addSeparator();
        JMenuItem jmitExportBin = new JMenuItem();
        jmitExportBin.setAction(new ExportBinaryFileAction("Binary Data", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportBin);
        JMenuItem jmitExportBinMap = new JMenuItem();
        jmitExportBinMap.setAction(new ExportBinaryMapAction("Binary Map (current)", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportBinMap);
        jmenExport.addSeparator();
        JMenuItem jmitExportChrImgMono = new JMenuItem();
        jmitExportChrImgMono.setAction(new ExportCharImageAction(false, "Character Image (Mono)", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportChrImgMono);
        JMenuItem jmitExportChrImgColor = new JMenuItem();
        jmitExportChrImgColor.setAction(new ExportCharImageAction(true, "Character Image (Color)", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportChrImgColor);
        JMenuItem jmitExportSpriteImg = new JMenuItem();
        jmitExportSpriteImg.setAction(new ExportSpriteImageAction(true, "Sprite Image", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportSpriteImg);
        JMenuItem jmitExportMapImg = new JMenuItem();
        jmitExportMapImg.setAction(new ExportMapImageAction("Map Image", parent, mapEditor, dataSet, preferences));
        jmenExport.add(jmitExportMapImg);
        // Add menu
        jMenuBar.add(jmenExport);

        JMenu jmenTools = new JMenu("Tools");
        JMenuItem jmitSwapChars = new JMenuItem("Replace Characters");
        jmitSwapChars.setActionCommand(Globals.CMD_SWAPCHARS);
        jmitSwapChars.addActionListener(parent);
        jmenTools.add(jmitSwapChars);
        jmenTools.addSeparator();
        JMenuItem jmitAnalyzeCharUsage = new JMenuItem("Analyze Character Usage");
        jmitAnalyzeCharUsage.setActionCommand(Globals.CMD_ANALYZECHARUSAGE);
        jmitAnalyzeCharUsage.addActionListener(parent);
        jmenTools.add(jmitAnalyzeCharUsage);
        JMenuItem jmitAnalyzeCharTrans = new JMenuItem("Analyze Character Transitions");
        jmitAnalyzeCharTrans.setActionCommand(Globals.CMD_ANALYZECHARTRANS);
        jmitAnalyzeCharTrans.addActionListener(parent);
        jmenTools.add(jmitAnalyzeCharTrans);
        jmenTools.addSeparator();
        JMenuItem jmitOverlayImage = new JMenuItem();
        jmitOverlayImage.setAction(new OverlayImageAction("Overlay Image", parent, mapEditor, dataSet, preferences));
        jmitOverlayImage.addActionListener(parent);
        jmenTools.add(jmitOverlayImage);
        // Add menu
        jMenuBar.add(jmenTools);

        JMenu jmenOptions = new JMenu("Options");
        JMenuItem jmitShowPos = new JCheckBoxMenuItem("Show Position", mapEditor.showPosIndic());
        jmitShowPos.setActionCommand(Globals.CMD_SHOWPOS);
        jmitShowPos.addActionListener(parent);
        jmenOptions.add(jmitShowPos);
        JMenuItem jmitBase0Pos = new JCheckBoxMenuItem("Base 0 for Position", mapEditor.base0Position());
        jmitBase0Pos.setActionCommand(Globals.CMD_BASE0POS);
        jmitBase0Pos.addActionListener(parent);
        jmenOptions.add(jmitBase0Pos);

        jmenOptions.addSeparator();

        characterSetSizeButtonGroup = new ButtonGroup();

        jmitCharacterSetBasic = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_BASIC], preferences.getCharacterSetCapacity() == CHARACTER_SET_BASIC);
        characterSetSizeButtonGroup.add(jmitCharacterSetBasic);
        jmitCharacterSetBasic.setActionCommand(Globals.CMD_BASICCHARSETSIZE);
        jmitCharacterSetBasic.addActionListener(parent);
        jmenOptions.add(jmitCharacterSetBasic);

        jmitCharacterSetExpanded = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_EXPANDED], preferences.getCharacterSetCapacity() == CHARACTER_SET_EXPANDED);
        characterSetSizeButtonGroup.add(jmitCharacterSetExpanded);
        jmitCharacterSetExpanded.setActionCommand(Globals.CMD_EXPANDEDCHARSETSIZE);
        jmitCharacterSetExpanded.addActionListener(parent);
        jmenOptions.add(jmitCharacterSetExpanded);

        jmitCharacterSetSuper = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_SUPER], preferences.getCharacterSetCapacity() == CHARACTER_SET_SUPER);
        characterSetSizeButtonGroup.add(jmitCharacterSetSuper);
        jmitCharacterSetSuper.setActionCommand(Globals.CMD_SUPERCHARSETSIZE);
        jmitCharacterSetSuper.addActionListener(parent);
        jmenOptions.add(jmitCharacterSetSuper);

        jmenOptions.addSeparator();

        colorModeButtonGroup = new ButtonGroup();
        jmitGraphicsColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_GRAPHICS_1], colorMode == COLOR_MODE_GRAPHICS_1);
        colorModeButtonGroup.add(jmitGraphicsColorMode);
        jmitGraphicsColorMode.setActionCommand(Globals.CMD_GRAPHICSCOLORMODE);
        jmitGraphicsColorMode.addActionListener(parent);
        jmenOptions.add(jmitGraphicsColorMode);
        jmitBitmapColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_BITMAP], colorMode == COLOR_MODE_BITMAP);
        colorModeButtonGroup.add(jmitBitmapColorMode);
        jmitBitmapColorMode.setActionCommand(Globals.CMD_BITMAPCOLORMODE);
        jmitBitmapColorMode.addActionListener(parent);
        jmenOptions.add(jmitBitmapColorMode);
        jmitECM2ColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_ECM_2], colorMode == COLOR_MODE_ECM_2);
        colorModeButtonGroup.add(jmitECM2ColorMode);
        jmitECM2ColorMode.setActionCommand(Globals.CMD_ECM2COLORMODE);
        jmitECM2ColorMode.addActionListener(parent);
        jmenOptions.add(jmitECM2ColorMode);
        jmitECM3ColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_ECM_3], colorMode == COLOR_MODE_ECM_3);
        colorModeButtonGroup.add(jmitECM3ColorMode);
        jmitECM3ColorMode.setActionCommand(Globals.CMD_ECM3COLORMODE);
        jmitECM3ColorMode.addActionListener(parent);
        jmenOptions.add(jmitECM3ColorMode);

        jmenOptions.addSeparator();

        JMenuItem jmitViewCharLayer = new JCheckBoxMenuItem("View Character Layer", mapEditor.getViewCharLayer());
        jmitViewCharLayer.setActionCommand(Globals.CMD_VIEW_CHAR_LAYER);
        jmitViewCharLayer.addActionListener(parent);
        jmenOptions.add(jmitViewCharLayer);
        JMenuItem jmitViewSpriteLayer = new JCheckBoxMenuItem("View Sprite Layer", mapEditor.getViewSpriteLayer());
        jmitViewSpriteLayer.setActionCommand(Globals.CMD_VIEW_SPRITE_LAYER);
        jmitViewSpriteLayer.addActionListener(parent);
        jmenOptions.add(jmitViewSpriteLayer);

        jmenOptions.addSeparator();

        JMenuItem jmitMagnifySprites = new JCheckBoxMenuItem("Magnify Sprites", mapEditor.getMagnifySprites());
        jmitMagnifySprites.setActionCommand(CMD_MAGNIFY_SPRITES);
        jmitMagnifySprites.addActionListener(parent);
        jmenOptions.add(jmitMagnifySprites);
        JMenuItem jmitSnapSpritesToGrid = new JCheckBoxMenuItem("Snap Sprites to Grid", mapEditor.getSnapSpritesToGrid());
        jmitSnapSpritesToGrid.setActionCommand(CMD_SNAP_SPRITES_TO_GRID);
        jmitSnapSpritesToGrid.addActionListener(parent);
        jmenOptions.add(jmitSnapSpritesToGrid);

        // Add menu
        jMenuBar.add(jmenOptions);

        JMenu jmenHelp = new JMenu("Help");
        JMenuItem jmitHelpAbout = new JMenuItem("About Magellan");
        jmitHelpAbout.setActionCommand(Globals.CMD_ABOUT);
        jmitHelpAbout.addActionListener(parent);
        jmenHelp.add(jmitHelpAbout);
        // Add menu
        jMenuBar.add(jmenHelp);

        return jMenuBar;
    }

    public void setColorModeOption(int colorMode) {
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                colorModeButtonGroup.setSelected(jmitGraphicsColorMode.getModel(), true);
                break;
            case COLOR_MODE_BITMAP:
                colorModeButtonGroup.setSelected(jmitBitmapColorMode.getModel(), true);
                break;
            case COLOR_MODE_ECM_2:
                colorModeButtonGroup.setSelected(jmitECM2ColorMode.getModel(), true);
                break;
            case COLOR_MODE_ECM_3:
                colorModeButtonGroup.setSelected(jmitECM3ColorMode.getModel(), true);
                break;
        }
    }

    public void setSuperCharacterSetOption() {
        characterSetSizeButtonGroup.setSelected(jmitCharacterSetSuper.getModel(), true);
    }
}
