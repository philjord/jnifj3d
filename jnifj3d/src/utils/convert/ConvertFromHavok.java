package utils.convert;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import utils.ESConfig;

import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifQuaternionXYZW;
import nif.compound.NifVector3;
import nif.compound.NifVector4;

public class ConvertFromHavok
{
	public static Quat4f toJ3d(NifQuaternionXYZW rotation)
	{
		return NifRotToJava3DRot.makeJ3dQ4f(rotation.x, rotation.y, rotation.z, rotation.w);
	}

	public static Quat4f toJ3dQ4f(NifMatrix33 rotation)
	{
		return NifRotToJava3DRot.makeJ3dQ4f(rotation);
	}

	public static float toJ3d(float x, float scale)
	{
		return x * ESConfig.HAVOK_TO_METERS_SCALE * scale;
	}

	public static float toJ3d(float x)
	{
		return x * ESConfig.HAVOK_TO_METERS_SCALE;
	}

	public static Vector3f toJ3dV3f(NifMatrix44 transform, float scale)
	{
		return createScaledVector(transform.m14, transform.m24, transform.m34, scale);
	}

	public static Vector3f toJ3dV3f(NifMatrix44 transform)
	{
		return createScaledVector(transform.m14, transform.m24, transform.m34, 1.0f);
	}

	public static Vector3f toJ3d(NifVector3 v, float scale)
	{
		return createScaledVector(v.x, v.y, v.z, scale);
	}

	public static Vector3f toJ3d(NifVector3 v)
	{
		return createScaledVector(v.x, v.y, v.z, 1.0f);
	}

	public static Vector3f toJ3d(NifVector4 v, float scale)
	{
		return createScaledVector(v.x, v.y, v.z, scale);
	}

	public static Vector3f toJ3d(NifVector4 v)
	{
		return createScaledVector(v.x, v.y, v.z, 1.0f);
	}

	public static Point3f toJ3dP3f(NifVector3 v, float scale)
	{
		return createScaledPoint(v.x, v.y, v.z, scale);
	}

	public static Point3f toJ3dP3f(NifVector3 v)
	{
		return createScaledPoint(v.x, v.y, v.z, 1.0f);
	}

	public static Point3f toJ3dP3f(float x, float y, float z, float scale)
	{
		return createScaledPoint(x, y, z, scale);
	}

	public static Point3f toJ3dP3f(float x, float y, float z)
	{
		return createScaledPoint(x, y, z, 1.0f);
	}

	public static Point3f toJ3dP3f(NifVector4 v, float scale)
	{
		return createScaledPoint(v.x, v.y, v.z, scale);
	}

	public static Point3f toJ3dP3f(NifVector4 v)
	{
		return createScaledPoint(v.x, v.y, v.z, 1.0f);
	}

	private static Point3f createScaledPoint(float x, float y, float z, float scale)
	{
		return new Point3f(x * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				z * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				-y * ESConfig.HAVOK_TO_METERS_SCALE * scale);
	}

	private static Vector3f createScaledVector(float x, float y, float z, float scale)
	{
		return new Vector3f(x * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				z * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				-y * ESConfig.HAVOK_TO_METERS_SCALE * scale);
	}

	// NOTE:!! no scale
	public static Vector3f toJ3dNoScale(NifVector3 v)
	{
		return new Vector3f(v.x, v.z, -v.y);
	}

	// NOTE:!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! this uses the nif scale NOT the havok one!!!!
	public static Point3f toJ3dP3fNif(NifVector3 v, float scale)
	{
		return new Point3f(v.x * ESConfig.ES_TO_METERS_SCALE * scale, //
				v.z * ESConfig.ES_TO_METERS_SCALE * scale, //
				-v.y * ESConfig.ES_TO_METERS_SCALE * scale);
	}

	// NOTE:!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! this uses the nif scale NOT the havok one!!!!
	public static Point3f toJ3dP3fNif(NifVector4 v, float scale)
	{
		return new Point3f(v.x * ESConfig.ES_TO_METERS_SCALE * scale, //
				v.z * ESConfig.ES_TO_METERS_SCALE * scale, //
				-v.y * ESConfig.ES_TO_METERS_SCALE * scale);
	}

	//NOTE:!!! this is for extents must remain positive in each dimension
	public static Vector3f toJ3dExtents(NifVector3 dimensions, float scale)
	{
		return new Vector3f(dimensions.x * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				dimensions.z * ESConfig.HAVOK_TO_METERS_SCALE * scale, //
				dimensions.y * ESConfig.HAVOK_TO_METERS_SCALE * scale);
	}

}
