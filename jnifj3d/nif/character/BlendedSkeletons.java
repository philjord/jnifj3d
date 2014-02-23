package nif.character;

import javax.media.j3d.Alpha;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NifTransformGroup;
import utils.convert.NifRotToJava3DRot;
import utils.source.MeshSource;

public class BlendedSkeletons extends Group
{

	// not modified to give base line
	//private NifJ3dSkeletonRoot baseSkeleton;

	// modified by wieghted input, used for output
	private NifJ3dSkeletonRoot outputSkeleton;

	// animated and weighted toward by alpha from prev skeleton
	private NifJ3dSkeletonRoot inputSkeleton;

	// This is not availible outside so it is NOT animated
	private NifJ3dSkeletonRoot prevSkeleton;

	private Alpha currentAlpha = new Alpha(1, 0, 0, 0, 0, 0);// default to 1 i.e. totally input

	public BlendedSkeletons(String skeletonNifFilename, MeshSource meshSource)
	{
		//baseSkeleton = createSkeleton();
		outputSkeleton = new NifJ3dSkeletonRoot(skeletonNifFilename, meshSource);
		inputSkeleton = new NifJ3dSkeletonRoot(skeletonNifFilename, meshSource);
		prevSkeleton = new NifJ3dSkeletonRoot(skeletonNifFilename, meshSource);

		// for simple hats to be attched to etc
		addChild(outputSkeleton);
	}

	/**
	 * This will take a copy of the current output and set it as the prev, then blend to the input skeleton using the alpha
	 * It is assumed that the return input skeleton will be animated immediately after this call
	 * @param newAlpha
	 */
	public NifJ3dSkeletonRoot startNewInputAnimation(Alpha newAlpha)
	{
		currentAlpha = newAlpha;

		Transform3D temp = new Transform3D();

		for (String boneName : outputSkeleton.getAllBonesInSkeleton().keySet())
		{
			J3dNiAVObject outputBone = outputSkeleton.getAllBonesInSkeleton().get(boneName);
			NifTransformGroup output = outputBone.getTransformGroup();

			J3dNiAVObject prevBone = prevSkeleton.getAllBonesInSkeleton().get(boneName);
			NifTransformGroup prev = prevBone.getTransformGroup();

			output.getTransform(temp);
			prev.setTransform(temp);
		}

		return inputSkeleton;
	}

	//deburners
	private Transform3D prevT = new Transform3D();

	private Transform3D inputT = new Transform3D();

	private Transform3D outputT = new Transform3D();

	private Transform3D prevOutputT = new Transform3D();

	public void updateOutputBones()
	{
		float alphaValue = currentAlpha.value();

		for (String boneName : outputSkeleton.getAllBonesInSkeleton().keySet())
		{
			J3dNiAVObject outputBone = outputSkeleton.getAllBonesInSkeleton().get(boneName);
			NifTransformGroup output = outputBone.getTransformGroup();

			if (alphaValue == 0f)
			{
				J3dNiAVObject prevBone = prevSkeleton.getAllBonesInSkeleton().get(boneName);
				NifTransformGroup prev = prevBone.getTransformGroup();
				prev.getTransform(prevT);
				outputT.set(prevT);
			}
			else if (alphaValue == 1f)
			{
				J3dNiAVObject inputBone = inputSkeleton.getAllBonesInSkeleton().get(boneName);
				NifTransformGroup input = inputBone.getTransformGroup();
				input.getTransform(inputT);
				outputT.set(inputT);
			}
			else
			{
				// get out 3 transfrom groups			
				J3dNiAVObject prevBone = prevSkeleton.getAllBonesInSkeleton().get(boneName);
				NifTransformGroup prev = prevBone.getTransformGroup();

				J3dNiAVObject inputBone = inputSkeleton.getAllBonesInSkeleton().get(boneName);
				NifTransformGroup input = inputBone.getTransformGroup();

				// combine prev and input for second transform3d
				prev.getTransform(prevT);
				input.getTransform(inputT);

				computeTransform(alphaValue, prevT, inputT, outputT);

			}
			//only set on a change
			if (!outputT.equals(prevOutputT))
			{
				output.setTransform(outputT);
				prevOutputT.set(outputT);
			}
		}
	}

	public NifJ3dSkeletonRoot getOutputSkeleton()
	{
		return outputSkeleton;
	}

	//deburners
	private Quat4f tempQuat0 = new Quat4f();

	private Quat4f tempQuat1 = new Quat4f();

	private Vector3f tempPos0 = new Vector3f();

	private Vector3f tempPos1 = new Vector3f();

	private Matrix4d tempMat = new Matrix4d();

	private void computeTransform(float alphaValue, Transform3D t0, Transform3D t1, Transform3D out)
	{
		t0.normalizeCP();
		t1.normalizeCP();
		NifRotToJava3DRot.safeGetQuat(t0, tempQuat0);
		t0.get(tempPos0);
		NifRotToJava3DRot.safeGetQuat(t1, tempQuat1);
		t1.get(tempPos1);

		out.set(computeTransform(alphaValue, tempQuat0, tempQuat1, tempPos0, tempPos1, tempMat));
		out.normalizeCP();
	}

	//deburners
	private Vector3f tPos = new Vector3f();

	private Quat4f tQuat = new Quat4f();

	/**
	 * Note do not call with alpha == 0 or alpha ==1!
	 * @param alphaValue
	 * @param quat0
	 * @param quat1
	 * @param pos0
	 * @param pos1
	 * @param out
	 * @return
	 */
	private Matrix4d computeTransform(float alphaValue, Quat4f quat0, Quat4f quat1, Vector3f pos0, Vector3f pos1, Matrix4d out)
	{
		double quatDot;

		quatDot = quat0.x * quat1.x + quat0.y * quat1.y + quat0.z * quat1.z + quat0.w * quat1.w;
		if (quatDot < 0)
		{
			tQuat.x = quat0.x + (-quat1.x - quat0.x) * alphaValue;
			tQuat.y = quat0.y + (-quat1.y - quat0.y) * alphaValue;
			tQuat.z = quat0.z + (-quat1.z - quat0.z) * alphaValue;
			tQuat.w = quat0.w + (-quat1.w - quat0.w) * alphaValue;
		}
		else
		{
			tQuat.x = quat0.x + (quat1.x - quat0.x) * alphaValue;
			tQuat.y = quat0.y + (quat1.y - quat0.y) * alphaValue;
			tQuat.z = quat0.z + (quat1.z - quat0.z) * alphaValue;
			tQuat.w = quat0.w + (quat1.w - quat0.w) * alphaValue;
		}
		tPos.x = pos0.x + (pos1.x - pos0.x) * alphaValue;
		tPos.y = pos0.y + (pos1.y - pos0.y) * alphaValue;
		tPos.z = pos0.z + (pos1.z - pos0.z) * alphaValue;

		tQuat.normalize();

		// Set the rotation components (and zero pos)
		out.set(tQuat);

		// Set the translation components.
		out.m03 = tPos.x;
		out.m13 = tPos.y;
		out.m23 = tPos.z;

		//Set scale to 1 always

		return out;
	}

}
