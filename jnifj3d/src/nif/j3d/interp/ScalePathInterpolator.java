package nif.j3d.interp;

import javax.media.j3d.Transform3D;

public class ScalePathInterpolator extends KnotInterpolator
{
	private float tScale = 1;

	// Array of positions at each knot
	private float[] scales;

	public ScalePathInterpolator(float[] knots, float[] scales)
	{
		super(knots);

		if (knots.length != scales.length)
			throw new IllegalArgumentException("knots.length != scales.length");
		setPathArrays(scales);
		fixed = isFixed();
		if (fixed)
		{
			tScale = scales[0];
		}
	}

	private boolean isFixed()
	{
		//check for a fixed value
		for (int i = 0; i + 1 < scales.length; i++)
		{
			if (scales[i] != scales[i + 1])
				return false;
		}

		return true;
	}

	// Set the specific arrays for this path interpolator
	private void setPathArrays(float[] scales)
	{

		this.scales = new float[scales.length];
		for (int i = 0; i < scales.length; i++)
		{
			this.scales[i] = scales[i];
		}
	}

	@Override
	public void computeTransform(float alphaValue)
	{
		if (!fixed)
		{
			computePathInterpolation(alphaValue);

			tScale = 1.0f;
			if (currentKnotIndex == 0 && currentInterpolationValue == 0f)
			{
				tScale = scales[0];
			}
			else
			{
				tScale = scales[currentKnotIndex] + (scales[currentKnotIndex + 1] - scales[currentKnotIndex]) * currentInterpolationValue;
			}
		}
	}

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setScale(tScale);		
	}

}
