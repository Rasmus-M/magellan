package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * Editor for the characters a metatile consists of: a size x size grid of character images.
 * Clicks and drags are reported per cell to a listener.
 */
public class MetatileCanvas extends JPanel {

    public interface Listener {
        void cellClicked(int x, int y, MouseEvent e);
    }

    private static final Color CLR_GRID = new Color(128, 128, 128);

    private final HashMap<Integer, Image> charImages;
    private final Listener listener;
    private int[][] metatile;
    private Color screenColor = Color.BLACK;
    private boolean showGrid = true;
    private final Point hotCell = new Point(-1, -1);
    private final Point lastCell = new Point(-1, -1);

    public MetatileCanvas(HashMap<Integer, Image> charImages, Listener listener) {
        this.charImages = charImages;
        this.listener = listener;
        setOpaque(true);
        setBackground(Globals.CLR_COMPONENTBACK);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point cell = getCellAt(e.getPoint());
                lastCell.setLocation(cell);
                if (cell.x != -1) {
                    MetatileCanvas.this.listener.cellClicked(cell.x, cell.y, e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point cell = getCellAt(e.getPoint());
                setHotCell(cell);
                if (cell.x != -1 && !cell.equals(lastCell)) {
                    lastCell.setLocation(cell);
                    MetatileCanvas.this.listener.cellClicked(cell.x, cell.y, e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setHotCell(getCellAt(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHotCell(new Point(-1, -1));
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setMetatile(int[][] metatile) {
        this.metatile = metatile;
        repaint();
    }

    public void setScreenColor(Color screenColor) {
        this.screenColor = screenColor;
        repaint();
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        repaint();
    }

    private void setHotCell(Point cell) {
        if (!cell.equals(hotCell)) {
            hotCell.setLocation(cell);
            repaint();
        }
    }

    private int getCellPixels() {
        if (metatile == null) {
            return 0;
        }
        return Math.max(1, Math.min(getWidth(), getHeight()) / metatile.length);
    }

    private Point getOrigin() {
        int size = getCellPixels() * (metatile != null ? metatile.length : 0);
        return new Point((getWidth() - size) / 2, (getHeight() - size) / 2);
    }

    private Point getCellAt(Point p) {
        if (metatile != null) {
            int cellPixels = getCellPixels();
            Point origin = getOrigin();
            int x = Math.floorDiv(p.x - origin.x, cellPixels);
            int y = Math.floorDiv(p.y - origin.y, cellPixels);
            if (x >= 0 && y >= 0 && x < metatile.length && y < metatile.length) {
                return new Point(x, y);
            }
        }
        return new Point(-1, -1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (metatile == null) {
            return;
        }
        int cellPixels = getCellPixels();
        Point origin = getOrigin();
        for (int y = 0; y < metatile.length; y++) {
            for (int x = 0; x < metatile[y].length; x++) {
                int px = origin.x + x * cellPixels;
                int py = origin.y + y * cellPixels;
                Image image = charImages.get(metatile[y][x]);
                if (image != null) {
                    g.drawImage(image, px, py, cellPixels, cellPixels, this);
                }
                else {
                    g.setColor(screenColor);
                    g.fillRect(px, py, cellPixels, cellPixels);
                }
            }
        }
        if (showGrid) {
            // Character cell grid
            g.setColor(CLR_GRID);
            for (int y = 0; y < metatile.length; y++) {
                for (int x = 0; x < metatile[y].length; x++) {
                    g.drawRect(origin.x + x * cellPixels, origin.y + y * cellPixels, cellPixels - 1, cellPixels - 1);
                }
            }
        }
        if (hotCell.x != -1) {
            g.setColor(Globals.CLR_HIGHLIGHT);
            g.drawRect(origin.x + hotCell.x * cellPixels, origin.y + hotCell.y * cellPixels, cellPixels - 1, cellPixels - 1);
            g.drawRect(origin.x + hotCell.x * cellPixels + 1, origin.y + hotCell.y * cellPixels + 1, cellPixels - 3, cellPixels - 3);
        }
    }
}
