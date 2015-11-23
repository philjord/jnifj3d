package nif.gui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nif.NifToJ3d;
import nif.character.NifCharacter;
import nif.character.NifCharacterTes3;
import nif.character.NifJ3dSkeletonRoot;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import tools.swing.DetailsFileChooser;
import tools.swing.TitledJFileChooser;
import utils.source.MediaSources;
import utils.source.file.FileMeshSource;
import utils.source.file.FileSoundSource;
import utils.source.file.FileTextureSource;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.universe.SimpleUniverse;

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
	private static Preferences prefs;

	private static String skeletonNifModelFile;

	private static ArrayList<String> skinNifFiles = new ArrayList<String>();

	public static KfDisplayTester nifDisplay;

	public static void main(String[] args)
	{
		NifToJ3d.SUPPRESS_EXCEPTIONS = false;
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
			String baseDir = prefs.get("skeletonNifModelFile", System.getProperty("user.dir"));
			TitledJFileChooser skeletonFc = new TitledJFileChooser(baseDir);
			skeletonFc.setDialogTitle("Select Skeleton");
			skeletonFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			skeletonFc.setMultiSelectionEnabled(false);
			skeletonFc.setFileFilter(new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return f.isDirectory() || f.getName().toLowerCase().contains("skeleton");
				}

				@Override
				public String getDescription()
				{
					return "Skeleton Files";
				}
			});

			skeletonFc.showOpenDialog(new JFrame());

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
				skinFc.showOpenDialog(new JFrame());

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
					DetailsFileChooser dfc = new DetailsFileChooser(skeletonNifModelFile, new DetailsFileChooser.Listener()
					{
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

	private static void display(String skeletonNifFile, ArrayList<String> skinNifFiles2, File kff)
	{
		transformGroup.removeAllChildren();

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

		transformGroup.addChild(bg);

	}

	private static void displayTes3(String skeletonNifFile, ArrayList<String> skinNifFiles2)
	{
		transformGroup.removeAllChildren();

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		NifJ3dSkeletonRoot.showBoneMarkers = true;
		J3dNiSkinInstance.showSkinBoneMarkers = false;//TODO: this doesn't show anything?
		MediaSources mediaSources = new MediaSources(new FileMeshSource(), new FileTextureSource(), new FileSoundSource());

		final NifCharacterTes3 nifCharacter = new NifCharacterTes3(skeletonNifFile, skinNifFiles2, mediaSources);
		bg.addChild(nifCharacter);

		transformGroup.addChild(bg);

		// now display all sequences from the kf file for user to pickage
		J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper = nifCharacter.getJ3dNiSequenceStreamHelper();

		JFrame frame = new JFrame("Select Sequence");
		frame.setSize(200, 600);

		final DefaultTableModel tableModel = new DefaultTableModel(new String[]
		{ "FireName", "Length (ms)", }, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; // disallow editing of the table
			}

			@Override
			@SuppressWarnings("unchecked")
			public Class<? extends Object> getColumnClass(int c)
			{
				return getValueAt(0, c).getClass();
			}
		};

		for (String fireName : j3dNiSequenceStreamHelper.getAllSequences())
		{
			long len = j3dNiSequenceStreamHelper.getSequence(fireName).getLengthMS();
			tableModel.addRow(new Object[]
			{ fireName, len });
		}
		final JTable table = new JTable(tableModel);
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				String newAnimation = (String) tableModel.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), 0);
				System.out.println("newAnimation " + newAnimation);
				nifCharacter.startAnimation(newAnimation, false);
			}

		});

		table.setRowSorter(new TableRowSorter<DefaultTableModel>(tableModel));

		frame.getContentPane().add(new JScrollPane(table));
		frame.setVisible(true);
	}

	private static SimpleUniverse universe;

	private static TransformGroup transformGroup;

	private static long c = 0;

	public KfDisplayTester(GraphicsConfiguration config)
	{
		Canvas3D canvas = new Canvas3D(config)
		{
			public void postRender()
			{
				//System.out.println("Count " + (c++));
			}
		};

		universe = new SimpleUniverse(canvas);

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