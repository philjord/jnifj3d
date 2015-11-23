package nif.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import nif.NifFile;
import nif.NifFileReader;
import nif.NifToJ3d;
import nif.character.KfJ3dRoot;
import nif.character.NifJ3dSkeletonRoot;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiControllerSequence;
import tools.swing.TitledJFileChooser;
import utils.source.file.FileMeshSource;

public class KfLoaderTester
{
	private static Preferences prefs;

	private static NifJ3dSkeletonRoot nifJ3dSkeletonRoot;

	public static void main(String[] args)
	{
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		prefs = Preferences.userNodeForPackage(KfLoaderTester.class);
		String baseDir = prefs.get("KfLoaderTester.baseDir", System.getProperty("user.dir"));

		// pick the nif model
		TitledJFileChooser skeletonFc = new TitledJFileChooser(prefs.get("skeletonNifModelFile", System.getProperty("user.dir")));
		skeletonFc.setDialogTitle("Select Skeleton");
		skeletonFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		skeletonFc.setMultiSelectionEnabled(false);
		skeletonFc.setFileFilter(new FileNameExtensionFilter("nif files", "nif"));
		skeletonFc.showOpenDialog(new JFrame());

		if (skeletonFc.getSelectedFile() != null)
		{
			try
			{
				String skeletonNifModelFile = skeletonFc.getSelectedFile().getCanonicalPath();
				prefs.put("skeletonNifModelFile", skeletonNifModelFile);
				System.out.println("Selected skeleton file: " + skeletonNifModelFile);

				NifJ3dSkeletonRoot.showBoneMarkers = true;

				// create a skeleton from the nif file
				nifJ3dSkeletonRoot = new NifJ3dSkeletonRoot(skeletonNifModelFile, new FileMeshSource());

				JFileChooser fc = new JFileChooser(baseDir);
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

				fc.showOpenDialog(new JFrame());

				if (fc.getSelectedFile() != null)
				{
					File f = fc.getSelectedFile();
					prefs.put("KfLoaderTester.baseDir", f.getPath());
					System.out.println("Selected file: " + f);

					if (f.isDirectory())
					{
						processDir(f);
					}
					else if (f.isFile())
					{

						processFile(f);

					}

					System.out.println("done");
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		System.exit(0);
	}

	private static void processFile(File f)
	{
		try
		{
			System.out.println("\tFile: " + f);

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(f), (int) (f.length() + 10));
			NifFile kfFile = NifFileReader.readNif(f.getCanonicalPath(), in);
			in.close();
			// make the kf file root 
			NiToJ3dData niToJ3dData = new NiToJ3dData(kfFile.blocks);
			KfJ3dRoot kfJ3dRoot = new KfJ3dRoot((NiControllerSequence) niToJ3dData.root(), niToJ3dData);
			kfJ3dRoot.setAnimatedSkeleton(nifJ3dSkeletonRoot.getAllBonesInSkeleton(), null);

			/*	addChild(kfJ3dRoot);


				kfJ3dRoot.getJ3dNiControllerSequence().fireSequence();
				try
				{
					Thread.sleep(kfJ3dRoot.getJ3dNiControllerSequence().getLength());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}*/

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
				if (fs[i].isFile() && (fs[i].getName().endsWith(".nif") || fs[i].getName().endsWith(".kf")))
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