package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.ECMPalette;
import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 02-11-13
 * Time: 22:29
 */
public class ECMPaletteComboBox extends JComboBox implements ItemListener {

    /**
     * Receives the palette dock gestures. Implementations decide whether they
     * apply to the character or the sprite editor.
     */
    public interface PaletteHandler {

        void foreColorSelected(int index, ActionEvent e);

        void backColorSelected(int index, ActionEvent e);

        void colorChosen(int index, ActionEvent e);

        void paletteSelected();
    }

    private final static Border labelBorder = BorderFactory.createLineBorder(Color.GRAY);
    private final static Dimension buttonSize = new Dimension(28, 28);
    private final static Insets buttonInsets = new Insets(0, 0, 0, 0);

    private PaletteHandler handler;
    private int indexFore;
    private int indexBack;

    private class ECMPaletteListCellRenderer extends JPanel implements ListCellRenderer {

        private Border selectedBorder = BorderFactory.createEtchedBorder();
        private Border deselectedBorder = BorderFactory.createEmptyBorder();

        JLabel[] labels;

        public ECMPaletteListCellRenderer(int size) {
            setLayout(new GridLayout(1, size));
            labels = new JLabel[size];
            for (int i = 0; i < size; i++) {
                JLabel label = new JLabel();
                label.setOpaque(true);
                label.setBorder(labelBorder);
                label.setPreferredSize(buttonSize);
                add(label);
                labels[i] = label;
            }
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ECMPalette ecmPalette = (ECMPalette) value;
            for (int i = 0; i < ecmPalette.getSize(); i++) {
                labels[i].setBackground(ecmPalette.getColor(i));
            }
            setBorder(isSelected ? selectedBorder : deselectedBorder);
            return this;
        }
    }

    private class ECMComboBoxEditor implements ComboBoxEditor {

        JPanel editor;
        TripleClickButton[] buttons;
        ECMPalette ecmPalette;

        public ECMComboBoxEditor(int size) {
            editor = new JPanel(new GridLayout(1, size));
            buttons = new TripleClickButton[size];
            for (int i = 0; i < size; i++) {
                final int index = i;
                TripleClickButton button = new TripleClickButton(
                    "",
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            indexFore = index;
                            updateEditor();
                            if (handler != null) {
                                handler.foreColorSelected(index, e);
                            }
                        }
                    },
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            indexBack = index;
                            updateEditor();
                            if (handler != null) {
                                handler.backColorSelected(index, e);
                            }
                        }
                    },
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            updateEditor();
                            if (handler != null) {
                                handler.colorChosen(index, e);
                            }
                        }
                    }
                );
                button.setPreferredSize(Globals.DM_TOOL);
                button.setMargin(buttonInsets);
                button.setBorder(labelBorder);
                button.setFocusable(false);
                editor.add(button, i);
                buttons[i] = button;
            }
        }

        public Component getEditorComponent() {
            return editor;
        }

        public void setItem(Object anObject) {
            ecmPalette = (ECMPalette) anObject;
            updateEditor();
        }

        public Object getItem() {
            return ecmPalette;
        }

        public void selectAll() {
            // Not relevant
        }

        public void addActionListener(ActionListener l) {
        }

        public void removeActionListener(ActionListener l) {
        }

        private void updateEditor() {
            for (int i = 0; i < ecmPalette.getSize(); i++) {
                TripleClickButton button = buttons[i];
                button.setBackground(ecmPalette.getColor(i));
                if (i == indexFore) {
                    button.setText("F");
                }
                else if (i == indexBack) {
                    button.setText("B");
                }
                else {
                    button.setText("");
                }
            }
        }
    }

    public ECMPaletteComboBox(ECMPalette[] palettes, int indexFore, int indexBack, PaletteHandler handler) {
        super(palettes);
        this.indexFore = indexFore;
        this.indexBack = indexBack;
        this.handler = handler;
        setRenderer(new ECMPaletteListCellRenderer(palettes[0].getSize()));
        setEditor(new ECMComboBoxEditor(palettes[0].getSize()));
        setEditable(true);
        addItemListener(this);
    }

    public void itemStateChanged(ItemEvent e) {
        if (handler != null) {
            handler.paletteSelected();
        }
    }

    public int getIndexFore() {
        return indexFore;
    }

    public int getIndexBack() {
        return indexBack;
    }
}
