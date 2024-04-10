package nif.j3d.animation.j3dinterp;

import nif.compound.NifKeyGroup.NifKeyGroupByte;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.BoolInterpolator;
import nif.j3d.animation.j3dinterp.interp.data.KnotsBools;
import nif.niobject.NiBoolData;
import nif.niobject.interpolator.NiBoolInterpolator;

public class J3dNiBoolInterpolator extends J3dNiInterpolator
{
	private KnotsBools knotsBools;

	private boolean constantBool = false;

	public J3dNiBoolInterpolator(NiBoolInterpolator niBoolInterpolator, NiToJ3dData niToJ3dData, float startTimeS, float lengthS,
			BoolInterpolator.Listener callBack)
	{
		if (niBoolInterpolator.data.ref != -1)
		{
			NifKeyGroupByte boolData = ((NiBoolData) niToJ3dData.get(niBoolInterpolator.data)).data;
			knotsBools = makeKnotsBools(boolData, startTimeS, lengthS);
		}
		else
		{
			constantBool = niBoolInterpolator.boolValue;
		}
		createInterpolator(callBack);
	}

	private void createInterpolator(BoolInterpolator.Listener callBack)
	{
		if (knotsBools != null)
		{
			BoolInterpolator interpolator = new BoolInterpolator(callBack, knotsBools.knots, knotsBools.bools);
			setInterpolator(interpolator);

			// also set the first value now 
			callBack.update(knotsBools.bools[0]);
		}
		else
		{
			// otherwise it just a  constant value set once now
			callBack.update(constantBool);
		}
	}

	public static KnotsBools makeKnotsBools(NifKeyGroupByte boolData, float startTimeS, float lengthS)
	{
		if (boolData.value.length > 2 || (boolData.value.length == 2 && boolData.value[0] != boolData.value[1]))
		{
			//floatData.interpolation.type tends to be 1 or 2

			float[] knots = new float[boolData.value.length];
			boolean[] values = new boolean[boolData.value.length];

			for (int i = 0; i < boolData.time.length; i++)
			{
				// make into 0 to 1 form
				knots[i] = (boolData.time[i] - startTimeS) / lengthS;
				values[i] = (boolData.value[i]) != 0;
			}
			KnotsBools kb = new KnotsBools();
			kb.knots = knots;
			kb.bools = values;
			return kb;
		}
		return null;
	}

	public KnotsBools getKnotsBools()
	{
		return knotsBools;
	}
}
