package com.dreamcodex.ti.iface;

import javax.swing.Icon;

public abstract interface IconProvider
{
	public Icon getIconForChar(int i);
    public Icon getIconForSprite(int i);
}
