package nif.j3d.animation;

import javax.media.j3d.Alpha;
import javax.media.j3d.Node;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiTransformInterpolatorFactory;
import nif.niobject.NiAVObject;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTransformController;
import nif.niobject.interpolator.NiInterpolator;

public class J3dNiSingleInterpController extends J3dNiTimeController
{
	private J3dNiInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;

	public J3dNiSingleInterpController(NiSingleInterpController controller, NiToJ3dData niToJ3dData)
	{
		super(controller, null);
		float startTimeS = controller.startTime;
		float stopTimeS = controller.stopTime;

		NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
		nodeTarget = niToJ3dData.get(target);
		if (nodeTarget != null)
		{
			nodeTarget.setCapability(Node.ALLOW_BOUNDS_READ);
			NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(controller.interpolator);
			if (niInterpolator != null)
			{
				if (controller instanceof NiTransformController)
				{
					//TODO: shouldn't controller be used like the other  interpolators below?
					// I appear to go straight for the transformgroup, but I should step through the controller to it
					// controller link makes this hard note
					j3dNiInterpolator = J3dNiTransformInterpolatorFactory.createTransformInterpolator(niInterpolator, niToJ3dData,
							(J3dNiAVObject) nodeTarget, startTimeS, stopTimeS);
				}
				else
				{
					J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, niToJ3dData,
							(J3dNiAVObject) nodeTarget, null);

					if (j3dNiTimeController != null)
					{
						j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator, niToJ3dData,
								startTimeS, stopTimeS);
					}
				}

				if (j3dNiInterpolator != null)
				{
					addChild(j3dNiInterpolator);
					baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
					// single interp controllers are just animations they always run, they are not controller links
					j3dNiInterpolator.fire(baseAlpha);
				}
			}
		}

	}

	public J3dNiInterpolator getJ3dNiInterpolator()
	{
		return j3dNiInterpolator;
	}
}
