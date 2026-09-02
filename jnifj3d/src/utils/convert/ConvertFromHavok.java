package utils.convert;

import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Matrix3f;
import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;
import org.jogamp.vecmath.AxisAngle4f;

import nif.NifVer;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifQuaternion;
import nif.compound.NifQuaternionXYZW;
import nif.compound.NifVector3;
import nif.compound.NifVector4;

public class ConvertFromHavok {
	public static float getHavokScale(NifVer nifVer) {
		//hkx file have no NifVer, but they are always Skyrim+
		if (nifVer == null)
			return ConvertFromNif.toJ3d(69.994f);//SKYRIM_HAVOK_TO_METERS_SCALE;

		//TODO: confirm the fallout/skyrim interface USER2 number (might be >26)
		if (nifVer.LOAD_VER == NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11 && nifVer.BS_Version >= 83) {
			//Skrim has x10'ed on me! so converter must look up values
			return ConvertFromNif.toJ3d(69.994f);//SKYRIM_HAVOK_TO_METERS_SCALE;
		} else {
			// humans are about 128 units high which at  makes them 1.6256 meters tall would be correct size
			return ConvertFromNif.toJ3d(6.9994f);//PRE_SKYRIM_HAVOK_TO_METERS_SCALE;//it is 7 times the nif
		}
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Quat4f toJ3d(NifQuaternionXYZW rotation) {
		return NifRotToJava3DRot.makeJ3dQ4f(rotation.x, rotation.y, rotation.z, rotation.w);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(NifQuaternionXYZW rotation, Quat4f out) {
		NifRotToJava3DRot.makeJ3dQ4f(rotation.x, rotation.y, rotation.z, rotation.w, out);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Quat4f toJ3d(NifQuaternion rotation) {
		return NifRotToJava3DRot.makeJ3dQ4f(rotation.x, rotation.y, rotation.z, rotation.w);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Matrix4f toJ3dM4(NifMatrix44 mIn, NifVer nifVer) {
		//Future phil look!!!! there was an old comment about pay attention, I've done more more work now
		// this matrix flipper is very much correct now, but if you need to investigate in the future 
		// break it into parts and work without scale or trans which are both good for sure
		//MAJOR!!! note if you want to negate twice you CANNOT USE -- that's a pre-decrement operator!

		/*
		//just the raw rotation in nif axis
		Matrix3f nifRotScale = new Matrix3f(mIn.m11, mIn.m12, mIn.m13,//
				mIn.m21, mIn.m22, mIn.m23,//
				mIn.m31, mIn.m32, mIn.m33 );
		// rotation nif axis mapped to j3d
		// so swap cols 2-3 and row 2-3 do negates of "2"; notice m22 is -*-=+
		nifRotScale = new Matrix3f(mIn.m11, mIn.m13, -mIn.m12,//
				mIn.m31, mIn.m33, -mIn.m32,//
				-mIn.m21, -mIn.m23, mIn.m22 );
		// the translation known good
		float hs2 = getHavokScale(nifVer);
		Vector3f trans = new Vector3f(mIn.m14 * hs2, mIn.m34 * hs2, -mIn.m24 * hs2);
		// the scale taken out
		float s = nifRotScale.getScale();//we assume no non uniform skew
		// the scale set to 1
		nifRotScale.setScale(1.0f); // take the scale out		
				
		// put the 3 parts together
		Matrix4f m2 = new Matrix4f(nifRotScale,trans,s);
		*/

		float hs = getHavokScale(nifVer);

		Matrix4f m = new Matrix4f(//
				mIn.m11, mIn.m13, -mIn.m12, mIn.m14 * hs, //x
				mIn.m31, mIn.m33, -mIn.m32, mIn.m34 * hs, //z
				-mIn.m21, -mIn.m23, mIn.m22, -mIn.m24 * hs, //-y
				0, 0, 0, 1);

		return m;
	}
	
	/**
	 * Creates a new object!
	 * FIXME: this should be better than the Quat4f below, but it doesn't product as correct results
	 * It may be related to the warning inside NifRotToJava3DRot?
	 * @param rotation
	 * @return
	 */
	public static Matrix3f toJ3dM3(NifMatrix33 mIn) {
		//see toJ3dM4
		Matrix3f m = new Matrix3f(//
				mIn.m11, mIn.m13, -mIn.m12, //x
				mIn.m31, mIn.m33, -mIn.m32, //z
				-mIn.m21, -mIn.m23, mIn.m22 //-y
				);

		return m;
	}
	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Quat4f toJ3dQ4f(NifMatrix33 rotation) {
		return NifRotToJava3DRot.makeJ3dQ4f(rotation);
	}

	public static float toJ3d(float x, float scale, NifVer nifVer) {
		return x * getHavokScale(nifVer) * scale;
	}

	public static float toJ3d(float x, NifVer nifVer) {
		return x * getHavokScale(nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3d(NifVector3 v, float scale, NifVer nifVer) {
		return createScaledVector(v.x, v.y, v.z, scale, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3d(NifVector3 v, NifVer nifVer) {
		return createScaledVector(v.x, v.y, v.z, 1.0f, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3d(NifVector4 v, float scale, NifVer nifVer) {
		return createScaledVector(v.x, v.y, v.z, scale, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3d(NifVector4 v, NifVer nifVer) {
		return createScaledVector(v.x, v.y, v.z, 1.0f, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(NifVector3 v, NifVer nifVer) {
		return createScaledPoint(v.x, v.y, v.z, 1.0f, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(float x, float y, float z, float scale, NifVer nifVer) {
		return createScaledPoint(x, y, z, scale, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(float x, float y, float z, NifVer nifVer) {
		return createScaledPoint(x, y, z, 1.0f, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(NifVector4 v, float scale, NifVer nifVer) {
		return createScaledPoint(v.x, v.y, v.z, scale, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(NifVector4 v, NifVer nifVer) {
		return createScaledPoint(v.x, v.y, v.z, 1.0f, nifVer);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	private static Point3f createScaledPoint(float x, float y, float z, float scale, NifVer nifVer) {
		float hs = getHavokScale(nifVer);
		return new Point3f(x * hs * scale, //
				z * hs * scale, //
				-y * hs * scale);
	}

	/**
	 * Creates a new object!
	 * @param rotation
	 * @return
	 */
	private static Vector3f createScaledVector(float x, float y, float z, float scale, NifVer nifVer) {
		float hs = getHavokScale(nifVer);
		return new Vector3f(x * hs * scale, //
				z * hs * scale, //
				-y * hs * scale);
	}

	/** 
	 * Creates a new object! 
	 * NOTE:!! no scale 
	 *
	 * @param v
	 * @return
	 */
	public static Vector3f toJ3dNoScale(NifVector3 v) {
		return new Vector3f(v.x, v.z, -v.y);
	}

	/**
	 * Creates a new object!
	 * NOTE:!!!! this uses the nif scale NOT the havok one as well as the passed in scale
	 * @param v
	 * @param scale
	 * @return
	 */
	public static Point3f toJ3dP3fNif(NifVector3 v, float scale) {
		return ConvertFromNif.toJ3dP3fNif(v, scale);
	}

	/**
	 * Creates a new object!
	 * NOTE:!!!! this uses the nif scale NOT the havok one as well as the passed in scale
	 * @param v
	 * @param scale
	 * @return
	 */
	public static Point3f toJ3dP3fNif(NifVector4 v, float scale) {
		return ConvertFromNif.toJ3dP3fNif(v, scale);
	}

	/**
	 * Creates a new object!
	 * NOTE:!!! this is for extents must remain positive in each dimension
	 * @param dimensions
	 * @param scale
	 * @param nifVer
	 * @return
	 */
	public static Vector3f toJ3dExtents(NifVector3 dimensions, float scale, NifVer nifVer) {
		float hs = getHavokScale(nifVer);
		return new Vector3f(dimensions.x * hs * scale, //
				dimensions.z * hs * scale, //
				dimensions.y * hs * scale);
	}

}
