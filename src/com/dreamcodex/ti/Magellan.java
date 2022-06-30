package com.dreamcodex.ti;

import com.dreamcodex.ti.actions.exporting.*;
import com.dreamcodex.ti.actions.importing.*;
import com.dreamcodex.ti.component.*;
import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.iface.ScreenColorListener;
import com.dreamcodex.ti.iface.UndoRedoListener;
import com.dreamcodex.ti.ui.MagellanUI;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.dreamcodex.ti.util.ColorMode.*;
import static com.dreamcodex.ti.util.Globals.*;
import static com.dreamcodex.ti.util.TIGlobals.*;

/**
 * Magellan
 * TI-99/4A graphical map editor
 *
 * @author Howard Kistler
 * @author Rasmus Moustgaard
 */

public class Magellan extends JFrame implements Runnable, WindowListener, ActionListener, MouseListener, MouseMotionListener, IconProvider, ScreenColorListener, UndoRedoListener, MapChangeListener {

// Constants -------------------------------------------------------------------------------/

    public static final String VERSION_NUMBER = "4.5.1 beta";

    public static final int CHARACTER_SET_BASIC = 0;
    public static final int CHARACTER_SET_EXPANDED = 1;
    public static final int CHARACTER_SET_SUPER = 2;
    public static String[] CHARACTER_SET_SIZES = new String[] {
        "Basic Character Set",
        "Expanded Character Set",
        "Super Character Set"
    };

// Local Constants -------------------------------------------------------------------------/

    private static final String APPTITLE = "Magellan v" + VERSION_NUMBER + " : TI-99/4A Map Editor";

    public static final int FONT_COLS = 8;

    public static final int SPRITE_COLS = 4;

    private static final int MAP_ROWS = 24;
    private static final int MAP_COLS = 32;
    private static final int MAP_CELL = 8;

// Variables -------------------------------------------------------------------------------/

    protected DataSet dataSet = new DataSet();
    protected int activeChar = TIGlobals.CUSTOMCHAR;
    protected int lastActiveChar = MapCanvas.NOCHAR;
    protected int activeSprite = 0;
    protected int lastActiveSprite = MapCanvas.NOCHAR;
    protected HashMap<Integer, int[][]> defaultChars;
    protected Preferences preferences = new Preferences();
    private final String openFilePath;        // File to open upon startup
    private File mapDataFile;                 // Current map file
    private boolean projectModified = false;  // Current project modified?

// Components ------------------------------------------------------------------------------/

    private MagellanUI ui;

    // Map editor
    private MapEditor mapEditor;

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
                new SaveDataFileAction("", this, mapEditor, dataSet, preferences).actionPerformed(null);
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

        URL icon = getClass().getResource("images/icon64.png");
        if (icon != null) {
            setIconImage(new ImageIcon(icon).getImage());
        }

        // Create map editor panel (needs to initialise early for the listeners)
        mapEditor = new MapEditor(MAP_COLS, MAP_ROWS, MAP_CELL, this, this);
        mapEditor.fillGrid(TIGlobals.SPACECHAR);
        mapEditor.setBkgrndColor(Globals.CLR_COMPONENTBACK);
        mapEditor.resetUndoManager();

        // Read application properties (if exist)
        readPreferences();

        // Initialize data structures

        // Default characters
        defaultChars = new HashMap<>();
        for (int ch = TIGlobals.CHARMAPSTART; ch <= TIGlobals.CHARMAPEND; ch++) {
            defaultChars.put(ch, Globals.getIntGrid(TIGlobals.DEFAULT_TI_CHARS[ch - TIGlobals.CHARMAPSTART], 8));
        }

