package nif.j3d.animation.hkx;

import org.jogamp.java3d.Bounds;
import org.jogamp.java3d.Group;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.animation.SequenceAlpha.SequenceAlphaListener;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.TransformTrack;

/**
 * A modification of the J3dControllerLink
 */
public class J3dTransformTrack extends Group implements SequenceAlphaListener {
	protected J3dNiInterpolator	j3dNiInterpolator	= null;

	protected J3dNiAVObject		nodeTarget			= null;

	protected boolean			isAccumNodeTarget	= false;

	private TransformTrack		transformTrack;

	public J3dTransformTrack(TransformTrack transformTrack) {
		this.transformTrack = transformTrack;

	}

	public void setBinding(	String boneName, int numFrame, float lengthS,
							J3dNiDefaultAVObjectPalette allBonesInSkeleton) {

		nodeTarget = allBonesInSkeleton.getByName(boneName);
		
		if (nodeTarget == null) {
			// this is likely fine
			//e:\game media\skyrim\meshes\actors\character\animations\female\mt_idle_a_left_long.kf
			// has animation for SkirtFBone01 which may not be in the skins

		} else {
			if (!nodeTarget.isLive() && !nodeTarget.isCompiled())
				nodeTarget.setCapability(ALLOW_BOUNDS_READ);

			j3dNiInterpolator = new J3dSplineTransformInterpolator(transformTrack, nodeTarget, numFrame, lengthS);
			addChild(j3dNiInterpolator);

		}
	}
	
	@Override
	public Bounds getBounds() {
		if (nodeTarget != null && nodeTarget.getCapability(ALLOW_BOUNDS_READ)) {
			return nodeTarget.getBounds();
		}

		//TODO: how is this not set correctly? but I need a better bounds system anyway

		return null;
	}
	
	boolean outOnce = true;
	public void process(float alphaValue) {
		if (j3dNiInterpolator != null) {			
			j3dNiInterpolator.process(alphaValue);
		}
	}

	@Override
	public void sequenceStarted() {
		//is it the accum node?
		if (isAccumNodeTarget) {
			nodeTarget.sequenceStarted();
		}
	}

	@Override
	public void sequenceFinished() {
		//is it the accum node?
		if (isAccumNodeTarget) {
			nodeTarget.sequenceFinished();
		}
	}

	@Override
	public void sequenceLooped(boolean inner) {
		if (isAccumNodeTarget) {
			nodeTarget.sequenceLooped(inner);
		}
	}

}
