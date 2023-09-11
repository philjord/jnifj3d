package nif.character;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiNode;
import utils.source.MeshSource;

public class NifJ3dSkeletonRoot extends Group
{
	public static boolean showBoneMarkers = false;

	private NiToJ3dData niToJ3dData;

	private J3dNiAVObject root;

	private J3dNiNode headJ3dNiNode;

	private J3dNiAVObject nonAccumRoot;

	private J3dNiAVObject accumRoot;

	private J3dNiAVObject skeletonRoot;

	private J3dNiDefaultAVObjectPalette allBonesInSkeleton = new J3dNiDefaultAVObjectPalette();

	public NifJ3dSkeletonRoot(String skeletonNifModelFile, MeshSource meshSource)
	{
		this(NifToJ3d.loadShapes(skeletonNifModelFile, meshSource, true));
	}

	public NifJ3dSkeletonRoot(NifJ3dVisRoot skeleton)
	{
		this(skeleton.getVisualRoot(), skeleton.getNiToJ3dData());
	}

	/**
	 * Do not call unless hasSkeletonRoot = true!
	 * @param root
	 * @param niToJ3dData
	 */
	public NifJ3dSkeletonRoot(J3dNiAVObject root, NiToJ3dData niToJ3dData)
	{
		this.root = root;
		this.niToJ3dData = niToJ3dData;
		if (root.getParent() == null)
		{
			addChild(root);
		}

		for (J3dNiAVObject j3dNiAVObject : niToJ3dData.j3dNiAVObjectValues())
		{
			if (j3dNiAVObject.getClass() == J3dNiNode.class)
			{
				J3dNiNode j3dNiNode = (J3dNiNode) j3dNiAVObject;
				NiNode niNode = (NiNode) j3dNiNode.getNiAVObject();

				if (niNode.name.equals("Bip01 Head") || niNode.name.equals("Bip02 Head") || niNode.name.indexOf("[HEAD]") != -1)
				{
					headJ3dNiNode = j3dNiNode;
				}

				j3dNiNode.setUncompactable();
				j3dNiNode.setVisualMarker(showBoneMarkers);

				if (isNonAccumNodeName(niNode.name))
				{
					if (nonAccumRoot != null)
					{
						System.out.println(
								"setting nonAccumRoot more than once!!was " + nonAccumRoot.getName() + " set to " + j3dNiNode.getName());
						System.out.println("for file " + niToJ3dData.nifVer.fileName);
					}

					nonAccumRoot = j3dNiNode;
				}
				else if (isAccumNodeName(niNode.name))
				{
					if (accumRoot != null)
					{
						System.out
								.println("setting accumRoot more than once!!was " + accumRoot.getName() + " set to " + j3dNiNode.getName());
						System.out.println("for file " + niToJ3dData.nifVer.fileName);
					}

					accumRoot = j3dNiNode;
				}
				else if (isRootBoneName(niNode.name))
				{
					if (skeletonRoot != null)
					{
						System.out.println(
								"setting skeletonRoot more than once!! was " + skeletonRoot.getName() + " set to " + j3dNiNode.getName());
						System.out.println("for file " + niToJ3dData.nifVer.fileName);
					}

					skeletonRoot = j3dNiNode;
				}

				allBonesInSkeleton.put(j3dNiNode);
			}
		}

		// Non accum mean node above is the accum node, means rotr above needs to be pushed down to non accum
		// http://gamebryo32docchs.googlecode.com/svn/trunk/gamebryo3_2_doc_chs/HTML/Convert/Previous/NiAnimation_Conversion.htm
		if (skeletonRoot == null)
		{
			//TODO: sometime the parent is a NifJ3dSkeletonRoot which can't be cast to J3dNiAvObject
			// possibly this code is being called twice and hoping for a null parent
			if (accumRoot != null && accumRoot.getParent() != null && (accumRoot.getParent() instanceof J3dNiAVObject))
				skeletonRoot = (J3dNiAVObject) accumRoot.getParent();
		}

		if (accumRoot == null)
		{
			if (nonAccumRoot != null && nonAccumRoot.getParent() != null)
				accumRoot = (J3dNiAVObject) nonAccumRoot.getParent();
		}

		//FIXME : TES3 reset this for now to show the motions of accum, but
		//not the final system at all, accum needs to be animated properly by itself
		// I notice this does not currently break anything
		// tes3 probably wants this to be bip01 pelvis?? but that collides with other games bones
		nonAccumRoot = accumRoot;

		if (nonAccumRoot == null)
		{
			// TES3 often has not these, let's see if the parent of Bip01 will do
			// new Throwable("nonAccumRoot == null").printStackTrace();
			//nonAccumRoot = root;
			nonAccumRoot = accumRoot;
		}
		
		if (accumRoot == null)
		{
			// this skeleton is probably stuffed now
			System.err.println("NifJ3dSkeletonRoot.accumRoot == null " + niToJ3dData.root());
		}
	}

