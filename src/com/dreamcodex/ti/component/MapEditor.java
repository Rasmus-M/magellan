package com.dreamcodex.ti.component;

import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.iface.MapSelectListener;
import com.dreamcodex.ti.iface.ScreenColorListener;
import com.dreamcodex.ti.iface.UndoRedoListener;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.HashPoint;
import com.dreamcodex.ti.util.NotifyingUndoManager;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MapEditor extends JPanel implements ItemListener, ActionListener, KeyListener, UndoRedoListener, FocusListener {
// Constants -------------------------------------------------------------------------------/

    public static final String[] MAGNIFICATIONS = {"1x", "2x", "3x", "4x", "5x", "6x", "7x", "8x"};

// Components ------------------------------------------------------------------------------/

    protected MapCanvas mpCanvas;
    protected JScrollPane jsclCanvas;
    protected JComboBox jcmbScreen;
    protected JComboBox jcmbMagnif;
    protected JComboBox jcmbGridScale;
    protected JButton jbtnToggleGrid;
    protected JButton jbtnFloodFillMode;
    protected JButton jbtnPrevMap;
    protected JButton jbtnNextMap;
    protected JButton jbtnBackMap;
    protected JButton jbtnForwMap;
    protected JButton jbtnTextCursor;
    protected JButton jbtnClone;
    protected JButton jbtnUndo;
    protected JButton jbtnRedo;
    protected JLabel jlblMapNum;
    protected JTextField jtxtWidth;
    protected JTextField jtxtHeight;
    protected JLabel jlblPosLabel;
    protected JLabel jlblPosIndic;

// Variables -------------------------------------------------------------------------------/

    protected ArrayList<int[][]> arMaps = new ArrayList<int[][]>();
    protected ArrayList<Integer> arClrs = new ArrayList<Integer>();
    protected ArrayList<HashMap<Point, ArrayList<Integer>>> spriteMaps = new ArrayList<HashMap<Point, ArrayList<Integer>>>();
    protected int currMap = 0;
    protected boolean showPositionIndicator = true;
    protected boolean base0forPosition = false;
    protected ArrayList<ScreenColorListener> screenColorListeners = new ArrayList<ScreenColorListener>();
    protected ArrayList<MapChangeListener> mapChangeListeners = new ArrayList<MapChangeListener>();
    protected ArrayList<MapSelectListener> mapSelectListeners = new ArrayList<MapSelectListener>();
    protected NotifyingUndoManager undoManager;

// Constructors ----------------------------------------------------------------------------/

    public MapEditor(int gridWidth, int gridHeight, int cellSize, MouseListener mlParent, MouseMotionListener mmlParent) {

        super(new BorderLayout(), true);
        setOpaque(true);
        addKeyListener(this);

        undoManager = new NotifyingUndoManager();
        undoManager.addUndoRedoListener(this);

        mpCanvas = new MapCanvas(gridWidth, gridHeight, cellSize, mlParent, mmlParent, undoManager);

        arMaps.add(mpCanvas.getGridData());
        arClrs.add(mpCanvas.getColorScreen());
        spriteMaps.add(mpCanvas.getSpriteMap());

        jcmbScreen = new JComboBox(TIGlobals.TI_PALETTE_SELECT_VALUES);
        jcmbScreen.addItemListener(this);
        jcmbScreen.setSelectedIndex(mpCanvas.getColorScreen());
        jcmbScreen.setRenderer(new ColorComboRenderer());

        jcmbMagnif = new JComboBox(MAGNIFICATIONS);
        jcmbMagnif.addItemListener(this);
        jcmbMagnif.setSelectedIndex(0);
        jcmbMagnif.setToolTipText("Magnification");

        jcmbGridScale = new JComboBox(new Integer[] {1, 2, 4, 8, 12, 16, 24});
        jcmbGridScale.addItemListener(this);
        jcmbGridScale.setToolTipText("Grid Size");

        JButton jbtnFillSpace = getToolButton(Globals.CMD_CLEAR_CHR, "Fill map with spaces");
        JButton jbtnFillActive = getToolButton(Globals.CMD_FILL_CHR, "Fill map with active character");
        jbtnFloodFillMode = getToolButton(Globals.CMD_FLOOD_FILL, "Flood fill map with active character");
        JButton jbtnRotateLeft = getToolButton(Globals.CMD_ROTATEL_MAP, "Rotate map left (will swap dimensions)");
        JButton jbtnRotateRight = getToolButton(Globals.CMD_ROTATER_MAP, "Rotate map right (will swap dimensions)");
        JButton jbtnFlipH = getToolButton(Globals.CMD_FLIPH_MAP, "Flip horizontal");
        JButton jbtnFlipV = getToolButton(Globals.CMD_FLIPV_MAP, "Flip vertical");
        jbtnToggleGrid = getToolButton(Globals.CMD_GRID_CHR, "Toggle grid on and off");
        JButton jbtnAddMap = getToolButton(Globals.CMD_ADDMAP, "Add new map");
        jbtnAddMap.setBackground(new Color(192, 255, 192));
        JButton jbtnDelMap = getToolButton(Globals.CMD_DELMAP, "Delete current map");
        jbtnDelMap.setBackground(new Color(255, 192, 192));
        jbtnPrevMap = getToolButton(Globals.CMD_PREVMAP, "Go to previous map");
        jbtnNextMap = getToolButton(Globals.CMD_NEXTMAP, "Go to next map");
        jbtnBackMap = getToolButton(Globals.CMD_BACKMAP, "Move map backward in set");
        jbtnForwMap = getToolButton(Globals.CMD_FORWMAP, "Move map forward in set");
        jbtnTextCursor = getToolButton(Globals.CMD_TCURSOR, "Toggle text cursor display");
        jbtnClone = getToolButton(Globals.CMD_CLONE, "Select Region To Clone");
        jbtnUndo = getToolButton(Globals.CMD_UNDO_CHR, "Undo Edit");
        jbtnUndo.setEnabled(false);
        jbtnRedo = getToolButton(Globals.CMD_REDO_CHR, "Redo Edit");
        jbtnRedo.setEnabled(false);

        jbtnTextCursor.setBackground(mpCanvas.showTypeCell() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL);
        jlblMapNum = getLabel("", JLabel.CENTER);
        jtxtWidth = getTextField("" + gridWidth);
        jtxtHeight = getTextField("" + gridHeight);
        jlblPosLabel = getLabel(" Pos", JLabel.RIGHT);
        jlblPosIndic = getLabel("", JLabel.CENTER);

        JPanel jpnlTools = getPanel(new FlowLayout(FlowLayout.LEFT));
        jpnlTools.setOpaque(true);
        jpnlTools.setBackground(Globals.CLR_COMPONENTBACK);
        jpnlTools.add(getLabel("Screen", JLabel.RIGHT));
        jpnlTools.add(jcmbScreen);
        jpnlTools.add(getLabel("Mag", JLabel.RIGHT));
        jpnlTools.add(jcmbMagnif);
        jpnlTools.add(jbtnToggleGrid);
        jpnlTools.add(jcmbGridScale);
        jpnlTools.add(new JLabel("  "));
        jpnlTools.add(jbtnFillSpace);
        jpnlTools.add(jbtnFillActive);
        jpnlTools.add(jbtnFloodFillMode);
        jpnlTools.add(new JLabel("  "));
        jpnlTools.add(jbtnRotateLeft);
        jpnlTools.add(jbtnRotateRight);
        jpnlTools.add(jbtnFlipH);
        jpnlTools.add(jbtnFlipV);
        jpnlTools.add(new JLabel("  "));
        jpnlTools.add(jbtnTextCursor);
        jpnlTools.add(jbtnClone);
        jpnlTools.add(new JLabel("  "));
        jpnlTools.add(jbtnAddMap);
        jpnlTools.add(jbtnPrevMap);
        jpnlTools.add(jlblMapNum);
        jpnlTools.add(jbtnNextMap);
        jpnlTools.add(jbtnDelMap);
        jpnlTools.add(new JLabel("  "));
        jpnlTools.add(jbtnBackMap);
        jpnlTools.add(jbtnForwMap);
        jpnlTools.add(getLabel(" W:", JLabel.RIGHT));
        jpnlTools.add(jtxtWidth);
        jpnlTools.add(getLabel(" H:", JLabel.RIGHT));
        jpnlTools.add(jtxtHeight);

        JPanel jpnlUndoRedo = getPanel(new FlowLayout());
        jpnlUndoRedo.add(jbtnUndo);
        jpnlUndoRedo.add(jbtnRedo);

        JPanel jpnlToolsContainer = getPanel(new BorderLayout());
        jpnlToolsContainer.add(jpnlTools, BorderLayout.WEST);
        jpnlToolsContainer.add(jpnlUndoRedo, BorderLayout.EAST);

        JPanel jpnlStatus = getPanel(new FlowLayout(FlowLayout.LEFT));
        jpnlStatus.add(jlblPosLabel);
        jpnlStatus.add(jlblPosIndic);

        jsclCanvas = new JScrollPane(mpCanvas);
        jsclCanvas.setBorder(BorderFactory.createLineBorder(new Color(64, 64, 64), 1));

        this.add(jsclCanvas, BorderLayout.CENTER);
        this.add(jpnlToolsContainer, BorderLayout.NORTH);
        this.add(jpnlStatus, BorderLayout.SOUTH);

        mpCanvas.requestFocus();
    }

// Accessors -------------------------------------------------------------------------------/

    public int[][] getGridData() {
        return mpCanvas.getGridData();
    }

    public HashMap<Point, ArrayList<Integer>> getSpriteMap() {
        return mpCanvas.getSpriteMap();
    }

    public Color getColorGrid() {
        return mpCanvas.getColorGrid();
    }

    public Color getColorHigh() {
        return mpCanvas.getColorHigh();
    }

    public boolean isPaintOn() {
        return mpCanvas.isPaintOn();
    }

    public int getActiveChar() {
        return mpCanvas.getActiveChar();
    }

    public int getViewScale() {
        return mpCanvas.getViewScale();
    }

    public int getColorScreen() {
        return mpCanvas.getColorScreen();
    }

    public boolean isShowGrid() {
        return mpCanvas.isShowGrid();
    }

    public boolean isFloodFillModeOn() {
        return mpCanvas.isFloodFillModeOn();
    }

    public int getGridScale() {
        return mpCanvas.getGridScale();
    }

    public boolean isLookModeOn() {
        return mpCanvas.isLookModeOn();
    }

    public boolean isCloneModeOn() {
        return mpCanvas.isCloneModeOn();
    }

    public int getLookChar() {
        return mpCanvas.getLookChar();
    }

    public Point getHotCell() {
        return mpCanvas.getHotCell();
    }

    public Point getTypeCell() {
        return mpCanvas.getTypeCell();
    }

    public boolean showTypeCell() {
        return mpCanvas.showTypeCell();
    }

    public Color getBkgrndColor() {
        return this.getBackground();
    }

    public int getMapCount() {
        return arMaps.size();
    }

    public int getCurrentMapId() {
        return currMap;
    }

    public boolean showPosIndic() {
        return showPositionIndicator;
    }

    public boolean base0Position() {
        return base0forPosition;
    }

    public int[][] getMapData(int i) {
        if ((i == currMap) || (i < 0) || (i >= arMaps.size())) {
            return getGridData();
        } else {
            return arMaps.get(i);
        }
    }

    public int getScreenColor(int i) {
        if ((i == currMap) || (i < 0) || (i >= arClrs.size())) {
            return getColorScreen();
        } else {
            return arClrs.get(i);
        }
    }

    public HashMap<Point, ArrayList<Integer>> getSpriteMap(int i) {
        if ((i == currMap) || (i < 0) || (i >= spriteMaps.size())) {
            return getSpriteMap();
        } else {
            return spriteMaps.get(i);
        }
    }

    public int getScreenColorTI(int i) {
        return getScreenColor(i) + 1;
    }

    public void setGridData(int[][] gd) {
        mpCanvas.setGridData(gd);
    }

    public void setColorGrid(Color clr) {
        mpCanvas.setColorGrid(clr);
    }

    public void setColorHigh(Color clr) {
        mpCanvas.setColorHigh(clr);
    }

    public void setPaintOn(boolean b) {
        mpCanvas.setPaintOn(b);
    }

    public void setActiveChar(int i) {
        mpCanvas.setActiveChar(i);
    }

    public void setActiveSprite(int i) {
        mpCanvas.setActiveSprite(i);
    }

    public void setViewScale(int i) {
        mpCanvas.setViewScale(i);
        jcmbMagnif.setSelectedIndex(i - 1);
    }

    public void setGridScale(int i) {
        mpCanvas.setGridScale(i);
        jcmbGridScale.setSelectedItem(i);
    }

    public void setColorScreen(int i) {
        mpCanvas.setColorScreen(i);
        jcmbScreen.setSelectedIndex(i);
    }

    public void setShowGrid(boolean b) {
        mpCanvas.setShowGrid(b);
    }

    public void setLookModeOn(boolean b) {
        mpCanvas.setLookModeOn(b);
    }

    public void setCloneModeOn(boolean b) {
        mpCanvas.setCloneModeOn(b);
    }

    public void toggleCloneMode() {
        mpCanvas.toggleCloneMode();
    }

    public void setLookChar(int i) {
        mpCanvas.setLookChar(i);
    }

    public void setHotCell(Point pt) {
        mpCanvas.setHotCell(pt);
    }

    public void setTypeCell(Point pt) {
        mpCanvas.setTypeCell(pt);
    }

    public void setTypeCellOn(boolean b) {
        mpCanvas.setTypeCellOn(b);
    }

    public void setBkgrndColor(Color clr) {
        this.setBackground(clr);
        mpCanvas.setBackground(clr);
    }

    public void setCurrentMapId(int i) {
        currMap = Math.max(Math.min((arMaps.size() - 1), i), 0);
    }

    public void setShowPosIndic(boolean b) {
        showPositionIndicator = b;
        jlblPosLabel.setVisible(showPositionIndicator);
        jlblPosIndic.setVisible(showPositionIndicator);
        updateComponents();
    }

    public void setBase0Position(boolean b) {
        base0forPosition = b;
        updateComponents();
    }

    public void toggleShowPosIndic() {
        setShowPosIndic(!showPositionIndicator);
    }

    public void toggleBase0Position() {
        setBase0Position(!base0forPosition);
    }

    public void addMapData(int[][] m, int c, HashMap<Point, ArrayList<Integer>> s) {
        addMap(m, c, s);
    }

    public void setMapData(int i, int[][] m, int c, HashMap<Point, ArrayList<Integer>> s) {
        storeMap(i, m, c, s);
    }

    public void setScreenColor(int i, int c) {
        arClrs.set(i, new Integer(c));
    }

    public int getGridWidth() {
        return mpCanvas.getGridWidth();
    }

    public int getGridHeight() {
        return mpCanvas.getGridHeight();
    }

    public void setGridWidth(int i) {
        mpCanvas.setGridWidth(i);
    }

    public void setGridHeight(int i) {
        mpCanvas.setGridHeight(i);
    }

    public void highlightCell(int x, int y) {
        mpCanvas.highlightCell(x, y);
    }

    public void removeAllHighlights() {
        mpCanvas.removeAllHighlights();
    }

    public int getGridAt(int x, int y) {
        return mpCanvas.getGridAt(x, y);
    }

    public void setGridAt(int x, int y, int v) {
        mpCanvas.setGridAt(x, y, v);
    }

    public void setGridAt(Point pt, int v) {
        mpCanvas.setGridAt(pt, v);
    }

    public void setCharImage(int charnum, Image img) {
        mpCanvas.setCharImage(charnum, img);
    }

    public void setSpriteImage(int spritenum, Image img) {
        mpCanvas.setSpriteImage(spritenum, img);
    }

    public void setSpriteMode(boolean enabled) {
        if (enabled) {
            setLookModeOn(false);
            setCloneModeOn(false);
            setTypeCellOn(false);
        }
        mpCanvas.setSpriteMode(enabled);
        updateComponents();
    }

    public boolean isSpriteMode() {
        return mpCanvas.isSpriteMode();
    }

    public boolean getViewCharLayer() {
        return mpCanvas.getViewCharLayer();
    }

    public void setViewCharLayer(boolean viewCharLayer) {
        mpCanvas.setViewCharLayer(viewCharLayer);
    }

    public boolean getViewSpriteLayer() {
        return mpCanvas.getViewSpriteLayer();
    }

    public void setViewSpriteLayer(boolean viewSpriteLayer) {
        mpCanvas.setViewSpriteLayer(viewSpriteLayer);
    }

    public boolean getMagnifySprites() {
        return mpCanvas.getMagnifySprites();
    }

    public void setMagnifySprites(boolean magnifySprites) {
        mpCanvas.setMagnifySprites(magnifySprites);
    }

    public BufferedImage getBuffer() {
        return mpCanvas.getBuffer();
    }

    public void advanceTypeCell() {
        mpCanvas.advanceTypeCell();
    }

    public void redrawCanvas() {
        mpCanvas.redrawCanvas();
    }

    public void addMap(int[][] mapdata, int scrclr, HashMap<Point, ArrayList<Integer>> spriteMap) {
        arMaps.add(mapdata);
        arClrs.add(scrclr);
        spriteMaps.add(spriteMap);
    }

    public void addBlankMap(int w, int h) {
        int[][] newMap = new int[h][w];
        for (int y = 0; y < newMap.length; y++) {
            for (int x = 0; x < newMap[y].length; x++) {
                newMap[y][x] = TIGlobals.SPACECHAR;
            }
        }
        arMaps.add(newMap);
        arClrs.add((mpCanvas != null ? getColorScreen() : 15));
        HashMap<Point, ArrayList<Integer>> spriteMap = new HashMap<Point, ArrayList<Integer>>();
        spriteMaps.add(spriteMap);
        mpCanvas.setSpriteMap(spriteMap);
    }

    public void addBlankMap() {
        addBlankMap(getGridWidth(), getGridHeight());
    }

    public void delMap(int index) {
        if ((index >= 0) && (index < arMaps.size())) {
            arMaps.remove(index);
            arClrs.remove(index);
            spriteMaps.remove(index);
            index--;
            if (index < 0) {
                index = 0;
            }
            if (arMaps.size() < 1) {
                addBlankMap();
            }
            goToMap(index, false);
        }
    }

    public void delAllMaps() {
        clearGrid();
        clearSpriteMap();
        for (int i = arMaps.size() - 1; i >= 0; i--) {
            delMap(i);
        }
    }

    public void storeMap(int index, int[][] mapdata, int scrcolor, HashMap<Point, ArrayList<Integer>> spriteMap) {
        if ((index >= 0) && (index < arMaps.size())) {
            arMaps.set(index, cloneMapArray(mapdata));
        }
        if ((index >= 0) && (index < arClrs.size())) {
            arClrs.set(index, scrcolor);
        }
        if ((index >= 0) && (index < spriteMaps.size())) {
            spriteMaps.set(index, cloneSpriteMap(spriteMap));
        }
    }

    public void storeCurrentMap() {
        storeMap(currMap, mpCanvas.getGridData(), mpCanvas.getColorScreen(), mpCanvas.getSpriteMap());
    }

    public void updateState(int index) {
        mpCanvas.setGridData(arMaps.get(index));
        mpCanvas.setColorScreen(arClrs.get(index));
        mpCanvas.setSpriteMap(spriteMaps.get(index));
    }

    public int[][] cloneMapArray(int[][] mapsrc) {
        int[][] cloneArray = new int[mapsrc.length][mapsrc[0].length];
        for (int y = 0; y < mapsrc.length; y++) {
            System.arraycopy(mapsrc[y], 0, cloneArray[y], 0, mapsrc[y].length);
        }
        return cloneArray;
    }

    public HashMap<Point, ArrayList<Integer>> cloneSpriteMap(HashMap<Point, ArrayList<Integer>> spriteMap) {
        HashMap<Point, ArrayList<Integer>> cloneSpriteMap = new HashMap<Point, ArrayList<Integer>>();
        for (Point p : spriteMap.keySet()) {
            ArrayList<Integer> cloneSpriteList = new ArrayList<Integer>();
            for (Integer i : spriteMap.get(p)) {
                cloneSpriteList.add(i);
            }
            cloneSpriteMap.put(new HashPoint(p), cloneSpriteList);

        }
        return cloneSpriteMap;
    }

    public void goToMap(int index, boolean storeCurr) {
        if ((index >= 0) && (index < arMaps.size())) {
            if (storeCurr) {
                storeCurrentMap();
            }
            currMap = index;
            updateState(currMap);
            updateComponents();
        }
    }

    public void goToMap(int index) {
        goToMap(index, true);
    }

    public void swapMaps(int mapA, int mapB) {
        if (mapA != mapB) {
            int[][] tmpMap = getMapData(mapA);
            Integer tmpClr = getScreenColor(mapA);
            HashMap<Point, ArrayList<Integer>> tmpSpr = getSpriteMap(mapA);
            storeMap(mapA, getMapData(mapB), getScreenColor(mapB), getSpriteMap(mapB));
            storeMap(mapB, tmpMap, tmpClr, tmpSpr);
        }
    }

    public int moveMap(int index, boolean forward) {
        if (index < 0 || index >= arMaps.size()) {
            return index;
        }
        int newIndex = index + (forward ? 1 : -1);
        if (newIndex < 0) {
            newIndex = arMaps.size() - 1;
        } else if (newIndex >= arMaps.size()) {
            newIndex = 0;
        }
        swapMaps(index, newIndex);
        return newIndex;
    }

    public int moveMapForward(int index) {
        return moveMap(index, true);
    }

    public int moveMapBackward(int index) {
        return moveMap(index, false);
    }

    public void resetUndoManager() {
        undoManager.discardAllEdits();
    }

    public void setScreenColorPalette(Color[] palette) {
        ((ColorComboRenderer) jcmbScreen.getRenderer()).setPalette(palette);
        jcmbScreen.revalidate();
    }

// Component Builder Methods ---------------------------------------------------------------/

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
        String imagePath = "images/icon_" + name + ".png";
        URL imageURL = getClass().getResource(imagePath);
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(imageURL));
    }

    protected JButton getToolButton(String actcmd, String tooltip) {
        JButton jbtnTool = new JButton(getIcon(actcmd));
        jbtnTool.setActionCommand(actcmd);
        jbtnTool.addActionListener(this);
        jbtnTool.setToolTipText(tooltip);
        jbtnTool.setMargin(new Insets(1, 1, 1, 1));
        jbtnTool.setOpaque(true);
        jbtnTool.setBackground(Globals.CLR_BUTTON_NORMAL);
        jbtnTool.setPreferredSize(Globals.DM_TOOL);
        return jbtnTool;
    }

    protected JLabel getLabel(String text, int align) {
        JLabel jlblRtn = new JLabel(text, align);
        jlblRtn.setOpaque(true);
        jlblRtn.setBackground(Globals.CLR_COMPONENTBACK);
        return jlblRtn;
    }

    protected JTextField getTextField(String text) {
        JTextField jtxtRtn = new JTextField(text);
        jtxtRtn.setHorizontalAlignment(SwingConstants.RIGHT);
        jtxtRtn.addActionListener(this);
        jtxtRtn.addFocusListener(this);
        jtxtRtn.setPreferredSize(Globals.DM_TEXT);
        jtxtRtn.setBorder(BorderFactory.createLineBorder(new Color(64, 64, 64), 1));
        return jtxtRtn;
    }

