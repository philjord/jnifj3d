package nif.j3d;

import java.nio.FloatBuffer;
import java.util.HashMap;

import org.jogamp.java3d.Geometry;
import org.jogamp.java3d.Transform3D;

import nif.character.NifCharacter;
import nif.compound.BSSkinBoneTrans;
import nif.niobject.bs.BSSkin.BoneData;
import nif.niobject.bs.BSTriShape;
import utils.convert.ConvertFromNif;

public class J3dBSSkin extends J3dSkin
{
	private BoneData niSkinData;
 

	public J3dBSSkin(BoneData niSkinData, J3dBSTriShape j3dBSTriShape, J3dNiNode[] skinBonesInOrder,
			HashMap<String, J3dNiNode> skeletonBones)
	{
		//http://sourceforge.net/p/niftools/niflib/ci/0b2d0541c5a17af892ab2f416acbbfd2fdc369b2/tree/src/obj/NiSkinData.cpp

		// TODO: head of dog still has gaps? why?
		// TODO: possibly also proper undertand of non accum etc
		// TODO: ant feelers in fallout still flick the wrong way?
		// TODO: hahaa spider daedra jaw still backwards in castself
		// TODO: deathclaw skin totally rooted up

		this.niSkinData = niSkinData;
		this.j3dNiTriBasedGeom = j3dBSTriShape;

		//skinDataTrans.setRotation(ConvertFromNif.toJ3d(niSkinData.nifSkinTransform.rotation));
		//skinDataTrans.setTranslation(ConvertFromNif.toJ3d(niSkinData.nifSkinTransform.translation));
		//skinDataTrans.setScale(niSkinData.nifSkinTransform.scale);

		skinBonesSkinOffsetInOrder = new Transform3D[niSkinData.Bones.length];
		skeletonBonesInSkinBoneIdOrder = new J3dNiNode[niSkinData.Bones.length];
		for (int spBoneId = 0; spBoneId < niSkinData.Bones.length; spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder[spBoneId];

			BSSkinBoneTrans boneOffsetTransform = niSkinData.Bones[spBoneId];
			Transform3D boneOffsetTrans = new Transform3D();
			boneOffsetTrans.setRotation(ConvertFromNif.toJ3d(boneOffsetTransform.Rotation));
			boneOffsetTrans.setTranslation(ConvertFromNif.toJ3d(boneOffsetTransform.Translation));
			boneOffsetTrans.setScale(boneOffsetTransform.Scale);
			skinBonesSkinOffsetInOrder[spBoneId] = boneOffsetTrans;

			J3dNiNode skeletonBone = skeletonBones.get(skinBone.getName());
			//bow bones are sometime not present
			if (skeletonBone == null && !skinBone.getName().startsWith("Bow_"))
				System.out.println("Null bone! mixed games or creatures? " + skinBone.getName());
			skeletonBonesInSkinBoneIdOrder[spBoneId] = skeletonBone;

		}

		currentIndexedGeometryArray = j3dBSTriShape.getCurrentGeometryArray();
		baseIndexedGeometryArray = j3dBSTriShape.getBaseGeometryArray();

	}

	  

	// for reuse inside loop
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	//reused in loop
	private Transform3D accumulatorTrans = new Transform3D();

	private float[] currentCoordRefFloatbf;	
	private static float[] currentCoordRefFloatbfClearer = new float[40000];// is 20k big enough?
	private float[] baseCoordRefFloatbf;

