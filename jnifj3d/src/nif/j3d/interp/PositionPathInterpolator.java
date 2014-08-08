package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class PositionPathInterpolator extends KnotInterpolator
{
	private Vector3f pos = new Vector3f();

	// Array of positions at each knot
	private Point3f positions[];

	public PositionPathInterpolator(float[] knots, Point3f[] positions)
	{
		super(knots);

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

	@Override
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

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setTranslation(pos);
	}
}
