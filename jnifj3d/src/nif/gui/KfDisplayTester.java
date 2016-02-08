package nif.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.Light;
import javax.media.j3d.TransformGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.sun.j3d.utils.universe.SimpleUniverse;

import awt.tools3d.resolution.GraphicsSettings;
import awt.tools3d.resolution.QueryProperties;
import awt.tools3d.resolution.ScreenResolution;
import nif.NifToJ3d;
import nif.character.NifCharacter;
import nif.character.NifCharacterTes3;
import nif.character.NifJ3dSkeletonRoot;
import nif.gui.util.SpinTransform;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import tools.ddstexture.DDSTextureLoader;
import tools.swing.DetailsFileChooser;
import tools.swing.TitledJFileChooser;
import tools3d.camera.simple.SimpleCameraHandler;
import tools3d.utils.leafnode.Cube;
import utils.source.MediaSources;
import utils.source.file.FileMeshSource;
import utils.source.file.FileSoundSource;
import utils.source.file.FileTextureSource;

/**
 * Usage note
 * You must select a skeleteon at least
 * If you cancel on skins it will show only bones
 * If you cancel on animation select it will show the bind pose with no animations 
 * @author philip
 *
 */
public class KfDisplayTester
{

	private static String skeletonNifModelFile;

	private static ArrayList<String> skinNifFiles = new ArrayList<String>();

	public static KfDisplayTester nifDisplay;

	private static Preferences prefs;

	public JMenuItem setGraphics = new JMenuItem("Set Graphics");

	private SimpleCameraHandler simpleCameraHandler;

	private TransformGroup spinTransformGroup = new TransformGroup();

	private TransformGroup rotateTransformGroup = new TransformGroup();

	private BranchGroup modelGroup = new BranchGroup();

	private SpinTransform spinTransform;

	private SimpleUniverse simpleUniverse;

	private Background background = new Background();

	private JFrame win = new JFrame("Nif model");

	public KfDisplayTester()
	{
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		//jogl recomends for non phones 
		System.setProperty("jogl.disable.opengles", "true");

		//DDS requires no installed java3D
		if (QueryProperties.checkForInstalledJ3d())
		{
			System.exit(0);
		}

		//win.setVisible(true);
		win.setLocation(400, 0);
		win.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		Canvas3D canvas3D = new Canvas3D();

		//win.getContentPane().add(canvas3D);
		 
		simpleUniverse = new SimpleUniverse(canvas3D);
	//	GraphicsSettings gs = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(NifDisplayTester.class), win, false, true,
		//		true);

		//canvas3D.getView().setSceneAntialiasingEnable(gs.isAaRequired());
	//	DDSTextureLoader.setAnisotropicFilterDegree(gs.getAnisotropicFilterDegree());

		//win.setVisible(true);
		canvas3D.getGLWindow().setSize(800, 600);
		canvas3D.addNotify();
		spinTransformGroup.addChild(rotateTransformGroup);
		rotateTransformGroup.addChild(modelGroup);
		simpleCameraHandler = new SimpleCameraHandler(simpleUniverse.getViewingPlatform(), simpleUniverse.getCanvas(), modelGroup,
				rotateTransformGroup, false);

		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		modelGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		modelGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it
		Color3f alColor = new Color3f(1f, 1f, 1f);
		AmbientLight ambLight = new AmbientLight(true, alColor);
		ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		Color3f dlColor = new Color3f(0.1f, 0.1f, 0.6f);
		DirectionalLight dirLight = new DirectionalLight(true, dlColor, new Vector3f(0f, -1f, 0f));
		dirLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		dirLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		//Color3f plColor = new Color3f(0.6f, 0.6f, 0.6f);
		//PointLight pLight = new PointLight(true, plColor, new Point3f(10f, 10f, 0f), new Point3f(1f, 0.1f, 0f));
		//pLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		//pLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup bg = new BranchGroup();

		bg.addChild(ambLight);
		bg.addChild(dirLight);
		//bg.addChild(pLight);
		bg.addChild(simpleCameraHandler);

		//bg.addChild(fileManageBehavior);

		bg.addChild(spinTransformGroup);
		spinTransform = new SpinTransform(spinTransformGroup);
		spinTransform.setEnable(false);
		bg.addChild(spinTransform);

		background.setColor(0.8f, 0.8f, 0.8f);
		background.setApplicationBounds(null);
		background.setCapability(Background.ALLOW_APPLICATION_BOUNDS_WRITE);
		background.setCapability(Background.ALLOW_APPLICATION_BOUNDS_READ);
		bg.addChild(background);

		bg.addChild(new Cube(0.01f));

		simpleUniverse.addBranchGraph(bg);

		simpleUniverse.getViewer().getView().setBackClipDistance(50000);//big cos it's only 1 nif file anyway

		simpleUniverse.getCanvas().getGLWindow().addKeyListener(new KeyHandler());

		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		JMenu menu = new JMenu("File");
		menu.setMnemonic(70);
		menuBar.add(menu);

		menu.add(setGraphics);
		setGraphics.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				GraphicsSettings gs2 = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(NifDisplayTester.class), win,
						false, true, true);

