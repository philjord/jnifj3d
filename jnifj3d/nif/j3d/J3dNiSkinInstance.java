package nif.j3d;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Group;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;

import nif.basic.NifPtr;
import nif.character.NifJ3dSkeletonRoot;
import nif.compound.NifSkinPartition;
import nif.niobject.NiAVObject;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import nif.niobject.NiSkinInstance;
import nif.niobject.NiSkinPartition;
import nif.niobject.NiTriShape;
import tools3d.utils.scenegraph.Unsharable;

public class J3dNiSkinInstance extends Group implements Unsharable
{

	public static boolean showSkinBoneMarkers = false;

	//private HashMap<String, J3dNiNode> skinBones = new HashMap<String, J3dNiNode>();

	private ArrayList<J3dNiNode> skinBonesInOrder = new ArrayList<J3dNiNode>();

	private ArrayList<J3dNifSkinPartition> j3dNifPartitions = new ArrayList<J3dNifSkinPartition>();

	private LinkedHashMap<String, J3dNiNode> skeletonBones = new LinkedHashMap<String, J3dNiNode>();

	private J3dNiAVObject skinSkeletonRoot;

	private UpdateLastPerFrameBehavior behave = new UpdateLastPerFrameBehavior();

	public J3dNiSkinInstance(NiSkinInstance niSkinInstance, J3dNiTriShape j3dNiTriShape, NiToJ3dData niToJ3dData,
			NifJ3dSkeletonRoot nifJ3dSkeletonRoot)
	{
		J3dNiDefaultAVObjectPalette allSkeletonBones = nifJ3dSkeletonRoot.getAllBonesInSkeleton();
		J3dNiAVObject skeletonNonAccumRoot = nifJ3dSkeletonRoot.getNonAccumRoot();

		if (j3dNiTriShape.getParent() != null)
		{
			((Group) j3dNiTriShape.getParent()).removeChild(j3dNiTriShape);
		}

		addChild(j3dNiTriShape);

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
				j3dNifPartitions.add(new J3dNifSkinPartition(nifSkinPartition, j3dNiTriShape, skinSkeletonRoot, skeletonNonAccumRoot,
						skinBonesInOrder, skeletonBones));
			}
		}

		addChild(behave);
	}

	private void processSkinPartitions()
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

	/**
	 * This behaviour is simply called last per frame of update. It is last so the bone interpolators have
	 * finished moving the bones appropriately
	 * @author Administrator
	 *
	 */
	class UpdateLastPerFrameBehavior extends Behavior
	{
		private WakeupOnElapsedFrames passiveWakeupCriterion = new WakeupOnElapsedFrames(0, true);

		public UpdateLastPerFrameBehavior()
		{
			//TODO: this is set to 50 meters to try to limits animation at a distance
			// In fact it should LOD at distance and update after x frames, just like lodbillboard
			//see also UpdateBonesBehavior
			setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 20));
			//must happen last, so all bones updated by the time we use them
			this.setSchedulingInterval(Behavior.getNumSchedulingIntervals() - 1);
		}

		public void initialize()
		{
			wakeupOn(passiveWakeupCriterion);
		}

		@SuppressWarnings(
		{ "unchecked", "rawtypes" })
		public void processStimulus(Enumeration critiria)
		{
			processSkinPartitions();
			wakeupOn(passiveWakeupCriterion);
		}

	}

}