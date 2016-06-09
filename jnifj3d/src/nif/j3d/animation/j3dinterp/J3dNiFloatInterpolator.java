package nif.j3d.animation.j3dinterp;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.compound.NifMorph;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.FloatInterpolator;
import nif.j3d.animation.j3dinterp.interp.data.KnotsFloats;
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
		if (niFloatInterpolator.data.ref != -1)
		{
			NifKeyGroup floatData = ((NiFloatData) niToJ3dData.get(niFloatInterpolator.data)).data;

			makeKnotsFloats(floatData.keys, startTimeS, lengthS);
		}
		else
		{
			constantFloat = niFloatInterpolator.floatValue;
		}

		if (callBack != null)
			createInterpolator(callBack);
		else
			new Throwable("null callback in " + niFloatInterpolator.nVer.fileName).printStackTrace();

	}

	//FOR TES3
	public J3dNiFloatInterpolator(NifMorph nifMorph, float startTimeS, float lengthS, FloatInterpolator.Listener callBack)
	{
		makeKnotsFloats(nifMorph.Keys, startTimeS, lengthS);

		if (callBack != null)
			createInterpolator(callBack);
		else
			new Throwable("null callback in nifMorph " + nifMorph.nVer.fileName).printStackTrace();

	}

	//FOR TES3 (true??)
	public J3dNiFloatInterpolator(NifKeyGroup keyGroup, float startTimeS, float lengthS, FloatInterpolator.Listener callBack)
	{
		makeKnotsFloats(keyGroup.keys, startTimeS, lengthS);

		if (callBack != null)
			createInterpolator(callBack);
		else
			new Throwable("null callback in NifKeyGroup ").printStackTrace();

	}

	//FOR TES3  
	public J3dNiFloatInterpolator(float startTimeS, float stopTimeS, FloatInterpolator.Listener callBack)
	{		
		float[] knots = new float[]{0,1};
		float[] values = new float[]{startTimeS, stopTimeS};
		
		knotsFloats = new KnotsFloats();
		knotsFloats.knots = knots;
		knotsFloats.floats = values;

		if (callBack != null)
			createInterpolator(callBack);
		else
			new Throwable("null callback in NifKeyGroup ").printStackTrace();

	}

	//TODO: make this add interp so many can be called back from one
	private void createInterpolator(FloatInterpolator.Listener callBack)
	{
		if (knotsFloats != null)
		{
			FloatInterpolator interpolator = new FloatInterpolator(callBack, knotsFloats.knots, knotsFloats.floats);
			setInterpolator(interpolator);
		}
		else if (constantFloat != Float.NEGATIVE_INFINITY)
		{
			// otherwise it just a constant value set once now
			callBack.update(constantFloat);
		}
	}

	private void makeKnotsFloats(NifKey[] keys, float startTimeS, float lengthS)
	{
		if (keys != null && keys.length > 0)
		{
			if (keys.length > 2 || (keys.length == 2 && !keys[0].value.equals(keys[1].value)))
			{
				// floatData.interpolation.type tends to be 1 LINEAR_KEY or 2 QUADRATIC_KEY
				// below is ONLY LINEAR_KEY

				float[] knots = new float[keys.length];
				float[] values = new float[keys.length];

				for (int i = 0; i < keys.length; i++)
				{
					NifKey key = keys[i];
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
				NifKey key = keys[0];
				constantFloat = (Float) key.value;
			}
		}
		else
		{
			// it's buggered
			constantFloat = 0;
		}

	}

	//The interp type should be used by users of this float interp to interp the value as below

	/** Quaternion linear interpolation /
	Quaternion slerp(float t, Quaternion p, Quaternion q) {
	  float cosTheta = p.data.Dot(q.data);
	  float theta    = acosf(cosTheta);
	  float sinTheta = sinf(theta);
	  float wp, wq;
	
	  if (sinTheta > 0.001f) {
	    wq = sinf(        t  * theta) / sinTheta;
	    wp = sinf((1.0f - t) * theta) / sinTheta;
	  }
	  else {
	    wq =         t ;
	    wp = (1.0f - t);
	  }
	
	  return (p.data * wp) + (q.data * wq);
	}
	
	
	   Quaternion quadratic interpolation 
	Quaternion squad(float t, Quaternion p, Quaternion a, Quaternion b, Quaternion q) {
	  Quaternion sq1 = slerp(t, p, q);
	  Quaternion sq2 = slerp(t, a, b);
	
	  return slerp(2.0f * t * (1.0f - t), sq1, sq2);
	}
	
	/ Scalar/Vector linear interpolation /
	K lerp(float t, Key<K> p, Key<K> q) {
	  float wp, wq;
	
	  wq =         t ;
	  wp = (1.0f - t);
	
	  return (p->data * wp) + (q->data * wq);
	}
	
	/ Scalar/Vector quadratic interpolation /
	K herp(float t, Key<K> p, K a, K b, Key<K> q) {
	  K po1 = (q->value - p->value) * 3.0f - (a * 2.0f + b);
	  K po2 = (p->value - q->value) * 2.0f + (a        + b);
	
	  K po3 = (po2 * t) + po1;
	  K po4 = (po3 * t) + a;
	  K po5 = (po4 * t) + p;
	
	  return po5;
	}*/
}
