package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TIGlobals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * Grid of character images for picking a character, drawn on a single component
 * so it doesn't need a button per character.
 */
public class CharacterPicker extends JPanel {

    public interface Listener {
        void characterPicked(int charNum, MouseEvent e);
    }

    public static final int COLS = 8;
    public static final int CELL_SIZE = 24;
    private static final int IMAGE_SIZE = 16;

    private final HashMap<Integer, Image> charImages;
    private final Listener listener;
    private int firstChar;
    private int lastChar;
    private int selectedChar = -1;

    public CharacterPicker(HashMap<Integer, Image> charImages, int firstChar, int lastChar, Listener listener) {
        this.charImages = charImages;
        this.listener = listener;
        setOpaque(true);
        setBackground(Globals.CLR_COMPONENTBACK);
        setToolTipText("");
        setRange(firstChar, lastChar);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int charNum = getCharAt(e.getPoint());
                if (charNum != -1) {
                    CharacterPicker.this.listener.characterPicked(charNum, e);
                }
            }
        });
    }

    public void setRange(int firstChar, int lastChar) {
        this.firstChar = firstChar;
        this.lastChar = lastChar;
        int rows = ((lastChar - firstChar) / COLS) + 1;
        Dimension size = new Dimension(COLS * CELL_SIZE, rows * CELL_SIZE);
        setPreferredSize(size);
        revalidate();
        repaint();
    }

    public void setSelectedChar(int charNum) {
        if (charNum != selectedChar) {
            selectedChar = charNum;
            repaint();
            // Keep the selection in view
            Point p = getCellOrigin(charNum);
            if (p != null) {
                scrollRectToVisible(new Rectangle(p.x, p.y, CELL_SIZE, CELL_SIZE));
            }
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int charNum = getCharAt(e.getPoint());
        return charNum != -1 ? charNum + " (>" + Integer.toHexString(charNum).toUpperCase() + ")" : null;
    }

    private int getCharAt(Point p) {
        int col = p.x / CELL_SIZE;
        int row = p.y / CELL_SIZE;
        if (col < 0 || col >= COLS || row < 0) {
            return -1;
        }
        int charNum = firstChar + row * COLS + col;
        return charNum <= lastChar ? charNum : -1;
    }

    private Point getCellOrigin(int charNum) {
        if (charNum < firstChar || charNum > lastChar) {
            return null;
        }
        int i = charNum - firstChar;
        return new Point((i % COLS) * CELL_SIZE, (i / COLS) * CELL_SIZE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int offset = (CELL_SIZE - IMAGE_SIZE) / 2;
        for (int charNum = firstChar; charNum <= lastChar; charNum++) {
            Point p = getCellOrigin(charNum);
            Image image = charImages.get(charNum);
            if (image != null) {
                g.drawImage(image, p.x + offset, p.y + offset, IMAGE_SIZE, IMAGE_SIZE, this);
            }
            else {
                // Undefined character
                g.setColor(TIGlobals.TI_COLOR_UNUSED);
                int charMapIndex = charNum - TIGlobals.CHARMAPSTART;
                String text = charMapIndex >= 0 && charMapIndex < TIGlobals.CHARMAP.length ? "" + TIGlobals.CHARMAP[charMapIndex] : "?";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, p.x + (CELL_SIZE - fm.stringWidth(text)) / 2, p.y + (CELL_SIZE + fm.getAscent()) / 2 - 2);
            }
        }
        Point p = getCellOrigin(selectedChar);
        if (p != null) {
            g.setColor(Globals.CLR_HIGHLIGHT);
            g.drawRect(p.x + 1, p.y + 1, CELL_SIZE - 3, CELL_SIZE - 3);
            g.drawRect(p.x + 2, p.y + 2, CELL_SIZE - 5, CELL_SIZE - 5);
        }
    }
}
