package nif.gui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import java3d.nativelinker.Java3dLinker2;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.Light;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import nif.NifJ3dVisPhysRoot;
import nif.NifToJ3d;
import nif.gui.util.ControllerInvokerThread;
import nif.gui.util.NiObjectDisplayTable;
import nif.gui.util.NifFileDisplayTable;
import nif.gui.util.NifFileDisplayTree;
import nif.gui.util.SpinTransform;
import nif.j3d.J3dNiAVObject;
import tools.swing.DetailsFileChooser;
import tools3d.camera.simple.SimpleCameraHandler;
import utils.ESConfig;
import utils.source.MeshSource;
import utils.source.TextureSource;
import utils.source.file.FileMeshSource;
import utils.source.file.FileTextureSource;

import com.sun.j3d.utils.universe.SimpleUniverse;

public class NifDisplayTester
{
	private SimpleCameraHandler simpleCameraHandler;

	private TransformGroup spinTransformGroup = new TransformGroup();

	private TransformGroup rotateTransformGroup = new TransformGroup();

	private BranchGroup modelGroup = new BranchGroup();

	private SpinTransform spinTransform;

	private FileManageBehavior fileManageBehavior = new FileManageBehavior();

	private boolean cycle = true;

	private boolean showHavok = true;

	private boolean showVisual = true;

	private boolean animateModel = true;

	private boolean spin = false;

	private long currentFileLoadTime = 0;

	private File currentFileTreeRoot;

	private File nextFileTreeRoot;

	private File currentFileDisplayed;

	private File nextFileToDisplay;

	private JSplitPane splitterV = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private JSplitPane splitterH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	private NiObjectDisplayTable niObjectDisplayTable = new NiObjectDisplayTable();

	private NifFileDisplayTable nifFileDisplayTable = new NifFileDisplayTable(niObjectDisplayTable);

	private NifFileDisplayTree nifFileDisplayTree = new NifFileDisplayTree(niObjectDisplayTable);

	private SimpleUniverse simpleUniverse;

	public NifDisplayTester(GraphicsConfiguration config)
	{
		simpleUniverse = new SimpleUniverse(new Canvas3D(config));

		JFrame dataF = new JFrame();
		dataF.getContentPane().setLayout(new GridLayout(1, 1));

		splitterH.setTopComponent(nifFileDisplayTree);
		splitterH.setBottomComponent(nifFileDisplayTable);

		splitterV.setTopComponent(splitterH);
		splitterV.setBottomComponent(niObjectDisplayTable);

		dataF.getContentPane().add(splitterV);

		dataF.setSize(900, 900);
		dataF.setLocation(400, 0);
		dataF.setVisible(true);

		spinTransformGroup.addChild(rotateTransformGroup);
		rotateTransformGroup.addChild(modelGroup);
		simpleCameraHandler = new SimpleCameraHandler(simpleUniverse.getViewingPlatform(), simpleUniverse.getCanvas(), modelGroup, rotateTransformGroup, false);

		JFrame f = new JFrame();
		f.getContentPane().setLayout(new GridLayout(1, 1));

		f.getContentPane().add(simpleUniverse.getCanvas());

		f.setSize(900, 900);
		f.setLocation(400, 0);
		f.setVisible(true);

		splitterV.setDividerLocation(0.5d);
		splitterH.setDividerLocation(0.5d);

		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		modelGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		modelGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it
		Color3f alColor = new Color3f(1f, 1f, 1f);
		AmbientLight ambLight = new AmbientLight(true, alColor);
		ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		Color3f dlColor = new Color3f(0.6f, 0.6f, 0.6f);
		DirectionalLight dirLight = new DirectionalLight(true, dlColor, new Vector3f(0f, -1f, 0f));
		dirLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		dirLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup bg = new BranchGroup();

		bg.addChild(ambLight);
		bg.addChild(dirLight);
		bg.addChild(simpleCameraHandler);

		bg.addChild(fileManageBehavior);

		bg.addChild(spinTransformGroup);
		spinTransform = new SpinTransform(spinTransformGroup);
		spinTransform.setEnable(false);
		bg.addChild(spinTransform);

		simpleUniverse.addBranchGraph(bg);

		simpleUniverse.getViewer().getView().setBackClipDistance(50000);//big cos it's only 1 nif file anyway

		simpleUniverse.getCanvas().addKeyListener(new KeyHandler());

	}

	public void setNextFileTreeRoot(File nextFileTreeRoot)
	{
		this.nextFileToDisplay = null;
		this.nextFileTreeRoot = nextFileTreeRoot;
	}

	public void setNextFileToDisplay(File nextFileToDisplay)
	{
		this.nextFileTreeRoot = null;
		this.nextFileToDisplay = nextFileToDisplay;
	}

	private void manage()
	{
		if (nextFileTreeRoot != null)
		{
			if (!nextFileTreeRoot.equals(currentFileTreeRoot))
			{
				currentFileTreeRoot = nextFileTreeRoot;
				currentFileDisplayed = null;
				currentFileLoadTime = Long.MAX_VALUE;
			}
		}
		else if (currentFileTreeRoot != null)
		{
			if (cycle)
			{
				File[] files = currentFileTreeRoot.listFiles(new NifKfFileFilter());
				if (files.length > 0)
				{
					if (currentFileDisplayed == null)
					{
						currentFileDisplayed = files[0];
						displayNif(currentFileDisplayed);
					}
					else if (System.currentTimeMillis() - currentFileLoadTime > 3000)
					{

					}
				}
			}
		}
		else if (nextFileToDisplay != null)
		{
			if (!nextFileToDisplay.equals(currentFileDisplayed))
			{
				currentFileDisplayed = nextFileToDisplay;
				displayNif(currentFileDisplayed);
				nextFileToDisplay = null;
			}
		}
	}

