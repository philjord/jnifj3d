package nif.j3d.animation.j3dinterp.interp;

import javax.media.j3d.Transform3D;

public abstract class KnotInterpolator
{

	// Array of knots
	private float knots[];

	protected float currentInterpolationValue;

	protected int currentKnotIndex;

	protected boolean fixed = false;

	public KnotInterpolator(float[] knots)
	{
		setKnots(knots);
	}

	/** Notice no check for a 0 knot or a 1 knot to finish, not mandatory
	 * 
	 * @param knots
	 */
	protected void setKnots(float[] knots)
	{
		for (int i = 0; i < knots.length; i++)
		{
			if (i > 0 && knots[i] < knots[i - 1])
			{
				throw new IllegalArgumentException("KnotInterpolator bum! " + i + " ! " + knots[i] + " < " + knots[i - 1]);
			}

		}
		this.knots = knots;

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

	public abstract void computeTransform(float alphaValue);

	// Note each sub class should only call one of setRotation, setTranslation or setScale
	public abstract void applyTransform(Transform3D t);

}
