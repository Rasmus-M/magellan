package com.dreamcodex.ti.component;

import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.HashPoint;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MapCanvas extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
// Constants -------------------------------------------------------------------------------/

    public static final Point PT_OFFGRID = new Point(-1, -1);
    public static final int NOCHAR = -1;

    public static Color CLR_COPY = new Color(255, 128, 128, 128);
    public static Color CLR_PASTE = new Color(128, 128, 255, 128);

// Variables -------------------------------------------------------------------------------/

    protected int[][] gridData;
    protected HashMap<Point, ArrayList<Integer>> spriteMap;
    protected Color clrGrid;
    protected Color clrHigh;
    protected Color clrType;
    protected boolean paintOn = true;
    protected HashMap<Integer, Image> hmCharImages;
    protected HashMap<Integer, Image> hmSpriteImages;
    protected int activeChar = TIGlobals.SPACECHAR;
    protected int activeSprite = 0;
    protected int viewScale = 1;
    protected int gridScale = 1;
    protected int colorScreen = 0;
    protected boolean showGrid = true;
    protected boolean lookModeOn = false;
    protected boolean cloneModeOn = false;
    protected boolean stickyCloneModeOn = false;
    protected boolean floodFillModeOn = false;
    protected int[][] cloneArray = null;
    protected int lookChar = NOCHAR;
    protected boolean typeCellOn = true;
    protected boolean[][] highlightLayer;
    protected boolean spriteMode = false;
    protected boolean viewCharLayer = true;
    protected boolean viewSpriteLayer = true;

// Components ------------------------------------------------------------------------------/

    protected BufferedImage bufferDraw;
    protected Image currBufferScaled;

// Listeners -------------------------------------------------------------------------------/

    protected MouseListener parentMouseListener;
    protected MouseMotionListener parentMouseMotionListener;
    protected ArrayList<MapChangeListener> mapChangeListeners = new ArrayList<MapChangeListener>();
    protected UndoManager undoManager;
    protected CompoundEdit strokeEdit;

// Convenience Variables -------------------------------------------------------------------/

    private Rectangle currBounds = (Rectangle) null;
    private int gridOffsetX = 0;
    private int gridOffsetY = 0;
    private int optScale = 8;
    private Point hotCell = new Point(PT_OFFGRID);
    private Point typeCell = new Point(PT_OFFGRID);
    private Point cloneCell = new Point(PT_OFFGRID);
    private Rectangle rectClone = (Rectangle) null;
    private Point ptLastGrid = new Point(PT_OFFGRID);
    private boolean isTyping = false;

// Constructors ----------------------------------------------------------------------------/

    public MapCanvas(int gridWidth, int gridHeight, int cellSize, MouseListener mlParent, MouseMotionListener mmlParent, UndoManager undoManager) {
        super(true);
        this.undoManager = undoManager;

        this.setOpaque(true);
        this.setAutoscrolls(true);

        gridData = new int[gridHeight][gridWidth];
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                gridData[y][x] = TIGlobals.SPACECHAR;
            }
        }
        spriteMap = new HashMap<Point, ArrayList<Integer>>();
        optScale = cellSize;
        clrGrid = new Color(128, 128, 128);
        clrHigh = Globals.CLR_HIGHLIGHT;
        clrType = new Color(164, 164, 255);

        bufferDraw = getImageBuffer(gridWidth * optScale, gridHeight * optScale);

        hmCharImages = new HashMap<Integer, Image>();
        hmSpriteImages = new HashMap<Integer, Image>();

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);

        parentMouseListener = mlParent;
        parentMouseMotionListener = mmlParent;

        highlightLayer = new boolean[gridHeight][gridWidth];
    }

