package nif.j3d;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;

import tools3d.utils.scenegraph.Unsharable;

import nif.compound.NifSkinPartition;

public class J3dNifSkinPartition extends Group implements Unsharable
{
	private NifSkinPartition nifSkinPartition;

	private J3dNiAVObject skeletonNonAccumRoot;

	private ArrayList<J3dNiNode> skinBonesInOrder;

	private HashMap<String, J3dNiNode> skeletonBones;

	private IndexedGeometryArray baseIndexedGeometryArray;

	private IndexedGeometryArray currentIndexedGeometryArray;

	private Transform3D shapeVWTrans = new Transform3D();

	private Transform3D shapeVWInvTrans = new Transform3D();

	private ArrayList<Transform3D> skinBonesVWInvTransInOrder = new ArrayList<Transform3D>();

	private ArrayList<Transform3D> accumulatedTransByBonesIndex = new ArrayList<Transform3D>();

	private SkinGeomteryUpdater skinGeomteryUpdater = new SkinGeomteryUpdater();

	public J3dNifSkinPartition(NifSkinPartition nifSkinPartition, J3dNiTriShape j3dNiTriShape, J3dNiAVObject skinSkeletonRoot,
			J3dNiAVObject skeletonNonAccumRoot, ArrayList<J3dNiNode> skinBonesInOrder, HashMap<String, J3dNiNode> skeletonBones)
	{
		this.nifSkinPartition = nifSkinPartition;
		this.skeletonNonAccumRoot = skeletonNonAccumRoot;
		this.skinBonesInOrder = skinBonesInOrder;
		this.skeletonBones = skeletonBones;

		j3dNiTriShape.getTreeTransform(shapeVWTrans, skinSkeletonRoot);
		shapeVWInvTrans.set(shapeVWTrans);
		shapeVWInvTrans.invert();

		for (J3dNiNode skinBone : skinBonesInOrder)
		{
			Transform3D skinBoneVWInvTrans = new Transform3D();
			skinBone.getTreeTransform(skinBoneVWInvTrans, skinSkeletonRoot);
			skinBoneVWInvTrans.invert();

			skinBonesVWInvTransInOrder.add(skinBoneVWInvTrans);
		}

		currentIndexedGeometryArray = j3dNiTriShape.getCurrentIndexedGeometryArray();
		baseIndexedGeometryArray = j3dNiTriShape.getBaseIndexedGeometryArray();

		//prep an accumulation transform set for reuse in the updater
		for (int spBoneIndex = 0; spBoneIndex < nifSkinPartition.bones.length; spBoneIndex++)
		{
			accumulatedTransByBonesIndex.add(new Transform3D());
		}

	}

	public void updateSkin()
	{
		currentIndexedGeometryArray.updateData(skinGeomteryUpdater);
	}

	private class SkinGeomteryUpdater implements GeometryUpdater
	{
		// for reuse inside loop
		private Transform3D skeletonBoneVWTrans = new Transform3D();

		private Point3f basePoint = new Point3f();

		private Point3f accumPoint = new Point3f();

		private Point3f transformedPoint = new Point3f();

		public void updateData(Geometry geometry)
		{
			float[] baseCoordRefFloat = baseIndexedGeometryArray.getCoordRefFloat();
			float[] currentCoordRefFloat = currentIndexedGeometryArray.getCoordRefFloat();

			// pre multiply transforms for repeated use for each vertex
			for (int spBoneIndex = 0; spBoneIndex < nifSkinPartition.bones.length; spBoneIndex++)
			{
				int spBoneId = nifSkinPartition.bones[spBoneIndex];

				J3dNiNode skinBone = skinBonesInOrder.get(spBoneId);
				Transform3D skinBoneVWInvTrans = skinBonesVWInvTransInOrder.get(spBoneId);

				//TODO: can we not pre look these up to id?
				//This is where multiple skeletonbone could be used
				J3dNiNode skeletonBone = skeletonBones.get(skinBone.getName());

				skeletonBone.getTreeTransform(skeletonBoneVWTrans, skeletonNonAccumRoot);

				Transform3D accumulatedTrans = accumulatedTransByBonesIndex.get(spBoneIndex);

				accumulatedTrans.set(shapeVWInvTrans);
				accumulatedTrans.mul(skeletonBoneVWTrans);
				accumulatedTrans.mul(skinBoneVWInvTrans);
				accumulatedTrans.mul(shapeVWTrans);

			}

			// now go through each vertex and find out where it should be
			for (int vm = 0; vm < nifSkinPartition.vertexMap.length; vm++)
			{
				accumPoint.set(0, 0, 0);
				int vIdx = nifSkinPartition.vertexMap[vm];

				basePoint.x = baseCoordRefFloat[vIdx * 3 + 0];
				basePoint.y = baseCoordRefFloat[vIdx * 3 + 1];
				basePoint.z = baseCoordRefFloat[vIdx * 3 + 2];

				// not used currently, but might be needed for a final scaling?
				float totalWeight = 0;

				for (int w = 0; w < nifSkinPartition.numWeightsPerVertex; w++)
				{
					float weight = nifSkinPartition.vertexWeights[vm][w];

					// If this bone has any effect add it in 
					if (weight > 0)
					{
						// find the transform for this bone
						int spBoneIndex = nifSkinPartition.boneIndices[vm][w];
						Transform3D accumulatedTrans = accumulatedTransByBonesIndex.get(spBoneIndex);

						// set the transformed point from base, ready for transformation
						transformedPoint.set(basePoint);
						// transform point
						accumulatedTrans.transform(transformedPoint);
						//scale by the weight of the bone
						transformedPoint.scale(weight);
						// accumulate in the final point
						accumPoint.add(transformedPoint);
						// record running total weight
						totalWeight += weight;
					}
				}

				currentCoordRefFloat[vIdx * 3 + 0] = accumPoint.x;
				currentCoordRefFloat[vIdx * 3 + 1] = accumPoint.y;
				currentCoordRefFloat[vIdx * 3 + 2] = accumPoint.z;
			}
		}

	}

}
