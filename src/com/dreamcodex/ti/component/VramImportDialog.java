package com.dreamcodex.ti.component;

import java.beans.*;
import javax.swing.*;

import com.dreamcodex.ti.util.TIGlobals;

public class VramImportDialog extends JDialog implements PropertyChangeListener
{
	private String OK_TEXT = "Import";
	private String CANCEL_TEXT = "Cancel";

	private JTextField jtxtCharDataOffset;
	private JTextField jtxtMapDataOffset;
	private JTextField jtxtColorDataOffset;
    private JTextField jtxtSpriteDataOffset;
    private JTextField jtxtSpriteAttrDataOffset;
    private JCheckBox jchkBitmapMode;
	private JCheckBox  jchkHexValues;
	private boolean hexValues = false;
	private boolean clickedOkay = false;

	public VramImportDialog(JFrame parent, boolean hexValuesSet)
	{
		super(parent, "Import Settings", true);
		jtxtCharDataOffset = new JTextField();
		jtxtMapDataOffset = new JTextField();
		jtxtColorDataOffset = new JTextField();
        jtxtSpriteDataOffset = new JTextField();
        jtxtSpriteAttrDataOffset = new JTextField();
        jchkBitmapMode = new JCheckBox("Bitmap Mode", hexValuesSet);
        jchkHexValues = new JCheckBox("Use Hexadecimal", hexValuesSet);

		Object[] objForm = new Object[8];
		int objCount = 0;
		objForm[objCount] = new JLabel("Character Data Block Start"); objCount++;
		objForm[objCount] = jtxtCharDataOffset; objCount++;
		objForm[objCount] = new JLabel("Map Data Block Start"); objCount++;
		objForm[objCount] = jtxtMapDataOffset; objCount++;
		objForm[objCount] = new JLabel("Color Data Block Start"); objCount++;
		objForm[objCount] = jtxtColorDataOffset; objCount++;
        objForm[objCount] = new JLabel("Sprite Data Block Start"); objCount++;
        objForm[objCount] = jtxtSpriteDataOffset; objCount++;
        objForm[objCount] = new JLabel("Sprite Attribute Data Block Start"); objCount++;
        objForm[objCount] = jtxtSpriteAttrDataOffset; objCount++;
        objForm[objCount] = jchkHexValues; objCount++;
        objForm[objCount] = jchkBitmapMode; objCount++;
        Object[] objButtons = { OK_TEXT, CANCEL_TEXT };

		JOptionPane joptMain = new JOptionPane(objForm, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, objButtons, objButtons[0]);
		joptMain.addPropertyChangeListener(this);
		this.setContentPane(joptMain);

		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(this);
		this.setVisible(true);
	}

	public VramImportDialog(JFrame parent)
	{
		this(parent, false);
	}

	public int getCharDataOffset()  { return Integer.parseInt(jtxtCharDataOffset.getText(), (jchkHexValues.isSelected() ? 16 : 10)); }
	public int getMapDataOffset()   { return Integer.parseInt(jtxtMapDataOffset.getText(), (jchkHexValues.isSelected() ? 16 : 10)); }
	public int getColorDataOffset() { return Integer.parseInt(jtxtColorDataOffset.getText(), (jchkHexValues.isSelected() ? 16 : 10)); }
    public int getSpriteDataOffset()  { return Integer.parseInt(jtxtSpriteDataOffset.getText(), (jchkHexValues.isSelected() ? 16 : 10)); }
    public int getSpriteAttrDataOffset()  { return Integer.parseInt(jtxtSpriteAttrDataOffset.getText(), (jchkHexValues.isSelected() ? 16 : 10)); }
    public boolean isBitmapMode()   { return jchkBitmapMode.isSelected(); }

	public boolean isOkay() { return clickedOkay; }

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

