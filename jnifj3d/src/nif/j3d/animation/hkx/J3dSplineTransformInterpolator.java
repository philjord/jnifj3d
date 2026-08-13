package nif.j3d.animation.hkx;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import nif.compound.NifQuaternionXYZW;
import nif.compound.NifVector3;
import nif.j3d.animation.hkx.TTRotPosScaleInterpolator.TTKnotInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.SplineTrackQuaternion;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.SplineTrackVector3;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.TransformTrack;
import utils.convert.ConvertFromHavok;
import utils.convert.ConvertFromNif;

/**
 * Very similar to a J3dNiTransformInterpolator
 */
public class J3dSplineTransformInterpolator extends J3dNiInterpolator {
	//private TransformTrack									transformTrack;

	private TTRotPathInterpolator	quatRotInterpolator;

	private Quat4f					defaultRot		= null;

	private TTPosPathInterpolator	positionPathInterpolator;

	private Vector3f				defaultTrans	= null;

	private TTScalePathInterpolator	scalePathInterpolator;

	private float					defaultScale	= Float.MIN_VALUE;

	/*	public static boolean									CACHE_WEAK			= true;
		private static Map<TransformTrack, QuatRotationData>	quatRotationDataMap	= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, QuatRotationData>());
	
		private static Map<TransformTrack, TranslationData>		translationDataMap	= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, TranslationData>());
	
		private static Map<TransformTrack, ScaleData>			scaleDataMap		= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, ScaleData>());*/

	public J3dSplineTransformInterpolator(	TransformTrack transformTrack, TransformGroup targetTransform, int numFrame,
											float lengthS) {
		if (transformTrack.HasSplineRotation) {
			quatRotInterpolator = new TTRotPathInterpolator(numFrame, transformTrack.SplineRotation);
		} else {
			defaultRot = ConvertFromHavok.toJ3d(transformTrack.StaticRotation);
		}
		if (transformTrack.HasSplinePosition) {
			positionPathInterpolator = new TTPosPathInterpolator(numFrame, transformTrack.SplinePosition,
					ConvertFromNif.toJ3d(transformTrack.StaticPosition));
		} else {
			defaultTrans = ConvertFromNif.toJ3d(transformTrack.StaticPosition);
		}
		if (transformTrack.HasSplineScale) {
			scalePathInterpolator = new TTScalePathInterpolator(numFrame, transformTrack.SplineScale,
					transformTrack.StaticScale);
		} else {
			//Notice scale is a percentage change so no Nif conversion done!
			defaultScale = transformTrack.StaticScale.x;
		}

		TTRotPosScaleInterpolator splinePathInterpolator = new TTRotPosScaleInterpolator(
				J3dNiInterpolator.prepTransformGroup(targetTransform), lengthS, quatRotInterpolator,
				positionPathInterpolator, scalePathInterpolator, defaultTrans, defaultRot, defaultScale);

		setInterpolator(splinePathInterpolator);

	}

	public static class TTRotPathInterpolator extends TTKnotInterpolator {
		SplineTrackQuaternion	quatKeys;
		float					maxFrames;
		Quat4f					tQuat	= new Quat4f();	// for holding the computed value

		public TTRotPathInterpolator(float maxFrames, SplineTrackQuaternion quatKeys) {
			this.maxFrames = maxFrames;
			this.quatKeys = quatKeys;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;
			NifQuaternionXYZW nq = quatKeys.GetValue(frameNo);
			ConvertFromHavok.toJ3d(nq, tQuat);
			tQuat.normalize();// as suggested in the havok docs
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setRotation(tQuat);
		}

	}

	public static class TTPosPathInterpolator extends TTKnotInterpolator {
		private SplineTrackVector3	splinePosition;
		private Vector3f			staticPosition;
		private float				maxFrames;
		private NifVector3			temp	= new NifVector3(0, 0, 0);	// for holding the computed value
		private Vector3f			pos		= new Vector3f(0, 0, 0);

		public TTPosPathInterpolator(float maxFrames, SplineTrackVector3 splinePosition, Vector3f staticPosition) {
			this.maxFrames = maxFrames;
			this.splinePosition = splinePosition;
			this.staticPosition = staticPosition;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;

			float x = splinePosition.GetValueX(frameNo);
			float y = splinePosition.GetValueY(frameNo);
			float z = splinePosition.GetValueZ(frameNo);
			temp.set(!Float.isNaN(x) ? x : staticPosition.x, !Float.isNaN(y) ? y : staticPosition.y,
					!Float.isNaN(z) ? z : staticPosition.z);

			ConvertFromNif.toJ3d(temp, pos);
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setTranslation(pos);
		}
	}

	public static class TTScalePathInterpolator extends TTKnotInterpolator {
		private SplineTrackVector3	splineScale;
		private NifVector3			staticScale;
		private float				maxFrames;
		private Vector3d			tScale	= new Vector3d();	// for holding the computed value

		public TTScalePathInterpolator(float maxFrames, SplineTrackVector3 splineScale, NifVector3 staticScale) {
			this.maxFrames = maxFrames;
			this.splineScale = splineScale;
			this.staticScale = staticScale;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;
			float x = splineScale.GetValueX(frameNo);
			float y = splineScale.GetValueY(frameNo);
			float z = splineScale.GetValueZ(frameNo);
			tScale.set(!Float.isNaN(x) ? x : staticScale.x, !Float.isNaN(y) ? y : staticScale.y,
					!Float.isNaN(z) ? z : staticScale.z);
			//notice z/y swap but no negation, and no change of scale
			ConvertFromNif.toJ3d(tScale);
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setScale(tScale);
		}
	}
}
