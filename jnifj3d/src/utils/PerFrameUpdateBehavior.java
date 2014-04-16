package utils;

import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;

public class PerFrameUpdateBehavior extends Behavior
{
	private CallBack callBack;

	private WakeupCondition FPSCriterion;

	public PerFrameUpdateBehavior(CallBack callBack)
	{
		this(0, callBack);
	}

	public PerFrameUpdateBehavior(int frameCount, CallBack callBack)
	{
		FPSCriterion = new WakeupOnElapsedFrames(frameCount, false);
		setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		setEnable(true);
		this.callBack = callBack;
	}

	public void initialize()
	{
		wakeupOn(FPSCriterion);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration criteria)
	{
		callBack.update();

		wakeupOn(FPSCriterion);
	}

	public static interface CallBack
	{
		public void update();
	}
}
