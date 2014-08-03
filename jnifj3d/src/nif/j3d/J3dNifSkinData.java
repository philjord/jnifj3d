package nif.j3d;

import java.util.LinkedHashMap;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;

import nif.compound.NifSkinData;
import nif.compound.NifSkinTransform;
import nif.compound.NifSkinWeight;
import nif.niobject.NiSkinData;
import utils.convert.ConvertFromNif;

public class J3dNifSkinData extends Group implements GeometryUpdater
{
	private NiSkinData niSkinData;

	private J3dNiNode[] skeletonBonesInSkinBoneIdOrder;//prelookups

	private GeometryArray baseIndexedGeometryArray;

	private GeometryArray currentIndexedGeometryArray;

	private Transform3D skinDataTrans = new Transform3D();

	private Transform3D[] skinBonesSkinOffsetInOrder;

	public J3dNifSkinData(NiSkinData niSkinData, J3dNiTriShape j3dNiTriShape, J3dNiNode[] skinBonesInOrder,
			LinkedHashMap<String, J3dNiNode> skeletonBones)
	{
		//http://sourceforge.net/p/niftools/niflib/ci/0b2d0541c5a17af892ab2f416acbbfd2fdc369b2/tree/src/obj/NiSkinData.cpp

		// TODO: head of dog needs attachign proper to blended output, in the correct update frame
		// TODO: spider daedra jaw not better, troll jaw also bad		
		// TODO: possibly also proper undertand of non accum etc
		// Horses and dogs in skyrim have back legs that flick like a null rotate, (like jaw?)
		// animated fingers in fallout show similar

		this.niSkinData = niSkinData;

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
			if (skeletonBone == null)
				System.out.println("Null bone! mixed games or creatures? " + skinBone.getName());
			skeletonBonesInSkinBoneIdOrder[spBoneId] = skeletonBone;

		}

		currentIndexedGeometryArray = j3dNiTriShape.getCurrentGeometryArray();
		baseIndexedGeometryArray = j3dNiTriShape.getBaseGeometryArray();

	}

	public void updateSkin()
	{
		currentIndexedGeometryArray.updateData(this);
	}

	// for reuse inside loop
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	//reused in loop
	private Transform3D accumulatorTrans = new Transform3D();

	@Override
	public void updateData(Geometry geometry)
	{
		// holder of the transform data to speed up transform (possibly)
		double[] accTransMat = new double[16];

		float[] baseCoordRefFloat = baseIndexedGeometryArray.getCoordRefFloat();
		float[] currentCoordRefFloat = currentIndexedGeometryArray.getCoordRefFloat();

		//clear out current in order to accum into it
		for (int i = 0; i < currentCoordRefFloat.length; i++)
		{
			currentCoordRefFloat[i] = 0;
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

			for (NifSkinWeight vw : nsd.vertexWeights)
			{
				int vIdx = vw.index;
				float weight = vw.weight;
				// If this bone has any effect add it in 
				if (weight > 0)
				{
					float px = baseCoordRefFloat[vIdx * 3 + 0];
					float py = baseCoordRefFloat[vIdx * 3 + 1];
					float pz = baseCoordRefFloat[vIdx * 3 + 2];

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
					currentCoordRefFloat[vIdx * 3 + 0] += px;
					currentCoordRefFloat[vIdx * 3 + 1] += py;
					currentCoordRefFloat[vIdx * 3 + 2] += pz;
				}

			}
		}

	}

}
