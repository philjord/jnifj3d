package nif.j3d.animation.hkx;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import nif.compound.NifQuaternionXYZW;
import nif.compound.NifVector3;
import nif.j3d.animation.j3dinterp.interp.TransformInterpolator;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.SplineTrackQuaternion;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.SplineTrackVector3;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.TransformTrack;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromHavok;
import utils.convert.ConvertFromNif;

/**
 * Based on RotPosScaleInterpolator
 */
public class TTRotPosScaleInterpolator extends TransformInterpolator {

	private Transform3D			baseTransform	= null;

	private TransformTrack[]	transformTracks;

	private float				maxFrames;
	private int					framesPerBlock;

	public TTRotPosScaleInterpolator(	TransformGroup target, float lengthS, int maxFrames, int framesPerBlock,
										TransformTrack[] transformTracks) {
		super(target, 0, lengthS); // start time always 0

		this.transformTracks = transformTracks;
		this.maxFrames = maxFrames;
		this.framesPerBlock = framesPerBlock;

		// examples of multi blocks
		//ArchiveFile:Skyrim - Animations.bsa/meshes/actors/character/animations/animobjectchoploop.hkx
		//animationTracks.transformBlocks.size() != 1 : 3
		//block duration = 4.233333
		//anim duration 10.9

		//or

		//Animation file: meshes/actors/character/animations/1hm_idle.hkx
		//block duration = 8.5
		//anim duration 9.0

		// transformTracks is block long, each needs to be end on end of the previous, note masks do NOT have to be the same

	}

	// deburners
	private Quat4f		tQuat			= new Quat4f();				// for holding the computed value
	private NifVector3	temp			= new NifVector3(0, 0, 0);	// for holding the computed value
	private Vector3f	pos				= new Vector3f(0, 0, 0);
	private Vector3d	tScale			= new Vector3d();			// for holding the computed value

	private Vector3f	defaultTrans	= new Vector3f();
	private Quat4f		defaultRot		= new Quat4f();
	private float		defaultScale	= 1;

	/**
	 * Method override as we have 3 elements to update 
	 * @see nif.j3d.animation.j3dinterp.J3dNiInterpolator#process(float)
	 */
	@Override
	public void process(float alphaValue) {
		// preserve the target values if interps have no defaults, only grab it the first time round		
		if (baseTransform == null) {
			baseTransform = new Transform3D();
			target.getTransform(baseTransform);
		}

		// set to the base target
		targetTransform.set(baseTransform);

		if (alphaValue != prevAlphaValue) {
			float frameNo = alphaValue * maxFrames;

			int blockNo = (int)(frameNo / framesPerBlock);
			float frameInBlock = frameNo % framesPerBlock;

			//System.out.println("some info"	+ " alphaValue " + alphaValue + " frameNo " + frameNo + " framesPerBlock "
			//					+ framesPerBlock + " blockNo " + blockNo + " frameInBlock " + frameInBlock);

			TransformTrack transformTrack = transformTracks[blockNo];

			if (transformTrack.HasSplineRotation) {
				//TODO: given that there is a +2 on the knot count I wager there is room to be below 0 or just above 1 
				// after rounding etc, so I bet that might be part of this calc? maybe. 
				SplineTrackQuaternion quatKeys = transformTrack.SplineRotation;
				NifQuaternionXYZW nq = quatKeys.GetValue(frameInBlock);
				ConvertFromHavok.toJ3d(nq, tQuat);
				tQuat.normalize();// as suggested in the havok docs	

				targetTransform.setRotation(tQuat);
			} else {
				ConvertFromHavok.toJ3d(transformTrack.StaticRotation, defaultRot);
				targetTransform.setRotation(defaultRot);
			}

			if (transformTrack.HasSplinePosition) {
				SplineTrackVector3 splinePosition = transformTrack.SplinePosition;
				splinePosition.GetValueXYZ(frameInBlock, temp);
				Vector3f staticPosition = ConvertFromNif.toJ3d(transformTrack.StaticPosition);
				temp.set(!Float.isNaN(temp.x) ? temp.x : staticPosition.x,
						!Float.isNaN(temp.y) ? temp.y : staticPosition.y,
						!Float.isNaN(temp.z) ? temp.z : staticPosition.z);

				ConvertFromNif.toJ3d(temp, pos);

				targetTransform.setTranslation(pos);
			} else {
				ConvertFromNif.toJ3d(transformTrack.StaticPosition, defaultTrans);
				targetTransform.setTranslation(defaultTrans);
			}

			if (transformTrack.HasSplineScale) {
				SplineTrackVector3 splineScale = transformTrack.SplineScale;
				splineScale.GetValueXYZ(frameInBlock, temp);
				NifVector3 staticScale = transformTrack.StaticScale;
				tScale.set(!Float.isNaN(temp.x) ? temp.x : staticScale.x, //
						!Float.isNaN(temp.y) ? temp.y : staticScale.y, //
						!Float.isNaN(temp.z) ? temp.z : staticScale.z);
				//notice z/y swap but no negation, and no change of scale
				ConvertFromNif.toJ3d(tScale);

				targetTransform.setScale(tScale);

			} else {
				//Notice scale is a percentage change so no Nif conversion done!
				defaultScale = transformTrack.StaticScale.x;
				targetTransform.setScale(defaultScale);
			}

			if (!Utils3D.isAffine(targetTransform)) {
				System.out.println(
						"no no good Utils3D.isAffine(targetTransform) in TTRotPosScaleInterpolator.process(float alphaValue)");
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
}
