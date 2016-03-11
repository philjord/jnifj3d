package nif.j3d.animation.tes3;

import javax.media.j3d.Alpha;
import javax.media.j3d.Node;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiTransformInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiKeyframeData;
import nif.niobject.controller.NiKeyframeController;

/**
 * this is just a interp constructor with the right target and time in it
 * a controller link can use this to run offsetted interp chunk for an animation
 * this is a bit like J3dNiSingleInterpController
 * @author phil
 * @See J3dNiSingleInterpController
 *
 */
public class J3dNiKeyframeController extends J3dNiTimeController
{
	private J3dNiTransformInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;

	private float totalLengthS;

	/**
	 * For use like a single interp controller
	 * @param controller
	 * @param niToJ3dData
	 */
	public J3dNiKeyframeController(NiKeyframeController controller, NiToJ3dData niToJ3dData)
	{
		super(controller, null);
		NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
		nodeTarget = niToJ3dData.get(target);
		if (nodeTarget != null)
		{
			nodeTarget.setCapability(Node.ALLOW_BOUNDS_READ);
			
			float startTimeS = controller.startTime;
			float stopTimeS = controller.stopTime;
			totalLengthS = stopTimeS - startTimeS;

			NiKeyframeData niTransformData = (NiKeyframeData) niToJ3dData.get(controller.data);
			NifTransformGroup targetTransform = ((J3dNiAVObject) nodeTarget).getTransformGroup();

			j3dNiInterpolator = new J3dNiTransformInterpolator(niTransformData, targetTransform, startTimeS, totalLengthS);

			if (j3dNiInterpolator != null)
			{
				addChild(j3dNiInterpolator);

				baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
				j3dNiInterpolator.fire(baseAlpha);
			}
		}
	}

	/**
	 * For use like by controller link in kf style
	 * @param controller
	 * @param niToJ3dData
	 * @param nodeTarget
	 */
	public J3dNiKeyframeController(NiKeyframeController controller, NiToJ3dData niToJ3dData, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);

		float totalStartTimeS = controller.startTime;
		float totalStopTimeS = controller.stopTime;
		totalLengthS = totalStopTimeS - totalStartTimeS;

		NiKeyframeData niTransformData = (NiKeyframeData) niToJ3dData.get(controller.data);
		NifTransformGroup targetTransform = nodeTarget.getTransformGroup();

		j3dNiInterpolator = new J3dNiTransformInterpolator(niTransformData, targetTransform, totalStartTimeS, totalLengthS);
	}

	public J3dNiAVObject getNodeTarget()
	{
		return (J3dNiAVObject) nodeTarget;
	}

	public J3dNiInterpolator getJ3dNiInterpolator()
	{
		return j3dNiInterpolator;
	}

	public float getTotalLengthS()
	{
		return totalLengthS;
	}
}
