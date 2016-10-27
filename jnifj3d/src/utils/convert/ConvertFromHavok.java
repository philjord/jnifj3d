package utils.convert;

import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import nif.NifVer;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifQuaternionXYZW;
import nif.compound.NifVector3;
import nif.compound.NifVector4;
import utils.ESConfig;

public class ConvertFromHavok
{
	//Skrim has x10'ed on me! so converter must look up values
	public static float SKYRIM_HAVOK_TO_METERS_SCALE = ESConfig.ES_TO_METERS_SCALE * 69.994f;

	// humans are about 128 units high which at  makes them 1.6256 meters tall would be correct size
	public static float PRE_SKYRIM_HAVOK_TO_METERS_SCALE = ESConfig.ES_TO_METERS_SCALE * 6.9994f;//it is 7 times the nif

	public static float getHavokScale(NifVer nifVer)
	{
		//TODO: confirm the fallout/skyrim interface USER2 number (might be >26)
		if (nifVer.LOAD_VER == NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11 && nifVer.LOAD_USER_VER2 >= 83)
			return SKYRIM_HAVOK_TO_METERS_SCALE;
		else
			return PRE_SKYRIM_HAVOK_TO_METERS_SCALE;
	}

	public static Quat4f toJ3d(NifQuaternionXYZW rotation)
	{
		return NifRotToJava3DRot.makeJ3dQ4f(rotation.x, rotation.y, rotation.z, rotation.w);
	}


	public static Matrix4f toJ3dM4(NifMatrix44 mIn, NifVer nifVer)
	{
		//Future phil look!!!! empirically decided
		// the set q and flip appears very strongly correct
		/*		Matrix4f m4 = new Matrix4f(mIn.m11, mIn.m12, mIn.m13, //
						0, //
						mIn.m21, mIn.m22, mIn.m23, //
						0, //
						mIn.m31, mIn.m32, mIn.m33, //
						0,//
						mIn.m41, mIn.m42, mIn.m43, mIn.m44);

				q.set(m4);
				NifRotToJava3DRot.flipAxis(q);

				Matrix4f mo = new Matrix4f();
				mo.set(q);*/

		// In flipping I see empirically
		// 00->00 -01->02  02->01  03->03(t)
		// -?  10->20  11->22 -12->21  13->13(t)
		// 20->10  21->12? 22->11  23->23(t)?0
		// 30-32 all 0 33,0.94->33,1

		float hs = getHavokScale(nifVer);
		Matrix4f m = new Matrix4f(mIn.m11, mIn.m13, -mIn.m12, //
				mIn.m14 * hs, //x
				mIn.m31, mIn.m33, mIn.m32, //
				mIn.m34 * hs, //z
				-mIn.m21, -mIn.m23, mIn.m22, //
				-mIn.m24 * hs,//-y
				0, 0, 0, 1);
		return m;
	}

	public static Quat4f toJ3dQ4f(NifMatrix33 rotation)
	{
		return NifRotToJava3DRot.makeJ3dQ4f(rotation);
	}

	public static float toJ3d(float x, float scale, NifVer nifVer)
	{
		return x * getHavokScale(nifVer) * scale;
	}

	public static float toJ3d(float x, NifVer nifVer)
	{
		return x * getHavokScale(nifVer);
	}

	public static Vector3f toJ3dV3f(NifMatrix44 transform, float scale, NifVer nifVer)
	{
		return createScaledVector(transform.m14, transform.m24, transform.m34, scale, nifVer);
	}

	public static Vector3f toJ3dV3f(NifMatrix44 transform, NifVer nifVer)
	{
		return createScaledVector(transform.m14, transform.m24, transform.m34, 1.0f, nifVer);
	}

	public static Vector3f toJ3d(NifVector3 v, float scale, NifVer nifVer)
	{
		return createScaledVector(v.x, v.y, v.z, scale, nifVer);
	}

	public static Vector3f toJ3d(NifVector3 v, NifVer nifVer)
	{
		return createScaledVector(v.x, v.y, v.z, 1.0f, nifVer);
	}

	public static Vector3f toJ3d(NifVector4 v, float scale, NifVer nifVer)
	{
		return createScaledVector(v.x, v.y, v.z, scale, nifVer);
	}

	public static Vector3f toJ3d(NifVector4 v, NifVer nifVer)
	{
		return createScaledVector(v.x, v.y, v.z, 1.0f, nifVer);
	}

	public static Point3f toJ3dP3f(NifVector3 v, float scale, NifVer nifVer)
	{
		return createScaledPoint(v.x, v.y, v.z, scale, nifVer);
	}

	public static Point3f toJ3dP3f(NifVector3 v, NifVer nifVer)
	{
		return createScaledPoint(v.x, v.y, v.z, 1.0f, nifVer);
	}

	public static Point3f toJ3dP3f(float x, float y, float z, float scale, NifVer nifVer)
	{
		return createScaledPoint(x, y, z, scale, nifVer);
	}

	public static Point3f toJ3dP3f(float x, float y, float z, NifVer nifVer)
	{
		return createScaledPoint(x, y, z, 1.0f, nifVer);
	}

	public static Point3f toJ3dP3f(NifVector4 v, float scale, NifVer nifVer)
	{
		return createScaledPoint(v.x, v.y, v.z, scale, nifVer);
	}

	public static Point3f toJ3dP3f(NifVector4 v, NifVer nifVer)
	{
		return createScaledPoint(v.x, v.y, v.z, 1.0f, nifVer);
	}

	private static Point3f createScaledPoint(float x, float y, float z, float scale, NifVer nifVer)
	{
		float hs = getHavokScale(nifVer);
		return new Point3f(x * hs * scale, //
				z * hs * scale, //
				-y * hs * scale);
	}

	private static Vector3f createScaledVector(float x, float y, float z, float scale, NifVer nifVer)
	{
		float hs = getHavokScale(nifVer);
		return new Vector3f(x * hs * scale, //
				z * hs * scale, //
				-y * hs * scale);
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
	public static Vector3f toJ3dExtents(NifVector3 dimensions, float scale, NifVer nifVer)
	{
		float hs = getHavokScale(nifVer);
		return new Vector3f(dimensions.x * hs * scale, //
				dimensions.z * hs * scale, //
				dimensions.y * hs * scale);
	}

}