        // Character structures
        dataSet.setCharGrids(new HashMap<>());
        if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
            dataSet.setCharColors(new HashMap<>());
        }
        dataSet.setCharImages(new HashMap<>());
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
            if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
                int[][] emptyColors = new int[8][2];
                for (int y = 0; y < emptyColors.length; y++) {
                    emptyColors[y][0] = 0;
                    emptyColors[y][1] = 1;
                }
                charColors.put(ch, emptyColors);
            }
        }

        // Sprite structures
        HashMap<Integer, int[][]> spriteGrids = new HashMap<>();
        dataSet.setSpriteGrids(spriteGrids);
        HashMap<Integer, Image> spriteImages = new HashMap<>();
        dataSet.setSpriteImages(spriteImages);
        int[] spriteColors = dataSet.getSpriteColors();
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            spriteGrids.put(i, new int[16][16]);
            spriteColors[i] = 1;
        }

        // ECM palettes
        buildECMPalettes();

        // Main UI components
        ui = new MagellanUI(this, mapEditor, dataSet, preferences);
        JMenuBar jMenuBar = ui.createMenu();
        JPanel jpnlMain = ui.createMainPanel();

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
        ui.getCharGridCanvas().addUndoRedoListener(this);
        ui.getSpriteGridCanvas().addUndoRedoListener(this);
        mapEditor.addMapChangeListener(this);
        mapEditor.addScreenColorListener(this);
        final JTabbedPane tabbedPane = ui.getEditorTabbedPane();
        tabbedPane.addChangeListener(e -> {
            boolean spriteMode = tabbedPane.getSelectedIndex() == 1;
            mapEditor.setSpriteMode(spriteMode);
            if (spriteMode) {
                ui.getLookButton().setBackground(Globals.CLR_BUTTON_NORMAL);
            }
        });

        updateScreenColorPalette();
        updateCharButtons();

        SwingUtilities.invokeLater(
            () -> {
                editDefault();
                // Open command line file
                if (openFilePath != null) {
                    new OpenDataFileAction("", openFilePath, Magellan.this, mapEditor, dataSet, preferences).actionPerformed(null);
                }
            }
        );
    }

    protected void buildColorDocks() {
        buildECMPalettes();
        ui.buildColorDocks();
        updateScreenColorPalette();
    }

    protected void buildECMPalettes() {
        ECMPalette[] ecmPalettes;
        if (dataSet.getColorMode() == COLOR_MODE_ECM_2) {
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

// Listeners -------------------------------------------------------------------------------/

    /* ActionListener methods */
    public void actionPerformed(ActionEvent ae) {
        ColorMode colorMode = dataSet.getColorMode();
        GridCanvas charCanvas = ui.getCharGridCanvas();
        GridCanvas spriteCanvas = ui.getSpriteGridCanvas();
        try {
            String command = ae.getActionCommand();
            if (command.equals(Globals.CMD_EXIT)) {
                exitApp(0);
            } else if (command.equals(Globals.CMD_CLEAR_CHR)) {
                charCanvas.clearGrid();
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_CLEAR_SPR)) {
                spriteCanvas.clearGrid();
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_CHR)) {
                charCanvas.fillGrid();
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FILL_SPR)) {
                spriteCanvas.fillGrid();
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_GRID_CHR)) {
                charCanvas.toggleGrid();
                updateComponents();
            } else if (command.equals(Globals.CMD_GRID_SPR)) {
                spriteCanvas.toggleGrid();
            } else if (command.equals(Globals.CMD_UNDO_CHR)) {
                charCanvas.undo();
            } else if (command.equals(Globals.CMD_UNDO_SPR)) {
                spriteCanvas.undo();
            } else if (command.equals(Globals.CMD_REDO_CHR)) {
                charCanvas.redo();
            } else if (command.equals(Globals.CMD_REDO_SPR)) {
                spriteCanvas.redo();
            } else if (command.equals(Globals.CMD_FLIPH_CHR)) {
                charCanvas.setGrid(Globals.flipGrid(charCanvas.getGridData(), false));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPH_SPR)) {
                spriteCanvas.setGrid(Globals.flipGrid(spriteCanvas.getGridData(), false));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_CHR)) {
                charCanvas.setGridAndColors(Globals.flipGrid(charCanvas.getGridData(), true), colorMode == COLOR_MODE_BITMAP ? Globals.flipGrid(charCanvas.getGridColors(), true) : null);
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_FLIPV_SPR)) {
                spriteCanvas.setGrid(Globals.flipGrid(spriteCanvas.getGridData(), true));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_CHR)) {
                charCanvas.setGrid(Globals.rotateGrid(charCanvas.getGridData(), true));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATEL_SPR)) {
                spriteCanvas.setGrid(Globals.rotateGrid(spriteCanvas.getGridData(), true));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_CHR)) {
                charCanvas.setGrid(Globals.rotateGrid(charCanvas.getGridData(), false));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_ROTATER_SPR)) {
                spriteCanvas.setGrid(Globals.rotateGrid(spriteCanvas.getGridData(), false));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_CHR)) {
                if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0 || colorMode != COLOR_MODE_BITMAP) {
                    charCanvas.setGrid(Globals.invertGrid(charCanvas.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                }
                else {
                    charCanvas.setGridAndColors(Globals.invertGrid(charCanvas.getGridData(), 1), Globals.flipGrid(charCanvas.getGridColors(), false));
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_INVERT_SPR)) {
                spriteCanvas.setGrid(Globals.invertGrid(spriteCanvas.getGridData(), colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : (colorMode == COLOR_MODE_ECM_2 ? 3 : 7)));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_CHR)) {
                charCanvas.setGrid(Globals.cycleGridUp(charCanvas.getGridData()));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    charCanvas.setColors(Globals.cycleGridUp(charCanvas.getGridColors()));
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTU_SPR)) {
                spriteCanvas.setGrid(Globals.cycleGridUp(spriteCanvas.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateCharButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_CHR)) {
                charCanvas.setGrid(Globals.cycleGridDown(charCanvas.getGridData()));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                if (colorMode == COLOR_MODE_BITMAP) {
                    charCanvas.setColors(Globals.cycleGridDown(charCanvas.getGridColors()));
                    dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                }
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTD_SPR)) {
                spriteCanvas.setGrid(Globals.cycleGridDown(spriteCanvas.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_CHR)) {
                charCanvas.setGrid(Globals.cycleGridLeft(charCanvas.getGridData()));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTL_SPR)) {
                spriteCanvas.setGrid(Globals.cycleGridLeft(spriteCanvas.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_CHR)) {
                charCanvas.setGrid(Globals.cycleGridRight(charCanvas.getGridData()));
                dataSet.getCharGrids().put(activeChar, charCanvas.getGridData());
                updateCharButton(activeChar);
                updateComponents();
            } else if (command.equals(Globals.CMD_SHIFTR_SPR)) {
                spriteCanvas.setGrid(Globals.cycleGridRight(spriteCanvas.getGridData()));
                dataSet.getSpriteGrids().put(activeSprite, spriteCanvas.getGridData());
                updateSpriteButton(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_LOOK)) {
                mapEditor.setLookModeOn(!mapEditor.isLookModeOn());
                ui.getLookButton().setBackground((mapEditor.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
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
                    charCanvas.clearGrid();
                    charGrids.put(activeChar, charCanvas.getGridData());
                    if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
                        dataSet.getCharColors().put(activeChar, charCanvas.getGridColors());
                    }
                }
                charCanvas.resetUndoRedo();
                charCanvas.setGridAndColors(charGrids.get(activeChar), dataSet.getColorMode() == COLOR_MODE_BITMAP ? dataSet.getCharColors().get(activeChar) : null);
                if (dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1) {
                    int cset = activeChar / 8;
                    charCanvas.setColorBack(dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK]);
                    charCanvas.setColorDraw(dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE]);
                    DualClickButton[] charColorDockButtons = ui.getCharColorDockButtons();
                    for (int i = 0; i < charColorDockButtons.length; i++) {
                        charColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == charCanvas.getColorDraw() ? "F" : ""));
                    }
                }
                else if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
                    ECMPalette ecmPalette = dataSet.getEcmCharPalettes()[activeChar];
                    charCanvas.setPalette(ecmPalette.getColors());
                    ui.getCharECMPaletteComboBox().setSelectedItem(ecmPalette);
                }
                charCanvas.setECMTransparency(dataSet.getEcmCharTransparency()[activeChar]);
                charCanvas.redrawCanvas();
                mapEditor.setActiveChar(activeChar);
                mapEditor.setCloneModeOn(false);
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
                    spriteCanvas.clearGrid();
                    spriteGrids.put(activeSprite, spriteCanvas.getGridData());
                }
                spriteCanvas.resetUndoRedo();
                spriteCanvas.setGrid(spriteGrids.get(activeSprite));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    spriteCanvas.setColorDraw(spriteColors[activeSprite]);
                    DualClickButton[] spriteColorDockButtons = ui.getSpriteColorDockButtons();
                    for (int i = 0; i < spriteColorDockButtons.length; i++) {
                        spriteColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == spriteCanvas.getColorDraw() ? "F" : ""));
                    }
                }
                else if (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3) {
                    spriteCanvas.setPalette(ecmSpritePalettes[activeSprite].getColors());
                    ui.getSpriteECMPaletteComboBox().setSelectedItem(ecmSpritePalettes[activeSprite]);
                }
                spriteCanvas.redrawCanvas();
                mapEditor.setActiveSprite(activeSprite);
                updateComponents();
            } else if (command.equals(Globals.CMD_UPDATE_CHR)) {
                String hexString = "";
                JTextField charTextField = ui.getCharTextField();
                switch (colorMode) {
                    case COLOR_MODE_GRAPHICS_1:
                        hexString = Globals.padHexString(charTextField.getText(), 16);
                        break;
                    case COLOR_MODE_BITMAP:
                        hexString = Globals.padHexString(charTextField.getText(), charTextField.getText().length() <= 16 ? 16 : 32);
                        break;
                    case COLOR_MODE_ECM_2:
                        hexString = Globals.padHexString(charTextField.getText(), 32 + 4);
                        break;
                    case COLOR_MODE_ECM_3:
                        hexString = Globals.padHexString(charTextField.getText(), 48 + 4);
                        break;
                }
                charTextField.setText(hexString);
                charTextField.setCaretPosition(0);
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
                    ui.getCharECMPaletteComboBox().setSelectedItem(dataSet.getEcmCharPalettes()[activeChar]);
                }
                dataSet.getCharGrids().put(activeChar, charGrid);
                charCanvas.setGridAndColors(dataSet.getCharGrids().get(activeChar), colorMode == COLOR_MODE_BITMAP ? charColors : null);
                updateCharButton(activeChar);
            } else if (command.equals(Globals.CMD_UPDATE_SPR)) {
                String hexString = "";
                JTextField spriteTextField = ui.getSpriteTextField();
                switch (colorMode) {
                    case COLOR_MODE_BITMAP:
                    case COLOR_MODE_GRAPHICS_1:
                        hexString = Globals.padHexString(spriteTextField.getText(), 64);
                        break;
                    case COLOR_MODE_ECM_2:
                        hexString = Globals.padHexString(spriteTextField.getText(), 128 + 4);
                        break;
                    case COLOR_MODE_ECM_3:
                        hexString = Globals.padHexString(spriteTextField.getText(), 192 + 4);
                        break;
                }
                spriteTextField.setText(hexString);
                spriteTextField.setCaretPosition(0);
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
                    ui.getSpriteECMPaletteComboBox().setSelectedItem(dataSet.getEcmSpritePalettes()[activeSprite]);
                }
                dataSet.getSpriteGrids().put(activeSprite, spriteGrid);
                spriteCanvas.setGrid(dataSet.getSpriteGrids().get(activeSprite));
                updateCharButton(activeSprite);
            } else if (command.startsWith(Globals.CMD_CLRFORE_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRFORE_CHR.length()));
                if (colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP) {
                    // Mark the selected foreground color
                    for (DualClickButton colorButton : ui.getCharColorDockButtons()) {
                        if ("F".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    ui.getCharColorDockButtons()[index].setText("F");
                    if (colorMode != COLOR_MODE_BITMAP) {
                        int cset = activeChar / 8;
                        dataSet.getClrSets()[cset][Globals.INDEX_CLR_FORE] = index;
                        for (int c = 0; c < FONT_COLS; c++) {
                            updateCharButton((cset * 8) + c, false);
                        }
                        updateCharButton(activeChar);
                    }
                    charCanvas.setColorDraw(index);
                    charCanvas.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        charCanvas.setColorDraw(index);
                    }
                    else {
                        // Swap two colors of the palette and of the character grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getCharECMPaletteComboBox().getSelectedIndex()];
                        int index2 = ui.getCharECMPaletteComboBox().getIndexBack();
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
                    for (DualClickButton colorButton : ui.getSpriteColorDockButtons()) {
                        if ("F".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    ui.getSpriteColorDockButtons()[index].setText("F");
                    updateSpriteButton(activeSprite);
                    spriteCanvas.setColorDraw(index);
                    spriteCanvas.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        spriteCanvas.setColorDraw(index);
                    }
                    else {
                        // Swap two colors of the palette and of the sprite grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getSpriteECMPaletteComboBox().getSelectedIndex()];
                        int index2 = ui.getSpriteECMPaletteComboBox().getIndexBack();
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
                    for (DualClickButton colorButton : ui.getCharColorDockButtons()) {
                        if ("B".equals(colorButton.getText())) {
                            colorButton.setText("");
                        }
                    }
                    ui.getCharColorDockButtons()[index].setText("B");
                    if (colorMode != COLOR_MODE_BITMAP) {
                        int cset = activeChar / 8;
                        dataSet.getClrSets()[cset][Globals.INDEX_CLR_BACK] = index;
                        for (int c = 0; c < FONT_COLS; c++) {
                            updateCharButton((cset * 8) + c, false);
                        }
                        updateCharButton(activeChar);
                    }
                    charCanvas.setColorBack(index);
                    charCanvas.redrawCanvas();
                }
                else {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        charCanvas.setColorBack(index);
                    }
                    else {
                        // Swap two colors of the palette and of the character grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getCharECMPaletteComboBox().getSelectedIndex()];
                        int index2 = ui.getCharECMPaletteComboBox().getIndexFore();
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
                if (colorMode != COLOR_MODE_GRAPHICS_1 && colorMode != COLOR_MODE_BITMAP) {
                    if ((ae.getModifiers() & (ActionEvent.SHIFT_MASK | KeyEvent.CTRL_MASK)) == 0) {
                        spriteCanvas.setColorBack(index);
                    }
                    else {
                        // Swap two colors of the palette and of the sprite grids
                        ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getSpriteECMPaletteComboBox().getSelectedIndex()];
                        int index2 = ui.getSpriteECMPaletteComboBox().getIndexFore();
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
                int selectedIndex = ui.getCharECMPaletteComboBox().getSelectedIndex();
                if (selectedIndex != -1) {
                    ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
                    dataSet.getEcmCharPalettes()[activeChar] = ecmPalette;
                    charCanvas.setPalette(ecmPalette.getColors());
                }
                charCanvas.redrawCanvas();
                updateCharButton(activeChar);
            } else if (command.equals(Globals.CMD_PALSELECT_SPR)) {
                int selectedIndex = ui.getSpriteECMPaletteComboBox().getSelectedIndex();
                if (selectedIndex != -1) {
                    ECMPalette ecmPalette = dataSet.getEcmPalettes()[selectedIndex];
                    dataSet.getEcmSpritePalettes()[activeSprite] = ecmPalette;
                    spriteCanvas.setPalette(ecmPalette.getColors());
                }
                spriteCanvas.redrawCanvas();
                updateSpriteButton(activeSprite);
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_CHR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_CHR.length()));
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getCharECMPaletteComboBox().getSelectedIndex()];
                // Choose a new palette color
                Color color = ECMColorChooser.showDialog(this, "Select Color", ecmPalette.getColor(index));
                if (color != null) {
                    setECMPaletteColor(ecmPalette, index, color);
                }
            } else if (command.startsWith(Globals.CMD_CLRCHOOSE_SPR)) {
                int index = Integer.parseInt(command.substring(Globals.CMD_CLRCHOOSE_SPR.length()));
                ECMPalette ecmPalette = dataSet.getEcmPalettes()[ui.getSpriteECMPaletteComboBox().getSelectedIndex()];
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
                mapEditor.toggleShowPosIndic();
            } else if (command.equals(Globals.CMD_BASE0POS)) {
                mapEditor.toggleBase0Position();
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
                mapEditor.setViewCharLayer(!mapEditor.getViewCharLayer());
            } else if (command.equals(Globals.CMD_VIEW_SPRITE_LAYER)) {
                mapEditor.setViewSpriteLayer(!mapEditor.getViewSpriteLayer());
            } else if (command.equals(CMD_MAGNIFY_SPRITES)) {
                mapEditor.setMagnifySprites(!mapEditor.getMagnifySprites());
            } else if (command.equals(CMD_SNAP_SPRITES_TO_GRID)) {
                mapEditor.setSnapSpritesToGrid(!mapEditor.getSnapSpritesToGrid());
            } else if (command.equals(Globals.CMD_TRANSPARENCY)) {
                dataSet.getEcmCharTransparency()[activeChar] = ui.getTransparencyCheckBox().isSelected();
                charCanvas.setECMTransparency(dataSet.getEcmCharTransparency()[activeChar]);
                charCanvas.redrawCanvas();
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
            mapEditor.redrawCanvas();
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
        if (mapEditor.isLookModeOn()) {
            if (mapEditor.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapEditor.getLookChar());
                this.actionPerformed(aeChar);
                // Turn look mode off
                ActionEvent aeLook = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_LOOK);
                this.actionPerformed(aeLook);
            }
        }
        if (!mapEditor.getHotCell().equals(MapCanvas.PT_OFFGRID)) {
            mapEditor.requestFocus();
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
        if (mapEditor.isLookModeOn()) {
            if (mapEditor.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapEditor.getLookChar());
                this.actionPerformed(aeChar);
                mapEditor.requestFocus();
            }
        }
        mapEditor.updatePositionIndicator();
    }

    public void mouseDragged(MouseEvent me) {
        if (mapEditor.isLookModeOn()) {
            if (mapEditor.getLookChar() != MapCanvas.NOCHAR) {
                ActionEvent aeChar = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_CHR + mapEditor.getLookChar());
                this.actionPerformed(aeChar);
                ActionEvent aeLook = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_LOOK);
                this.actionPerformed(aeLook);
            }
        }
        if (!mapEditor.getHotCell().equals(MapCanvas.PT_OFFGRID)) {
            mapEditor.requestFocus();
        }
        updateComponents();
    }

