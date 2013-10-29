package nif.j3d;

import javax.media.j3d.Alpha;
import javax.media.j3d.Group;

import nif.j3d.animation.J3dNiControllerManager;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.j3d.animation.interp.J3dNiTransformInterpolatorFactory;
import nif.niobject.NiAVObject;
import nif.niobject.NiExtraData;
import nif.niobject.controller.NiControllerManager;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiObjectNET;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.controller.NiTransformController;
import nif.niobject.interpolator.NiInterpolator;

public abstract class J3dNiObjectNET extends Group
{
	private NiObjectNET niObjectNET;

	private J3dNiControllerManager j3dNiControllerManager;

	private J3dNiInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;

	private NiExtraData[] extraDataList;

	public J3dNiObjectNET(NiObjectNET niObjectNET, NiToJ3dData niToJ3dData)
	{
		this.niObjectNET = niObjectNET;
		this.setName(niObjectNET.name);

		extraDataList = new NiExtraData[niObjectNET.extraDataList.length];
		for (int i = 0; i < niObjectNET.extraDataList.length; i++)
		{
			extraDataList[i] = (NiExtraData) niToJ3dData.get(niObjectNET.extraDataList[i]);
		}
	}

	public J3dNiControllerManager getJ3dNiControllerManager()
	{
		return j3dNiControllerManager;
	}

	public NiExtraData[] getExtraDataList()
	{
		return extraDataList;
	}

	/**
	 * 
	 * return true if a controller exists
	 */
	public void setupController(NiToJ3dData niToJ3dData)
	{
		NiTimeController controller = (NiTimeController) niToJ3dData.get(niObjectNET.controller);
		setupController(controller, niToJ3dData);
	}

	private void setupController(NiTimeController controller, NiToJ3dData niToJ3dData)
	{

		if (controller != null)
		{
			float startTimeS = controller.startTime;
			float stopTimeS = controller.stopTime;

			NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
			J3dNiAVObject nodeTarget = niToJ3dData.get(target);
			if (nodeTarget != null)
			{
				if (controller instanceof NiControllerManager)
				{
					j3dNiControllerManager = new J3dNiControllerManager((NiControllerManager) controller, niToJ3dData);
					addChild(j3dNiControllerManager);

				}
				else if (controller instanceof NiGeomMorpherController)
				{
					//TODO: NiGeomMorpherController
				}
				else if (controller instanceof NiMultiTargetTransformController)
				{
					// this looks like it is just an object palette for optomisation ignore?? controller link uses its single node target
				}
				else if (controller instanceof NiSingleInterpController)
				{
					NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(((NiSingleInterpController) controller).interpolator);
					if (niInterpolator != null)
					{
						if (controller instanceof NiTransformController)
						{
							//TODO: shouldn't controller be used like the other time interpolators below?
							// I appear to go straight for the transformgroup, but I should step through the controller to it
							// controller link makes this hard note
							j3dNiInterpolator = J3dNiTransformInterpolatorFactory.createTransformInterpolator(niInterpolator, niToJ3dData,
									nodeTarget, startTimeS, stopTimeS);
						}
						else
						{
							J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller,
									niToJ3dData, nodeTarget, null);

							if (j3dNiTimeController != null)
							{
								j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator,
										niToJ3dData, startTimeS, stopTimeS, -1);
							}
						}

						if (j3dNiInterpolator != null)
						{
							addChild(j3dNiInterpolator);

							baseAlpha = J3dNiTimeController.createAlpha(startTimeS, stopTimeS, -1);
							j3dNiInterpolator.fire(baseAlpha);
						}
					}
				}
			}

			NiTimeController nextController = (NiTimeController) niToJ3dData.get(controller.nextController);
			if (nextController != null)
			{
				if (nextController instanceof NiMultiTargetTransformController)
				{
					//this is an object palette ignore
				}
				else
				{
					//TODO: this is used by the particle modifer controller system, see J3dParticleSystem
					//I've also seen texturetransform controllers U then V use this
					setupController(nextController, niToJ3dData);
				}

			}
		}

	}
}
