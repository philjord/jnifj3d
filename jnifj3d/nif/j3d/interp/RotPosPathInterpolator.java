package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * RotPosPathInterpolator is my copy if the j3d RotPosPathInterpolator with multiple targets 
 */

public class RotPosPathInterpolator extends PathInterpolator
{

	private Vector3f pos = new Vector3f();

	private Quat4f tQuat = new Quat4f();

	//private Matrix4d tMat = new Matrix4d();

	// Arrays of quaternions and positions at each knot
	private Quat4f quats[];

	private Point3f positions[];

	public RotPosPathInterpolator(TransformGroup target, float[] knots, Quat4f[] quats, Point3f[] positions)
	{
		super(target, knots);

		if (knots.length != positions.length)
			throw new IllegalArgumentException(J3dI18N.getString("RotPosPathInterpolator0"));

		if (knots.length != quats.length)
			throw new IllegalArgumentException(J3dI18N.getString("RotPosPathInterpolator0"));

		setPathArrays(quats, positions);
		fixed = isFixed();
		if (fixed)
		{
			tQuat.x = quats[0].x;
			tQuat.y = quats[0].y;
			tQuat.z = quats[0].z;
			tQuat.w = quats[0].w;
			pos.x = positions[0].x;
			pos.y = positions[0].y;
			pos.z = positions[0].z;
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
		for (int i = 0; i + 1 < positions.length; i++)
		{
			if (!positions[i].equals(positions[i + 1]))
				return false;
		}

		return true;
	}

	/**
	 * Sets the quat at the specified index for this interpolator.
	 * @param index the index to be changed
	 * @param quat the new quat value
	 */
	public void setQuat(int index, Quat4f quat)
	{
		this.quats[index].set(quat);
	}

	/**
	 * Retrieves the quat value at the specified index.
	 * @param index the index of the value requested
	 * @param quat the quat to receive the quat value at the index
	 */
	public void getQuat(int index, Quat4f quat)
	{
		quat.set(this.quats[index]);
	}

	/**
	 * Sets the position at the specified index for this
	 * interpolator.
	 * @param index the index to be changed
	 * @param position the new position value
	 */
	public void setPosition(int index, Point3f position)
	{
		this.positions[index].set(position);
	}

	// Set the specific arrays for this path interpolator
	private void setPathArrays(Quat4f[] quats, Point3f[] positions)
	{

		this.quats = new Quat4f[quats.length];
		for (int i = 0; i < quats.length; i++)
		{
			this.quats[i] = new Quat4f();
			this.quats[i].set(quats[i]);
		}

		this.positions = new Point3f[positions.length];
		for (int i = 0; i < positions.length; i++)
		{
			this.positions[i] = new Point3f();
			this.positions[i].set(positions[i]);
		}
	}

	public void computeTransform(float alphaValue)
	{
		if (!fixed)
		{
			double quatDot;

			computePathInterpolation(alphaValue);

			if (currentKnotIndex == 0 && currentInterpolationValue == 0f)
			{
				tQuat.x = quats[0].x;
				tQuat.y = quats[0].y;
				tQuat.z = quats[0].z;
				tQuat.w = quats[0].w;
				pos.x = positions[0].x;
				pos.y = positions[0].y;
				pos.z = positions[0].z;
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
				pos.x = positions[currentKnotIndex].x + (positions[currentKnotIndex + 1].x - positions[currentKnotIndex].x)
						* currentInterpolationValue;
				pos.y = positions[currentKnotIndex].y + (positions[currentKnotIndex + 1].y - positions[currentKnotIndex].y)
						* currentInterpolationValue;
				pos.z = positions[currentKnotIndex].z + (positions[currentKnotIndex + 1].z - positions[currentKnotIndex].z)
						* currentInterpolationValue;
			}
			tQuat.normalize();

			// Set the rotation components
			//		tMat.set(tQuat);

			// Set the translation components.
			//		tMat.m03 = pos.x;
			//		tMat.m13 = pos.y;
			//		tMat.m23 = pos.z;
		}
	}

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setRotation(tQuat);
		targetTransform.setTranslation(pos);
	}

}
