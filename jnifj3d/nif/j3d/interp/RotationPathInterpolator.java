package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Quat4f;

/**
 * RotationPathInterpolator is my copy if the j3d RotationPathInterpolator fixed up a bit, by not overriding the rest of the transforms components
 */

public class RotationPathInterpolator extends PathInterpolator
{
	private Quat4f tQuat = new Quat4f();

	// Array of quaternions at each knot
	private Quat4f quats[];

	public RotationPathInterpolator(TransformGroup target, float[] knots, Quat4f[] quats)
	{
		super(target, knots);

		if (knots.length != quats.length)
			throw new IllegalArgumentException("knots.length != quats.length");

		setPathArrays(quats);
	}

	// Set the specific arrays for this path interpolator
	private void setPathArrays(Quat4f[] quats)
	{
		this.quats = new Quat4f[quats.length];
		for (int i = 0; i < quats.length; i++)
		{
			this.quats[i] = new Quat4f();
			this.quats[i].set(quats[i]);
		}
	}

	@Override
	public void computeTransform(float alphaValue)
	{

		double quatDot;
		computePathInterpolation(alphaValue);
		// For RPATH, take quaternion average and set rotation in TransformGroup

		if (currentKnotIndex == 0 && currentInterpolationValue == 0f)
		{
			tQuat.x = quats[0].x;
			tQuat.y = quats[0].y;
			tQuat.z = quats[0].z;
			tQuat.w = quats[0].w;
		}
		else
		{
			quatDot = quats[currentKnotIndex].x * quats[currentKnotIndex + 1].x + quats[currentKnotIndex].y * quats[currentKnotIndex + 1].y
					+ quats[currentKnotIndex].z * quats[currentKnotIndex + 1].z + quats[currentKnotIndex].w * quats[currentKnotIndex + 1].w;
			if (quatDot < 0)
			{
				tQuat.x = quats[currentKnotIndex].x + (-quats[currentKnotIndex + 1].x - quats[currentKnotIndex].x)
						* currentInterpolationValue;
				tQuat.y = quats[currentKnotIndex].y + (-quats[currentKnotIndex + 1].y - quats[currentKnotIndex].y)
						* currentInterpolationValue;
				tQuat.z = quats[currentKnotIndex].z + (-quats[currentKnotIndex + 1].z - quats[currentKnotIndex].z)
						* currentInterpolationValue;
				tQuat.w = quats[currentKnotIndex].w + (-quats[currentKnotIndex + 1].w - quats[currentKnotIndex].w)
						* currentInterpolationValue;
			}
			else
			{
				tQuat.x = quats[currentKnotIndex].x + (quats[currentKnotIndex + 1].x - quats[currentKnotIndex].x)
						* currentInterpolationValue;
				tQuat.y = quats[currentKnotIndex].y + (quats[currentKnotIndex + 1].y - quats[currentKnotIndex].y)
						* currentInterpolationValue;
				tQuat.z = quats[currentKnotIndex].z + (quats[currentKnotIndex + 1].z - quats[currentKnotIndex].z)
						* currentInterpolationValue;
				tQuat.w = quats[currentKnotIndex].w + (quats[currentKnotIndex + 1].w - quats[currentKnotIndex].w)
						* currentInterpolationValue;
			}
		}

		tQuat.normalize();
	}

	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setRotation(tQuat);
	}

}
