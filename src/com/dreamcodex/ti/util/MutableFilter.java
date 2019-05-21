package com.dreamcodex.ti.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/** MutableFilter
  * Class for providing JFileChooser with a FileFilter
  *
  * @author Howard Kistler
  * @version 1.0
  *
  * REQUIREMENTS
  * Java 1 (JDK 1.3 or higher)
  * Swing Library
  */

public class MutableFilter extends FileFilter
{
	private String[] acceptableExtensions;
	private String descriptor;

	public MutableFilter(String[] exts, String desc)
	{
		acceptableExtensions = exts;
		StringBuffer strbDesc = new StringBuffer(desc + " (");
		for(int i = 0; i < acceptableExtensions.length; i++)
		{
			if(i > 0) { strbDesc.append(", "); }
			strbDesc.append("*." + acceptableExtensions[i]);
		}
		strbDesc.append(")");
		descriptor = strbDesc.toString();
	}

	public boolean accept(File file)
	{
		if(file.isDirectory())
		{
			return true;
		}
		String fileName = file.getName();
		String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
		if(fileExt != null)
		{
			for(int i = 0; i < acceptableExtensions.length; i++)
			{
				if(fileExt.equals(acceptableExtensions[i]))
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			return false;
		}
	}

	public String getDescription()
	{
		return descriptor;
	}
}