// Accessors -------------------------------------------------------------------------------/

    public int[][] getGridData() {
        return gridData;
    }

    public Color getColorGrid() {
        return clrGrid;
    }

    public HashMap<Point, ArrayList<Integer>> getSpriteMap() {
        return spriteMap;
    }

    public Color getColorHigh() {
        return clrHigh;
    }

    public Color getColorType() {
        return clrType;
    }

    public boolean isPaintOn() {
        return paintOn;
    }

    public int getActiveChar() {
        return activeChar;
    }

    public int getViewScale() {
        return viewScale;
    }

    public int getGridScale() {
        return gridScale;
    }

    public int getColorScreen() {
        return colorScreen;
    }

    public int getColorScreenTI() {
        return colorScreen + 1;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public boolean isLookModeOn() {
        return lookModeOn;
    }

    public boolean isCloneModeOn() {
        return cloneModeOn;
    }

    public boolean isFloodFillModeOn() {
        return floodFillModeOn;
    }

    public int[][] getCloneArray() {
        return cloneArray;
    }

    public int getLookChar() {
        return lookChar;
    }

    public Point getHotCell() {
        return hotCell;
    }

    public Point getCloneCell() {
        return cloneCell;
    }

    public Point getTypeCell() {
        return typeCell;
    }

    public boolean showTypeCell() {
        return typeCellOn;
    }

    public void setGridData(int[][] gd) {
        gridData = gd;
        highlightLayer = new boolean[gd.length][gd[0].length];
    }

    public void setSpriteMap(HashMap<Point, ArrayList<Integer>> spriteMap) {
        this.spriteMap = spriteMap;
    }

    public void setColorGrid(Color clr) {
        clrGrid = clr;
    }

    public void setColorHigh(Color clr) {
        clrHigh = clr;
    }

    public void setColorType(Color clr) {
        clrType = clr;
    }

    public void setPaintOn(boolean b) {
        paintOn = b;
    }

    public void setActiveChar(int i) {
        activeChar = i;
    }

    public void setActiveSprite(int i) { activeSprite = i; }

    public void setViewScale(int i) {
        viewScale = i;
    }

    public void setGridScale(int gridScale) {
        this.gridScale = gridScale;
    }

    public void setColorScreen(int i) {
        colorScreen = i;
    }

    public void setColorScreenTI(int i) {
        colorScreen = i - 1;
    }

    public void setShowGrid(boolean b) {
        showGrid = b;
    }

    public void setLookModeOn(boolean b) {
        lookModeOn = b;
        lookChar = NOCHAR;
    }

    public void setCloneModeOn(boolean b) {
        cloneModeOn = b;
        setCloneCell(PT_OFFGRID);
        rectClone = null;
        cloneArray = null;
    }

    public void setFloodFillMode(boolean floodFillMode) {
        this.floodFillModeOn = floodFillMode;
    }

    public void setCloneArray(int[][] ar) {
        cloneArray = ar;
    }

    public void setLookChar(int i) {
        lookChar = i;
    }

    public void setHotCell(Point pt) {
        hotCell.setLocation(pt);
    }

    public void setCloneCell(Point pt) {
        cloneCell.setLocation(pt);
    }

    public void setTypeCell(Point pt) {
        typeCell.setLocation(pt);
    }

    public void setTypeCellOn(boolean b) {
        typeCellOn = b;
    }

    public BufferedImage getBuffer() {
        redrawCanvas();
        return bufferDraw;
    }

    public int getGridWidth() {
        return gridData[0].length;
    }

    public int getGridHeight() {
        return gridData.length;
    }

    public void setGridWidth(int i) {
        int[][] newGrid = new int[this.getGridHeight()][i];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < i; x++) {
                if (x < this.getGridWidth()) {
                    newGrid[y][x] = gridData[y][x];
                } else {
                    newGrid[y][x] = TIGlobals.SPACECHAR;
                }
            }
        }
        gridData = newGrid;
        bufferDraw = getImageBuffer(gridData[0].length * optScale, gridData.length * optScale);
        highlightLayer = new boolean[getGridHeight()][i];
        redrawCanvas();
    }

    public void setGridHeight(int i) {
        int[][] newGrid = new int[i][this.getGridWidth()];
        for (int y = 0; y < i; y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                if (y < this.getGridHeight()) {
                    newGrid[y][x] = gridData[y][x];
                } else {
                    newGrid[y][x] = TIGlobals.SPACECHAR;
                }
            }
        }
        gridData = newGrid;
        bufferDraw = getImageBuffer(gridData[0].length * optScale, gridData.length * optScale);
        highlightLayer = new boolean[i][getGridWidth()];
        redrawCanvas();
    }

    public void rotateLeft() {
        int[][] newGrid = new int[this.getGridWidth()][this.getGridHeight()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[x][y] = gridData[y][this.getGridWidth() - 1 - x];
            }
        }
        gridData = newGrid;
        bufferDraw = getImageBuffer(gridData[0].length * optScale, gridData.length * optScale);
        highlightLayer = new boolean[getGridHeight()][getGridWidth()];
        redrawCanvas();
    }

    public void rotateRight() {
        int[][] newGrid = new int[this.getGridWidth()][this.getGridHeight()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[x][this.getGridHeight() - 1 - y] = gridData[y][x];
            }
        }
        gridData = newGrid;
        bufferDraw = getImageBuffer(gridData[0].length * optScale, gridData.length * optScale);
        highlightLayer = new boolean[getGridHeight()][getGridWidth()];
        redrawCanvas();
    }

    public int getGridAtHotCell() {
        if (!hotCell.equals(PT_OFFGRID)) {
            return getGridAt(hotCell);
        }
        return NOCHAR;
    }

    public int getGridAt(Point p) {
        return getGridAt(p.x, p.y);
    }

    public int getGridAt(int x, int y) {
        if (x >= 0 && y >= 0 && y < gridData.length && x < gridData[y].length) {
            return gridData[y][x];
        }
        return NOCHAR;
    }

    public void setGridAt(int x, int y, int v) {
        if (x >= 0 && y >= 0 && y < gridData.length && x < gridData[y].length) {
            gridData[y][x] = v;
        }
    }

    public void setGridAt(Point pt, int v) {
        setGridAt((int) (pt.getX()), (int) (pt.getY()), v);
    }

    public void setCharImage(int charnum, Image img) {
        hmCharImages.put(charnum, img);
    }

    public int setSprite(Point p, int spriteNum) {
        HashPoint hp = new HashPoint(p);
        ArrayList<Integer> spriteList = spriteMap.get(hp);
        if (spriteList == null) {
            spriteList = new ArrayList<Integer>();
            spriteMap.put(hp, spriteList);
        }
        if (!spriteList.contains(spriteNum) && hmSpriteImages.containsKey(spriteNum)) {
            if (spriteList.add(spriteNum)) {
                return spriteNum;
            }
        }
        return -1;
    }

    public int removeSprite(Point p) {
        ArrayList<Integer> spriteList = spriteMap.get(new HashPoint(p));
        if (spriteList != null && !spriteList.isEmpty()) {
            return spriteList.remove(spriteList.size() - 1);
        }
        else {
            return -1;
        }
    }

    public void setSpriteImage(int spritenum, Image img) {
        hmSpriteImages.put(spritenum, img);
    }

    public void setSpriteMode(boolean enabled) {
        this.spriteMode = enabled;
    }

    public boolean isSpriteMode() {
        return spriteMode;
    }

    public void clearSpriteMap() {
        spriteMap = new HashMap<Point, ArrayList<Integer>>();
    }

    public boolean getViewCharLayer() {
        return viewCharLayer;
    }

    public void setViewCharLayer(boolean viewCharLayer) {
        this.viewCharLayer = viewCharLayer;
        redrawCanvas();
    }

    public boolean getViewSpriteLayer() {
        return viewSpriteLayer;
    }

    public void setViewSpriteLayer(boolean viewSpriteLayer) {
        this.viewSpriteLayer = viewSpriteLayer;
        redrawCanvas();
    }

    public void toggleGrid() {
        this.setShowGrid(!this.isShowGrid());
        this.redrawCanvas();
    }

    public void toggleTextCursor() {
        this.setTypeCellOn(!this.showTypeCell());
        this.redrawCanvas();
    }

    public void toggleCloneMode() {
        this.setCloneModeOn(!isCloneModeOn());
    }

    public void toggleFloodFillMode() {
        this.setFloodFillMode(!isFloodFillModeOn());
    }

    public void advanceTypeCell() {
        if (!typeCell.equals(PT_OFFGRID)) {
            int typeX = (int) (typeCell.getX());
            int typeY = (int) (typeCell.getY());
            typeX++;
            if (typeX >= gridData[typeY].length) {
                typeX = 0;
                typeY++;
                if (typeY >= gridData.length) {
                    typeY = 0;
                }
            }
            typeCell.setLocation(typeX, typeY);
        }
    }

    public void highlightCell(int x, int y) {
        highlightLayer[y][x] = true;
    }

    public void removeAllHighlights() {
        highlightLayer = new boolean[getGridHeight()][getGridWidth()];
    }

    private int[][] getGridDataCopy() {
        int[][] copy = new int[gridData.length][gridData[0].length];
        for (int y = 0; y < gridData.length; y++) {
            copy[y] = Arrays.copyOf(gridData[y], gridData[y].length);
        }
        return copy;
    }

