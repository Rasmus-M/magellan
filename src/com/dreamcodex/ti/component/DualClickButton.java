package com.dreamcodex.ti.component;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Action;
import javax.swing.JButton;

/**
 * A button that fires one action on a left click and another on a right click.
 * The actions are invoked directly from the mouse listener rather than through
 * the button's own ActionListeners, since Swing does not fire an action event
 * for the right mouse button.
 */
public class DualClickButton extends JButton implements MouseListener
{
	protected final Action leftClickAction;
	protected final Action rightClickAction;

	public DualClickButton(String textLabel, Action leftClickAction, Action rightClickAction)
	{
		super(textLabel);
		this.leftClickAction = leftClickAction;
		this.rightClickAction = rightClickAction;
		this.addMouseListener(this);
	}

	protected void fire(Action action, MouseEvent me)
	{
		if (action != null)
		{
			action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, me.getWhen(), me.getModifiers()));
		}
	}

	public void mouseReleased(MouseEvent me)
	{
		fire(me.getButton() == MouseEvent.BUTTON3 ? rightClickAction : leftClickAction, me);
	}

	public void mousePressed(MouseEvent me) {}
	public void mouseClicked(MouseEvent me) {}
	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent me) {}
}
