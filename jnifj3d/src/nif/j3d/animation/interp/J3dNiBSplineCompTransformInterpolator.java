package nif.j3d.animation.interp;

import java.util.ArrayList;
import java.util.WeakHashMap;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.j3d.interp.RotPosScaleTCBSplinePathInterpolator;
import nif.niobject.NiBSplineBasisData;
import nif.niobject.NiBSplineData;
import nif.niobject.interpolator.NiBSplineCompTransformInterpolator;
import utils.convert.ConvertFromNif;
import utils.convert.NifRotToJava3DRot;

import com.sun.j3d.utils.behaviors.interpolators.TCBKeyFrame;

public class J3dNiBSplineCompTransformInterpolator extends J3dNiInterpolator
{
	private NiBSplineCompTransformInterpolator nibs;

	private NiBSplineData niBSplineData;

	private NiBSplineBasisData niBSplineBasisData;

	private Point3f defaultTrans = new Point3f();

	private Quat4f defaultRot = new Quat4f(0f, 0f, 0f, 1f);

	private Point3f defaultScale = new Point3f(1, 1, 1);

	private static WeakHashMap<NiBSplineCompTransformInterpolator, TCBKeyFrame[]> keysMap = new WeakHashMap<NiBSplineCompTransformInterpolator, TCBKeyFrame[]>();

	public J3dNiBSplineCompTransformInterpolator(NiBSplineCompTransformInterpolator niBSplineCompTransformInterpolator,
			NiToJ3dData niToJ3dData, NifTransformGroup targetTransform)
	{
		this.nibs = niBSplineCompTransformInterpolator;

		if (niBSplineCompTransformInterpolator.splineData.ref != -1)
		{
			niBSplineData = (NiBSplineData) niToJ3dData.get(niBSplineCompTransformInterpolator.splineData);
			if (niBSplineCompTransformInterpolator.basisData.ref != -1)
			{
				niBSplineBasisData = (NiBSplineBasisData) niToJ3dData.get(niBSplineCompTransformInterpolator.basisData);

				TCBKeyFrame[] keys = keysMap.get(nibs);//note key!
				if (keys == null)
				{
					setDefaultRotPosScale();

					ArrayList<Quat4f> quats = getQuatRotateControlData();
					ArrayList<Point3f> points = getTranslateControlData();
					ArrayList<Point3f> scales = getScaleControlData();

					int numberOfControlPoints = niBSplineBasisData.numControlPoints;
					keys = new TCBKeyFrame[numberOfControlPoints];
					for (int i = 0; i < numberOfControlPoints; i++)
					{
						float knot = (float) i / (float) (numberOfControlPoints - 1);
						Point3f p = points == null ? defaultTrans : points.get(i);
						Quat4f q = quats == null ? defaultRot : quats.get(i);
						Point3f s = scales == null ? defaultScale : scales.get(i);

						//NOTE no TCB because this is not a TCB interpolator so linear=1 (faster?)
						TCBKeyFrame key = new TCBKeyFrame(knot, 1, p, q, s, 0.0f, 0.0f, 0.0f);
						keys[i] = key;
					}

					keysMap.put(nibs, keys);
				}

				RotPosScaleTCBSplinePathInterpolator tCBSplinePathInterpolator = new RotPosScaleTCBSplinePathInterpolator(
						J3dNiInterpolator.prepTransformGroup(targetTransform), keys);
				setInterpolator(tCBSplinePathInterpolator);
			}
		}
	}

	private void setDefaultRotPosScale()
	{
		if (nibs.translation.x != NIF_FLOAT_MIN)
		{
			defaultTrans = ConvertFromNif.toJ3dP3f(nibs.translation);
		}

		if (nibs.rotation.x != NIF_FLOAT_MIN)
		{
			defaultRot = ConvertFromNif.toJ3d(nibs.rotation);
		}

		if (nibs.scale != NIF_FLOAT_MIN)
		{
			//Note scale is a percentage change so no conversion from nif
			float s = nibs.scale;
			defaultScale = new Point3f(s, s, s);
		}

	}

	private short[] getShortControlPointRange(short[] allControlPoints, int offset, int numberOfControlPoints)
	{
		short[] ret = new short[numberOfControlPoints];
		System.arraycopy(allControlPoints, offset, ret, 0, numberOfControlPoints);
		return ret;
	}

	private ArrayList<Quat4f> getQuatRotateControlData()
	{

		if (nibs.rotationOffset != NIF_USHRT_MAX)
		{
			// has rotation data
			ArrayList<Quat4f> ret = new ArrayList<Quat4f>();
			int numberOfControlPoints = niBSplineBasisData.numControlPoints;

			short[] points = getShortControlPointRange(niBSplineData.shortControlPoints, nibs.rotationOffset, numberOfControlPoints * 4);

			for (int i = 0; i < numberOfControlPoints; i++)
			{
				float w = ((points[i * 4 + 0] / 32767f) * nibs.rotationMultiplier) + nibs.rotationBias;
				float x = ((points[i * 4 + 1] / 32767f) * nibs.rotationMultiplier) + nibs.rotationBias;
				float y = ((points[i * 4 + 2] / 32767f) * nibs.rotationMultiplier) + nibs.rotationBias;
				float z = ((points[i * 4 + 3] / 32767f) * nibs.rotationMultiplier) + nibs.rotationBias;
				Quat4f key = NifRotToJava3DRot.makeJ3dQ4f(x, y, z, w);

				ret.add(key);
			}
			return ret;
		}
		else
		{
			return null;
		}

	}

	private ArrayList<Point3f> getTranslateControlData()
	{

		if (nibs.translationOffset != NIF_USHRT_MAX)
		{
			// has translation data
			ArrayList<Point3f> ret = new ArrayList<Point3f>();
			int numberOfControlPoints = niBSplineBasisData.numControlPoints;

			short[] points = getShortControlPointRange(niBSplineData.shortControlPoints, nibs.translationOffset, numberOfControlPoints * 3);

			for (int i = 0; i < numberOfControlPoints; i++)
			{
				float x = ((points[i * 3 + 0] / 32767f) * nibs.translationMultiplier) + nibs.translationBias;
				float y = ((points[i * 3 + 1] / 32767f) * nibs.translationMultiplier) + nibs.translationBias;
				float z = ((points[i * 3 + 2] / 32767f) * nibs.translationMultiplier) + nibs.translationBias;

				Point3f key = ConvertFromNif.toJ3dP3f(x, y, z);
				ret.add(key);
			}
			return ret;
		}
		else
		{
			return null;
		}
	}

	private ArrayList<Point3f> getScaleControlData()
	{

		if (nibs.scaleOffset != NIF_USHRT_MAX)
		{
			// has scale data
			ArrayList<Point3f> ret = new ArrayList<Point3f>();
			int numberOfControlPoints = niBSplineBasisData.numControlPoints;

			short[] points = getShortControlPointRange(niBSplineData.shortControlPoints, nibs.scaleOffset, numberOfControlPoints * 1);
			for (int i = 0; i < numberOfControlPoints; i++)
			{
				float s = ((points[i + 0] / 32767f) * nibs.scaleMultiplier) + nibs.scaleBias;
				//Note scale is a percentage change so no conversion from Nif
				ret.add(new Point3f(s, s, s));
			}
			return ret;
		}
		else
		{
			return null;
		}
	}

}