	private void toggleSpin()
	{
		spin = !spin;
		if (spinTransform != null)
		{
			spinTransform.setEnable(spin);
		}
	}

	private void toggleAnimateModel()
	{
		animateModel = !animateModel;
		update();
	}

	private void toggleHavok()
	{
		showHavok = !showHavok;
		update();
	}

	private void toggleVisual()
	{
		showVisual = !showVisual;
		update();
	}

	private void toggleCycling()
	{
		cycle = !cycle;
		/*if (cycle)
		{
			// awake the directory processing thread
			synchronized (waitMonitor)
			{
				waitMonitor.notifyAll();
			}
		}*/
	}

	public void displayNif(File f)
	{
		System.out.println("Selected file: " + f);

		if (f.isDirectory())
		{
			//spinTransform.setEnable(true);
			//processDir(f);
			System.out.println("Bad news dir sent into display nif");
		}
		else if (f.isFile())
		{
			showNif(f.getAbsolutePath(), new FileMeshSource(), new FileTextureSource());
		}

		System.out.println("done");

	}

	public void showNif(String filename, MeshSource meshSource, TextureSource textureSource)
	{

		if (filename.contains("skyrim"))
		{
			ESConfig.HAVOK_TO_METERS_SCALE = ESConfig.SKYRIM_HAVOK_TO_METERS_SCALE;
		}
		display(NifToJ3d.loadNif(filename, meshSource, textureSource));
	}

	private BranchGroup hbg;

	private BranchGroup vbg;

	private void update()
	{
		modelGroup.removeAllChildren();
		if (showHavok)
		{
			modelGroup.addChild(hbg);
		}
		if (showVisual)
		{
			modelGroup.addChild(vbg);
		}
	}

	private void display(NifJ3dVisPhysRoot nif)
	{

		if (nif != null)
		{

			J3dNiAVObject havok = nif.getHavokRoot();
			if (nif.getVisualRoot().getJ3dNiControllerManager() != null && animateModel)
			{
				//note self cleaning uping
				ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(nif.getVisualRoot().getName(), nif
						.getVisualRoot().getJ3dNiControllerManager(), havok.getJ3dNiControllerManager());
				controllerInvokerThread.start();
			}

			modelGroup.removeAllChildren();

			hbg = new BranchGroup();
			hbg.setCapability(BranchGroup.ALLOW_DETACH);

			if (showHavok && havok != null)
			{
				hbg.addChild(havok);
				modelGroup.addChild(hbg);
			}

			vbg = new BranchGroup();
			vbg.setCapability(BranchGroup.ALLOW_DETACH);

			if (showVisual && nif != null)
			{
				vbg.addChild(nif.getVisualRoot());
				modelGroup.addChild(vbg);
			}

			simpleCameraHandler.viewBounds(nif.getVisualRoot().getBounds());

			spinTransform.setEnable(spin);

		}
		else
		{
			System.out.println("why you give display a null eh?");
		}

	}

	public static NifDisplayTester nifDisplay;

	private static Preferences prefs;

	public static void main(String[] args)
	{
		new Java3dLinker2();
		prefs = Preferences.userNodeForPackage(NifDisplayTester.class);
		String baseDir = prefs.get("NifDisplayTester.baseDir", System.getProperty("user.dir"));

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration[] gc = gd.getConfigurations();
		GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
		template.setStencilSize(8);
		GraphicsConfiguration config = template.getBestConfiguration(gc);

		nifDisplay = new NifDisplayTester(config);

		DetailsFileChooser dfc = new DetailsFileChooser(baseDir, new DetailsFileChooser.Listener()
		{

			@Override
			public void directorySelected(File dir)
			{
				prefs.put("NifDisplayTester.baseDir", dir.getPath());
				nifDisplay.setNextFileTreeRoot(dir);
			}

			@Override
			public void fileSelected(File file)
			{
				prefs.put("NifDisplayTester.baseDir", file.getPath());
				nifDisplay.setNextFileToDisplay(file);
			}
		});

		dfc.setFileFilter(new FileNameExtensionFilter("Nif or Kf", "nif", "kf"));

	}

	private class FileManageBehavior extends Behavior
	{

		private WakeupCondition FPSCriterion = new WakeupOnElapsedFrames(0, false);

		public FileManageBehavior()
		{

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
			manage();
		}

	}

	private class KeyHandler extends KeyAdapter
	{

		public KeyHandler()
		{
			System.out.println("H toggle havok display");
			System.out.println("L toggle visual display");
			System.out.println("J toggle spin");
			System.out.println("K toggle animate model");
			System.out.println("Space toggle cycle through files");
		}

		public void keyPressed(KeyEvent e)
		{

			if (e.getKeyCode() == KeyEvent.VK_SPACE)
			{
				toggleCycling();
			}
			else if (e.getKeyCode() == KeyEvent.VK_H)
			{
				toggleHavok();
			}
			else if (e.getKeyCode() == KeyEvent.VK_J)
			{
				toggleSpin();
			}
			else if (e.getKeyCode() == KeyEvent.VK_K)
			{
				toggleAnimateModel();
			}
			else if (e.getKeyCode() == KeyEvent.VK_L)
			{
				toggleVisual();
			}
		}

	}

}