package nif.j3d.interp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.media.j3d.Alpha;
import javax.media.j3d.TransformGroup;

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

	public PathInterpolator(Alpha alpha, TransformGroup target, float[] knots)
	{
		super(alpha, target);
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
				throw new IllegalArgumentException(J3dI18N.getString("PathInterpolator2"));
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

	static class J3dI18N
	{
		static String getString(String key)
		{
			String s;
			try
			{
				s = ResourceBundle.getBundle("javax.media.j3d.ExceptionStrings").getString(key);
			}
			catch (MissingResourceException e)
			{
				System.err.println("J3dI18N: Error looking up: " + key);
				s = key;
			}
			return s;
		}
	}
}
