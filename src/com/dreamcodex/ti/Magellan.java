package com.dreamcodex.ti;

import com.dreamcodex.ti.component.*;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.iface.ScreenColorListener;
import com.dreamcodex.ti.iface.UndoRedoListener;
import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.MutableFilter;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.*;
import java.net.URL;
import java.util.*;

import static com.dreamcodex.ti.util.TIGlobals.*;

/**
 * Magellan
 * TI-99/4A graphical map editor
 *
 * @author Howard Kistler
 */

public class Magellan extends JFrame implements Runnable, WindowListener, ActionListener, MouseListener, MouseMotionListener, IconProvider, ScreenColorListener, UndoRedoListener, MapChangeListener {

// Constants -------------------------------------------------------------------------------/

    public static final String VERSION_NUMBER = "4.0.0 beta";

    // Rasmus' special settings
    public static final boolean ISOMETRIC = false;
    public static final boolean ROAD_HUNTER = false;
    public static final boolean ARTRAG = false;
    public static final boolean ANIMATE_SCROLLED_FRAMES = false;
    public static final boolean INVERT_SUPPORTED = true;

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

    private final String FILEEXT = "mag";
    private final String[] FILEEXTS = {FILEEXT};
    private final String XBEXT = "xb";
    private final String[] XBEXTS = {XBEXT, "bas", "txt"};
    private final String ASMEXT = "a99";
    private final String[] ASMEXTS = {ASMEXT, "asm"};
    private final String IMGEXT = "png";
    private final String[] IMGEXTS = {IMGEXT, "gif"};
    private final String BINEXT = "mgb";
    private final String[] BINEXTS = {BINEXT};
    private final String VDPEXT = "vdp";
    private final String[] VDPEXTS = {VDPEXT, "vram", "bin"};
    private final String ANY = "";
    private final String[] ANYS = {ANY};

    private final int FONT_ROWS = 32;
    private final int FONT_COLS = 8;

    private final int SPRITE_COLS = 4;

    private final int MAP_ROWS = 24;
    private final int MAP_COLS = 32;
    private final int MAP_CELL = 8;

    private final Color CLR_CHARS_BASE1 = new Color(232, 232, 232);
    private final Color CLR_CHARS_BASE2 = new Color(196, 196, 196);
    private final Color CLR_CHARS_LOWER = new Color(222, 242, 255);
    private final Color CLR_CHARS_UPPER = new Color(255, 222, 242);

    private final int EDITOR_GRID_SIZE = 192;

// Variables -------------------------------------------------------------------------------/

    protected int colorMode = COLOR_MODE_GRAPHICS_1;

    protected int activeChar = TIGlobals.CUSTOMCHAR;
    protected int lastActiveChar = MapCanvas.NOCHAR;
    protected HashMap<Integer, int[][]> hmCharGrids;
    protected HashMap<Integer, int[][]> hmCharColors;
    protected HashMap<Integer, int[][]> hmDefaultChars;
    protected HashMap<Integer, Image> hmCharImages;
    protected ECMPalette[] ecmCharPalettes = null;
    boolean[] ecmCharTransparency = new boolean[N_CHARS];
    protected int[][] clrSets = new int[COLOR_SETS][2];

    protected int activeSprite = 0;
    protected int lastActiveSprite = MapCanvas.NOCHAR;
    protected HashMap<Integer, int[][]> hmSpriteGrids;
    protected HashMap<Integer, Image> hmSpriteImages;
    protected int[] spriteColors = new int[TIGlobals.MAX_SPRITE + 1];
    protected ECMPalette[] ecmSpritePalettes = null;

    protected ECMPalette[] ecmPalettes = null;

    // Import / export settings

    protected boolean bExportComments = true;
    protected boolean bIncludeCharNumbers = true;
    protected boolean bCurrentMapOnly = false;
    protected boolean bSwapBoth = true;
    protected boolean bSwapImgs = true;
    protected boolean bAllMaps = true;
    protected boolean bWrap = false;
    protected boolean bIncludeSpriteData = false;
    protected boolean bExcludeBlank = false;

    protected int characterSetSize = CHARACTER_SET_BASIC;
    protected int defStartChar = TIGlobals.BASIC_FIRST_CHAR;
    protected int defEndChar = TIGlobals.BASIC_LAST_CHAR;
    protected int defStartSprite = TIGlobals.MIN_SPRITE;
    protected int defEndSprite = TIGlobals.MAX_SPRITE;
    protected int compression = MagellanExportDialog.COMPRESSION_NONE;
    protected int scrollOrientation = SCROLL_ORIENTATION_VERTICAL;
    protected int scrollFrames = 0;

    protected ArrayList<String> recentFiles = new ArrayList<String>();

    // Fields

    private Properties appProperties = new Properties();
    private String openFilePath;              // File to open upon startup
    private String currentDirectory;          // Last used directory
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
    private JMenuItem jmitBitmapColorMode;
    private JMenuItem jmitECM2ColorMode;
    private JMenuItem jmitECM3ColorMode;
    private JRadioButtonMenuItem jmitCharacterSetSuper;

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
            int result = confirmationAction(this, "Confirm save on exit", "Do you want to save your changes first?", true);
            if (result == JOptionPane.YES_OPTION) {
                saveDataFile();
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

        // Read application properties (if exist)
        try {
            File prefsFile = new File(System.getProperty("user.home") + "/Magellan.prefs");
            if (prefsFile.exists()) {
                FileInputStream fis = new FileInputStream(prefsFile);
                appProperties.load(fis);
                fis.close();
            }
        } catch (Exception e) {
            errorAction(this, "Error reading preferences", e.getMessage());
            e.printStackTrace(System.err);
        }

        if (appProperties.getProperty("exportComments") != null) {
            bExportComments = appProperties.getProperty("exportComments").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("includeCharNumbers") != null) {
            bIncludeCharNumbers = appProperties.getProperty("includeCharNumbers").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("currentMapOnly") != null) {
            bCurrentMapOnly = appProperties.getProperty("currentMapOnly").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("wrap") != null) {
            bWrap = appProperties.getProperty("wrap").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("includeSpriteData") != null) {
            bIncludeSpriteData = appProperties.getProperty("includeSpriteData").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("excludeBlank") != null) {
            bExcludeBlank = appProperties.getProperty("excludeBlank").toLowerCase().equals("true");
        }
        if (appProperties.getProperty("characterSetSize") != null) {
            characterSetSize = Integer.parseInt(appProperties.getProperty("characterSetSize"));
        } else if (appProperties.getProperty("expandCharacters") != null) {
            characterSetSize = appProperties.getProperty("expandCharacters").toLowerCase().equals("true") ? CHARACTER_SET_EXPANDED : CHARACTER_SET_BASIC;
        }
        if (appProperties.getProperty("colorMode") != null) {
            colorMode = Integer.parseInt(appProperties.getProperty("colorMode"));
        }
        if (appProperties.getProperty("defStartChar") != null) {
            defStartChar = Integer.parseInt(appProperties.getProperty("defStartChar"));
        }
        if (appProperties.getProperty("defEndChar") != null) {
            defEndChar = Integer.parseInt(appProperties.getProperty("defEndChar"));
        }
        if (appProperties.getProperty("defStartSprite") != null) {
            defStartSprite = Integer.parseInt(appProperties.getProperty("defStartSprite"));
        }
        if (appProperties.getProperty("defEndSprite") != null) {
            defEndSprite = Integer.parseInt(appProperties.getProperty("defEndSprite"));
        }
        if (appProperties.getProperty("compression") != null) {
            compression = Integer.parseInt(appProperties.getProperty("compression"));
        }
        if (appProperties.getProperty("scrollOrientation") != null) {
            scrollOrientation = Integer.parseInt(appProperties.getProperty("scrollOrientation"));
        }
        if (appProperties.getProperty("scrollFrames") != null) {
            scrollFrames = Integer.parseInt(appProperties.getProperty("scrollFrames"));
        }
        currentDirectory = appProperties.getProperty("filePath");
        if (currentDirectory == null || currentDirectory.length() == 0) {
            currentDirectory = ".";
        }
        String recentFileList = appProperties.getProperty("recentFiles");
        if (recentFileList != null) {
            String[] recentFilesArray = recentFileList.split("\\|");
            for (int i = recentFilesArray.length - 1; i >= 0; i--) {
                addRecentFile(recentFilesArray[i]);
            }
        }

        // Create map editor panel (needs to initialise early for the listeners)
        mapdMain = new MapEditor(MAP_COLS, MAP_ROWS, MAP_CELL, this, this);
        mapdMain.fillGrid(TIGlobals.SPACECHAR);
        mapdMain.setBkgrndColor(Globals.CLR_COMPONENTBACK);
        if (appProperties.getProperty("magnif") != null) {
            mapdMain.setViewScale(Integer.parseInt(appProperties.getProperty("magnif")));
        }
        if (appProperties.getProperty("textCursor") != null) {
            mapdMain.setTypeCellOn(appProperties.getProperty("textCursor").toLowerCase().equals("true"));
        }
        if (appProperties.getProperty("showGrid") != null) {
            mapdMain.setShowGrid(appProperties.getProperty("showGrid").toLowerCase().equals("true"));
        }
        if (appProperties.getProperty("gridScale") != null) {
            mapdMain.setGridScale(Integer.parseInt(appProperties.getProperty("gridScale")));
        }
        if (appProperties.getProperty("showPosition") != null) {
            mapdMain.setShowPosIndic(appProperties.getProperty("showPosition").toLowerCase().equals("true"));
        }
        if (appProperties.getProperty("base0Position") != null) {
            mapdMain.setBase0Position(appProperties.getProperty("base0Position").toLowerCase().equals("true"));
        }
        mapdMain.resetUndoManager();

        // Initialize data structures

        // Default characters
        hmDefaultChars = new HashMap<Integer, int[][]>();
        for (int ch = TIGlobals.CHARMAPSTART; ch <= TIGlobals.CHARMAPEND; ch++) {
            hmDefaultChars.put(ch, Globals.getIntGrid(TIGlobals.DEFAULT_TI_CHARS[ch - TIGlobals.CHARMAPSTART], 8));
        }

        // Character structures
        hmCharGrids = new HashMap<Integer, int[][]>();
        if (colorMode == COLOR_MODE_BITMAP) {
            hmCharColors = new HashMap<Integer, int[][]>();
        }
        hmCharImages = new HashMap<Integer, Image>();
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int colorSet = (int) (Math.floor(ch / COLOR_SET_SIZE));
            clrSets[colorSet][Globals.INDEX_CLR_BACK] = 0;
            clrSets[colorSet][Globals.INDEX_CLR_FORE] = 1;
            int[][] emptyGrid = new int[8][8];
            for (int y = 0; y < emptyGrid.length; y++) {
                for (int x = 0; x < emptyGrid[y].length; x++) {
                    if (ch >= TIGlobals.CHARMAPSTART && ch <= TIGlobals.CHARMAPEND) {
                        emptyGrid[y][x] = hmDefaultChars.get(ch)[y][x];
                    } else {
                        emptyGrid[y][x] = 0;
                    }
                }
            }
            hmCharGrids.put(ch, emptyGrid);
            if (colorMode == COLOR_MODE_BITMAP) {
                int[][] emptyColors = new int[8][2];
                for (int y = 0; y < emptyColors.length; y++) {
                    emptyColors[y][0] = 0;
                    emptyColors[y][1] = 1;
                }
                hmCharColors.put(ch, emptyColors);
            }
        }

