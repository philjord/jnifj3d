package nif.j3d.animation.interp;

import javax.vecmath.Point3f;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.compound.NifVector3;
import nif.j3d.NiToJ3dData;
import nif.j3d.compound.KnotsPoint3fs;
import nif.j3d.interp.Point3Interpolator;
import nif.niobject.NiPosData;
import nif.niobject.interpolator.NiPoint3Interpolator;

public class J3dNiPoint3Interpolator extends J3dNiInterpolator
{

	/**	  
	 * @param niPoint3Interp
	 * @param blocks
	 * @param nodeTarget
	 * @param startTimeS
	 * @param lengthS
	 */

	protected KnotsPoint3fs knotsPoints;

	private Point3f constantPoint3f = null;

	public J3dNiPoint3Interpolator(NiPoint3Interpolator niPoint3Interp, NiToJ3dData niToJ3dData, float startTimeS, float lengthS,
			Point3Interpolator.Listener callBack)
	{
		makeKnotsPoints(niPoint3Interp, niToJ3dData, startTimeS, lengthS);
		createInterpolator(callBack);
	}

	//TODO: make this add interp so many can be called back from one
	private void createInterpolator(Point3Interpolator.Listener callBack)
	{
		if (knotsPoints != null)
		{
			Point3Interpolator interpolator = new Point3Interpolator(callBack, knotsPoints.knots, knotsPoints.points);
			addInterpolator(interpolator);
		}
		else if (constantPoint3f != null)
		{
			// otherwise it just a  constant value set once now
			callBack.update(constantPoint3f);
		}
	}

	/**
	 * EITHER create the knots point or create a constant value
	 * @param niPoint3Interp
	 * @param blocks
	 * @param startTimeS
	 * @param lengthS
	 */
	private void makeKnotsPoints(NiPoint3Interpolator niPoint3Interp, NiToJ3dData niToJ3dData, float startTimeS, float lengthS)
	{
System.out.println("J3dNiPoint3Interpolator created note no xyz->x-zy yet!");
		if (niPoint3Interp.data.ref != -1)
		{
			NifKeyGroup posData = ((NiPosData) niToJ3dData.get(niPoint3Interp.data)).data;

			// check for no data
			if (posData.keys.length > 2 || (posData.keys.length == 2 && !posData.keys[0].value.equals(posData.keys[1].value)))
			{
				if (posData.interpolation.type == 2)
				{
					float[] knots = new float[posData.keys.length];
					Point3f[] values = new Point3f[posData.keys.length];

					for (int i = 0; i < posData.keys.length; i++)
					{
						NifKey key = posData.keys[i];
						// make into 0 to 1 form
						knots[i] = (key.time - startTimeS) / lengthS;
						NifVector3 nv3 = (NifVector3) key.value;

						values[i] = new Point3f(nv3.x, nv3.y, nv3.z);

					}
					knotsPoints = new KnotsPoint3fs();
					knotsPoints.knots = knots;
					knotsPoints.points = values;

				}
				else
				{
					System.out.println("J3dNiPoint3Interpolator has none type 2 it has " + posData.interpolation.type);
				}
			}
			else
			{
				// if it's a single value (or 2 the same) then make a constant out of it
				NifKey key = posData.keys[0];
				NifVector3 nv3 = (NifVector3) key.value;
				constantPoint3f = new Point3f(nv3.x, nv3.y, nv3.z);
			}
		}
		else
		{
			constantPoint3f = new Point3f(niPoint3Interp.point3Value.x, niPoint3Interp.point3Value.y, niPoint3Interp.point3Value.z);
		}

	}
}
