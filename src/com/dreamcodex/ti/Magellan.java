package com.dreamcodex.ti;

import com.dreamcodex.ti.actions.clipboard.CopyCharAction;
import com.dreamcodex.ti.actions.clipboard.CopySpriteAction;
import com.dreamcodex.ti.actions.clipboard.PasteCharAction;
import com.dreamcodex.ti.actions.clipboard.PasteSpriteAction;
import com.dreamcodex.ti.actions.exporting.*;
import com.dreamcodex.ti.actions.importing.*;
import com.dreamcodex.ti.component.*;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.iface.ScreenColorListener;
import com.dreamcodex.ti.iface.UndoRedoListener;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.dreamcodex.ti.util.Globals.*;
import static com.dreamcodex.ti.util.TIGlobals.*;

/**
 * Magellan
 * TI-99/4A graphical map editor
 *
 * @author Howard Kistler
 */

public class Magellan extends JFrame implements Runnable, WindowListener, ActionListener, MouseListener, MouseMotionListener, IconProvider, ScreenColorListener, UndoRedoListener, MapChangeListener {

// Constants -------------------------------------------------------------------------------/

    public static final String VERSION_NUMBER = "4.1.2";

    public static final int CHARACTER_SET_BASIC = 0;
    public static final int CHARACTER_SET_EXPANDED = 1;
    public static final int CHARACTER_SET_SUPER = 2;
    public static String[] CHARACTER_SET_SIZES = new String[] {
        "Basic Character Set",
        "Expanded Character Set",
        "Super Character Set"
    };

    public static final int COLOR_MODE_GRAPHICS_1 = 0;
    public static final int COLOR_MODE_BITMAP = 1;
    public static final int COLOR_MODE_ECM_2 = 2;
    public static final int COLOR_MODE_ECM_3 = 3;
    public static String[] COLOR_MODES = new String[] {
        "Graphics 1 Color Mode",
        "Bitmap Color Mode",
        "Enhanced Color Mode - 2 bpp",
        "Enhanced Color Mode - 3 bpp"
    };

    public static final int SCROLL_ORIENTATION_VERTICAL = 0;
    public static final int SCROLL_ORIENTATION_HORIZONTAL = 1;
    public static final int SCROLL_ORIENTATION_ISOMETRIC = 2;

// Local Constants -------------------------------------------------------------------------/

    private static final String APPTITLE = "Magellan v" + VERSION_NUMBER + " : TI-99/4A Map Editor";

    private static final int FONT_ROWS = 32;
    private static final int FONT_COLS = 8;

    private static final int SPRITE_COLS = 4;

    private static final int MAP_ROWS = 24;
    private static final int MAP_COLS = 32;
    private static final int MAP_CELL = 8;

    private static final Color CLR_CHARS_BASE1 = new Color(232, 232, 232);
    private static final Color CLR_CHARS_BASE2 = new Color(196, 196, 196);
    private static final Color CLR_CHARS_LOWER = new Color(222, 242, 255);
    private static final Color CLR_CHARS_UPPER = new Color(255, 222, 242);

    private static final int EDITOR_GRID_SIZE = 192;

// Variables -------------------------------------------------------------------------------/

    protected DataSet dataSet = new DataSet();
    protected int activeChar = TIGlobals.CUSTOMCHAR;
    protected int lastActiveChar = MapCanvas.NOCHAR;
    protected int activeSprite = 0;
    protected int lastActiveSprite = MapCanvas.NOCHAR;
    protected HashMap<Integer, int[][]> defaultChars;
    protected Preferences preferences = new Preferences();
    protected int colorMode = preferences.getColorMode();
    private final String openFilePath;        // File to open upon startup
    private File mapDataFile;                 // Current map file
    private boolean projectModified = false;  // Current project modified?

// Components ------------------------------------------------------------------------------/

    // Tabbed pane
    JTabbedPane jtbpEdit;

    // Character editor
    private GridCanvas gcChar;
    private JTextField jtxtChar;
    private JButton jbtnUpdateChar;
    private JButton jbtnLook;
    private JButton jbtnCharUndo;
    private JButton jbtnCharRedo;
    private JLabel jlblCharInt;
    private JLabel jlblCharHex;
    private JCheckBox jchkTransparency;
    private JPanel jpnlCharColorDock;
    private DualClickButton[] charColorDockButtons = new DualClickButton[0];
    private ECMPaletteComboBox charECMPaletteComboBox;
    private JPanel jpnlCharacterDock;
    private JButton[] jbtnChar;

    // Sprite editor
    private GridCanvas gcSprite;
    private JTextField jtxtSprite;
    private JButton jbtnUpdateSprite;
    private JButton jbtnSpriteUndo;
    private JButton jbtnSpriteRedo;
    private JLabel jlblSpriteInt;
    private JLabel jlblSpriteHex;
    private JPanel jpnlSpriteColorDock;
    private DualClickButton[] spriteColorDockButtons = new DualClickButton[0];
    private ECMPaletteComboBox spriteECMPaletteComboBox;
    private JPanel jpnlSpriteDock;
    private JButton[] jbtnSprite;

    // Menu items
    JMenuItem jmitGraphicsColorMode;
    private ButtonGroup colorModeButtonGroup;
    private JMenuItem jmitBitmapColorMode;
    private JMenuItem jmitECM2ColorMode;
    private JMenuItem jmitECM3ColorMode;
    private ButtonGroup characterSetSizeButtonGroup;
    private JMenuItem jmitCharacterSetSuper;

    // Map editor
    private MapEditor mapdMain;

    // Dialogs
    AnalyzeCharUsageDialog charUsageDialog;
    AnalyzeCharTransDialog charTransDialog;

// Main Method -----------------------------------------------------------------------------/

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Magellan(args.length > 0 ? args[0] : null));
    }

    public void exitApp(int status) throws IOException {
        if (isModified()) {
            int result = showConfirmation("Confirm save on exit", "Do you want to save your changes first?", true);
            if (result == JOptionPane.YES_OPTION) {
                new SaveDataFileAction("", this, mapdMain, dataSet, preferences).actionPerformed(null);
            }
            else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        if (status == 0) {
            savePreferences();
        }
        this.dispose();
        System.exit(status);
    }

// Constructor ----------------------------------------------------------------------------/

    public Magellan(String openFilePath) {
        super(APPTITLE);
        this.openFilePath = openFilePath;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    public void run() {

        try {
            setIconImage(new ImageIcon(getClass().getResource("images/icon64.png")).getImage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        // Create map editor panel (needs to initialise early for the listeners)
        mapdMain = new MapEditor(MAP_COLS, MAP_ROWS, MAP_CELL, this, this);
        mapdMain.fillGrid(TIGlobals.SPACECHAR);
        mapdMain.setBkgrndColor(Globals.CLR_COMPONENTBACK);
        mapdMain.resetUndoManager();

        // Read application properties (if exist)
        readPreferences();

        // Initialize data structures

        // Default characters
        defaultChars = new HashMap<Integer, int[][]>();
        for (int ch = TIGlobals.CHARMAPSTART; ch <= TIGlobals.CHARMAPEND; ch++) {
            defaultChars.put(ch, Globals.getIntGrid(TIGlobals.DEFAULT_TI_CHARS[ch - TIGlobals.CHARMAPSTART], 8));
        }

        // Character structures
        dataSet.setCharGrids(new HashMap<Integer, int[][]>());
        if (colorMode == COLOR_MODE_BITMAP) {
            dataSet.setCharColors(new HashMap<Integer, int[][]>());
        }
        dataSet.setCharImages(new HashMap<Integer, Image>());
        int[][] clrSets = dataSet.getClrSets();
        HashMap<Integer, int[][]> charGrids = dataSet.getCharGrids();
        HashMap<Integer, int[][]> charColors = dataSet.getCharColors();
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int colorSet = ch / COLOR_SET_SIZE;
            clrSets[colorSet][Globals.INDEX_CLR_BACK] = 0;
            clrSets[colorSet][Globals.INDEX_CLR_FORE] = 1;
            int[][] emptyGrid = new int[8][8];
            for (int y = 0; y < emptyGrid.length; y++) {
                for (int x = 0; x < emptyGrid[y].length; x++) {
                    if (ch >= TIGlobals.CHARMAPSTART && ch <= TIGlobals.CHARMAPEND) {
                        emptyGrid[y][x] = defaultChars.get(ch)[y][x];
                    } else {
                        emptyGrid[y][x] = 0;
                    }
                }
            }
            charGrids.put(ch, emptyGrid);
            if (colorMode == COLOR_MODE_BITMAP) {
                int[][] emptyColors = new int[8][2];
                for (int y = 0; y < emptyColors.length; y++) {
                    emptyColors[y][0] = 0;
                    emptyColors[y][1] = 1;
                }
                charColors.put(ch, emptyColors);
            }
        }

        // Sprite structures
        HashMap<Integer, int[][]> spriteGrids = new HashMap<Integer, int[][]>();
        dataSet.setSpriteGrids(spriteGrids);
        HashMap<Integer, Image> spriteImages = new HashMap<Integer, Image>();
        dataSet.setSpriteImages(spriteImages);
        int[] spriteColors = dataSet.getSpriteColors();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            spriteGrids.put(i, new int[16][16]);
            spriteColors[i] = 1;
        }

        // ECM palettes
        buildECMPalettes();

        // Main UI components
        JMenuBar jMenuBar = createMenu();
        JPanel jpnlMain = createUI();

        // Assemble the application
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(jpnlMain, BorderLayout.CENTER);
        this.getContentPane().add(jMenuBar, BorderLayout.NORTH);
        this.addWindowListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.pack();
        this.setVisible(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Register listeners
        gcChar.addUndoRedoListener(this);
        gcSprite.addUndoRedoListener(this);
        mapdMain.addMapChangeListener(this);
        mapdMain.addScreenColorListener(this);
        jtbpEdit.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean spriteMode = jtbpEdit.getSelectedIndex() == 1;
                mapdMain.setSpriteMode(spriteMode);
                if (spriteMode) {
                    jbtnLook.setBackground(Globals.CLR_BUTTON_NORMAL);
                }
            }
        });

        updateScreenColorPalette();
        updateCharButtons();

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    editDefault();
                    // Open command line file
                    if (openFilePath != null) {
                        new OpenDataFileAction("", openFilePath, Magellan.this, mapdMain, dataSet, preferences).actionPerformed(null);
                    }
                }
            }
        );
    }

