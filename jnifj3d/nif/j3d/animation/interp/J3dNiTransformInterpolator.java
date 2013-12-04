package nif.j3d.animation.interp;

import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.compound.NifQuatKey;
import nif.compound.NifVector3;
import nif.enums.KeyType;
import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.j3d.compound.KnotsQuats;
import nif.j3d.interp.PositionPathInterpolator;
import nif.j3d.interp.RotationPathInterpolator;
import nif.j3d.interp.ScalePathInterpolator;
import nif.j3d.interp.XYZRotPathInterpolator;
import nif.niobject.NiTransformData;
import nif.niobject.interpolator.NiTransformInterpolator;
import utils.convert.ConvertFromNif;

public class J3dNiTransformInterpolator extends J3dNiInterpolator
{
	private PositionPathInterpolator positionPathInterpolator;

	private ScalePathInterpolator scalePathInterpolator;

	private XYZRotPathInterpolator xYZRotPathInterpolator;

	private RotationPathInterpolator quatRotInterpolator;

	public J3dNiTransformInterpolator(NiTransformInterpolator niTransformInterp, NiToJ3dData niToJ3dData,
			NifTransformGroup targetTransform, float startTimeS, float lengthS)
	{
		if (niTransformInterp.data.ref != -1)
		{
			NiTransformData niTransformData = (NiTransformData) niToJ3dData.get(niTransformInterp.data);

			if (niTransformData.numRotationKeys > 0)
			{
				if (niTransformData.rotationType.type == KeyType.XYZ_ROTATION_KEY)
				{
					//	niTransformData.xYZRotations.length is 3 for x rot y rot z rot

					NifKeyGroup xRotation = niTransformData.xYZRotations[0];
					if (xRotation.keys != null && xRotation.keys.length > 0)
					{
						// all three will be not null and l > 0
						float[] xKnots = new float[xRotation.keys.length];
						float[] xRots = new float[xRotation.keys.length];
						for (int i = 0; i < xRotation.keys.length; i++)
						{
							NifKey key = xRotation.keys[i];
							xKnots[i] = (key.time - startTimeS) / lengthS;
							xRots[i] = ((Float) key.value).floatValue();
						}

						NifKeyGroup yRotation = niTransformData.xYZRotations[2];
						float[] yKnots = new float[yRotation.keys.length];
						float[] yRots = new float[yRotation.keys.length];
						for (int i = 0; i < yRotation.keys.length; i++)
						{
							NifKey key = yRotation.keys[i];
							yKnots[i] = (key.time - startTimeS) / lengthS;
							yRots[i] = ((Float) key.value).floatValue();
						}
						NifKeyGroup zRotation = niTransformData.xYZRotations[1];
						float[] zKnots = new float[zRotation.keys.length];
						float[] zRots = new float[zRotation.keys.length];
						for (int i = 0; i < zRotation.keys.length; i++)
						{
							NifKey key = zRotation.keys[i];
							zKnots[i] = (key.time - startTimeS) / lengthS;
							zRots[i] = -((Float) key.value).floatValue();
						}

						xYZRotPathInterpolator = new XYZRotPathInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform), xKnots,
								xRots, yKnots, yRots, zKnots, zRots);
						addInterpolator(xYZRotPathInterpolator);
					}
				}
				else if (niTransformData.rotationType.type == KeyType.QUADRATIC_KEY || niTransformData.rotationType.type == KeyType.TBC_KEY
						|| niTransformData.rotationType.type == KeyType.LINEAR_KEY)
				{
					//TODO: the TCBKeyFrame class and interpolator in the j3dcore jar should be used here!!
					NifQuatKey[] quats = niTransformData.quaternionKeys;
					if (quats != null && quats.length > 0)
					{
						KnotsQuats kq = makeKnotsQuats(quats, startTimeS, lengthS);
						quatRotInterpolator = new RotationPathInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform), kq.knots,
								kq.quats);
						addInterpolator(quatRotInterpolator);
					}
				}
				else
				{
					System.out.println("bad key detected " + niTransformData.rotationType.type);
				}
			}

			NifKeyGroup translations = niTransformData.translations;
			if (translations.keys != null && translations.keys.length > 0)
			{
				float[] knots = new float[translations.keys.length];
				Point3f[] positions = new Point3f[translations.keys.length];
				for (int i = 0; i < translations.keys.length; i++)
				{
					NifKey key = translations.keys[i];
					knots[i] = (key.time - startTimeS) / lengthS;
					positions[i] = ConvertFromNif.toJ3dP3f((NifVector3) key.value);
				}

				positionPathInterpolator = new PositionPathInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform), knots,
						positions);
				addInterpolator(positionPathInterpolator);
			}

			NifKeyGroup scales = niTransformData.scales;
			if (scales.keys != null && scales.keys.length > 0)
			{
				float[] knots = new float[scales.keys.length];
				float[] ss = new float[scales.keys.length];
				for (int i = 0; i < scales.keys.length; i++)
				{
					NifKey key = scales.keys[i];
					knots[i] = (key.time - startTimeS) / lengthS;
					ss[i] = ((Float) key.value).floatValue();
				}

				scalePathInterpolator = new ScalePathInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform), knots, ss);
				addInterpolator(scalePathInterpolator);
			}

		}
	}

	private KnotsQuats makeKnotsQuats(NifQuatKey[] keys, float startTimeS, float lengthS)
	{
		// all this mucking around is because the time value is sometimes repeated in the key set
		// the last time has been seen definately
		ArrayList<Float> knotsal = new ArrayList<Float>();
		ArrayList<Quat4f> quatsal = new ArrayList<Quat4f>();
		float prevKnot = -1;
		for (int i = 0; i < keys.length; i++)
		{
			NifQuatKey key = keys[i];
			float knot = (key.time - startTimeS) / lengthS;
			if (knot != prevKnot)
			{
				knotsal.add(new Float(knot));
				quatsal.add(ConvertFromNif.toJ3d(key.value));
			}
		}

		// now copy into arrays
		float[] knots = new float[knotsal.size()];
		for (int i = 0; i < knotsal.size(); i++)
		{
			knots[i] = knotsal.get(i).floatValue();
		}
		Quat4f[] quats = new Quat4f[quatsal.size()];
		quatsal.toArray(quats);
		KnotsQuats kq = new KnotsQuats();
		kq.knots = knots;
		kq.quats = quats;
		return kq;
	}
}
