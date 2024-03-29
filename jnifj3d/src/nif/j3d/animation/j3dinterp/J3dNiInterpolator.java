package nif.j3d.animation.j3dinterp;

import java.util.Iterator;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Interpolator;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.WakeupCriterion;

import nif.j3d.animation.j3dinterp.interp.Interpolated;
import tools3d.utils.Utils3D;

public abstract class J3dNiInterpolator extends Group
{
	public static final int NIF_USHRT_MAX = 65535;

	public static final float NIF_FLOAT_MIN = (float) -3.4028235E38;

	private Interpolated interpolator;

	public J3dNiInterpolator()
	{
	}

	public void setInterpolator(Interpolated interpolator)
	{
		this.interpolator = interpolator;
	}

	public Interpolated getInterpolator()
	{
		return interpolator;
	}

	/**
	 * This can be called by any external behavior to progress this interp along
	 * Or it will be called by the auto behaviour if fire is called below
	 */
	public void process(float alphaValue)
	{
		if (interpolator != null)
			interpolator.process(alphaValue);
	}

	public static TransformGroup prepTransformGroup(TransformGroup targetTransform)
	{
		if (targetTransform != null)
		{
			if (!targetTransform.isLive() && !targetTransform.isCompiled())
			{
				targetTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
				targetTransform .setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			}
			return targetTransform;
		}
		return null;
	}

	/**
	 * This MUST be called before the interpolators is added to the scene graph
	 * If this is called I should really not allow the process method above to be called by any other behavior
	 * @param baseAlpha
	 */
	public void fire(Alpha baseAlpha)
	{
		Interpolator interp = new Interpolator(baseAlpha)
		{
			@Override
			public void processStimulus(Iterator<WakeupCriterion> criteria)
			{
				process(getAlpha().value());
				wakeupOn(defaultWakeupCriterion);
			}

		};
		interp.setEnable(true);
		interp.setSchedulingBounds(Utils3D.defaultBounds);
		addChild(interp);
	}
}
