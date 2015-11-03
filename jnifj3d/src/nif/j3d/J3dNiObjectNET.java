package nif.j3d;

import java.util.ArrayList;

import javax.media.j3d.Group;

import nif.NifVer;
import nif.j3d.animation.J3dNiControllerManager;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.animation.J3dNiSingleInterpController;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.J3dNiUVController;
import nif.niobject.NiExtraData;
import nif.niobject.controller.NiControllerManager;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.controller.NiKeyframeController;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiObjectNET;
import nif.niobject.controller.NiPathController;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.controller.NiUVController;

public abstract class J3dNiObjectNET extends Group
{
	private NiObjectNET niObjectNET;

	private J3dNiTimeController j3dNiTimeController;

	private NiExtraData[] extraDataList;

	public J3dNiObjectNET(NiObjectNET niObjectNET, NiToJ3dData niToJ3dData)
	{
		this.niObjectNET = niObjectNET;
		this.setName(niObjectNET.name);

		buildExtraDataInList(niObjectNET, niToJ3dData);

	}

	private void buildExtraDataInList(NiObjectNET niObjectNET2, NiToJ3dData niToJ3dData)
	{
		//early versions used a chain, build a list if needed
		if (niObjectNET2.nVer.LOAD_VER <= NifVer.VER_4_2_2_0)
		{
			ArrayList<NiExtraData> niExtraDatas = new ArrayList<NiExtraData>();
			NiExtraData ned = (NiExtraData) niToJ3dData.get(niObjectNET.extraData);
			while (ned != null)
			{
				niExtraDatas.add(ned);
				ned = (NiExtraData) niToJ3dData.get(ned.NextExtraData);
			}
			extraDataList = new NiExtraData[niExtraDatas.size()];
			niExtraDatas.toArray(extraDataList);
		}
		else
		{
			extraDataList = new NiExtraData[niObjectNET.extraDataList.length];
			for (int i = 0; i < niObjectNET.extraDataList.length; i++)
			{
				extraDataList[i] = (NiExtraData) niToJ3dData.get(niObjectNET.extraDataList[i]);
			}
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
			else if (controller instanceof NiKeyframeController)
			{
				// ignored as we don't have a target yet for this (which is a controllerlink type thing anyway)
				// this gets sorted out as a special operation
			}
			else if (controller instanceof NiMultiTargetTransformController)
			{
				// this looks like it is just an object palette for optomisation ignore?? controller link uses its single node target
			}
			else if (controller instanceof NiPathController)
			{
				// TODO: handle NiPathController, much like the UV controller below
			}
			else if (controller instanceof NiUVController)
			{
				ret = new J3dNiUVController((NiUVController) controller, niToJ3dData);
			}
			else
			{
				System.out.println("Unknown controller set up " + controller + " in " + controller.nVer.fileName);
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
