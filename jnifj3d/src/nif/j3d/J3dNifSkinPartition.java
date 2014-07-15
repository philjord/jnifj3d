package nif.j3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;
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
			
			
			
			Point3d tester = new Point3d();
			System.out.println("skinBone " +skinBone.getName());
			
			System.out.println("de " + skinBoneVWInvTrans.getBestType() );
			skinBoneVWInvTrans.transform(tester);
			System.out.println("tester " +tester );
			Matrix4f mat = new Matrix4f();
			skinBoneVWInvTrans.get(mat);;
			System.out.println("mat="+mat);
			
			
			// determinatns should be near 1 otherwise inverting make crazy big transfomrs!
			if( Math.abs(1-skinBoneVWInvTrans.determinant() )>0.2)
			{				
				System.out.println("problem! determinants must be near 1 or invert fails " + skinBoneVWInvTrans.determinant());
				skinBoneVWInvTrans.determinant() ;				
			}
			skinBoneVWInvTrans.transform(tester);
			System.out.println("dist1 " +tester.distance(new Point3d()));
			 
			
			if(skinBone.getName().equals("Bip01 L Finger3") )
			{
				System.out.println("this one");
			}
			
			
			
			
			skinBoneVWInvTrans.invert();
			skinBonesVWInvTransInOrder[spBoneId] = skinBoneVWInvTrans;

			tester = new Point3d();
			skinBoneVWInvTrans.transform(tester);
			System.out.println("dist2 " +tester.distance(new Point3d()));
			
			if(  tester.distance(new Point3d())>3.6)
			{
				 System.out.println("spBoneId "+spBoneId);
			}
			
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
			
			
		/*	Point3d tester = new Point3d();
			accumulatedTrans.transform(tester);
			System.out.println("tester.distance(new Point3d()) " +tester.distance(new Point3d()));
			
			if(  tester.distance(new Point3d())>3.6)
			{
				System.out.println("tester goes far " + tester );
				skeletonBoneVWTrans.transform(tester);
				System.out.println("skeletonBoneVWTrans " + tester );
				skinBoneVWInvTrans.transform(tester);
				System.out.println("skinBoneVWInvTrans " + tester );
				 
				
				//accumulatedTrans.setIdentity();
				//accumulatedTrans.get(accTransMats[spBoneIndex]);
			}*/

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

		//	if (nifSkinPartition.numVertices == 649 && nifSkinPartition.numTriangles == 1487 && vm == 24)
			{
				//System.out.println("start here ");

				// spBoneIndex 4 appear to be screwing me up, it just transfromed base to crazy value
				// spBoneIndex 11 is not so crazy!
			}

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
			
			
			//////////////////////////////////////////////////////////////////////////////////////

		/*	Point3d cp = new Point3d(accumPointx, accumPointy, accumPointz);
			Point3d bp = new Point3d(basePointx, basePointy, basePointz);
			if (bp.distance(cp) > 2)
			{
				//if(nifSkinPartition.numVertices!=649 && nifSkinPartition.numTriangles!=1487 )
				{
					System.out.println("distant " + bp.distance(cp));

					// I am point(0.18, 5.5 1.27)
					//vertexes base and current: prev = 1672 - 5016 (17,18) point(0.41,3.48,0.28)
					//vertexes base and current: next = 1804 - 5412 (13,14)

					// this vm (24) use bones 4,11 which are  bone 7 and 15
				}
			}*/

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
