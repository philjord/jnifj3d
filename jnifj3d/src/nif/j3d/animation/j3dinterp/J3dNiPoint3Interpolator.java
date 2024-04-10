package nif.j3d.animation.j3dinterp;

import org.jogamp.vecmath.Point3f;

import nif.compound.NifKeyGroup.NifKeyGroupNifVector3;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.Point3Interpolator;
import nif.j3d.animation.j3dinterp.interp.data.KnotsPoint3fs;
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
			setInterpolator(interpolator);
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
 		if (niPoint3Interp.data.ref != -1)
		{
			NifKeyGroupNifVector3 posData = ((NiPosData) niToJ3dData.get(niPoint3Interp.data)).data;

			// check for no data
			if (posData.time.length > 2 || (posData.time.length == 2 && (
					posData.value[0*3+0] != posData.value[1*3+0] && posData.value[0*3+1] != posData.value[1*3+1] && posData.value[0*3+2] != posData.value[1*3+2])))
			{
				if (posData.interpolation.type == 2)
				{
					float[] knots = new float[posData.time.length];
					Point3f[] values = new Point3f[posData.time.length];

					for (int i = 0; i < posData.time.length; i++)
					{
						// make into 0 to 1 form
						knots[i] = (posData.time[i] - startTimeS) / lengthS;
						//not Converted because it may be controlling color! see Meshes\Architecture\Megaton\MegatonGateHouse01.NIF 
						values[i] = new Point3f(posData.value[i*3+0], posData.value[i*3+1], posData.value[i*3+2]);

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
				constantPoint3f = new Point3f(posData.value[0*3+0], posData.value[0*3+1], posData.value[0*3+2]);
			}
		}
		else
		{
			constantPoint3f = new Point3f(niPoint3Interp.point3Value.x, niPoint3Interp.point3Value.y, niPoint3Interp.point3Value.z);
		}

	}
}