// Rendering Methods -----------------------------------------------------------------------/

    public void clearGrid() {
        mpCanvas.clearGrid();
    }

    public void fillGrid(int v) {
        mpCanvas.fillGrid(v);
    }

    public void clearSpriteMap() {
        mpCanvas.clearSpriteMap();
    }

    public void updateComponents() {
        redrawCanvas();
        if (arMaps.size() > 1) {
            jbtnPrevMap.setEnabled(true);
            jbtnNextMap.setEnabled(true);
            jbtnBackMap.setEnabled(true);
            jbtnForwMap.setEnabled(true);
        } else {
            jbtnPrevMap.setEnabled(false);
            jbtnNextMap.setEnabled(false);
            jbtnBackMap.setEnabled(false);
            jbtnForwMap.setEnabled(false);
        }
        jcmbScreen.setSelectedIndex(mpCanvas.getColorScreen());
        jsclCanvas.revalidate();
        jlblMapNum.setText((currMap + 1) + " / " + getMapCount());
        jtxtWidth.setText("" + mpCanvas.getGridWidth());
        jtxtHeight.setText("" + mpCanvas.getGridHeight());
        jbtnTextCursor.setBackground(mpCanvas.showTypeCell() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL);
        jbtnClone.setBackground(isCloneModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL);
        jbtnToggleGrid.setBackground(isShowGrid() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL);
        jbtnFloodFillMode.setBackground(isFloodFillModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL);
        updatePositionIndicator();
    }

    public void updatePositionIndicator() {
        if (showPositionIndicator) {
            if (getHotCell().equals(MapCanvas.PT_OFFGRID)) {
                jlblPosIndic.setText("(X, Y) = ( - , - ) = ( - , - )    (PX, PY) = ( - , - ) = ( - , - )");
            } else {
                int posx = (int) getHotCell().getX();
                int posy = (int) getHotCell().getY();
                int pixx = posx * 8;
                int pixy = posy * 8;
                if (!base0forPosition) {
                    posx++;
                    posy++;
                    pixx++;
                    pixy++;
                }
                jlblPosIndic.setText(
                    "(X, Y) = (" + posx + ", " + posy + ") = (>" + Globals.toHexString(posx, 4) + ", >" + Globals.toHexString(posy, 4) + ")    " +
                    "(PX, PY) = (" + pixx + ", " + pixy + ") = (>" + Globals.toHexString(pixx, 4) + ", >" + Globals.toHexString(pixy, 4) + ")"
                );
            }
        }
    }

