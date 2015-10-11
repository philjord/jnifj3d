package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nif.j3d.NifTransformGroup;
import nif.j3d.interp.data.CubicSplineCurve;
import nif.j3d.interp.data.CubicSplineSegment;

import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;

/**
 * RotPosScaleTCBSplinePathInterpolator behavior copied from the org j3d one
 */

public class RotPosScaleTCBSplinePathInterpolator extends TCBSplinePathInterpolator
{
	private Quat4f iQuat = new Quat4f(); // interpolated quaternion

	private Vector3f iPos = new Vector3f(); // interpolated position 

	private Vector3d iScale = new Vector3d(); // interpolated scale 

	private CubicSplineCurve cubicSplineCurve = new CubicSplineCurve();

	int numSegments;

	int currentSegmentIndex;

	public RotPosScaleTCBSplinePathInterpolator(NifTransformGroup target, TCBKeyFrame keys[])
	{
		super(target, keys);
		// Create a spline curve using the derived key frames
		cubicSplineCurve = new CubicSplineCurve(this.keyFrames);
		numSegments = cubicSplineCurve.numSegments;

	}

	@Override
	public void computeTransform(float alphaValue)
	{

		// compute the current value of u from alpha and the 
		// determine lower and upper knot points
		computePathInterpolation(alphaValue);

		// Determine the segment within which we will be interpolating
		currentSegmentIndex = this.lowerKnot - 1;

		
		if (currentSegmentIndex == 0 && currentU == 0f)
		{// if we are at the start of the curve 

			iQuat.set(keyFrames[1].quat);
			iPos.set(keyFrames[1].position);
			iScale.set(keyFrames[1].scale);

			 
		}
		else if (currentSegmentIndex == (numSegments - 1) && currentU == 1.0)
		{// if we are at the end of the curve

			iQuat.set(keyFrames[upperKnot].quat);
			iPos.set(keyFrames[upperKnot].position);
			iScale.set(keyFrames[upperKnot].scale);

			
		}
		else
		{// if we are somewhere in between the curve

			// Get a reference to the current spline segment i.e. the
			// one bounded by lowerKnot and upperKnot 
			CubicSplineSegment currentSegment = cubicSplineCurve.getSegment(currentSegmentIndex);

			// interpolate quaternions 
			currentSegment.getInterpolatedQuaternion(currentU, iQuat);

			// interpolate position
			currentSegment.getInterpolatedPositionVector(currentU, iPos);

			// interpolate scale?
			currentSegment.getInterpolatedScale(currentU, iScale);

		}

		// Alway normalize the quaternion
		iQuat.normalize();

	}

	@Override
	protected void applyTransform(Transform3D targetTransform1)
	{
		targetTransform1.setRotation(iQuat);
		targetTransform1.setTranslation(iPos);
		targetTransform1.setScale(iScale);
	}

}