        // Sprite structures
        hmSpriteGrids = new HashMap<Integer, int[][]>();
        hmSpriteImages = new HashMap<Integer, Image>();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            hmSpriteGrids.put(i, new int[16][16]);
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

        mapdMain.setScreenColorPalette(getScreenColorPalette());
        updateCharButtons();

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    editDefault();
                    // Open command line file
                    if (openFilePath != null) {
                        try {
                            openDataFile(openFilePath);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                        }
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
        JMenuItem jmitOpen = new JMenuItem("Open Map Project");
        jmitOpen.setActionCommand(Globals.CMD_OPEN);
        jmitOpen.addActionListener(this);
        jmenFile.add(jmitOpen);
        JMenu jmenuOpenRecent = new RecentMenu(recentFiles, this);
        jmenFile.add(jmenuOpenRecent);
        JMenuItem jmitSave = new JMenuItem("Save Map Project");
        jmitSave.setActionCommand(Globals.CMD_SAVE);
        jmitSave.addActionListener(this);
        jmenFile.add(jmitSave);
        JMenuItem jmitSaveAs = new JMenuItem("Save Map Project as...");
        jmitSaveAs.setActionCommand(Globals.CMD_SAVEAS);
        jmitSaveAs.addActionListener(this);
        jmenFile.add(jmitSaveAs);
        jmenFile.addSeparator();
        JMenuItem jmitAppend = new JMenuItem("Append Maps");
        jmitAppend.setActionCommand(Globals.CMD_APPEND);
        jmitAppend.addActionListener(this);
        jmenFile.add(jmitAppend);
        jmenFile.addSeparator();
        JMenuItem jmitExit = new JMenuItem("Exit");
        jmitExit.setActionCommand(Globals.CMD_EXIT);
        jmitExit.addActionListener(this);
        jmenFile.add(jmitExit);
        // Add menu
        jMenuBar.add(jmenFile);

        JMenu jmenImport = new JMenu("Import");
        JMenuItem jmitImportChrImgMono = new JMenuItem("Character Image (Mono)");
        jmitImportChrImgMono.setActionCommand(Globals.CMD_MPCIMGMN);
        jmitImportChrImgMono.addActionListener(this);
        jmenImport.add(jmitImportChrImgMono);
        JMenuItem jmitImportChrImgColor = new JMenuItem("Character Image (Color)");
        jmitImportChrImgColor.setActionCommand(Globals.CMD_MPCIMGCL);
        jmitImportChrImgColor.addActionListener(this);
        jmenImport.add(jmitImportChrImgColor);
        JMenuItem jmitImportVramDump = new JMenuItem("VRAM Dump");
        jmitImportVramDump.setActionCommand(Globals.CMD_MPVDP);
        jmitImportVramDump.addActionListener(this);
        jmenImport.add(jmitImportVramDump);
        JMenuItem jmitImportMapImage = new JMenuItem("Map Image");
        jmitImportMapImage.setActionCommand(Globals.CMD_MPMAPIMG);
        jmitImportMapImage.addActionListener(this);
        jmenImport.add(jmitImportMapImage);
        jMenuBar.add(jmenImport);
        JMenuItem jmitImportSpriteImage = new JMenuItem("Sprite Image");
        jmitImportSpriteImage.setActionCommand(Globals.CMD_MPSPRITES);
        jmitImportSpriteImage.addActionListener(this);
        jmenImport.add(jmitImportSpriteImage);
        // Add menu
        jMenuBar.add(jmenImport);

        JMenu jmenExport = new JMenu("Export");
        JMenuItem jmitExportData = new JMenuItem("BASIC Data");
        jmitExportData.setActionCommand(Globals.CMD_XPDATA);
        jmitExportData.addActionListener(this);
        jmenExport.add(jmitExportData);

        JMenuItem jmitExportBasic = new JMenuItem("BASIC Program");
        jmitExportBasic.setActionCommand(Globals.CMD_BASIC);
        jmitExportBasic.addActionListener(this);
        jmenExport.add(jmitExportBasic);

        JMenuItem jmitExportExec = new JMenuItem("XB Program");
        jmitExportExec.setActionCommand(Globals.CMD_XPEXEC);
        jmitExportExec.addActionListener(this);
        jmenExport.add(jmitExportExec);

        JMenuItem jmitExportXB256 = new JMenuItem("XB 256 Program");
        jmitExportXB256.setActionCommand(Globals.CMD_XPXB256);
        jmitExportXB256.addActionListener(this);
        jmenExport.add(jmitExportXB256);

        JMenuItem jmitExportAsm = new JMenuItem("Assembler Data");
        jmitExportAsm.setActionCommand(Globals.CMD_XPASM);
        jmitExportAsm.addActionListener(this);
        jmenExport.add(jmitExportAsm);
        JMenuItem jmitExportScrollMap = new JMenuItem("Assembler Character Transition Data");
        jmitExportScrollMap.setActionCommand(Globals.CMD_XPSCROLL);
        jmitExportScrollMap.addActionListener(this);
        jmenExport.add(jmitExportScrollMap);
        JMenuItem jmitExportBin = new JMenuItem("Binary Data");
        jmitExportBin.setActionCommand(Globals.CMD_XPBIN);
        jmitExportBin.addActionListener(this);
        jmenExport.add(jmitExportBin);
        JMenuItem jmitExportBinMap = new JMenuItem("Binary Map (Current)");
        jmitExportBinMap.setActionCommand(Globals.CMD_XPBINMAP);
        jmitExportBinMap.addActionListener(this);
        jmenExport.add(jmitExportBinMap);
        JMenuItem jmitExportXBDisMer = new JMenuItem("XB Display Merge");
        jmitExportXBDisMer.setActionCommand(Globals.CMD_XPXBDISMER);
        jmitExportXBDisMer.addActionListener(this);
        jmenExport.add(jmitExportXBDisMer);
        jmenExport.addSeparator();
        JMenuItem jmitExportChrImgMono = new JMenuItem("Character Image (Mono)");
        jmitExportChrImgMono.setActionCommand(Globals.CMD_XPCIMGMN);
        jmitExportChrImgMono.addActionListener(this);
        jmenExport.add(jmitExportChrImgMono);
        JMenuItem jmitExportChrImgColor = new JMenuItem("Character Image (Color)");
        jmitExportChrImgColor.setActionCommand(Globals.CMD_XPCIMGCL);
        jmitExportChrImgColor.addActionListener(this);
        jmenExport.add(jmitExportChrImgColor);
        JMenuItem jmitExportMapImg = new JMenuItem("Map Image");
        jmitExportMapImg.setActionCommand(Globals.CMD_XPMAPIMG);
        jmitExportMapImg.addActionListener(this);
        jmenExport.add(jmitExportMapImg);
        if (ROAD_HUNTER) {
            JMenuItem levelItem = new JMenuItem();
            levelItem.setAction(new AbstractAction() {
                {
                    putValue(Action.NAME, "Road Hunter Level");
                }
                public void actionPerformed(ActionEvent e) {
                    int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
                    for (int y = 0; y < mapData.length; y++) {
                        int scr = mapData[mapData.length - 1 - y][0] - 64;
                        if (y % 4 == 0) {
                            System.out.print("       DATA ");
                        }
                        System.out.print("SCR0" + (scr < 10 ? "0" : "") + scr + (y % 4 != 3 ? "," : ""));
                        if (y % 4 == 3) {
                            System.out.println();
                        }
                    }
                }
            });
            jmenExport.add(levelItem);
        }
        if (ARTRAG) {
            JMenuItem artragItem = new JMenuItem();
            artragItem.setAction(new AbstractAction() {
                {
                    putValue(Action.NAME, "Artrag Scroll File");
                }
                public void actionPerformed(ActionEvent e) {
                    MagellanImportExport mio = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                    File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, ASMEXTS, "Assembler Source Files");
                    if (file != null) {
                        try {
                            mio.writeArtragFile(file, false, true, false, 8);
                        } catch (Exception e1) {
                            errorAction(null, "Error exporting file", e1.getMessage());
                        }
                    }
                }
            });
            jmenExport.add(artragItem);
        }
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

        if (ISOMETRIC) {
            JMenuItem jmitAnalyzeIsoCharTrans = new JMenuItem();
            jmitAnalyzeIsoCharTrans.setAction(
                    new AbstractAction() {
                        {
                            putValue(Action.NAME, "Analyze Isometric Character Transitions");
                        }
                        public void actionPerformed(ActionEvent e) {
                            Map<String, Integer> transMap = new HashMap<String, Integer>();
                            int[][] mapData = mapdMain.getMapData(mapdMain.getCurrentMapId());
                            for (int y = 0; y < mapData.length - 1; y++) {
                                for (int x = 0; x < mapData[y].length - 2; x++) {
                                    String key =
                                        mapData[y][x] + "-" + mapData[y][x + 1] + "-" + mapData[y][x + 2] + "-" +
                                        mapData[y + 1][x] + "-" + mapData[y + 1][x + 1] + "-" + mapData[y + 1][x + 2];
                                    Integer count = transMap.get(key);
                                    transMap.put(key, count == null ? 1 : count + 1);
                                }
                            }
                            for (String key : transMap.keySet()) {
                                System.out.println(key + ": " + transMap.get(key));
                            }
                            informationAction(Magellan.this, "Analyze Isometric Character Transitions", "Total transitions: " + Integer.toString(transMap.size()));
                        }
                    }
            );
            jmenTools.add(jmitAnalyzeIsoCharTrans);
        }
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

