package nif.j3d.animation.j3dinterp.interp;

import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Vector3f;

public class PositionPathInterpolator extends KnotInterpolator
{
	private Vector3f pos = new Vector3f();

	// Array of positions at each knot
	//private Point3f positions[];
	// faster primitive version xyz floats
	private float positions[];
	
	public PositionPathInterpolator(float[] knots, float[] positions)
	{
		super(knots);

		if (knots.length*3 != positions.length)
			throw new IllegalArgumentException("knots.length != positions.length");

		this.positions = positions;

		fixed = isFixed();
		if (fixed)
		{
			pos.x = positions[0*3+0];
			pos.y = positions[0*3+1];
			pos.z = positions[0*3+2];
		}
	}

	private boolean isFixed()
	{
		//check for a fixed value
		for (int i = 0; i + 1 < positions.length/3; i++)
		{
			if (positions[i*3+0] != positions[(i + 1)*3+0] || positions[i*3+1] != positions[(i + 1)*3+1] || positions[i*3+2] != positions[(i + 1)*3+2])
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
				pos.x = positions[0*3+0];
				pos.y = positions[0*3+1];
				pos.z = positions[0*3+2];
			}
			else
			{
				pos.x = positions[currentKnotIndex*3+0] + (positions[(currentKnotIndex + 1)*3+0] - positions[currentKnotIndex*3+0])
						* currentInterpolationValue;
				pos.y = positions[currentKnotIndex*3+1] + (positions[(currentKnotIndex + 1)*3+1] - positions[currentKnotIndex*3+1])
						* currentInterpolationValue;
				pos.z = positions[currentKnotIndex*3+2] + (positions[(currentKnotIndex + 1)*3+2] - positions[currentKnotIndex*3+2])
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