// Rendering Methods -----------------------------------------------------------------------/

    public void redrawCanvas() {
        if (this.getGraphics() != null && bufferDraw != null) {
            if (bufferDraw.getWidth() != (gridData[0].length * optScale) || bufferDraw.getHeight() != (gridData.length * optScale)) {
                bufferDraw = getImageBuffer(gridData[0].length * optScale, gridData.length * optScale);
            }
            Graphics g = bufferDraw.getGraphics();
            g.setColor(TIGlobals.TI_PALETTE_OPAQUE[colorScreen]);
            g.fillRect(0, 0, bufferDraw.getWidth(this), bufferDraw.getHeight(this));
            if (viewCharLayer) {
                boolean painted;
                for (int y = 0; y < gridData.length; y++) {
                    for (int x = 0; x < gridData[0].length; x++) {
                        painted = false;
                        if (gridData[y][x] != NOCHAR) {
                            Image charImage = hmCharImages.get(gridData[y][x]);
                            if (charImage != null) {
                                g.drawImage(charImage, x * optScale, y * optScale, optScale, optScale, this);
                                painted = true;
                            }
                        }
                        if (!painted) {
                            g.setColor(TIGlobals.TI_PALETTE_OPAQUE[colorScreen]);
                            g.fillRect(x * optScale, y * optScale, optScale, optScale);
                        }
                        if (highlightLayer[y][x]) {
                            g.setColor(CLR_COPY);
                            g.fillRect(x * optScale, y * optScale, optScale, optScale);
                        }
                    }
                }
            }
            if (viewSpriteLayer) {
                for (Point p : spriteMap.keySet()) {
                    ArrayList<Integer> spriteList = spriteMap.get(p);
                    if (spriteList != null) {
                        for (int spriteNum : spriteList) {
                            Image spriteImage = hmSpriteImages.get(spriteNum);
                            if (spriteImage != null) {
                                g.drawImage(spriteImage, p.x * optScale, p.y * optScale, 16, 16, this);
                            }
                        }
                    }
                }
            }
            if (cloneModeOn) {
                if (cloneArray != null) {
                    if (!hotCell.equals(PT_OFFGRID)) {
                        g.setColor(CLR_PASTE);
                        g.fillRect((int) (hotCell.getX() * optScale), (int) (hotCell.getY() * optScale), cloneArray[0].length * optScale, cloneArray.length * optScale);
                    }
                } else if (rectClone != null) {
                    g.setColor(CLR_COPY);
                    g.fillRect(rectClone.x * optScale, rectClone.y * optScale, (rectClone.width - rectClone.x) * optScale, (rectClone.height - rectClone.y) * optScale);
                } else {
                    Rectangle rectTmp = getCloneRectangle(hotCell);
                    if (rectTmp != null) {
                        g.setColor(CLR_COPY);
                        g.fillRect(rectTmp.x * optScale, rectTmp.y * optScale, (rectTmp.width - rectTmp.x) * optScale, (rectTmp.height - rectTmp.y) * optScale);
                    }
                }
            }
            if (typeCellOn && !typeCell.equals(PT_OFFGRID)) {
                outlineCell(g, typeCell.getX(), typeCell.getY(), clrType);
            }
            if (!hotCell.equals(PT_OFFGRID)) {
                highlightCell(g, hotCell.getX(), hotCell.getY());
            }
            currBufferScaled = bufferDraw.getScaledInstance(gridData[0].length * optScale * viewScale, gridData.length * optScale * viewScale, BufferedImage.SCALE_REPLICATE);
            this.setPreferredSize(new Dimension(gridData[0].length * optScale * viewScale, gridData.length * optScale * viewScale));
            g.dispose();
            this.repaint();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            int size = optScale * viewScale;
            this.setPreferredSize(new Dimension(gridData[0].length * size, gridData.length * size));
            currBounds = this.getBounds();
            gridOffsetX = (currBounds.width - (size * gridData[0].length)) / 2;
            gridOffsetY = (currBounds.height - (size * gridData.length)) / 2;
            g.drawRect(gridOffsetX - 1, gridOffsetY - 1, (size * gridData[0].length) + 1, (size * gridData.length) + 1);
            if (currBufferScaled != null) {
                g.drawImage(currBufferScaled, gridOffsetX, gridOffsetY, this);
                if (showGrid) {
                    g.setColor(clrGrid);
                    int gridSize = size * gridScale;
                    for (int y = 0; y < gridData.length / gridScale; y++) {
                        for (int x = 0; x < gridData[0].length / gridScale; x++) {
                            g.drawRect(x * gridSize + gridOffsetX, y * gridSize + gridOffsetY, gridSize - 1, gridSize - 1);
                        }
                    }
                }
            }
            g.dispose();
        } catch (NullPointerException npe) { /* component not yet initialised */ }
    }

    private void outlineCell(Graphics g, int x, int y, Color clr) {
        g.setColor(clr);
        int size = (optScale << (spriteMode ? 1 : 0)) - 1;
        g.drawRect(x * optScale, y * optScale, size, size);
    }

    private void outlineCell(Graphics g, double x, double y, Color clr) {
        outlineCell(g, (int) x, (int) y, clr);
    }

    private void highlightCell(Graphics g, int x, int y) {
        Image image = spriteMode ? hmSpriteImages.get(activeSprite) : hmCharImages.get(activeChar);
        if (lookModeOn || cloneModeOn || !paintOn || image == null) {
            outlineCell(g, x, y, clrHigh);
        } else {
            int size = optScale << (spriteMode ? 1 : 0);
            g.drawImage(image, x * optScale, y * optScale, size, size, this);
        }
    }

    private void highlightCell(Graphics g, double x, double y) {
        highlightCell(g, (int) x, (int) y);
    }

    private void setAllGrid(int v) {
        int[][] oldValue = getGridDataCopy();
        for (int y = 0; y < gridData.length; y++) {
            for (int x = 0; x < gridData[y].length; x++) {
                gridData[y][x] = v;
            }
        }
        redrawCanvas();
        undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
    }

    public void wipeGrid() {
        setAllGrid(NOCHAR);
    }

    public void clearGrid() {
        setAllGrid(TIGlobals.SPACECHAR);
    }

    public void fillGrid(int v) {
        setAllGrid(v);
    }

    public void floodFillGrid() {
        int oldChar = getGridAtHotCell();
        if (oldChar != NOCHAR) {
            int[][] oldValue = getGridDataCopy();
            floodFill(getHotCell(), oldChar, getActiveChar());
            redrawCanvas();
            undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
        }
    }

    void floodFill(Point p, int oldChar, int newChar) {
        if (getGridAt(p) == oldChar) {
            setGridAt(p, newChar);
            floodFill(new Point(p.x + 1, p.y), oldChar, newChar);
            floodFill(new Point(p.x - 1, p.y), oldChar, newChar);
            floodFill(new Point(p.x, p.y + 1), oldChar, newChar);
            floodFill(new Point(p.x, p.y - 1), oldChar, newChar);
        }
    }

    protected void typeCharToGrid(int x, int y, char ch) {
        int charNum = -1;
        if (ch == ' ') {
            charNum = TIGlobals.SPACECHAR;
        } else {
            for (int i = 0; i < TIGlobals.CHARMAP.length; i++) {
                if (TIGlobals.CHARMAP[i] == ch) {
                    charNum = i + TIGlobals.CHARMAPSTART;
                }
            }
        }
        if (charNum > -1) {
            int oldValue = getGridAt(x, y);
            setGridAt(x, y, charNum);
            advanceTypeCell();
            redrawCanvas();
            undoManager.undoableEditHappened(new UndoableEditEvent(this, new CellEdit(x, y, oldValue)));
        }
        notifyMapChangedListeners();
    }

    protected void typeCharToGrid(Point pt, char ch) {
        typeCharToGrid((int) (pt.getX()), (int) (pt.getY()), ch);
    }

    protected Rectangle getCloneRectangle(Point endpoint) {
        if (cloneCell.equals(PT_OFFGRID) || endpoint.equals(PT_OFFGRID)) {
            return null;
        } else {
            return new Rectangle((int) (Math.min(cloneCell.getX(), endpoint.getX())), (int) (Math.min(cloneCell.getY(), endpoint.getY())), (int) (Math.max(cloneCell.getX(), endpoint.getX())) + 1, (int) (Math.max(cloneCell.getY(), endpoint.getY())) + 1);
        }
    }

    protected BufferedImage getImageBuffer(int width, int height) {
        BufferedImage bimgNew = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        while (bimgNew == null) {
            bimgNew = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }
        return bimgNew;
    }

