package nif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jogamp.java3d.Group;

import nif.character.KfJ3dRoot;
import nif.j3d.J3dBSTreeNode;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiCamera;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiTriShape;
import nif.j3d.J3dRootCollisionNode;
import nif.j3d.J3dbhkCollisionObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.SimpleCamera;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiSequenceStreamHelper;
import nif.niobject.NiTriShape;
import nif.niobject.RootCollisionNode;
import nif.niobject.bhk.bhkCollisionObject;
import nif.niobject.bs.BSTreeNode;
import tools.WeakValueHashMap;
import tools3d.utils.PhysAppearance;
import utils.optimize.NifFileOptimizer;
import utils.source.MeshSource;
import utils.source.TextureSource;

public class NifToJ3d
{
	public static boolean SUPPRESS_EXCEPTIONS = false;

	public static boolean HIDE_EDITORS = true;

	public static boolean USE_SHADERS = false;

	//Note this is caching the file read operations, not the j3d built object which are not shared
	//private static SoftValueHashMap<String, NifFile> loadedFiles = new SoftValueHashMap<String, NifFile>();
	
	
//	private static Map<String, NifFile> loadedFiles = Collections.synchronizedMap(new WeakValueHashMap<String, NifFile>());
	
	private static Map<String, NifFile> loadedFiles = Collections.synchronizedMap(new WeakHashMap<String, NifFile>());

	// we can't request the same file at the same time, this tell threads to wait for each other
	private static Set<String> loadingFiles = Collections.synchronizedSet(new HashSet<String>());

	public static void clearCache()
	{
		loadedFiles.clear();
	}

	//private static RequestStats requestStats = new RequestStats(loadedFiles);

	/**
	 * This is a caching system and should generally be the ONLY class to call getNifFile on a MeshSource or else trouble
	 * @param nifFilename
	 * @param meshSource
	 * @return
	 */
	public static NifFile loadNiObjects(String nifFilename, MeshSource meshSource)
	{
		//enable to test is caching is good
		//requestStats.request(nifFilename);

		NifFile nifFile = loadedFiles.get(nifFilename);

		if (nifFile == null)
		{
			boolean loading = loadingFiles.contains(nifFilename);

			while (loading)
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				loading = loadingFiles.contains(nifFilename);

			}

			loadingFiles.add(nifFilename);

			nifFile = meshSource.getNifFile(nifFilename);
			if (nifFile != null)
			{
				NifFileOptimizer nifFileOptimizer = new NifFileOptimizer(nifFile);
				nifFileOptimizer.optimize();
			}
			loadedFiles.put(nifFilename, nifFile);

			loadingFiles.remove(nifFilename);

		}

