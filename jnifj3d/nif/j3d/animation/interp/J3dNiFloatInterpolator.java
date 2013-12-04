package nif.j3d.animation.interp;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.j3d.NiToJ3dData;
import nif.j3d.compound.KnotsFloats;
import nif.j3d.interp.FloatInterpolator;
import nif.niobject.NiFloatData;
import nif.niobject.interpolator.NiFloatInterpolator;

public class J3dNiFloatInterpolator extends J3dNiInterpolator
{
	/**
	 * Some Float interpolators are for handling any game mechanic type thing that might need to be attached to an animation
	 * for example the amount of health healed by a medic animation at a given point in the anmation. Generally not visual or renderish 
	 * reads these values, but any one else who knows what the animation is about can do so if they wish.
	 * However they are also for interpolating for particle system controllers etc
	 */

	protected KnotsFloats knotsFloats;

	private float constantFloat = Float.NEGATIVE_INFINITY;

	public J3dNiFloatInterpolator(NiFloatInterpolator niFloatInterpolator, NiToJ3dData niToJ3dData, float startTimeS, float lengthS,
			FloatInterpolator.Listener callBack)
	{
		makeKnotsFloats(niFloatInterpolator, niToJ3dData, startTimeS, lengthS);
		createInterpolator(callBack);
	}

	//TODO: make this add interp so many can be called back from one
	private void createInterpolator(FloatInterpolator.Listener callBack)
	{
		if (knotsFloats != null)
		{
			FloatInterpolator interpolator = new FloatInterpolator(callBack, knotsFloats.knots, knotsFloats.floats);
			addInterpolator(interpolator);
		}
		else if (constantFloat != Float.NEGATIVE_INFINITY)
		{
			// otherwise it just a  constant value set once now
			callBack.update(constantFloat);
		}
	}

	private void makeKnotsFloats(NiFloatInterpolator niFloatInterpolator, NiToJ3dData niToJ3dData, float startTimeS, float lengthS)
	{
		if (niFloatInterpolator.data.ref != -1)
		{
			NifKeyGroup floatData = ((NiFloatData) niToJ3dData.get(niFloatInterpolator.data)).data;

			if (floatData.keys.length > 2 || (floatData.keys.length == 2 && !floatData.keys[0].value.equals(floatData.keys[1].value)))
			{
				//floatData.interpolation.type tends to be 1 or 2

				float[] knots = new float[floatData.keys.length];
				float[] values = new float[floatData.keys.length];

				for (int i = 0; i < floatData.keys.length; i++)
				{
					NifKey key = floatData.keys[i];
					// make into 0 to 1 form
					knots[i] = (key.time - startTimeS) / lengthS;
					values[i] = (Float) key.value;
				}
				knotsFloats = new KnotsFloats();
				knotsFloats.knots = knots;
				knotsFloats.floats = values;

			}
			else
			{
				// if it's a single value (or 2 the same) then make a constant out of it
				NifKey key = floatData.keys[0];
				constantFloat = (Float) key.value;

			}

		}
		else
		{
			constantFloat = niFloatInterpolator.floatValue;

		}

	}
}
