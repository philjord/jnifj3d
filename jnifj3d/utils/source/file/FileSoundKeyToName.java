package utils.source.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import utils.source.SoundKeyToName;

public class FileSoundKeyToName implements SoundKeyToName
{
	public static HashMap<String, String> soundNameToFile = null;

	public String getFileName(String soundName)
	{
		if (soundNameToFile == null)
		{
			loadSoundNames();
		}

		String ret = soundNameToFile.get(soundName);
		if (ret != null)
		{
			File snf = new File(ret);
			if (snf.exists())
			{
				if (!snf.isDirectory())
				{
					return ret;
				}
				else
				{
					// grab a random file from it
					String[] files = snf.list();
					if (files.length > 0)
					{
						int idx = new Random().nextInt(files.length);
						String firstFile = snf.list()[idx];
						return snf.getAbsolutePath() + "/" + firstFile;
					}

				}

			}

		}		
		return null;
	}

	private static void loadSoundNames()
	{
		Set<String> doneDirs = new HashSet<String>();
		soundNameToFile = new HashMap<String, String>();

		for (String mediaRoot : FileMediaRoots.getMediaRoots())
		{
			if (!doneDirs.contains(mediaRoot))
			{
				File dir = new File(mediaRoot);
				if (dir.exists() && dir.isDirectory())
				{
					for (String fileName : dir.list())
					{
						if (fileName.toLowerCase().endsWith("sounds.txt"))
						{
							loadSoundNameFile(dir.getAbsolutePath(), fileName);
						}
					}
				}
				doneDirs.add(mediaRoot);
			}
		}
	}

	private static void loadSoundNameFile(String dir, String fileName)
	{
		File f = new File(dir + "/" + fileName);
		if (f.exists())
		{
			try
			{
				BufferedReader fileReader = new BufferedReader(new FileReader(f));
				String line = fileReader.readLine();

				while (line != null)
				{
					String[] parts = line.split(":");

					//0 is ordernum
					//1 is formid
					//2 is key
					//3 is file (or folder), might be null					
					if (parts.length > 3)
					{
						soundNameToFile.put(parts[2], dir + "/sound/" + parts[3]);
					}
					line = fileReader.readLine();
				}
				
				fileReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}
}
