package nif.j3d.animation.tes3;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.j3d.animation.interp.J3dNiTransformInterpolator;
import nif.niobject.NiKeyframeData;
import nif.niobject.controller.NiKeyframeController;

/**
 * this is just a interp constructor with the right target and time in it
 * a controller link uses this to run offsetted interp chunk for an animation
 * @author phil
 *
 */
public class J3dNiKeyframeController
{
	public J3dNiAVObject nodeTarget;

	public J3dNiTransformInterpolator j3dNiInterpolator;

	public float totalStartTimeS;

	public float totalStopTimeS;

	public float totalLengthS;

	public J3dNiKeyframeController(NiKeyframeController controller, NiToJ3dData niToJ3dData, J3dNiAVObject nodeTarget)
	{
		this.nodeTarget = nodeTarget;

		totalStartTimeS = controller.startTime;
		totalStopTimeS = controller.stopTime;
		totalLengthS = totalStartTimeS - totalStopTimeS;

		NiKeyframeData niTransformData = (NiKeyframeData) niToJ3dData.get(controller.data);
		NifTransformGroup targetTransform = nodeTarget.getTransformGroup();

		j3dNiInterpolator = new J3dNiTransformInterpolator(niTransformData, targetTransform, totalStartTimeS, totalLengthS);
	}
}

