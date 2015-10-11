package nif.j3d.animation.j3dinterp;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
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
			NifKeyGroup boolData = ((NiBoolData) niToJ3dData.get(niBoolInterpolator.data)).data;
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

	public static KnotsBools makeKnotsBools(NifKeyGroup boolData, float startTimeS, float lengthS)
	{
		if (boolData.keys.length > 2 || (boolData.keys.length == 2 && !boolData.keys[0].value.equals(boolData.keys[1].value)))
		{
			//floatData.interpolation.type tends to be 1 or 2

			float[] knots = new float[boolData.keys.length];
			boolean[] values = new boolean[boolData.keys.length];

			for (int i = 0; i < boolData.keys.length; i++)
			{
				NifKey key = boolData.keys[i];
				// make into 0 to 1 form
				knots[i] = (key.time - startTimeS) / lengthS;
				values[i] = ((Byte) key.value) != 0;
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
