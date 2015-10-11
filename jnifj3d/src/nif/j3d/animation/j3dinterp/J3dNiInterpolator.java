package nif.j3d.animation.j3dinterp;

import java.util.Enumeration;

import javax.media.j3d.Alpha;
import javax.media.j3d.Group;
import javax.media.j3d.Interpolator;

import nif.j3d.NifTransformGroup;
import nif.j3d.animation.j3dinterp.interp.Interpolated;
import tools3d.utils.Utils3D;

public abstract class J3dNiInterpolator extends Group
{

	private Object owner;

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

	public static NifTransformGroup prepTransformGroup(NifTransformGroup targetTransform)
	{
		if (targetTransform != null)
		{
			targetTransform.makeWritable();
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
			@SuppressWarnings("rawtypes")
			@Override
			public void processStimulus(Enumeration criteria)
			{
				process(getAlpha().value());
				wakeupOn(defaultWakeupCriterion);
			}

		};
		interp.setEnable(true);
		interp.setSchedulingBounds(Utils3D.defaultBounds);
		addChild(interp);
	}

	public Object getOwner()
	{
		return owner;
	}

	public void setOwner(Object owner)
	{
		this.owner = owner;
	}

}