	@Override
	public void updateData(Geometry geometry)
	{
		// holder of the transform data to speed up transform (possibly)
		double[] accTransMat = new double[16];

		FloatBuffer baseCoordRefFloat = (FloatBuffer) baseIndexedGeometryArray.getCoordRefBuffer().getBuffer();
		FloatBuffer currentCoordRefFloat = (FloatBuffer) currentIndexedGeometryArray.getCoordRefBuffer().getBuffer();

		if (NifCharacter.BULK_BUFFER_UPDATES)
		{
			// let's try bulk get/set
			if (baseCoordRefFloatbf == null || baseCoordRefFloatbf.length != baseCoordRefFloat.limit())
			{
				baseCoordRefFloatbf = new float[baseCoordRefFloat.limit()];
				baseCoordRefFloat.position(0);
				baseCoordRefFloat.get(baseCoordRefFloatbf);
			}
			if (currentCoordRefFloatbf == null || currentCoordRefFloatbf.length != currentCoordRefFloat.limit())
			{
				currentCoordRefFloatbf = new float[currentCoordRefFloat.capacity()];				
				currentCoordRefFloat.position(0);
				currentCoordRefFloat.get(currentCoordRefFloatbf);
			}
			//clear out current in order to accum into it
			System.arraycopy(currentCoordRefFloatbfClearer, 0, currentCoordRefFloatbf, 0, currentCoordRefFloatbf.length);
		}
		else			
		{
			//clear out current in order to accum into it
			// sadly no real bulk operation here :(
			for (int i = 0; i < currentCoordRefFloat.limit(); i++)
			{
				currentCoordRefFloat.put(i, 0);
			}
		}
		
		// pre multiply transforms for repeated use for each vertex
		for (int spBoneId = 0; spBoneId < niSkinData.Bones.length; spBoneId++)
		{
			J3dNiNode skeletonBone = skeletonBonesInSkinBoneIdOrder[spBoneId];

			//mismatched kf and skin? already output above, don't spam here		
			if (skeletonBone == null)
			{
				continue;
			}

			// this getBoneCurrentAccumedTrans has been just updated in the bone update behavior
			skeletonBoneVWTrans.set(skeletonBone.getBoneCurrentAccumedTrans());

			Transform3D nifSkinTransformTrans = skinBonesSkinOffsetInOrder[spBoneId];

			accumulatorTrans.set(skinDataTrans);
			accumulatorTrans.mul(skeletonBoneVWTrans);
			accumulatorTrans.mul(nifSkinTransformTrans);

			// get accumulatorTrans out to a stright float [] to speed up transform (possibly)
			accumulatorTrans.get(accTransMat);

			// apply it's effect to it's dependant vertices
			
			// bone weights for skin data are part of BSTriShape
		
			J3dBSTriShape j3dBSTriShape = (J3dBSTriShape)j3dNiTriBasedGeom;
			//float[] BoneWeights;
			//int[] BoneIndices;
			BSTriShape BSTriShape = (BSTriShape)j3dBSTriShape.niAVObject;
			int[] boneIndex = BSTriShape.BoneIndices;
			float[] boneWeight = BSTriShape.BoneWeights;
			 
			//NifSkinData nsd = niSkinData.Bones[spBoneId];

			if (NifCharacter.BULK_BUFFER_UPDATES)
			{
				for (int i = 0 ; i < boneIndex.length; i++)
				{
					int vIdx = boneIndex[i];
					float weight = boneWeight[i];
					// If this bone has any effect add it in 
					if (weight > 0)
					{
						float px = baseCoordRefFloatbf[vIdx * 3 + 0];
						float py = baseCoordRefFloatbf[vIdx * 3 + 1];
						float pz = baseCoordRefFloatbf[vIdx * 3 + 2];

						// transform point by using code from Transform3D.transform(Point3f) to speed up transform (possibly)
						float x = (float) (accTransMat[0] * px + accTransMat[1] * py + accTransMat[2] * pz + accTransMat[3]);
						float y = (float) (accTransMat[4] * px + accTransMat[5] * py + accTransMat[6] * pz + accTransMat[7]);
						pz = (float) (accTransMat[8] * px + accTransMat[9] * py + accTransMat[10] * pz + accTransMat[11]);
						px = x;
						py = y;

						//scale by the weight of the bone
						px *= weight;
						py *= weight;
						pz *= weight;

						// accumulate into the output
						currentCoordRefFloatbf[vIdx * 3 + 0] += px;
						currentCoordRefFloatbf[vIdx * 3 + 1] += py;
						currentCoordRefFloatbf[vIdx * 3 + 2] += pz;
					}

				}
				
			}
			else
			{
				for (int i = 0 ; i < boneIndex.length; i++)
				{
					int vIdx = boneIndex[i];
					float weight = boneWeight[i];
					// If this bone has any effect add it in 
					if (weight > 0)
					{
						float px = baseCoordRefFloat.get(vIdx * 3 + 0);
						float py = baseCoordRefFloat.get(vIdx * 3 + 1);
						float pz = baseCoordRefFloat.get(vIdx * 3 + 2);

						// transform point by using code from Transform3D.transform(Point3f) to speed up transform (possibly)
						float x = (float) (accTransMat[0] * px + accTransMat[1] * py + accTransMat[2] * pz + accTransMat[3]);
						float y = (float) (accTransMat[4] * px + accTransMat[5] * py + accTransMat[6] * pz + accTransMat[7]);
						pz = (float) (accTransMat[8] * px + accTransMat[9] * py + accTransMat[10] * pz + accTransMat[11]);
						px = x;
						py = y;

						//scale by the weight of the bone
						px *= weight;
						py *= weight;
						pz *= weight;

						// accumulate into the output
						currentCoordRefFloat.put(vIdx * 3 + 0, currentCoordRefFloat.get(vIdx * 3 + 0) + px);
						currentCoordRefFloat.put(vIdx * 3 + 1, currentCoordRefFloat.get(vIdx * 3 + 1) + py);
						currentCoordRefFloat.put(vIdx * 3 + 2, currentCoordRefFloat.get(vIdx * 3 + 2) + pz);
					}

				}
			}
		}
		
		if (NifCharacter.BULK_BUFFER_UPDATES)
		{
			currentCoordRefFloat.position(0);
			currentCoordRefFloat.put(currentCoordRefFloatbf);
		}
	}

}
