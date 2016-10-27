package nif.j3d.animation.j3dinterp.interp;

import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

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

		this.positions = positions;

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
