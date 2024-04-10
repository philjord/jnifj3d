package nif.j3d.animation.j3dinterp;

import java.util.Collections;
import java.util.Map;

import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import nif.compound.NifKeyGroup.NifKeyGroupFloat;
import nif.compound.NifKeyGroup.NifKeyGroupNifVector3;
import nif.compound.NifQuatKey;
import nif.enums.KeyType;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.PositionPathInterpolator;
import nif.j3d.animation.j3dinterp.interp.RotPosScaleInterpolator;
import nif.j3d.animation.j3dinterp.interp.RotationPathInterpolator;
import nif.j3d.animation.j3dinterp.interp.ScalePathInterpolator;
import nif.j3d.animation.j3dinterp.interp.XYZRotPathInterpolator;
import nif.niobject.NiKeyframeData;
import nif.niobject.NiTransformData;
import nif.niobject.interpolator.NiTransformInterpolator;
import tools.WeakValueHashMap;
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
	
	
	//OMG, if the class type of the data is not a static class then the instance object needs to be retained!!
	//fudge! double fracking!! so into WEkHashMaps must only go static or top level class types!!!
	//cristo I need to double check every damn inner class to make sure they are AALLLL! static all of them!

	
	//FIXME: with the memory leak fixed this guy in now a slow mcslow face
	public static boolean CACHE_WEAK = true;
	private static Map<NiKeyframeData, XyzRotationData> xyzRotationDataMap = Collections
			.synchronizedMap(new WeakValueHashMap<NiKeyframeData, XyzRotationData>());

	private static Map<NiKeyframeData, QuatRotationData> quatRotationDataMap = Collections
			.synchronizedMap(new WeakValueHashMap<NiKeyframeData, QuatRotationData>());

	private static Map<NiKeyframeData, TranslationData> translationDataMap = Collections
			.synchronizedMap(new WeakValueHashMap<NiKeyframeData, TranslationData>());

	private static Map<NiKeyframeData, ScaleData> scaleDataMap = Collections.synchronizedMap(new WeakValueHashMap<NiKeyframeData, ScaleData>());

	public J3dNiTransformInterpolator(NiTransformInterpolator niTransformInterp, NiToJ3dData niToJ3dData, TransformGroup targetTransform,
			float startTimeS, float lengthS)
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
			processNiKeyframeData(niTransformData);
		}

		RotPosScaleInterpolator rotPosScaleInterpolator = new RotPosScaleInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform),
				startTimeS, lengthS, positionPathInterpolator, scalePathInterpolator, xYZRotPathInterpolator, quatRotInterpolator,
				defaultTrans, defaultRot, defaultScale);
		setInterpolator(rotPosScaleInterpolator);
	}

	public J3dNiTransformInterpolator(NiKeyframeData niTransformData, TransformGroup targetTransform, float startTimeS, float lengthS)
	{
		processNiKeyframeData(niTransformData);

		RotPosScaleInterpolator rotPosScaleInterpolator = new RotPosScaleInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform),
				startTimeS, lengthS, positionPathInterpolator, scalePathInterpolator, xYZRotPathInterpolator, quatRotInterpolator,
				defaultTrans, defaultRot, defaultScale);
		setInterpolator(rotPosScaleInterpolator);
	}

	protected void processNiKeyframeData(NiKeyframeData niTransformData)
	{
		if (niTransformData.numRotationKeys > 0)
		{
			if (niTransformData.rotationType.type == KeyType.XYZ_ROTATION_KEY)
			{
				//XYZ rotate axis swap xyz=xz-y occurs in XYZRotPathInterpolator
				//It's MADNESS !!! do nothing ere but give it across
				// so we do NO modification here, the interpolator will do the change over work

				//	niTransformData.xYZRotations.length is 3 for x rot y rot z rot
				NifKeyGroupFloat xRotation = niTransformData.xYZRotations[0];
				if (xRotation.value != null && xRotation.value.length > 0)
				{
					// don't let 2 threads load up the data into weak map
					XyzRotationData data = null;
					synchronized (niTransformData)
					{
						data = xyzRotationDataMap.get(niTransformData);
						if (data == null)
						{
							// all three will be not null and l >= 0
							float[] xKnots = xRotation.time;
							float[] xRots = xRotation.value;

							NifKeyGroupFloat yRotation = niTransformData.xYZRotations[1];
							float[] yKnots = yRotation.time;
							float[] yRots = yRotation.value;

							NifKeyGroupFloat zRotation = niTransformData.xYZRotations[2];
							float[] zKnots = zRotation.time;
							float[] zRots = zRotation.value;

							data = new XyzRotationData(xKnots, xRots, yKnots, yRots, zKnots, zRots);
							
							if(CACHE_WEAK)
								xyzRotationDataMap.put(niTransformData, data);
						}						 
					}

					xYZRotPathInterpolator = new XYZRotPathInterpolator(data.xKnots, data.xRots, data.yKnots, data.yRots, data.zKnots,
							data.zRots);
				}
			} else if (niTransformData.rotationType.type == KeyType.QUADRATIC_KEY || niTransformData.rotationType.type == KeyType.TBC_KEY
					|| niTransformData.rotationType.type == KeyType.LINEAR_KEY) {
				NifQuatKey[] quatKeys = niTransformData.quaternionKeys;
				if (quatKeys != null && quatKeys.length > 0) {
					// don't let 2 threads load up the data into weak map
					QuatRotationData data = null;
					synchronized (niTransformData) {
						data = quatRotationDataMap.get(niTransformData);
						if (data == null) {
							float[] knots = new float[quatKeys.length];
							Quat4f[] quats = new Quat4f[quatKeys.length];
							for (int i = 0; i < quatKeys.length; i++) {
								NifQuatKey key = quatKeys[i];
								knots[i] = key.time;
								quats[i] = ConvertFromNif.toJ3d(key.value);
							}
							data = new QuatRotationData(knots, quats);
							if(CACHE_WEAK)
								quatRotationDataMap.put(niTransformData, data);
						}
					}
					quatRotInterpolator = new RotationPathInterpolator(data.knots, data.quats);

				}
			}
			else
			{
				System.out.println("bad key detected " + niTransformData.rotationType.type);
			}
		}

		NifKeyGroupNifVector3 translations = niTransformData.translations;
		if (translations.value != null && translations.value.length > 0) {
			// don't let 2 threads load up the data into weak map
			TranslationData data = null;
			synchronized (niTransformData) {
				data = translationDataMap.get(niTransformData);
				if (data == null) {
					float[] knots = translations.time;
					Point3f[] positions = new Point3f[translations.time.length];
					for (int i = 0; i < translations.time.length; i++) {
						positions[i] = ConvertFromNif.toJ3dP3f(translations.value[i*3+0],translations.value[i*3+1],translations.value[i*3+2]);
					}
					data = new TranslationData(knots, positions);
					
					if(CACHE_WEAK)
						translationDataMap.put(niTransformData, data);
				}
			}
			positionPathInterpolator = new PositionPathInterpolator(data.knots, data.positions);

		}

		NifKeyGroupFloat scaleKeys = niTransformData.scales;
		if (scaleKeys.value != null && scaleKeys.value.length > 0)
		{
			ScaleData data = scaleDataMap.get(niTransformData);
			if (data == null)
			{
				float[] knots = scaleKeys.time;
				float[] scales = scaleKeys.value;
				for (int i = 0; i < scales.length; i++){
					//skyrim hkx bad converter to kf
					if (Float.isNaN(scales[i])){
						scales = null;//set ignore flag
						break;
					}
				}
				data = new ScaleData(knots, scales);
				
				if(CACHE_WEAK)
					scaleDataMap.put(niTransformData, data);
			}
			//check for ignore flagging above
			if (data.scales != null)
				scalePathInterpolator = new ScalePathInterpolator(data.knots, data.scales);
		}
	}


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

	public static class QuatRotationData
	{
		public float[] knots;

		public Quat4f[] quats;

		public QuatRotationData(float[] knots, Quat4f[] quats)
		{
			this.knots = knots;
			this.quats = quats;
		}
	}

	public static class TranslationData
	{
		public float[] knots;

		public Point3f[] positions;

		public TranslationData(float[] knots, Point3f[] positions)
		{
			this.knots = knots;
			this.positions = positions;
		}
	}

	public static class ScaleData
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
