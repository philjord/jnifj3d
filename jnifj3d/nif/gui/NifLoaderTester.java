package nif.gui;

import java.io.File;
import java.util.prefs.Preferences;
import java3d.nativelinker.Java3dLinker2;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import nif.NifToJ3d;
import tools.texture.DDSToTextureOld;
import utils.source.DummyTextureSource;
import utils.source.file.FileMeshSource;

public class NifLoaderTester
{
	private static Preferences prefs;

	public static void main(String[] args)
	{
		new Java3dLinker2();
		prefs = Preferences.userNodeForPackage(NifLoaderTester.class);
		String baseDir = prefs.get("NifToJ3dTester.baseDir", System.getProperty("user.dir"));

		JFileChooser fc = new JFileChooser(baseDir);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		fc.showOpenDialog(new JFrame());

		if (fc.getSelectedFile() != null)
		{
			File f = fc.getSelectedFile();
			prefs.put("NifToJ3dTester.baseDir", f.getPath());
			System.out.println("Selected file: " + f);

			if (f.isDirectory())
			{
				processDir(f);
			}
			else if (f.isFile())
			{
				try
				{
					processFile(f);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			System.out.println("done");
		}
		System.exit(0);
	}

	private static void processFile(File f)
	{
		try
		{
			System.out.print("\tFile: " + f);
			long start = System.currentTimeMillis();
			if (f.getName().endsWith(".kf"))
			{
				NifToJ3d.loadKf(f.getCanonicalPath(), new FileMeshSource());
			}
			else if (f.getName().endsWith(".dds"))
			{
				DDSToTextureOld.getTexture(f);
				DDSToTextureOld.clearCache();
			}
			else
			{
				NifToJ3d.loadHavok(f.getCanonicalPath(), new FileMeshSource());
				NifToJ3d.loadShapes(f.getCanonicalPath(), new FileMeshSource(), new DummyTextureSource());
			}

			NifToJ3d.clearCache();

			System.out.println(" in " + (System.currentTimeMillis() - start));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void processDir(File dir)
	{
		System.out.println("Processing directory " + dir);
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++)
		{
			try
			{
				if (fs[i].isFile()
						&& (fs[i].getName().endsWith(".nif") || fs[i].getName().endsWith(".kf") || fs[i].getName().endsWith(".dds")))
				{
					processFile(fs[i]);
				}
				else if (fs[i].isDirectory())
				{
					processDir(fs[i]);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}