package nif.j3d.interp;

import javax.media.j3d.Alpha;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

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

	private Point3f iScale = new Point3f(); // interpolated scale 

	private CubicSplineCurve cubicSplineCurve = new CubicSplineCurve();

	int numSegments;

	int currentSegmentIndex;

	public RotPosScaleTCBSplinePathInterpolator(Alpha alpha, TransformGroup target, TCBKeyFrame keys[])
	{
		super(alpha, target, keys);
		// Create a spline curve using the derived key frames
		cubicSplineCurve = new CubicSplineCurve(this.keyFrames);
		numSegments = cubicSplineCurve.numSegments;
	}

	public void computeTransform(float alphaValue)
	{

		// compute the current value of u from alpha and the 
		// determine lower and upper knot points
		computePathInterpolation(alphaValue);

		// Determine the segment within which we will be interpolating
		currentSegmentIndex = this.lowerKnot - 1;

		// if we are at the start of the curve 
		if (currentSegmentIndex == 0 && currentU == 0f)
		{

			iQuat.set(keyFrames[1].quat);
			iPos.set(keyFrames[1].position);
			iScale.set(keyFrames[1].scale);

			// if we are at the end of the curve 
		}
		else if (currentSegmentIndex == (numSegments - 1) && currentU == 1.0)
		{

			iQuat.set(keyFrames[upperKnot].quat);
			iPos.set(keyFrames[upperKnot].position);
			iScale.set(keyFrames[upperKnot].scale);

			// if we are somewhere in between the curve
		}
		else
		{

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

	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setRotation(iQuat);
		targetTransform.setTranslation(iPos);
		//TODO: can't do non uniform scale in J3D
		targetTransform.setScale(iScale.x);
	}

}
