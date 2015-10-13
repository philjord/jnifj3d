package nif.j3d.animation.j3dinterp.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import nif.j3d.NifTransformGroup;

public class RotPosScaleInterpolator extends TransformInterpolator
{
	private PositionPathInterpolator positionPathInterpolator;

	private ScalePathInterpolator scalePathInterpolator;

	private XYZRotPathInterpolator xYZRotPathInterpolator;

	private RotationPathInterpolator quatRotInterpolator;

	private Vector3f defaultTrans = null;

	private Quat4f defaultRot = null;

	private float defaultScale = 1;

	private Transform3D baseTransform = null;

	public RotPosScaleInterpolator(NifTransformGroup target, float startTimeS, float lengthS,
			PositionPathInterpolator positionPathInterpolator, ScalePathInterpolator scalePathInterpolator,
			XYZRotPathInterpolator xYZRotPathInterpolator, RotationPathInterpolator quatRotInterpolator, Vector3f defaultTrans,
			Quat4f defaultRot, float defaultScale)
	{
		super(target, startTimeS, lengthS);
		this.positionPathInterpolator = positionPathInterpolator;
		this.defaultTrans = defaultTrans;
		this.scalePathInterpolator = scalePathInterpolator;
		this.defaultScale = defaultScale;
		this.xYZRotPathInterpolator = xYZRotPathInterpolator;
		this.quatRotInterpolator = quatRotInterpolator;
		this.defaultRot = defaultRot;

	}

	/**
	 * Method overrride as we have 3 elements to update and the rotation can be one of 2 types
	 * @see nif.j3d.animation.j3dinterp.J3dNiInterpolator#process(float)
	 */
	@Override
	public void process(float alphaValue)
	{
		// preserve the target values if interps have no defaults		
		if (baseTransform == null)
		{
			baseTransform = new Transform3D();
			target.getTransform(baseTransform);
		}

		// convert to an offsetted time in seconds
		float normAlphaValue = alphaValue * lengthS;
		normAlphaValue += startTimeS;

		// set to the base target
		targetTransform.set(baseTransform);

		if (alphaValue != prevAlphaValue)
		{
			if (xYZRotPathInterpolator != null)
			{
				xYZRotPathInterpolator.computeTransform(normAlphaValue);
				xYZRotPathInterpolator.applyTransform(targetTransform);
			}
			else if (quatRotInterpolator != null)
			{
				quatRotInterpolator.computeTransform(normAlphaValue);
				quatRotInterpolator.applyTransform(targetTransform);
			}
			else if (defaultRot != null)
			{
				targetTransform.setRotation(defaultRot);
			}

			if (positionPathInterpolator != null)
			{
				positionPathInterpolator.computeTransform(normAlphaValue);
				positionPathInterpolator.applyTransform(targetTransform);
			}
			else if (defaultTrans != null)
			{
				targetTransform.setTranslation(defaultTrans);
			}

			if (scalePathInterpolator != null)
			{
				scalePathInterpolator.computeTransform(normAlphaValue);
				scalePathInterpolator.applyTransform(targetTransform);
			}
			else if (defaultScale != Float.MIN_VALUE)
			{
				targetTransform.setScale(defaultScale);
			}

			//only set on a change
			if (!targetTransform.equals(prevTargetTransform))
			{
				target.setTransform(targetTransform);
				prevTargetTransform.set(targetTransform);
			}

			prevAlphaValue = alphaValue;
		}

	}

	@Override
	public void computeTransform(float alphaValue)
	{
		//dummy as process does it special
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyTransform(Transform3D t)
	{
		//dummy as process does it special		
		throw new UnsupportedOperationException();
	}
}
