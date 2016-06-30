package nif.character;

import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;

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

				// Non accum mean node above is the accum node, means rotr above needs to be pushed down to non accum
				// http://gamebryo32docchs.googlecode.com/svn/trunk/gamebryo3_2_doc_chs/HTML/Convert/Previous/NiAnimation_Conversion.htm

				// note extra space character
				if (niNode.name.indexOf("NonAccum") != -1 || niNode.name.indexOf("[COM ]") != -1 || niNode.name.indexOf("Main Bone") != -1)
				{
					if (nonAccumRoot != null)
						System.out.println("setting nonAccumRoot more than once!!");

					// TODO: if there is trans or rot above tis node I need to accumulate some how
					nonAccumRoot = j3dNiNode;
				}

				if (isRootBoneName(niNode.name))
				{
					if (skeletonRoot != null)
						System.out.println("setting skeletonRoot more than once!!");
					skeletonRoot = j3dNiNode;

					// clear the bone root of it's rot and trans as these are Y trans for height and y rots for yaw
					// because in the real Gamebryo this is the node that is transformed by movement, where as we simply
					// do it one up from the root.

					skeletonRoot.setTransform(new Transform3D());
				}
				allBonesInSkeleton.put(j3dNiNode.getName(), j3dNiNode);

			}
		}

		if (nonAccumRoot == null)
		{
			// TES3 often has not these, let's see if the parent of Bip01 will do
			// new Throwable("nonAccumRoot == null").printStackTrace();
			nonAccumRoot = root;
		}

		if (skeletonRoot == null)
			new Throwable("skeletonRoot == null").printStackTrace();
	}

	public static boolean hasSkeletonRoot(NiToJ3dData niToJ3dData)
	{
		for (J3dNiAVObject j3dNiAVObject : niToJ3dData.j3dNiAVObjectValues())
		{
			if (j3dNiAVObject.getClass() == J3dNiNode.class)
			{
				J3dNiNode j3dNiNode = (J3dNiNode) j3dNiAVObject;
				NiNode niNode = (NiNode) j3dNiNode.getNiAVObject();

				if (isRootBoneName(niNode.name))
					return true;
			}
		}
		return false;
	}

	public static boolean isRootBoneName(String name)
	{
		return name.equals("Bip01") || name.equals("Bip02") || name.indexOf("[Root]") != -1 || name.indexOf("Root Bone") != -1;
	}

	public J3dNiNode getHeadJ3dNiNode()
	{
		return headJ3dNiNode;
	}

	public J3dNiAVObject getNonAccumRoot()
	{
		return nonAccumRoot;
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
	 * Called to update teh transform, not used by blened skeleton system
	 */
	public void updateBones()
	{

		// set each to zero to indicate not calced yet
		for (String boneName : allBonesInSkeleton.keySet())
		{
			J3dNiNode outputBone = (J3dNiNode) allBonesInSkeleton.get(boneName);// wild cast			
			outputBone.getBoneCurrentAccumedTrans().setZero();//mark as not yet worked out
		}

		// store a accumed boneyTransform into each bone 
		for (String boneName : allBonesInSkeleton.keySet())
		{
			J3dNiNode outputBone = (J3dNiNode) allBonesInSkeleton.get(boneName);
			if (outputBone != skeletonRoot)
			{
				BlendedSkeletons.calcBoneVWTrans(outputBone, nonAccumRoot);
			}
		}
	}

}
