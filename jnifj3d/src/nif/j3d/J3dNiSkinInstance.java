package nif.j3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.media.j3d.Group;
import javax.vecmath.Color3f;

import tools3d.utils.scenegraph.Fadable;
import nif.basic.NifPtr;
import nif.character.NifJ3dSkeletonRoot;
import nif.niobject.NiAVObject;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import nif.niobject.NiSkinData;
import nif.niobject.NiSkinInstance;
import nif.niobject.NiTriShape;

public class J3dNiSkinInstance extends Group implements Fadable
{
	public static boolean showSkinBoneMarkers = false;

	private J3dNiNode[] skinBonesInOrder;

	private J3dNifSkinData j3dNifSkinData;

	private LinkedHashMap<String, J3dNiNode> skeletonBones = new LinkedHashMap<String, J3dNiNode>();

	private J3dNiAVObject skinSkeletonRoot;

	public J3dNiSkinInstance(NiSkinInstance niSkinInstance, J3dNiTriShape j3dNiTriShape, NiToJ3dData niToJ3dData,
			NifJ3dSkeletonRoot nifJ3dSkeletonRoot)
	{
		J3dNiDefaultAVObjectPalette allSkeletonBones = nifJ3dSkeletonRoot.getAllBonesInSkeleton();

		if (j3dNiTriShape.getParent() != null)
		{
			((Group) j3dNiTriShape.getParent()).removeChild(j3dNiTriShape);
		}

		skinSkeletonRoot = niToJ3dData.get((NiAVObject) niToJ3dData.get(niSkinInstance.skeletonRoot));
		skinSkeletonRoot.setVisualMarker(showSkinBoneMarkers);

		addChild(j3dNiTriShape);

		//add bones to list
		skinBonesInOrder = new J3dNiNode[niSkinInstance.bones.length];
		for (int boneIdx = 0; boneIdx < niSkinInstance.bones.length; boneIdx++)
		{
			NifPtr p = niSkinInstance.bones[boneIdx];
			if (p.ptr != -1)
			{
				NiNode n = (NiNode) niToJ3dData.get(p);
				J3dNiNode skinBone = (J3dNiNode) niToJ3dData.get(n);
				skinBone.setVisualMarker(showSkinBoneMarkers);

				skinBonesInOrder[boneIdx] = skinBone;

				J3dNiNode skeletonBone = (J3dNiNode) allSkeletonBones.get(n.name);
				skeletonBones.put(n.name, skeletonBone);
			}
		}

		if (niSkinInstance.data.ref != -1 && true)
		{
			NiSkinData niSkinData = (NiSkinData) niToJ3dData.get(niSkinInstance.data);
			j3dNifSkinData = new J3dNifSkinData(niSkinData, j3dNiTriShape, skinBonesInOrder, skeletonBones);
		}

	}

	public void processSkinInstance()
	{
		if (j3dNifSkinData != null)
		{
			j3dNifSkinData.updateSkin();
		}
	}

	@Override
	public void fade(float percent)
	{
		if (j3dNifSkinData != null)
		{
			j3dNifSkinData.fade(percent);
		}
	}

	@Override
	public void setOutline(Color3f c)
	{
		if (j3dNifSkinData != null)
		{
			j3dNifSkinData.setOutline(c);
		}
	}

	 

	public static ArrayList<J3dNiSkinInstance> createSkins(NiToJ3dData niToJ3dData, NifJ3dSkeletonRoot nifJ3dSkeletonRoot)
	{
		ArrayList<J3dNiSkinInstance> j3dNiSkinInstances = new ArrayList<J3dNiSkinInstance>();
		for (J3dNiAVObject j3dNiAVObject : niToJ3dData.j3dNiAVObjectValues())
		{
			if (j3dNiAVObject instanceof J3dNiGeometry)
			{
				J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) j3dNiAVObject;
				NiGeometry niGeometry = (NiGeometry) j3dNiGeometry.getNiAVObject();
				if (niGeometry.skin.ref != -1)
				{
					NiSkinInstance niSkinInstance = (NiSkinInstance) niToJ3dData.get(niGeometry.skin);

					if (niGeometry instanceof NiTriShape)
					{
						NiTriShape niTriShape = (NiTriShape) niGeometry;
						J3dNiTriShape j3dNiTriShape = (J3dNiTriShape) niToJ3dData.get(niTriShape);
						j3dNiSkinInstances.add(new J3dNiSkinInstance(niSkinInstance, j3dNiTriShape, niToJ3dData, nifJ3dSkeletonRoot));
					}
					else
					{
						System.out.println("What the hell non trishape has skin instance!!");
					}
				}
			}
		}

		return j3dNiSkinInstances;
	}

}