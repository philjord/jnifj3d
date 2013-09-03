package nif.gui;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class NifKfFileFilter extends FileFilter implements FilenameFilter
{

	@Override
	public boolean accept(File f)
	{
		return f.getName().endsWith(".nif") || f.getName().endsWith(".kf");
	}

	@Override
	public String getDescription()
	{
		return "Nif or Kf";
	}

	public boolean accept(File dir, String name)
	{
		return name.endsWith(".nif") || name.endsWith(".kf");
	}

}