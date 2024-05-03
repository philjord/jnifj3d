package utils.source.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import nif.NifFile;
import nif.NifFileReader;
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
			RandomAccessFile nifIn = null;

			try
			{
				nifIn = new RandomAccessFile(file, "r");

				ByteBuffer inputStream = nifIn.getChannel().map(MapMode.READ_ONLY, 0, file.length());

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
					if (nifIn != null)
						nifIn.close();
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

	@Override
	public InputStream getInputStreamForFile(String fileName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(fileName);
		File file = new File(parts[0] + parts[1]);

		if (file.exists())
		{
			try
			{
				return new BufferedInputStream(new FileInputStream(file));

			}
			catch (IOException e)
			{
				System.out.println("FileMeshSource:  " + fileName + " " + e + " " + e.getStackTrace()[0]);
			}

		}

		System.out.println("FileMeshSource - Problem with loading niffile: " + fileName + "||" + parts[0] + "|" + parts[1]);

		return null;
	}

	@Override
	public ByteBuffer getByteBuffer(String fileName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(fileName);
		File file = new File(parts[0] + parts[1]);

		if (file.exists())
		{
			RandomAccessFile nifIn = null;
			try
			{
				nifIn = new RandomAccessFile(file, "r");
				ByteBuffer buf = nifIn.getChannel().map(MapMode.READ_ONLY, 0, file.length());
				return buf;

			}
			catch (IOException e)
			{
				System.out.println("FileMeshSource:  " + fileName + " " + e + " " + e.getStackTrace()[0]);
			}
			finally
			{
				try
				{
					if (nifIn != null)
						nifIn.close();
				}
				catch (IOException e)
				{

					e.printStackTrace();
				}
			}

		}

		System.out.println("FileMeshSource - Problem with loading niffile: " + fileName + "||" + parts[0] + "|" + parts[1]);

		return null;
	}

}
