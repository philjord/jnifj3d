package nif.j3d.animation.interp;

import java.util.ArrayList;

import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Group;
import javax.media.j3d.Interpolator;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;

import nif.j3d.NifTransformGroup;

public abstract class J3dNiInterpolator extends Group
{
	public static final int NIF_USHRT_MAX = 65535;

	public static final float NIF_FLOAT_MIN = (float) -3.4028235E38;

	public static BoundingSphere defaultBoundingShpere = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY);

	private ArrayList<Interpolator> interpolators = new ArrayList<Interpolator>();

	public J3dNiInterpolator()
	{
	}

	public void addInterpolator(Interpolator interpolator)
	{
		interpolator.setEnable(false);
		interpolator.setSchedulingBounds(defaultBoundingShpere);
		addChild(interpolator);
		interpolators.add(interpolator);
	}

	public void fire(Alpha resetAlpha)
	{
		setEnable(true);
		for (Interpolator interpolator : interpolators)
		{
			interpolator.setAlpha(resetAlpha);
		}
	}

	public void setEnable(boolean enable)
	{
		for (Interpolator interpolator : interpolators)
		{
			interpolator.setEnable(enable);
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

}
