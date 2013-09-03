package nif.j3d.interp;

import java.util.Enumeration;

import javax.media.j3d.Alpha;
import javax.media.j3d.Interpolator;

public class BoolInterpolator extends Interpolator
{
	private Listener listener;

	private float[] knots;

	private boolean[] values;

	/**
	 * x,y,z of each point are r, g,b
	 */

	public BoolInterpolator(Alpha alpha, Listener listener, float[] knots, boolean[] values)
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

			int currentKnotIndex = 0;
			if (alphaValue == 1f)
			{
				currentKnotIndex = knots.length - 1;
			}
			else
			{
				for (int i = 0; i < knots.length; i++)
				{
					if ((i == 0 && alphaValue <= knots[i]) || (i > 0 && alphaValue >= knots[i - 1] && alphaValue <= knots[i]))
					{
						if (i == 0)
						{
							currentKnotIndex = 0;
						}
						else
						{
							currentKnotIndex = i - 1;
						}
						break;
					}
				}
			}

			boolean value = values[currentKnotIndex];

			listener.update(value);
		}
		wakeupOn(defaultWakeupCriterion);

	}

	public static interface Listener
	{
		public void update(boolean value);
	}

}
