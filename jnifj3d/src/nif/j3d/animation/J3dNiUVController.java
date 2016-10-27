package nif.j3d.animation;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.Node;

import nif.compound.NifKeyGroup;
import nif.enums.TexTransform;
import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiUVData;
import nif.niobject.controller.NiUVController;

public class J3dNiUVController extends J3dNiTimeController
{
	public J3dNiUVController(NiUVController controller, NiToJ3dData niToJ3dData)
	{
		super(controller, null);
		NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
		nodeTarget = niToJ3dData.get(target);
		if (nodeTarget != null)
		{
			nodeTarget.setCapability(Node.ALLOW_BOUNDS_READ);
			
			float startTimeS = controller.startTime;
			float stopTimeS = controller.stopTime;

			float totalLengthS = stopTimeS - startTimeS;

			NiUVData niUVData = (NiUVData) niToJ3dData.get(controller.data);

			// 4 ops in the group
			int[] ops = new int[]
			{ TexTransform.TT_TRANSLATE_U, TexTransform.TT_TRANSLATE_V, TexTransform.TT_SCALE_U, TexTransform.TT_SCALE_V };

			for (int i = 0; i < 4; i++)
			{
				NifKeyGroup keyGroup = niUVData.uVGroups[i];
				if (keyGroup.keys != null)
				{
					J3dNiTimeController j3dNiTimeController2 = new J3dNiTextureTransformController(controller, (J3dNiAVObject) nodeTarget,
							ops[i]);
					J3dNiInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(keyGroup, startTimeS, totalLengthS,
							j3dNiTimeController2);
					addChild(j3dNiInterpolator);
					Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
					j3dNiInterpolator.fire(baseAlpha);
				}
			}
		}
	}
}
