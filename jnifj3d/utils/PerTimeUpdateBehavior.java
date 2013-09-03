package utils;

import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.vecmath.Point3d;

public class PerTimeUpdateBehavior extends Behavior
{
	private CallBack callBack;

	private WakeupCondition timeCriterion;

	public PerTimeUpdateBehavior(long milliseconds, CallBack callBack)
	{
		timeCriterion = new WakeupOnElapsedTime(milliseconds);
		setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		setEnable(true);
		this.callBack = callBack;
	}

	public void initialize()
	{
		wakeupOn(timeCriterion);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration criteria)
	{
		callBack.update();

		wakeupOn(timeCriterion);
	}

	public static interface CallBack
	{
		public void update();
	}
}