// Class Methods ---------------------------------------------------------------------------/

    public Point getMouseCell(Point pt) {
        if (
            pt.getX() <= gridOffsetX ||
            pt.getY() <= gridOffsetY ||
            pt.getX() >= (gridOffsetX + (optScale * viewScale * gridData[0].length)) ||
            pt.getY() >= (gridOffsetY + (optScale * viewScale * gridData.length))
        ) {
            return new Point(PT_OFFGRID);
        }
        int ptx = (int) ((pt.getX() - gridOffsetX) / (optScale * viewScale));
        int pty = (int) ((pt.getY() - gridOffsetY) / (optScale * viewScale));
        return new Point(ptx, pty);
    }

    public void processCellAtPoint(Point pt, int btn) {
        if (btn > 0) {
            paintOn = (btn != MouseEvent.BUTTON3);
        }
        setHotCell(getMouseCell(pt));
        typeCell.setLocation(hotCell);
        if (!hotCell.equals(PT_OFFGRID)) {
            int x = (int) (hotCell.getX());
            int y = (int) (hotCell.getY());
            if (lookModeOn) {
                lookChar = gridData[y][x];
            } else {
                int oldValue = gridData[y][x];
                int addedSpriteNum = -1;
                int removedSpriteNum = -1;
                if (!spriteMode) {
                    gridData[y][x] = paintOn ? activeChar : TIGlobals.SPACECHAR;
                }
                else if (paintOn) {
                    addedSpriteNum = setSprite(hotCell, activeSprite);
                }
                else {
                    removedSpriteNum = removeSprite(hotCell);
                }
                redrawCanvas();
                UndoableEdit undoableEdit = spriteMode ? new SpriteEdit(x, y, addedSpriteNum, removedSpriteNum) : new CellEdit(x, y, oldValue);
                if (strokeEdit == null) {
                    undoManager.undoableEditHappened(new UndoableEditEvent(this, undoableEdit));
                }
                else {
                    strokeEdit.addEdit(undoableEdit);
                }
            }
            notifyMapChangedListeners();
        }
    }

