package com.dreamcodex.ti.component;

import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_BITMAP;
import static com.dreamcodex.ti.util.ColorMode.COLOR_MODE_GRAPHICS_1;
import static java.lang.Math.floorMod;

public class AnalyzeCharTransDialog extends JDialog implements ActionListener, MapChangeListener, WindowListener, ListSelectionListener {

    private final MapEditor mapEditor;
    private final HashMap<Integer, Image> charImages;
    private final HashMap<Integer, int[][]> charGrids;
    private final HashMap<Integer, int[][]> charColors;
    private final int[][] clrSets;
    private final ColorMode colorMode;
    private final int screenColor;
    private static TransitionType transitionType = TransitionType.TOP_TO_BOTTOM;
    private static boolean wrap = false;
    private final JComboBox transitionTypeComboBox;
    private final JCheckBox wrapCheckbox;
    private final JTable jTable;
    private final CharTransTableModel tableModel;
    private final JButton refreshButton;
    private final JButton closeButton;
    private ArrayList<TransChar> sortedTransCharList;

    private class CharTransTableModel extends AbstractTableModel {

        Map<String, TransChar> transCharMap;

        CharTransTableModel() {
            buildMap();
        }

        public void refresh() {
            buildMap();
            fireTableDataChanged();
            mapEditor.removeAllHighlights();
            mapEditor.redrawCanvas();
        }

        public void buildMap() {
            transCharMap = new HashMap<>();
            int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
            int height = mapData.length;
            int width = mapData[0].length;
            if (width > 1 && height > 1) {
                switch (transitionType) {
                    case TOP_TO_BOTTOM:
                    case BOTTOM_TO_TOP:
                    case LEFT_TO_RIGHT:
                    case RIGHT_TO_LEFT:
                        int yStart = transitionType.getyOffset() < 0 && !wrap ? 1 : 0;
                        int yEnd = height - (transitionType.getyOffset() > 0 && !wrap ? 1 : 0);
                        int xStart = transitionType.getxOffset() < 0 && !wrap ? 1 : 0;
                        int xEnd = width - (transitionType.getxOffset() > 0 && !wrap ? 1 : 0);
                        for (int y = yStart; y < yEnd; y++) {
                            for (int x = xStart; x < xEnd; x++) {
                                int fromChar = mapData[y][x];
                                int toChar = mapData[floorMod(y + transitionType.getyOffset(), height)][floorMod(x + transitionType.getxOffset(), width)];
                                addTransCharToMap(new TransChar(fromChar, toChar));
                            }
                        }
                        break;
                    case TWO_DIMENSIONAL:
                        for (int y = wrap ? 0 : 1; y < mapData.length; y++) {
                            for (int x = 0; x < width - (wrap ? 0 : 1); x++) {
                                int fromChar = mapData[y][x];
                                int toChar1 = mapData[y][x < width - 1 ? x + 1 : 0];
                                int toChar2 = mapData[y > 0 ? y - 1 : height - 1][x];
                                int toChar3 = mapData[y > 0 ? y - 1 : height - 1][x < width - 1 ? x + 1 : 0];
                                addTransCharToMap(new TransChar(fromChar, toChar1, toChar2, toChar3));
                            }
                        }
                        break;
                    case ISOMETRIC:
                        break;
                }
            }
            sortedTransCharList = new ArrayList<>(transCharMap.values());
            sortedTransCharList.sort(new TransChar.TransCharCountComparator());
        }

        private void addTransCharToMap(TransChar transChar) {
            String key = transChar.getKey();

            TransChar existingTransChar = transCharMap.get(key);
            if (existingTransChar != null) {
                existingTransChar.incCount();
            }
            else {
                transChar.setColorsOK(areColorsOK(transChar));
                transCharMap.put(key, transChar);
            }
        }

