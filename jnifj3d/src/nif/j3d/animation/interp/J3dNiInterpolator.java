package nif.j3d.animation.interp;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Alpha;
import javax.media.j3d.Group;
import javax.media.j3d.Interpolator;
import javax.media.j3d.TransformGroup;

import tools3d.utils.Utils3D;
import nif.j3d.NifTransformGroup;
import nif.j3d.interp.Interpolated;

 
public abstract class J3dNiInterpolator extends Group
{
	public static final int NIF_USHRT_MAX = 65535;

	public static final float NIF_FLOAT_MIN = (float) -3.4028235E38;

	private ArrayList<Interpolated> interpolators = new ArrayList<Interpolated>();

	public J3dNiInterpolator()
	{
	}

	public void addInterpolator(Interpolated interpolator)
	{
		interpolators.add(interpolator);
	}

	/**
	 * This can be called by any external behavior to progress this interp along
	 * Or it will be called by the auto behaviour if fire is called below
	 */
	public void process(float alphaValue)
	{
		for (Interpolated interpolator : interpolators)
		{
			interpolator.process(alphaValue);
		}
	}

	public static TransformGroup prepTransformGroup(NifTransformGroup targetTransform)
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

}
