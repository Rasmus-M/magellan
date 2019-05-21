package com.dreamcodex.ti.component;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ImagePreview extends JComponent implements PropertyChangeListener
{
	private static final int previewWidth  = 8*8;
	private static final int previewHeight = 8*32;

	private ImageIcon imageThumb = null;
	private File imageFile = null;

	public ImagePreview(JFileChooser parent)
	{
		setPreferredSize(new Dimension(previewWidth , previewHeight));
		parent.addPropertyChangeListener(this);
	}

	public void loadImage()
	{
		if(imageFile == null)
		{
			imageThumb = null;
			return;
		}
		imageThumb = new ImageIcon(imageFile.getPath());

		if(imageThumb.getIconHeight() < previewHeight && imageThumb.getIconWidth() < previewWidth)
		{
			return;
		}
		int	w = previewWidth;
		int	h = previewHeight;
		if(imageThumb.getIconHeight() > imageThumb.getIconWidth())
		{
			w = -1;
		}
		else
		{
			h = -1;
		}
		imageThumb = new ImageIcon(imageThumb.getImage().getScaledInstance(w, h, Image.SCALE_DEFAULT));
	}

	public void propertyChange(PropertyChangeEvent pce)
	{
		if(pce.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY))
		{
			imageFile = (File)(pce.getNewValue());
			if(this.isShowing())
			{
				loadImage();
				repaint();
			}
		}
	}

	public void paintComponent(Graphics g)
	{
		if(imageThumb == null)
		{
			loadImage();
		}
		if(imageThumb == null)
		{
			return;
		}
		int	x = (this.getWidth() - imageThumb.getIconWidth()) / 2;
		int	y = (this.getHeight() - imageThumb.getIconHeight()) / 2;
		if(y < 0)
		{
			y = 0;
		}
		if(x < 5)
		{
			x = 5;
		}
		imageThumb.paintIcon(this, g, x, y);
	}
}

