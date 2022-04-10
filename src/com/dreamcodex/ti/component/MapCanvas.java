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

    private static Color[] SPRITE_PER_LINE_COLORS = new Color[] {
        Color.WHITE,
        Color.GREEN,
        Color.YELLOW,
        Color.ORANGE,
        Color.RED,
        Color.BLACK
    };

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
    protected boolean magnifySprites = false;
    protected int spriteMagnification = 1;
    protected BufferedImage overlay;
    protected boolean showSpritesPerLine = false;

// Components ------------------------------------------------------------------------------/

    protected BufferedImage gridImage;
    protected Image gridImageScaled;
    protected Image spritesPerLineImageScaled;

// Listeners -------------------------------------------------------------------------------/

    protected MouseListener parentMouseListener;
    protected MouseMotionListener parentMouseMotionListener;
    protected ArrayList<MapChangeListener> mapChangeListeners = new ArrayList<>();
    protected UndoManager undoManager;
    protected CompoundEdit strokeEdit;

// Convenience Variables -------------------------------------------------------------------/

    private int gridOffsetX = 0;
    private int gridOffsetY = 0;
    private final int cellSize;
    private boolean snapSpritesToGrid = false;
    private final Point hotCell = new Point(PT_OFFGRID);
    private final Point typeCell = new Point(PT_OFFGRID);
    private final Point cloneCell = new Point(PT_OFFGRID);
    private Rectangle rectClone = null;
    private final Point ptLastGrid = new Point(PT_OFFGRID);
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
        spriteMap = new HashMap<>();
        this.cellSize = cellSize;
        clrGrid = new Color(128, 128, 128);
        clrHigh = Globals.CLR_HIGHLIGHT;
        clrType = new Color(164, 164, 255);

        hmCharImages = new HashMap<>();
        hmSpriteImages = new HashMap<>();

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

    public int getLookChar() {
        return lookChar;
    }

    public Point getHotCell() {
        return hotCell;
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

    public void setHotCell(Point pt) {
        hotCell.setLocation(pt);
    }

    public void setCloneCell(Point pt) {
        cloneCell.setLocation(pt);
    }

    public void setTypeCellOn(boolean b) {
        typeCellOn = b;
    }

    public BufferedImage getGridImage() {
        redrawCanvas();
        return gridImage;
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
        highlightLayer = new boolean[i][getGridWidth()];
        redrawCanvas();
    }

    public void rotateLeft(boolean withUndo) {
        int[][] newGrid = new int[this.getGridWidth()][this.getGridHeight()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[x][y] = gridData[y][this.getGridWidth() - 1 - x];
            }
        }
        gridData = newGrid;
        highlightLayer = new boolean[getGridHeight()][getGridWidth()];
        redrawCanvas();
        if (withUndo) {
            undoManager.undoableEditHappened(new UndoableEditEvent(this, new RotationEdit(true)));
        }
    }

    public void rotateRight(boolean withUndo) {
        int[][] newGrid = new int[this.getGridWidth()][this.getGridHeight()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[x][this.getGridHeight() - 1 - y] = gridData[y][x];
            }
        }
        gridData = newGrid;
        highlightLayer = new boolean[getGridHeight()][getGridWidth()];
        redrawCanvas();
        if (withUndo) {
            undoManager.undoableEditHappened(new UndoableEditEvent(this, new RotationEdit(false)));
        }
    }

    public void flipHorizontal() {
        int[][] oldValue = gridData;
        int[][] newGrid = new int[this.getGridHeight()][this.getGridWidth()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[y][x] = gridData[y][this.getGridWidth() - 1 - x];
            }
        }
        gridData = newGrid;
        redrawCanvas();
        undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
    }

    public void flipVertical() {
        int[][] oldValue = gridData;
        int[][] newGrid = new int[this.getGridHeight()][this.getGridWidth()];
        for (int y = 0; y < this.getGridHeight(); y++) {
            for (int x = 0; x < this.getGridWidth(); x++) {
                newGrid[y][x] = gridData[this.getGridHeight() - 1 - y][x];
            }
        }
        gridData = newGrid;
        redrawCanvas();
        undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
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
        ArrayList<Integer> spriteList = spriteMap.computeIfAbsent(hp, k -> new ArrayList<>());
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
        if (enabled) {
            this.floodFillModeOn = false;
        }
    }

    public boolean isSpriteMode() {
        return spriteMode;
    }

    public void clearSpriteMap() {
        spriteMap = new HashMap<>();
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

    public boolean getMagnifySprites() {
        return magnifySprites;
    }

    public void setMagnifySprites(boolean magnifySprites) {
        this.magnifySprites = magnifySprites;
        this.spriteMagnification = magnifySprites ? 2 : 1;
    }

    public boolean getSnapSpritesToGrid() {
        return snapSpritesToGrid;
    }

    public void setSnapSpritesToGrid(boolean snapSpritesToGrid) {
        this.snapSpritesToGrid = snapSpritesToGrid;
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

    public void setOverlay(BufferedImage overlay) {
        this.overlay = overlay;
        redrawCanvas();
    }

    public boolean getShowSpritesPerLine() {
        return showSpritesPerLine;
    }

    public void setShowSpritesPerLine(boolean showSpritesPerLine) {
        this.showSpritesPerLine = showSpritesPerLine;
        redrawCanvas();
    }

    // Rendering Methods -----------------------------------------------------------------------/

    public void redrawCanvas() {
        int gridWidth = gridData[0].length;
        int gridHeight = gridData.length;
        int bufferWidth = gridWidth * cellSize;
        int bufferHeight = gridHeight * cellSize;
        gridImage = getImageBuffer(bufferWidth, bufferHeight);
        Graphics g = gridImage.getGraphics();
        g.setColor(TIGlobals.TI_PALETTE_OPAQUE[colorScreen]);
        g.fillRect(0, 0, gridImage.getWidth(this), gridImage.getHeight(this));
        if (viewCharLayer) {
            boolean painted;
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    painted = false;
                    if (gridData[y][x] != NOCHAR) {
                        Image charImage = hmCharImages.get(gridData[y][x]);
                        if (charImage != null) {
                            g.drawImage(charImage, x * cellSize, y * cellSize, cellSize, cellSize, this);
                            painted = true;
                        }
                    }
                    if (!painted) {
                        g.setColor(TIGlobals.TI_PALETTE_OPAQUE[colorScreen]);
                        g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    }
                    if (highlightLayer[y][x]) {
                        g.setColor(CLR_COPY);
                        g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
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
                            g.drawImage(spriteImage, p.x, p.y, 16 * spriteMagnification, 16 * spriteMagnification, this);
                        }
                    }
                }
            }
        }
        if (cloneModeOn) {
            if (cloneArray != null) {
                if (!hotCell.equals(PT_OFFGRID)) {
                    g.setColor(CLR_PASTE);
                    g.fillRect((int) (hotCell.getX() * cellSize), (int) (hotCell.getY() * cellSize), cloneArray[0].length * cellSize, cloneArray.length * cellSize);
                }
            } else if (rectClone != null) {
                g.setColor(CLR_COPY);
                g.fillRect(rectClone.x * cellSize, rectClone.y * cellSize, (rectClone.width - rectClone.x) * cellSize, (rectClone.height - rectClone.y) * cellSize);
            } else {
                Rectangle rectTmp = getCloneRectangle(hotCell);
                if (rectTmp != null) {
                    g.setColor(CLR_COPY);
                    g.fillRect(rectTmp.x * cellSize, rectTmp.y * cellSize, (rectTmp.width - rectTmp.x) * cellSize, (rectTmp.height - rectTmp.y) * cellSize);
                }
            }
        }
        if (typeCellOn && !typeCell.equals(PT_OFFGRID)) {
            outlineCell(g, typeCell.getX(), typeCell.getY(), clrType);
        }
        if (!hotCell.equals(PT_OFFGRID)) {
            highlightCell(g, hotCell.getX(), hotCell.getY());
        }
        g.dispose();
        int scaledWidth = bufferWidth * viewScale;
        int scaledHeight = bufferHeight * viewScale;
        gridImageScaled = gridImage.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_REPLICATE);
        if (showSpritesPerLine) {
            BufferedImage spritesPerLineImage = getImageBuffer(cellSize, bufferHeight);
            g = spritesPerLineImage.getGraphics();
            for (int y = 0; y < bufferHeight; y++) {
               int spritesOnLine = getSpritesOnLine(y);
               g.setColor(SPRITE_PER_LINE_COLORS[Math.min(spritesOnLine, SPRITE_PER_LINE_COLORS.length - 1)]);
               g.drawRect(0, y, cellSize, 1);
            }
            g.dispose();
            spritesPerLineImageScaled = spritesPerLineImage.getScaledInstance(cellSize * viewScale, scaledHeight, BufferedImage.SCALE_REPLICATE);
        }
        this.setPreferredSize(new Dimension(scaledWidth + (showSpritesPerLine ? cellSize * viewScale : 0) + 2, scaledHeight + 2));
        this.repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            int gridWidth = gridData[0].length;
            int gridHeight = gridData.length;
            int size = cellSize * viewScale;
            int width = size * gridWidth;
            int height = size * gridHeight;
            // this.setPreferredSize(new Dimension((gridWidth + (showSpritesPerLine ? 1 : 0)) * size, gridHeight * size));
            Rectangle currBounds = this.getBounds();
            gridOffsetX = (currBounds.width - width) / 2;
            gridOffsetY = (currBounds.height - height) / 2;
            g.drawRect(gridOffsetX - 1, gridOffsetY - 1, width + 1, height + 1);
            if (gridImageScaled != null) {
                g.drawImage(gridImageScaled, gridOffsetX, gridOffsetY, this);
            }
            if (showGrid) {
                g.setColor(clrGrid);
                int gridSize = size * gridScale;
                for (int y = 0; y < gridHeight / gridScale; y++) {
                    for (int x = 0; x < gridWidth / gridScale; x++) {
                        g.drawRect(x * gridSize + gridOffsetX, y * gridSize + gridOffsetY, gridSize - 1, gridSize - 1);
                    }
                }
            }
            if (showSpritesPerLine && spritesPerLineImageScaled != null) {
                g.drawImage(spritesPerLineImageScaled, gridOffsetX + width + 1, gridOffsetY, this);
            }
            if (overlay != null) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33f));
                g.drawImage(overlay, gridOffsetX, gridOffsetY, width, height, this);
            }
            g.dispose();
        } catch (NullPointerException npe) { /* component not yet initialised */ }
    }

    private int getSpritesOnLine(int y) {
        int spritesOnLine = 0;
        for (Point p : spriteMap.keySet()) {
            if (y >= p.y && y < p.y + 16) {
                spritesOnLine += spriteMap.get(p).size();
            }
        }
        return spritesOnLine;
    }

    private void outlineCell(Graphics g, int x, int y, Color clr) {
        g.setColor(clr);
        int size = (cellSize << (spriteMode ? spriteMagnification : 0)) - 1;
        if (spriteMode) {
            g.drawRect(x, y, size, size);
        } else {
            g.drawRect(x * cellSize, y * cellSize, size, size);
        }
    }

    private void outlineCell(Graphics g, double x, double y, Color clr) {
        outlineCell(g, (int) x, (int) y, clr);
    }

    private void highlightCell(Graphics g, int x, int y) {
        Image image = spriteMode ? hmSpriteImages.get(activeSprite) : hmCharImages.get(activeChar);
        if (lookModeOn || cloneModeOn || !paintOn || image == null) {
            outlineCell(g, x, y, clrHigh);
        } else {
            int size = cellSize << (spriteMode ? spriteMagnification : 0);
            if (spriteMode) {
                g.drawImage(image, x, y, size, size, this);
            } else {
                g.drawImage(image, x * cellSize, y * cellSize, size, size, this);
            }
        }
    }

    private void highlightCell(Graphics g, double x, double y) {
        highlightCell(g, (int) x, (int) y);
    }

    private void setAllGrid(int v) {
        int[][] oldValue = getGridDataCopy();
        for (int[] gridDatum : gridData) {
            Arrays.fill(gridDatum, v);
        }
        redrawCanvas();
        undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
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
            int charsFilled = floodFill(getHotCell(), oldChar, getActiveChar());
            if (charsFilled > 0) {
                redrawCanvas();
                undoManager.undoableEditHappened(new UndoableEditEvent(this, new AllMapEdit(oldValue)));
            }
        }
    }

    int floodFill(Point p, int oldChar, int newChar) {
        int charsFilled = 0;
        if (getGridAt(p) == oldChar) {
            setGridAt(p, newChar);
            charsFilled++;
            charsFilled += floodFill(new Point(p.x + 1, p.y), oldChar, newChar);
            charsFilled += floodFill(new Point(p.x - 1, p.y), oldChar, newChar);
            charsFilled += floodFill(new Point(p.x, p.y + 1), oldChar, newChar);
            charsFilled += floodFill(new Point(p.x, p.y - 1), oldChar, newChar);
        }
        return charsFilled;
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
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

// Class Methods ---------------------------------------------------------------------------/

    public Point getMouseCell(Point pt) {
        if (
            pt.getX() <= gridOffsetX ||
            pt.getY() <= gridOffsetY ||
            pt.getX() >= (gridOffsetX + (cellSize * viewScale * gridData[0].length)) ||
            pt.getY() >= (gridOffsetY + (cellSize * viewScale * gridData.length))
        ) {
            return new Point(PT_OFFGRID);
        }
        int ptx, pty;
        if (spriteMode) {
            ptx = (int) ((pt.getX() - gridOffsetX) / viewScale);
            pty = (int) ((pt.getY() - gridOffsetY) / viewScale);
            if (snapSpritesToGrid) {
                int gridSize = cellSize * gridScale;
                ptx -= ptx % gridSize;
                pty -= pty % gridSize;
            }
        } else {
            ptx = (int) ((pt.getX() - gridOffsetX) / (cellSize * viewScale));
            pty = (int) ((pt.getY() - gridOffsetY) / (cellSize * viewScale));
        }
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
                int oldValue = -1;
                int addedSpriteNum = -1;
                int removedSpriteNum = -1;
                boolean change = false;
                if (!spriteMode) {
                    oldValue = gridData[y][x];
                    int newChar = paintOn ? activeChar : TIGlobals.SPACECHAR;
                    if (gridData[y][x] != newChar) {
                        gridData[y][x] = newChar;
                        change = true;
                    }
                }
                else if (paintOn) {
                    addedSpriteNum = setSprite(hotCell, activeSprite);
                    change = addedSpriteNum != -1;
                }
                else {
                    removedSpriteNum = removeSprite(hotCell);
                    change = removedSpriteNum != -1;
                }
                redrawCanvas();
                if (change) {
                    UndoableEdit undoableEdit = spriteMode ? new SpriteEdit(x, y, addedSpriteNum, removedSpriteNum) : new CellEdit(x, y, oldValue);
                    if (strokeEdit == null) {
                        undoManager.undoableEditHappened(new UndoableEditEvent(this, undoableEdit));
                    } else {
                        strokeEdit.addEdit(undoableEdit);
                    }
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
        if (isFloodFillModeOn() && !spriteMode) {
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
                        System.arraycopy(gridData[rectClone.y + y], rectClone.x, cloneArray[y], 0, cloneArray[y].length);
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
        setHotCell(PT_OFFGRID);
        redrawCanvas();
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
        int size = cellSize * viewScale;
        Rectangle r = new Rectangle(me.getX() - size, me.getY() - size, 2 * size, 2 * size);
        this.scrollRectToVisible(r);
    }

    public void mouseMoved(MouseEvent me) {
        if (!me.getPoint().equals(ptLastGrid)) {
            if (!cloneModeOn || cloneCell.equals(PT_OFFGRID)) {
                setHotCell(getMouseCell(me.getPoint()));
            }
            ptLastGrid.setLocation(me.getPoint());
            if (lookModeOn) {
                if (!hotCell.equals(PT_OFFGRID)) {
                    lookChar = gridData[(int) (hotCell.getY())][(int) (hotCell.getX())];
                } else {
                    lookChar = NOCHAR;
                }
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

    private class RotationEdit extends AbstractUndoableEdit {

        private final boolean left;

        public RotationEdit(boolean left) {
            this.left = left;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            if (left) {
                rotateRight(false);
            } else {
                rotateLeft(false);
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            if (left) {
                rotateLeft(false);
            } else {
                rotateRight(false);
            }
        }
    }
}
