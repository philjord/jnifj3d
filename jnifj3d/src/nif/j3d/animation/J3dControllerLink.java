package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.Bounds;
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
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;

public class J3dControllerLink extends Group
{
	protected J3dNiInterpolator j3dNiInterpolator = null;

	protected J3dNiAVObject nodeTarget = null;

	private J3dNiGeomMorpherController j3dNiGeomMorpherController = null;

	private String nodeName = "";

	private String controllerType = "";

	//private String variable1 = "";

	private String variable2 = "";

	// for TES3
	protected J3dControllerLink()
	{

	}

	public J3dControllerLink(NifControllerLink controllerLink, NiToJ3dData niToJ3dData, float startTimeS, float stopTimeS,
			J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		float lengthS = stopTimeS - startTimeS;

		if (niToJ3dData.nifVer.LOAD_VER == NifVer.VER_10_1_0_106 || niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_20_1_0_3)
		{
			nodeName = controllerLink.nodeName;
			//variable1 = controllerLink.variable1;
			variable2 = controllerLink.variable2;
			controllerType = controllerLink.controllerType;
		}
		else if (niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_10_2_0_0 && niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
		{
			int nodeNameOffset = controllerLink.nodeNameOffset.offset;
			if (nodeNameOffset != -1)
				nodeName = lookUpPaletteString(nodeNameOffset, niToJ3dData, controllerLink.stringPalette);
			//int o1 = controllerLink.variable1Offset.offset;
			//if (o1 != -1)
			//	variable1 = lookUpPaletteString(o1, niToJ3dData, controllerLink.stringPalette);
			int o2 = controllerLink.variable2Offset.offset;
			if (o2 != -1)
				variable2 = lookUpPaletteString(o2, niToJ3dData, controllerLink.stringPalette);
			int controllerTypeOffset = controllerLink.controllerTypeOffset.offset;
			if (controllerTypeOffset != -1)
				controllerType = lookUpPaletteString(controllerTypeOffset, niToJ3dData, controllerLink.stringPalette);
		}

		nodeTarget = allBonesInSkeleton.get(nodeName);
		NiToJ3dData targetNiToJ3dData = niToJ3dData;

		// sometimes we are also controlling NiGeomorpherController from the skin files
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
			//e:\game media\skyrim\meshes\actors\character\animations\female\mt_idle_a_left_long.kf
			// has animation for SkirtFBone01 which may not be in the skins

		}
		else
		{
			NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(controllerLink.interpolator);

			//NOTE controller can be null as a transfrom interpolator can directly set the node targets transform
			if (controllerType.equals("NiTransformController"))
			{
				//NiTransformController
				//NiMultiTargetTransformController
				j3dNiInterpolator = J3dNiTransformInterpolatorFactory.createTransformInterpolator(niInterpolator, niToJ3dData, nodeTarget,
						startTimeS, lengthS);
				addChild(j3dNiInterpolator);
			}
			else if (controllerType.equals("NiGeomMorpherController"))
			{
				J3dNiAVObject targetParent = targetNiToJ3dData.get(nodeName);
				j3dNiGeomMorpherController = targetParent.getJ3dNiGeomMorpherController();
				//note set frame name called on process
				j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiGeomMorpherController, niInterpolator, niToJ3dData,
						startTimeS, stopTimeS);

				addChild(j3dNiInterpolator);
			}
			else
			{
				NiTimeController controller = (NiTimeController) niToJ3dData.get(controllerLink.controller);
				if (controller != null)
				{
					J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, niToJ3dData,
							nodeTarget, null);

					if (j3dNiTimeController != null)
					{
						j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator, niToJ3dData,
								startTimeS, stopTimeS);
						addChild(j3dNiInterpolator);
					}
				}

			}

		}
	}

	@Override
	public Bounds getBounds()
	{
		if (nodeTarget != null)
		{
			return nodeTarget.getBounds();
		}
		return null;
	}

	public void process(float alphaValue)
	{
		// because we need to ensure the controller is running our frame (could be set by another link at some point)
		if (j3dNiGeomMorpherController != null)
		{
			 
			j3dNiGeomMorpherController.setFrameName(variable2);
		}

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
