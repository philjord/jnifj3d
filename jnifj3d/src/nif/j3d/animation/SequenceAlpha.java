package nif.j3d.animation;

import javax.media.j3d.Alpha;

/**
 * Basically an alpha that loops if given a loop start and end and stop when told to
 * @author philip
 *
 */
public class SequenceAlpha extends Alpha
{
	private long lengthMS = -1;

	private long loopStartTimeMS = -1;

	private long loopEndTimeMS = -1;

	private long loopLengthMS = -1;

	private float loopStartTimeS = -1;

	private float loopEndTimeS = -1;

	private boolean shouldLoop = false;

	public SequenceAlpha(float startTimeS, float stopTimeS, boolean shouldLoop)
	{
		super((shouldLoop ? -1 : 1), 0, (long) (startTimeS * 1000L), ((long) (stopTimeS * 1000L) - (long) (startTimeS * 1000L)), 0, 0);
		this.lengthMS = ((long) (stopTimeS * 1000L) - (long) (startTimeS * 1000L));

		//this.setStartTime(System.nanoTime() / 1000000);
		this.shouldLoop = shouldLoop;
		this.setStartTime(System.currentTimeMillis());

	}

	public SequenceAlpha(float startTimeS, float stopTimeS, float loopStartTimeS, float loopEndTimeS, boolean shouldLoop)
	{
		this(startTimeS, stopTimeS, shouldLoop);
		this.loopStartTimeS = loopStartTimeS;
		this.loopEndTimeS = loopEndTimeS;
		loopStartTimeMS = (long) (loopStartTimeS * 1000L);
		loopEndTimeMS = (long) (loopEndTimeS * 1000L);
		loopLengthMS = loopEndTimeMS - loopStartTimeMS;
	}

	public void start()
	{
		this.setStartTime(System.currentTimeMillis());
	}
	
	/**
	 * Means finish the current cycle and don't loop at the next next loopend event
	 */
	public void beginExit()
	{
		shouldLoop = false;
	}

	@Override
	public float value(long atTime)
	{
		float val = super.value(atTime);
		if (shouldLoop == false)
		{
			return val;
		}
		else
		{
			// loop round if needed
			if (loopStartTimeMS != -1 && (val * lengthMS) > loopEndTimeMS)
			{
				// see if I've rolled past loopend then ADD loop length to the current start time?
				long newLoopedStart = getStartTime() + loopLengthMS;
				this.setStartTime(newLoopedStart);
				// now get the alpha to recalc
				return super.value(atTime);
			}
			return val;
		}
	}

	public float getLoopStartTimeS()
	{
		return loopStartTimeS;
	}

	public float getLoopEndTimeS()
	{
		return loopEndTimeS;
	}
}