		return nifFile;
	}

	/** 
	 * loads both nif vis and nif havok into the root
	 */
	public static NifJ3dVisPhysRoot loadNif(String modelFileName, MeshSource meshSource, TextureSource textureSource)
	{
		NifFile nifFile = loadNiObjects(modelFileName, meshSource);
		if (nifFile != null)
		{
			try
			{
				NifJ3dVisRoot root = extractShapes(nifFile, textureSource, false);
				NifJ3dHavokRoot phys = extractHavok(nifFile);
				if (root != null)
				{
					NifJ3dVisPhysRoot nifJ3dVisPhysRoot = new NifJ3dVisPhysRoot(root.getVisualRoot(),
							phys == null ? null : phys.getHavokRoot(), root.getNiToJ3dData());
					return nifJ3dVisPhysRoot;
				}
			}
			catch (RuntimeException e)
			{
				System.out.println("RuntimeException " + e.toString() + " extracting shapes from " + modelFileName);
				if (!SUPPRESS_EXCEPTIONS)
					throw e;
			}

		}
		return null;
	}

	public static NifJ3dVisRoot loadShapes(String filename, MeshSource meshSource, TextureSource textureSource)
	{
		//System.out.println("filename " +filename);
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			try
			{
				return extractShapes(nifFile, textureSource, false);
			}
			catch (RuntimeException e)
			{
				System.out.println("RuntimeException " + e.toString() + " extracting shapes from " + filename);
				if (!SUPPRESS_EXCEPTIONS)
					throw e;
			}
		}
		return null;
	}

	public static NifJ3dVisRoot loadShapes(String filename, MeshSource meshSource, boolean nodesOnly)
	{
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			return extractShapes(nifFile, null, nodesOnly);
		}
		return null;
	}

	public static NifJ3dHavokRoot loadHavok(String filename, MeshSource meshSource)
	{
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			try
			{
				return extractHavok(nifFile);
			}
			catch (RuntimeException e)
			{
				System.out.println("RuntimeException " + e.toString() + " extracting havok from " + filename);
				if (!SUPPRESS_EXCEPTIONS)
					throw e;
			}
		}
		return null;
	}

	public static KfJ3dRoot loadKf(String filename, MeshSource meshSource)
	{
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			return extractKf(nifFile);
		}
		return null;
	}

	private static NifJ3dVisRoot extractShapes(NifFile nifFile, TextureSource textureSource, boolean nodesOnly)
	{
		if (nifFile != null)
		{

			NiObject root = nifFile.blocks.root();
			//sometimes in tes3 nif files are just a nitrishape
			if (root instanceof NiNode || root instanceof BSTreeNode || root instanceof NiTriShape)
			{
				NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
				J3dNiAVObject j3dNiAVObjectRoot = null;

				if (root instanceof NiNode)
				{
					j3dNiAVObjectRoot = J3dNiNode.createNiNode((NiNode) root, niToJ3dData, textureSource, nodesOnly);
				}
				else if (root instanceof BSTreeNode)
				{
					j3dNiAVObjectRoot = new J3dBSTreeNode((BSTreeNode) root, niToJ3dData, textureSource, nodesOnly);
				}
				else if (root instanceof NiTriShape && !nodesOnly)
				{
					j3dNiAVObjectRoot = new J3dNiTriShape((NiTriShape) root, niToJ3dData, textureSource);
				}
				else
				{
					System.out.println("*****************************bad root type! " + root);
				}

				// we want to force merging and compiling to stop at the root, this should be enough
				j3dNiAVObjectRoot.setCapability(Group.ALLOW_PARENT_READ);

				// now setupcontrollers for all J3dNiAVObject now everything is constructed
				for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
				{
					jnao.setupController(niToJ3dData);
				}

				NifJ3dVisRoot nifJ3dRoot = new NifJ3dVisRoot(j3dNiAVObjectRoot, niToJ3dData);
				nifJ3dRoot.setCameras(NifToJ3d.extractCameras(niToJ3dData));

				// now to compact the nif model by removing unused transforms (Note: after everything is finsihed not before!)
				// this in fact does nothing at all now! but will probably again one day
				for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
				{
					jnao.compact();
				}

				return nifJ3dRoot;
			}
			else if (nifFile.blocks.root() instanceof NiControllerSequence)
			{
				// let's just pretend its a kf file for now, and do nothing.
				System.out.println("extractShapes got handed a file " + nifFile + " with a NiControllerSequence root");

			}

		}
		return null;
	}

	private static NifJ3dHavokRoot extractHavok(NifFile nifFile)
	{
		if (nifFile != null)
		{
			NiObject root = nifFile.blocks.root();
			if (root instanceof NiNode || root instanceof BSTreeNode || root instanceof NiTriShape)
			{

				NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
				J3dNiAVObject j3dNiAVObjectRoot = null;

				if (root instanceof NiNode)
				{
					// create only the ninode so we can animate and have a full tree structure to support physics
					j3dNiAVObjectRoot = J3dNiNode.createNiNode((NiNode) root, niToJ3dData, null, true);
				}
				else if (root instanceof BSTreeNode)
				{
					j3dNiAVObjectRoot = new J3dBSTreeNode((BSTreeNode) root, niToJ3dData, null, true);
				}
				else if (root instanceof NiTriShape)
				{
					//in this case this is the only node so we need to make it phys looking
					J3dNiTriShape s = new J3dNiTriShape((NiTriShape) root, niToJ3dData, null);
					s.getShape().setAppearance(PhysAppearance.makeAppearance());
					j3dNiAVObjectRoot = s;
				}
				else
				{
					System.out.println("No root found in extractHavok!");
				}
				if (j3dNiAVObjectRoot != null)
				{
					// we want to force merging and compiling to stop at the root, this should be enough
					j3dNiAVObjectRoot.setCapability(Group.ALLOW_PARENT_READ);
				}
				else
				{
					//I feel I should tell someone there's an issue?
				}

				// now attach each havok node to it appropriate NiNOde				
				for (NiObject niObject : nifFile.blocks.getNiObjects())
				{
					if (niObject instanceof bhkCollisionObject)
					{
						// NOTE attaches itself into the hierarchy in j3dNiNodes
						new J3dbhkCollisionObject((bhkCollisionObject) niObject, niToJ3dData);
					}
					else if (niObject instanceof RootCollisionNode)
					{
						// morrowind special verison of above
						J3dRootCollisionNode jrcn = new J3dRootCollisionNode((RootCollisionNode) niObject, niToJ3dData);

						// I hope they are always off root??? check this in all files						
						if (((RootCollisionNode) niObject).parent.parent != null)
							System.out.println("Bugger RootCollisionNode not off root!!!");

						j3dNiAVObjectRoot.addChild(jrcn);
					}
				}

				// now setupcontrollers for all
				for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
				{
					jnao.setupController(niToJ3dData);
				}

				NifJ3dHavokRoot nifJ3dRoot = new NifJ3dHavokRoot(j3dNiAVObjectRoot, niToJ3dData);

				// now to compact the nif model by removing unused transforms (Note: after everything is finsihed not before!)
				for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
				{
					jnao.compact();
				}

				return nifJ3dRoot;
			}
			else if (nifFile.blocks.root() instanceof NiControllerSequence)
			{
				System.out.println("extractHavok does not work with kf file at this stage");
			}

		}
		return null;

	}

	private static KfJ3dRoot extractKf(NifFile nifFile)
	{
		if (nifFile != null)
		{
			NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
			// make the kf file root 
			if (nifFile.blocks.root() instanceof NiControllerSequence)
			{
				KfJ3dRoot kfJ3dRoot = new KfJ3dRoot((NiControllerSequence) niToJ3dData.root(), niToJ3dData);
				return kfJ3dRoot;
			}
			else if (nifFile.blocks.root() instanceof NiSequenceStreamHelper)
			{
				KfJ3dRoot kfJ3dRoot = new KfJ3dRoot((NiSequenceStreamHelper) niToJ3dData.root(), niToJ3dData);
				return kfJ3dRoot;
			}
			else
			{
				System.out.println("kf file MUST have a NiControllerSequence as root: " + nifFile.toString());
			}
		}

		return null;
	}

	//TODO: the NifFooter in the Nif file actually points to roots and cameras and should be used
	public static List<SimpleCamera> extractCameras(NiToJ3dData niToJ3dData)
	{
		ArrayList<SimpleCamera> cams = new ArrayList<SimpleCamera>();
		if (niToJ3dData != null)
		{
			for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
			{
				if (jnao instanceof J3dNiCamera)
				{
					J3dNiCamera j3dNiCamera = (J3dNiCamera) jnao;

					SimpleCamera camera = new SimpleCamera();
					j3dNiCamera.addChild(camera);
					cams.add(camera);
				}
			}
		}
		return cams;
	}

}
