package nif.j3d;

import java.nio.FloatBuffer;
import java.util.LinkedHashMap;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;

import nif.character.NifCharacter;
import nif.compound.NifSkinData;
import nif.compound.NifSkinTransform;
import nif.compound.NifSkinWeight;
import nif.niobject.NiSkinData;
import tools3d.utils.scenegraph.Fadable;
import utils.convert.ConvertFromNif;

public class J3dNifSkinData extends Group implements GeometryUpdater, Fadable
{
	private NiSkinData niSkinData;

	private J3dNiNode[] skeletonBonesInSkinBoneIdOrder;//prelookups

	private GeometryArray baseIndexedGeometryArray;

	private GeometryArray currentIndexedGeometryArray;

	private Transform3D skinDataTrans = new Transform3D();

	private Transform3D[] skinBonesSkinOffsetInOrder;

	private J3dNiTriShape j3dNiTriShape;

	public J3dNifSkinData(NiSkinData niSkinData, J3dNiTriShape j3dNiTriShape, J3dNiNode[] skinBonesInOrder,
			LinkedHashMap<String, J3dNiNode> skeletonBones)
	{
		//http://sourceforge.net/p/niftools/niflib/ci/0b2d0541c5a17af892ab2f416acbbfd2fdc369b2/tree/src/obj/NiSkinData.cpp

		// TODO: head of dog still has gaps? why?
		// TODO: possibly also proper undertand of non accum etc
		// TODO: ant feelers in fallout still flick the wrong way?
		// TODO: hahaa spider daedra jaw still backwards in castself
		// TODO: deathclaw skin totally rooted up

		this.niSkinData = niSkinData;
		this.j3dNiTriShape = j3dNiTriShape;

		skinDataTrans.setRotation(ConvertFromNif.toJ3d(niSkinData.nifSkinTransform.rotation));
		skinDataTrans.setTranslation(ConvertFromNif.toJ3d(niSkinData.nifSkinTransform.translation));
		skinDataTrans.setScale(niSkinData.nifSkinTransform.scale);

		skinBonesSkinOffsetInOrder = new Transform3D[niSkinData.boneList.length];
		skeletonBonesInSkinBoneIdOrder = new J3dNiNode[niSkinData.boneList.length];
		for (int spBoneId = 0; spBoneId < niSkinData.boneList.length; spBoneId++)
		{
			J3dNiNode skinBone = skinBonesInOrder[spBoneId];

			NifSkinTransform boneOffsetTransform = niSkinData.boneList[spBoneId].nifSkinTransform;
			Transform3D boneOffsetTrans = new Transform3D();
			boneOffsetTrans.setRotation(ConvertFromNif.toJ3d(boneOffsetTransform.rotation));
			boneOffsetTrans.setTranslation(ConvertFromNif.toJ3d(boneOffsetTransform.translation));
			boneOffsetTrans.setScale(boneOffsetTransform.scale);
			skinBonesSkinOffsetInOrder[spBoneId] = boneOffsetTrans;

			J3dNiNode skeletonBone = skeletonBones.get(skinBone.getName());
			//bow bones are sometime not present
			if (skeletonBone == null && !skinBone.getName().startsWith("Bow_"))
				System.out.println("Null bone! mixed games or creatures? " + skinBone.getName());
			skeletonBonesInSkinBoneIdOrder[spBoneId] = skeletonBone;

		}

		currentIndexedGeometryArray = j3dNiTriShape.getCurrentGeometryArray();
		baseIndexedGeometryArray = j3dNiTriShape.getBaseGeometryArray();

	}

	@Override
	public void fade(float percent)
	{
		j3dNiTriShape.fade(percent);
	}

	@Override
	public void setOutline(Color3f c)
	{
		j3dNiTriShape.setOutline(c);
	}

	public void updateSkin()
	{
		currentIndexedGeometryArray.updateData(this);
	}

	// for reuse inside loop
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	//reused in loop
	private Transform3D accumulatorTrans = new Transform3D();

	private float[] currentCoordRefFloatbf;
	private float[] baseCoordRefFloatbf;

	@Override
	public void updateData(Geometry geometry)
	{
		// holder of the transform data to speed up transform (possibly)
		double[] accTransMat = new double[16];

		FloatBuffer baseCoordRefFloat = (FloatBuffer) baseIndexedGeometryArray.getCoordRefBuffer().getBuffer();
		FloatBuffer currentCoordRefFloat = (FloatBuffer) currentIndexedGeometryArray.getCoordRefBuffer().getBuffer();

		//clear out current in order to accum into it
		//TODO: a bulk copy of a blank array might be faster here?
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
			
			for (int i = 0; i < currentCoordRefFloat.limit(); i++)
			{
				currentCoordRefFloatbf[i] = 0;
			}
		}
		else
		{
			for (int i = 0; i < currentCoordRefFloat.limit(); i++)
			{
				currentCoordRefFloat.put(i, 0);
			}
		}

		// pre multiply transforms for repeated use for each vertex
		for (int spBoneId = 0; spBoneId < niSkinData.boneList.length; spBoneId++)
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
			NifSkinData nsd = niSkinData.boneList[spBoneId];

			if (NifCharacter.BULK_BUFFER_UPDATES)
			{

				for (NifSkinWeight vw : nsd.vertexWeights)
				{
					int vIdx = vw.index;
					float weight = vw.weight;
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
				currentCoordRefFloat.position(0);
				currentCoordRefFloat.put(currentCoordRefFloatbf);
			}
			else
			{
				for (NifSkinWeight vw : nsd.vertexWeights)
				{
					int vIdx = vw.index;
					float weight = vw.weight;
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

	}

}
