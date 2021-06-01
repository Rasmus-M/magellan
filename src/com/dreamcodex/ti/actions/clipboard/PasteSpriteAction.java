package com.dreamcodex.ti.actions.clipboard;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PasteSpriteAction extends AbstractAction {

    private final JTextField jtxtSprite;
    private final JButton jbtnUpdateSprite;

    public PasteSpriteAction(Icon icon, JTextField jtxtSprite, JButton jbtnUpdateSprite) {
        super(null, icon);
        this.jtxtSprite = jtxtSprite;
        this.jbtnUpdateSprite = jbtnUpdateSprite;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        jtxtSprite.selectAll();
        jtxtSprite.paste();
        jbtnUpdateSprite.doClick();
    }
}
