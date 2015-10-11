package nif.j3d.animation.j3dinterp.interp;


public class BoolInterpolator implements Interpolated
{
	private Listener listener;

	private float[] knots;

	private boolean[] values;

	/**
	 * x,y,z of each point are r, g,b
	 */

	public BoolInterpolator(Listener listener, float[] knots, boolean[] values)
	{
		this.listener = listener;
		this.knots = knots;
		this.values = values;

	}

	@Override
	public void process(float alphaValue)
	{
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

	public static interface Listener
	{
		public void update(boolean value);
	}

}
