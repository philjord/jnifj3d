package nif.j3d.animation.interp;

import java.util.WeakHashMap;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.compound.NifQuatKey;
import nif.compound.NifVector3;
import nif.enums.KeyType;
import nif.j3d.NiToJ3dData;
import nif.j3d.NifTransformGroup;
import nif.j3d.interp.PositionPathInterpolator;
import nif.j3d.interp.RotPosScaleInterpolator;
import nif.j3d.interp.RotationPathInterpolator;
import nif.j3d.interp.ScalePathInterpolator;
import nif.j3d.interp.XYZRotPathInterpolator;
import nif.niobject.NiTransformData;
import nif.niobject.interpolator.NiTransformInterpolator;
import utils.convert.ConvertFromNif;

/**
 * A small aside regarding nif x,y,z to java3d x,y,z
 * Translations are simply 
 * Point3f(x * ESConfig.ES_TO_METERS_SCALE, //
 *				z * ESConfig.ES_TO_METERS_SCALE, //
 *				-y * ESConfig.ES_TO_METERS_SCALE);
 *j3dx=nifx, j3dy=nifz, j3dz=nif-y
 * Note scaling too.
 * 
 * For XYZ rotations, if we assume right handedness (that is thumb in positive axis dir, 
 * then finger wrap is rotation dir) (if we don't just negate all three)
 * 
 * X rotation remains unchanged
 * 
 * j3dY is taken from nifZ 
 * j3dZ is taken from nifY but -ve  
 * you do the maths
 *
 * @author philip
 *
 */
public class J3dNiTransformInterpolator extends J3dNiInterpolator
{
	private XYZRotPathInterpolator xYZRotPathInterpolator;

	private RotationPathInterpolator quatRotInterpolator;

	private Quat4f defaultRot = null;

	private PositionPathInterpolator positionPathInterpolator;

	private Vector3f defaultTrans = null;

	private ScalePathInterpolator scalePathInterpolator;

	private float defaultScale = Float.MIN_VALUE;

