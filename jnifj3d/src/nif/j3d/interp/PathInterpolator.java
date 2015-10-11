package nif.j3d.interp;

import nif.j3d.NifTransformGroup;

/**
 * This is a  copy of the PathInterpolator from j3d, with target swapped out for a multiple targets and axis dumped
 * @author Administrator
 *
 */
public abstract class PathInterpolator extends TransformInterpolator
{

	// Array of knots
	private float knots[];

	protected float currentInterpolationValue;

	protected int currentKnotIndex;

	public PathInterpolator(NifTransformGroup target, float[] knots, float startTimeS, float lengthS)
	{
		super(target, startTimeS, lengthS);
		setKnots(knots);
	}

	/** Notice no check for a 0 knot or a 1 knot to finish, not mandatory
	 * 
	 * @param knots
	 */
	protected void setKnots(float[] knots)
	{
		this.knots = new float[knots.length];
		for (int i = 0; i < knots.length; i++)
		{
			if (i > 0 && knots[i] < knots[i - 1])
			{
				throw new IllegalArgumentException("KnotInterpolator bum! " + i + " ! " + knots[i] + " < " + knots[i - 1]);
			}
			this.knots[i] = knots[i];
		}
	}

	protected void computePathInterpolation(float alphaValue)
	{
		for (int i = 0; i < knots.length; i++)
		{
			if ((i == 0 && alphaValue <= knots[i]) || (i > 0 && alphaValue >= knots[i - 1] && alphaValue <= knots[i]))
			{
				if (i == 0)
				{
					currentInterpolationValue = 0f;
					currentKnotIndex = 0;
				}
				else
				{
					currentInterpolationValue = (alphaValue - knots[i - 1]) / (knots[i] - knots[i - 1]);
					currentKnotIndex = i - 1;
				}
				break;
			}
		}
	}
}
