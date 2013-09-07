package nif.gui;

import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import java3d.nativelinker.Java3dLinker2;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nif.NifFile;
import nif.NifFileReader;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.character.KfJ3dRoot;
import nif.character.NifJ3dSkeletonRoot;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiControllerSequence;
import tools.swing.TitledJFileChooser;
import utils.source.file.FileMeshSource;
import utils.source.file.FileTextureSource;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class KfDisplayTester
{
	private static Preferences prefs;

	public static void main(String[] args)
	{
		new Java3dLinker2();
		prefs = Preferences.userNodeForPackage(KfDisplayTester.class);

		setUpUniverseAndCanvas(false);
		try
		{
			// pick the nif model
			TitledJFileChooser skeletonFc = new TitledJFileChooser(prefs.get("skeletonNifModelFile", System.getProperty("user.dir")));
			skeletonFc.setDialogTitle("Select Skeleton");
			skeletonFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			skeletonFc.setMultiSelectionEnabled(false);
			skeletonFc.setFileFilter(new FileNameExtensionFilter("nif files", "nif"));
			skeletonFc.showOpenDialog(new JFrame());

			if (skeletonFc.getSelectedFile() != null)
			{
				String skeletonNifModelFile = skeletonFc.getSelectedFile().getCanonicalPath();
				prefs.put("skeletonNifModelFile", skeletonNifModelFile);
				System.out.println("Selected skeleton file: " + skeletonNifModelFile);

				TitledJFileChooser skinFc = new TitledJFileChooser(prefs.get("skinNifModelFile", skeletonNifModelFile));
				skinFc.setDialogTitle("Select Skin(s)");
				skinFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				skinFc.setMultiSelectionEnabled(true);
				skinFc.setFileFilter(new FileNameExtensionFilter("nif files", "nif"));
				skinFc.showOpenDialog(new JFrame());

				String[] skinNifFiles = new String[0];

				if (skinFc.getSelectedFile() != null)
				{
					File[] skinNifModelFiles = skinFc.getSelectedFiles();
					prefs.put("skinNifModelFile", skinNifModelFiles[0].getCanonicalPath());

					System.out.println("Selected skin file[0]: " + skinNifModelFiles[0]);

					skinNifFiles = new String[skinNifModelFiles.length];
					int i = 0;
					for (File skinNifModelFile : skinNifModelFiles)
					{
						skinNifFiles[i] = skinNifModelFile.getCanonicalPath();
						i++;
					}

					TitledJFileChooser kfFc = new TitledJFileChooser(prefs.get("kfModelFile", skeletonNifModelFile));
					kfFc.setDialogTitle("Select KF(s)");
					kfFc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					kfFc.setMultiSelectionEnabled(false);
					kfFc.setFileFilter(new FileNameExtensionFilter("kf files", "kf"));

					kfFc.showOpenDialog(new JFrame());

					if (kfFc.getSelectedFile() != null)
					{
						File kfModelFile = kfFc.getSelectedFile();
						prefs.put("kfModelFile", kfModelFile.getCanonicalPath());

						if (kfModelFile.isDirectory())
						{

							while (true)
							{
								processDir(skeletonNifModelFile, skinNifFiles, kfModelFile);
							}
						}
						else if (kfModelFile.isFile())
						{
							try
							{
								System.out.println("\tFile: " + kfModelFile);
								NifFile kfFile = NifFileReader.readNif(kfModelFile);

								while (true)
								{
									display(skeletonNifModelFile, skinNifFiles, kfFile);
								}

							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}

							System.out.println("done");
						}

					}
					else
					{
						//no kf picked just display the skin 
						display(skeletonNifModelFile, skinNifFiles);
					}
				}
			}
			else
			{
				System.exit(0);
			}

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void processDir(String skeletonNifFile, String[] skinNifFiles, File dir)
	{
		System.out.println("Processing directory " + dir);
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++)
		{
			try
			{
				if (fs[i].isFile() && fs[i].getName().endsWith(".kf"))
				{
					System.out.println("\tFile: " + fs[i]);
					NifFile kfFile = NifFileReader.readNif(fs[i]);
					display(skeletonNifFile, skinNifFiles, kfFile);
				}
				else if (fs[i].isDirectory())
				{
					processDir(skeletonNifFile, skinNifFiles, fs[i]);
				}

			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private static void display(String skeletonNifFile, String[] skinNifFiles, NifFile kfFile)
	{
		transformGroup.removeAllChildren();

		if (kfFile.blocks.root() instanceof NiControllerSequence)
		{
			BranchGroup bg = new BranchGroup();
			bg.setCapability(BranchGroup.ALLOW_DETACH);

			NifJ3dSkeletonRoot.showBoneMarkers = true;

			// create a skeleton from the nif file
			NifJ3dSkeletonRoot nifJ3dSkeletonRoot = new NifJ3dSkeletonRoot(skeletonNifFile, new FileMeshSource());
			// now add the scene root so the bones are live and can move etc
			bg.addChild(nifJ3dSkeletonRoot);

			for (String skinNifFile : skinNifFiles)
			{
				NifJ3dVisRoot skin = NifToJ3d.loadShapes(skinNifFile, new FileMeshSource(), new FileTextureSource());

				// create skins form the skeleton and skin nif
				ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(skin.getNiToJ3dData(), nifJ3dSkeletonRoot);

				// add the skins to the scene
				for (J3dNiSkinInstance j3dNiSkinInstance : skins)
				{
					bg.addChild(j3dNiSkinInstance);
				}
			}

			// make the kf file root 
			NiToJ3dData niToJ3dData = new NiToJ3dData(kfFile.blocks);
			KfJ3dRoot kfJ3dRoot = new KfJ3dRoot((NiControllerSequence) niToJ3dData.root(), niToJ3dData);
			kfJ3dRoot.setAnimatedSkeleton(nifJ3dSkeletonRoot.getAllBonesInSkeleton());

			// now add the root to the scene so the controller sequence is live
			bg.addChild(kfJ3dRoot);

			transformGroup.addChild(bg);

			kfJ3dRoot.getJ3dNiControllerSequence().fireSequenceOnce();
			try
			{
				Thread.sleep(kfJ3dRoot.getJ3dNiControllerSequence().getLengthMS());
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

		}
		else
		{
			System.out.println("if(kfFile.blocks.get(0] instanceof NiControllerSequence) is not true!");
		}

	}

	/**
	 * If no kf needs to be shown then this just sets up the skin and skeleton
	 * @param skeletonNifFile
	 * @param skinNifFiles
	 */
	private static void display(String skeletonNifFile, String[] skinNifFiles)
	{
		transformGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;

		// create a skeleton from the nif file
		NifJ3dSkeletonRoot nifJ3dSkeletonRoot = new NifJ3dSkeletonRoot(skeletonNifFile, new FileMeshSource());
		// now add the scene root so the bones are live and can move etc
		bg.addChild(nifJ3dSkeletonRoot);

		for (String skinNifFile : skinNifFiles)
		{
			NifJ3dVisRoot skin = NifToJ3d.loadShapes(skinNifFile, new FileMeshSource(), new FileTextureSource());

			// create skins form the skeleton and skin nif
			ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(skin.getNiToJ3dData(), nifJ3dSkeletonRoot);

			// add the skins to the scene
			for (J3dNiSkinInstance j3dNiSkinInstance : skins)
			{
				bg.addChild(j3dNiSkinInstance);
			}
		}
		transformGroup.addChild(bg);
	}

	private static SimpleUniverse universe;

	private static TransformGroup transformGroup;

	private static void setUpUniverseAndCanvas(boolean autoSpin)
	{
		universe = new SimpleUniverse();
		transformGroup = new TransformGroup();

		Window win = SwingUtilities.getWindowAncestor(universe.getCanvas());
		win.setSize(1000, 1000);
		((JFrame) win).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		transformGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		transformGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it
		Color3f alColor = new Color3f(1f, 1f, 1f);
		AmbientLight ambLight = new AmbientLight(true, alColor);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		DirectionalLight dirLight1 = new DirectionalLight(true, alColor, new Vector3f(0f, -1f, 0f));
		dirLight1.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		Vector3f v = new Vector3f(0f, 1f, 1f);
		v.normalize();
		DirectionalLight dirLight2 = new DirectionalLight(true, alColor, v);
		dirLight2.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup lbg = new BranchGroup();
		lbg.addChild(ambLight);
		lbg.addChild(dirLight1);
		lbg.addChild(dirLight2);
		universe.addBranchGraph(lbg);

		BranchGroup bg = new BranchGroup();
		bg.addChild(transformGroup);

		if (autoSpin)
		{
			bg.addChild(new SpinTransform(transformGroup));
		}
		else
		{
			MouseRotate mr = new MouseRotate(transformGroup);
			mr.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			mr.setEnable(true);
			bg.addChild(mr);
		}

		universe.addBranchGraph(bg);

		setEye();

		universe.getViewer().getView().setBackClipDistance(5000);

		universe.getCanvas().addMouseWheelListener(new MouseWheelListener()
		{
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (e.getWheelRotation() < 0)
				{
					zoomIn();
				}
				else
				{
					zoomOut();
				}
			}
		});

		universe.getCanvas().addKeyListener(new KeyAdapter()
		{

			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_W)
				{
					eye.z = eye.z - 10;
					center.z = center.z - 10;
				}
				else if (e.getKeyCode() == KeyEvent.VK_S)
				{
					eye.z = eye.z + 10;
					center.z = center.z + 10;
				}
				else if (e.getKeyCode() == KeyEvent.VK_A)
				{
					eye.x = eye.x - 10;
					center.x = center.x - 10;
				}
				else if (e.getKeyCode() == KeyEvent.VK_D)
				{
					eye.x = eye.x + 10;
					center.x = center.x + 10;
				}
				else if (e.getKeyCode() == KeyEvent.VK_E)
				{
					center.z = center.z - 3;
					center.y = -center.z / 20;
				}
				else if (e.getKeyCode() == KeyEvent.VK_C)
				{
					center.z = center.z + 3;
					center.y = -center.z / 20;
				}

				setEye();
			}

		});

	}

	static Point3d eye = new Point3d(0, 10, 0);

	static Point3d center = new Point3d(0, 0, 0);

	private static void zoomOut()
	{
		eye.y = eye.y * 1.1d;
		setEye();
	}

	private static void zoomIn()
	{
		eye.y = eye.y * 0.9d;
		setEye();

	}

	private static void setEye()
	{
		TransformGroup tg = universe.getViewingPlatform().getViewPlatformTransform();
		Transform3D t = new Transform3D();
		t.lookAt(eye, center, new Vector3d(0, 0, -1));
		t.invert();
		tg.setTransform(t);
	}

	private static class SpinTransform extends Behavior
	{
		private TransformGroup trans;

		//Calculations for frame duration timing, 
		//used between successive calls to process 
		private long previousFrameEndTime;

		private double currentRot = 0;

		private WakeupCondition FPSCriterion = new WakeupOnElapsedFrames(0, false);

		public SpinTransform(TransformGroup trans)
		{
			this.trans = trans;
			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			setEnable(true);
		}

		public void initialize()
		{
			wakeupOn(FPSCriterion);
		}

		@SuppressWarnings(
		{ "unchecked", "rawtypes" })
		public void processStimulus(Enumeration criteria)
		{
			process();
			wakeupOn(FPSCriterion);
		}

		private void process()
		{
			long timeNow = System.currentTimeMillis();
			long frameDuration = timeNow - previousFrameEndTime;
			currentRot += frameDuration / 1000d;
			Transform3D t = new Transform3D();
			t.setRotation(new AxisAngle4d(0, 1, 0, currentRot));
			trans.setTransform(t);
			// record when we last thought about movement
			previousFrameEndTime = timeNow;
		}

	}
}