	public J3dNiTransformInterpolator(NiTransformInterpolator niTransformInterp, NiToJ3dData niToJ3dData,
			NifTransformGroup targetTransform, float startTimeS, float lengthS)
	{
		if (niTransformInterp.rotation.x != NIF_FLOAT_MIN)
		{
			defaultRot = ConvertFromNif.toJ3d(niTransformInterp.rotation);
		}

		if (niTransformInterp.translation.x != NIF_FLOAT_MIN)
		{
			defaultTrans = ConvertFromNif.toJ3d(niTransformInterp.translation);
		}

		if (niTransformInterp.scale != NIF_FLOAT_MIN)
		{
			//Notice scale is a percentage change so no Nif conversion done!
			defaultScale = niTransformInterp.scale;
		}

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
						XyzRotationData data = xyzRotationDataMap.get(niTransformData);
						if (data == null)
						{
							// all three will be not null and l >= 0
							float[] xKnots = new float[xRotation.keys.length];
							float[] xRots = new float[xRotation.keys.length];
							for (int i = 0; i < xRotation.keys.length; i++)
							{
								NifKey key = xRotation.keys[i];
								xKnots[i] = key.time;
								xRots[i] = ((Float) key.value).floatValue();

								//System.out.println("xRot " +xRots[i] + " ref "+ niTransformInterp.data.ref);
							}

							NifKeyGroup yRotation = niTransformData.xYZRotations[2];// note nif Z
							float[] yKnots = new float[yRotation.keys.length];
							float[] yRots = new float[yRotation.keys.length];
							for (int i = 0; i < yRotation.keys.length; i++)
							{
								NifKey key = yRotation.keys[i];
								yKnots[i] = key.time;
								yRots[i] = ((Float) key.value).floatValue();

								//System.out.println("yRot " +yRots[i] + " ref "+ niTransformInterp.data.ref);

								//TODO: spider daedra castself wants this reversed, but brahmin turnleft doesn't
								// In fact this is wrong: I see the siper daedra problem is that these little -1.4 (half Pis)
								// need to be +1.4 but how the fuck does that make a jaw flap? it MUST be a non static frame, surely?
								// which is a mighty tricky mapping to discover
								// though given a full x rotate before it perhaps static frame wrong?
								// these rots are what make the jaw talk up and down (oddly enough)
								// there is no z fix up because of static frame ( this rot doesn't mod z in any way

								// I see that breast that are also fixed by this -ve have a +Pi and a bit in this slot

								//possibly static frame wrong see XYZRotPathInterpolator for apply

								//if (niTransformInterp.nVer.LOAD_VER < NifVer.VER_20_2_0_7)
								//	yRots[i] = -yRots[i];

							}

							NifKeyGroup zRotation = niTransformData.xYZRotations[1]; // note nif Y
							float[] zKnots = new float[zRotation.keys.length];
							float[] zRots = new float[zRotation.keys.length];
							for (int i = 0; i < zRotation.keys.length; i++)
							{
								NifKey key = zRotation.keys[i];
								zKnots[i] = key.time;
								zRots[i] = -((Float) key.value).floatValue();
								//System.out.println("zRot " +zRots[i] + " ref "+ niTransformInterp.data.ref);

							}
							data = new XyzRotationData(xKnots, xRots, yKnots, yRots, zKnots, zRots);
							xyzRotationDataMap.put(niTransformData, data);

						}

						xYZRotPathInterpolator = new XYZRotPathInterpolator(data.xKnots, data.xRots, data.yKnots, data.yRots, data.zKnots,
								data.zRots);
					}
				}
				else if (niTransformData.rotationType.type == KeyType.QUADRATIC_KEY || niTransformData.rotationType.type == KeyType.TBC_KEY
						|| niTransformData.rotationType.type == KeyType.LINEAR_KEY)
				{
					NifQuatKey[] quatKeys = niTransformData.quaternionKeys;
					if (quatKeys != null && quatKeys.length > 0)
					{
						QuatRotationData data = quatRotationDataMap.get(niTransformData);
						if (data == null)
						{
							float[] knots = new float[quatKeys.length];
							Quat4f[] quats = new Quat4f[quatKeys.length];
							for (int i = 0; i < quatKeys.length; i++)
							{
								NifQuatKey key = quatKeys[i];
								knots[i] = key.time;
								quats[i] = ConvertFromNif.toJ3d(key.value);

							}
							data = new QuatRotationData(knots, quats);
							quatRotationDataMap.put(niTransformData, data);
						}

						quatRotInterpolator = new RotationPathInterpolator(data.knots, data.quats);

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
				TranslationData data = translationDataMap.get(niTransformData);
				if (data == null)
				{
					float[] knots = new float[translations.keys.length];
					Point3f[] positions = new Point3f[translations.keys.length];
					for (int i = 0; i < translations.keys.length; i++)
					{
						NifKey key = translations.keys[i];
						knots[i] = key.time;
						positions[i] = ConvertFromNif.toJ3dP3f((NifVector3) key.value);
					}
					data = new TranslationData(knots, positions);
					translationDataMap.put(niTransformData, data);
				}
				positionPathInterpolator = new PositionPathInterpolator(data.knots, data.positions);

			}

			NifKeyGroup scaleKeys = niTransformData.scales;
			if (scaleKeys.keys != null && scaleKeys.keys.length > 0)
			{
				ScaleData data = scaleDataMap.get(niTransformData);
				if (data == null)
				{
					float[] knots = new float[scaleKeys.keys.length];
					float[] scales = new float[scaleKeys.keys.length];
					for (int i = 0; i < scaleKeys.keys.length; i++)
					{
						NifKey key = scaleKeys.keys[i];
						knots[i] = key.time;
						scales[i] = ((Float) key.value).floatValue();
					}
					data = new ScaleData(knots, scales);
					scaleDataMap.put(niTransformData, data);
				}
				scalePathInterpolator = new ScalePathInterpolator(data.knots, data.scales);

			}

		}

		RotPosScaleInterpolator rotPosScaleInterpolator = new RotPosScaleInterpolator(
				J3dNiInterpolator.prepTransformGroup(targetTransform), startTimeS, lengthS, positionPathInterpolator,
				scalePathInterpolator, xYZRotPathInterpolator, quatRotInterpolator, defaultTrans, defaultRot, defaultScale);
		setInterpolator(rotPosScaleInterpolator);
		rotPosScaleInterpolator.setOwner(this);
		this.setOwner(niTransformInterp);

	}

	private static WeakHashMap<NiTransformData, XyzRotationData> xyzRotationDataMap = new WeakHashMap<NiTransformData, XyzRotationData>();

	private static WeakHashMap<NiTransformData, QuatRotationData> quatRotationDataMap = new WeakHashMap<NiTransformData, QuatRotationData>();

	private static WeakHashMap<NiTransformData, TranslationData> translationDataMap = new WeakHashMap<NiTransformData, TranslationData>();

	private static WeakHashMap<NiTransformData, ScaleData> scaleDataMap = new WeakHashMap<NiTransformData, ScaleData>();

	public static class XyzRotationData
	{
		public float[] xKnots;

		public float[] xRots;

		public float[] yKnots;

		public float[] yRots;

		public float[] zKnots;

		public float[] zRots;

		public XyzRotationData(float[] xKnots, float[] xRots, float[] yKnots, float[] yRots, float[] zKnots, float[] zRots)
		{
			this.xKnots = xKnots;
			this.xRots = xRots;
			this.yKnots = yKnots;
			this.yRots = yRots;
			this.zKnots = zKnots;
			this.zRots = zRots;
		}
	}

	public class QuatRotationData
	{
		public float[] knots;

		public Quat4f[] quats;

		public QuatRotationData(float[] knots, Quat4f[] quats)
		{
			this.knots = knots;
			this.quats = quats;
		}
	}

	public class TranslationData
	{
		public float[] knots;

		public Point3f[] positions;

		public TranslationData(float[] knots, Point3f[] positions)
		{
			this.knots = knots;
			this.positions = positions;
		}
	}

	public class ScaleData
	{
		public float[] knots;

		public float[] scales;

		public ScaleData(float[] knots, float[] scales)
		{
			this.knots = knots;
			this.scales = scales;
		}
	}

}
