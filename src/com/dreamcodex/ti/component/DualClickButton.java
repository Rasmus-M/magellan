package com.dreamcodex.ti.component;

import com.dreamcodex.ti.util.Globals;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;

public class DualClickButton extends JButton implements MouseListener
{
	protected String leftClickCommand;
	protected String rightClickCommand;
	protected int modifiers;
	protected ActionListener parent;

	public DualClickButton(String textLabel, String leftCmd, String rightCmd, ActionListener aparent)
	{
		super(textLabel);
		leftClickCommand = leftCmd;
		rightClickCommand = rightCmd;
		parent = aparent;
		this.addMouseListener(this);
		this.addActionListener(parent);
	}

	public void mousePressed(MouseEvent me)
	{
        if (me.getButton() == MouseEvent.BUTTON3)
		{
            this.setActionCommand(rightClickCommand);
			this.modifiers = me.getModifiers();
		}
		else
		{
			this.setActionCommand(leftClickCommand);
			this.modifiers = 0;
		}
	}

	public void mouseReleased(MouseEvent me)
	{
		ActionEvent aeInit = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, this.getActionCommand(), modifiers);
		parent.actionPerformed(aeInit);
	}

	public void mouseClicked(MouseEvent me) {}
	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent me) {}
}
