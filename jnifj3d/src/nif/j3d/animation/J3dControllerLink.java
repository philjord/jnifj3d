package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.Group;

import nif.NifJ3dVisRoot;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifControllerLink;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.j3d.animation.interp.J3dNiTransformInterpolatorFactory;
import nif.niobject.NiStringPalette;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;

public class J3dControllerLink extends Group
{
	private J3dNiInterpolator j3dNiInterpolator = null;

	private J3dNiGeomMorpherController j3dNiGeomMorpherController = null;

	private String nodeName = "";

	private String variable1 = "";

	private String variable2 = "";

	public J3dControllerLink(NifControllerLink controllerLink, NiToJ3dData niToJ3dData, float startTimeS, float stopTimeS,
			J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		float lengthS = stopTimeS - startTimeS;

		if (niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_20_1_0_3)
		{
			nodeName = controllerLink.nodeName;
			variable1 = controllerLink.variable1;
			variable2 = controllerLink.variable2;
		}
		else if (niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
		{
			// lets get the target name
			int nodeNameOffset = controllerLink.nodeNameOffset.offset;
			nodeName = lookUpPaletteString(nodeNameOffset, niToJ3dData, controllerLink.stringPalette);
			int o1 = controllerLink.variable1Offset.offset;
			if (o1 != -1)
				variable1 = lookUpPaletteString(o1, niToJ3dData, controllerLink.stringPalette);
			int o2 = controllerLink.variable2Offset.offset;
			if (o2 != -1)
				variable2 = lookUpPaletteString(o2, niToJ3dData, controllerLink.stringPalette);
		}

		J3dNiAVObject nodeTarget = allBonesInSkeleton.get(nodeName);
		NiToJ3dData targetNiToJ3dData = niToJ3dData;

		// sometimes we are also controlling nigeomorphs from teh skin files
		if (nodeTarget == null && allOtherModels != null)
		{
			for (NifJ3dVisRoot otherModel : allOtherModels)
			{
				NiToJ3dData otherNiToJ3dData = otherModel.getNiToJ3dData();
				nodeTarget = otherModel.getNiToJ3dData().get(nodeName);

				if (nodeTarget != null)
				{
					targetNiToJ3dData = otherNiToJ3dData;
					break;
				}
			}
		}

		if (nodeTarget == null)
		{
			// this is likely fine
			//new Exception("NULL nodeTarget!!! " + controllerLink.nodeName + " " + niToJ3dData.nifVer).printStackTrace();
			//e:\game media\skyrim\meshes\actors\character\animations\female\mt_idle_a_left_long.kf
			// has animation for SkirtFBone01 which may not be in the skins

		}
		else
		{
			NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(controllerLink.interpolator);

			String controllerType = "";
			if (niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_20_1_0_3)
			{
				controllerType = controllerLink.controllerType;
			}
			else if (niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
			{
				// lets get the target name
				int controllerTypeOffset = controllerLink.controllerTypeOffset.offset;
				controllerType = lookUpPaletteString(controllerTypeOffset, niToJ3dData, controllerLink.stringPalette);
			}

			//NOTE controller can be null but transform still valid, can't rely on controller type?
			if (controllerType.equals("NiTransformController"))
			{
				//NiTransformController
				//NiMultiTargetTransformController
				j3dNiInterpolator = J3dNiTransformInterpolatorFactory.createTransformInterpolator(niInterpolator, targetNiToJ3dData,
						nodeTarget, startTimeS, lengthS);
			}
			else if (controllerType.equals("NiGeomMorpherController"))
			{
				
				J3dNiAVObject targetParent  = targetNiToJ3dData.get(controllerLink.nodeName);
				
				j3dNiGeomMorpherController = targetParent.getJ3dNiGeomMorpherController();
				System.out.println("I just seen a NiGeomMorpherController under a J3dControllerLink!  I should link them together "
						+ controllerLink.nodeName + " " + niToJ3dData.nifVer);
				
				j3dNiInterpolator = j3dNiGeomMorpherController.setFrameName(variable2);
			}
			else
			{
				NiTimeController controller = (NiTimeController) targetNiToJ3dData.get(controllerLink.controller);
				if (controller != null)
				{
					J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, targetNiToJ3dData,
							nodeTarget, null);

					if (j3dNiTimeController != null)
					{
						j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator,
								targetNiToJ3dData, startTimeS, stopTimeS);
					}
				}

			}
			if (j3dNiInterpolator != null)
			{
				addChild(j3dNiInterpolator);
			}
		}
	}

	public void process(float alphaValue)
	{
		if (j3dNiInterpolator != null)
		{
			j3dNiInterpolator.process(alphaValue);
		}
	}

	public static String lookUpPaletteString(int offset, NiToJ3dData niToJ3dData, NifRef stringPaletteRef)
	{
		if (niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
		{
			NiStringPalette stringPalette = (NiStringPalette) niToJ3dData.get(stringPaletteRef);
			if (stringPalette != null)
			{
				String paletteString = stringPalette.palette.palette;
				return paletteString.substring(offset, paletteString.indexOf(0, offset));
			}
		}
		else
		{
			System.out.println("Bad NifVer for string palette lookup! " + niToJ3dData.nifVer);
		}
		return null;
	}

}
