package nif.character;

import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;

import nif.NiObjectList;
import nif.NifFile;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.basic.NifRef;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiStringExtraData;
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
		//TODO: this could be ninode only?
		NifJ3dVisRoot skeleton = NifToJ3d.loadShapes(skeletonNifModelFile, meshSource, null);
		NifFile sNifFile = NifToJ3d.loadNiObjects(skeletonNifModelFile, meshSource);

		this.root = skeleton.getVisualRoot();
		niToJ3dData = skeleton.getNiToJ3dData();
		addChild(root);

		for (J3dNiAVObject j3dNiAVObject : niToJ3dData.j3dNiAVObjectValues())
		{
			if (j3dNiAVObject.getClass() == J3dNiNode.class)
			{
				J3dNiNode j3dNiNode = (J3dNiNode) j3dNiAVObject;
				NiNode niNode = (NiNode) j3dNiNode.getNiAVObject();

				if (niNode.name.equals("Bip01 Head") || niNode.name.indexOf("[HEAD]") != -1)
				{
					headJ3dNiNode = j3dNiNode;
				}

				j3dNiNode.setUncompactable();
				j3dNiNode.setVisualMarker(showBoneMarkers);

				// note extra space character
				if (niNode.name.indexOf("NonAccum") != -1 || niNode.name.indexOf("[COM ]") != -1)
				{
					if (nonAccumRoot != null)
						System.out.println("setting nonAccumRoot more than once!!");

					//TODO: if there is trans or rot above tis node I need to accumulate some how
					nonAccumRoot = j3dNiNode;
				}

				if (niNode.name.equals("Bip01") || niNode.name.indexOf("[Root]") != -1)
				{
					if (skeletonRoot != null)
						System.out.println("setting accumNode more than once!!");
					skeletonRoot = j3dNiNode;
				}
				allBonesInSkeleton.put(j3dNiNode.getName(), j3dNiNode);

				// clear the bone root of it's rot and trans as these are Y trans for height and y rots for yaw
				// because in the real Gamebryo this is the node that is transformed by movement, where as we simply 
				// do it one up from the root.
				NiStringExtraData niStringExtraData = getNiStringExtraData(niNode, sNifFile.blocks);
				if ((niStringExtraData != null && niStringExtraData.stringData.indexOf("BoneRoot#") != -1)
						|| niNode.name.indexOf("[Root]") != -1)
				{
					//TODO: taking this out make the horses head appear correct (bones must be right)
					// but the skinned stuff is still on it's side
					j3dNiNode.getTransformGroup().setTransform(new Transform3D());
				}
			}
		}
	}

	public J3dNiNode getHeadJ3dNiNode()
	{
		return headJ3dNiNode;
	}

	private NiStringExtraData getNiStringExtraData(NiNode niNode, NiObjectList blocks)
	{
		for (NifRef nifRef : niNode.extraDataList)
		{
			NiObject niObject = blocks.get(nifRef);
			if (niObject instanceof NiStringExtraData)
			{
				return (NiStringExtraData) niObject;
			}
		}
		return null;
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

}
