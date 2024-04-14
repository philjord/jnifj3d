package nif.j3d;

import java.util.HashMap;

import org.jogamp.java3d.Group;

import nif.basic.NifPtr;
import nif.character.NifJ3dSkeletonRoot;
import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.bs.BSSkin;
import nif.niobject.bs.BSSkin.BoneData;


public class J3dBsSkinInstance extends J3dNiSkinInstance
{
 
	public J3dBsSkinInstance(BSSkin.Instance bsSkinInstance, J3dBSTriShape j3dBSTriShape, NiToJ3dData niToJ3dData,
			NifJ3dSkeletonRoot nifJ3dSkeletonRoot)
	{
		this.j3dNiTriBasedGeom = j3dBSTriShape;
		J3dNiDefaultAVObjectPalette allSkeletonBones = nifJ3dSkeletonRoot.getAllBonesInSkeleton();

		if (j3dBSTriShape.getParent() != null)
		{
			((Group) j3dBSTriShape.getParent()).removeChild(j3dBSTriShape);
		}

		skinSkeletonRoot = niToJ3dData.get((NiAVObject) niToJ3dData.get(bsSkinInstance.Target));
		skinSkeletonRoot.setVisualMarker(showSkinBoneMarkers);

		addChild(j3dBSTriShape);

		//add bones to list
		J3dNiNode[] skinBonesInOrder = new J3dNiNode[bsSkinInstance.Bones.length];
		HashMap<String, J3dNiNode> skeletonBones = new HashMap<String, J3dNiNode>();

		for (int boneIdx = 0; boneIdx < bsSkinInstance.Bones.length; boneIdx++)
		{
			NifPtr p = bsSkinInstance.Bones[boneIdx];
			if (p.ptr != -1)
			{
				NiNode n = (NiNode) niToJ3dData.get(p);
				J3dNiNode skinBone = (J3dNiNode) niToJ3dData.get(n);
				skinBone.setVisualMarker(showSkinBoneMarkers);

				skinBonesInOrder[boneIdx] = skinBone;

				// notice we are the skin now, so our node refId are not at all the skeleton bone refIds
				J3dNiNode skeletonBone = (J3dNiNode) allSkeletonBones.getByName(n.name);
				skeletonBones.put(n.name, skeletonBone);
			}
		}

		if (bsSkinInstance.BoneData.ref != -1 && true)
		{
			//NOTE!! skinwieghts are now stored in the BSTriShape
			BoneData niSkinData = (BSSkin.BoneData) niToJ3dData.get(bsSkinInstance.BoneData);
			j3dSkin = new J3dBSSkin(niSkinData, j3dBSTriShape, skinBonesInOrder, skeletonBones);
		}

	}

  

	
}