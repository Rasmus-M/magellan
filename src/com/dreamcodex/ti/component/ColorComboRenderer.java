package com.dreamcodex.ti.component;

import java.awt.*;
import javax.swing.*;

import com.dreamcodex.ti.util.TIGlobals;

public class ColorComboRenderer extends JLabel implements ListCellRenderer
{
    private Color[] palette = TIGlobals.TI_PALETTE_OPAQUE;

	public ColorComboRenderer()
	{
		setOpaque(true);
		setHorizontalAlignment(LEFT);
		setVerticalAlignment(CENTER);
	}

	public Component getListCellRendererComponent(JList jlist, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		int thisIndex = ((Integer)value).intValue();
		this.setText(palette == TIGlobals.TI_PALETTE_OPAQUE ? TIGlobals.TI_PALETTE_NAMES[thisIndex] : "Color " + thisIndex);
		this.setBackground(palette[thisIndex]);
		if(thisIndex == 1) { this.setForeground(TIGlobals.TI_PALETTE[15]); } else { this.setForeground(TIGlobals.TI_PALETTE[1]); }
		return this;
	}

    public void setPalette(Color[] palette) {
        this.palette = palette;
    }
}