// Listener Methods ------------------------------------------------------------------------/

    public void addMapChangeListener(MapChangeListener mapChangeListener) {
        mapChangeListeners.add(mapChangeListener);
    }

    public void removeMapChangeListener(MapChangeListener mapChangeListener) {
        mapChangeListeners.remove(mapChangeListener);
    }

    private void notifyMapChangedListeners() {
        for (MapChangeListener mapChangeListener : mapChangeListeners) {
            mapChangeListener.mapChanged();
        }
    }

    /* MouseListener methods */

    public void mousePressed(MouseEvent me) {
        if (isFloodFillModeOn()) {
            floodFillGrid();
        }
        if (isCloneModeOn()) {
            Point clickCell = getMouseCell(me.getPoint());
            if (cloneArray == null && !clickCell.equals(PT_OFFGRID)) {
                // Set start of clone region
                setCloneCell(clickCell);
            }
        }
        if (!isFloodFillModeOn() && !isCloneModeOn()) {
            strokeEdit = new CompoundEdit();
            processCellAtPoint(me.getPoint(), me.getButton());
            parentMouseListener.mousePressed(me);
        }
    }

    public void mouseClicked(MouseEvent me) {
        if (cloneModeOn) {
            if (me.getButton() == MouseEvent.BUTTON1) {
                Point clickCell = getMouseCell(me.getPoint());
                if (cloneArray != null && !clickCell.equals(PT_OFFGRID)) {
                    // Make a clone
                    int[][] oldValue = new int[cloneArray.length][cloneArray[0].length];
                    for (int y = 0; y < cloneArray.length; y++) {
                        int plotY = (int) (clickCell.getY() + y);
                        if (plotY >= 0 && plotY < gridData.length) {
                            for (int x = 0; x < cloneArray[y].length; x++) {
                                int plotX = (int) (clickCell.getX() + x);
                                if (plotX >= 0 && plotX < gridData[y].length) {
                                    oldValue[y][x] = gridData[plotY][plotX];
                                    gridData[plotY][plotX] = cloneArray[y][x];
                                }
                            }
                        }
                    }
                    undoManager.undoableEditHappened(new UndoableEditEvent(this, new AreaEdit(clickCell.x, clickCell.y, oldValue)));
                    // Hold down shift or ctrl to continue cloning
                    if (!me.isShiftDown() && !me.isControlDown()) {
                        stickyCloneModeOn = false;
                        rectClone = null;
                        cloneArray = null;
                    } else {
                        stickyCloneModeOn = true;
                    }
                    redrawCanvas();
                    parentMouseListener.mouseClicked(me);
                    notifyMapChangedListeners();
                }
            } else {
                setCloneModeOn(false);
                parentMouseListener.mouseClicked(null);
            }
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (cloneModeOn) {
            if (cloneArray == null && rectClone == null && !cloneCell.equals(PT_OFFGRID)) {
                // Set end of clone region
                Point clickCell = getMouseCell(me.getPoint());
                if (!clickCell.equals(PT_OFFGRID)) {
                    rectClone = getCloneRectangle(clickCell);
                    cloneArray = new int[rectClone.height - rectClone.y][rectClone.width - rectClone.x];
                    for (int y = 0; y < cloneArray.length; y++) {
                        for (int x = 0; x < cloneArray[y].length; x++) {
                            cloneArray[y][x] = gridData[rectClone.y + y][rectClone.x + x];
                        }
                    }
                    setCloneCell(PT_OFFGRID);
                } else {
                    rectClone = null;
                    cloneArray = null;
                }
                redrawCanvas();
            }
        } else {
            paintOn = true;
            if (strokeEdit != null && strokeEdit.isSignificant()) {
                strokeEdit.end();
                undoManager.undoableEditHappened(new UndoableEditEvent(this, strokeEdit));
            }
            strokeEdit = null;
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    /* MouseMotionListener methods */

    public void mouseDragged(MouseEvent me) {
        if (cloneModeOn) {
            setHotCell(getMouseCell(me.getPoint()));
            redrawCanvas();
        } else {
            processCellAtPoint(me.getPoint(), me.getButton());
            parentMouseMotionListener.mouseDragged(me);
        }
        // Scroll when edge is reached
        int size = optScale * viewScale;
        Rectangle r = new Rectangle(me.getX() - size, me.getY() - size, 2 * size, 2 * size);
        this.scrollRectToVisible(r);
    }

    public void mouseMoved(MouseEvent me) {
        if (!me.getPoint().equals(ptLastGrid)) {
            if (!cloneModeOn || cloneCell.equals(PT_OFFGRID)) {
                setHotCell(getMouseCell(me.getPoint()));
            }
            ptLastGrid.setLocation(me.getPoint());
            if (!hotCell.equals(PT_OFFGRID)) {
                lookChar = gridData[(int) (hotCell.getY())][(int) (hotCell.getX())];
            } else {
                lookChar = NOCHAR;
            }
            redrawCanvas();
            parentMouseMotionListener.mouseMoved(me);
        }
        // Release sticky clone mode
        if (stickyCloneModeOn && !me.isShiftDown() && !me.isControlDown()) {
            stickyCloneModeOn = false;
            rectClone = null;
            cloneArray = null;
            parentMouseListener.mouseClicked(null); // Update Clone button
        }
    }

    /* KeyListener methods */
    public void keyTyped(KeyEvent ke) {
        if (!typeCell.equals(PT_OFFGRID) && !isTyping) {
            isTyping = true;
            typeCharToGrid(typeCell, ke.getKeyChar());
            isTyping = false;
        }
    }

    public void keyPressed(KeyEvent ke) {
        if (cloneModeOn && ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
            stickyCloneModeOn = false;
            setCloneCell(PT_OFFGRID);
            rectClone = null;
            cloneArray = null;
            redrawCanvas();
        }
    }

    public void keyReleased(KeyEvent ke) {
    }

    // Undoable edits ------------------------------------------------------------------------/

    private class SpriteEdit extends AbstractUndoableEdit {

        private final HashPoint p;
        private int addedSpriteNum;
        private int removedSpriteNum;

        public SpriteEdit(int x, int y, int addedSpriteNum, int removedSpriteNum) {
            this.p = new HashPoint(x, y);
            this.addedSpriteNum = addedSpriteNum;
            this.removedSpriteNum = removedSpriteNum;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swap();
            redrawCanvas();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            swap();
            redrawCanvas();
        }

        private void swap() {
            if (addedSpriteNum != -1) {
                removedSpriteNum = removeSprite(p);
                addedSpriteNum = -1;
            }
            else if (removedSpriteNum != -1) {
                addedSpriteNum = setSprite(p, removedSpriteNum);
                removedSpriteNum = -1;
            }
        }
    }

    private class CellEdit extends AbstractUndoableEdit {

        private final int x;
        private final int y;
        private int oldValue;

        public CellEdit(int x, int y, int oldValue) {
            this.x = x;
            this.y = y;
            this.oldValue = oldValue;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swap();
            redrawCanvas();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            swap();
            redrawCanvas();
        }

        private void swap() {
            int temp = gridData[y][x];
            gridData[y][x] = oldValue;
            oldValue = temp;
        }
    }

    private class AreaEdit extends AbstractUndoableEdit {

        private final int x;
        private final int y;
        private final int[][] oldValue;

        public AreaEdit(int x, int y, int[][] oldValue) {
            this.x = x;
            this.y = y;
            this.oldValue = oldValue;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swap();
            redrawCanvas();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            swap();
            redrawCanvas();
        }

        private void swap() {
            for (int plotY = y; plotY < y + oldValue.length; plotY++) {
                for (int plotX = x; plotX < x + oldValue[0].length; plotX++) {
                    if (plotY < gridData.length && plotX < gridData[plotY].length) {
                        int temp = gridData[plotY][plotX];
                        gridData[plotY][plotX] = oldValue[plotY - y][plotX - x];
                        oldValue[plotY - y][plotX - x] = temp;
                    }
                }
            }
        }
    }

    private class AllMapEdit extends AbstractUndoableEdit {

        private final int[][] oldValue;

        public AllMapEdit(int[][] oldValue) {
            this.oldValue = oldValue;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swap();
            redrawCanvas();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            swap();
            redrawCanvas();
        }

        private void swap() {
            for (int plotY = 0; plotY < oldValue.length; plotY++) {
                for (int plotX = 0; plotX < oldValue[0].length; plotX++) {
                    int temp = gridData[plotY][plotX];
                    gridData[plotY][plotX] = oldValue[plotY][plotX];
                    oldValue[plotY][plotX] = temp;
                }
            }
        }
    }
}