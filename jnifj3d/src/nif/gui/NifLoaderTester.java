package nif.gui;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileNameExtensionFilter;

import nif.BgsmSource;
import nif.NifToJ3d;
import nif.niobject.bs.BSTriShape;
import tools.ddstexture.DDSTextureLoader;
import tools.swing.DetailsFileChooser;
import utils.source.DummyTextureSource;
import utils.source.file.FileMeshSource;

public class NifLoaderTester
{
	private static final boolean NO_J3D = true;
	private static Preferences prefs;

	public static void main(String[] args)
	{
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		prefs = Preferences.userNodeForPackage(NifLoaderTester.class);
		String baseDir = prefs.get("NifToJ3dTester.baseDir", System.getProperty("user.dir"));

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener() {
			@Override
			public void directorySelected(final File dir)
			{
				prefs.put("NifToJ3dTester.baseDir", dir.getPath());
				Thread t = new Thread() {
					public void run()
					{
						processDir(dir);
						System.out.println("Processing " + dir + "complete");

					/*	System.out.println("formats");						 
						for (Entry<String, Integer> e : BSTriShape.allFormatToCount.entrySet())
						{
							System.out.println("format " + e.getKey() + " count " + e.getValue());
						}
						
						System.out.println("In the presence of 7 & 0x01 == 1");
						System.out.println("flags7ToSizeDisagreements " + BSTriShape.flags7ToSizeDisagreements);
						for (Entry<Integer, Integer> e : BSTriShape.flags7ToSize.entrySet())
						{
							System.out.println("flag " + e.getKey() + " size " + e.getValue());
						}*/
						

					}
				};
				t.start();
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
			//System.out.println("\tFile: " + f);
			// long start = System.currentTimeMillis();
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
				FileMeshSource fileMeshSource = new FileMeshSource();
				BgsmSource.setBgsmSource(fileMeshSource);

				if (NO_J3D)
				{
					NifToJ3d.loadNiObjects(f.getCanonicalPath(), fileMeshSource);
				}
				else
				{
					NifToJ3d.loadHavok(f.getCanonicalPath(), fileMeshSource);
					// NifJ3dVisRoot r =
					NifToJ3d.loadShapes(f.getCanonicalPath(), fileMeshSource, new DummyTextureSource());

					// System.out.println("modelSizes.put(\"\\" + f.getParent().substring(f.getParent().lastIndexOf("\\")) +
					// "\\\\" + f.getName() + "\", "
					// + ((BoundingSphere) r.getVisualRoot().getBounds()).getRadius() + "f);");
				}
			}

			NifToJ3d.clearCache();

			// System.out.println(" in " + (System.currentTimeMillis() - start));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void processDir(File dir)
	{
		// is this dir full of any files we want
		File[] fs = dir.listFiles();
		boolean hasFileOfInterest = false;
		for (int i = 0; i < fs.length; i++)
		{
			String fn = fs[i].getName().toLowerCase();
			if (fs[i].isFile() && (fn.endsWith(".nif") || fn.endsWith(".kf") || fn.endsWith(".dds") || fn.endsWith(".btr")))
			{
				hasFileOfInterest = true;
				break;
			}
		}

		//precombined is 124k of 196k files! and I don't yet parse the geom data anyway
		if (hasFileOfInterest && !dir.getAbsolutePath().toLowerCase().contains("precombined"))
		{
			System.out.println("Processing directory " + dir);
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

			for (int i = 0; i < fs.length; i++)
			{
				try
				{
					String fn = fs[i].getName().toLowerCase();
					if (fs[i].isFile() && (fn.endsWith(".nif") || fn.endsWith(".kf") || fn.endsWith(".dds") || fn.endsWith(".btr")))
					{

						// only skels
						// if(!fs[i].getName().toLowerCase().contains("skeleton"))
						// continue;

						processFile(fs[i]);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}

		for (int i = 0; i < fs.length; i++)
		{
			try
			{
				if (fs[i].isDirectory())
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