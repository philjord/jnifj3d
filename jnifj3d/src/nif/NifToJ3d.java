package nif;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import nif.character.KfJ3dRoot;
import nif.j3d.J3dBSTreeNode;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiCamera;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dRootCollisionNode;
import nif.j3d.J3dbhkCollisionObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.SimpleCamera;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiSequenceStreamHelper;
import nif.niobject.RootCollisionNode;
import nif.niobject.bhk.bhkCollisionObject;
import nif.niobject.bs.BSTreeNode;
import utils.source.MeshSource;
import utils.source.TextureSource;

public class NifToJ3d
{
	public static boolean HIDE_EDITORS = true;

	public static boolean USE_SHADERS = false;

	//Note this is caching the file read operations, not the j3d built object which are not shared
	//private static SoftValueHashMap<String, NifFile> loadedFiles = new SoftValueHashMap<String, NifFile>();
	private static WeakHashMap<String, NifFile> loadedFiles = new WeakHashMap<String, NifFile>();

	public static void clearCache()
	{
		loadedFiles.clear();
	}

	/**
	 * This is a caching sytem and should generally be the ONLY class to call getNifFile on a MeshSource or else trouble
	 * @param nifFilename
	 * @param meshSource
	 * @return
	 */
	public static NifFile loadNiObjects(String nifFilename, MeshSource meshSource)
	{
		NifFile nifFile = loadedFiles.get(nifFilename);

		if (nifFile == null)
		{
			nifFile = meshSource.getNifFile(nifFilename);
			loadedFiles.put(nifFilename, nifFile);
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
			NifJ3dVisRoot root = extractShapes(nifFile, textureSource);
			NifJ3dHavokRoot phys = extractHavok(nifFile);

			if (root != null)
			{
				NifJ3dVisPhysRoot nifJ3dVisPhysRoot = new NifJ3dVisPhysRoot(root.getVisualRoot(),
						phys == null ? null : phys.getHavokRoot(), new NiToJ3dData(nifFile.blocks));
				return nifJ3dVisPhysRoot;
			}
		}
		return null;
	}

	public static NifJ3dVisRoot loadShapes(String filename, MeshSource meshSource, TextureSource textureSource)
	{
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			return extractShapes(nifFile, textureSource);
		}
		return null;
	}

	public static NifJ3dHavokRoot loadHavok(String filename, MeshSource meshSource)
	{
		NifFile nifFile = loadNiObjects(filename, meshSource);
		if (nifFile != null)
		{
			return extractHavok(nifFile);
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

	private static NifJ3dVisRoot extractShapes(NifFile nifFile, TextureSource textureSource)
	{
		if (nifFile != null)
		{
			NiObject root = nifFile.blocks.root();
			if (root instanceof NiNode || root instanceof BSTreeNode)
			{
				NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
				J3dNiAVObject j3dNiAVObjectRoot = null;

				if (root instanceof NiNode)
				{
					j3dNiAVObjectRoot = J3dNiNode.createNiNode((NiNode) root, niToJ3dData, textureSource, false);
				}
				else if (root instanceof BSTreeNode)
				{
					j3dNiAVObjectRoot = new J3dBSTreeNode((BSTreeNode) root, niToJ3dData, textureSource, false);
				}
				else
				{
					System.out.println("*****************************bad root type! " + root);
				}

				// now setupcontrollers for all J3dNiAVObject now everything is constructed
				for (J3dNiAVObject jnao : niToJ3dData.j3dNiAVObjectValues())
				{
					jnao.setupController(niToJ3dData);
				}

				NifJ3dVisRoot nifJ3dRoot = new NifJ3dVisRoot(j3dNiAVObjectRoot, niToJ3dData);
				nifJ3dRoot.setCameras(NifToJ3d.extractCameras(niToJ3dData));

				// now to compact the nif model by removing unused transforms (Note: after everything is finsihed not before!)
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
			if (root instanceof NiNode || root instanceof BSTreeNode)
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
						// I hope they are always off root??? TODO: check this in all files
						j3dNiAVObjectRoot.addChild(jrcn);
						
						//TODO: very much mix this into the jbullet system
						//use BhkCollisionToNifBullet.makeFromGeometryInfo(GeometryInfo gi)
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
			if (nifFile.blocks.root() instanceof NiControllerSequence)
			{
				// make the kf file root 
				NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
				KfJ3dRoot kfJ3dRoot = new KfJ3dRoot((NiControllerSequence) niToJ3dData.root(), niToJ3dData);

				return kfJ3dRoot;
			}
			else if (nifFile.blocks.root() instanceof NiSequenceStreamHelper)
			{
				//Pre 10.1.0.0 
				//TODO: NiSequenceStreamHelper root F:\game media\Morrowind\Meshes\f\xfurn_redoran_flag_01.kf
				return null;
			}
			else
			{
				System.out.println("kf file MUST have a NiControllerSequence as root: " + nifFile.toString());
			}
		}

		return null;
	}

	//TODO: the NIfFooter in teh Niffile actually points to roots and cameras and should be used
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
