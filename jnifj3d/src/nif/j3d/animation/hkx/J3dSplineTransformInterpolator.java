package nif.j3d.animation.hkx;

import org.jogamp.java3d.TransformGroup;

import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.TransformTrack;

/**
 * Very similar to a J3dNiTransformInterpolator
 */
public class J3dSplineTransformInterpolator extends J3dNiInterpolator {

	/*	public static boolean									CACHE_WEAK			= true;
		private static Map<TransformTrack, QuatRotationData>	quatRotationDataMap	= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, QuatRotationData>());
	
		private static Map<TransformTrack, TranslationData>		translationDataMap	= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, TranslationData>());
	
		private static Map<TransformTrack, ScaleData>			scaleDataMap		= Collections
				.synchronizedMap(new WeakValueHashMap<TransformTrack, ScaleData>());*/

	TransformTrack[] transformTracks;

	public J3dSplineTransformInterpolator(	TransformTrack[] transformTracks, TransformGroup targetTransform,
											int numFrame, int framesPerBlock, float lengthS) {
		this.transformTracks = transformTracks;

		TTRotPosScaleInterpolator splinePathInterpolator = new TTRotPosScaleInterpolator(
				J3dNiInterpolator.prepTransformGroup(targetTransform), lengthS, numFrame, framesPerBlock,
				transformTracks);

		setInterpolator(splinePathInterpolator);
	}

}
