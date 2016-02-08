package nif.gui;

import java.io.File;
import java.util.Enumeration;
import java.util.prefs.Preferences;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Light;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import utils.source.file.FileMeshSource;
import utils.source.file.FileTextureSource;

import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * @author Administrator
 * 
 */
public class NifModelPerformanceRunner
{

	private static Preferences prefs;

	public static void main(String[] args)
	{

		setUpUniverseAndCanvas();

		prefs = Preferences.userNodeForPackage(NifModelPerformanceRunner.class);
		String baseDir = prefs.get("NifModelPerformanceRunner.baseDir", System.getProperty("user.dir"));

		JFileChooser fc = new JFileChooser(baseDir);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		fc.showOpenDialog(new JFrame());

		if (fc.getSelectedFile() != null)
		{

			File f = fc.getSelectedFile();
			System.out.println("Selected file: " + f);
			prefs.put("NifModelPerformanceRunner.baseDir", f.getPath());
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
		// System.exit(0);
	}

	private static void processFile(File f)
	{
		try
		{
			System.out.println("\tFile: " + f);

			NifToJ3d.loadHavok(f.getCanonicalPath(), new FileMeshSource());
			runPerformance(NifToJ3d.loadShapes(f.getCanonicalPath(), new FileMeshSource(), new FileTextureSource()).getVisualRoot());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static void processDir(File dir)
	{
		System.out.println("Processing directory " + dir);
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++)
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
	}

	private static SimpleUniverse universe;

	private static TransformGroup transformGroup = new TransformGroup();

	private static void setUpUniverseAndCanvas()
	{
		universe = new SimpleUniverse();

		//SwingUtilities.getWindowAncestor(universe.getCanvas()).setSize(600, 600);
		universe.getCanvas().getGLWindow().setSize(600, 600);
		universe.getCanvas().addNotify();

		transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		transformGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		transformGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light and add it
		Color3f alColor = new Color3f(0.6f, 0.6f, 0.6f);
		AmbientLight ambLight = new AmbientLight(true, alColor);
		ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		BranchGroup bg = new BranchGroup();
		bg.addChild(transformGroup);
		bg.addChild(ambLight);
		bg.addChild(new SpinTransform(transformGroup));
		bg.addChild(new FramesBehavior());
		bg.addChild(new TimeBehavior());

		universe.addBranchGraph(bg);

		Transform3D t = new Transform3D();
		t.lookAt(new Point3d(5, 0, -40), new Point3d(0, 0, 0), new Vector3d(0, 1, 0));
		t.invert();
		universe.getViewingPlatform().getViewPlatformTransform().setTransform(t);

	}

	private static void runPerformance(J3dNiAVObject model)
	{
		transformGroup.removeAllChildren();
		model.setCapability(BranchGroup.ALLOW_DETACH);
		for (int i = 0; i < 50; i++)
		{
			BranchGroup bg = new BranchGroup();
			bg.addChild(model.cloneTree());
			bg.setCapability(BranchGroup.ALLOW_DETACH);
			transformGroup.addChild(bg);
		}

		try
		{
			Thread.sleep(3000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.out.println("");
	}

	private static class SpinTransform extends Behavior
	{
		private TransformGroup trans;

		// Calculations for frame duration timing,
		// used between successive calls to process
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

	public static int FRAME_SAMPLE = 5;

	public static int TIME_SAMPLE = 1000;

	private static long currtime = 0;

	private static long lasttime = 0;

	private static long deltatime;

	private static int numOfFrames = 0;

	private static long timeOfFrames = 0;

	private static class FramesBehavior extends Behavior
	{
		private WakeupOnElapsedFrames wakeUp = new WakeupOnElapsedFrames(0);

		public FramesBehavior()
		{
			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			setEnable(true);
		}

		public void initialize()
		{
			wakeupOn(wakeUp);
		}

		@SuppressWarnings(
		{ "unchecked", "rawtypes" })
		public void processStimulus(Enumeration critera)
		{
			currtime = System.currentTimeMillis();
			deltatime = currtime - lasttime;
			lasttime = System.currentTimeMillis();

			numOfFrames++;
			timeOfFrames += deltatime;

			// Set the trigger for the behavior
			wakeupOn(wakeUp);
		}
	}

	private static class TimeBehavior extends Behavior
	{
		private WakeupOnElapsedTime wakeUp = new WakeupOnElapsedTime(TIME_SAMPLE);

		public TimeBehavior()
		{
			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
			setEnable(true);
		}

		public void initialize()
		{
			wakeupOn(wakeUp);
		}

		@SuppressWarnings(
		{ "unchecked", "rawtypes" })
		public void processStimulus(Enumeration critera)
		{
			// time is in millisec, so multiply by 1000 to get frames/sec
			double fps = numOfFrames / (timeOfFrames / 1000.0);

			fps = fps * 10;
			fps = Math.rint(fps);

			System.out.print(" " + ((int) fps / 10) + "." + ((int) fps % 10));

			numOfFrames = 0;
			timeOfFrames = 0;

			// Set the trigger for the behavior
			wakeupOn(wakeUp);

		}
	}
}
