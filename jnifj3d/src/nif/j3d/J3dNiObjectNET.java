package nif.j3d;

import javax.media.j3d.BranchGroup;

import nif.j3d.animation.J3dNiControllerManager;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.animation.J3dNiSingleInterpController;
import nif.j3d.animation.J3dNiTimeController;
import nif.niobject.NiExtraData;
import nif.niobject.controller.NiControllerManager;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiObjectNET;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;

public abstract class J3dNiObjectNET extends BranchGroup
{
	private NiObjectNET niObjectNET;

	private J3dNiTimeController j3dNiTimeController;

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
		if (j3dNiTimeController instanceof J3dNiControllerManager)
			return (J3dNiControllerManager) j3dNiTimeController;
		else
			return null;
	}

	public J3dNiGeomMorpherController getJ3dNiGeomMorpherController()
	{
		if (j3dNiTimeController instanceof J3dNiGeomMorpherController)
			return (J3dNiGeomMorpherController) j3dNiTimeController;
		else
			return null;
	}

	public J3dNiTimeController getJ3dNiTimeController()
	{
		return j3dNiTimeController;
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
		j3dNiTimeController = setupController(controller, niToJ3dData);
		if (j3dNiTimeController != null)
		{
			addChild(j3dNiTimeController);
		}
	}

	private J3dNiTimeController setupController(NiTimeController controller, NiToJ3dData niToJ3dData)
	{
		J3dNiTimeController ret = null;
		if (controller != null)
		{
			if (controller instanceof NiControllerManager)
			{
				ret = new J3dNiControllerManager((NiControllerManager) controller, niToJ3dData);
			}
			else if (controller instanceof NiGeomMorpherController)
			{
				ret = new J3dNiGeomMorpherController((NiGeomMorpherController) controller, niToJ3dData);
			}
			else if (controller instanceof NiSingleInterpController)
			{
				ret = new J3dNiSingleInterpController((NiSingleInterpController) controller, niToJ3dData);
			}
			else if (controller instanceof NiMultiTargetTransformController)
			{
				// this looks like it is just an object palette for optomisation ignore?? controller link uses its single node target
			}

			if (ret != null)
			{
				NiTimeController nextController = (NiTimeController) niToJ3dData.get(controller.nextController);
				if (nextController != null)
				{
					J3dNiTimeController jtc2 = setupController(nextController, niToJ3dData);
					if (jtc2 != null)
					{
						ret.setJ3dNiTimeController(jtc2);
					}
				}
			}

		}
		return ret;
	}
}
