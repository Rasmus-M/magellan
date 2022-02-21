package com.dreamcodex.ti.component;

import com.dreamcodex.ti.Magellan;
import com.dreamcodex.ti.iface.MapChangeListener;
import com.dreamcodex.ti.iface.MapSelectListener;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 06-05-13
 * Time: 20:43
 */
public class AnalyzeCharUsageDialog extends JDialog implements ActionListener, MapChangeListener, MapSelectListener, WindowListener, ListSelectionListener {

    private final MapEditor mapEditor;
    private final HashMap<Integer, Image> charImages;
    private final int maxChar;
    private int[] charCounts;
    private static boolean currentMapOnly = true;
    private JCheckBox currentMapCheckBox;
    private JTable jTable;
    private CharUsageTableModel tableModel;
    private JButton refreshButton;
    private JButton closeButton;

    private class CharUsageTableModel extends AbstractTableModel {

        CharUsageTableModel() {
            buildMap();
        }

        public void refresh() {
            buildMap();
            fireTableDataChanged();
            mapEditor.removeAllHighlights();
            mapEditor.redrawCanvas();
        }

        public void buildMap() {
            charCounts = new int[maxChar];
            for (int i = 0; i < mapEditor.getMapCount(); i++) {
                if (!currentMapCheckBox.isSelected() || i == mapEditor.getCurrentMapId()) {
                    int[][] mapData = mapEditor.getMapData(i);
                    for (int y = 0; y < mapData.length; y++) {
                        int[] row  = mapData[y];
                        for (int x = 0; x < row.length; x++) {
                            charCounts[row[x]]++;
                        }
                    }
                }
            }
        }

        public int getRowCount() {
            return charCounts.length;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return rowIndex;
            }
            if (columnIndex == 1) {
                return charCounts[rowIndex];
            }
            return null;
        }

        public String getColumnName(int column) {
            if (column == 0) {
                return "Character";
            }
            if (column == 1) {
                return "Count";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            }
            if (columnIndex == 1) {
                return Integer.class;
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

    private class CharRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Integer ch = (Integer) value;
            setText(Integer.toString(ch));
            Image image = charImages.get(ch);
            if (image != null) {
                setIcon(new ImageIcon(image));
            }
            return this;
        }
    }

    public AnalyzeCharUsageDialog(
        JFrame parent,
        MapEditor mapEditor,
        HashMap<Integer, Image> charImages,
        int maxChar
    ) {
        super(parent, "Analyze Character Usage");
        this.mapEditor = mapEditor;
        this.charImages = charImages;
        this.maxChar = maxChar;
        setLayout(new BorderLayout());
        currentMapCheckBox = new JCheckBox("Current Map Only", currentMapOnly);
        JPanel optionsPanel = new JPanel();
        optionsPanel.add(currentMapCheckBox);
        add(optionsPanel, BorderLayout.NORTH);
        jTable = new JTable();
        tableModel = new CharUsageTableModel();
        jTable.setModel(tableModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(jTable.getModel());
        sorter.setSortsOnUpdates(true);
        sorter.toggleSortOrder(1); // Sort by count
        sorter.toggleSortOrder(1);
        jTable.setRowSorter(sorter);
        jTable.getColumnModel().getColumn(0).setCellRenderer(new CharRenderer());
        jTable.getColumnModel().getColumn(1).setCellRenderer(new CenteredRenderer());
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
        setSize(250, 500);
        setVisible(true);
        addWindowListener(this);
        currentMapCheckBox.addActionListener(this);
        refreshButton.addActionListener(this);
        closeButton.addActionListener(this);
        jTable.setRowSelectionAllowed(true);
        jTable.setColumnSelectionAllowed(false);
        jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable.getSelectionModel().addListSelectionListener(this);
        mapEditor.addMapChangeListener(this);
        mapEditor.addMapSelectListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == currentMapCheckBox) {
            currentMapOnly = currentMapCheckBox.isSelected();
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
        if (jTable.getSelectedRow() != -1) {
            mapEditor.removeAllHighlights();
            int ch =  jTable.convertRowIndexToModel(jTable.getSelectedRow());
            int[][] mapData = mapEditor.getMapData(mapEditor.getCurrentMapId());
            if (mapData.length > 1 && mapData[0].length > 1) {
                for (int y = 0; y < mapData.length; y++) {
                    int[] row = mapData[y];
                    for (int x = 0; x < row.length; x++) {
                        if (row[x] == ch) {
                            mapEditor.highlightCell(x, y);
                        }
                    }
                }
            }
            mapEditor.redrawCanvas();
        }
    }

    public void mapChanged() {
        Integer ch = jTable.getSelectedRow() != -1 ? jTable.convertRowIndexToModel(jTable.getSelectedRow()) : null;
        tableModel.refresh();
        if (ch != null) {
            int viewIndex = jTable.convertRowIndexToView(ch);
            jTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
        }
    }

    @Override
    public void mapSelected() {
        mapChanged();
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
