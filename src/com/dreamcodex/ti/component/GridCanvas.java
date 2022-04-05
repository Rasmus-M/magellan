package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.iface.ScreenColorListener;
import com.dreamcodex.ti.iface.UndoRedoListener;
import com.dreamcodex.ti.util.ColorMode;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;

import static com.dreamcodex.ti.util.ColorMode.*;

public class GridCanvas extends JPanel implements MouseListener, MouseMotionListener {

// Constants -------------------------------------------------------------------------------/

    public static final Point PT_OFFGRID = new Point(-1, -1);
    public static final int NODATA = -1;

// Variables -------------------------------------------------------------------------------/

    protected Color[] palette;
    protected int[][] gridData;
    protected int clrDraw;
    protected int clrBack;
    protected Color clrScreen;
    protected Color clrGrid;
    protected Color clrGridDraw;
    protected Color clrGrup;
    protected Color clrHigh;
    protected boolean paintOn = true;
    protected int viewScale = 1;
    protected int gridGroup = 0;
    protected boolean showGrid = true;
    // Bitmap mode colors
    // First index is the row
    // Second index is 0 = background color, 1 = foreground color
    // E.g. gridColors[3][1] is row 3 foreground
    protected int[][] gridColors;
    protected ColorMode colorMode;
    protected boolean ecmTransparency;
    protected int mouseButton;
    protected ArrayList<int[][][]> undoList;
    int undoIndex;
    protected ArrayList<UndoRedoListener> undoRedoListeners = new ArrayList<UndoRedoListener>();

// Components ------------------------------------------------------------------------------/

    protected BufferedImage bufferDraw;
    protected Image currBufferScaled;

// Listeners -------------------------------------------------------------------------------/

    protected MouseListener parentMouseListener;
    protected MouseMotionListener parentMouseMotionListener;

// Convenience Variables -------------------------------------------------------------------/

    private Rectangle currBounds;
    private int gridOffsetX = 0;
    private int gridOffsetY = 0;
    private int optScale = 8;
    private Point hotCell = new Point(PT_OFFGRID);
    private Point lastCell = new Point(PT_OFFGRID);
    private Point ptLastGrid = new Point(PT_OFFGRID);

// Constructors ----------------------------------------------------------------------------/

    public GridCanvas(Color[] palette, int gridWidth, int gridHeight, int cellSize, MouseListener mlParent, MouseMotionListener mmlParent, ColorMode colorMode) {
        super(true);
        this.palette = palette;
        this.colorMode = colorMode;
        this.setOpaque(true);
        this.setBackground(Color.white);

        gridData = new int[gridHeight][gridWidth];
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                gridData[y][x] = 0;
            }
        }
        optScale = cellSize;
        clrDraw = 1; // Black
        clrBack = 0; // Transparent
        clrScreen = TIGlobals.TI_PALETTE[0]; // Transparent
        clrGrid = new Color(128, 128, 128);
        clrGridDraw = new Color(240, 240, 240);
        clrGrup = new Color(164, 164, 255);
        clrHigh = Globals.CLR_HIGHLIGHT;
        if (colorMode == COLOR_MODE_BITMAP) {
            gridColors = new int[gridHeight][2];
            for (int y = 0; y < gridHeight; y++) {
                gridColors[y][0] = 0;
                gridColors[y][1] = 1;
            }
        }

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        parentMouseListener = mlParent;
        parentMouseMotionListener = mmlParent;

        resetUndoRedo();
        saveUndoState();

        bufferDraw = getImageBuffer(gridWidth * optScale, gridHeight * optScale);
    }