/* IconProvider methods */

    public Icon getIconForChar(int i) {
        try {
            return ui.getCharButtons()[i].getIcon();
        } catch (Exception e) {
            return new ImageIcon(this.createImage(ui.getCharGridCanvas().getGridData().length, ui.getCharGridCanvas().getGridData()[0].length));
        }
    }

    public Icon getIconForSprite(int i) {
        try {
            return ui.getSpriteButtons()[i].getIcon();
        } catch (Exception e) {
            return new ImageIcon(this.createImage(ui.getSpriteGridCanvas().getGridData().length, ui.getSpriteGridCanvas().getGridData()[0].length));
        }
    }

    /* ScreenColorListener methods */
    public void screenColorChanged(int screenColor, boolean modified) {
        ui.getCharGridCanvas().setColorScreen(getScreenColorPalette()[screenColor]);
        updateCharButtons();
        ui.getSpriteGridCanvas().setColorScreen(getScreenColorPalette()[screenColor]);
        updateSpriteButtons();
        if (modified) {
            setModified(true);
        }
    }

    // Notification from gcChar GridCanvas
    public void undoRedoStateChanged(boolean canUndo, boolean canRedo, Object source) {
        if (source == ui.getCharGridCanvas()) {
            ui.getCharUndoButton().setEnabled(canUndo);
            ui.getCharRedoButton().setEnabled(canRedo);
        }
        else {
            ui.getSpriteUndoButton().setEnabled(canUndo);
            ui.getSpriteRedoButton().setEnabled(canRedo);
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

    public void setColorModeOption(ColorMode colorMode) {
        switch (colorMode) {
            case COLOR_MODE_GRAPHICS_1:
                setGraphicsColorMode();
                break;
            case COLOR_MODE_BITMAP:
                setBitmapColorMode();
                break;
            case COLOR_MODE_ECM_2:
                setECM2ColorMode();
                break;
            case COLOR_MODE_ECM_3:
                setECM3ColorMode();
                break;
        }
        ui.setColorModeOption(colorMode);
    }

    protected void setGraphicsColorMode() {
        ColorMode oldColorMode = dataSet.getColorMode();
        ColorMode newColorMode = COLOR_MODE_GRAPHICS_1;
        dataSet.setColorMode(newColorMode);
        preferences.setColorMode(newColorMode);
        buildColorDocks();
        // Characters
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitCharGrids(2, oldColorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        GridCanvas charCanvas = ui.getCharGridCanvas();
        charCanvas.setColorMode(newColorMode , TIGlobals.TI_PALETTE_OPAQUE);
        int[] clrSet = dataSet.getClrSets()[activeChar / 8];
        charCanvas.setColorBack(clrSet[Globals.INDEX_CLR_BACK]);
        charCanvas.setColorDraw(clrSet[Globals.INDEX_CLR_FORE]);
        DualClickButton[] charColorDockButtons = ui.getCharColorDockButtons();
        for (int i = 0; i < charColorDockButtons.length; i++) {
            charColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == charCanvas.getColorDraw() ? "F" : ""));
        }
        updateCharButtons();
        // Sprites
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitSpriteGrids(2, oldColorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        GridCanvas spriteCanvas = ui.getSpriteGridCanvas();
        spriteCanvas.setColorMode(newColorMode, TIGlobals.TI_PALETTE_OPAQUE);
        spriteCanvas.setColorBack(0);
        spriteCanvas.setColorDraw(dataSet.getSpriteColors()[activeSprite]);
        DualClickButton[] spriteColorDockButtons = ui.getSpriteColorDockButtons();
        for (int i = 0; i < spriteColorDockButtons.length; i++) {
            spriteColorDockButtons[i].setText(i == spriteCanvas.getColorBack() ? "B" : (i == spriteCanvas.getColorDraw() ? "F" : ""));
        }
        updateSpriteButtons();
    }

    protected void setBitmapColorMode() {
        ColorMode oldColorMode = dataSet.getColorMode();
        ColorMode newColorMode = COLOR_MODE_BITMAP;
        dataSet.setColorMode(newColorMode);
        preferences.setColorMode(newColorMode);
        buildColorDocks();
        // Characters
        HashMap<Integer, int[][]> charColors = new HashMap<>();
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
        GridCanvas charCanvas = ui.getCharGridCanvas();
        ui.getCharGridCanvas().setColorMode(newColorMode, TIGlobals.TI_PALETTE_OPAQUE);
        DualClickButton[] charColorDockButtons = ui.getCharColorDockButtons();
        for (int i = 0; i < charColorDockButtons.length; i++) {
            charColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == charCanvas.getColorDraw() ? "F" : ""));
        }
        updateCharButtons();
        // Sprites
        ui.getSpriteGridCanvas().setColorMode(COLOR_MODE_GRAPHICS_1, TIGlobals.TI_PALETTE_OPAQUE);
        if (oldColorMode == COLOR_MODE_ECM_2 || oldColorMode == COLOR_MODE_ECM_3) {
            limitSpriteGrids(2, oldColorMode == COLOR_MODE_ECM_2 ? 4 : 8);
        }
        GridCanvas spriteCanvas = ui.getSpriteGridCanvas();
        spriteCanvas.setColorBack(0);
        spriteCanvas.setColorDraw(dataSet.getSpriteColors()[activeSprite]);
        DualClickButton[] spriteColorDockButtons = ui.getSpriteColorDockButtons();
        for (int i = 0; i < spriteColorDockButtons.length; i++) {
            spriteColorDockButtons[i].setText(i == spriteCanvas.getColorBack() ? "B" : (i == spriteCanvas.getColorDraw() ? "F" : ""));
        }
        updateSpriteButtons();
    }

    protected void setECM2ColorMode() {
        ColorMode oldColorMode = dataSet.getColorMode();
        if (oldColorMode == COLOR_MODE_ECM_3) {
            limitCharGrids(4, 8);
            limitSpriteGrids(4, 8);
        }
        ColorMode newColorMode = COLOR_MODE_ECM_2;
        preferences.setColorMode(newColorMode);
        dataSet.setColorMode(newColorMode);
        buildColorDocks();
        ui.getCharGridCanvas().setColorMode(newColorMode, dataSet.getEcmCharPalettes()[activeChar].getColors());
        updateCharButtons();
        ui.getSpriteGridCanvas().setColorMode(newColorMode, dataSet.getEcmSpritePalettes()[activeSprite].getColors());
        updateSpriteButtons();
    }

    protected void setECM3ColorMode() {
        int[] spriteColors = dataSet.getSpriteColors();
        for (int i = 0; i < spriteColors.length; i++) {
            if (spriteColors[i] > 7) {
                spriteColors[i] >>= 1;
            }
        }
        ColorMode newColorMode = COLOR_MODE_ECM_3;
        preferences.setColorMode(newColorMode);
        dataSet.setColorMode(newColorMode);
        buildColorDocks();
        ui.getCharGridCanvas().setColorMode(newColorMode, dataSet.getEcmCharPalettes()[activeChar].getColors());
        updateCharButtons();
        ui.getSpriteGridCanvas().setColorMode(newColorMode, dataSet.getEcmSpritePalettes()[activeSprite].getColors());
        updateSpriteButtons();
    }

    public void setSuperCharacterSetOption() {
        setCharacterSetSizeSuper();
        ui.setSuperCharacterSetOption();
    }

    public void setCharacterSetSizeBasic() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_BASIC);
        ui.buildDocks();
    }

    public void setCharacterSetSizeExpanded() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_EXPANDED);
        ui.buildDocks();
    }

    public void setCharacterSetSizeSuper() {
        preferences.setCharacterSetCapacity(CHARACTER_SET_SUPER);
        ui.buildDocks();
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
                    for (int m = (doAllMaps ? 0 : mapEditor.getCurrentMapId()); m < (doAllMaps ? mapEditor.getMapCount() : mapEditor.getCurrentMapId() + 1); m++) {
                        int[][] arrayToProc = mapEditor.getMapData(m);
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
                    if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
                        int[][] swapColors = charColors.get(swapChar);
                        charColors.put(swapChar, charColors.get(baseChar));
                        charColors.put(baseChar, swapColors);
                    }
                    else if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
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
        for (int m = 0; m < mapEditor.getMapCount(); m++) {
            HashMap<Point, ArrayList<Integer>> spriteMap = mapEditor.getSpriteMap(m);
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
        if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
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
            charUsageDialog = new AnalyzeCharUsageDialog(this, mapEditor, dataSet.getCharImages(), preferences.getCharacterSetEnd());
        }
    }

    protected void analyzeCharTrans() {
        if (charTransDialog != null && charTransDialog.isVisible()) {
            charTransDialog.transferFocus();
        }
        else {
            charTransDialog = new AnalyzeCharTransDialog(this, mapEditor, dataSet.getCharImages(), dataSet.getCharGrids(), dataSet.getCharColors(), dataSet.getClrSets(), dataSet.getColorMode());
        }
    }

