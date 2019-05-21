package com.dreamcodex.ti.component;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

import com.dreamcodex.ti.iface.IconProvider;
import com.dreamcodex.ti.util.NamedIcon;
import com.dreamcodex.ti.util.TIGlobals;

public class CharacterSwapDialog extends JDialog implements ItemListener, PropertyChangeListener
{
	private String OK_TEXT = "OK";
	private String CANCEL_TEXT = "Cancel";

	private JComboBox jcmbBaseChar;
	private JComboBox jcmbSwapChar;
	// private JLabel jlblBaseChar;
	// private JLabel jlblSwapChar;
	private JCheckBox jchkSwapChars;
	private JCheckBox jchkSwapImgs;
	private JCheckBox jchkAllMaps;
    private JSpinner jspnRepeatCount;
	private boolean clickedOkay = false;
	private int minChar = 0;
	private int maxChar = 0;

    public CharacterSwapDialog(JFrame parent, IconProvider ip, boolean setSwapBoth, boolean setSwapImgs, boolean setAllMaps, int minc, int maxc, int baseChar)
	{
		super(parent, "Swap/Replace Characters", true);
		minChar = minc;
		maxChar = maxc;
		jcmbBaseChar = new JComboBox(); jcmbBaseChar.addItemListener(this);
        jcmbBaseChar.setRenderer(new CharListCellRenderer());
		jcmbSwapChar = new JComboBox(); jcmbSwapChar.addItemListener(this);
        jcmbSwapChar.setRenderer(new CharListCellRenderer());
		// jlblBaseChar = new JLabel(); jlblBaseChar.setHorizontalAlignment(JLabel.CENTER);
		// jlblSwapChar = new JLabel(); jlblSwapChar.setHorizontalAlignment(JLabel.CENTER);
		jchkSwapImgs = new JCheckBox("Swap Images/Patterns", setSwapImgs);
        jchkSwapChars = new JCheckBox("Swap Characters on Maps", setSwapBoth);
		jchkAllMaps = new JCheckBox("On All Maps", setAllMaps);
		int chardex = 0;
		for (int i = minChar; i <= maxChar; i++)
		{
            Icon icon = ip.getIconForChar(i);
            chardex = i - TIGlobals.CHARMAPSTART;
            NamedIcon namedIcon = new NamedIcon(icon, (icon == null && chardex >= 0 && chardex < TIGlobals.CHARMAP.length ? TIGlobals.CHARMAP[chardex] + " " : "") + Integer.toString(i));
            jcmbBaseChar.addItem(namedIcon);
            jcmbSwapChar.addItem(namedIcon);
//
//			if(chardex >= 0 && chardex < TIGlobals.CHARMAP.length)
//			{
//				jcmbBaseChar.addItem(new IconChar(ip.getIconForObject(i), "x" + i + " " + TIGlobals.CHARMAP[chardex]));
//				jcmbSwapChar.addItem(new IconChar(ip.getIconForObject(i), "x" + i + " " + TIGlobals.CHARMAP[chardex]));
//			}
//			else
//			{
//				jcmbBaseChar.addItem(new IconChar(ip.getIconForObject(i), "x" + i));
//				jcmbSwapChar.addItem(new IconChar(ip.getIconForObject(i), "x" + i));
//			}
		}
		jcmbBaseChar.setSelectedIndex(baseChar - minChar);
		jcmbSwapChar.setSelectedIndex(baseChar - minChar);
        jspnRepeatCount = new JSpinner(new SpinnerNumberModel(0, 0, 128, 1));

		JPanel jpnlForm = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.insets = new Insets(1, 1, 1, 1);
		gbc.ipadx = 2;
		gbc.ipady = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; jpnlForm.add(new JLabel("Replace This"), gbc);
		gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 2; jpnlForm.add(jcmbBaseChar, gbc);
		// gbc.gridx = 3; gbc.gridy = 1; jpnlForm.add(jlblBaseChar, gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1; jpnlForm.add(new JLabel("With This"), gbc);
		gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2; jpnlForm.add(jcmbSwapChar, gbc);
		// gbc.gridx = 3; gbc.gridy = 2; jpnlForm.add(jlblSwapChar, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; jpnlForm.add(jchkSwapImgs, gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 3; jpnlForm.add(jchkSwapChars, gbc);
		gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 3; jpnlForm.add(jchkAllMaps, gbc);
        gbc.ipady = 8;
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 1; jpnlForm.add(new JLabel("Repeat for next"), gbc);
        gbc.gridx = 2; gbc.gridy = 6; gbc.gridwidth = 1; jpnlForm.add(jspnRepeatCount, gbc);
        gbc.gridx = 3; gbc.gridy = 6; gbc.gridwidth = 1; jpnlForm.add(new JLabel("characters"), gbc);

        Object[] objForm = new Object[1];
		objForm[0] = jpnlForm;
		Object[] objButtons = { OK_TEXT, CANCEL_TEXT };

		JOptionPane joptMain = new JOptionPane(objForm, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, objButtons, objButtons[0]);
		joptMain.addPropertyChangeListener(this);
		this.setContentPane(joptMain);

		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(this);
		this.setVisible(true);
	}

	public int getBaseChar() { return jcmbBaseChar.getSelectedIndex() + minChar; }
	public int getSwapChar() { return jcmbSwapChar.getSelectedIndex() + minChar; }
	public boolean doSwapChars() { return jchkSwapChars.isSelected(); }
	public boolean doSwapImages() { return jchkSwapImgs.isSelected(); }
	public boolean doAllMaps() { return jchkAllMaps.isSelected(); }
    public int getRepeatCount() { return (Integer) jspnRepeatCount.getValue(); }
	public boolean isOkay() { return clickedOkay; }

	/* ItemListener method */
	public void itemStateChanged(ItemEvent ie)
	{
		if (ie.getSource().equals(jcmbBaseChar)) {
			// jlblBaseChar.setIcon(iconProv.getIconForObject(new Integer(getBaseChar())));
		}
		else if (ie.getSource().equals(jcmbSwapChar)) {
			// jlblSwapChar.setIcon(iconProv.getIconForObject(new Integer(getSwapChar())));
		}
	}

	/* PropertyChangeListener method */
	public void propertyChange(PropertyChangeEvent pce)
	{
		if(pce != null && pce.getNewValue() != null)
		{
			if(pce.getNewValue().equals(OK_TEXT))
			{
				clickedOkay = true;
				this.setVisible(false);
			}
			else if(pce.getNewValue().equals(CANCEL_TEXT))
			{
				clickedOkay = false;
				this.setVisible(false);
			}
		}
	}
}