        private boolean areColorsOK(TransChar transChar) {
            boolean colorsOK;
            if (colorMode == COLOR_MODE_GRAPHICS_1 ) {
                int[] fromClrSet = clrSets[transChar.getFromChar() / 8];
                int[] toClrSet = clrSets[transChar.getToChar() / 8];
                colorsOK = Globals.isColorTransitionOK(
                        fromClrSet[Globals.INDEX_CLR_FORE],
                        toClrSet[Globals.INDEX_CLR_FORE],
                        fromClrSet[Globals.INDEX_CLR_BACK],
                        toClrSet[Globals.INDEX_CLR_BACK],
                        screenColor,
                        charGrids.get(transChar.getFromChar()),
                        charGrids.get(transChar.getToChar())
                );
            }
            else if (colorMode == COLOR_MODE_BITMAP) {
                if (transitionType == TransitionType.TOP_TO_BOTTOM ||transitionType == TransitionType.BOTTOM_TO_TOP) {
                    colorsOK = true;
                }
                else {
                    colorsOK = true;
                    int[][] fromColorGrid = charColors.get(transChar.getFromChar());
                    int[][] toColorGrid = charColors.get(transChar.getToChar());
                    int[][] fromCharGrid = charGrids.get(transChar.getFromChar());
                    int[][] toCharGrid = charGrids.get(transChar.getToChar());
                    for (int i = 0; i < 8 && colorsOK; i++) {
                        if (!Globals.isColorTransitionOK(
                                fromColorGrid[i][Globals.INDEX_CLR_FORE],
                                toColorGrid[i][Globals.INDEX_CLR_FORE],
                                fromColorGrid[i][Globals.INDEX_CLR_BACK],
                                toColorGrid[i][Globals.INDEX_CLR_BACK],
                                screenColor,
                                fromCharGrid[i],
                                toCharGrid[i]
                        )) {
                            colorsOK = false;
                        }
                    }
                }
            }
            else {
                colorsOK = true;
            }
            return colorsOK;
        }

        public int getRowCount() {
            return sortedTransCharList.size();
        }

        public int getColumnCount() {
            return 5;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return rowIndex;
            }
            if (columnIndex == 1) {
                return sortedTransCharList.get(rowIndex);
            }
            if (columnIndex == 2) {
                return sortedTransCharList.get(rowIndex);
            }
            if (columnIndex == 3) {
                return sortedTransCharList.get(rowIndex).getCount();
            }
            if (columnIndex == 4) {
                return sortedTransCharList.get(rowIndex).isColorsOK() ? "Yes" : "No";
            }
            return null;
        }

