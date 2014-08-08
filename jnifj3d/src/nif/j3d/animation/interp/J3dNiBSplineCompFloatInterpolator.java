package nif.j3d.animation.interp;

import nif.j3d.NiToJ3dData;
import nif.j3d.interp.FloatInterpolator;
import nif.niobject.NiBSplineBasisData;
import nif.niobject.NiBSplineData;
import nif.niobject.interpolator.NiBSplineCompFloatInterpolator;

public class J3dNiBSplineCompFloatInterpolator extends J3dNiInterpolator
{
	private static float SHORT_RANGE = (-Short.MIN_VALUE + Short.MAX_VALUE); // range of which we want 0-1 across

	private static int UPLIFT = -Short.MIN_VALUE;// move shorts up to 0 to range

	private NiBSplineCompFloatInterpolator nibs;

	private NiBSplineData niBSplineData;

	private NiBSplineBasisData niBSplineBasisData;

	public J3dNiBSplineCompFloatInterpolator(NiBSplineCompFloatInterpolator niBSplineCompFloatInterpolator, NiToJ3dData niToJ3dData,
			FloatInterpolator.Listener callBack)
	{
		this.nibs = niBSplineCompFloatInterpolator;

		if (niBSplineCompFloatInterpolator.splineData.ref != -1)
		{
			niBSplineData = (NiBSplineData) niToJ3dData.get(niBSplineCompFloatInterpolator.splineData);
			if (niBSplineCompFloatInterpolator.basisData.ref != -1)
			{
				niBSplineBasisData = (NiBSplineBasisData) niToJ3dData.get(niBSplineCompFloatInterpolator.basisData);

				float defaultF = 0f;
				if (nibs.base != NIF_FLOAT_MIN)
				{
					defaultF = nibs.base;
				}

				float[] fs = getFloatControlData();

				int numberOfControlPoints = niBSplineBasisData.numControlPoints;
				float[] knots = new float[numberOfControlPoints];
				float[] values = new float[numberOfControlPoints];
				for (int i = 0; i < numberOfControlPoints; i++)
				{
					knots[i] = (float) i / (float) (numberOfControlPoints - 1);
					values[i] = fs == null ? defaultF : fs[i];
				}

				
				FloatInterpolator interpolator = new FloatInterpolator(callBack, knots, values);
				setInterpolator(interpolator);

			}
		}
	}

	private short[] getShortControlPointRange(short[] allControlPoints, int offset, int numberOfControlPoints)
	{
		short[] ret = new short[numberOfControlPoints];
		System.arraycopy(allControlPoints, offset, ret, 0, numberOfControlPoints);
		return ret;
	}

	private float[] getFloatControlData()
	{
		if (nibs.offset != NIF_USHRT_MAX)
		{
			int numberOfControlPoints = niBSplineBasisData.numControlPoints;
			float[] ret = new float[numberOfControlPoints];
			short[] points = getShortControlPointRange(niBSplineData.shortControlPoints, nibs.offset, numberOfControlPoints);

			for (int i = 0; i < numberOfControlPoints; i++)
			{
				ret[i] = (((points[i] + UPLIFT) / SHORT_RANGE) * nibs.multiplier) + nibs.bias;
			}
			return ret;
		}
		else
		{
			return null;
		}
	}
}
