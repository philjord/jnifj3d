package nif.gui;

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import nif.NifToJ3d;
import tools.ddstexture.DDSTextureLoader;
import tools.swing.DetailsFileChooser;
import utils.source.DummyTextureSource;
import utils.source.file.FileMeshSource;

public class NifLoaderTester
{
	private static Preferences prefs;

	public static void main(String[] args)
	{
		prefs = Preferences.userNodeForPackage(NifLoaderTester.class);
		String baseDir = prefs.get("NifToJ3dTester.baseDir", System.getProperty("user.dir"));

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener()
		{
			@Override
			public void directorySelected(File dir)
			{
				prefs.put("NifToJ3dTester.baseDir", dir.getPath());
				processDir(dir);
			}

			@Override
			public void fileSelected(File file)
			{
				prefs.put("NifToJ3dTester.baseDir", file.getPath());
				processFile(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif", "nif"));

		
	}

	private static void processFile(File f)
	{
		try
		{
			System.out.println("\tFile: " + f);
			//long start = System.currentTimeMillis();
			if (f.getName().endsWith(".kf"))
			{
				NifToJ3d.loadKf(f.getCanonicalPath(), new FileMeshSource());
			}
			else if (f.getName().endsWith(".dds"))
			{
				DDSTextureLoader.getTexture(f);
				DDSTextureLoader.clearCache();
			}
			else
			{
				NifToJ3d.loadHavok(f.getCanonicalPath(), new FileMeshSource());
				//NifJ3dVisRoot r = 
						NifToJ3d.loadShapes(f.getCanonicalPath(), new FileMeshSource(), new DummyTextureSource());

		//		System.out.println("modelSizes.put(\"\\" + f.getParent().substring(f.getParent().lastIndexOf("\\")) + "\\\\" + f.getName() + "\", "
		//				+ ((BoundingSphere) r.getVisualRoot().getBounds()).getRadius() + "f);");

			}

			NifToJ3d.clearCache();

			//System.out.println(" in " + (System.currentTimeMillis() - start));
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

					//only skels
					//if(!fs[i].getName().toLowerCase().contains("skeleton"))
					//	continue;

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