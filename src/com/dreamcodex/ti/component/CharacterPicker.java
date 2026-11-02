package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.function.IntFunction;

/**
 * Character picker that looks like the character dock. Each cell is painted with a single
 * "rubber stamp" button that copies the icon, text, colors and border of the corresponding
 * character dock button, so there's no need for a second set of buttons.
 */
public class CharacterPicker extends JPanel {

    public interface Listener {
        void characterPicked(int charNum, MouseEvent e);
    }

    // Characters per row, as in the character dock. The first column holds the row labels.
    public static final int COLS = 8;
    public static final int CELL_HEIGHT = Globals.DM_TOOL.height;

    private final JButton[] charButtons;
    private final IntFunction<JLabel> rowLabelFactory;
    private final Listener listener;
    private final CellRendererPane rendererPane = new CellRendererPane();
    private final JButton stamp = new JButton();
    // Row labels by the first character of the row
    private final HashMap<Integer, JLabel> rowLabels = new HashMap<>();
    private int firstChar;
    private int lastChar;
    private int selectedChar = -1;

    /**
     * @param charButtons     the character dock buttons, whose look is copied
     * @param rowLabelFactory creates the label for the row starting at the given character
     */
    public CharacterPicker(JButton[] charButtons, IntFunction<JLabel> rowLabelFactory, int firstChar, int lastChar, Listener listener) {
        super(null);
        this.charButtons = charButtons;
        this.rowLabelFactory = rowLabelFactory;
        this.listener = listener;
        setOpaque(true);
        setBackground(Globals.CLR_COMPONENTBACK);
        setToolTipText("");
        add(rendererPane);
        stamp.setOpaque(true);
        stamp.setMargin(new Insets(0, 0, 0, 0));
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
        rowLabels.clear();
        int rows = ((lastChar - firstChar) / COLS) + 1;
        setPreferredSize(new Dimension((COLS + 1) * Globals.DM_TOOL.width, rows * CELL_HEIGHT));
        revalidate();
        repaint();
    }

    public void setSelectedChar(int charNum) {
        if (charNum != selectedChar) {
            selectedChar = charNum;
            Rectangle bounds = getCellBounds(charNum);
            if (bounds != null) {
                scrollRectToVisible(bounds);
            }
        }
        // The selection is shown by the border copied from the dock button
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int charNum = getCharAt(e.getPoint());
        return charNum != -1 ? charNum + " (>" + Integer.toHexString(charNum).toUpperCase() + ")" : null;
    }

    // Cells stretch to fill the width like the dock's GridLayout, which centers any remainder
    private int getCellWidth() {
        return Math.max(1, getWidth() / (COLS + 1));
    }

    private int getOffsetX() {
        return (getWidth() - getCellWidth() * (COLS + 1)) / 2;
    }

    private int getCharAt(Point p) {
        int col = Math.floorDiv(p.x - getOffsetX(), getCellWidth()) - 1;
        int row = p.y / CELL_HEIGHT;
        if (col < 0 || col >= COLS || row < 0) {
            return -1;
        }
        int charNum = firstChar + row * COLS + col;
        return charNum <= lastChar ? charNum : -1;
    }

    private Rectangle getCellBounds(int charNum) {
        if (charNum < firstChar || charNum > lastChar) {
            return null;
        }
        int i = charNum - firstChar;
        int cellWidth = getCellWidth();
        return new Rectangle(getOffsetX() + (i % COLS + 1) * cellWidth, (i / COLS) * CELL_HEIGHT, cellWidth, CELL_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle clip = g.getClipBounds();
        int cellWidth = getCellWidth();
        int offsetX = getOffsetX();
        int rows = ((lastChar - firstChar) / COLS) + 1;
        int firstRow = clip != null ? Math.max(0, clip.y / CELL_HEIGHT) : 0;
        int lastRow = clip != null ? Math.min(rows - 1, (clip.y + clip.height) / CELL_HEIGHT) : rows - 1;
        for (int row = firstRow; row <= lastRow; row++) {
            int rowFirstChar = firstChar + row * COLS;
            int y = row * CELL_HEIGHT;
            JLabel label = rowLabels.computeIfAbsent(rowFirstChar, rowLabelFactory::apply);
            rendererPane.paintComponent(g, label, this, offsetX, y, cellWidth, CELL_HEIGHT, true);
            for (int col = 0; col < COLS; col++) {
                int charNum = rowFirstChar + col;
                if (charNum > lastChar) {
                    break;
                }
                JButton button = charButtons[charNum];
                stamp.setIcon(button.getIcon());
                stamp.setText(button.getText());
                stamp.setFont(button.getFont());
                stamp.setBackground(button.getBackground());
                stamp.setForeground(button.getForeground());
                stamp.setBorder(button.getBorder());
                rendererPane.paintComponent(g, stamp, this, offsetX + (col + 1) * cellWidth, y, cellWidth, CELL_HEIGHT, true);
            }
        }
    }
}
