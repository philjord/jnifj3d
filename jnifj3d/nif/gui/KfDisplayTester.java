package nif.gui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nif.character.NifCharacter;
import nif.character.NifJ3dSkeletonRoot;
import tools.swing.DetailsFileChooser;
import tools.swing.TitledJFileChooser;
import utils.source.MediaSources;
import utils.source.file.FileMeshSource;
import utils.source.file.FileSoundSource;
import utils.source.file.FileTextureSource;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class KfDisplayTester
{
	private static Preferences prefs;

	private static String skeletonNifModelFile;

	private static ArrayList<String> skinNifFiles = new ArrayList<String>();

	public static KfDisplayTester nifDisplay;

	public static void main(String[] args)
	{
		prefs = Preferences.userNodeForPackage(KfDisplayTester.class);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration[] gc = gd.getConfigurations();
		GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
		template.setStencilSize(8);
		GraphicsConfiguration config = template.getBestConfiguration(gc);

		nifDisplay = new KfDisplayTester(config);
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
				skeletonNifModelFile = skeletonFc.getSelectedFile().getCanonicalPath();
				prefs.put("skeletonNifModelFile", skeletonNifModelFile);
				System.out.println("Selected skeleton file: " + skeletonNifModelFile);

				TitledJFileChooser skinFc = new TitledJFileChooser(prefs.get("skinNifModelFile", skeletonNifModelFile));
				skinFc.setDialogTitle("Select Skin(s)");
				skinFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				skinFc.setMultiSelectionEnabled(true);
				skinFc.setFileFilter(new FileNameExtensionFilter("nif files", "nif"));
				skinFc.showOpenDialog(new JFrame());

				if (skinFc.getSelectedFile() != null)
				{
					File[] skinNifModelFiles = skinFc.getSelectedFiles();
					prefs.put("skinNifModelFile", skinNifModelFiles[0].getCanonicalPath());

					for (File skinNifModelFile : skinNifModelFiles)
					{
						System.out.println("Selected skin file : " + skinNifModelFile);
						skinNifFiles.add(skinNifModelFile.getCanonicalPath());
					}

					String baseDir = prefs.get("KfDisplayTester.baseDir", System.getProperty("user.dir"));

					DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener()
					{

						@Override
						public void fileSelected(File file)
						{
							prefs.put("KfDisplayTester.baseDir", file.getPath());
							try
							{
								System.out.println("\tFile: " + file);
								display(skeletonNifModelFile, skinNifFiles, file);

							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}

							System.out.println("done");
						}

						@Override
						public void directorySelected(File dir)
						{
							// nothing ignored

						}
					});

					dfc.setFileFilter(new FileNameExtensionFilter("Kf", "kf"));

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

	private static void display(String skeletonNifFile, ArrayList<String> skinNifFiles, File kff)
	{
		transformGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;

		ArrayList<String> idleAnimations = new ArrayList<String>();
		idleAnimations.add(kff.getAbsolutePath());

		MediaSources mediaSources = new MediaSources(new FileMeshSource(), new FileTextureSource(), new FileSoundSource());
		NifCharacter nifCharacter = new NifCharacter(skeletonNifFile, skinNifFiles, mediaSources, idleAnimations);

		// now add the root to the scene so the controller sequence is live
		bg.addChild(nifCharacter);

		transformGroup.addChild(bg);

	}

	private static SimpleUniverse universe;

	private static TransformGroup transformGroup;

	public KfDisplayTester(GraphicsConfiguration config)
	{
		universe = new SimpleUniverse(new Canvas3D(config));

		transformGroup = new TransformGroup();

		JFrame f = new JFrame();
		f.getContentPane().setLayout(new GridLayout(1, 1));

		f.getContentPane().add(universe.getCanvas());

		f.setSize(900, 900);
		f.setLocation(400, 0);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

		MouseRotate mr = new MouseRotate(transformGroup);
		mr.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		mr.setEnable(true);
		bg.addChild(mr);

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

}