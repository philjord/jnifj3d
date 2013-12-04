package nif.j3d.interp;

import javax.media.j3d.TransformGroup;

import com.sun.j3d.internal.J3dUtilsI18N;
import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;

/**
 * TCBSplinePathInterpolator copied from orgj3d one, but supports multiple targets  
 *
 * @since Java3D 1.1
 */

public abstract class TCBSplinePathInterpolator extends TransformInterpolator
{

	private int keysLength;

	/**
	 * An array of KBKeyFrame for interpolator
	 */
	protected TCBKeyFrame[] keyFrames;

	/**
	 * This value is the distance between knots 
	 * value which can be used in further calculations by the subclass.
	 */
	protected float currentU;

	/**
	 * The lower knot
	 */
	protected int lowerKnot;

	/**
	 * The upper knot
	 */
	protected int upperKnot;

	public TCBSplinePathInterpolator(TransformGroup target, TCBKeyFrame keys[])
	{
		super(target);
		processKeyFrames(keys);
	}

	private void processKeyFrames(TCBKeyFrame keys[])
	{

		// Make sure that we have at least two key frames
		keysLength = keys.length;
		if (keysLength < 2)
		{
			throw new IllegalArgumentException(J3dUtilsI18N.getString("TCBSplinePathInterpolator0"));

		}

		// Make sure that the first key frame's knot is equal to 0.0 
		if (keys[0].knot < -0.0001 || keys[0].knot > 0.0001)
		{
			throw new IllegalArgumentException(J3dUtilsI18N.getString("TCBSplinePathInterpolator1"));
		}

		// Make sure that the last key frames knot is equal to 1.0 
		if (keys[keysLength - 1].knot - 1.0 < -0.0001 || keys[keysLength - 1].knot - 1.0 > 0.0001)
		{
			throw new IllegalArgumentException(J3dUtilsI18N.getString("TCBSplinePathInterpolator2"));
		}

		// Make sure that all the knots are in sequence 
		for (int i = 0; i < keysLength; i++)
		{
			if (i > 0 && keys[i].knot < keys[i - 1].knot)
			{
				throw new IllegalArgumentException(J3dUtilsI18N.getString("TCBSplinePathInterpolator3"));
			}
		}

		// Make space for a leading and trailing key frame in addition to 
		// the keys passed in
		keyFrames = new TCBKeyFrame[keysLength + 2];

		keyFrames[0] = keys[0];
		for (int i = 1; i < keysLength + 1; i++)
		{
			keyFrames[i] = keys[i - 1];
		}

		keyFrames[keysLength + 1] = keys[keysLength - 1];

		// Make key frame length reflect the 2 added key frames
		keysLength += 2;
	}

	protected void computePathInterpolation(float alphaValue)
	{

		// skip knots till we find the two we fall between  
		int i = 1;
		int len = keysLength - 2;
		while ((alphaValue > keyFrames[i].knot) && (i < len))
		{
			i++;
		}

		if (i == 1)
		{
			currentU = 0f;
			lowerKnot = 1;
			upperKnot = 2;
		}
		else
		{
			currentU = (alphaValue - keyFrames[i - 1].knot) / (keyFrames[i].knot - keyFrames[i - 1].knot);
			lowerKnot = i - 1;
			upperKnot = i;
		}
	}

}