// UI Builder Methods ----------------------------------------------------------------------------/

    protected JMenuBar createMenu() {
        // Create the menus
        JMenuBar jMenuBar = new JMenuBar();
        JMenu jmenFile = new JMenu("File");
        JMenuItem jmitNew = new JMenuItem("New Map Project");
        jmitNew.setActionCommand(Globals.CMD_NEW);
        jmitNew.addActionListener(this);
        jmenFile.add(jmitNew);
        JMenuItem jmitOpen = new JMenuItem();
        jmitOpen.setAction(new OpenDataFileAction("Open Map Project", null, this, mapdMain, dataSet, preferences));
        jmenFile.add(jmitOpen);
        JMenu jmenuOpenRecent = new RecentMenu(preferences.getRecentFiles(), this, mapdMain, dataSet, preferences);
        jmenFile.add(jmenuOpenRecent);
        JMenuItem jmitSave = new JMenuItem();
        jmitSave.setAction(new SaveDataFileAction("Save Map Project", this, mapdMain, dataSet, preferences));
        jmenFile.add(jmitSave);
        JMenuItem jmitSaveAs = new JMenuItem();
        jmitSaveAs.setAction(new SaveDataFileAsAction("Save Map Project as...", this, mapdMain, dataSet, preferences));
        jmenFile.add(jmitSaveAs);
        jmenFile.addSeparator();
        JMenuItem jmitAppend = new JMenuItem();
        jmitAppend.setAction(new AppendDataFileAction("Append Maps", this, mapdMain, dataSet, preferences));
        jmenFile.add(jmitAppend);
        jmenFile.addSeparator();
        JMenuItem jmitExit = new JMenuItem("Exit");
        jmitExit.setActionCommand(Globals.CMD_EXIT);
        jmitExit.addActionListener(this);
        jmenFile.add(jmitExit);
        // Add menu
        jMenuBar.add(jmenFile);

        JMenu jmenImport = new JMenu("Import");
        JMenuItem jmitImportChrImgMono = new JMenuItem();
        jmitImportChrImgMono.setAction(new ImportCharImageAction(false, "Character Image (Mono)", this, mapdMain, dataSet, preferences));
        jmenImport.add(jmitImportChrImgMono);
        JMenuItem jmitImportChrImgColor = new JMenuItem();
        jmitImportChrImgColor.setAction(new ImportCharImageAction(true, "Character Image (Color)", this, mapdMain, dataSet, preferences));
        jmenImport.add(jmitImportChrImgColor);
        JMenuItem jmitImportVramDump = new JMenuItem();
        jmitImportVramDump.setAction(new ImportVRAMDumpAction("VRAM Dump", this, mapdMain, dataSet, preferences));
        jmenImport.add(jmitImportVramDump);
        JMenuItem jmitImportMapImage = new JMenuItem();
        jmitImportMapImage.setAction(new ImportMapImageAction("Map Image", this, mapdMain, dataSet, preferences));
        jmenImport.add(jmitImportMapImage);
        JMenuItem jmitImportSpriteImage = new JMenuItem();
        jmitImportSpriteImage.setAction(new ImportSpriteImageAction("Sprite Image", this, mapdMain, dataSet, preferences));
        jmenImport.add(jmitImportSpriteImage);
        // Add menu
        jMenuBar.add(jmenImport);

        JMenu jmenExport = new JMenu("Export");

        JMenuItem jmitExportData = new JMenuItem();
        jmitExportData.setAction(new ExportXBDataFileAction(BASIC_DATA, "BASIC Data", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportData);
        JMenuItem jmitExportBasic = new JMenuItem();
        jmitExportBasic.setAction(new ExportXBDataFileAction(BASIC_PROGRAM, "BASIC Program", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportBasic);
        JMenuItem jmitExportExec = new JMenuItem();
        jmitExportExec.setAction(new ExportXBDataFileAction(XB_PROGRAM, "XB Program", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportExec);
        JMenuItem jmitExportXB256 = new JMenuItem();
        jmitExportXB256.setAction(new ExportXBDataFileAction(XB256_PROGRAM, "XB 256 Program", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportXB256);
        JMenuItem jmitExportXBDisMer = new JMenuItem();
        jmitExportXBDisMer.setAction(new ExportXBDisplayMergeAction("XB Display Merge", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportXBDisMer);

        jmenExport.addSeparator();

        JMenuItem jmitExportAsm = new JMenuItem();
        jmitExportAsm.setAction(new ExportAssemblyDataFileAction("Assembly Data", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportAsm);
        JMenuItem jmitExportScrollMap = new JMenuItem();
        jmitExportScrollMap.setAction(new ExportScrollFileAction("Assembly Scroll Data", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportScrollMap);

        jmenExport.addSeparator();

        JMenuItem jmitExportBin = new JMenuItem();
        jmitExportBin.setAction(new ExportBinaryFileAction("Binary Data", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportBin);
        JMenuItem jmitExportBinMap = new JMenuItem();
        jmitExportBinMap.setAction(new ExportBinaryMapAction("Binary Map (current)", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportBinMap);

        jmenExport.addSeparator();

        JMenuItem jmitExportChrImgMono = new JMenuItem();
        jmitExportChrImgMono.setAction(new ExportCharImageAction(false, "Character Image (Mono)", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportChrImgMono);
        JMenuItem jmitExportChrImgColor = new JMenuItem();
        jmitExportChrImgColor.setAction(new ExportCharImageAction(true, "Character Image (Color)", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportChrImgColor);
        JMenuItem jmitExportSpriteImg = new JMenuItem();
        jmitExportSpriteImg.setAction(new ExportSpriteImageAction(true, "Sprite Image", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportSpriteImg);
        JMenuItem jmitExportMapImg = new JMenuItem();
        jmitExportMapImg.setAction(new ExportMapImageAction("Map Image", this, mapdMain, dataSet, preferences));
        jmenExport.add(jmitExportMapImg);

        // Add menu
        jMenuBar.add(jmenExport);

        JMenu jmenTools = new JMenu("Tools");
        JMenuItem jmitSwapChars = new JMenuItem("Replace Characters");
        jmitSwapChars.setActionCommand(Globals.CMD_SWAPCHARS);
        jmitSwapChars.addActionListener(this);
        jmenTools.add(jmitSwapChars);
        JMenuItem jmitAnalyzeCharUsage = new JMenuItem("Analyze Character Usage");
        jmitAnalyzeCharUsage.setActionCommand(Globals.CMD_ANALYZECHARUSAGE);
        jmitAnalyzeCharUsage.addActionListener(this);
        jmenTools.add(jmitAnalyzeCharUsage);
        JMenuItem jmitAnalyzeCharTrans = new JMenuItem("Analyze Character Transitions");
        jmitAnalyzeCharTrans.setActionCommand(Globals.CMD_ANALYZECHARTRANS);
        jmitAnalyzeCharTrans.addActionListener(this);
        jmenTools.add(jmitAnalyzeCharTrans);

        // Add menu
        jMenuBar.add(jmenTools);

        JMenu jmenOptions = new JMenu("Options");
        JMenuItem jmitShowPos = new JCheckBoxMenuItem("Show Position", mapdMain.showPosIndic());
        jmitShowPos.setActionCommand(Globals.CMD_SHOWPOS);
        jmitShowPos.addActionListener(this);
        jmenOptions.add(jmitShowPos);
        JMenuItem jmitBase0Pos = new JCheckBoxMenuItem("Base 0 for Position", mapdMain.base0Position());
        jmitBase0Pos.setActionCommand(Globals.CMD_BASE0POS);
        jmitBase0Pos.addActionListener(this);
        jmenOptions.add(jmitBase0Pos);

        jmenOptions.addSeparator();

        characterSetSizeButtonGroup = new ButtonGroup();

        JRadioButtonMenuItem jmitCharacterSetBasic = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_BASIC], preferences.getCharacterSetCapacity() == CHARACTER_SET_BASIC);
        characterSetSizeButtonGroup.add(jmitCharacterSetBasic);
        jmitCharacterSetBasic.setActionCommand(Globals.CMD_BASICCHARSETSIZE);
        jmitCharacterSetBasic.addActionListener(this);
        jmenOptions.add(jmitCharacterSetBasic);

        JRadioButtonMenuItem jmitCharacterSetExpanded = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_EXPANDED], preferences.getCharacterSetCapacity() == CHARACTER_SET_EXPANDED);
        characterSetSizeButtonGroup.add(jmitCharacterSetExpanded);
        jmitCharacterSetExpanded.setActionCommand(Globals.CMD_EXPANDEDCHARSETSIZE);
        jmitCharacterSetExpanded.addActionListener(this);
        jmenOptions.add(jmitCharacterSetExpanded);

        jmitCharacterSetSuper = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_SUPER], preferences.getCharacterSetCapacity() == CHARACTER_SET_SUPER);
        characterSetSizeButtonGroup.add(jmitCharacterSetSuper);
        jmitCharacterSetSuper.setActionCommand(Globals.CMD_SUPERCHARSETSIZE);
        jmitCharacterSetSuper.addActionListener(this);
        jmenOptions.add(jmitCharacterSetSuper);

        jmenOptions.addSeparator();

        colorModeButtonGroup = new ButtonGroup();
        jmitGraphicsColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_GRAPHICS_1], colorMode == COLOR_MODE_GRAPHICS_1);
        colorModeButtonGroup.add(jmitGraphicsColorMode);
        jmitGraphicsColorMode.setActionCommand(Globals.CMD_GRAPHICSCOLORMODE);
        jmitGraphicsColorMode.addActionListener(this);
        jmenOptions.add(jmitGraphicsColorMode);
        jmitBitmapColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_BITMAP], colorMode == COLOR_MODE_BITMAP);
        colorModeButtonGroup.add(jmitBitmapColorMode);
        jmitBitmapColorMode.setActionCommand(Globals.CMD_BITMAPCOLORMODE);
        jmitBitmapColorMode.addActionListener(this);
        jmenOptions.add(jmitBitmapColorMode);
        jmitECM2ColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_ECM_2], colorMode == COLOR_MODE_ECM_2);
        colorModeButtonGroup.add(jmitECM2ColorMode);
        jmitECM2ColorMode.setActionCommand(Globals.CMD_ECM2COLORMODE);
        jmitECM2ColorMode.addActionListener(this);
        jmenOptions.add(jmitECM2ColorMode);
        jmitECM3ColorMode = new JRadioButtonMenuItem(COLOR_MODES[COLOR_MODE_ECM_3], colorMode == COLOR_MODE_ECM_3);
        colorModeButtonGroup.add(jmitECM3ColorMode);
        jmitECM3ColorMode.setActionCommand(Globals.CMD_ECM3COLORMODE);
        jmitECM3ColorMode.addActionListener(this);
        jmenOptions.add(jmitECM3ColorMode);

        jmenOptions.addSeparator();

        JMenuItem jmitViewCharLayer = new JCheckBoxMenuItem("View Character Layer", mapdMain.getViewCharLayer());
        jmitViewCharLayer.setActionCommand(Globals.CMD_VIEW_CHAR_LAYER);
        jmitViewCharLayer.addActionListener(this);
        jmenOptions.add(jmitViewCharLayer);
        JMenuItem jmitViewSpriteLayer = new JCheckBoxMenuItem("View Sprite Layer", mapdMain.getViewSpriteLayer());
        jmitViewSpriteLayer.setActionCommand(Globals.CMD_VIEW_SPRITE_LAYER);
        jmitViewSpriteLayer.addActionListener(this);
        jmenOptions.add(jmitViewSpriteLayer);

        // Add menu
        jMenuBar.add(jmenOptions);

        JMenu jmenHelp = new JMenu("Help");
        JMenuItem jmitHelpAbout = new JMenuItem("About Magellan");
        jmitHelpAbout.setActionCommand(Globals.CMD_ABOUT);
        jmitHelpAbout.addActionListener(this);
        jmenHelp.add(jmitHelpAbout);
        // Add menu
        jMenuBar.add(jmenHelp);

        return jMenuBar;
    }

    protected JPanel createUI() {

        Insets insets = new Insets(1, 1, 1, 1);
        Insets insets2 = new Insets(4, 0, 0, 0);

        // Create the main panel
        JPanel jpnlMain = getPanel(new BorderLayout());

        // Create the tabbed pane
        jtbpEdit = new JTabbedPane();

        // Create Character Editor
        JPanel jpnlCharTools = getPanel(new GridBagLayout());

        // Create toolbar on the left side of character editor
        JPanel jpnlToolButtons = getPanel(new GridLayout(7, 1, 0, 2));
        jpnlToolButtons.add(getToolButton(Globals.CMD_FILL_CHR, "Fill"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_CLEAR_CHR, "Clear"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_INVERT_CHR, "Invert Image"));
        jpnlToolButtons.add(getToolButton(Globals.CMD_GRID_CHR, "Toggle Grid"));
        jbtnLook = getToolButton(Globals.CMD_LOOK, "Look At Character");
        jpnlToolButtons.add(jbtnLook);
        jbtnCharUndo = getToolButton(Globals.CMD_UNDO_CHR, "Undo Edit");
        jbtnCharUndo.setEnabled(false);
        jpnlToolButtons.add(jbtnCharUndo);
        jbtnCharRedo = getToolButton(Globals.CMD_REDO_CHR, "Redo Edit");
        jbtnCharRedo.setEnabled(false);
        jpnlToolButtons.add(jbtnCharRedo);
        jpnlCharTools.add(jpnlToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create character editor grid and surrounding buttons
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATEL_CHR, "Rotate Left", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblCharInt = getLabel();
        jlblCharInt.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(jlblCharInt, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTU_CHR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblCharHex = getLabel();
        jlblCharHex.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(jlblCharHex, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_ROTATER_CHR, "Rotate Right", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTL_CHR, "Shift Left", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTR_CHR, "Shift Right", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPH_CHR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTD_CHR, "Shift Down", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_FLIPV_CHR, "Flip Vertical", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        JPanel jpnlChar = getPanel(new BorderLayout());
        gcChar = new GridCanvas(TIGlobals.TI_PALETTE_OPAQUE, 8, 8, 8, this, this, colorMode);
        gcChar.setColorScreen(TIGlobals.TI_PALETTE_OPAQUE[mapdMain.getColorScreen()]);
        jpnlChar.add(gcChar, BorderLayout.CENTER);
        Dimension jpnlCharDimension = new Dimension(EDITOR_GRID_SIZE, EDITOR_GRID_SIZE);
        jpnlChar.setPreferredSize(jpnlCharDimension);
        jpnlChar.setMinimumSize(jpnlCharDimension);
        jpnlCharTools.add(jpnlChar, new GridBagConstraints(3, 2, 3, 3, 2, 2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 2, 2));

        // Create character copy/paste tool
        JPanel jpnlCharTool = getPanel(new BorderLayout());
        jchkTransparency = new JCheckBox();
        jchkTransparency.setOpaque(false);
        jchkTransparency.setToolTipText("Toggle Transparency");
        jchkTransparency.setVisible(colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3);
        jchkTransparency.setActionCommand(Globals.CMD_TRANSPARENCY);
        jchkTransparency.addActionListener(this);
        jpnlToolButtons.add(jchkTransparency);
        jpnlCharTool.add(jchkTransparency, BorderLayout.WEST);
        jtxtChar = new JTextField();
        jpnlCharTool.add(jtxtChar, BorderLayout.CENTER);
        JPanel jpnlCharToolbar = getPanel(new GridLayout(1, 3));
        jbtnUpdateChar = getToolButton(Globals.CMD_UPDATE_CHR, "Set Char");
        jbtnUpdateChar.addActionListener(this);
        jpnlCharToolbar.add(jbtnUpdateChar);

        Action copyCharAction = new CopyCharAction(getIcon(Globals.CMD_COPY_CHR), jtxtChar);
        JButton copyButton = getToolButton(copyCharAction, "Copy");
        copyButton.getActionMap().put(Globals.CMD_COPY_CHR, copyCharAction);
        copyButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) copyCharAction.getValue(Action.ACCELERATOR_KEY), Globals.CMD_COPY_CHR);
        jpnlCharToolbar.add(copyButton);

        Action pasteCharAction = new PasteCharAction(getIcon(Globals.CMD_PASTE_CHR), jtxtChar, jbtnUpdateChar);
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
        jpnlCharColorDock = buildCharColorDock(null);

        // Create Character Dock Buttons
        jbtnChar = new JButton[(TIGlobals.MAX_CHAR - TIGlobals.MIN_CHAR) + 1];
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int rowNum = ch / 8;
            jbtnChar[ch] = getDockButton(((ch >= TIGlobals.CHARMAPSTART) && (ch <= TIGlobals.CHARMAPEND) ? "" + TIGlobals.CHARMAP[ch - TIGlobals.CHARMAPSTART] : "?"), Globals.CMD_EDIT_CHR + ch, TIGlobals.TI_PALETTE_OPAQUE[dataSet.getClrSets()[rowNum][Globals.INDEX_CLR_BACK]]);
            jbtnChar[ch].setForeground(TIGlobals.TI_COLOR_UNUSED);
        }

        // Initialise the border objects for button selection
        Globals.bordButtonNormal = jbtnChar[0].getBorder();

        // Create Character Dock
        jpnlCharacterDock = buildCharacterDock(null);
        JScrollPane jsclCharacterDock = new JScrollPane(jpnlCharacterDock, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Assemble Character Editor Panel
        JPanel jpnlCharEdit = getPanel(new GridBagLayout());
        jpnlCharEdit.add(jpnlCharTools, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlCharEdit.add(jpnlCharColorDock, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlCharEdit.add(jsclCharacterDock, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets2, 1, 1));

        // Create Sprite Editor
        JPanel jpnlSpriteTools = getPanel(new GridBagLayout());

        // Create toolbar on the left side of sprite editor
        JPanel jpnlSpriteToolButtons = getPanel(new GridLayout(6, 1, 0, 2));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_FILL_SPR, "Fill"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_CLEAR_SPR, "Clear"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_INVERT_SPR, "Invert Image"));
        jpnlSpriteToolButtons.add(getToolButton(Globals.CMD_GRID_SPR, "Toggle Grid"));
        jbtnSpriteUndo = getToolButton(Globals.CMD_UNDO_SPR, "Undo Edit");
        jbtnSpriteUndo.setEnabled(false);
        jpnlSpriteToolButtons.add(jbtnSpriteUndo);
        jbtnSpriteRedo = getToolButton(Globals.CMD_REDO_SPR, "Redo Edit");
        jbtnSpriteRedo.setEnabled(false);
        jpnlSpriteToolButtons.add(jbtnSpriteRedo);
        jpnlSpriteTools.add(jpnlSpriteToolButtons, new GridBagConstraints(1, 1, 1, 5, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, insets, 2, 2));

        // Create sprite editor grid and surrounding buttons
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATEL_SPR, "Rotate Left", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblSpriteInt = getLabel();
        jlblSpriteInt.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(jlblSpriteInt, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTU_SPR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblSpriteHex = getLabel();
        jlblSpriteHex.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(jlblSpriteHex, new GridBagConstraints(5, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_ROTATER_SPR, "Rotate Right", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTL_SPR, "Shift Left", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(2, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTR_SPR, "Shift Right", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(6, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPH_SPR, "Flip Horizontal", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(2, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTD_SPR, "Shift Down", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_FLIPV_SPR, "Flip Vertical", Globals.CLR_BUTTON_TRANS), new GridBagConstraints(6, 5, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 2, 2));
        JPanel jpnlSprite = getPanel(new BorderLayout());
        gcSprite = new GridCanvas(TIGlobals.TI_PALETTE_OPAQUE, 16, 16, 6, this, this, colorMode == COLOR_MODE_BITMAP ? COLOR_MODE_GRAPHICS_1 : colorMode);
        gcSprite.setECMTransparency(true);
        gcSprite.setColorScreen(TIGlobals.TI_PALETTE_OPAQUE[mapdMain.getColorScreen()]);
        jpnlSprite.add(gcSprite, BorderLayout.CENTER);
        Dimension jpnlSpriteDimension = new Dimension(EDITOR_GRID_SIZE, EDITOR_GRID_SIZE);
        jpnlSprite.setPreferredSize(jpnlSpriteDimension);
        jpnlSprite.setMinimumSize(jpnlSpriteDimension);
        jpnlSpriteTools.add(jpnlSprite, new GridBagConstraints(3, 2, 3, 3, 2, 2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 2, 2));

        // Create sprite copy/paste tool
        JPanel jpnlSpriteTool = getPanel(new BorderLayout());
        jtxtSprite = new JTextField();
        jpnlSpriteTool.add(jtxtSprite, BorderLayout.CENTER);
        JPanel jpnlSpriteToolbar = getPanel(new GridLayout(1, 3));
        jbtnUpdateSprite = getToolButton(Globals.CMD_UPDATE_SPR, "Set Sprite");
        jbtnUpdateSprite.addActionListener(this);
        jpnlSpriteToolbar.add(jbtnUpdateSprite);

        Action copySpriteAction = new CopySpriteAction(getIcon(Globals.CMD_COPY_SPR), jtxtSprite);
        JButton copySpriteButton = getToolButton(copySpriteAction, "Copy");
        copySpriteButton.getActionMap().put(CMD_COPY_SPR, copySpriteAction);
        copySpriteButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) copySpriteAction.getValue(Action.ACCELERATOR_KEY), Globals.CMD_COPY_SPR);
        jpnlSpriteToolbar.add(copySpriteButton);

        Action pasteSpriteAction = new PasteSpriteAction(getIcon(Globals.CMD_PASTE_SPR), jtxtSprite, jbtnUpdateSprite);
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
        jpnlSpriteColorDock = buildSpriteColorDock(null);

        // Create Sprite Dock Buttons
        jbtnSprite = new JButton[TIGlobals.MAX_SPRITE + 1];
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            jbtnSprite[i] = getDockButton(Integer.toString(i), Globals.CMD_EDIT_SPR + i, TIGlobals.TI_PALETTE_OPAQUE[0], Globals.DM_SPRITE);
            jbtnSprite[i].setForeground(TIGlobals.TI_COLOR_UNUSED);
        }

        // Create Sprite Dock
        jpnlSpriteDock = buildSpriteDock(null);
        JScrollPane jsclSpriteDock = new JScrollPane(jpnlSpriteDock, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Assemble Sprite Editor Panel
        JPanel jpnlSpriteEdit = getPanel(new GridBagLayout());
        jpnlSpriteEdit.add(jpnlSpriteTools, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlSpriteEdit.add(jpnlSpriteColorDock, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets2, 1, 1));
        jpnlSpriteEdit.add(jsclSpriteDock, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets2, 1, 1));

        // Tabs
        jtbpEdit.add("Characters", jpnlCharEdit);
        jtbpEdit.add("Sprites", jpnlSpriteEdit);

        // Main
        jpnlMain.add(mapdMain, BorderLayout.CENTER);
        jpnlMain.add(jtbpEdit, BorderLayout.WEST);

        return jpnlMain;
    }

// Component Builder Methods ---------------------------------------------------------------/

    protected void buildColorDocks() {
        buildECMPalettes();
        jpnlCharColorDock = buildCharColorDock(jpnlCharColorDock);
        jpnlSpriteColorDock = buildSpriteColorDock(jpnlSpriteColorDock);
        updateScreenColorPalette();
    }

    protected void buildECMPalettes() {
        ECMPalette[] ecmPalettes;
        if (colorMode == COLOR_MODE_ECM_2) {
            ecmPalettes = new ECMPalette[16];
            for (int i = 0; i < 16; i++) {
                ecmPalettes[i] = new ECMPalette(4, 4 * (i % 4));
            }
        }
        else {
            ecmPalettes = new ECMPalette[8];
            for (int i = 0; i < 8; i++) {
                ecmPalettes[i] = new ECMPalette(8, 8 * (i % 2));
            }
        }
        dataSet.setEcmPalettes(ecmPalettes);
        ECMPalette[] ecmCharPalettes = dataSet.getEcmCharPalettes();
        if (ecmCharPalettes == null) {
            ecmCharPalettes = new ECMPalette[TIGlobals.MAX_CHAR + 1];
            dataSet.setEcmCharPalettes(ecmCharPalettes);
        }
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            ecmCharPalettes[i] = ecmPalettes[0];
        }
        ECMPalette[] ecmSpritePalettes = dataSet.getEcmSpritePalettes();
        if (ecmSpritePalettes == null) {
            ecmSpritePalettes = new ECMPalette[TIGlobals.MAX_SPRITE + 1];
            dataSet.setEcmSpritePalettes(ecmSpritePalettes);
        }
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            ecmSpritePalettes[i] = ecmPalettes[0];
        }
    }

    protected JPanel buildCharColorDock(JPanel jPanel) {
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
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
                if (cd == gcChar.getColorBack()) {
                    dbtnColorButton.setText("B");
                }
                if (cd == gcChar.getColorDraw()) {
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
            charECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), gcChar.getColorDraw(), gcChar.getColorBack(), this, true);
            jPanel.add(charECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
    }

    protected JPanel buildSpriteColorDock(JPanel jPanel) {
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
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
                if (cd == gcSprite.getColorBack()) {
                    dbtnColorButton.setText("B");
                }
                if (cd == gcSprite.getColorDraw()) {
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
            spriteECMPaletteComboBox = new ECMPaletteComboBox(dataSet.getEcmPalettes(), gcSprite.getColorDraw(), gcSprite.getColorBack(), this, false);
            jPanel.add(spriteECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
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
                    jPanel.add(jbtnChar[c]);
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
                jPanel.add(jbtnChar[c]);
                col++;
            } else if (c <= EXP_LAST_CHAR) {
                if (preferences.getCharacterSetCapacity() >= CHARACTER_SET_EXPANDED) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel("U" + ucount + " ", JLabel.RIGHT, CLR_CHARS_UPPER));
                        ucount++;
                        col = 0;
                    }
                    jPanel.add(jbtnChar[c]);
                    col++;
                }
            } else {
                if (preferences.getCharacterSetCapacity() >= CHARACTER_SET_SUPER) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel((ucount < 100 ? "U" : "") + ucount + " ", JLabel.RIGHT, CLR_CHARS_UPPER));
                        ucount++;
                        col = 0;
                    }
                    jPanel.add(jbtnChar[c]);
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
            jPanel.add(jbtnSprite[i]);
        }
        return jPanel;
    }

    protected JPanel getPanel(LayoutManager layout) {
        JPanel jpnlRtn = new JPanel(layout);
        jpnlRtn.setOpaque(true);
        jpnlRtn.setBackground(Globals.CLR_COMPONENTBACK);
        return jpnlRtn;
    }

    protected ImageIcon getIcon(String name) {
        if (name.endsWith("spr") || name.endsWith("chr")) {
            name = name.substring(0, name.length() - 3);
        }
        URL imageURL = getClass().getResource("images/icon_" + name + "_mono.png");
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(imageURL));
    }

    protected JButton getToolButton(Action action, String tooltip) {
        JButton jbtnTool = getToolButton((ImageIcon) null, tooltip, CLR_BUTTON_NORMAL);
        jbtnTool.setAction(action);
        jbtnTool.setToolTipText(tooltip);
        return jbtnTool;
    }

    protected JButton getToolButton(String buttonKey, String tooltip) {
        return getToolButton(buttonKey, tooltip, Globals.CLR_BUTTON_NORMAL);
    }

    protected JButton getToolButton(String buttonKey, String tooltip, Color bgcolor) {
        JButton jbtnTool = getToolButton(getIcon(buttonKey), tooltip, bgcolor);
        jbtnTool.setToolTipText(tooltip);
        jbtnTool.setActionCommand(buttonKey);
        jbtnTool.addActionListener(this);
        return jbtnTool;
    }

    private JButton getToolButton(ImageIcon imageIcon, String tooltip, Color bgcolor) {
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
        jbtnDock.addActionListener(this);
        jbtnDock.setOpaque(true);
        jbtnDock.setBackground(bgcolor);
        jbtnDock.setMargin(new Insets(0, 0, 0, 0));
        jbtnDock.setPreferredSize(size);
        return jbtnDock;
    }

    protected DualClickButton getPaletteButton(String forecmd, String backcmd, Color bgcolor) {
        DualClickButton dbtnPal = new DualClickButton("", forecmd, backcmd, this);
        dbtnPal.addActionListener(this);
        dbtnPal.setOpaque(true);
        dbtnPal.setBackground(bgcolor);
        dbtnPal.setMargin(new Insets(0, 0, 0, 0));
        dbtnPal.setPreferredSize(Globals.DM_TOOL);
        dbtnPal.setFocusable(false);
        return dbtnPal;
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

// Listeners -------------------------------------------------------------------------------/

    /* ActionListener methods */
    public void actionPerformed(ActionEvent ae) {
        try {
            String command = ae.getActionCommand();
            if (command.equals(Globals.CMD_EXIT)) {
                exitApp(0);
            } else if (command.equals(Globals.CMD_CLEAR_CHR)) {
                gcChar.clearGrid();
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_CLEAR_SPR)) {
                gcSprite.clearGrid();
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_CHR)) {
                gcChar.fillGrid();
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_SPR)) {
                gcSprite.fillGrid();
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_GRID_CHR)) {
                gcChar.toggleGrid();
                updateComponents();
            } else if (command.equals(Globals.CMD_GRID_SPR)) {
                gcSprite.toggleGrid();
            } else if (command.equals(Globals.CMD_UNDO_CHR)) {
                gcChar.undo();
            } else if (command.equals(Globals.CMD_UNDO_SPR)) {
                gcSprite.undo();
            } else if (command.equals(Globals.CMD_REDO_CHR)) {
                gcChar.redo();
            } else if (command.equals(Globals.CMD_REDO_SPR)) {
                gcSprite.redo();
            } else if (command.equals(Globals.CMD_FLIPH_CHR)) {
                gcChar.setGrid(Globals.flipGrid(gcChar.getGridData(), false));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPH_SPR)) {
                gcSprite.setGrid(Globals.flipGrid(gcSprite.getGridData(), false));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_CHR)) {
                gcChar.setGridAndColors(Globals.flipGrid(gcChar.getGridData(), true), colorMode == COLOR_MODE_BITMAP ? Globals.flipGrid(gcChar.getGridColors(), true) : null);
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_SPR)) {
                gcSprite.setGrid(Globals.flipGrid(gcSprite.getGridData(), true));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_CHR)) {
                gcChar.setGrid(Globals.rotateGrid(gcChar.getGridData(), true));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_SPR)) {
                gcSprite.setGrid(Globals.rotateGrid(gcSprite.getGridData(), true));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_CHR)) {
                gcChar.setGrid(Globals.rotateGrid(gcChar.getGridData(), false));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_SPR)) {
                gcSprite.setGrid(Globals.rotateGrid(gcSprite.getGridData(), false));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_CHR)) {
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0 || colorMode != COLOR_MODE_BITMAP) {
                    gcChar.setGrid(Globals.invertGrid(gcChar.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                }
                else {
                    gcChar.setGridAndColors(Globals.invertGrid(gcChar.getGridData(), 1), Globals.flipGrid(gcChar.getGridColors(), false));
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_SPR)) {
                gcSprite.setGrid(Globals.invertGrid(gcSprite.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_CHR)) {
                gcChar.setGrid(Globals.cycleGridUp(gcChar.getGridData()));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    gcChar.setColors(Globals.cycleGridUp(gcChar.getGridColors()));
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_SPR)) {
                gcSprite.setGrid(Globals.cycleGridUp(gcSprite.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateCharButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_CHR)) {
                gcChar.setGrid(Globals.cycleGridDown(gcChar.getGridData()));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    gcChar.setColors(Globals.cycleGridDown(gcChar.getGridColors()));
                    dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_SPR)) {
                gcSprite.setGrid(Globals.cycleGridDown(gcSprite.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_CHR)) {
                gcChar.setGrid(Globals.cycleGridLeft(gcChar.getGridData()));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_SPR)) {
                gcSprite.setGrid(Globals.cycleGridLeft(gcSprite.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_CHR)) {
                gcChar.setGrid(Globals.cycleGridRight(gcChar.getGridData()));
                dataSet.getCharGrids().put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_SPR)) {
                gcSprite.setGrid(Globals.cycleGridRight(gcSprite.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_LOOK)) {
                mapdMain.setLookModeOn(!mapdMain.isLookModeOn());
                jbtnLook.setBackground((mapdMain.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
            } else if (command.equals(Globals.CMD_NEW)) {
                int userResponse = showConfirmation("Confirm New Project", "This will delete all current data.\n\rAre you sure?", false);
                if (userResponse == JOptionPane.YES_OPTION) {
                    newProject();
                    setAppTitle();
                    editDefault();
                }
            } else if (command.startsWith(Globals.CMD_EDIT_CHR)) {
                int oldActiveChar = activeChar;
                activeChar = Integer.parseInt(command.substring(Globals.CMD_EDIT_CHR.length()));
                HashMap<Integer, int[][]> charGrids = dataSet.getCharGrids();
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
                    swapCharacters(activeChar, oldActiveChar, 0, true, true, true);
                }
                if (charGrids.get(activeChar) == null) {
                    gcChar.clearGrid();
                    charGrids.put(activeChar, gcChar.getGridData());
                    if (colorMode == COLOR_MODE_BITMAP) {
                        dataSet.getCharColors().put(activeChar, gcChar.getGridColors());
                    }
                }
                gcChar.resetUndoRedo();
                gcChar.setGridAndColors(charGrids.get(activeChar), colorMode == COLOR_MODE_BITMAP ? dataSet.getCharColors().get(activeChar) : null);
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    int cset = activeChar / 8;
                    gcChar.setColorBack(dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK]);
                    gcChar.setColorDraw(dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE]);
                    for (int i = 0; i < charColorDockButtons.length; i++) {
                        charColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcChar.getColorDraw() ? "F" : ""));
                    }
                }
                else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    ECMPalette ecmPalette = dataSet.getEcmCharPalettes()[activeChar];
                    gcChar.setPalette(ecmPalette.getColors());
                    charECMPaletteComboBox.setSelectedItem(ecmPalette);
                }
                gcChar.setECMTransparency(dataSet.getEcmCharTransparency()[activeChar]);
                gcChar.redrawCanvas();
                mapdMain.setActiveChar(activeChar);
                mapdMain.setCloneModeOn(false);
                updateComponents();
            } else if (command.startsWith(Globals.CMD_EDIT_SPR)) {
                int oldActiveSprite = activeSprite;
                activeSprite = Integer.parseInt(command.substring(Globals.CMD_EDIT_SPR.length()));
                HashMap<Integer, int[][]> spriteGrids = dataSet.getSpriteGrids();
                int[] spriteColors = dataSet.getSpriteColors();
                ECMPalette[] ecmSpritePalettes = dataSet.getEcmSpritePalettes();
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
                    swapSprites(activeSprite, oldActiveSprite);
                }
                if (spriteGrids.get(activeSprite) == null) {
                    gcSprite.clearGrid();
                    spriteGrids.put(activeSprite, gcSprite.getGridData());
                }
                gcSprite.resetUndoRedo();
                gcSprite.setGrid(spriteGrids.get(activeSprite));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    gcSprite.setColorDraw(spriteColors[activeSprite]);
                    for (int i = 0; i < spriteColorDockButtons.length; i++) {
                        spriteColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
                    }
                }
                else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    gcSprite.setPalette(ecmSpritePalettes[activeSprite].getColors());
                    spriteECMPaletteComboBox.setSelectedItem(ecmSpritePalettes[activeSprite]);
                }
                gcSprite.redrawCanvas();
                mapdMain.setActiveSprite(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_UPDATE_CHR)) {
                String hexString = "";
                switch (colorMode) {
                    case COLOR_MODE_GRAPHICS_1:
                        hexString = Globals.padHexString(jtxtChar.getText(), 16);
                        break;
                    case COLOR_MODE_BITMAP:
                        hexString = Globals.padHexString(jtxtChar.getText(), jtxtChar.getText().length() <= 16 ? 16 : 32);
                        break;
                    case COLOR_MODE_ECM_2:
                        hexString = Globals.padHexString(jtxtChar.getText(), 32 + 4);
                        break;
                    case COLOR_MODE_ECM_3:
                        hexString = Globals.padHexString(jtxtChar.getText(), 48 + 4);
                        break;
                }
                jtxtChar.setText(hexString);
                jtxtChar.setCaretPosition(0);
                // Plane 0
                int[][] charGrid = Globals.getIntGrid(hexString.substring(0, 16), 8);
                // Bitmap colors
                int[][] charColors = dataSet.getCharColors() != null ? dataSet.getCharColors().get(activeChar) : null;
                if (colorMode == COLOR_MODE_BITMAP && hexString.length() == 32) {
                    charColors = Globals.parseColorHexString(hexString.substring(16));
                    dataSet.getCharColors().put(activeChar, charColors);
                }
                // Plane 1
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    Globals.orGrid(Globals.getIntGrid(hexString.substring(16, 32), 8), charGrid, 1);
                }
                // Plane 2
                if (colorMode == COLOR_MODE_ECM_3) {
                    Globals.orGrid(Globals.getIntGrid(hexString.substring(32, 48), 8), charGrid, 2);
                }
                // Palette
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    int palette = Integer.parseInt(colorMode == COLOR_MODE_ECM_2 ? hexString.substring(32, 36) : hexString.substring(48, 52), 16);
                    dataSet.getEcmCharPalettes()[activeChar] = dataSet.getEcmPalettes()[palette];
                    charECMPaletteComboBox.setSelectedItem(dataSet.getEcmCharPalettes()[activeChar]);
                }
                dataSet.getCharGrids().put(activeChar, charGrid);
                gcChar.setGridAndColors(dataSet.getCharGrids().get(activeChar), colorMode == COLOR_MODE_BITMAP ? charColors : null);
                updateCharButton(activeChar);
            } else if (command.equals(Globals.CMD_UPDATE_SPR)) {
                String hexString = "";
                switch (colorMode) {
                    case COLOR_MODE_BITMAP:
                    case COLOR_MODE_GRAPHICS_1:
                        hexString = Globals.padHexString(jtxtSprite.getText(), 64);
                        break;
                    case COLOR_MODE_ECM_2:
                        hexString = Globals.padHexString(jtxtSprite.getText(), 128 + 4);
                        break;
                    case COLOR_MODE_ECM_3:
                        hexString = Globals.padHexString(jtxtSprite.getText(), 192 + 4);
                        break;
                }
                jtxtSprite.setText(hexString);
                jtxtSprite.setCaretPosition(0);
                // Plane 0
                int[][] spriteGrid = Globals.getSpriteIntGrid(hexString.substring(0, 64));
                // Plane 1
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    Globals.orGrid(Globals.getSpriteIntGrid(hexString.substring(64, 128)), spriteGrid, 1);
                }
                // Plane 2
                if (colorMode == COLOR_MODE_ECM_3) {
                    Globals.orGrid(Globals.getSpriteIntGrid(hexString.substring(128, 192)), spriteGrid, 2);
                }
                // Palette
                if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    int palette = Integer.parseInt(colorMode == COLOR_MODE_ECM_2 ? hexString.substring(128, 132) : hexString.substring(192, 196), 16);
                    dataSet.getEcmSpritePalettes()[activeSprite] = dataSet.getEcmPalettes()[palette];
                    spriteECMPaletteComboBox.setSelectedItem(dataSet.getEcmSpritePalettes()[activeSprite]);
                }
                dataSet.getSpriteGrids().put(activeSprite, spriteGrid);
                gcSprite.setGrid(dataSet.getSpriteGrids().get(activeSprite));
                updateCharButton(activeSprite);
            } else if (command.startsWith(Globals.CMD_CLRFORE_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRFORE_CHR.length()));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    // Mark the selected foreground color
                    for (DualClickButton colorButton : charColorDockButtons) {
                        if ("F".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    charColorDockButtons[index].setText("F");
                    if (colorMode != COLOR_MODE_BITMAP) {
                        int cset = activeChar / 8;
                        dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE] = index;
                        for (int c = 0; c < FONT_COLS; c++) {
                            updateCharButton((cset * 8) + c, false);
                        }
                        updateCharButton(activeChar);
                    }
                    gcChar.setColorDraw(index);
                    gcChar.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        gcChar.setColorDraw(index);
                    }
                    else {
                        // Swap two colors of the palette and of the character grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[charECMPaletteComboBox.getSelectedIndex()];
                        int index2 = charECMPaletteComboBox.getIndexBack();
                        for (int i = 0; i < dataSet.getEcmCharPalettes().length; i++) {
                            if (dataSet.getEcmCharPalettes()[i] == ecmPalette) {
                                Globals.swapGridValues(dataSet.getCharGrids().get(i), index, index2);
                            }
                        }
                        Color color = ecmPalette.getColor(index);
                        Color color2 = ecmPalette.getColor(index2);
                        setECMPaletteColor(ecmPalette, index2, color);
                        setECMPaletteColor(ecmPalette, index, color2);
                    }
                }
            } else if (command.startsWith(Globals.CMD_CLRFORE_SPR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRFORE_SPR.length()));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getSpriteColors()[activeSprite] = index;
                    // Mark the selected foreground color
                    for (DualClickButton colorButton : spriteColorDockButtons) {
                        if ("F".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    spriteColorDockButtons[index].setText("F");
                    updateSpriteButton(activeSprite);
                    gcSprite.setColorDraw(index);
                    gcSprite.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        gcSprite.setColorDraw(index);
                    }
                    else {
                        // Swap two colors of the palette and of the sprite grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[spriteECMPaletteComboBox.getSelectedIndex()];
                        int index2 = spriteECMPaletteComboBox.getIndexBack();
                        for (int i = 0; i < dataSet.getEcmSpritePalettes().length; i++) {
                            if (dataSet.getEcmSpritePalettes()[i] == ecmPalette) {
                                Globals.swapGridValues(dataSet.getSpriteGrids().get(i), index, index2);
                            }
                        }
                        Color color = ecmPalette.getColor(index);
                        Color color2 = ecmPalette.getColor(index2);
                        setECMPaletteColor(ecmPalette, index2, color);
                        setECMPaletteColor(ecmPalette, index, color2);
                    }
                }
            } else if (command.startsWith(Globals.CMD_CLRBACK_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRBACK_CHR.length()));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    // Mark the selected background color
                    for (DualClickButton colorButton : charColorDockButtons) {
                        if ("B".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    charColorDockButtons[index].setText("B");
                    if (colorMode != COLOR_MODE_BITMAP) {
                        int cset = activeChar / 8;
                        dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK] = index;
                        for (int c = 0; c < FONT_COLS; c++) {
                            updateCharButton((cset * 8) + c, false);
                        }
                        updateCharButton(activeChar);
                    }
                    gcChar.setColorBack(index);
                    gcChar.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        gcChar.setColorBack(index);
                    }
                    else {
                        // Swap two colors of the palette and of the character grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[charECMPaletteComboBox.getSelectedIndex()];
                        int index2 = charECMPaletteComboBox.getIndexFore();
                        for (int i = 0; i < dataSet.getEcmCharPalettes().length; i++) {
                            if (dataSet.getEcmCharPalettes()[i] == ecmPalette) {
                                Globals.swapGridValues(dataSet.getCharGrids().get(i), index, index2);
                            }
                        }
                        Color color = ecmPalette.getColor(index);
                        Color color2 = ecmPalette.getColor(index2);
                        setECMPaletteColor(ecmPalette, index2, color);
                        setECMPaletteColor(ecmPalette, index, color2);
                    }
                }
            } else if (command.startsWith(Globals.CMD_CLRBACK_SPR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRBACK_SPR.length()));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    // Mark the selected background color
                    /*
                    for (DualClickButton colorButton : spriteColorDockButtons) {
                        if ("B".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    spriteColorDockButtons[index].setText("B");
                    updateSpriteButton(activeSprite);
                    gcSprite.setColorBack(index);
                    gcSprite.redrawCanvas();
                    */
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        gcSprite.setColorBack(index);
                    }
                    else {
                        // Swap two colors of the palette and of the sprite grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[spriteECMPaletteComboBox.getSelectedIndex()];
                        int index2 = spriteECMPaletteComboBox.getIndexFore();
                        for (int i = 0; i < dataSet.getEcmSpritePalettes().length; i++) {
                            if (dataSet.getEcmSpritePalettes()[i] == ecmPalette) {
                                Globals.swapGridValues(dataSet.getSpriteGrids().get(i), index, index2);
                            }
                        }
                        Color color = ecmPalette.getColor(index);
                        Color color2 = ecmPalette.getColor(index2);
                        setECMPaletteColor(ecmPalette, index2, color);
                        setECMPaletteColor(ecmPalette, index, color2);
                    }
                }
            } else if (command.equals(Globals.CMD_PALSELECT_CHR)) {
                int selectedIndex = charECMPaletteComboBox.getSelectedIndex();
                if (selectedIndex != -1) {
                    ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
                    dataSet.getEcmCharPalettes()[activeChar] = ecmPalette;
                    gcChar.setPalette(ecmPalette.getColors());
                }
                gcChar.redrawCanvas();
                updateCharButton(activeChar);
            } else if (command.equals(Globals.CMD_PALSELECT_SPR)) {
                int selectedIndex = spriteECMPaletteComboBox.getSelectedIndex();
                if (selectedIndex != -1) {
                    ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
                    dataSet.getEcmSpritePalettes()[activeSprite] = ecmPalette;
                    gcSprite.setPalette(ecmPalette.getColors());
                }
                gcSprite.redrawCanvas();
                updateSpriteButton(activeSprite);
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_CHR.length()));
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[charECMPaletteComboBox.getSelectedIndex()];
                // Choose a new palette color
                Color color = ECMColorChooser.showDialog(this, "Select Color", ecmPalette.getColor(index));
                if (color != null) {
                    setECMPaletteColor(ecmPalette, index, color);
                }
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_SPR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_SPR.length()));
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[spriteECMPaletteComboBox.getSelectedIndex()];
                Color color = ECMColorChooser.showDialog(this, "Select Color", ecmPalette.getColor(index));
                if (color != null) {
                    setECMPaletteColor(ecmPalette, index, color);
                }
            } else if (command.equals(Globals.CMD_SWAPCHARS)) {
                showSwapCharactersDialog();
            } else if (command.equals(Globals.CMD_ANALYZECHARUSAGE)) {
                analyzeCharUsage();
            } else if (command.equals(Globals.CMD_ANALYZECHARTRANS)) {
                analyzeCharTrans();
            } else if (command.equals(Globals.CMD_SHOWPOS)) {
                mapdMain.toggleShowPosIndic();
            } else if (command.equals(Globals.CMD_BASE0POS)) {
                mapdMain.toggleBase0Position();
            } else if (command.equals(Globals.CMD_BASICCHARSETSIZE)) {
                setCharacterSetSizeBasic();
            } else if (command.equals(Globals.CMD_EXPANDEDCHARSETSIZE)) {
                setCharacterSetSizeExpanded();
            } else if (command.equals(Globals.CMD_SUPERCHARSETSIZE)) {
                setCharacterSetSizeSuper();
            } else if (command.equals(Globals.CMD_GRAPHICSCOLORMODE)) {
                setGraphicsColorMode();
            } else if (command.equals(Globals.CMD_BITMAPCOLORMODE)) {
                setBitmapColorMode();
            } else if (command.equals(Globals.CMD_ECM2COLORMODE)) {
                setECM2ColorMode();
            } else if (command.equals(Globals.CMD_ECM3COLORMODE)) {
                setECM3ColorMode();
            } else if (command.equals(Globals.CMD_VIEW_CHAR_LAYER)) {
                mapdMain.setViewCharLayer(!mapdMain.getViewCharLayer());
            } else if (command.equals(Globals.CMD_VIEW_SPRITE_LAYER)) {
                mapdMain.setViewSpriteLayer(!mapdMain.getViewSpriteLayer());
            } else if (command.equals(Globals.CMD_TRANSPARENCY)) {
                dataSet.getEcmCharTransparency()[activeChar] = jchkTransparency.isSelected();
                gcChar.setECMTransparency(dataSet.getEcmCharTransparency()[activeChar]);
                gcChar.redrawCanvas();
                updateCharButton(activeChar, true);
            } else if (command.equals(Globals.CMD_ABOUT)) {
                showInformation(
                    "About Magellan",
                    "<html>" +
                        "<h1>Magellan, version " + VERSION_NUMBER + "</h1>" +
                        "<p>\u00a9 2010 Howard Kistler/Dream Codex Retrogames (<a href=\"http://www.dreamcodex.com\">www.dreamcodex.com</a>)</p>" +
                        "<p>Magellan is free software maintained by the TI-99/4A community.</p>" +
                        "<p>Modified by:</p>" +
                        "<ul>" +
                            "<li>Retroclouds (2011)</li>" +
                            "<li>Sometimes99er (2013)</li>" +
                            "<li>David Vella (2016)</li>" +
                            "<li>Rasmus Moustgaard (2013 - ongoing)</li>" +
                        "</ul>" +
                        "<p>Source code available from: <a href=\"https://github.com/Rasmus-M/magellan\">github.com/Rasmus-M/magellan</a></p>" +
                        "<p>Java runtime version: " + System.getProperty("java.version") + "</p>" +
                    "</html>"
                );
            }
            mapdMain.redrawCanvas();
        } catch (Exception e) {
            showError("Program error", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /* WindowListener methods */
    public void windowClosing(WindowEvent we) {
        try {
            exitApp(0);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void windowOpened(WindowEvent we) {
    }

    public void windowClosed(WindowEvent we) {
    }

    public void windowActivated(WindowEvent we) {
    }

    public void windowDeactivated(WindowEvent we) {
    }

    public void windowIconified(WindowEvent we) {
    }

    public void windowDeiconified(WindowEvent we) {
    }

    /* MouseListener methods */
    public void mousePressed(MouseEvent me) {
        if (mapdMain.isLookModeOn()) {
            if (mapdMain.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapdMain.getLookChar());
                this.actionPerformed(aeChar);
                // Turn look mode off
                ActionEvent aeLook = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_LOOK);
                this.actionPerformed(aeLook);
            }
        }
        if (!mapdMain.getHotCell().equals(MapCanvas.PT_OFFGRID)) {
            mapdMain.requestFocus();
        }
        updateComponents();
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
        updateComponents();
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    /* MouseMotionListener methods */
    public void mouseMoved(MouseEvent me) {
        if (mapdMain.isLookModeOn()) {
            if (mapdMain.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapdMain.getLookChar());
                this.actionPerformed(aeChar);
                mapdMain.requestFocus();
            }
        }
        mapdMain.updatePositionIndicator();
    }

    public void mouseDragged(MouseEvent me) {
        if (mapdMain.isLookModeOn()) {
            if (mapdMain.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapdMain.getLookChar());
                this.actionPerformed(aeChar);
                ActionEvent aeLook = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_LOOK);
                this.actionPerformed(aeLook);
            }
        }
        if (!mapdMain.getHotCell().equals(MapCanvas.PT_OFFGRID)) {
            mapdMain.requestFocus();
        }
        updateComponents();
    }

