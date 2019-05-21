package com.dreamcodex.ti.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

/**
 * Created with IntelliJ IDEA.
 * User: RasmusM
 * Date: 04-11-13
 * Time: 10:30
 */
public class TripleClickButton extends DualClickButton {

    private String dblClick;

    public TripleClickButton(String textLabel, String leftCmd, String rightCmd, String dblClick, ActionListener actionListener) {
        super(textLabel, leftCmd, rightCmd, actionListener);
        this.dblClick = dblClick;
    }

    public void mouseClicked(MouseEvent me) {
        if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2 && dblClick != null) {
            ActionEvent aeInit = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, dblClick);
            parent.actionPerformed(aeInit);
        }
    }
}
