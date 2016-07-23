package nif.j3d.animation.j3dinterp.interp;


public class FloatInterpolator implements Interpolated
{
	private Listener listener;

	private float[] knots;

	private float[] values;

	public FloatInterpolator(Listener listener, float[] knots, float[] values)
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

		float value = values[0];

		if (currentKnotIndex != 0 || currentInterpolationValue != 0f)
		{
			value = values[currentKnotIndex] + (values[currentKnotIndex + 1] - values[currentKnotIndex]) * currentInterpolationValue;
		}
		listener.update(value);
	}

	public static interface Listener
	{
		public void update(float value);
	}

}