// Accessors -------------------------------------------------------------------------------/


    public Color[] getPalette() {
        return palette;
    }

    public void setPalette(Color[] palette) {
        this.palette = palette;
    }

    public boolean useTransparency() {
        return colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP || ecmTransparency;
    }

    public void setECMTransparency(boolean enabled) {
        ecmTransparency = enabled;
    }

    public int[][] getGridData() {
        return gridData;
    }

    public int getColorDraw() {
        return clrDraw;
    }

    public int getColorBack() {
        return clrBack;
    }

    public Color getColorScreen() {
        return clrScreen;
    }

    public Color getColorGrid() {
        return clrGrid;
    }

    public Color getColorHigh() {
        return clrHigh;
    }

    public boolean isPaintOn() {
        return paintOn;
    }

    public int getViewScale() {
        return viewScale;
    }

    public int getGridGroup() {
        return gridGroup;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public int[][] getGridColors() {
        return gridColors;
    }

    private void setGridData(int[][] gd) {
        for (int y = 0; y < gridData.length; y++) {
            for (int x = 0; x < gridData[0].length; x++) {
                gridData[y][x] = gd[y][x];
            }
        }
    }

    private void setGridColors(int[][] gc) {
        for (int y = 0; y < gridColors.length; y++) {
            for (int x = 0; x < gridColors[0].length; x++) {
                gridColors[y][x] = gc[y][x];
            }
        }
    }

    public void setColorDraw(int clr) {
        clrDraw = clr;
    }

    public void setColorBack(int clr) {
        clrBack = clr;
    }

    public void setColorScreen(Color clr) {
        clrScreen = clr;
        redrawCanvas();
    }

    public void setColorGrid(Color clr) {
        clrGrid = clr;
    }

    public void setColorHigh(Color clr) {
        clrHigh = clr;
    }

    public void setPaintOn(boolean b) {
        paintOn = b;
    }

    public void setViewScale(int i) {
        viewScale = i;
    }

    public void setGridGroup(int i) {
        gridGroup = i;
    }

    public void setShowGrid(boolean b) {
        showGrid = b;
    }

    public void setColorMode(ColorMode colorMode, Color[] palette) {
        this.colorMode = colorMode;
        if (colorMode == COLOR_MODE_BITMAP && gridColors == null) {
            gridColors = new int[gridData.length][2];
            for (int y = 0; y < gridData.length; y++) {
                gridColors[y][0] = 0;
                gridColors[y][1] = 1;
            }
        }
        // Reduce bpp
        if (colorMode != COLOR_MODE_ECM_3)  {
            int max = colorMode == COLOR_MODE_ECM_2 ? 3 : 1;
            for (int y = 0; y < gridData.length; y++) {
                for (int x = 0; x < gridData[0].length; x++) {
                    gridData[y][x] = gridData[y][x] > max ? max : gridData[y][x];
                }
            }
        }
        this.palette = palette;
        clrBack = 0;
        clrDraw = 1;
        resetUndoRedo();
        redrawCanvas();
    }

    public int getGridWidth() {
        return gridData[0].length;
    }

    public int getGridHeight() {
        return gridData.length;
    }

    public int getGridAt(int x, int y) {
        if (x >= 0 && y >= 0 && y < gridData.length && x < gridData[y].length) {
            return gridData[y][x];
        }
        return NODATA;
    }

    public void setGridAt(int x, int y, int v) {
        if (x >= 0 && y >= 0 && y < gridData.length && x < gridData[y].length) {
            gridData[y][x] = v;
        }
        saveUndoState();
    }

    public void setGrid(int[][] gd) {
        gridData = gd;
        redrawCanvas();
        saveUndoState();
    }

    public void setColors(int[][] gc) {
        gridColors = gc;
        redrawCanvas();
        saveUndoState();
    }

    public void setGridAndColors(int[][] gd, int[][] gc) {
        gridData = gd;
        if (gc != null) {
            gridColors = gc;
        }
        redrawCanvas();
        saveUndoState();
    }

    public void toggleGrid() {
        this.setShowGrid(!this.isShowGrid());
        this.redrawCanvas();
    }

    public void resetUndoRedo() {
        undoList = new ArrayList<int[][][]>();
        undoIndex = -1;
        notifyUndoRedoListeners();
    }

    public void undo() {
        if (canUndo()) {
            int[][][] undoState = undoList.get(--undoIndex);
            setGridData(undoState[0]);
            if (undoState[1] != null) {
                setGridColors(undoState[1]);
            }
            redrawCanvas();
            notifyUndoRedoListeners();
        }
    }

    private boolean canUndo() {
        return undoIndex > 0;
    }

    public void redo() {
        if (canRedo()) {
            int[][][] undoState = undoList.get(++undoIndex);
            setGridData(undoState[0]);
            if (undoState[1] != null) {
                setGridColors(undoState[1]);
            }
            redrawCanvas();
            notifyUndoRedoListeners();
        }
    }

    private boolean canRedo() {
        return undoIndex < undoList.size() - 1;
    }

    private void saveUndoState() {
        boolean change = false;
        int[][][] oldState = undoIndex != -1 ? undoList.get(undoIndex) : null;
        int[][] undoGrid = new int[gridData.length][gridData[0].length];
        for (int y = 0; y < gridData.length; y++) {
            for (int x = 0; x < gridData[0].length; x++) {
                if (oldState == null || oldState[0][y][x] != gridData[y][x]) {
                    change = true;
                }
                undoGrid[y][x] = gridData[y][x];
            }
        }
        int[][] undoColors = colorMode == COLOR_MODE_BITMAP ? new int[gridColors.length][2] : null;
        if (colorMode == COLOR_MODE_BITMAP) {
            for (int y = 0; y < gridColors.length; y++) {
                for (int x = 0; x < gridColors[0].length; x++) {
                    if (oldState == null || oldState[1][y][x] != gridColors[y][x]) {
                        change = true;
                    }
                    undoColors[y][x] = gridColors[y][x];
                }
            }
        }
        if (change) {
            int[][][] undoState = new int[2][][];
            undoState[0] = undoGrid;
            undoState[1] = undoColors;
            undoList.add(++undoIndex, undoState);
            for (int i = undoList.size() - 1; i > undoIndex ; i--) {
                undoList.remove(i);
            }
            notifyUndoRedoListeners();
        }
    }

    public void addUndoRedoListener(UndoRedoListener undoRedoListener) {
        undoRedoListeners.add(undoRedoListener);
    }

    private void notifyUndoRedoListeners() {
        for (UndoRedoListener undoRedoListener : undoRedoListeners) {
            undoRedoListener.undoRedoStateChanged(canUndo(), canRedo(), this);
        }
    }

// Rendering Methods -----------------------------------------------------------------------/

    public void redrawCanvas() {
        if (bufferDraw != null) {
            Graphics g = bufferDraw.getGraphics();
            g.setColor(palette[clrBack]);
            g.fillRect(0, 0, bufferDraw.getWidth(this), bufferDraw.getHeight(this));
            for (int y = 0; y < gridData.length; y++) {
                for (int x = 0; x < gridData[0].length; x++) {
                    if (gridData[y][x] != NODATA) {
                        int c;
                        if (colorMode == COLOR_MODE_GRAPHICS_1) {
                            c = (gridData[y][x] == 1 ? clrDraw : clrBack);
                        }
                        else if (colorMode == COLOR_MODE_BITMAP) {
                            c = gridColors[y][gridData[y][x]];
                        }
                        else {
                            c = gridData[y][x];
                        }
                        try {
                            g.setColor(c == 0 && useTransparency() ? clrScreen : palette[c]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                            System.out.println("Color mode: " + colorMode);
                            System.out.println("Palette size: " + palette.length);
                            System.out.println("Index requested: " + c);
                        }
                        g.fillRect(x * optScale, y * optScale, optScale, optScale);
                    }
                }
            }
            if (!hotCell.equals(PT_OFFGRID)) {
                highlightCell(g, hotCell.getX(), hotCell.getY());
            }
            currBounds = this.getBounds();
            viewScale = (int) (Math.max((Math.floor(Math.min((currBounds.width - currBounds.x) / gridData[0].length, (currBounds.height - currBounds.y) / gridData.length)) / optScale), 1));
            currBufferScaled = bufferDraw.getScaledInstance(gridData[0].length * optScale * viewScale, gridData.length * optScale * viewScale, BufferedImage.SCALE_REPLICATE);
            g.dispose();
            this.repaint();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            if (currBufferScaled != null) {
                g.drawImage(currBufferScaled, gridOffsetX, gridOffsetY, this);
                if (showGrid) {
                    g.setColor(clrGrid);
                    for (int y = 0; y < gridData.length; y++) {
                        for (int x = 0; x < gridData[0].length; x++) {
                            if (colorMode == COLOR_MODE_BITMAP) {
                                g.setColor(gridData[y][x] == 1 ? clrGridDraw : clrGrid);
                            }
                            g.drawRect(x * optScale * viewScale + gridOffsetX, y * optScale * viewScale + gridOffsetY, (optScale * viewScale) - 1, (optScale * viewScale) - 1);
                        }
                    }
                    if (gridGroup > 0) {
                        for (int y = 0; y < (gridData.length / gridGroup); y++) {
                            for (int x = 0; x < (gridData[y].length / gridGroup); x++) {
                                g.setColor(clrGrup);
                                g.drawRect(gridOffsetX + ((x * optScale * viewScale) * gridGroup), gridOffsetY + ((y * optScale * viewScale) * gridGroup), optScale * viewScale * gridGroup, optScale * viewScale * gridGroup);
                            }
                        }
                    }
                }
            }
            g.dispose();
        } catch (NullPointerException npe) { /* component not yet initialised */ }
    }

    protected void highlightCell(Graphics g, int x, int y) {
        g.setColor(clrHigh);
        g.drawRect(x * optScale, y * optScale, optScale - 1, optScale - 1);
    }

    protected void highlightCell(Graphics g, double x, double y) {
        highlightCell(g, (int) x, (int) y);
    }

    private void setAllGrid(int v) {
        for (int y = 0; y < gridData.length; y++) {
            for (int x = 0; x < gridData[y].length; x++) {
                gridData[y][x] = v;
            }
        }
        redrawCanvas();
        saveUndoState();
    }

    private void setAllColors(int back, int fore) {
        for (int y = 0; y < gridColors.length; y++) {
            gridColors[y][0] = back;
            gridColors[y][1] = fore;
        }
        saveUndoState();
    }

    public void clearGrid() {
        setAllGrid(colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 0 : clrBack);
        if (colorMode == COLOR_MODE_BITMAP) {
            setAllColors(clrBack, clrDraw);
        }
    }

    public void fillGrid() {
        setAllGrid(colorMode == COLOR_MODE_GRAPHICS_1 || colorMode == COLOR_MODE_BITMAP ? 1 : clrDraw);
        if (colorMode == COLOR_MODE_BITMAP) {
            setAllColors(clrBack, clrDraw);
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
                        pt.getX() >= (gridOffsetX + (optScale * viewScale * gridData[0].length)) ||
                        pt.getY() >= (gridOffsetY + (optScale * viewScale * gridData.length))
                ) {
            return PT_OFFGRID;
        }
        int ptx = (int) ((pt.getX() - gridOffsetX) / (optScale * viewScale));
        int pty = (int) ((pt.getY() - gridOffsetY) / (optScale * viewScale));
        return new Point(ptx, pty);
    }

    public void setPaintMode(Point pt) {
        Point mouseCell = getMouseCell(pt);
        paintOn = true;
        if (!mouseCell.equals(PT_OFFGRID)) {
            paintOn = (gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] == 0);
            /*
            if (colorMode == Magellan.COLOR_MODE_BITMAP && !paintOn) {
                paintOn = gridColors[(int) (mouseCell.getY())][1] != clrDraw;
            }
            */
        }
    }

    public void processCellAtPoint(Point pt) {
        Point mouseCell = getMouseCell(pt);
        if (!mouseCell.equals(PT_OFFGRID)) {
            if (colorMode == COLOR_MODE_GRAPHICS_1) {
                gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = (paintOn ? 1 : 0);
            }
            else if (colorMode == COLOR_MODE_BITMAP) {
                if (mouseButton == MouseEvent.BUTTON1) {
                    if (paintOn) {
                        gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = 1;
                    }
                    else if (gridColors[(int) (mouseCell.getY())][1] == clrDraw) {
                        gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = 0;
                    }
                    gridColors[(int) (mouseCell.getY())][1] = clrDraw;
                } else {
                    if (!paintOn) {
                        gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = 0;
                    }
                    else if (gridColors[(int) (mouseCell.getY())][0] == clrBack) {
                        gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = 1;
                    }
                    gridColors[(int) (mouseCell.getY())][0] = clrBack;
                }
            }
            else {
                gridData[(int) (mouseCell.getY())][(int) (mouseCell.getX())] = (mouseButton == MouseEvent.BUTTON1 ? clrDraw : clrBack);
            }
            hotCell.setLocation(mouseCell);
            redrawCanvas();
        }
    }

// Listener Methods ------------------------------------------------------------------------/

    /* MouseListener methods */
    public void mousePressed(MouseEvent me) {
        mouseButton = me.getButton();
        setPaintMode(me.getPoint());
        processCellAtPoint(me.getPoint());
        parentMouseListener.mousePressed(me);
        lastCell = null;
    }

    public void mouseReleased(MouseEvent me) {
        mouseButton = 0;
        saveUndoState();
    }

    public void mouseClicked(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    /* MouseMotionListener methods */
    public void mouseMoved(MouseEvent me) {
        if (!me.getPoint().equals(ptLastGrid)) {
            hotCell.setLocation(getMouseCell(me.getPoint()));
            ptLastGrid.setLocation(me.getPoint());
            redrawCanvas();
            parentMouseMotionListener.mouseMoved(me);
        }
    }

    public void mouseDragged(MouseEvent me) {
        Point mouseCell = getMouseCell(me.getPoint());
        if (lastCell != null && !mouseCell.equals(lastCell)) {
            processCellAtPoint(me.getPoint());
            parentMouseMotionListener.mouseDragged(me);
        }
        lastCell = mouseCell;
    }
}
