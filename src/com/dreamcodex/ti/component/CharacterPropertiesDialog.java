package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class CharacterPropertiesDialog extends JDialog implements ActionListener {

    private final int ch;
    private final HashMap<Integer, String> charNames;
    private final HashMap<Integer, Integer> charProperties;
    private final String[] charPropertyLabels;
    private JTextField nameField;
    private JCheckBox[] charPropertyCheckboxes;
    private JTextField[] charPropertyLabelFields;
    private JButton okButton;
    private JButton cancelButton;

    public CharacterPropertiesDialog(JFrame parent, int ch, HashMap<Integer, String> charNames, HashMap<Integer, Integer> charProperties, String[] charPropertyLabels) {
        super(parent, "Character Properties");
        this.ch = ch;
        this.charNames = charNames;
        this.charProperties = charProperties;
        this.charPropertyLabels = charPropertyLabels;
        setLocationRelativeTo(this);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weighty = 1;

        JLabel nameLabel = new JLabel("Name");
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(nameLabel, gbc);

        nameField = new JTextField(charNames.get(ch)) {
            @Override
            public Insets getInsets() {
                return new Insets(4, 4, 4,4);
            }
        };
        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(nameField, gbc);

        charPropertyCheckboxes = new JCheckBox[8];
        charPropertyLabelFields = new JTextField[8];
        Integer prop = charProperties.get(ch);
        for (int i = 0; i < 8; i++) {
            charPropertyCheckboxes[i] = new JCheckBox("Bit weight >" + Globals.toHexString(1 << i, 2) + " labelled ", prop != null && (prop & (1 << i)) != 0);
            gbc.gridy = i + 1;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(charPropertyCheckboxes[i], gbc);
            charPropertyLabelFields[i] = new JTextField(charPropertyLabels[i]);
            gbc.gridx = 2;
            gbc.gridwidth = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(charPropertyLabelFields[i], gbc);
        }

        add(panel, BorderLayout.CENTER);

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // pack();
        setSize(400, 300);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            charNames.put(ch, nameField.getText());
            int prop = 0;
            for (int i = 0; i < 8; i++) {
                if (charPropertyCheckboxes[i].isSelected()) {
                    prop |= (1 << i);
                }
                charProperties.put(ch, prop);
                charPropertyLabels[i] = charPropertyLabelFields[i].getText();
            }
            this.dispose();
        }
        else if (e.getSource() == cancelButton) {
            this.dispose();
        }
    }
}