// Listener Methods ------------------------------------------------------------------------/

    public void addScreenColorListener(ScreenColorListener screenColorListener) {
        screenColorListeners.add(screenColorListener);
    }

    public void addMapChangeListener(MapChangeListener mapChangeListener) {
        mapChangeListeners.add(mapChangeListener);
        mpCanvas.addMapChangeListener(mapChangeListener);
    }

    public void removeMapChangeListener(MapChangeListener mapChangeListener) {
        mapChangeListeners.remove(mapChangeListener);
        mpCanvas.removeMapChangeListener(mapChangeListener);
    }

    private void notifyMapChangedListeners() {
        for (MapChangeListener mapChangeListener : mapChangeListeners) {
            mapChangeListener.mapChanged();
        }
    }

    public void addMapSelectListener(MapSelectListener mappSelectListener) {
        mapSelectListeners.add(mappSelectListener);
    }

    public void removeMapSelectListener(MapSelectListener mapSelectListener) {
        mapSelectListeners.remove(mapSelectListener);
    }

    private void notifyMapSelectListeners() {
        for (MapSelectListener mapSelectListener : mapSelectListeners) {
            mapSelectListener.mapSelected();
        }
    }

    /* ItemListener methods */
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(jcmbScreen)) {
            boolean modified = false;
            if (jcmbScreen.getSelectedIndex() != mpCanvas.getColorScreen()) {
                mpCanvas.setColorScreen(jcmbScreen.getSelectedIndex());
                modified = true;
            }
            mpCanvas.redrawCanvas();
            // Notify listeners
            for (ScreenColorListener screenColorListener : screenColorListeners) {
                screenColorListener.screenColorChanged(getColorScreen(), modified);
            }
        } else if (ie.getSource().equals(jcmbMagnif)) {
            mpCanvas.setViewScale(jcmbMagnif.getSelectedIndex() + 1);
            mpCanvas.redrawCanvas();
            mpCanvas.revalidate();
            jsclCanvas.revalidate();
        } else if (ie.getSource().equals(jcmbGridScale)) {
            mpCanvas.setGridScale((Integer) jcmbGridScale.getSelectedItem());
            mpCanvas.redrawCanvas();
            mpCanvas.revalidate();
            jsclCanvas.revalidate();
        }
    }

    /* ActionListener methods */
    public void actionPerformed(ActionEvent ae) {
        try {
            mpCanvas.requestFocus();
            if (ae.getSource().equals(jtxtWidth)) {
                updateWidth();
            } else if (ae.getSource().equals(jtxtHeight)) {
                updateHeight();
            } else {
                String command = ae.getActionCommand();
                if (command.equals(Globals.CMD_CLEAR_CHR)) {
                    if (confirmationAction("Confirm Clear", "Are you sure you want to fill the map with spaces?") == JOptionPane.YES_OPTION) {
                        clearGrid();
                        updateComponents();
                    }
                } else if (command.equals(Globals.CMD_FILL_CHR)) {
                    if (confirmationAction("Confirm Fill", "Are you sure you want to fill the map with the active character?") == JOptionPane.YES_OPTION) {
                        fillGrid(mpCanvas.getActiveChar());
                        updateComponents();
                    }
                } else if (command.equals(Globals.CMD_FLOOD_FILL)) {
                    mpCanvas.toggleFloodFillMode();
                    updateComponents();
                } else if (command.equals(Globals.CMD_ROTATEL_MAP)) {
                    mpCanvas.rotateLeft(true);
                    jsclCanvas.revalidate();
                    updateComponents();
                } else if (command.equals(Globals.CMD_ROTATER_MAP)) {
                    mpCanvas.rotateRight(true);
                    jsclCanvas.revalidate();
                    updateComponents();
                } else if (command.equals(Globals.CMD_FLIPH_MAP)) {
                    mpCanvas.flipHorizontal();
                    updateComponents();
                    return;
                } else if (command.equals(Globals.CMD_FLIPV_MAP)) {
                    mpCanvas.flipVertical();
                    updateComponents();
                    return;
                } else if (command.equals(Globals.CMD_GRID_CHR)) {
                    mpCanvas.toggleGrid();
                    updateComponents();
                    return;
                } else if (command.equals(Globals.CMD_TCURSOR)) {
                    if (!mpCanvas.isSpriteMode()) {
                        mpCanvas.toggleTextCursor();
                        updateComponents();
                    }
                    return;
                } else if (command.equals(Globals.CMD_CLONE)) {
                    // if (!mpCanvas.isSpriteMode()) {
                        toggleCloneMode();
                        jbtnClone.setBackground((isCloneModeOn() ? Globals.CLR_BUTTON_ACTIVE : Globals.CLR_BUTTON_NORMAL));
                    // }
                    return;
                } else if (command.equals(Globals.CMD_ADDMAP)) {
                    storeCurrentMap();
                    clearGrid();
                    clearSpriteMap();
                    addMap(mpCanvas.getGridData(), mpCanvas.getColorScreen(), mpCanvas.getSpriteMap());
                    currMap = (arMaps.size() - 1);
                    updateComponents();
                    undoManager.discardAllEdits();
                } else if (command.equals(Globals.CMD_DELMAP)) {
                    if (confirmationAction("Confirm Delete", "Are you sure you want to delete the current map?") == JOptionPane.YES_OPTION) {
                        clearGrid();
                        delMap(currMap);
                        updateComponents();
                        undoManager.discardAllEdits();
                    }
                } else if (command.equals(Globals.CMD_PREVMAP)) {
                    storeCurrentMap();
                    currMap--;
                    if (currMap < 0) {
                        currMap = (arMaps.size() - 1);
                    }
                    updateState(currMap);
                    updateComponents();
                    mpCanvas.redrawCanvas();
                    mpCanvas.revalidate();
                    jsclCanvas.revalidate();
                    undoManager.discardAllEdits();
                    notifyMapSelectListeners();
                    return;
                } else if (command.equals(Globals.CMD_NEXTMAP)) {
                    storeCurrentMap();
                    currMap++;
                    if (currMap >= arMaps.size()) {
                        currMap = 0;
                    }
                    updateState(currMap);
                    updateComponents();
                    mpCanvas.redrawCanvas();
                    mpCanvas.revalidate();
                    jsclCanvas.revalidate();
                    undoManager.discardAllEdits();
                    notifyMapSelectListeners();
                    return;
                } else if (command.equals(Globals.CMD_BACKMAP)) {
                    storeCurrentMap();
                    currMap = moveMapBackward(currMap);
                    goToMap(currMap);
                    updateComponents();
                } else if (command.equals(Globals.CMD_FORWMAP)) {
                    storeCurrentMap();
                    currMap = moveMapForward(currMap);
                    goToMap(currMap);
                    updateComponents();
                } else if (command.equals(Globals.CMD_UNDO_CHR) && undoManager.canUndo()) {
                    undoManager.undo();
                } else if (command.equals(Globals.CMD_REDO_CHR) && undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
            notifyMapChangedListeners();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Program error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.err);
        }
    }

    public void focusLost(FocusEvent e) {
        if (e.getSource() == jtxtWidth) {
            updateWidth();
        } else if (e.getSource() == jtxtHeight) {
            updateHeight();
        }
    }

    public void focusGained(FocusEvent e) {
    }

    private void updateWidth() {
        try {
            int newWidth = Integer.parseInt(jtxtWidth.getText());
            int oldWidth = mpCanvas.getWidth();
            mpCanvas.setGridWidth(newWidth);
            jsclCanvas.revalidate();
            if (newWidth < oldWidth) {
                undoManager.discardAllEdits();
            }
        } catch (NumberFormatException nfe) {
            jtxtWidth.setText("" + mpCanvas.getGridWidth());
        }
    }

    private void updateHeight() {
        try {
            int newHeight = Integer.parseInt(jtxtHeight.getText());
            int oldHeight = mpCanvas.getHeight();
            mpCanvas.setGridHeight(newHeight);
            jsclCanvas.revalidate();
            if (newHeight < oldHeight) {
                undoManager.discardAllEdits();
            }
        } catch (NumberFormatException nfe) {
            jtxtHeight.setText("" + mpCanvas.getGridHeight());
        }
    }

    /* KeyListener methods */
    public void keyTyped(KeyEvent ke) {
        mpCanvas.keyTyped(ke);
    }

    public void keyPressed(KeyEvent ke) {
        mpCanvas.keyPressed(ke);
    }

    public void keyReleased(KeyEvent ke) {
        mpCanvas.keyReleased(ke);
    }

    public void undoRedoStateChanged(boolean canUndo, boolean canRedo, Object source) {
        if (jbtnUndo != null && jbtnRedo != null) {
            jbtnUndo.setEnabled(canUndo);
            jbtnRedo.setEnabled(canRedo);
        }
    }

    protected int confirmationAction(String title, String message) {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
}
