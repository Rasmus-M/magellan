package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.util.Globals;
import com.dreamcodex.ti.util.TransChar;

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

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 06-05-13
 * Time: 20:43
 */
public class AnalyzeCharTransDialog extends JDialog implements ActionListener, MapChangeListener, WindowListener, ListSelectionListener {

    private final MapEditor mapEditor;
    private final HashMap<Integer, Image> charImages;
    private final HashMap<Integer, int[][]> charGrids;
    private final HashMap<Integer, int[][]> charColors;
    private final int[][] clrSets;
    private int colorMode;
    private int screenColor;
    private ArrayList<TransChar> sortedTransCharList;
    private static boolean vertical = true;
    private static boolean wrap = false;
    private JRadioButton verticalButton;
    private JRadioButton horizontalButton;
    private JCheckBox wrapCheckbox;
    private JTable jTable;
    private CharTransTableModel tableModel;
    private JButton refreshButton;
    private JButton closeButton;

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
            transCharMap = new HashMap<String, TransChar>();
            int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
            if (mapData.length > 1 && mapData[0].length > 1) {
                for (int y = (vertical && !wrap ? 1 : 0); y < mapData.length; y++) {
                    for (int x = 0; x < mapData[0].length - (vertical || wrap ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        int toChar = vertical ? mapData[y > 0 ? y - 1 : mapData.length - 1][x] : mapData[y][x < mapData[0].length - 1 ? x + 1 : 0];
                        String key = Integer.toString(fromChar) + "-" + Integer.toString(toChar);
                        TransChar transChar = transCharMap.get(key);
                        if (transChar != null) {
                            transChar.incCount();
                        }
                        else {
                            boolean colorsOK;
                            if (colorMode == Magellan.COLOR_MODE_GRAPHICS_1 ) {
                                int[] fromClrSet = clrSets[fromChar / 8];
                                int[] toClrSet = clrSets[toChar / 8];
                                colorsOK = Globals.isColorTransitionOK(
                                    fromClrSet[Globals.INDEX_CLR_FORE],
                                    toClrSet[Globals.INDEX_CLR_FORE],
                                    fromClrSet[Globals.INDEX_CLR_BACK],
                                    toClrSet[Globals.INDEX_CLR_BACK],
                                    screenColor,
                                    charGrids.get(fromChar),
                                    charGrids.get(toChar)
                                );
                            }
                            else if (colorMode == Magellan.COLOR_MODE_BITMAP) {
                                if (vertical) {
                                    colorsOK = true;
                                }
                                else {
                                    colorsOK = true;
                                    int[][] fromColorGrid = charColors.get(fromChar);
                                    int[][] toColorGrid = charColors.get(toChar);
                                    int[][] fromCharGrid = charGrids.get(fromChar);
                                    int[][] toCharGrid = charGrids.get(toChar);
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
                            transCharMap.put(key, new TransChar(fromChar, toChar, colorsOK));
                        }
                    }
                }
            }
            sortedTransCharList = new ArrayList<TransChar>(transCharMap.values());
            Collections.sort(sortedTransCharList, new TransChar.TransCharCountComparator());
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

    private class CenteredRenderer extends DefaultTableCellRenderer {
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
                setIcon(new ImageIcon(charImages.get(transChar.getFromChar())));
            }
            else {
                setText(Integer.toString(transChar.getToChar()));
                setIcon(new ImageIcon(charImages.get(transChar.getToChar())));
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
        int colorMode
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
        verticalButton = new JRadioButton("From Bottom to Top", vertical);
        horizontalButton = new JRadioButton("From Left to Right", !vertical);
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(verticalButton);
        radioButtonGroup.add(horizontalButton);
        JPanel radioButtonPanel = new JPanel();
        radioButtonPanel.add(verticalButton);
        radioButtonPanel.add(horizontalButton);
        wrapCheckbox = new JCheckBox("Wrap Edges", wrap);
        JPanel optionsPanel = new JPanel();
        optionsPanel.add(radioButtonPanel);
        optionsPanel.add(wrapCheckbox);
        add(optionsPanel, BorderLayout.NORTH);
        jTable = new JTable();
        tableModel = new CharTransTableModel();
        jTable.setModel(tableModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(jTable.getModel());
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
        verticalButton.addActionListener(this);
        horizontalButton.addActionListener(this);
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
        if (e.getSource() == verticalButton) {
            vertical = true;
            tableModel.refresh();
        }
        else if (e.getSource() == horizontalButton) {
            vertical = false;
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
            if (mapData.length > 1 && mapData[0].length > 1) {
                for (int y = (vertical ? 1 : 0); y < mapData.length; y++) {
                    for (int x = 0; x < mapData[0].length - (vertical ? 0 : 1); x++) {
                        int fromChar = mapData[y][x];
                        int toChar = vertical ? mapData[y - 1][x] : mapData[y][x + 1];
                        if (fromChar == transChar.getFromChar() && toChar == transChar.getToChar()) {
                            mapEditor.highlightCell(x, y);
                            mapEditor.highlightCell(vertical ? x : x + 1, vertical ? y - 1 : y);
                        }
                    }
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
