package com.dreamcodex.ti.actions.clipboard;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CopySpriteAction extends AbstractAction {

    private final JTextField jtxtSprite;

    public CopySpriteAction(Icon icon, JTextField jtxtSprite) {
        super(null, icon);
        this.jtxtSprite = jtxtSprite;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        jtxtSprite.selectAll();
        jtxtSprite.copy();
    }
}