        ButtonGroup characterSetSizeButtonGroup = new ButtonGroup();

        JRadioButtonMenuItem jmitCharacterSetBasic = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_BASIC], characterSetSize == CHARACTER_SET_BASIC);
        characterSetSizeButtonGroup.add(jmitCharacterSetBasic);
        jmitCharacterSetBasic.setActionCommand(Globals.CMD_BASICCHARSETSIZE);
        jmitCharacterSetBasic.addActionListener(this);
        jmenOptions.add(jmitCharacterSetBasic);

        JRadioButtonMenuItem jmitCharacterSetExpanded = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_EXPANDED], characterSetSize == CHARACTER_SET_EXPANDED);
        characterSetSizeButtonGroup.add(jmitCharacterSetExpanded);
        jmitCharacterSetExpanded.setActionCommand(Globals.CMD_EXPANDEDCHARSETSIZE);
        jmitCharacterSetExpanded.addActionListener(this);
        jmenOptions.add(jmitCharacterSetExpanded);

        jmitCharacterSetSuper = new JRadioButtonMenuItem(CHARACTER_SET_SIZES[CHARACTER_SET_SUPER], characterSetSize == CHARACTER_SET_SUPER);
        characterSetSizeButtonGroup.add(jmitCharacterSetSuper);
        jmitCharacterSetSuper.setActionCommand(Globals.CMD_SUPERCHARSETSIZE);
        jmitCharacterSetSuper.addActionListener(this);
        jmenOptions.add(jmitCharacterSetSuper);

        jmenOptions.addSeparator();

        ButtonGroup colorModeButtonGroup = new ButtonGroup();
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
        jlblCharInt = getLabel("", JLabel.CENTER);
        jlblCharInt.setPreferredSize(Globals.DM_TEXT);
        jpnlCharTools.add(jlblCharInt, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlCharTools.add(getToolButton(Globals.CMD_SHIFTU_CHR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblCharHex = getLabel("", JLabel.CENTER);
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
        JButton copyButton = getToolButton(Globals.CMD_COPY_CHR, "Copy");
        copyButton.addActionListener(this);
        jpnlCharToolbar.add(copyButton);
        JButton pasteButton = getToolButton(Globals.CMD_PASTE_CHR, "Paste and set");
        pasteButton.addActionListener(this);
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
            int rowNum = (int) (Math.floor(ch / 8));
            jbtnChar[ch] = getDockButton(((ch >= TIGlobals.CHARMAPSTART) && (ch <= TIGlobals.CHARMAPEND) ? "" + TIGlobals.CHARMAP[ch - TIGlobals.CHARMAPSTART] : "?"), Globals.CMD_EDIT_CHR + ch, TIGlobals.TI_PALETTE_OPAQUE[clrSets[rowNum][Globals.INDEX_CLR_BACK]]);
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
        jlblSpriteInt = getLabel("", JLabel.CENTER);
        jlblSpriteInt.setPreferredSize(Globals.DM_TEXT);
        jpnlSpriteTools.add(jlblSpriteInt, new GridBagConstraints(3, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jpnlSpriteTools.add(getToolButton(Globals.CMD_SHIFTU_SPR, "Shift Up", Globals.CLR_BUTTON_SHIFT), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insets, 2, 2));
        jlblSpriteHex = getLabel("", JLabel.CENTER);
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
        JButton sprCopyButton = getToolButton(Globals.CMD_COPY_SPR, "Copy");
        sprCopyButton.addActionListener(this);
        jpnlSpriteToolbar.add(sprCopyButton);
        JButton sprPasteButton = getToolButton(Globals.CMD_PASTE_SPR, "Paste and set");
        sprPasteButton.addActionListener(this);
        jpnlSpriteToolbar.add(sprPasteButton);
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
        mapdMain.setScreenColorPalette(getScreenColorPalette());
    }

    protected void buildECMPalettes() {
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
        if (ecmCharPalettes == null) {
            ecmCharPalettes = new ECMPalette[TIGlobals.MAX_CHAR + 1];
        }
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            ecmCharPalettes[i] = ecmPalettes[0];
        }
        if (ecmSpritePalettes == null) {
            ecmSpritePalettes = new ECMPalette[TIGlobals.MAX_SPRITE + 1];
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
            charECMPaletteComboBox = new ECMPaletteComboBox(ecmPalettes, gcChar.getColorDraw(), gcChar.getColorBack(), this, true);
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
            spriteECMPaletteComboBox = new ECMPaletteComboBox(ecmPalettes, gcSprite.getColorDraw(), gcSprite.getColorBack(), this, false);
            jPanel.add(spriteECMPaletteComboBox, BorderLayout.CENTER);
        }
        return jPanel;
    }

    protected JPanel buildCharacterDock(JPanel jPanel) {
        int dockFontRows = getCharacterSetSize() / FONT_COLS;
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
                if (characterSetSize >= CHARACTER_SET_EXPANDED) {
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
                if (characterSetSize >= CHARACTER_SET_EXPANDED) {
                    if (col >= FONT_COLS) {
                        jPanel.add(getLabel("U" + ucount + " ", JLabel.RIGHT, CLR_CHARS_UPPER));
                        ucount++;
                        col = 0;
                    }
                    jPanel.add(jbtnChar[c]);
                    col++;
                }
            } else {
                if (characterSetSize >= CHARACTER_SET_SUPER) {
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
        int dockSpriteRows = getSpriteSetSize() / SPRITE_COLS;
        if (jPanel != null) {
            jPanel.removeAll();
            jPanel.setLayout(new GridLayout(dockSpriteRows , 4));
        } else {
            jPanel = getPanel(new GridLayout(dockSpriteRows, 4));
        }
        for (int i = 0; i < getSpriteSetSize(); i++) {
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

    protected Image getImage(String name) {
        return Toolkit.getDefaultToolkit().getImage(name);
    }

    protected ImageIcon getIcon(String name) {
        if (name.endsWith("spr") || name.endsWith("chr")) {
            name = name.substring(0, name.length() - 3);
        }
        URL imageURL = getClass().getResource("images/icon_" + name + "_mono.png");
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(imageURL));
    }

    protected JButton getToolButton(String buttonkey, String tooltip, Color bgcolor) {
        JButton jbtnTool = new JButton(getIcon(buttonkey));
        jbtnTool.setActionCommand(buttonkey);
        jbtnTool.addActionListener(this);
        jbtnTool.setToolTipText(tooltip);
        jbtnTool.setMargin(new Insets(0, 0, 0, 0));
        jbtnTool.setBackground(bgcolor);
        jbtnTool.setPreferredSize(Globals.DM_TOOL);
        return jbtnTool;
    }

    protected JButton getToolButton(String buttonkey, String tooltip) {
        return getToolButton(buttonkey, tooltip, Globals.CLR_BUTTON_NORMAL);
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

    protected JLabel getLabel(String text, int align, Color clrback) {
        JLabel jlblRtn = new JLabel(text, align);
        jlblRtn.setOpaque(true);
        jlblRtn.setBackground(clrback);
        return jlblRtn;
    }

    protected JLabel getLabel(String text, int align) {
        return getLabel(text, align, Globals.CLR_COMPONENTBACK);
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
                hmCharGrids.put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_CLEAR_SPR)) {
                gcSprite.clearGrid();
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_CHR)) {
                gcChar.fillGrid();
                hmCharGrids.put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_SPR)) {
                gcSprite.fillGrid();
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
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
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPH_SPR)) {
                gcSprite.setGrid(Globals.flipGrid(gcSprite.getGridData(), false));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_CHR)) {
                gcChar.setGridAndColors(Globals.flipGrid(gcChar.getGridData(), true), colorMode == COLOR_MODE_BITMAP ? Globals.flipGrid(gcChar.getGridColors(), true) : null);
                hmCharGrids.put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_SPR)) {
                gcSprite.setGrid(Globals.flipGrid(gcSprite.getGridData(), true));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_CHR)) {
                gcChar.setGrid(Globals.rotateGrid(gcChar.getGridData(), true));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_SPR)) {
                gcSprite.setGrid(Globals.rotateGrid(gcSprite.getGridData(), true));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_CHR)) {
                gcChar.setGrid(Globals.rotateGrid(gcChar.getGridData(), false));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_SPR)) {
                gcSprite.setGrid(Globals.rotateGrid(gcSprite.getGridData(), false));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_CHR)) {
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0 || colorMode != COLOR_MODE_BITMAP) {
                    gcChar.setGrid(Globals.invertGrid(gcChar.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                }
                else {
                    gcChar.setGridAndColors(Globals.invertGrid(gcChar.getGridData(), 1), Globals.flipGrid(gcChar.getGridColors(), false));
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_SPR)) {
                gcSprite.setGrid(Globals.invertGrid(gcSprite.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_CHR)) {
                gcChar.setGrid(Globals.cycleGridUp(gcChar.getGridData()));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    gcChar.setColors(Globals.cycleGridUp(gcChar.getGridColors()));
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_SPR)) {
                gcSprite.setGrid(Globals.cycleGridUp(gcSprite.getGridData()));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateCharButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_CHR)) {
                gcChar.setGrid(Globals.cycleGridDown(gcChar.getGridData()));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    gcChar.setColors(Globals.cycleGridDown(gcChar.getGridColors()));
                    hmCharColors.put(activeChar, gcChar.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_SPR)) {
                gcSprite.setGrid(Globals.cycleGridDown(gcSprite.getGridData()));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_CHR)) {
                gcChar.setGrid(Globals.cycleGridLeft(gcChar.getGridData()));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_SPR)) {
                gcSprite.setGrid(Globals.cycleGridLeft(gcSprite.getGridData()));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_CHR)) {
                gcChar.setGrid(Globals.cycleGridRight(gcChar.getGridData()));
                hmCharGrids.put(activeChar, gcChar.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_SPR)) {
                gcSprite.setGrid(Globals.cycleGridRight(gcSprite.getGridData()));
                hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_LOOK)) {
                mapdMain.setLookModeOn(!mapdMain.isLookModeOn());
                jbtnLook.setBackground((mapdMain.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
            } else if (command.equals(Globals.CMD_NEW)) {
                int userResponse = confirmationAction(this, "Confirm New Project", "This will delete all current data.\n\rAre you sure?", false);
                if (userResponse == JOptionPane.YES_OPTION) {
                    newProject();
                    setAppTitle();
                    editDefault();
                }
            } else if (command.equals(Globals.CMD_OPEN)) {
                openDataFile(null);
            } else if (command.startsWith(Globals.CMD_OPEN_RECENT)) {
                openDataFile(command.substring(Globals.CMD_OPEN_RECENT.length()));
            } else if (command.equals(Globals.CMD_SAVE)) {
                saveDataFile();
            } else if (command.equals(Globals.CMD_SAVEAS)) {
                saveDataFileAs();
            } else if (command.equals(Globals.CMD_APPEND)) {
                appendDataFile();
            } else if (command.equals(Globals.CMD_LOADDEFS)) {
                loadDefaultCharacters();
            } else if (command.equals(Globals.CMD_MPCIMGMN)) {
                importCharImage(false);
            } else if (command.equals(Globals.CMD_MPCIMGCL)) {
                importCharImage(true);
            } else if (command.equals(Globals.CMD_MPVDP)) {
                importVramDump();
            } else if (command.equals(Globals.CMD_MPMAPIMG)) {
                importMapImage();
            } else if (command.equals(Globals.CMD_MPSPRITES)) {
                importSpriteImage();
            } else if (command.equals(Globals.CMD_XPDATA)) {
                exportDataFile(Globals.BASIC_DATA);
            } else if (command.equals(Globals.CMD_XPXB256)) {
                exportDataFile(Globals.XB256_PROGRAM);
            } else if (command.equals(Globals.CMD_XPEXEC)) {
                exportDataFile(Globals.XB_PROGRAM);
            } else if (command.equals(Globals.CMD_BASIC)) {
                exportDataFile(Globals.BASIC_PROGRAM);
            } else if (command.equals(Globals.CMD_XPASM)) {
                exportAssemblerFile();
            } else if (command.equals(Globals.CMD_XPSCROLL)) {
                exportScrollFile();
            } else if (command.equals(Globals.CMD_XPBIN)) {
                exportBinaryFile();
            } else if (command.equals(Globals.CMD_XPBINMAP)) {
                exportBinaryMapFile();
            } else if (command.equals(Globals.CMD_XPXBDISMER)) {
                exportXBDisplayMerge();
            } else if (command.equals(Globals.CMD_XPCIMGMN)) {
                exportCharImage(false);
            } else if (command.equals(Globals.CMD_XPCIMGCL)) {
                exportCharImage(true);
            } else if (command.equals(Globals.CMD_XPMAPIMG)) {
                exportMapImage();
            } else if (command.startsWith(Globals.CMD_EDIT_CHR)) {
                int oldActiveChar = activeChar;
                activeChar = Integer.parseInt(command.substring(Globals.CMD_EDIT_CHR.length()));
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
                    swapCharacters(activeChar, oldActiveChar, 0, true, true, true);
                }
                if (hmCharGrids.get(activeChar) == null) {
                    gcChar.clearGrid();
                    hmCharGrids.put(activeChar, gcChar.getGridData());
                    if (colorMode == COLOR_MODE_BITMAP) {
                        hmCharColors.put(activeChar, gcChar.getGridColors());
                    }
                }
                gcChar.resetUndoRedo();
                gcChar.setGridAndColors(hmCharGrids.get(activeChar), colorMode == COLOR_MODE_BITMAP ? hmCharColors.get(activeChar) : null);
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    int cset = (int) (Math.floor(activeChar / 8));
                    gcChar.setColorBack(clrSets[cset][Globals.INDEX_CLR_BACK]);
                    gcChar.setColorDraw(clrSets[cset][Globals.INDEX_CLR_FORE]);
                    for (int i = 0; i < charColorDockButtons.length; i++) {
                        charColorDockButtons[i].setText(i == gcChar.getColorBack() ? "B" : (i == gcChar.getColorDraw() ? "F" : ""));
                    }
                }
                else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    gcChar.setPalette(ecmCharPalettes[activeChar].getColors());
                    charECMPaletteComboBox.setSelectedItem(ecmCharPalettes[activeChar]);
                }
                gcChar.setECMTransparency(ecmCharTransparency[activeChar]);
                gcChar.redrawCanvas();
                mapdMain.setActiveChar(activeChar);
                mapdMain.setCloneModeOn(false);
                updateComponents();
            } else if (command.startsWith(Globals.CMD_EDIT_SPR)) {
                int oldActiveSprite = activeSprite;
                activeSprite = Integer.parseInt(command.substring(Globals.CMD_EDIT_SPR.length()));
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) != 0) {
                    // Swap first TODO
                    int[][] tempGrid = Globals.cloneGrid(hmSpriteGrids.get(activeSprite));
                    Globals.copyGrid(hmSpriteGrids.get(oldActiveSprite), hmSpriteGrids.get(activeSprite));
                    Globals.copyGrid(tempGrid, hmSpriteGrids.get(oldActiveSprite));
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
                if (hmSpriteGrids.get(activeSprite) == null) {
                    gcSprite.clearGrid();
                    hmSpriteGrids.put(activeSprite, gcSprite.getGridData());
                }
                gcSprite.resetUndoRedo();
                gcSprite.setGrid(hmSpriteGrids.get(activeSprite));
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
                int[][] charColors = hmCharColors != null ? hmCharColors.get(activeChar) : null;
                if (colorMode == COLOR_MODE_BITMAP && hexString.length() == 32) {
                    charColors = Globals.parseColorHexString(hexString.substring(16));
                    hmCharColors.put(activeChar, charColors);
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
                    ecmCharPalettes[activeChar] = ecmPalettes[palette];
                    charECMPaletteComboBox.setSelectedItem(ecmCharPalettes[activeChar]);
                }
                hmCharGrids.put(activeChar, charGrid);
                gcChar.setGridAndColors(hmCharGrids.get(activeChar), colorMode == COLOR_MODE_BITMAP ? charColors : null);
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
                    ecmSpritePalettes[activeSprite] = ecmPalettes[palette];
                    spriteECMPaletteComboBox.setSelectedItem(ecmSpritePalettes[activeSprite]);
                }
                hmSpriteGrids.put(activeSprite, spriteGrid);
                gcSprite.setGrid(hmSpriteGrids.get(activeSprite));
                updateCharButton(activeSprite);
            } else if (command.equals(Globals.CMD_COPY_CHR)) {
                jtxtChar.selectAll();
                jtxtChar.copy();
            } else if (command.equals(Globals.CMD_COPY_SPR)) {
                jtxtSprite.selectAll();
                jtxtSprite.copy();
            } else if (command.equals(Globals.CMD_PASTE_CHR)) {
                jtxtChar.selectAll();
                jtxtChar.paste();
                jbtnUpdateChar.doClick();
            } else if (command.equals(Globals.CMD_PASTE_SPR)) {
                jtxtSprite.selectAll();
                jtxtSprite.paste();
                jbtnUpdateSprite.doClick();
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
                        int cset = (int) (Math.floor(activeChar / 8));
                        clrSets[cset][Globals.INDEX_CLR_FORE] = index;
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
                        ECMPalette ecmPalette = ecmPalettes[charECMPaletteComboBox.getSelectedIndex()];
                        int index2 = charECMPaletteComboBox.getIndexBack();
                        for (int i = 0; i < ecmCharPalettes.length; i++) {
                            if (ecmCharPalettes[i] == ecmPalette) {
                                Globals.swapGridValues(hmCharGrids.get(i), index, index2);
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
                    spriteColors[activeSprite] = index;
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
                        ECMPalette ecmPalette = ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()];
                        int index2 = spriteECMPaletteComboBox.getIndexBack();
                        for (int i = 0; i < ecmSpritePalettes.length; i++) {
                            if (ecmSpritePalettes[i] == ecmPalette) {
                                Globals.swapGridValues(hmSpriteGrids.get(i), index, index2);
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
                        int cset = (int) (Math.floor(activeChar / 8));
                        clrSets[cset][Globals.INDEX_CLR_BACK] = index;
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
                        ECMPalette ecmPalette = ecmPalettes[charECMPaletteComboBox.getSelectedIndex()];
                        int index2 = charECMPaletteComboBox.getIndexFore();
                        for (int i = 0; i < ecmCharPalettes.length; i++) {
                            if (ecmCharPalettes[i] == ecmPalette) {
                                Globals.swapGridValues(hmCharGrids.get(i), index, index2);
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
                        ECMPalette ecmPalette = ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()];
                        int index2 = spriteECMPaletteComboBox.getIndexFore();
                        for (int i = 0; i < ecmSpritePalettes.length; i++) {
                            if (ecmSpritePalettes[i] == ecmPalette) {
                                Globals.swapGridValues(hmSpriteGrids.get(i), index, index2);
                            }
                        }
                        Color color = ecmPalette.getColor(index);
                        Color color2 = ecmPalette.getColor(index2);
                        setECMPaletteColor(ecmPalette, index2, color);
                        setECMPaletteColor(ecmPalette, index, color2);
                    }
                }
            } else if (command.equals(Globals.CMD_PALSELECT_CHR)) {
                ECMPalette ecmPalette = ecmPalettes[charECMPaletteComboBox.getSelectedIndex()];
                ecmCharPalettes[activeChar] = ecmPalette;
                gcChar.setPalette(ecmPalette.getColors());
                gcChar.redrawCanvas();
                updateCharButton(activeChar);
            } else if (command.equals(Globals.CMD_PALSELECT_SPR)) {
                ECMPalette ecmPalette = ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()];
                ecmSpritePalettes[activeSprite] = ecmPalette;
                gcSprite.setPalette(ecmPalette.getColors());
                gcSprite.redrawCanvas();
                updateSpriteButton(activeSprite);
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_CHR.length()));
                ECMPalette ecmPalette = ecmPalettes[charECMPaletteComboBox.getSelectedIndex()];
                // Choose a new palette color
                Color color = ECMColorChooser.showDialog(this, "Select Color", ecmPalette.getColor(index));
                if (color != null) {
                    setECMPaletteColor(ecmPalette, index, color);
                }
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_SPR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_SPR.length()));
                ECMPalette ecmPalette = ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()];
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
            }
            else if (command.equals(Globals.CMD_SHOWPOS)) {
                mapdMain.toggleShowPosIndic();
            } else if (command.equals(Globals.CMD_BASE0POS)) {
                mapdMain.toggleBase0Position();
            } else if (command.equals(Globals.CMD_BASICCHARSETSIZE)) {
                characterSetSize = CHARACTER_SET_BASIC;
                jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
                jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
            } else if (command.equals(Globals.CMD_EXPANDEDCHARSETSIZE)) {
                characterSetSize = CHARACTER_SET_EXPANDED;
                jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
                jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
            } else if (command.equals(Globals.CMD_SUPERCHARSETSIZE)) {
                characterSetSize = CHARACTER_SET_SUPER;
                jpnlCharacterDock = buildCharacterDock(jpnlCharacterDock);
                jpnlSpriteDock = buildSpriteDock(jpnlSpriteDock);
            } else if (command.equals(Globals.CMD_GRAPHICSCOLORMODE)) {
                int oldColorMode = colorMode;
                colorMode = COLOR_MODE_GRAPHICS_1;
                buildColorDocks();
                // Characters
                if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
                    limitCharGrids(2, colorMode == COLOR_MODE_ECM_2 ? 4 : 8);
                }
                gcChar.setColorMode(colorMode, TIGlobals.TI_PALETTE_OPAQUE);
                int[] clrSet = clrSets[activeChar / 8];
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
                gcSprite.setColorDraw(spriteColors[activeSprite]);
                for (int i = 0; i < spriteColorDockButtons.length; i++) {
                    spriteColorDockButtons[i].setText(i == gcSprite.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
                }
                updateSpriteButtons();
            } else if (command.equals(Globals.CMD_BITMAPCOLORMODE)) {
                int oldColorMode = colorMode;
                colorMode = COLOR_MODE_BITMAP;
                buildColorDocks();
                // Characters
                hmCharColors = new HashMap<Integer, int[][]>();
                for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
                    int[][] emptyColors = new int[8][2];
                    for (int y = 0; y < emptyColors.length; y++) {
                        int[] clrSet = clrSets[ch / 8];
                        if (oldColorMode == COLOR_MODE_GRAPHICS_1) {
                            emptyColors[y][Globals.INDEX_CLR_BACK] = clrSet[Globals.INDEX_CLR_BACK];
                            emptyColors[y][Globals.INDEX_CLR_FORE] = clrSet[Globals.INDEX_CLR_FORE];
                        }
                        else {
                            int[][] charGrid = hmCharGrids.get(ch);
                            if (charGrid != null) {
                                int[] row = charGrid[y];
                                ECMPalette ecmPalette = ecmCharPalettes[ch];
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
                    hmCharColors.put(ch, emptyColors);
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
                gcSprite.setColorDraw(spriteColors[activeSprite]);
                for (int i = 0; i < spriteColorDockButtons.length; i++) {
                    spriteColorDockButtons[i].setText(i == gcSprite.getColorBack() ? "B" : (i == gcSprite.getColorDraw() ? "F" : ""));
                }
                updateSpriteButtons();
            } else if (command.equals(Globals.CMD_ECM2COLORMODE)) {
                if (colorMode == COLOR_MODE_ECM_3) {
                    limitCharGrids(4, 8);
                    limitSpriteGrids(4, 8);
                }
                colorMode = COLOR_MODE_ECM_2;
                buildColorDocks();
                gcChar.setColorMode(colorMode, ecmCharPalettes[activeChar].getColors());
                updateCharButtons();
                gcSprite.setColorMode(colorMode, ecmSpritePalettes[activeSprite].getColors());
                updateSpriteButtons();
            } else if (command.equals(Globals.CMD_ECM3COLORMODE)) {
                for (int i = 0; i < spriteColors.length; i++) {
                    if (spriteColors[i] > 7) {
                        spriteColors[i] >>= 1;
                    }
                }
                colorMode = COLOR_MODE_ECM_3;
                buildColorDocks();
                gcChar.setColorMode(colorMode, ecmCharPalettes[activeChar].getColors());
                updateCharButtons();
                gcSprite.setColorMode(colorMode, ecmSpritePalettes[activeSprite].getColors());
                updateSpriteButtons();
            } else if (command.equals(Globals.CMD_VIEW_CHAR_LAYER)) {
                mapdMain.setViewCharLayer(!mapdMain.getViewCharLayer());
            } else if (command.equals(Globals.CMD_VIEW_SPRITE_LAYER)) {
                mapdMain.setViewSpriteLayer(!mapdMain.getViewSpriteLayer());
            } else if (command.equals(Globals.CMD_TRANSPARENCY)) {
                ecmCharTransparency[activeChar] = jchkTransparency.isSelected();
                gcChar.setECMTransparency(ecmCharTransparency[activeChar]);
                gcChar.redrawCanvas();
                updateCharButton(activeChar, true);
            } else if (command.equals(Globals.CMD_ABOUT)) {
                informationAction(
                    this,
                    "About Magellan",
                    "Magellan, version " + VERSION_NUMBER + ".\n\n" +
                    "\u00a9 2010 Howard Kistler/Dream Codex Retrogames (www.dreamcodex.com)\n\n" +
                    "Magellan is free software maintained by the TI-99/4A community.\n\n" +
                    "Modified by:\n\u2022 retroclouds (2011)\n\u2022 sometimes99er (2013)\n\u2022 David Vella (2016)\n\u2022 Rasmus Moustgaard (2013 - ongoing)\n\n" +
                    "Source code available from: https://github.com/Rasmus-M/magellan\n\n" +
                    "Java runtime version: " + System.getProperty("java.version")
                );
            }
            mapdMain.redrawCanvas();
        } catch (Exception e) {
            errorAction(this, "Program error", e.getMessage());
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

    private void setAppTitle() {
        this.setTitle(APPTITLE + (mapDataFile != null ? ": " : "") + (isModified() ? " *" : "") + (mapDataFile != null ? mapDataFile.getName() : ""));
    }

// Tool Methods -------------------------------------------------------------/

    protected void showSwapCharactersDialog() {
        CharacterSwapDialog swapper = new CharacterSwapDialog(this, this, bSwapBoth, bSwapImgs, bAllMaps, getCharacterSetStart(), getCharacterSetEnd(), activeChar);
        if (swapper.isOkay()) {
            swapCharacters(swapper.getBaseChar(), swapper.getSwapChar(), swapper.getRepeatCount(), swapper.doSwapChars(), swapper.doSwapImages(), swapper.doAllMaps());
            bSwapBoth = swapper.doSwapChars();
            bSwapImgs = swapper.doSwapImages();
            bAllMaps = swapper.doAllMaps();
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
                    int[][] charGrid = hmCharGrids.get(swapChar);
                    hmCharGrids.put(swapChar, hmCharGrids.get(baseChar));
                    hmCharGrids.put(baseChar, charGrid);
                    if (colorMode == COLOR_MODE_BITMAP) {
                        int[][] charColors = hmCharColors.get(swapChar);
                        hmCharColors.put(swapChar, hmCharColors.get(baseChar));
                        hmCharColors.put(baseChar, charColors);
                    }
                    else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
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

    protected void analyzeCharUsage() {
        if (charUsageDialog != null && charUsageDialog.isVisible()) {
            charUsageDialog.transferFocus();
        }
        else {
            charUsageDialog = new AnalyzeCharUsageDialog(this, mapdMain, hmCharImages, getCharacterSetEnd());
        }
    }

    protected void analyzeCharTrans() {
        if (charTransDialog != null && charTransDialog.isVisible()) {
            charTransDialog.transferFocus();
        }
        else {
            charTransDialog = new AnalyzeCharTransDialog(this, mapdMain, hmCharImages, hmCharGrids, hmCharColors, clrSets, colorMode);
        }
    }

// File Handling Methods -------------------------------------------------------------------/

    protected void openDataFile(String filePath) throws IOException {
        File file;
        if (filePath != null) {
            file = new File(filePath);
        }
        else {
            file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, FILEEXTS, "Map Data Files");
        }
        if (file != null && file.exists()) {
            // Check color mode and character range
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                String line;
                int linesToProcess = 2;
                do {
                    line = br.readLine();
                    if (line != null) {
                        if (line.startsWith(Globals.KEY_COLOR_MODE)) {
                            int fileColorMode = Integer.parseInt(line.substring(Globals.KEY_COLOR_MODE.length()));
                            if (fileColorMode != colorMode) {
                                int reply = confirmationAction(this, "Confirm Color Mode Change", "This file was saved in " + COLOR_MODES[fileColorMode] + ". Do you want to switch to that mode before loading the file?", true);
                                if (reply == JOptionPane.YES_OPTION) {
                                    switch (fileColorMode) {
                                        case COLOR_MODE_GRAPHICS_1:
                                            jmitGraphicsColorMode.doClick();
                                            break;
                                        case COLOR_MODE_BITMAP:
                                            jmitBitmapColorMode.doClick();
                                            break;
                                        case COLOR_MODE_ECM_2:
                                            jmitECM2ColorMode.doClick();
                                            break;
                                        case COLOR_MODE_ECM_3:
                                            jmitECM3ColorMode.doClick();
                                            break;
                                    }
                                }
                                else if (reply == JOptionPane.CANCEL_OPTION) {
                                    return;
                                }
                            }
                            linesToProcess--;
                        } else if (line.startsWith(Globals.KEY_CHARRANG)) {
                            int endChar = Integer.parseInt(line.substring(Globals.KEY_CHARRANG.length()).split("\\|")[1]);
                            if (endChar == SUPER_LAST_CHAR && getCharacterSetEnd() != endChar) {
                                jmitCharacterSetSuper.doClick();
                            }
                            linesToProcess--;
                        }
                    }
                } while (line != null && linesToProcess > 0);
            } catch (Exception e) {
                errorAction(this, "Error determining color mode or character range", e.getMessage());
                return;
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        errorAction(this, "Error opening data file", e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
            }
            newProject();
            mapDataFile = file;
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.readDataFile(mapDataFile);
            addRecentFile(file.getAbsolutePath());
        }
        for (int cn = 0; cn < jbtnChar.length; cn++) {
            jbtnChar[cn].setIcon((ImageIcon) null);
            jbtnChar[cn].setText((cn >= TIGlobals.CHARMAPSTART) && (cn < (TIGlobals.CHARMAPSTART + TIGlobals.CHARMAP.length)) ? "" + TIGlobals.CHARMAP[cn - TIGlobals.CHARMAPSTART] : "?");
        }
        mapdMain.goToMap(0);
        updateCharButtons();
        updateSpriteButtons();
        updateComponents();
        editDefault();
        mapdMain.setScreenColorPalette(getScreenColorPalette());
        setModified(false);
    }

    protected void saveDataFile() throws IOException {
        if (mapDataFile != null && mapDataFile.isFile()) {
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.writeDataFile(mapDataFile, characterSetSize);
            setModified(false);
            updateComponents();
        }
        else {
            saveDataFileAs();
        }
    }

    protected void saveDataFileAs() throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, FILEEXTS, "Map Data Files");
        if (file != null) {
            if (!file.getAbsolutePath().toLowerCase().endsWith("." + FILEEXT)) {
                file = new File(file.getAbsolutePath() + "." + FILEEXT);
            }
            mapDataFile = file;
            setModified(false);
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.writeDataFile(mapDataFile, characterSetSize);
            updateComponents();
            addRecentFile(file.getAbsolutePath());
        }
    }

    protected void appendDataFile() throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, FILEEXTS, "Map Data Files");
        if (file != null) {
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.readAppendDataFile(file);
        }
        updateComponents();
    }

    protected void loadDefaultCharacters() {
        updateCharButtons();
        updateCharButton(activeChar);
        updateComponents();
    }

    protected void importCharImage(boolean isColor) {
        File file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            Image charImg = getImage(file.getAbsolutePath());
            BufferedImage buffImg = new BufferedImage(8 * 8, 8 * 32, BufferedImage.TYPE_3BYTE_BGR);
            if (isColor) {
                Graphics2D g2d = ((Graphics2D) (buffImg.getGraphics()));
                g2d.setColor(TIGlobals.TI_COLOR_TRANSOPAQUE);
                g2d.fillRect(0, 0, 8 * 8, 8 * 32);
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.drawImage(charImg, 0, 0, this);
                g2d.setComposite(AlphaComposite.Src);
            } else {
                buffImg.getGraphics().drawImage(charImg, 0, 0, this);
            }
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            if (isColor) {
                magIO.readCharImageColor(buffImg);
            } else {
                magIO.readCharImageMono(buffImg);
            }
            if (colorMode == Magellan.COLOR_MODE_ECM_2 || colorMode == Magellan.COLOR_MODE_ECM_3) {
                charECMPaletteComboBox.setEditable(false);
                charECMPaletteComboBox.setSelectedItem(ecmCharPalettes[activeChar]);
                charECMPaletteComboBox.setEditable(true);
            }
            updateCharButtons();
            setModified(true);
        }
        updateComponents();
    }

    protected void importVramDump() throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, VDPEXTS, "VRAM Dump Files", false);
        int charOffset = 0;
        int mapOffset = 0;
        int colorOffset = 0;
        int spriteOffset = 0;
        int spriteAttrOffset = 0;
        if (file != null) {
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            if (file.length() == 0x4008) {
                boolean bitmapMode = false;
                boolean textMode = false;
                int textColor = 0;
                int screenColor = 0;
                try {
                    FileInputStream fib = new FileInputStream(file);
                    fib.skip(0x4000);
                    int[] vdpRegs = new int[8];
                    for (int i = 0; i < 8; i++) {
                        vdpRegs[i] = fib.read();
                    }
                    bitmapMode = (vdpRegs[0] & 0x02) != 0;
                    textMode = (vdpRegs[1] & 0x10) != 0;
                    mapOffset = (vdpRegs[2] & 0x0F) * 0x400;
                    if (bitmapMode) {
                        colorOffset = (vdpRegs[3] & 0x80) != 0 ? 0x2000 : 0;
                        charOffset = (vdpRegs[4] & 0x04) != 0 ? 0x2000 : 0;
                    } else {
                        colorOffset = (vdpRegs[3] & 0xFF) * 0x40;
                        charOffset = (vdpRegs[4] & 0x07) * 0x800;
                    }
                    spriteAttrOffset = (vdpRegs[5] & 0x7F) * 0x80;
                    spriteOffset = (vdpRegs[6] & 0x07) * 0x800;
                    textColor = (vdpRegs[7] & 0xF0) >> 4;
                    screenColor = (vdpRegs[7] & 0x0F);
                    mapdMain.setColorScreen(screenColor);
                    fib.close();
                } catch (Exception e) {
                    errorAction(this, "Program error", e.getMessage());
                    e.printStackTrace(System.err);
                }
                magIO.readVramDumpFile(file, charOffset, mapOffset, colorOffset, spriteOffset, spriteAttrOffset, bitmapMode);
                mapdMain.setColorScreen(screenColor);
                if (textMode) {
                    for (int i = 0; i < clrSets.length; i++) {
                        clrSets[i][Globals.INDEX_CLR_FORE] = textColor;
                        clrSets[i][Globals.INDEX_CLR_BACK] = 0;
                    }
                }
                else {
                    screenColor = mapdMain.getColorScreen();
                    int otherColor = screenColor < 15 ? screenColor + 1 : 1;
                    for (int i = TIGlobals.MIN_SPRITE; i < TIGlobals.MAX_SPRITE; i++) {
                        if (spriteColors[i] == screenColor) {
                            spriteColors[i] = otherColor;
                        }
                    }
                }
            } else {
                VramImportDialog importer = new VramImportDialog(this);
                if (importer.isOkay()) {
                    charOffset = importer.getCharDataOffset();
                    mapOffset = importer.getMapDataOffset();
                    colorOffset = importer.getColorDataOffset();
                    spriteOffset = importer.getSpriteDataOffset();
                    spriteAttrOffset = importer.getSpriteAttrDataOffset();
                    magIO.readVramDumpFile(file, charOffset, mapOffset, colorOffset, spriteOffset, spriteAttrOffset, importer.isBitmapMode());
                }
            }
            setModified(true);
        }
        updateCharButtons();
        updateSpriteButtons();
        updateComponents();
    }

    protected void importMapImage() {
        File file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            MagellanImportDialog importer = new MagellanImportDialog(MagellanImportDialog.TYPE_MAP_IMAGE, this, this, colorMode, getCharacterSetStart(), getCharacterSetEnd(), getSpriteSetEnd(), ecmPalettes);
            if (importer.isOkay()) {
                try {
                    MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                    magIO.readMapImageFile(file, importer.getStartChar(), importer.getEndChar(), importer.getStartPalette(), importer.getTolerance());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    errorAction(this, "Error importing file", e.getMessage());
                }
                importer.dispose();
            }
            setModified(true);
        }
        updateCharButtons();
        updateComponents();
        editDefault();
    }

    protected void importSpriteImage() {
        File file = getFileFromChooser(currentDirectory, JFileChooser.OPEN_DIALOG, IMGEXTS, "Image Files", true);
        if (file != null) {
            MagellanImportDialog importer = new MagellanImportDialog(MagellanImportDialog.TYPE_SPRITE_IMAGE, this, this, colorMode, getCharacterSetStart(), getCharacterSetEnd(), getSpriteSetEnd(), ecmPalettes);
            if (importer.isOkay()) {
                try {
                    MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                    magIO.readSpriteFile(file, importer.getStartSprite(), importer.getStartPalette(), importer.getGap());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    errorAction(this, "Error importing file", e.getMessage());
                }
                importer.dispose();
            }
            setModified(true);
            if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                charECMPaletteComboBox.setEditable(false);
                charECMPaletteComboBox.setEditable(true);
                spriteECMPaletteComboBox.setEditable(false);
                spriteECMPaletteComboBox.setEditable(true);
                gcChar.setPalette(ecmPalettes[charECMPaletteComboBox.getSelectedIndex()].getColors());
                gcChar.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
                gcChar.redrawCanvas();
                updateCharButtons();
                gcSprite.setPalette(ecmPalettes[spriteECMPaletteComboBox.getSelectedIndex()].getColors());
                gcSprite.setColorScreen(getScreenColorPalette()[mapdMain.getColorScreen()]);
                gcSprite.redrawCanvas();
                updateSpriteButtons();
                mapdMain.setScreenColorPalette(getScreenColorPalette());
            }
        }
        updateCharButtons();
        updateSpriteButtons();
        updateComponents();
        editDefault();
    }

    protected void exportDataFile(int exportType) throws IOException {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_BASIC, this, this, bExportComments, defStartChar, defEndChar, getCharacterSetStart(), characterSetSize != CHARACTER_SET_BASIC || exportType == Globals.XB256_PROGRAM ? getCharacterSetEnd() : (exportType == Globals.XB_PROGRAM ? TIGlobals.FINALXBCHAR : TIGlobals.BASIC_LAST_CHAR), getSpriteSetEnd(), bCurrentMapOnly, bExcludeBlank);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, XBEXTS, "XB Data Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < XBEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + XBEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + XBEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                int aLine = exporter.getCodeLineStart();
                int cLine = exporter.getCharLineStart();
                int mLine = exporter.getMapLineStart();
                int iLine = exporter.getLineInterval();
                bExportComments = exporter.includeComments();
                bCurrentMapOnly = exporter.currentMapOnly();
                bExcludeBlank = exporter.excludeBlank();
                defStartChar = Math.min(sChar, eChar);
                defEndChar = Math.max(sChar, eChar);
                MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                magIO.writeXBDataFile(file, defStartChar, defEndChar, aLine, cLine, mLine, iLine, exportType, bExportComments, bCurrentMapOnly, bExcludeBlank);
            }
        }
        exporter.dispose();
    }

    protected void exportAssemblerFile() {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_ASM, this, this, bExportComments, defStartChar, defEndChar, TIGlobals.MIN_CHAR, getCharacterSetEnd(), defStartSprite, defEndSprite, getSpriteSetEnd(), bCurrentMapOnly, bExcludeBlank, bIncludeCharNumbers, bWrap, bIncludeSpriteData, compression, scrollOrientation, scrollFrames);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, ASMEXTS, "Assembler Source Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < ASMEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + ASMEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + ASMEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                int sSprite = exporter.getStartSprite();
                int eSprite = exporter.getEndSprite();
                bExportComments = exporter.includeComments();
                bIncludeCharNumbers = exporter.includeCharNumbers();
                bCurrentMapOnly = exporter.currentMapOnly();
                defStartChar = Math.min(sChar, eChar);
                defEndChar = Math.max(sChar, eChar);
                bIncludeSpriteData = exporter.includeSpritedata();
                defStartSprite = Math.min(sSprite, eSprite);
                defEndSprite = Math.max(sSprite, eSprite);
                compression = exporter.getCompression();
                MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                try {
                    magIO.writeASMDataFile(file, defStartChar, defEndChar, defStartSprite, defEndSprite, compression, bExportComments, bCurrentMapOnly, bIncludeCharNumbers, bIncludeSpriteData);
                } catch (Exception e) {
                    errorAction(this, "Export failed", e.getMessage());
                }
            }
        }
        exporter.dispose();
    }

    protected void exportScrollFile() {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_SCROLL, this, this, bExportComments, defStartChar, defEndChar, TIGlobals.MIN_CHAR, getCharacterSetEnd(), defStartSprite, defEndSprite, getSpriteSetEnd(), bCurrentMapOnly, bExcludeBlank, bIncludeCharNumbers, bWrap, bIncludeSpriteData, compression, scrollOrientation, scrollFrames);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, ASMEXTS, "Assembler Source Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < ASMEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + ASMEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + ASMEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                bExportComments = exporter.includeComments();
                bIncludeCharNumbers = exporter.includeCharNumbers();
                bCurrentMapOnly = exporter.currentMapOnly();
                bWrap = exporter.isWrap();
                defStartChar = Math.min(sChar, eChar);
                defEndChar = Math.max(sChar, eChar);
                compression = exporter.getCompression();
                scrollOrientation = exporter.getScrollOrientation();
                scrollFrames = exporter.getFrames();
                MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                try {
                    magIO.writeScrollFile(file, scrollOrientation, bWrap, compression, bExportComments, bCurrentMapOnly, bIncludeCharNumbers, scrollFrames, ANIMATE_SCROLLED_FRAMES);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    errorAction(this, "Export failed", e.getMessage());
                }
            }
        }
        exporter.dispose();
    }

    protected void exportBinaryFile() throws IOException {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_BINARY, this, this, bExportComments, defStartChar, defEndChar, TIGlobals.MIN_CHAR, getCharacterSetEnd(), getSpriteSetEnd(), bCurrentMapOnly, bExcludeBlank);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, BINEXTS, "Binary Data Files");
            if (file != null) {
                boolean isExtensionAdded = false;
                for (int ex = 0; ex < BINEXTS.length; ex++) {
                    if (file.getAbsolutePath().toLowerCase().endsWith("." + BINEXTS[ex])) {
                        isExtensionAdded = true;
                    }
                }
                if (!isExtensionAdded) {
                    file = new File(file.getAbsolutePath() + "." + BINEXT);
                }
                int sChar = exporter.getStartChar();
                int eChar = exporter.getEndChar();
                boolean bIncludeColorsets = exporter.includeColorsets();
                boolean bIncludeChardata = exporter.includeChardata();
                boolean bIncludeSpritedata = exporter.includeSpritedata();
                byte chunkByte = (byte) (0 | (bIncludeColorsets ? MagellanImportExport.BIN_CHUNK_COLORS : 0) | (bIncludeChardata ? MagellanImportExport.BIN_CHUNK_CHARS : 0) | (bIncludeSpritedata ? MagellanImportExport.BIN_CHUNK_SPRITES : 0));
                bCurrentMapOnly = exporter.currentMapOnly();
                defStartChar = Math.min(sChar, eChar);
                defEndChar = Math.max(sChar, eChar);
                MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
                magIO.writeBinaryFile(file, chunkByte, defStartChar, defEndChar, bCurrentMapOnly);
                updateComponents();
            }
        }
        exporter.dispose();
    }

    protected void exportBinaryMapFile() throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, BINEXTS, "Binary Data Files");
        if (file != null) {
            boolean isExtensionAdded = false;
            for (int ex = 0; ex < BINEXTS.length; ex++) {
                if (file.getAbsolutePath().toLowerCase().endsWith("." + BINEXTS[ex])) {
                    isExtensionAdded = true;
                }
            }
            if (!isExtensionAdded) {
                file = new File(file.getAbsolutePath() + "." + BINEXT);
            }
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.writeBinaryMap(file);
        }
    }

    protected void exportXBDisplayMerge() throws IOException {
        MagellanExportDialog exporter = new MagellanExportDialog(MagellanExportDialog.TYPE_XBSCRMER, this, this, bExportComments, defStartChar, defEndChar, TIGlobals.MIN_CHAR, getCharacterSetEnd(), getSpriteSetEnd(), bCurrentMapOnly, bExcludeBlank);
        if (exporter.isOkay()) {
            File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, ANYS, "Screen Merge Files");
            if (file != null) {
                int lineNo = exporter.getCodeLineStart();
                int dispWidth = exporter.getCharLineStart();

                if (lineNo >= 0 && lineNo <= 32710 && (dispWidth == 28 || dispWidth == 32)) {
                    int currentMap = mapdMain.getCurrentMapId();
                    int[][] map = mapdMain.getMapData(currentMap);

                    byte[] init = {7, 'T', 'I', 'F', 'I', 'L', 'E', 'S', 0, 6, (byte) 0x80, 1, (byte) 0x8B, (byte) 0xA3, 6};
                    byte[] tifiles = new byte[1664];
                    System.arraycopy(init, 0, tifiles, 0, init.length);
                    int sectorPosition = 0x80;
                    byte lineSize = 0x76;
                    int mapLine = 0;
                    int outputPos = 0;
                    int colStart = 2;
                    int colEnd = 29;
                    byte stringSize = (byte) 0x70;
                    if (dispWidth == 32) {
                        lineSize = (byte) 0x86;
                        colStart = 0;
                        colEnd = 31;
                        stringSize = (byte) 0x80;
                    }

                    for (int chunk = 0; chunk < 6; chunk++) {
                        outputPos = sectorPosition;
                        tifiles[outputPos++] = lineSize;
                        tifiles[outputPos++] = (byte) (lineNo >> 8);
                        tifiles[outputPos++] = (byte) (lineNo & 0xFF);
                        tifiles[outputPos++] = (byte) 0x93; // DATA "
                        tifiles[outputPos++] = (byte) 0xC7; //
                        tifiles[outputPos++] = stringSize;
                        if (dispWidth == 28) {
                            for (int line = 0; line < 4; line++)
                                for (int column = colStart; column <= colEnd; column++)
                                    tifiles[outputPos++] = (byte) map[mapLine + line][column];
                        } else {
                            for (int line = 0; line < 4; line++)
                                for (int column = colStart; column <= colEnd; column++)
                                    tifiles[outputPos++] = (byte) (0x60 + map[mapLine + line][column]);
                        }
                        tifiles[outputPos++] = 0; // end of string
                        lineNo += 10;
                        mapLine += 4;
                        tifiles[outputPos] = (byte) 0xFF; // end of line and sector
                        sectorPosition += 0x100;
                    }
                    tifiles[outputPos++] = (byte) 0x02; // end of file
                    tifiles[outputPos++] = (byte) 0xFF;
                    tifiles[outputPos++] = (byte) 0xFF;
                    tifiles[outputPos] = (byte) 0xFF;

                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(tifiles);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        errorAction(this, "Program error", e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
        exporter.dispose();
    }

    protected void exportCharImage(boolean isColor) throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, IMGEXTS, "Image Files");
        if (file != null) {
            boolean isExtensionAdded = false;
            for (int ex = 0; ex < IMGEXTS.length; ex++) {
                if (file.getAbsolutePath().toLowerCase().endsWith("." + IMGEXTS[ex])) {
                    isExtensionAdded = true;
                }
            }
            if (!isExtensionAdded) {
                file = new File(file.getAbsolutePath() + "." + IMGEXT);
            }
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.writeCharImage(file, 8, isColor);
        }
    }

    protected void exportMapImage() throws IOException {
        File file = getFileFromChooser(currentDirectory, JFileChooser.SAVE_DIALOG, IMGEXTS, "Image Files");
        if (file != null) {
            boolean isExtensionAdded = false;
            for (int ex = 0; ex < IMGEXTS.length; ex++) {
                if (file.getAbsolutePath().toLowerCase().endsWith("." + IMGEXTS[ex])) {
                    isExtensionAdded = true;
                }
            }
            if (!isExtensionAdded) {
                file = new File(file.getAbsolutePath() + "." + IMGEXT);
            }
            updateComponents();
            MagellanImportExport magIO = new MagellanImportExport(mapdMain, ecmPalettes, clrSets, hmCharGrids, hmCharColors, ecmCharPalettes, ecmCharTransparency, hmSpriteGrids, spriteColors, ecmSpritePalettes, colorMode);
            magIO.writeMapImage(file);
        }
    }

    private File getFileFromChooser(String startDir, int dialogType, String[] exts, String desc) {
        return getFileFromChooser(startDir, dialogType, exts, desc, false);
    }

    private File getFileFromChooser(String startDir, int dialogType, String[] exts, String desc, boolean hasImagePreview) {
        JFileChooser jfileDialog = new JFileChooser(startDir);
        jfileDialog.setDialogType(dialogType);
        if (!exts[0].equals(ANY)) {
            jfileDialog.setFileFilter(new MutableFilter(exts, desc));
        }
        if (hasImagePreview) {
            jfileDialog.setAccessory(new ImagePreview(jfileDialog));
        }
        int optionSelected = JFileChooser.CANCEL_OPTION;
        if (dialogType == JFileChooser.OPEN_DIALOG) {
            optionSelected = jfileDialog.showOpenDialog(this);
        } else if (dialogType == JFileChooser.SAVE_DIALOG) {
            optionSelected = jfileDialog.showSaveDialog(this);
        } else // default to an OPEN_DIALOG
        {
            optionSelected = jfileDialog.showOpenDialog(this);
        }
        if (optionSelected == JFileChooser.APPROVE_OPTION) {
            File file = jfileDialog.getSelectedFile();
            if (file != null) {
                currentDirectory = file.getParent();
            }
            return file;
        }
        return null;
    }

    private void savePreferences() {
        try {
            FileOutputStream fos = new FileOutputStream(new File(System.getProperty("user.home") + "/Magellan.prefs"));
            appProperties.setProperty("magnif", "" + mapdMain.getViewScale());
            appProperties.setProperty("textCursor", (mapdMain.showTypeCell() ? "true" : "false"));
            appProperties.setProperty("exportComments", (bExportComments ? "true" : "false"));
            appProperties.setProperty("includeCharNumbers", (bIncludeCharNumbers ? "true" : "false"));
            appProperties.setProperty("currentMapOnly", (bCurrentMapOnly ? "true" : "false"));
            appProperties.setProperty("includeSpriteData", (bIncludeSpriteData ? "true" : "false"));
            appProperties.setProperty("excludeBlank", (bExcludeBlank ? "true" : "false"));
            appProperties.setProperty("wrap", (bWrap ? "true" : "false"));
            appProperties.setProperty("characterSetSize", Integer.toString(characterSetSize));
            appProperties.setProperty("colorMode", Integer.toString(colorMode));
            appProperties.setProperty("showGrid", (mapdMain.isShowGrid() ? "true" : "false"));
            appProperties.setProperty("gridScale", Integer.toString(mapdMain.getGridScale()));
            appProperties.setProperty("showPosition", (mapdMain.showPosIndic() ? "true" : "false"));
            appProperties.setProperty("base0Position", (mapdMain.base0Position() ? "true" : "false"));
            appProperties.setProperty("defStartChar", "" + defStartChar);
            appProperties.setProperty("defEndChar", "" + defEndChar);
            appProperties.setProperty("defStartSprite", "" + defStartSprite);
            appProperties.setProperty("defEndSprite", "" + defEndSprite);
            appProperties.setProperty("compression", "" + compression);
            appProperties.setProperty("scrollOrientation", "" + scrollOrientation);
            appProperties.setProperty("scrollFrames", "" + scrollFrames);
            appProperties.setProperty("filePath", currentDirectory != null ? currentDirectory : ".");
            StringBuilder recentFileList = new StringBuilder();
            for (String filePath : recentFiles) {
                if (filePath != null && new File(filePath).exists()) {
                    if (recentFileList.length() > 0) {
                        recentFileList.append("|");
                    }
                    recentFileList.append(filePath);
                }
            }
            appProperties.setProperty("recentFiles", recentFileList.toString());
            appProperties.store(fos, null);
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            errorAction(this, "Error saving preferences", ioe.getMessage());
            ioe.printStackTrace(System.err);
        }
    }

// Class Utility Methods -------------------------------------------------------------------/

    protected void newProject() {
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
        for (int r = 0; r < FONT_ROWS; r++) {
            clrSets[r][Globals.INDEX_CLR_BACK] = 0;
            clrSets[r][Globals.INDEX_CLR_FORE] = 1;
            for (int c = 0; c < FONT_COLS; c++) {
                int[][] emptyGrid = new int[8][8];
                for (int y = 0; y < emptyGrid.length; y++) {
                    for (int x = 0; x < emptyGrid[y].length; x++) {
                        if (charNum >= TIGlobals.CHARMAPSTART && charNum <= TIGlobals.CHARMAPEND) {
                            emptyGrid[y][x] = hmDefaultChars.get(charNum)[y][x];
                        } else {
                            emptyGrid[y][x] = 0;
                        }
                    }
                }
                hmCharGrids.put(charNum, emptyGrid);
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    int cellNum = (r * FONT_COLS) + c;
                    jbtnChar[cellNum].setBackground(TIGlobals.TI_PALETTE_OPAQUE[clrSets[r][Globals.INDEX_CLR_BACK]]);
                    jbtnChar[cellNum].setForeground(TIGlobals.TI_COLOR_UNUSED);
                }
                else if (colorMode == COLOR_MODE_BITMAP) {
                    int[][] emptyColors = new int[8][2];
                    for (int y = 0; y < emptyColors.length; y++) {
                        emptyColors[y][0] = 0;
                        emptyColors[y][1] = 1;
                    }
                    hmCharColors.put(charNum, emptyColors);
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
            hmSpriteGrids.put(i, new int[16][16]);
            spriteColors[i] = 1;
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

    protected void editDefault() {
        // Edit default character
        activeChar = TIGlobals.CUSTOMCHAR;
        ActionEvent aeInitChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + activeChar);
        Magellan.this.actionPerformed(aeInitChar);
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            charECMPaletteComboBox.setEditable(false);
            charECMPaletteComboBox.setEditable(true);
        }
        // Edit default sprite
        activeSprite = 0;
        ActionEvent aeInitSprite = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_SPR + activeSprite);
        Magellan.this.actionPerformed(aeInitSprite);
        if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
            spriteECMPaletteComboBox.setEditable(false);
            spriteECMPaletteComboBox.setEditable(true);
        }
    }

    protected int confirmationAction(Frame parent, String title, String message, boolean includeCancel) {
        return JOptionPane.showConfirmDialog(parent, message, title, includeCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    protected void informationAction(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("images/logo.png")));
    }

    protected void errorAction(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected void limitCharGrids(int newColors, int oldColors) {
        int factor = oldColors / newColors;
        for (int ch = TIGlobals.MIN_CHAR; ch <= TIGlobals.MAX_CHAR; ch++) {
            int[][] charGrid = hmCharGrids.get(ch);
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
            int[][] charGrid = hmSpriteGrids.get(ch);
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
        for (int i = 0; i < ecmPalettes.length; i++) {
            if (ecmPalettes[i] == ecmPalette) {
                return i;
            }
        }
        return -1;
    }

// Update Methods ---------------------------------------------------------------------/

    protected void updateComponents() {
        // Character editor
        updateCharButton(activeChar, false);
        jbtnLook.setBackground((mapdMain.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
        jchkTransparency.setVisible(colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3);
        jchkTransparency.setSelected(ecmCharTransparency[activeChar]);
        int[][] charGridData = gcChar.getGridData();
        jtxtChar.setText((
            Globals.getHexString(charGridData)
            + (colorMode == COLOR_MODE_BITMAP ? Globals.getColorHexString(gcChar.getGridColors()) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(ecmCharPalettes[activeChar]), 4) : "")
        ).toUpperCase());
        jtxtChar.setCaretPosition(0);
        // Sprite editor
        updateSpriteButton(activeSprite, false);
        int[][] spriteGridData = gcSprite.getGridData();
        jtxtSprite.setText((
            Globals.getSpriteHexString(spriteGridData)
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(ecmSpritePalettes[activeSprite]), 4) : "")
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
        // Handle missing char grid
        if (hmCharGrids.get(charNum) == null) {
            if (jbtnChar[charNum].getIcon() != null) {
                jbtnChar[charNum].setIcon(null);
                int charmapIndex = charNum - TIGlobals.CHARMAPSTART;
                jbtnChar[charNum].setText((charmapIndex >= 0) && (charmapIndex < TIGlobals.CHARMAP.length) ? "" + TIGlobals.CHARMAP[charmapIndex] : "?");
            }
            return;
        }
        // Generate icon image
        int imageScale = 2;
        Image image = this.createImage(gcChar.getGridData().length * imageScale, gcChar.getGridData()[0].length * imageScale);
        Graphics g = image.getGraphics();
        Color[] palette = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? TIGlobals.TI_PALETTE_OPAQUE : ecmCharPalettes[charNum].getColors();
        int[][] charGrid = hmCharGrids.get(charNum);
        int[][] charColors = null;
        if (colorMode == COLOR_MODE_BITMAP) {
            charColors = hmCharColors.get(charNum);
        }
        int cset = (int) (Math.floor(charNum / 8));
        for (int y = 0; y < charGrid.length; y++) {
            for (int x = 0; x < charGrid[y].length; x++) {
                if (colorMode == COLOR_MODE_GRAPHICS_1) {
                    if (charGrid[y][x] == 1) {
                        int fore = clrSets[cset][Globals.INDEX_CLR_FORE];
                        g.setColor(fore != 0 ? palette[fore] : screenColor);
                    }
                    else {
                        int back = clrSets[cset][Globals.INDEX_CLR_BACK];
                        g.setColor(back != 0 ? palette[back] : screenColor);
                    }
                }
                else if (colorMode == COLOR_MODE_BITMAP) {
                    int colorIndex = charColors[y][charGrid[y][x]];
                    g.setColor(colorIndex != 0 ? palette[colorIndex] : screenColor);
                }
                else {
                    int colorIndex = charGrid[y][x];
                    g.setColor(colorIndex != 0 || !ecmCharTransparency[charNum] ? palette[colorIndex] : screenColor);
                }
                g.fillRect(x * imageScale, y * imageScale, imageScale, imageScale);
            }
        }
        g.dispose();
        boolean empty = Globals.isGridEmpty(charGrid) && (colorMode == COLOR_MODE_GRAPHICS_1 && clrSets[cset][Globals.INDEX_CLR_BACK] == 0 || colorMode == COLOR_MODE_BITMAP && Globals.isColorGridEmpty(charColors));
        // Save image
        hmCharImages.put(charNum, image);
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
        if (hmSpriteGrids.get(spriteNum) == null) {
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
        int fore = spriteColors[spriteNum];
        Color[] palette = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? TIGlobals.TI_PALETTE_OPAQUE : ecmSpritePalettes[spriteNum].getColors();
        Color foreColor = colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? palette[fore] : null;
        Color transparent = TIGlobals.TI_COLOR_TRANSOPAQUE;
        int[][] spriteGrid = hmSpriteGrids.get(spriteNum);
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
        hmSpriteImages.put(spriteNum, image);
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

    protected Image makeImageTransparent(Image imgSrc) {
        ImageProducer ipTrans = new FilteredImageSource(imgSrc.getSource(), Globals.ifTrans);
        return Toolkit.getDefaultToolkit().createImage(ipTrans);
    }

    public Color[] getScreenColorPalette() {
        Color[] palette = null;
        if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
            palette = TIGlobals.TI_PALETTE_OPAQUE;
        }
        else if (colorMode == COLOR_MODE_ECM_2) {
            palette = new Color[16];
            System.arraycopy(ecmPalettes[0].getColors(), 0, palette, 0, 4);
            System.arraycopy(ecmPalettes[1].getColors(), 0, palette, 4, 4);
            System.arraycopy(ecmPalettes[2].getColors(), 0, palette, 8, 4);
            System.arraycopy(ecmPalettes[3].getColors(), 0, palette, 12, 4);
        }
        else if (colorMode == COLOR_MODE_ECM_3) {
            palette = new Color[16];
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
        mapdMain.setScreenColorPalette(getScreenColorPalette());
        setModified(true);
    }

    public int getCharacterSetStart() {
        return getCharacterSetStart(characterSetSize);
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

    public int getCharacterSetEnd() {
        return getCharacterSetEnd(characterSetSize);
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

    public int getCharacterSetSize() {
        return getCharacterSetSize(characterSetSize);
    }

    public static int getCharacterSetSize(int characterSetSize) {
        return getCharacterSetEnd(characterSetSize) - getCharacterSetStart(characterSetSize) + 1;
    }

    public int getSpriteSetEnd() {
        return getSpriteSetEnd(characterSetSize);
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

    public int getSpriteSetSize() {
        return getSpriteSetSize(characterSetSize);
    }

    public static int getSpriteSetSize(int characterSetSize) {
        return getSpriteSetEnd(characterSetSize) + 1;
    }

    private void addRecentFile(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        while (recentFiles.size() > 10) {
            recentFiles.remove(recentFiles.size() - 1);
        }
    }
}
