package nif.j3d.interp;

import java.util.Enumeration;

import javax.media.j3d.Alpha;
import javax.media.j3d.Interpolator;

public class FloatInterpolator extends Interpolator
{
	private Listener listener;

	private float[] knots;

	private float[] values;		

	public FloatInterpolator(Alpha alpha, Listener listener, float[] knots, float[] values)
	{
		super(alpha);
		this.listener = listener;
		this.knots = knots;
		this.values = values;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration arg0)
	{
		if (getAlpha() != null)
		{
			float alphaValue = getAlpha().value();

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
		wakeupOn(defaultWakeupCriterion);
	}

	public static interface Listener
	{
		public void update(float value);
	}

}
