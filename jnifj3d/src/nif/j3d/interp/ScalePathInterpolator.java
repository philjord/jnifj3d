package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * ScalePathInterpolator is my copy if the j3d ScalePathInterpolator fixed up a bit, by not overriding the rest of the transforms components
 */
public class ScalePathInterpolator extends PathInterpolator
{
	private float tScale = 1;

	// Array of positions at each knot
	private float[] scales;

	public ScalePathInterpolator(TransformGroup target, float[] knots, float[] scales, float startTimeS, float lengthS)
	{
		super(target, knots, startTimeS, lengthS);

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

	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setScale(tScale);
	}

}