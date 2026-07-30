package com.dreamcodex.ti.component;

import javax.swing.Action;
import java.awt.event.MouseEvent;

/**
 * Created with IntelliJ IDEA.
 * User: RasmusM
 * Date: 04-11-13
 * Time: 10:30
 */
public class TripleClickButton extends DualClickButton {

    private final Action dblClickAction;

    public TripleClickButton(String textLabel, Action leftClickAction, Action rightClickAction, Action dblClickAction) {
        super(textLabel, leftClickAction, rightClickAction);
        this.dblClickAction = dblClickAction;
    }

    public void mouseClicked(MouseEvent me) {
        if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
            fire(dblClickAction, me);
        }
    }
}
