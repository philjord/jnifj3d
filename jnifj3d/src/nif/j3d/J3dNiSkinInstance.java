package nif.j3d;

import java.util.ArrayList;
import java.util.HashMap;

import org.jogamp.java3d.Group;
import org.jogamp.vecmath.Color3f;

import nif.basic.NifPtr;
import nif.character.NifJ3dSkeletonRoot;
import nif.niobject.NiAVObject;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiSkinData;
import nif.niobject.NiSkinInstance;
import nif.niobject.NiTriShape;
import nif.niobject.bs.BSSkin;
import nif.niobject.bs.BSSubIndexTriShape;
import nif.niobject.bs.BSTriShape;
import tools3d.utils.scenegraph.Fadable;

public class J3dNiSkinInstance extends Group implements Fadable
{
	public static boolean showSkinBoneMarkers = false;

	protected J3dNiTriBasedGeom j3dNiTriBasedGeom;

	protected J3dSkin j3dSkin;

	protected J3dNiAVObject skinSkeletonRoot;
	
	protected J3dNiSkinInstance(){	             	}
	
	public J3dNiSkinInstance(NiSkinInstance niSkinInstance, J3dNiTriShape j3dNiTriShape, NiToJ3dData niToJ3dData,
			NifJ3dSkeletonRoot nifJ3dSkeletonRoot)
	{
		this.j3dNiTriBasedGeom = j3dNiTriShape;
		J3dNiDefaultAVObjectPalette allSkeletonBones = nifJ3dSkeletonRoot.getAllBonesInSkeleton();

		if (j3dNiTriShape.getParent() != null)
		{
			((Group) j3dNiTriShape.getParent()).removeChild(j3dNiTriShape);
		}

		skinSkeletonRoot = niToJ3dData.get((NiAVObject) niToJ3dData.get(niSkinInstance.skeletonRoot));
		skinSkeletonRoot.setVisualMarker(showSkinBoneMarkers);

		addChild(j3dNiTriShape);

		//add bones to list
		J3dNiNode[] skinBonesInOrder = new J3dNiNode[niSkinInstance.bones.length];
		HashMap<String, J3dNiNode> skeletonBones = new HashMap<String, J3dNiNode>();

		for (int boneIdx = 0; boneIdx < niSkinInstance.bones.length; boneIdx++)
		{
			NifPtr p = niSkinInstance.bones[boneIdx];
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

		if (niSkinInstance.data.ref != -1 && true)
		{
			NiSkinData niSkinData = (NiSkinData) niToJ3dData.get(niSkinInstance.data);
			j3dSkin = new J3dNifSkinData(niSkinData, j3dNiTriShape, skinBonesInOrder, skeletonBones);
		}

	}

	public J3dNiTriBasedGeom getJ3dNiTriShape()
	{
		return j3dNiTriBasedGeom;
	}

	public void processSkinInstance()
	{
		if (j3dSkin != null)
		{
			j3dSkin.updateSkin();
		}
	}

	@Override
	public void fade(float percent)
	{
		if (j3dSkin != null)
		{
			j3dSkin.fade(percent);
		}
	}

	@Override
	public void setOutline(Color3f c)
	{
		if (j3dSkin != null)
		{
			j3dSkin.setOutline(c);
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
					NiObject skinRef = niToJ3dData.get(niGeometry.skin);
					if(skinRef instanceof NiSkinInstance) {						 
						
						NiSkinInstance niSkinInstance = (NiSkinInstance) skinRef;
	
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
					} else if (skinRef instanceof BSSkin.Instance) {
						BSSkin.Instance bsSkinInstance = (BSSkin.Instance) skinRef;
						
						if (niGeometry instanceof BSSubIndexTriShape) {
							BSSubIndexTriShape niTriShape = (BSSubIndexTriShape) niGeometry;
							J3dBSTriShape j3dNiTriShape = (J3dBSTriShape) niToJ3dData.get(niTriShape);
							j3dNiSkinInstances.add(new J3dBsSkinInstance(bsSkinInstance, j3dNiTriShape, niToJ3dData, nifJ3dSkeletonRoot));
						} else if (niGeometry instanceof BSTriShape) {
							BSTriShape niTriShape = (BSTriShape) niGeometry;
							J3dBSTriShape j3dNiTriShape = (J3dBSTriShape) niToJ3dData.get(niTriShape);
							j3dNiSkinInstances.add(new J3dBsSkinInstance(bsSkinInstance, j3dNiTriShape, niToJ3dData, nifJ3dSkeletonRoot));
						} else {						
							System.out.println("What the hell non BSSubIndexTriShape and non BSTriShape has BSSkin.Instance!! " +niGeometry);
						}
					}
				}
			}
		}

		return j3dNiSkinInstances;
	}

}