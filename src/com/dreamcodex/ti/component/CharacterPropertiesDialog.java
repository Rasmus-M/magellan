package com.dreamcodex.ti.component;

import javax.swing.*;

public class CharacterPropertiesDialog extends JDialog {

    public CharacterPropertiesDialog(JFrame parent, int ch) {
        super(parent, "Character Properties");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(this);
        setSize(400, 300);
        setVisible(true);
    }
}
