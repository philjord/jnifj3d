package nif.j3d.animation.interp;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.niobject.interpolator.NiBSplineCompTransformInterpolator;
import nif.niobject.interpolator.NiInterpolator;
import nif.niobject.interpolator.NiLookAtInterpolator;
import nif.niobject.interpolator.NiPathInterpolator;
import nif.niobject.interpolator.NiTransformInterpolator;

public class J3dNiTransformInterpolatorFactory
{

	public static J3dNiInterpolator createTransformInterpolator(NiInterpolator niInterpolator, NiToJ3dData niToJ3dData,
			J3dNiAVObject nodeTarget, float startTimeS, float stopTimeS)
	{
		float lengthS = stopTimeS - startTimeS;

		NifTransformGroup targetTransform = nodeTarget.getTransformGroup();
		if (niInterpolator instanceof NiPathInterpolator)
		{
			return new J3dNiPathInterpolator((NiPathInterpolator) niInterpolator, niToJ3dData, targetTransform);
		}
		else if (niInterpolator instanceof NiTransformInterpolator)
		{
			return new J3dNiTransformInterpolator((NiTransformInterpolator) niInterpolator, niToJ3dData, targetTransform, startTimeS,
					lengthS);
		}
		else if (niInterpolator instanceof NiBSplineCompTransformInterpolator)
		{
			return new J3dNiBSplineCompTransformInterpolator((NiBSplineCompTransformInterpolator) niInterpolator, niToJ3dData,
					targetTransform);
		}
		else if (niInterpolator instanceof NiLookAtInterpolator)
		{
			//TODO:
		}
		else
		{
			System.out.println("J3dNiTransformInterpolatorFactory - unhandled NiInterpolator " + niInterpolator);
		}
		return null;
	}
}