				simpleUniverse.getCanvas().getView().setSceneAntialiasingEnable(gs2.isAaRequired());
				DDSTextureLoader.setAnisotropicFilterDegree(gs2.getAnisotropicFilterDegree());
				System.out.println("filtering will require newly loaded textures remember");
			}
		});

		win.setJMenuBar(menuBar);
		win.setVisible(true);

		try
		{
			// pick the nif model
			String baseDir = prefs.get("skeletonNifModelFile", System.getProperty("user.dir"));
			TitledJFileChooser skeletonFc = new TitledJFileChooser(baseDir);
			skeletonFc.setDialogTitle("Select Skeleton");
			skeletonFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			skeletonFc.setMultiSelectionEnabled(false);
			skeletonFc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f)
				{
					String fname = f.getName().toLowerCase();
					return f.isDirectory() || fname.contains("skeleton") || (fname.contains("xbase_anim") && fname.endsWith(".nif"));
				}

				@Override
				public String getDescription()
				{
					return "Skeleton Files";
				}
			});

			skeletonFc.showOpenDialog(win);

			if (skeletonFc.getSelectedFile() != null)
			{
				skeletonNifModelFile = skeletonFc.getSelectedFile().getCanonicalPath();
				prefs.put("skeletonNifModelFile", skeletonNifModelFile);

				System.out.println("Selected skeleton file: " + skeletonNifModelFile);

				TitledJFileChooser skinFc = new TitledJFileChooser(skeletonNifModelFile);
				skinFc.setDialogTitle("Select Skin(s)");
				skinFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				skinFc.setMultiSelectionEnabled(true);
				skinFc.setFileFilter(new FileNameExtensionFilter("Nif files", "nif"));
				skinFc.showOpenDialog(win);

				if (skinFc.getSelectedFile() != null)
				{
					File[] skinNifModelFiles = skinFc.getSelectedFiles();

					for (File skinNifModelFile : skinNifModelFiles)
					{
						System.out.println("Selected skin file : " + skinNifModelFile);
						skinNifFiles.add(skinNifModelFile.getCanonicalPath());
					}
				}
				else
				{
					//This is fine, just animate the bones and show them
				}
				if (!skeletonNifModelFile.toLowerCase().contains("morrowind"))
				{
					DetailsFileChooser dfc = new DetailsFileChooser(skeletonNifModelFile, new DetailsFileChooser.Listener() {
						@Override
						public void fileSelected(File file)
						{
							try
							{
								System.out.println("\tFile: " + file);
								display(skeletonNifModelFile, skinNifFiles, file);
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}
						}

						@Override
						public void directorySelected(File dir)
						{
							//  ignored
						}
					});

					dfc.setFileFilter(new FileNameExtensionFilter("Kf files", "kf"));

				}
				else
				{
					//morrowind has a single kf files named after sekeleton
					displayTes3(skeletonNifModelFile, skinNifFiles);
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

	private void display(String skeletonNifFile, ArrayList<String> skinNifFiles2, File kff)
	{
		modelGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;
		J3dNiSkinInstance.showSkinBoneMarkers = false;//TODO: this doesn't show anything?
		MediaSources mediaSources = new MediaSources(new FileMeshSource(), new FileTextureSource(), new FileSoundSource());

		ArrayList<String> idleAnimations = new ArrayList<String>();

		if (kff != null)
		{
			idleAnimations.add(kff.getAbsolutePath());
		}

		// now add the root to the scene so the controller sequence is live

		NifCharacter nifCharacter = new NifCharacter(skeletonNifFile, skinNifFiles2, mediaSources, idleAnimations);
		bg.addChild(nifCharacter);

		modelGroup.addChild(bg);

		simpleCameraHandler.viewBounds(nifCharacter.getBounds());

	}

	private void displayTes3(String skeletonNifFile, ArrayList<String> skinNifFiles2)
	{
		modelGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;
		J3dNiSkinInstance.showSkinBoneMarkers = false;//TODO: this doesn't show anything?
		MediaSources mediaSources = new MediaSources(new FileMeshSource(), new FileTextureSource(), new FileSoundSource());

		final NifCharacterTes3 nifCharacter = new NifCharacterTes3(skeletonNifFile, skinNifFiles2, mediaSources);
		bg.addChild(nifCharacter);

		modelGroup.addChild(bg);
		simpleCameraHandler.viewBounds(nifCharacter.getBounds());

		// now display all sequences from the kf file for user to pickage
		J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper = nifCharacter.getJ3dNiSequenceStreamHelper();

		JFrame seqFrame = new JFrame("Select Sequence");
		seqFrame.setSize(200, 600);
		seqFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final DefaultTableModel tableModel = new DefaultTableModel(new String[] { "FireName", "Length (ms)", }, 0) {
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; // disallow editing of the table
			}

			@Override
			public Class<? extends Object> getColumnClass(int c)
			{
				return getValueAt(0, c).getClass();
			}
		};

		for (String fireName : j3dNiSequenceStreamHelper.getAllSequences())
		{
			long len = j3dNiSequenceStreamHelper.getSequence(fireName).getLengthMS();
			tableModel.addRow(new Object[] { fireName, len });
		}
		final JTable table = new JTable(tableModel);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				String newAnimation = (String) tableModel.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), 0);
				System.out.println("newAnimation " + newAnimation);
				nifCharacter.startAnimation(newAnimation, false);
			}

		});

		table.setRowSorter(new TableRowSorter<DefaultTableModel>(tableModel));

		seqFrame.getContentPane().add(new JScrollPane(table));
		seqFrame.setVisible(true);
	}

	public static void main(String[] args)
	{
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
		prefs = Preferences.userNodeForPackage(KfDisplayTester.class);
		nifDisplay = new KfDisplayTester();
	}

	private class KeyHandler extends KeyAdapter
	{

		/*	public KeyHandler()
			{
				System.out.println("H toggle havok display");
				System.out.println("L toggle visual display");
				System.out.println("J toggle spin");
				System.out.println("K toggle animate model");
				System.out.println("P toggle background color");
				System.out.println("Space toggle cycle through files");
			}*/

		public void keyPressed(KeyEvent e)
		{

			/*	if (e.getKeyCode() == KeyEvent.VK_SPACE)
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
				else if (e.getKeyCode() == KeyEvent.VK_P)
				{
					toggleBackground();
				}*/
		}

	}

}