package nif.j3d;

import java.util.LinkedHashMap;

import org.jogamp.java3d.Geometry;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.GeometryUpdater;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;

import nif.compound.NifSkinPartition;
/**
 * superceeded by the skindata version, this is an extra on top of that, which I don't need
 * this does not have enough info in it to work alone
 */
@Deprecated
public class J3dNifSkinPartition extends Group implements GeometryUpdater
{
	private NifSkinPartition nifSkinPartition;

	private J3dNiNode[] skeletonBonesInSkinBoneIdOrder;//prelookups

	private GeometryArray baseIndexedGeometryArray;

	private GeometryArray currentIndexedGeometryArray;

	private Transform3D shapeVWTrans = new Transform3D();

	private Transform3D shapeVWInvTrans = new Transform3D();

	private Transform3D[] skinBonesVWInvTransInOrder;

	private Transform3D[] accumulatedTransByBonesIndex;

	/**
	 * superceeded by the skindata version, this is an extra on top of that, which I don't need
	 * this does not have enough info in it to work alone
	 * @param nifSkinPartition
	 * @param j3dNiTriShape
	 * @param skinSkeletonRoot
	 * @param skinBonesInOrder
	 * @param skeletonBones
	 */
	public J3dNifSkinPartition(NifSkinPartition nifSkinPartition, J3dNiTriShape j3dNiTriShape, J3dNiAVObject skinSkeletonRoot,
			J3dNiNode[] skinBonesInOrder, LinkedHashMap<String, J3dNiNode> skeletonBones)
	{
		this.nifSkinPartition = nifSkinPartition;

		j3dNiTriShape.getTreeTransform(shapeVWTrans, skinSkeletonRoot);
		shapeVWInvTrans.set(shapeVWTrans);
		shapeVWInvTrans.invert();

		skinBonesVWInvTransInOrder = new Transform3D[skinBonesInOrder.length];
		for (int spBoneId = 0; spBoneId < skinBonesInOrder.length; spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder[spBoneId];
			Transform3D skinBoneVWInvTrans = new Transform3D();
			skinBone.getTreeTransform(skinBoneVWInvTrans, skinSkeletonRoot);

			skinBoneVWInvTrans.invert();

			skinBonesVWInvTransInOrder[spBoneId] = skinBoneVWInvTrans;
		}

		currentIndexedGeometryArray = j3dNiTriShape.getCurrentGeometryArray();
		baseIndexedGeometryArray = j3dNiTriShape.getBaseGeometryArray();

		//prep an accumulation transform set for reuse in the updater
		accumulatedTransByBonesIndex = new Transform3D[nifSkinPartition.bones.length];
		for (int spBoneIndex = 0; spBoneIndex < nifSkinPartition.bones.length; spBoneIndex++)
		{
			accumulatedTransByBonesIndex[spBoneIndex] = new Transform3D();
		}

		skeletonBonesInSkinBoneIdOrder = new J3dNiNode[skinBonesInOrder.length];
		for (int spBoneId = 0; spBoneId < skinBonesInOrder.length; spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder[spBoneId];
			J3dNiNode skeletonBone = skeletonBones.get(skinBone.getName());
			if (skeletonBone == null)
				System.out.println("Null bone! mixed games or creatures? or is it a case issue? " + skinBone.getName());
			skeletonBonesInSkinBoneIdOrder[spBoneId] = skeletonBone;
		}

	}

	public void updateSkin()
	{
		currentIndexedGeometryArray.updateData(this);
	}