        public String getColumnName(int column) {
            if (column == 0) {
                return "#";
            }
            if (column == 1) {
                return "From";
            }
            if (column == 2) {
                return "To";
            }
            if (column == 3) {
                return "Count";
            }
            if (column == 4) {
                return "Colors OK";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            }
            if (columnIndex == 1) {
                return TransChar.class;
            }
            if (columnIndex == 2) {
                return TransChar.class;
            }
            if (columnIndex == 3) {
                return Integer.class;
            }
            if (columnIndex == 4) {
                return String.class;
            }
            return null;
        }
    }

    private static class CenteredRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setHorizontalAlignment(SwingConstants.CENTER);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private class TransCharRenderer extends DefaultTableCellRenderer {

        boolean from;

        public TransCharRenderer(boolean from) {
            this.from = from;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            TransChar transChar = (TransChar) value;
            if (from) {
                setText(Integer.toString(transChar.getFromChar()));
                Image image = charImages.get(transChar.getFromChar());
                if (image != null) {
                    setIcon(new ImageIcon(image));
                }
            }
            else {
                setText(Lists.commaSeparatedList(transChar.getToChars()));
                Image image = charImages.get(transChar.getToChar());
                if (image != null) {
                    setIcon(new ImageIcon(image));
                }
            }
            return this;
        }
    }

    public AnalyzeCharTransDialog(
        JFrame parent,
        MapEditor mapEditor,
        HashMap<Integer, Image> charImages,
        HashMap<Integer, int[][]> charGrids,
        HashMap<Integer, int[][]> charColors,
        int[][] clrSets,
        ColorMode colorMode
    ) {
        super(parent, "Analyze Character Transitions");
        this.mapEditor = mapEditor;
        this.charImages = charImages;
        this.charGrids = charGrids;
        this.charColors = charColors;
        this.clrSets = clrSets;
        this.colorMode = colorMode;
        screenColor = mapEditor.getColorScreen();
        setLayout(new BorderLayout());
        transitionTypeComboBox = new JComboBox(TransitionType.values());
        wrapCheckbox = new JCheckBox("Wrap Edges", wrap);
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(transitionTypeComboBox);
        optionsPanel.add(wrapCheckbox);
        add(optionsPanel, BorderLayout.NORTH);
        jTable = new JTable();
        tableModel = new CharTransTableModel();
        jTable.setModel(tableModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(jTable.getModel());
        sorter.setComparator(1, new TransChar.TransCharFromComparator());
        sorter.setComparator(2, new TransChar.TransCharToComparator());
        sorter.setSortsOnUpdates(true);
        sorter.toggleSortOrder(3); // Sort by count
        jTable.setRowSorter(sorter);
        jTable.getColumnModel().getColumn(0).setCellRenderer(new CenteredRenderer());
        jTable.getColumnModel().getColumn(1).setCellRenderer(new TransCharRenderer(true));
        jTable.getColumnModel().getColumn(2).setCellRenderer(new TransCharRenderer(false));
        jTable.getColumnModel().getColumn(4).setCellRenderer(new CenteredRenderer());
        jTable.setRowHeight(22);
        JScrollPane jScrollPane = new JScrollPane(jTable);
        add(jScrollPane, BorderLayout.CENTER);
        refreshButton = new JButton("Refresh");
        closeButton = new JButton("Close");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(this);
        setSize(400, 500);
        setVisible(true);
        addWindowListener(this);
        transitionTypeComboBox.addActionListener(this);
        wrapCheckbox.addActionListener(this);
        refreshButton.addActionListener(this);
        closeButton.addActionListener(this);
        jTable.setRowSelectionAllowed(true);
        jTable.setColumnSelectionAllowed(false);
        jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable.getSelectionModel().addListSelectionListener(this);
        mapEditor.addMapChangeListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == transitionTypeComboBox) {
            transitionType = (TransitionType) transitionTypeComboBox.getSelectedItem();
            tableModel.refresh();
        }
        else if (e.getSource() == wrapCheckbox) {
            wrap = wrapCheckbox.isSelected();
            tableModel.refresh();
        }
        else if (e.getSource() == refreshButton) {
            tableModel.refresh();
        }
        else if (e.getSource() == closeButton) {
            mapEditor.removeAllHighlights();
            mapEditor.removeMapChangeListener(this);
            this.dispose();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (sortedTransCharList != null && jTable.getSelectedRow() != -1 && sortedTransCharList.size() > jTable.getSelectedRow()) {
            mapEditor.removeAllHighlights();
            TransChar transChar =  sortedTransCharList.get(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
            int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
            int height = mapData.length;
            int width = mapData[0].length;
            if (height > 1 && width > 1) {
                switch (transitionType) {
                    case TOP_TO_BOTTOM:
                    case BOTTOM_TO_TOP:
                    case LEFT_TO_RIGHT:
                    case RIGHT_TO_LEFT:
                        int yStart = transitionType.getyOffset() < 0 && !wrap ? 1 : 0;
                        int yEnd = height - (transitionType.getyOffset() > 0 && !wrap ? 1 : 0);
                        int xStart = transitionType.getxOffset() < 0 && !wrap ? 1 : 0;
                        int xEnd = width - (transitionType.getxOffset() > 0 && !wrap ? 1 : 0);
                        for (int y = yStart; y < yEnd; y++) {
                            for (int x = xStart; x < xEnd; x++) {
                                int fromChar = mapData[y][x];
                                int toY = floorMod(y + transitionType.getyOffset(), height);
                                int toX = floorMod(x + transitionType.getxOffset(), width);
                                int toChar = mapData[toY][toX];
                                if (fromChar == transChar.getFromChar() && toChar == transChar.getToChar()) {
                                    mapEditor.highlightCell(x, y);
                                    mapEditor.highlightCell(toX, toY);
                                }
                            }
                        }
                        break;
                    case TWO_DIMENSIONAL:
                        for (int y = 1; y < height; y++) {
                            for (int x = 0; x < width - 1; x++) {
                                int fromChar = mapData[y][x];
                                int toChar1 = mapData[y][x + 1];
                                int toChar2 = mapData[y - 1][x];
                                int toChar3 = mapData[y - 1][x + 1];
                                if (fromChar == transChar.getFromChar() && Arrays.equals(transChar.getToChars(), new int[] {toChar1, toChar2, toChar3})) {
                                    mapEditor.highlightCell(x, y);
                                    mapEditor.highlightCell(x + 1, y);
                                    mapEditor.highlightCell(x, y - 1);
                                    mapEditor.highlightCell(x + 1, y - 1);
                                }
                            }
                        }
                        break;
                    case ISOMETRIC:
                        break;
                }
            }
            mapEditor.redrawCanvas();
        }
    }

    public void mapChanged() {
        TransChar transChar = null;
        if (sortedTransCharList != null && jTable.getSelectedRow() != -1 && sortedTransCharList.size() > jTable.getSelectedRow()) {
            transChar =  sortedTransCharList.get(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
        }
        tableModel.refresh();
        if (transChar != null) {
            int modelRowIndex = sortedTransCharList.indexOf(transChar);
            if (modelRowIndex != -1) {
                int viewIndex = jTable.convertRowIndexToView(modelRowIndex);
                jTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
            }
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        mapEditor.removeAllHighlights();
        mapEditor.removeMapChangeListener(this);
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }
}