/* IconProvider methods */

    public Icon getIconForChar(int i) {
        try {
            return jbtnChar[i].getIcon();
        } catch (Exception e) {
            return new ImageIcon(this.createImage(gcChar.getGridData().length, gcChar.getGridData()[0].length));
        }
    }

    public Icon getIconForSprite(int i) {
        try {
            return jbtnSprite[i].getIcon();
        } catch (Exception e) {
            return new ImageIcon(this.createImage(gcChar.getGridData().length, gcChar.getGridData()[0].length));
        }
    }

    /* ScreenColorListener methods */
    public void screenColorChanged(int screenColor, boolean modified) {
        gcChar.setColorScreen(getScreenColorPalette()[screenColor]);
        updateCharButtons();
        gcSprite.setColorScreen(getScreenColorPalette()[screenColor]);
        updateSpriteButtons();
        if (modified) {
            setModified(true);
        }
    }

    // Notification from gcChar GridCanvas
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo, Object source) {
        if (source == gcChar) {
            jbtnCharUndo.setEnabled(canUndo);
            jbtnCharRedo.setEnabled(canRedo);
        }
        else {
            jbtnSpriteUndo.setEnabled(canUndo);
            jbtnSpriteRedo.setEnabled(canRedo);
        }
        updateComponents();
        if (canUndo) {
            setModified(true);
        }
    }

    // Notification from MapEditor mapdMain
    public void mapChanged() {
        setModified(true);
    }

    public boolean isModified() {
        return projectModified;
    }

    public void setModified(boolean modified) {
        if (modified != projectModified) {
            projectModified = modified;
        }
        setAppTitle();
    }

    public File getMapDataFile() {
        return mapDataFile;
    }

    public void setMapDataFile(File mapDataFile) {
        this.mapDataFile = mapDataFile;
    }

    private void setAppTitle() {
        this.setTitle(APPTITLE + (mapDataFile != null ? ": " : "") + (isModified() ? " *" : "") + (mapDataFile != null ? mapDataFile.getName() : ""));
    }

