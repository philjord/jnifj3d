package nif.j3d.animation.j3dinterp.interp;

import javax.vecmath.Quat4f;

import org.jogamp.java3d.Transform3D;

public class RotationPathInterpolator extends KnotInterpolator
{
	private Quat4f tQuat = new Quat4f();

	// Array of quaternions at each knot
	private Quat4f quats[];

	public RotationPathInterpolator(float[] knots, Quat4f[] quats)
	{
		super(knots);

		if (knots.length != quats.length)
			throw new IllegalArgumentException("knots.length != quats.length");

		this.quats = quats;

		fixed = isFixed();
		if (fixed)
		{
			tQuat.x = quats[0].x;
			tQuat.y = quats[0].y;
			tQuat.z = quats[0].z;
			tQuat.w = quats[0].w;
		}
	}

	private boolean isFixed()
	{
		//check for a fixed value
		for (int i = 0; i + 1 < quats.length; i++)
		{
			if (!quats[i].equals(quats[i + 1]))
				return false;
		}

		return true;
	}

	@Override
	public void computeTransform(float alphaValue)
	{
		if (!fixed)
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
				quatDot = quats[currentKnotIndex].x * quats[currentKnotIndex + 1].x + quats[currentKnotIndex].y
						* quats[currentKnotIndex + 1].y + quats[currentKnotIndex].z * quats[currentKnotIndex + 1].z
						+ quats[currentKnotIndex].w * quats[currentKnotIndex + 1].w;
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
	}

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setRotation(tQuat);
	}

}