	public static boolean isSkeleton(NiToJ3dData niToJ3dData)
	{
		for (J3dNiAVObject j3dNiAVObject : niToJ3dData.j3dNiAVObjectValues())
		{
			if (j3dNiAVObject.getClass() == J3dNiNode.class)
			{
				J3dNiNode j3dNiNode = (J3dNiNode) j3dNiAVObject;
				NiNode niNode = (NiNode) j3dNiNode.getNiAVObject();

				if (isAccumNodeName(niNode.name))
					return true;
			}
		}
		return false;
	}

	/**
	 * Note many nif have this node that are not related to skeletons
	 * @param name
	 * @return
	 */
	public static boolean isRootBoneName(String name)
	{
		return name.indexOf("Scene Root") != -1;
	}

	public static boolean isAccumNodeName(String name)
	{
		return name.equals("Bip01") || name.equals("Bip02") || name.indexOf("Root Bone") != -1 || name.indexOf("[Root]") != -1;
	}

	public static boolean isNonAccumNodeName(String name)
	{
		// note extra space character [COM ]
		return name.indexOf("NonAccum") != -1 || name.indexOf("[COM ]") != -1 || name.indexOf("Main Bone") != -1
				|| name.indexOf("Body Bone") != -1;
	}

	public J3dNiNode getHeadJ3dNiNode()
	{
		return headJ3dNiNode;
	}

	public J3dNiAVObject getNonAccumRoot()
	{
		return nonAccumRoot;
	}

	public J3dNiAVObject getAccumRoot()
	{
		return accumRoot;
	}

	public J3dNiAVObject getSkeletonRoot()
	{
		return skeletonRoot;
	}

	public J3dNiDefaultAVObjectPalette getAllBonesInSkeleton()
	{
		return allBonesInSkeleton;
	}

	public J3dNiAVObject getVisualRoot()
	{
		return root;
	}

	public J3dNiAVObject detachVisualRoot()
	{
		removeChild(root);
		return root;
	}

	public NiToJ3dData getniToJ3dData()
	{
		return niToJ3dData;
	}

	/**
	 * Called to update the transform, not used by blended skeleton system
	 */
	public void updateBones()
	{

		// set each to zero to indicate not calced yet
		for (int refId : allBonesInSkeleton.keySet())
		{
			J3dNiNode outputBone = (J3dNiNode) allBonesInSkeleton.get(refId);// wild cast			
			outputBone.getBoneCurrentAccumedTrans().setZero();//mark as not yet worked out
		}

		// store a accumed boneyTransform into each bone 
		for (int refId : allBonesInSkeleton.keySet())
		{
			J3dNiNode outputBone = (J3dNiNode) allBonesInSkeleton.get(refId);
			if (outputBone != skeletonRoot)
			{
				BlendedSkeletons.calcBoneVWTrans(outputBone, nonAccumRoot);
			}
		}
	}

}