// Action Methods -------------------------------------------------------------/

    public void setColorModeOption(int colorMode) {
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                setGraphicsColorMode();
                colorModeButtonGroup.setSelected(jmitGraphicsColorMode.getModel(), true);
                break;
            case COLOR_MODE_BITMAP:
                setBitmapColorMode();
                colorModeButtonGroup.setSelected(jmitBitmapColorMode.getModel(), true);
                break;
            case COLOR_MODE_ECM_2:
                setECM2ColorMode();
                colorModeButtonGroup.setSelected(jmitECM2ColorMode.getModel(), true);
                break;
            case COLOR_MODE_ECM_3:
                setECM3ColorMode();
                colorModeButtonGroup.setSelected(jmitECM3ColorMode.getModel(), true);
                break;
        }
    }

    protected void setGraphicsColorMode() {
        int oldColorMode = colorMode;
        colorMode = COLOR_MODE_GRAPHICS_1;
        preferences.setColorMode(colorMode);
        buildColorDocks();
        // Characters
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitCharGrids(2, colorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        gcChar.setColorMode(colorMode, TIGlobals.TI_PALETTE_OPAQUE);
        int[] clrSet = dataSet.getClrSets()[activeChar / 8];
        gcChar.setColorBack(clrSet[Globals.INDEX_CLR_BACK]);
        gcChar.setColorDraw(clrSet[Globals.INDEX_CLR_FORE]);
        for (int i = 0; i < charColorDockButtons.length; i++) {
            charColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcChar.getColorDraw() ? "F" : ""));
        }
        updateCharButtons();
        // Sprites
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitSpriteGrids(2, colorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        gcSprite.setColorMode(colorMode, TIGlobals.TI_PALETTE_OPAQUE);
        gcSprite.setColorBack(0);
        gcSprite.setColorDraw(dataSet.getSpriteColors()[activeSprite]);
        for (int i = 0; i < spriteColorDockButtons.length; i++) {
            spriteColorDockButtons[i].setText(i == gcSprite.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
        }
        updateSpriteButtons();
    }

    protected void setBitmapColorMode() {
        int oldColorMode = colorMode;
        colorMode = COLOR_MODE_BITMAP;
        preferences.setColorMode(colorMode);
        buildColorDocks();
        // Characters
        HashMap<Integer, int[][]> charColors = new HashMap<Integer, int[][]>();
        dataSet.setCharColors(charColors);
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int[][] emptyColors = new int[8][2];
            for (int y = 0; y < emptyColors.length; y++) {
                int[] clrSet = dataSet.getClrSets()[ch / 8];
                if (oldColorMode == COLOR_MODE_GRAPHICS_1) {
                    emptyColors[y][Globals.INDEX_CLR_BACK] = clrSet[Globals.INDEX_CLR_BACK];
                    emptyColors[y][Globals.INDEX_CLR_FORE] = clrSet[Globals.INDEX_CLR_FORE];
                }
                else {
                    int[][] charGrid = dataSet.getCharGrids().get(ch);
                    if (charGrid != null) {
                        int[] row = charGrid[y];
                        ECMPalette ecmPalette = dataSet.getEcmCharPalettes()[ch];
                        int[][] palette = new int[8][2];
                        int maxIndex = -1;
                        for (Integer colorIndex : row) {
                            boolean found = false;
                            for (int p = 0; p <= maxIndex && !found; p++) {
                                if (palette[p][0] == colorIndex) {
                                    palette[p][1]++;
                                    found = true;
                                }
                            }
                            if (!found) {
                                maxIndex++;
                                palette[maxIndex][0] = colorIndex;
                                palette[maxIndex][1] = 1;
                            }
                        }
                        Globals.sortGrid(palette);
                        emptyColors[y][Globals.INDEX_CLR_BACK] = Globals.getClosestColorIndex(ecmPalette.getColor(palette[0][0]), TIGlobals.TI_PALETTE_OPAQUE);
                        emptyColors[y][Globals.INDEX_CLR_FORE] = maxIndex > 0  ? Globals.getClosestColorIndex(ecmPalette.getColor(palette[1][0]), TIGlobals.TI_PALETTE_OPAQUE, emptyColors[y][Globals.INDEX_CLR_BACK]) : emptyColors[y][Globals.INDEX_CLR_BACK];
                        for (int x = 0; x < row.length; x++) {
                            row[x] = Globals.colorDistance(ecmPalette.getColor(row[x]), TIGlobals.TI_PALETTE_OPAQUE[emptyColors[y][Globals.INDEX_CLR_BACK]]) <  Globals.colorDistance(ecmPalette.getColor(row[x]), TIGlobals.TI_PALETTE_OPAQUE[emptyColors[y][Globals.INDEX_CLR_FORE]]) ? 0 : 1;
                        }
                    }
                }
            }
            charColors.put(ch, emptyColors);
        }
        gcChar.setColorMode(colorMode, TIGlobals.TI_PALETTE_OPAQUE);
        for (int i = 0; i < charColorDockButtons.length; i++) {
            charColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcChar.getColorDraw() ? "F" : ""));
        }
        updateCharButtons();
        // Sprites
        gcSprite.setColorMode(COLOR_MODE_GRAPHICS_1, TIGlobals.TI_PALETTE_OPAQUE);
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitSpriteGrids(2, colorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        gcSprite.setColorBack(0);
        gcSprite.setColorDraw(dataSet.getSpriteColors()[activeSprite]);
        for (int i = 0; i < spriteColorDockButtons.length; i++) {
            spriteColorDockButtons[i].setText(i == gcSprite.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
        }
        updateSpriteButtons();
    }

    protected void setECM2ColorMode() {
        if (colorMode == COLOR_MODE_ECM_3) {
            limitCharGrids(4, 8);
            limitSpriteGrids(4, 8);
        }
        colorMode = COLOR_MODE_ECM_2;
        preferences.setColorMode(colorMode);
        buildColorDocks();
        gcChar.setColorMode(colorMode, dataSet.getEcmCharPalettes()[activeChar].getColors());
        updateCharButtons();
        gcSprite.setColorMode(colorMode, dataSet.getEcmSpritePalettes()[activeSprite].getColors());
        updateSpriteButtons();
    }

    protected void setECM3ColorMode() {
        int[] spriteColors = dataSet.getSpriteColors();
        for (int i = 0; i < spriteColors.length; i++) {
            if (spriteColors[i] > 7) {
                spriteColors[i] >>= 1;
            }
        }
        colorMode = COLOR_MODE_ECM_3;
        preferences.setColorMode(colorMode);
        buildColorDocks();
        gcChar.setColorMode(colorMode, dataSet.getEcmCharPalettes()[activeChar].getColors());
        updateCharButtons();
        gcSprite.setColorMode(colorMode, dataSet.getEcmSpritePalettes()[activeSprite].getColors());
        updateSpriteButtons();
    }

    public void setSuperCharacterSetOption() {
        setCharacterSetSizeSuper();
        characterSetSizeButtonGroup.setSelected(jmitCharacterSetSuper.getModel(), true);
    }

    public void setCharacterSetSizeBasic() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_BASIC);
        jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
        jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
    }

    public void setCharacterSetSizeExpanded() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_EXPANDED);
        jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
        jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
    }

    public void setCharacterSetSizeSuper() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_SUPER);
        jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
        jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
    }

