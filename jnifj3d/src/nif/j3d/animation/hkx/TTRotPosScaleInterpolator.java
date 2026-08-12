package nif.j3d.animation.hkx;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.animation.hkx.J3dSplineTransformInterpolator.TTPosPathInterpolator;
import nif.j3d.animation.hkx.J3dSplineTransformInterpolator.TTRotPathInterpolator;
import nif.j3d.animation.hkx.J3dSplineTransformInterpolator.TTScalePathInterpolator;
import nif.j3d.animation.j3dinterp.interp.TransformInterpolator;
import tools3d.utils.Utils3D;

public class TTRotPosScaleInterpolator extends TransformInterpolator {
	private TTRotPathInterpolator	quatRotInterpolator;
	private TTPosPathInterpolator	positionPathInterpolator;
	private TTScalePathInterpolator	scalePathInterpolator;

	private Vector3f				defaultTrans	= null;

	private Quat4f					defaultRot		= null;

	private float					defaultScale	= 1;

	private Transform3D				baseTransform	= null;

	public TTRotPosScaleInterpolator(	TransformGroup target, float lengthS, TTRotPathInterpolator quatRotInterpolator,
										TTPosPathInterpolator positionPathInterpolator,
										TTScalePathInterpolator scalePathInterpolator, Vector3f defaultTrans,
										Quat4f defaultRot, float defaultScale) {
		super(target, 0, lengthS); // start time always 0
		this.positionPathInterpolator = positionPathInterpolator;
		this.defaultTrans = defaultTrans;
		this.scalePathInterpolator = scalePathInterpolator;
		this.defaultScale = defaultScale;
		this.quatRotInterpolator = quatRotInterpolator;
		this.defaultRot = defaultRot;

	}

	/**
	 * Method overrride as we have 3 elements to update 
	 * @see nif.j3d.animation.j3dinterp.J3dNiInterpolator#process(float)
	 */
	@Override
	public void process(float alphaValue) {
		// preserve the target values if interps have no defaults		
		if (baseTransform == null) {
			baseTransform = new Transform3D();
			target.getTransform(baseTransform);
		}

		// convert to an offsetted time in seconds
		float normAlphaValue = alphaValue * lengthS;
		normAlphaValue += startTimeS;

		// set to the base target
		targetTransform.set(baseTransform);

		if (alphaValue != prevAlphaValue) {
			if (quatRotInterpolator != null) {
				quatRotInterpolator.computeTransform(normAlphaValue);
				quatRotInterpolator.applyTransform(targetTransform);
			} else if (defaultRot != null) {
				targetTransform.setRotation(defaultRot);
			}

			if (positionPathInterpolator != null) {
				positionPathInterpolator.computeTransform(normAlphaValue);
				positionPathInterpolator.applyTransform(targetTransform);
			} else if (defaultTrans != null) {
				targetTransform.setTranslation(defaultTrans);
			}

			if (scalePathInterpolator != null) {
				scalePathInterpolator.computeTransform(normAlphaValue);
				scalePathInterpolator.applyTransform(targetTransform);
			} else if (defaultScale != Float.MIN_VALUE) {
				targetTransform.setScale(defaultScale);
			}

			if (!Utils3D.isAffine(targetTransform)) {
				System.out.println("no no good");
			} else {
				//only set on a change
				if (!targetTransform.equals(prevTargetTransform)) {
					target.setTransform(targetTransform);
					prevTargetTransform.set(targetTransform);
				}
			}

			prevAlphaValue = alphaValue;
		}

	}

	@Override
	public void computeTransform(float alphaValue) {
		//dummy as process does it special
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyTransform(Transform3D t) {
		//dummy as process does it special		
		throw new UnsupportedOperationException();
	}

	public static abstract class TTKnotInterpolator {
		public abstract void computeTransform(float alphaValue);

		public abstract void applyTransform(Transform3D t);
	}
}
