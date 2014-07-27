package nif.j3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.media.j3d.Group;

import nif.basic.NifPtr;
import nif.character.NifJ3dSkeletonRoot;
import nif.compound.NifSkinData;
import nif.compound.NifSkinPartition;
import nif.compound.NifSkinWeight;
import nif.niobject.NiAVObject;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import nif.niobject.NiSkinData;
import nif.niobject.NiSkinInstance;
import nif.niobject.NiSkinPartition;
import nif.niobject.NiTriShape;

public class J3dNiSkinInstance extends Group
{

	public static boolean showSkinBoneMarkers = false;

	//private HashMap<String, J3dNiNode> skinBones = new HashMap<String, J3dNiNode>();

	private ArrayList<J3dNiNode> skinBonesInOrder = new ArrayList<J3dNiNode>();

	private ArrayList<J3dNifSkinPartition> j3dNifPartitions = new ArrayList<J3dNifSkinPartition>();

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

		addChild(j3dNiTriShape);

		if (niSkinInstance.data.ref != -1)
		{
			NiSkinData niSkinData = (NiSkinData) niToJ3dData.get(niSkinInstance.data);
			//TODO: the skin transform probably needs to be used here too

			for (NifSkinData nsd : niSkinData.boneList)
			{
				//each vertex weight moves towards the bone I guess 
				//but how does it keep station with other vextexes?

				// let's just sop out what vetex 2229 does I have 4 bones and a total of 1
				// another random has 2 0.95 and 0.05

				//I have a skin weight here too?
				//nsd.rotation;
				//nsd.translation;
				//nsd.scale;
				for (NifSkinWeight vw : nsd.vertexWeights)
				{
					if (vw.index == 2228)
					{
						System.out.println("weight = " + vw.weight);
					}
				}
			}

		}
		else
		{
			System.out.println("J3dNiSkinInstance niSkinInstance.data == -1");
		}

		if (niSkinInstance.skinPartition.ref != -1)
		{
			NiSkinPartition niSkinPartition = (NiSkinPartition) niToJ3dData.get(niSkinInstance.skinPartition);

			skinSkeletonRoot = niToJ3dData.get((NiAVObject) niToJ3dData.get(niSkinInstance.skeletonRoot));
			skinSkeletonRoot.setVisualMarker(showSkinBoneMarkers);

			//add bones to list
			for (NifPtr p : niSkinInstance.bones)
			{
				if (p.ptr != -1)
				{
					NiNode n = (NiNode) niToJ3dData.get(p);
					J3dNiNode skinBone = (J3dNiNode) niToJ3dData.get(n);
					skinBone.setVisualMarker(showSkinBoneMarkers);

					//skinBones.put(n.name, skinBone);
					skinBonesInOrder.add(skinBone);

					J3dNiNode skeletonBone = (J3dNiNode) allSkeletonBones.get(n.name);
					skeletonBones.put(n.name, skeletonBone);
				}
			}

			for (NifSkinPartition nifSkinPartition : niSkinPartition.skinPartitionBlocks)
			{
				j3dNifPartitions.add(new J3dNifSkinPartition(nifSkinPartition, j3dNiTriShape, skinSkeletonRoot, skinBonesInOrder,
						skeletonBones));
			}
		}

	}

	public void processSkinPartitions()
	{
		for (J3dNifSkinPartition j3dNifPartition : j3dNifPartitions)
		{
			j3dNifPartition.updateSkin();
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