// Tool Methods -------------------------------------------------------------/

    protected void showSwapCharactersDialog() {
        CharacterSwapDialog swapper = new CharacterSwapDialog(this, this, preferences.isSwapBoth(), preferences.isSwapImages(), preferences.isAllMaps(), preferences.getCharacterSetStart(), preferences.getCharacterSetEnd(), activeChar);
        if (swapper.isOkay()) {
            swapCharacters(swapper.getBaseChar(), swapper.getSwapChar(), swapper.getRepeatCount(), swapper.doSwapChars(), swapper.doSwapImages(), swapper.doAllMaps());
            preferences.setSwapBoth(swapper.doSwapChars());
            preferences.setSwapImages(swapper.doSwapImages());
            preferences.setAllMaps(swapper.doAllMaps());
        }
    }

    protected void swapCharacters(int baseChar, int swapChar, int repeatCount, boolean doSwapChars, boolean doSwapImages, boolean doAllMaps) {
        // Only process if characters are not the same
        if (baseChar != swapChar) {
            int charCount = repeatCount + 1;
            while (charCount > 0) {
                if (doSwapChars) {
                    for (int m = (doAllMaps ? 0 : mapdMain.getCurrentMapId()); m < (doAllMaps ? mapdMain.getMapCount() : mapdMain.getCurrentMapId() + 1); m++) {
                        int[][] arrayToProc = mapdMain.getMapData(m);
                        for (int y = 0; y < arrayToProc.length; y++) {
                            for (int x = 0; x < arrayToProc[y].length; x++) {
                                if (arrayToProc[y][x] == baseChar) {
                                    arrayToProc[y][x] = swapChar;
                                }
                                else if (arrayToProc[y][x] == swapChar) {
                                    arrayToProc[y][x] = baseChar;
                                }
                            }
                        }
                    }
                }
                if (doSwapImages) {
                    HashMap<Integer, int[][]> charGrids = dataSet.getCharGrids();
                    HashMap<Integer, int[][]> charColors = dataSet.getCharColors();
                    boolean[] ecmCharTransparency = dataSet.getEcmCharTransparency();
                    int[][] charGrid = charGrids.get(swapChar);
                    charGrids.put(swapChar, charGrids.get(baseChar));
                    charGrids.put(baseChar, charGrid);
                    if (colorMode == COLOR_MODE_BITMAP) {
                        int[][] swapColors = charColors.get(swapChar);
                        charColors.put(swapChar, charColors.get(baseChar));
                        charColors.put(baseChar, swapColors);
                    }
                    else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                        ECMPalette[] ecmCharPalettes = dataSet.getEcmCharPalettes();
                        ECMPalette tmpPalette = ecmCharPalettes[swapChar];
                        ecmCharPalettes[swapChar] = ecmCharPalettes[baseChar];
                        ecmCharPalettes[baseChar] = tmpPalette;
                        boolean transparency = ecmCharTransparency[swapChar];
                        ecmCharTransparency[swapChar] = ecmCharTransparency[baseChar];
                        ecmCharTransparency[baseChar] = transparency;
                    }
                    updateCharButton(baseChar, false);
                    updateCharButton(swapChar);
                }
                swapChar++;
                baseChar++;
                charCount--;
            }
            setModified(true);
        }
    }

    protected void swapSprites(int activeSprite, int oldActiveSprite) {
        // Swap on map
        for (int m = 0; m < mapdMain.getMapCount(); m++) {
            HashMap<Point, ArrayList<Integer>> spriteMap = mapdMain.getSpriteMap(m);
            for (Point p : spriteMap.keySet()) {
                ArrayList<Integer> spritesAtPoint = spriteMap.get(p);
                for (int i = 0; i < spritesAtPoint.size(); i++) {
                    if (spritesAtPoint.get(i) == oldActiveSprite) {
                        spritesAtPoint.set(i, activeSprite);
                    } else if (spritesAtPoint.get(i) == activeSprite) {
                        spritesAtPoint.set(i, oldActiveSprite);
                    }
                }
            }
        }
        HashMap<Integer, int[][]> spriteGrids = dataSet.getSpriteGrids();
        int[] spriteColors = dataSet.getSpriteColors();
        ECMPalette[] ecmSpritePalettes = dataSet.getEcmSpritePalettes();
        int[][] tempGrid = Globals.cloneGrid(spriteGrids.get(activeSprite));
        Globals.copyGrid(spriteGrids.get(oldActiveSprite), spriteGrids.get(activeSprite));
        Globals.copyGrid(tempGrid, spriteGrids.get(oldActiveSprite));
        int tempCol = spriteColors[activeSprite];
        spriteColors[activeSprite] = spriteColors[oldActiveSprite];
        spriteColors[oldActiveSprite] = tempCol;
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            ECMPalette tmpPalette = ecmSpritePalettes[activeSprite];
            ecmSpritePalettes[activeSprite] = ecmSpritePalettes[oldActiveSprite];
            ecmSpritePalettes[oldActiveSprite] = tmpPalette;
        }
        updateSpriteButton(oldActiveSprite);
        setModified(true);
    }

    protected void analyzeCharUsage() {
        if (charUsageDialog != null && charUsageDialog.isVisible()) {
            charUsageDialog.transferFocus();
        }
        else {
            charUsageDialog = new AnalyzeCharUsageDialog(this, mapdMain, dataSet.getCharImages(), preferences.getCharacterSetEnd());
        }
    }

    protected void analyzeCharTrans() {
        if (charTransDialog != null && charTransDialog.isVisible()) {
            charTransDialog.transferFocus();
        }
        else {
            charTransDialog = new AnalyzeCharTransDialog(this, mapdMain, dataSet.getCharImages(), dataSet.getCharGrids(), dataSet.getCharColors(), dataSet.getClrSets(), colorMode);
        }
    }