// File Handling Methods -------------------------------------------------------------------/

    private void readPreferences() {
        try {
            preferences.readPreferences();
            dataSet.setColorMode(preferences.getColorMode());
            mapEditor.setViewScale(preferences.getViewScale());
            mapEditor.setTypeCellOn(preferences.isTextCursor());
            mapEditor.setShowGrid(preferences.isShowGrid());
            mapEditor.setGridScale(preferences.getGridScale());
            mapEditor.setShowPosIndic(preferences.isShowPosition());
            mapEditor.setBase0Position(preferences.isBase0Position());
            mapEditor.setViewCharLayer(preferences.isViewCharLayer());
            mapEditor.setViewSpriteLayer(preferences.isViewSpriteLayer());
            mapEditor.setMagnifySprites(preferences.isMagnifySprites());
            mapEditor.setSnapSpritesToGrid(preferences.isSnapSpritesToGrid());
            mapEditor.setShowSpritesPerLine(preferences.isShowSpritesPerLine());
        } catch (Exception e) {
            showError("Error reading preferences", e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void savePreferences() {
        try {
            preferences.setColorMode(dataSet.getColorMode());
            preferences.setViewScale(mapEditor.getViewScale());
            preferences.setTextCursor(mapEditor.showTypeCell());
            preferences.setShowGrid(mapEditor.isShowGrid());
            preferences.setGridScale(mapEditor.getGridScale());
            preferences.setShowPosition(mapEditor.showPosIndic());
            preferences.setBase0Position(mapEditor.base0Position());
            preferences.setViewCharLayer(mapEditor.getViewCharLayer());
            preferences.setViewSpriteLayer(mapEditor.getViewSpriteLayer());
            preferences.setMagnifySprites(mapEditor.getMagnifySprites());
            preferences.setSnapSpritesToGrid(mapEditor.getSnapSpritesToGrid());
            preferences.setShowSpritesPerLine(mapEditor.getShowSpritesPerLine());
            preferences.savePreferences();
        } catch (IOException ioe) {
            showError("Error saving preferences", ioe.getMessage());
            ioe.printStackTrace(System.err);
        }
    }

// Class Utility Methods -------------------------------------------------------------------/

    public void newProject() {
        if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
            GridCanvas charCanvas = ui.getSpriteGridCanvas();
            DualClickButton[] charColorDockButtons = ui.getSpriteColorDockButtons();
            charCanvas.setColorBack(0);
            charCanvas.setColorDraw(1);
            for (int i = 0; i < charColorDockButtons.length; i++) {
                charColorDockButtons[i].setText(i == charCanvas.getColorBack() ? "B" : (i == charCanvas.getColorDraw() ? "F" : ""));
            }
            GridCanvas spriteCanvas = ui.getSpriteGridCanvas();
            DualClickButton[] spriteColorDockButtons = ui.getSpriteColorDockButtons();
            spriteCanvas.setColorBack(0);
            spriteCanvas.setColorDraw(1);
            for (int i = 0; i < spriteColorDockButtons.length; i++) {
                spriteColorDockButtons[i].setText(i == spriteCanvas.getColorBack() ? "B" : (i == spriteCanvas.getColorDraw() ? "F" : ""));
            }
        }
        else if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
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
                if (dataSet.getColorMode() == COLOR_MODE_GRAPHICS_1) {
                    int cellNum = (r * FONT_COLS) + c;
                    ui.getCharButtons()[cellNum].setBackground(TIGlobals.TI_PALETTE_OPAQUE[dataSet.getClrSets()[r][Globals.INDEX_CLR_BACK]]);
                    ui.getCharButtons()[cellNum].setForeground(TIGlobals.TI_COLOR_UNUSED);
                }
                else if (dataSet.getColorMode() == COLOR_MODE_BITMAP) {
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
        ui.getCharGridCanvas().resetUndoRedo();
        ui.getCharGridCanvas().clearGrid();
        activeChar = TIGlobals.CUSTOMCHAR;
        // Sprites
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            dataSet.getSpriteGrids().put(i, new int[16][16]);
            dataSet.getSpriteColors()[i] = 1;
            updateSpriteButton(i, false);
        }
        ui.getSpriteGridCanvas().resetUndoRedo();
        ui.getSpriteGridCanvas().clearGrid();
        activeSprite = 0;
        // Maps
        mapEditor.delAllMaps();
        mapEditor.setGridWidth(32);
        mapEditor.setGridHeight(24);
        mapEditor.setColorScreen(0);
        mapEditor.redrawCanvas();
        mapEditor.resetUndoManager();
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
        if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
            ui.updateCharPaletteCombo(-1);
        }
        // Edit default sprite
        activeSprite = 0;
        ActionEvent aeInitSprite = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Globals.CMD_EDIT_SPR + activeSprite);
        Magellan.this.actionPerformed(aeInitSprite);
        if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
            ui.updateSpritePaletteCombo(-1);
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
        URL logo = getClass().getResource("images/logo.png");
        JOptionPane.showMessageDialog(this, jEditorPane, title, JOptionPane.INFORMATION_MESSAGE, logo != null ? new ImageIcon(logo) : null);
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
        if (dataSet.getColorMode() == COLOR_MODE_ECM_2 || dataSet.getColorMode() == COLOR_MODE_ECM_3) {
            updatePalettes();
        }
    }

    public void updateComponents() {
        ColorMode colorMode = dataSet.getColorMode();
        // Character editor
        updateCharButton(activeChar, false);
        ui.getLookButton().setBackground((mapEditor.isLookModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
        ui.getTransparencyCheckBox().setVisible(colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3);
        ui.getTransparencyCheckBox().setSelected(dataSet.getEcmCharTransparency()[activeChar]);
        int[][] charGridData = ui.getCharGridCanvas().getGridData();
        ui.getCharTextField().setText((
            Globals.getHexString(charGridData)
            + (colorMode == COLOR_MODE_BITMAP ? Globals.getColorHexString(ui.getCharGridCanvas().getGridColors()) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getHexString(charGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(dataSet.getEcmCharPalettes()[activeChar]), 4) : "")
        ).toUpperCase());
        ui.getCharTextField().setCaretPosition(0);
        // Sprite editor
        updateSpriteButton(activeSprite, false);
        int[][] spriteGridData = ui.getSpriteGridCanvas().getGridData();
        ui.getSpriteTextField().setText((
            Globals.getSpriteHexString(spriteGridData)
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 2) : "")
            + (colorMode == COLOR_MODE_ECM_3 ? Globals.getSpriteHexString(spriteGridData, 4) : "")
            + (colorMode == COLOR_MODE_ECM_2 || colorMode == COLOR_MODE_ECM_3 ? Globals.toHexString(getECMPaletteIndex(dataSet.getEcmSpritePalettes()[activeSprite]), 4) : "")
        ).toUpperCase());
        ui.getSpriteTextField().setCaretPosition(0);
        // Map
        mapEditor.updateComponents();
    }

    public void updateCharButtons() {
        for (int i = TIGlobals.MIN_CHAR; i <= TIGlobals.MAX_CHAR; i++) {
            updateCharButton(i, false);
        }
        mapEditor.redrawCanvas();
    }

    protected void updateCharButton(int charNum) {
        updateCharButton(charNum, true);
    }

    protected void updateCharButton(int charNum, boolean redrawMap) {
        JButton[] charButtons = ui.getCharButtons();
        // Set border
        if (lastActiveChar != charNum && lastActiveChar != MapCanvas.NOCHAR) {
            charButtons[lastActiveChar].setBorder(Globals.bordButtonNormal);
        }
        lastActiveChar = charNum;
        charButtons[charNum].setBorder(Globals.bordButtonActive);
        // Update labels showing number
        ui.getCharIntLabel().setText((charNum < 100 ? " " : "") + charNum);
        ui.getCharHexLabel().setText(">" + Integer.toHexString(charNum).toUpperCase());
        // Background color
        Color screenColor = getScreenColorPalette()[mapEditor.getColorScreen()];
        charButtons[charNum].setBackground(screenColor);
        // Set text for buttons with no char grid
        if (dataSet.getCharGrids().get(charNum) == null) {
            if (charButtons[charNum].getIcon() != null) {
                charButtons[charNum].setIcon(null);
                int charMapIndex = charNum - TIGlobals.CHARMAPSTART;
                charButtons[charNum].setText(charMapIndex >= 0 && charMapIndex < TIGlobals.CHARMAP.length ? "" + TIGlobals.CHARMAP[charMapIndex] : "?");
            }
            return;
        }
        // Generate icon image
        IconImage iconImage = generateIconImage(charNum, screenColor);
        // Save image
        dataSet.getCharImages().put(charNum, iconImage.getImage());
        mapEditor.setCharImage(charNum, iconImage.getImage());
        // Display a question mark if image is empty (only some modes)
        if (iconImage.isEmpty()) {
            charButtons[charNum].setIcon(null);
            charButtons[charNum].setText(((charNum - TIGlobals.CHARMAPSTART) >= 0 && (charNum - TIGlobals.CHARMAPSTART) < TIGlobals.CHARMAP.length) ? "" + TIGlobals.CHARMAP[charNum - TIGlobals.CHARMAPSTART] : "?");
        }
        else {
            charButtons[charNum].setIcon(new ImageIcon(iconImage.getImage()));
            charButtons[charNum].setText("");
        }
        // Redraw map as requested
        if (redrawMap) {
            mapEditor.redrawCanvas();
        }
    }

    private IconImage generateIconImage(int charNum, Color screenColor) {
        ColorMode colorMode = dataSet.getColorMode();
        int imageScale = 2;
        int[][] gridData = ui.getCharGridCanvas().getGridData();
        Image image = this.createImage(gridData.length * imageScale, gridData[0].length * imageScale);
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
        return new IconImage(image, empty);
    }

    public void updateSpriteButtons() {
        for (int i = TIGlobals.MIN_SPRITE; i <= TIGlobals.MAX_SPRITE; i++) {
            updateSpriteButton(i, false);
        }
        mapEditor.redrawCanvas();
    }

    protected void updateSpriteButton(int spriteNum) {
        updateSpriteButton(spriteNum, true);
    }

    protected void updateSpriteButton(int spriteNum, boolean redrawMap) {
        JButton[] spriteButtons = ui.getSpriteButtons();
        // Set border
        if (lastActiveSprite != spriteNum && lastActiveSprite != MapCanvas.NOCHAR) {
            spriteButtons[lastActiveSprite].setBorder(Globals.bordButtonNormal);
        }
        lastActiveSprite = spriteNum;
        spriteButtons[spriteNum].setBorder(Globals.bordButtonActive);
        // Update labels showing number
        ui.getSpriteIntLabel().setText((spriteNum < 100 ? " " : "") + spriteNum);
        ui.getSpriteHexLabel().setText(">" + Integer.toHexString(spriteNum).toUpperCase());
        // Background color
        Color screenColor = getScreenColorPalette()[mapEditor.getColorScreen()];
        spriteButtons[spriteNum].setBackground(screenColor);
        // Handle missing sprite grid
        if (dataSet.getSpriteGrids().get(spriteNum) == null) {
            if (spriteButtons[spriteNum].getIcon() != null) {
                spriteButtons[spriteNum].setIcon(null);
                spriteButtons[spriteNum].setText(Integer.toString(spriteNum));
            }
            return;
        }
        // Generate icon image
        Image image = generateSpriteImage(spriteNum);
        // Save image
        dataSet.getSpriteImages().put(spriteNum, image);
        mapEditor.setSpriteImage(spriteNum, image);
        // Display a default text if image is empty
        if (image == null) {
            spriteButtons[spriteNum].setIcon(null);
            spriteButtons[spriteNum].setText(Integer.toString(spriteNum));
        }
        else {
            spriteButtons[spriteNum].setIcon(new ImageIcon(image));
            spriteButtons[spriteNum].setText("");
        }
        // Redraw map as requested
        if (redrawMap) {
            mapEditor.redrawCanvas();
        }
    }

    private Image generateSpriteImage(int spriteNum) {
        ColorMode colorMode = dataSet.getColorMode();
        int imageScale = 3;
        Image image = this.createImage(ui.getSpriteGridCanvas().getGridData().length * imageScale, ui.getSpriteGridCanvas().getGridData()[0].length * imageScale);
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
        return !empty ? makeImageTransparent(image) : null;
    }

    public Color[] getScreenColorPalette() {
        ColorMode colorMode = dataSet.getColorMode();
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

    public void updateScreenColorPalette() {
        mapEditor.setScreenColorPalette(getScreenColorPalette());
    }

    public void setECMPaletteColor(ECMPalette ecmPalette, int index, Color color) {
        ui.setECMPaletteColor(ecmPalette, index, color, getScreenColorPalette()[mapEditor.getColorScreen()]);
        updateCharButtons();
        updateSpriteButtons();
        updateScreenColorPalette();
        setModified(true);
    }

    public void updatePalettes() {
        ui.updatePalettes(getScreenColorPalette()[mapEditor.getColorScreen()]);
        updateCharButtons();
        updateSpriteButtons();
        updateScreenColorPalette();
    }
}
