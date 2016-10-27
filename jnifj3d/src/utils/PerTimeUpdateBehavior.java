package utils;

import java.util.Enumeration;

import org.jogamp.java3d.Behavior;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.WakeupCondition;
import org.jogamp.java3d.WakeupOnElapsedTime;
import org.jogamp.vecmath.Point3d;

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
