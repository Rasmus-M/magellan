package com.dreamcodex.ti.actions.clipboard;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CopyCharAction extends AbstractAction {

    private final JTextField jtxtChar;

    public CopyCharAction(Icon icon, JTextField jtxtChar) {
        super(null, icon);
        this.jtxtChar = jtxtChar;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        jtxtChar.selectAll();
        jtxtChar.copy();
    }
}
