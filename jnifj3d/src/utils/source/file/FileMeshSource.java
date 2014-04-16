package utils.source.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nif.NifFile;
import nif.NifFileReader;
import utils.ESConfig;
import utils.source.MeshSource;

public class FileMeshSource implements MeshSource
{
	public FileMeshSource()
	{
	}

	public boolean nifFileExists(String nifName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(nifName);
		File file = new File(parts[0] + parts[1]);
		return file.exists();
	}

	@Override
	public NifFile getNifFile(String nifName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(nifName);
		File file = new File(parts[0] + parts[1]);

		NifFile nifFile = null;
		if (file.exists())
		{
			String filename = file.getAbsolutePath();
			InputStream inputStream = null;
			try
			{
				if (filename.toLowerCase().contains("skyrim"))
				{
					ESConfig.HAVOK_TO_METERS_SCALE = ESConfig.SKYRIM_HAVOK_TO_METERS_SCALE;
				}
				else
				{
					ESConfig.HAVOK_TO_METERS_SCALE = ESConfig.PRE_SKYRIM_HAVOK_TO_METERS_SCALE;
				}

				inputStream = new BufferedInputStream(new FileInputStream(file));

				nifFile = NifFileReader.readNif(filename, inputStream);
			}
			catch (IOException e)
			{
				System.out.println("FileMeshSource:  " + nifName + " " + e + " " + e.getStackTrace()[0]);
			}
			finally
			{
				try
				{
					if (inputStream != null)
						inputStream.close();
				}
				catch (IOException e)
				{

					e.printStackTrace();
				}
			}
		}
		if (nifFile == null)
		{
			System.out.println("FileMeshSource - Problem with loading niffile: " + nifName + "||" + parts[0] + "|" + parts[1]);
		}

		return nifFile;
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
