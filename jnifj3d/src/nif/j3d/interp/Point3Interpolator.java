package nif.j3d.interp;

import javax.vecmath.Point3f;

public class Point3Interpolator implements Interpolated
{
	private Listener listener;

	private float[] knots;

	private Point3f[] values;

	/**
	 * x,y,z of each point are r, g,b
	 */

	public Point3Interpolator(Listener listener, float[] knots, Point3f[] values)
	{
		this.listener = listener;
		this.knots = knots;
		this.values = values;

	}

	@Override
	public void process(float alphaValue)
	{
		float currentInterpolationValue = 0;
		int currentKnotIndex = 0;

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

		Point3f value = values[0];

		if (currentKnotIndex != 0 || currentInterpolationValue != 0f)
		{
			value.x = values[currentKnotIndex].x + (values[currentKnotIndex + 1].x - values[currentKnotIndex].x)
					* currentInterpolationValue;
			value.y = values[currentKnotIndex].y + (values[currentKnotIndex + 1].y - values[currentKnotIndex].y)
					* currentInterpolationValue;
			value.z = values[currentKnotIndex].z + (values[currentKnotIndex + 1].z - values[currentKnotIndex].z)
					* currentInterpolationValue;
		}
		listener.update(value);

	}

	public static interface Listener
	{
		public void update(Point3f value);
	}

}
