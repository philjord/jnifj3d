package nif.j3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;

import nif.compound.NifSkinPartition;

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

	public J3dNifSkinPartition(NifSkinPartition nifSkinPartition, J3dNiTriShape j3dNiTriShape, J3dNiAVObject skinSkeletonRoot,
			ArrayList<J3dNiNode> skinBonesInOrder, LinkedHashMap<String, J3dNiNode> skeletonBones)
	{
		this.nifSkinPartition = nifSkinPartition;

		//TODO: is this a good idea? thread show blocked on update bounds
		j3dNiTriShape.getShape().setBoundsAutoCompute(false);
		j3dNiTriShape.getShape().setBounds(new BoundingSphere(new Point3d(0, 0, 0), 10));

		j3dNiTriShape.getTreeTransform(shapeVWTrans, skinSkeletonRoot);
		shapeVWInvTrans.set(shapeVWTrans);
		shapeVWInvTrans.invert();

		skinBonesVWInvTransInOrder = new Transform3D[skinBonesInOrder.size()];
		for (int spBoneId = 0; spBoneId < skinBonesInOrder.size(); spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder.get(spBoneId);
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

		skeletonBonesInSkinBoneIdOrder = new J3dNiNode[skinBonesInOrder.size()];
		for (int spBoneId = 0; spBoneId < skinBonesInOrder.size(); spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder.get(spBoneId);
			J3dNiNode skeletonBone = skeletonBones.get(skinBone.getName());
			if (skeletonBone == null)
				System.out.println("Null bone! mixed games or creatures? " + skinBone.getName());
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
	 * @see javax.media.j3d.GeometryUpdater#updateData(javax.media.j3d.Geometry)
	 */
	@Override
	public void updateData(Geometry geometry)
	{
		double[][] accTransMats = new double[nifSkinPartition.bones.length][16];

		// pre multiply transforms for repeated use for each vertex
		
		//TODO: note I repate the exact same bones in different partitions on a given model
		//see oblivion dogbody.nif 34/35/36 are done twice
		// my new higherlevel start pose at skin instance may force this issue anyway
		

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

		

			//spider daedra bones
		/*	if (bone.getName().equals("Bip01 jaw") //
					|| bone.getName().equals("Bip01 Head") //
					|| bone.getName().equals("Bip01 Spine2")//
					|| bone.getName().equals("Bip01 L Breast")//
					|| bone.getName().equals("Bip01 R Breast"))
			
			// ok so dog and wolf have exact same issue, but the tail bones appear perfectly placed?
			if (bone.getName().equals("Bip01 NonAccum")
					|| bone.getName().equals("Bip01 Spine0")
					|| bone.getName().equals("Bip01 Pelvis")
					|| bone.getName().equals("Bip01 Tail")
					|| bone.getName().equals("Bip01 Tail1")
					|| bone.getName().equals("Bip01 Tail2"))*/
			
				

			//I think this jaw bone inverstion may NOT be related to eh bound rest pose
			if (bone.getName().equals("Bip01 jaw"))
			{

				//TODO: I need to flip the last littel bit of teh 

				//Confirmed that the jaw bone is wrong, however it's not a little bit of flipp that's needed
				// it's something more
			}

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

					//This confirm jaw bone screwed, replace with head and all good
					//	if( nifSkinPartition.numTriangles == 4310 &&spBoneIndex == 15 )
					//	spBoneIndex = 0;

					//This show jaw bone is just flipped over simple x rotate of 180?
					//	if( nifSkinPartition.numTriangles == 4310 &&spBoneIndex == 0 )
					//	spBoneIndex = 15;
					
					// one of dog tail
					//if( nifSkinPartition.numTriangles == 1487 && spBoneIndex == 11 )
					//	spBoneIndex = 3;

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
				/*else if (weight < 0)
				{
				// doesn't appear to ever happen
					System.out.println("if (weight < 0) " +weight);
				}*/
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
