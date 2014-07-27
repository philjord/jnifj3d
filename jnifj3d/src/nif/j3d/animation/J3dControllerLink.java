package nif.j3d.animation;

import javax.media.j3d.Group;

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
	private J3dNiInterpolator j3dNiInterpolator;

	private String nodeName = "";

	public J3dControllerLink(NifControllerLink controllerLink, NiToJ3dData niToJ3dData, float startTimeS, float stopTimeS,
			J3dNiDefaultAVObjectPalette allBonesInSkeleton)
	{
		float lengthS = stopTimeS - startTimeS;

		if (niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_20_1_0_3)
		{
			nodeName = controllerLink.nodeName;
		}
		else if (niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
		{
			// lets get the target name
			int nodeNameOffset = controllerLink.nodeNameOffset.offset;
			nodeName = lookUpPaletteString(nodeNameOffset, niToJ3dData, controllerLink.stringPalette);
		}

		J3dNiAVObject nodeTarget = allBonesInSkeleton.get(nodeName);

		if (nodeTarget == null)
		{
			// this is fine for havok or animation data
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
				j3dNiInterpolator = J3dNiTransformInterpolatorFactory.createTransformInterpolator(niInterpolator, niToJ3dData, nodeTarget,
						startTimeS, lengthS);
			}
			else
			{
				NiTimeController controller = (NiTimeController) niToJ3dData.get(controllerLink.controller);
				if (controller != null)
				{
					if (controller instanceof NiGeomMorpherController)
					{
						System.out.println("I just seen a NiGeomMorpherController under a J3dControllerLink!");
						//FIXME: this should use one proper see ObjectNET
						/*	String morphFrameName = "";
							if (niToJ3dData.nifVer.LOAD_VER >= NifVer.VER_20_1_0_3)
							{
								morphFrameName = controllerLink.variable2;
							}
							else if (niToJ3dData.nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
							{
								int offset = controllerLink.variable2Offset.offset;
								morphFrameName = NifToJ3d.lookUpPaletteString(offset, blocks, controllerLink.stringPalette);
							}

							j3dNiTimeController = new J3dNiGeomMorpherController((NiGeomMorpherController) controller, morphFrameName, nodeTarget, blocks);
						*/
					}
					else
					{
						J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, niToJ3dData,
								nodeTarget, null);

						if (j3dNiTimeController != null)
						{
							j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator,
									niToJ3dData, startTimeS, stopTimeS);
						}
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
