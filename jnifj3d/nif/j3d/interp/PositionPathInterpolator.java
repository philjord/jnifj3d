package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * PositionPathInterpolator is my copy if the j3d PositionPathInterpolator fixed up a bit, by not overriding the rest of the transforms components
 */

public class PositionPathInterpolator extends PathInterpolator
{
	private Vector3f pos = new Vector3f();

	// Array of positions at each knot
	private Point3f positions[];

	public PositionPathInterpolator(TransformGroup target, float[] knots, Point3f[] positions, float startTimeS, float lengthS)
	{
		super(target, knots, startTimeS, lengthS);

		if (knots.length != positions.length)
			throw new IllegalArgumentException("knots.length != positions.length");
		setPathArrays(positions);
		fixed = isFixed();
		if (fixed)
		{
			pos.x = positions[0].x;
			pos.y = positions[0].y;
			pos.z = positions[0].z;
		}
	}	

	private boolean isFixed()
	{
		//check for a fixed value
		for (int i = 0; i + 1 < positions.length; i++)
		{
			if (!positions[i].equals(positions[i + 1]))
				return false;
		}

		return true;
	}

	// Set the specific arrays for this path interpolator
	private void setPathArrays(Point3f[] ps)
	{
		positions = new Point3f[ps.length];
		for (int i = 0; i < ps.length; i++)
		{
			positions[i] = new Point3f();
			positions[i].set(ps[i]);
		}
	}

	public void computeTransform(float alphaValue)
	{
		if (!fixed)
		{
			computePathInterpolation(alphaValue);

			if (currentKnotIndex == 0 && currentInterpolationValue == 0f)
			{
				pos.x = positions[0].x;
				pos.y = positions[0].y;
				pos.z = positions[0].z;
			}
			else
			{
				pos.x = positions[currentKnotIndex].x + (positions[currentKnotIndex + 1].x - positions[currentKnotIndex].x)
						* currentInterpolationValue;
				pos.y = positions[currentKnotIndex].y + (positions[currentKnotIndex + 1].y - positions[currentKnotIndex].y)
						* currentInterpolationValue;
				pos.z = positions[currentKnotIndex].z + (positions[currentKnotIndex + 1].z - positions[currentKnotIndex].z)
						* currentInterpolationValue;
			}
		}
	}

	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setTranslation(pos);
	}
}