// File Handling Methods -------------------------------------------------------------------/

    private void readPreferences() {
        try {
            preferences.readPreferences();
            colorMode = preferences.getColorMode();
            mapdMain.setViewScale(preferences.getViewScale());
            mapdMain.setTypeCellOn(preferences.isTextCursor());
            mapdMain.setShowGrid(preferences.isShowGrid());
            mapdMain.setGridScale(preferences.getGridScale());
            mapdMain.setShowPosIndic(preferences.isShowPosition());
            mapdMain.setBase0Position(preferences.isBase0Position());
        } catch (Exception e) {
            showError("Error reading preferences", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void savePreferences() {
        try {
            preferences.setColorMode(colorMode);
            preferences.setViewScale(mapdMain.getViewScale());
            preferences.setTextCursor(mapdMain.showTypeCell());
            preferences.setShowGrid(mapdMain.isShowGrid());
            preferences.setGridScale(mapdMain.getGridScale());
            preferences.setShowPosition(mapdMain.showPosIndic());
            preferences.setBase0Position(mapdMain.base0Position());
            preferences.savePreferences();
        } catch (IOException ioe) {
            showError("Error saving preferences", ioe.getMessage());
            ioe.printStackTrace(System.err);
        }
    }

// Class Utility Methods -------------------------------------------------------------------/

    public void newProject() {
        if (colorMode == COLOR_MODE_BITMAP) {
            gcChar.setColorBack(0);
            gcChar.setColorDraw(1);
            for (int i = 0; i < charColorDockButtons.length; i++) {
                charColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcChar.getColorDraw() ? "F" : ""));
            }
            gcSprite.setColorBack(0);
            gcSprite.setColorDraw(1);
            for (int i = 0; i < spriteColorDockButtons.length; i++) {
                spriteColorDockButtons[i].setText(i == gcSprite.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
            }
        }
        else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            buildColorDocks();
        }
        // Characters
        int charNum = TIGlobals.MIN_CHAR;
        for (int r = 0; r < preferences.getCharacterSetSize() / FONT_COLS; r++) {
            dataSet.getClrSets()[r][Globals.INDEX_CLR_BACK] = 0;
            dataSet.getClrSets()[r][Globals.INDEX_CLR_FORE] = 1;
            for (int c = 0; c < FONT_COLS; c++) {
                int[][] emptyGrid = new int[8][8];
                for (int y = 0; y < emptyGrid.length; y++) {
                    for (int x = 0; x < emptyGrid[y].length; x++) {
                        if (charNum >= TIGlobals.CHARMAPSTART && charNum <= TIGlobals.CHARMAPEND) {
                            emptyGrid[y][x] = defaultChars.get(charNum)[y][x];
                        } else {
                            emptyGrid[y][x] = 0;
                        }
                    }
                }
                dataSet.getCharGrids().put(charNum, emptyGrid);
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    int cellNum = (r * FONT_COLS) + c;
                    jbtnChar[cellNum].setBackground(TIGlobals.TI_PALETTE_OPAQUE[dataSet.getClrSets()[r][Globals.INDEX_CLR_BACK]]);
                    jbtnChar[cellNum].setForeground(TIGlobals.TI_COLOR_UNUSED);
                }
                else if (colorMode == COLOR_MODE_BITMAP) {
                    int[][] emptyColors = new int[8][2];
                    for (int y = 0; y < emptyColors.length; y++) {
                        emptyColors[y][0] = 0;
                        emptyColors[y][1] = 1;
                    }
                    dataSet.getCharColors().put(charNum, emptyColors);
                }
                updateCharButton(charNum, false);
                charNum++;
            }
        }
        gcChar.resetUndoRedo();
        gcChar.clearGrid();
        activeChar = TIGlobals.CUSTOMCHAR;
        // Sprites
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            dataSet.getSpriteGrids().put(i, new int[16][16]);
            dataSet.getSpriteColors()[i] = 1;
            updateSpriteButton(i, false);
        }
        gcSprite.resetUndoRedo();
        gcSprite.clearGrid();
        activeSprite = 0;
        // Maps
        mapdMain.delAllMaps();
        mapdMain.setGridWidth(32);
        mapdMain.setGridHeight(24);
        mapdMain.setColorScreen(0);
        mapdMain.redrawCanvas();
        mapdMain.resetUndoManager();
        mapDataFile = null;
        setModified(false);
        updateComponents();
        editDefault();
    }

    public void editDefault() {
        // Edit default character
        activeChar = TIGlobals.CUSTOMCHAR;
        ActionEvent aeInitChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + activeChar);
        Magellan.this.actionPerformed(aeInitChar);
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            updateCharPaletteCombo(false);
        }
        // Edit default sprite
        activeSprite = 0;
        ActionEvent aeInitSprite = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_SPR + activeSprite);
        Magellan.this.actionPerformed(aeInitSprite);
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            updateSpritePaletteCombo(false);
        }
    }

    public int showConfirmation(String title, String message, boolean includeCancel) {
        return JOptionPane.showConfirmDialog(this, message, title, includeCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    public void showInformation(String title, String message) {
        JEditorPane jEditorPane = new JEditorPane("text/html", message);
        jEditorPane.setEditable(false);
        jEditorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        jEditorPane.setBackground(new Color(238, 238, 238));
        jEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                    showError("Error browsing", e1.getMessage());
                }
            }
        });
        JOptionPane.showMessageDialog(this, jEditorPane, title, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("images/logo.png")));
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected void limitCharGrids(int newColors, int oldColors) {
        int factor = oldColors / newColors;
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int[][] charGrid = dataSet.getCharGrids().get(ch);
            if (charGrid != null) {
                for (int[] row : charGrid) {
                    for (int x = 0; x < row.length; x++) {
                        if (row[x] > newColors - 1) {
                            row[x] /= factor;
                        }
                    }
                }
            }
        }
    }

    protected void limitSpriteGrids(int newColors, int oldColors) {
        int factor = oldColors / newColors;
        for (int ch = TIGlobals.MIN_SPRITE; ch <= TIGlobals.MAX_SPRITE; ch++) {
            int[][] charGrid = dataSet.getSpriteGrids().get(ch);
            if (charGrid != null) {
                for (int[] row : charGrid) {
                    for (int x = 0; x < row.length; x++) {
                        if (row[x] > newColors - 1) {
                            row[x] /= factor;
                        }
                    }
                }
            }
        }
    }

    protected int getECMPaletteIndex(ECMPalette ecmPalette) {
        ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
        for (int i = 0; i < ecmPalettes.length; i++) {
            if (ecmPalettes[i] == ecmPalette) {
                return i;
            }
        }
        return -1;
    }


    protected Image makeImageTransparent(Image imgSrc) {
        ImageProducer ipTrans = new FilteredImageSource(imgSrc.getSource(), Globals.ifTrans);
        return Toolkit.getDefaultToolkit().createImage(ipTrans);
    }

    public static int getCharacterSetStart(int characterSetSize) {
        switch (characterSetSize) {
            case CHARACTER_SET_BASIC:
                return TIGlobals.BASIC_FIRST_CHAR;
            case CHARACTER_SET_EXPANDED:
                return TIGlobals.EXP_FIRST_CHAR;
            case CHARACTER_SET_SUPER:
                return TIGlobals.SUPER_FIRST_CHAR;
            default:
                return TIGlobals.BASIC_FIRST_CHAR;
        }
    }

    public static int getCharacterSetEnd(int characterSetSize) {
        switch (characterSetSize) {
            case CHARACTER_SET_BASIC:
                return TIGlobals.BASIC_LAST_CHAR;
            case CHARACTER_SET_EXPANDED:
                return TIGlobals.EXP_LAST_CHAR;
            case CHARACTER_SET_SUPER:
                return SUPER_LAST_CHAR;
            default:
                return TIGlobals.BASIC_LAST_CHAR;
        }
    }

    public static int getCharacterSetSize(int characterSetSize) {
        return getCharacterSetEnd(characterSetSize) - getCharacterSetStart(characterSetSize) + 1;
    }

    public static int getSpriteSetEnd(int characterSetSize) {
        switch (characterSetSize) {
            case CHARACTER_SET_BASIC:
                return BASIC_LAST_SPRITE;
            case CHARACTER_SET_EXPANDED:
                return EXP_LAST_SPRITE;
            case CHARACTER_SET_SUPER:
                return SUPER_LAST_SPRITE;
            default:
                return BASIC_LAST_SPRITE;
        }
    }

    public static int getSpriteSetSize(int characterSetSize) {
        return getSpriteSetEnd(characterSetSize) + 1;
    }