	// for reuse inside loop
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	/**
	 * Mega optomised, natural copy below
	 * @see org.jogamp.java3d.GeometryUpdater#updateData(org.jogamp.java3d.Geometry)
	 */
	@Override
	public void updateData(Geometry geometry)
	{
		double[][] accTransMats = new double[nifSkinPartition.bones.length][16];

		// pre multiply transforms for repeated use for each vertex
		for (int spBoneIndex = 0; spBoneIndex < nifSkinPartition.bones.length; spBoneIndex++)
		{
			int spBoneId = nifSkinPartition.bones[spBoneIndex];

			J3dNiNode bone = skeletonBonesInSkinBoneIdOrder[spBoneId];

			//mismatched kf and skin? already output above, don't spam here		
			if (bone == null)
			{
				return;
			}
			// this accumed has been just updated in the bone update behavior
			skeletonBoneVWTrans.set(bone.getBoneCurrentAccumedTrans());

			Transform3D skinBoneVWInvTrans = skinBonesVWInvTransInOrder[spBoneId];

			Transform3D accumulatedTrans = accumulatedTransByBonesIndex[spBoneIndex];

			accumulatedTrans.set(shapeVWInvTrans);
			accumulatedTrans.mul(skeletonBoneVWTrans);
			accumulatedTrans.mul(skinBoneVWInvTrans);
			accumulatedTrans.mul(shapeVWTrans);

			accumulatedTrans.get(accTransMats[spBoneIndex]);
		}

		float[] baseCoordRefFloat = baseIndexedGeometryArray.getCoordRefFloat();
		float[] currentCoordRefFloat = currentIndexedGeometryArray.getCoordRefFloat();

		// now go through each vertex and find out where it should be
		for (int vm = 0; vm < nifSkinPartition.vertexMap.length; vm++)
		{
			float accumPointx = 0;
			float accumPointy = 0;
			float accumPointz = 0;
			int vIdx = nifSkinPartition.vertexMap[vm];

			float basePointx = baseCoordRefFloat[vIdx * 3 + 0];
			float basePointy = baseCoordRefFloat[vIdx * 3 + 1];
			float basePointz = baseCoordRefFloat[vIdx * 3 + 2];

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

					double[] accTransMat = accTransMats[spBoneIndex];

					// set the transformed point from base, ready for transformation
					float transformedPointx = basePointx;
					float transformedPointy = basePointy;
					float transformedPointz = basePointz;

					// transform point
					float x = (float) (accTransMat[0] * transformedPointx + accTransMat[1] * transformedPointy + accTransMat[2]
							* transformedPointz + accTransMat[3]);
					float y = (float) (accTransMat[4] * transformedPointx + accTransMat[5] * transformedPointy + accTransMat[6]
							* transformedPointz + accTransMat[7]);
					transformedPointz = (float) (accTransMat[8] * transformedPointx + accTransMat[9] * transformedPointy + accTransMat[10]
							* transformedPointz + accTransMat[11]);
					transformedPointx = x;
					transformedPointy = y;

					//scale by the weight of the bone
					transformedPointx *= weight;
					transformedPointy *= weight;
					transformedPointz *= weight;
					// accumulate in the final point
					accumPointx += transformedPointx;
					accumPointy += transformedPointy;
					accumPointz += transformedPointz;
					// record running total weight
					totalWeight += weight;
				}
			}
			if (totalWeight != 1)
			{
				// only ever see tiny diffs 1.001 or 0.99994 so probably ok
				//if (Math.abs(1 - totalWeight) > 0.01)
				//System.out.println("bad total weight! " + totalWeight);
			}

			currentCoordRefFloat[vIdx * 3 + 0] = accumPointx;
			currentCoordRefFloat[vIdx * 3 + 1] = accumPointy;
			currentCoordRefFloat[vIdx * 3 + 2] = accumPointz;

		}

	}
	/*
	 * this is natural copy of mega optomised code eabove
	 * 
		private Point3f basePoint = new Point3f();

		private Point3f accumPoint = new Point3f();

		private Point3f transformedPoint = new Point3f();
		
		J3dNiAVObject	skeletonNonAccumRoot;
		add 	skeletonNonAccumRoot = skinSkeletonRoot; in constructor

		@Override
		public void updateData(Geometry geometry)
		{
			float[] baseCoordRefFloat = baseIndexedGeometryArray.getCoordRefFloat();
			float[] currentCoordRefFloat = currentIndexedGeometryArray.getCoordRefFloat();

			// pre multiply transforms for repeated use for each vertex
			for (int spBoneIndex = 0; spBoneIndex < nifSkinPartition.bones.length; spBoneIndex++)
			{
				int spBoneId = nifSkinPartition.bones[spBoneIndex];

				J3dNiNode skeletonBone = skeletonBonesInSkinBoneIdOrder[spBoneId];
				skeletonBone.getTreeTransform(skeletonBoneVWTrans, skeletonNonAccumRoot);

				Transform3D skinBoneVWInvTrans = skinBonesVWInvTransInOrder[spBoneId];

				Transform3D accumulatedTrans = accumulatedTransByBonesIndex[spBoneIndex];

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
				//float totalWeight = 0;

				for (int w = 0; w < nifSkinPartition.numWeightsPerVertex; w++)
				{
					float weight = nifSkinPartition.vertexWeights[vm][w];

					// If this bone has any effect add it in 
					if (weight > 0)
					{
						// find the transform for this bone
						int spBoneIndex = nifSkinPartition.boneIndices[vm][w];
						Transform3D accumulatedTrans = accumulatedTransByBonesIndex[spBoneIndex];

						// set the transformed point from base, ready for transformation
						transformedPoint.set(basePoint);
						// transform point
						accumulatedTrans.transform(transformedPoint);
						//scale by the weight of the bone
						transformedPoint.scale(weight);
						// accumulate in the final point
						accumPoint.add(transformedPoint);
						// record running total weight
						//totalWeight += weight;
					}
				}

				currentCoordRefFloat[vIdx * 3 + 0] = accumPoint.x;
				currentCoordRefFloat[vIdx * 3 + 1] = accumPoint.y;
				currentCoordRefFloat[vIdx * 3 + 2] = accumPoint.z;
			}

		}*/

}
