package nif.j3d.animation.hkx;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import nif.compound.NifQuaternion;
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
			defaultRot = ConvertFromNif.toJ3d(transformTrack.StaticRotation);
		}
		if (transformTrack.HasSplinePosition) {
			positionPathInterpolator = new TTPosPathInterpolator(numFrame, transformTrack.SplinePosition);
		} else {
			defaultTrans = ConvertFromNif.toJ3d(transformTrack.StaticPosition);
		}
		if (transformTrack.HasSplineScale) {
			scalePathInterpolator = new TTScalePathInterpolator(numFrame, transformTrack.SplineScale);
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
		private SplineTrackQuaternion	quatKeys;
		private float					maxFrames;
		private Quat4f					tQuat;		// for holding the computed value

		public TTRotPathInterpolator(float maxFrames, SplineTrackQuaternion quatKeys) {
			this.maxFrames = maxFrames;
			this.quatKeys = quatKeys;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;
			NifQuaternion nq = quatKeys.GetValue(frameNo);
			tQuat = ConvertFromNif.toJ3d(nq);
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setRotation(tQuat);
		}

	}

	public static class TTPosPathInterpolator extends TTKnotInterpolator {
		private SplineTrackVector3	splinePosition;
		private float				maxFrames;
		private Vector3f			pos	= new Vector3f();	// for holding the computed value

		public TTPosPathInterpolator(float maxFrames, SplineTrackVector3 splinePosition) {
			this.maxFrames = maxFrames;
			this.splinePosition = splinePosition;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;
			pos.set(splinePosition.GetValueX(frameNo), splinePosition.GetValueY(frameNo),
					splinePosition.GetValueZ(frameNo));
			pos.scale(ConvertFromHavok.getHavokScale(null));
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setTranslation(pos);
		}
	}

	public static class TTScalePathInterpolator extends TTKnotInterpolator {
		private SplineTrackVector3	splineScale;
		private float				maxFrames;
		private Vector3d			tScale	= new Vector3d();	// for holding the computed value

		public TTScalePathInterpolator(float maxFrames, SplineTrackVector3 splineScale) {
			this.maxFrames = maxFrames;
			this.splineScale = splineScale;
		}

		@Override
		public void computeTransform(float alphaValue) {
			float frameNo = alphaValue * maxFrames;
			tScale.set(splineScale.GetValueX(frameNo), splineScale.GetValueY(frameNo), splineScale.GetValueZ(frameNo));
			tScale.scale(ConvertFromHavok.getHavokScale(null));
		}

		@Override
		public void applyTransform(Transform3D targetTransform) {
			targetTransform.setScale(tScale);
		}
	}
}