// Update Methods ---------------------------------------------------------------------/

    public void updateAll() {
        updateCharButtons();
        updateSpriteButtons();
        updateComponents();
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            updatePalettes();
        }
    }

    public void updateComponents() {
        // Character editor
        updateCharButton(activeChar, false);
        jbtnLook.setBackground((mapdMain.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
        jchkTransparency.setVisible(colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3);
        jchkTransparency.setSelected(dataSet.getEcmCharTransparency()[activeChar]);
        int[][] charGridData = gcChar.getGridData();
        jtxtChar.setText((
            Globals.getHexString(charGridData)
            + (colorMode == COLOR_MODE_BITMAP ? Globals.getColorHexString(gcChar.getGridColors()) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(dataSet.getEcmCharPalettes()[activeChar]), 4) : "")
        ).toUpperCase());
        jtxtChar.setCaretPosition(0);
        // Sprite editor
        updateSpriteButton(activeSprite, false);
        int[][] spriteGridData = gcSprite.getGridData();
        jtxtSprite.setText((
            Globals.getSpriteHexString(spriteGridData)
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(dataSet.getEcmSpritePalettes()[activeSprite]), 4) : "")
        ).toUpperCase());
        jtxtSprite.setCaretPosition(0);
        // Map
        mapdMain.updateComponents();
    }

    public void updateCharButtons() {
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            updateCharButton(i, false);
        }
        mapdMain.redrawCanvas();
    }

    protected void updateCharButton(int charNum) {
        updateCharButton(charNum, true);
    }

    protected void updateCharButton(int charNum, boolean redrawMap) {
        // Set border
        if (lastActiveChar != charNum && lastActiveChar != MapCanvas.NOCHAR) {
            jbtnChar[lastActiveChar].setBorder(Globals.bordButtonNormal);
        }
        lastActiveChar = charNum;
        jbtnChar[charNum].setBorder(Globals.bordButtonActive);
        // Update labels showing number
        jlblCharInt.setText((charNum < 100 ? " " : "") + charNum);
        jlblCharHex.setText(">" + Integer.toHexString(charNum).toUpperCase());
        // Background color
        Color screenColor = getScreenColorPalette()[mapdMain.getColorScreen()];
        jbtnChar[charNum].setBackground(screenColor);
        // Set text for buttons with no char grid
        if (dataSet.getCharGrids().get(charNum) == null) {
            if (jbtnChar[charNum].getIcon() != null) {
                jbtnChar[charNum].setIcon(null);
                int charMapIndex = charNum - TIGlobals.CHARMAPSTART;
                jbtnChar[charNum].setText(charMapIndex >= 0 && charMapIndex < TIGlobals.CHARMAP.length ? "" + TIGlobals.CHARMAP[charMapIndex] : "?");
            }
            return;
        }
        // Generate icon image
        int imageScale = 2;
        Image image = this.createImage(gcChar.getGridData().length * imageScale, gcChar.getGridData()[0].length * imageScale);
        Graphics g = image.getGraphics();
        Color[] palette = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? TIGlobals.TI_PALETTE_OPAQUE : dataSet.getEcmCharPalettes()[charNum].getColors();
        int[][] charGrid = dataSet.getCharGrids().get(charNum);
        int[][] charColors = null;
        if (colorMode == COLOR_MODE_BITMAP) {
            charColors = dataSet.getCharColors().get(charNum);
        }
        int cset = charNum / 8;
        for (int y = 0; y < charGrid.length; y++) {
            for (int x = 0; x < charGrid[y].length; x++) {
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    if (charGrid[y][x] == 1) {
                        int fore = dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE];
                        g.setColor(fore != 0 ? palette[fore] : screenColor);
                    }
                    else {
                        int back = dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK];
                        g.setColor(back != 0 ? palette[back] : screenColor);
                    }
                }
                else if (colorMode == COLOR_MODE_BITMAP && charColors != null) {
                    int colorIndex = charColors[y][charGrid[y][x]];
                    g.setColor(colorIndex != 0 ? palette[colorIndex] : screenColor);
                }
                else {
                    int colorIndex = charGrid[y][x];
                    g.setColor(colorIndex != 0 || !dataSet.getEcmCharTransparency()[charNum] ? palette[colorIndex] : screenColor);
                }
                g.fillRect(x * imageScale, y * imageScale, imageScale, imageScale);
            }
        }
        g.dispose();
        boolean empty = Globals.isGridEmpty(charGrid) && (colorMode == COLOR_MODE_GRAPHICS_1 && dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK] == 0 || colorMode == COLOR_MODE_BITMAP && Globals.isColorGridEmpty(charColors));
        // Save image
        dataSet.getCharImages().put(charNum, image);
        mapdMain.setCharImage(charNum, image);
        // Display a question mark if image is empty (only some modes)
        if (empty) {
            jbtnChar[charNum].setIcon(null);
            jbtnChar[charNum].setText(((charNum - TIGlobals.CHARMAPSTART) >= 0 && (charNum - TIGlobals.CHARMAPSTART) < TIGlobals.CHARMAP.length) ? "" + TIGlobals.CHARMAP[charNum - TIGlobals.CHARMAPSTART] : "?");
        }
        else {
            jbtnChar[charNum].setIcon(new ImageIcon(image));
            jbtnChar[charNum].setText("");
        }
        // Redraw map as requested
        if (redrawMap) {
            mapdMain.redrawCanvas();
        }
    }

    public void updateSpriteButtons() {
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            updateSpriteButton(i, false);
        }
        mapdMain.redrawCanvas();
    }

    protected void updateSpriteButton(int spriteNum) {
        updateSpriteButton(spriteNum, true);
    }

    protected void updateSpriteButton(int spriteNum, boolean redrawMap) {
        // Set border
        if (lastActiveSprite != spriteNum && lastActiveSprite != MapCanvas.NOCHAR) {
            jbtnSprite[lastActiveSprite].setBorder(Globals.bordButtonNormal);
        }
        lastActiveSprite = spriteNum;
        jbtnSprite[spriteNum].setBorder(Globals.bordButtonActive);
        // Update labels showing number
        jlblSpriteInt.setText((spriteNum < 100 ? " " : "") + spriteNum);
        jlblSpriteHex.setText(">" + Integer.toHexString(spriteNum).toUpperCase());
        // Background color
        Color screenColor = getScreenColorPalette()[mapdMain.getColorScreen()];
        jbtnSprite[spriteNum].setBackground(screenColor);
        // Handle missing sprite grid
        if (dataSet.getSpriteGrids().get(spriteNum) == null) {
            if (jbtnSprite[spriteNum].getIcon() != null) {
                jbtnSprite[spriteNum].setIcon(null);
                jbtnSprite[spriteNum].setText(Integer.toString(spriteNum));
            }
            return;
        }
        // Generate icon image
        int imageScale = 3;
        Image image = this.createImage(gcSprite.getGridData().length * imageScale, gcSprite.getGridData()[0].length * imageScale);
        Graphics g = image.getGraphics();
        int fore = dataSet.getSpriteColors()[spriteNum];
        Color[] palette = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? TIGlobals.TI_PALETTE_OPAQUE : dataSet.getEcmSpritePalettes()[spriteNum].getColors();
        Color foreColor = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? palette[fore] : null;
        Color transparent = TIGlobals.TI_COLOR_TRANSOPAQUE;
        int[][] spriteGrid = dataSet.getSpriteGrids().get(spriteNum);
        boolean empty = true;
        for (int y = 0; y < spriteGrid.length; y++) {
            for (int x = 0; x < spriteGrid[y].length; x++) {
                int val = spriteGrid[y][x];
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    g.setColor(val == 1 && fore != 0 ? foreColor : transparent);
                }
                else {
                    g.setColor(val != 0 ? palette[val] : transparent);
                }
                g.fillRect(x * imageScale, y * imageScale, imageScale, imageScale);
                if (val != 0) {
                    empty = false;
                }
            }
        }
        g.dispose();
        image = makeImageTransparent(image);
        // Save image
        dataSet.getSpriteImages().put(spriteNum, image);
        mapdMain.setSpriteImage(spriteNum, image);
        // Display a default text if image is empty
        if (empty) {
            jbtnSprite[spriteNum].setIcon(null);
            jbtnSprite[spriteNum].setText(Integer.toString(spriteNum));
        }
        else {
            jbtnSprite[spriteNum].setIcon(new ImageIcon(image));
            jbtnSprite[spriteNum].setText("");
        }
        // Redraw map as requested
        if (redrawMap) {
            mapdMain.redrawCanvas();
        }
    }

    public Color[] getScreenColorPalette() {
        Color[] palette = null;
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
            palette = TIGlobals.TI_PALETTE_OPAQUE;
        }
        else if (colorMode == COLOR_MODE_ECM_2) {
            palette = new Color[16];
            ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
            System.arraycopy(ecmPalettes[0].getColors(), 0, palette, 0, 4);
            System.arraycopy(ecmPalettes[1].getColors(), 0, palette, 4, 4);
            System.arraycopy(ecmPalettes[2].getColors(), 0, palette, 8, 4);
            System.arraycopy(ecmPalettes[3].getColors(), 0, palette, 12, 4);
        }
        else if (colorMode == COLOR_MODE_ECM_3) {
            palette = new Color[16];
            ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
            System.arraycopy(ecmPalettes[0].getColors(), 0, palette, 0, 8);
            System.arraycopy(ecmPalettes[1].getColors(), 0, palette, 8, 8);
        }
        return palette;
    }

    public void setECMPaletteColor(ECMPalette ecmPalette, int index, Color color) {
        charECMPaletteComboBox.setEditable(false);
        spriteECMPaletteComboBox.setEditable(false);
        ecmPalette.setColor(index, color);
        charECMPaletteComboBox.setEditable(true);
        spriteECMPaletteComboBox.setEditable(true);
        gcChar.setPalette(ecmPalette.getColors());
        gcChar.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
        gcChar.redrawCanvas();
        updateCharButtons();
        gcSprite.setPalette(ecmPalette.getColors());
        gcSprite.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
        gcSprite.redrawCanvas();
        updateSpriteButtons();
        updateScreenColorPalette();
        setModified(true);
    }

    public void updatePalettes() {
        ECMPalette[] ecmPalettes = dataSet.getEcmPalettes();
        updateCharPaletteCombo(false);
        updateSpritePaletteCombo(false);
        gcChar.setPalette(ecmPalettes[charECMPaletteComboBox.getSelectedIndex()].getColors());
        gcChar.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
        gcChar.redrawCanvas();
        updateCharButtons();
        gcSprite.setPalette(ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()].getColors());
        gcSprite.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
        gcSprite.redrawCanvas();
        updateSpriteButtons();
        updateScreenColorPalette();
    }

    public void updateScreenColorPalette() {
        mapdMain.setScreenColorPalette(getScreenColorPalette());
    }

    public void updateCharPaletteCombo(boolean setIndex) {
        charECMPaletteComboBox.setEditable(false);
        if (setIndex) {
            charECMPaletteComboBox.setSelectedItem(dataSet.getEcmCharPalettes()[activeChar]);
        }
        charECMPaletteComboBox.setEditable(true);
    }

    public void updateSpritePaletteCombo(boolean setIndex) {
        spriteECMPaletteComboBox.setEditable(false);
        if (setIndex) {
            spriteECMPaletteComboBox.setSelectedItem(dataSet.getEcmSpritePalettes()[activeSprite]);
        }
        spriteECMPaletteComboBox.setEditable(true);
    }
}
