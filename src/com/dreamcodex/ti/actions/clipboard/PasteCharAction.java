package com.dreamcodex.ti.actions.clipboard;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PasteCharAction extends AbstractAction {

    private final JTextField jtxtChar;
    private final JButton jbtnUpdateChar;

    public PasteCharAction(Icon icon, JTextField jtxtChar, JButton jbtnUpdateChar) {
        super(null, icon);
        this.jtxtChar = jtxtChar;
        this.jbtnUpdateChar = jbtnUpdateChar;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        jtxtChar.selectAll();
        jtxtChar.paste();
        jbtnUpdateChar.doClick();
    }
}
