package utils.source.file;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.MediaContainer;
import javax.media.j3d.SoundException;

import utils.source.SoundSource;

public class FileSoundSource implements SoundSource
{

	public FileSoundSource()
	{
	}

	@Override
	public InputStream getInputStream(String mediaName)
	{
		//TODO:
		return null;
	}

	@Override
	public MediaContainer getMediaContainer(String mediaName)
	{
		String soundFile = new FileSoundKeyToName().getFileName(mediaName);
		if (soundFile != null)
		{
			try
			{
				return new MediaContainer("file:" + soundFile);
			}
			catch (SoundException e)
			{
				System.out.println("Error get sound key: " + mediaName + " file: " + soundFile);
				e.printStackTrace();
			}
		}
		else
		{
			//try the key itself
			try
			{
				String[] parts = FileMediaRoots.splitOffMediaRoot(mediaName);
				if (new File(parts[0] + parts[1]).exists())
					return new MediaContainer("file:" + parts[0] + parts[1]);
			}
			catch (SoundException e)
			{
				System.out.println("Error get sound key: " + mediaName + " file: " + soundFile);
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public List<String> getFilesInFolder(String folderName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(folderName);

		ArrayList<String> ret = new ArrayList<String>();
		for (File f : new File(parts[0] + parts[1]).listFiles())
		{
			ret.add(f.getAbsolutePath());
		}

		return ret;
	}
}
