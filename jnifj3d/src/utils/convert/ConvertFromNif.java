package utils.convert;

import org.jogamp.vecmath.Color4f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.TexCoord2f;
import org.jogamp.vecmath.Tuple3f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import nif.compound.NifColor4;
import nif.compound.NifMatrix33;
import nif.compound.NifQuaternion;
import nif.compound.NifTexCoord;
import nif.compound.NifVector3;
import nif.compound.NifVector4;
import nif.niobject.bs.BSTriShape;

public class ConvertFromNif {

	public static float toJ3d(float x) {
		return x * BSTriShape.ES_TO_METERS_SCALE;
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Quat4f toJ3d(NifMatrix33 rotation) {
		return NifRotToJava3DRot.makeJ3dQ4f(rotation);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Quat4f toJ3d(NifQuaternion rotation) {
		Quat4f q = new Quat4f(rotation.x, rotation.y, rotation.z, rotation.w);
		return NifRotToJava3DRot.flipAxis(q);
	}

	/**
	 * Creates a new object
	 * @param v
	 * @return
	 */
	public static Vector3f toJ3d(NifVector3 v) {
		return createScaledVector(v.x, v.y, v.z);
	}

	/**
	 * Creates a new object
	 * @param v
	 * @return
	 */
	public static Point3f toJ3dP3f(NifVector3 v) {
		return createScaledPoint(v.x, v.y, v.z);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3d(float x, float y, float z) {
		return createScaledVector(x, y, z);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Point3f toJ3dP3f(float x, float y, float z) {
		return createScaledPoint(x, y, z);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Color4f toJ3d(NifColor4 color4) {
		return new Color4f(color4.r, color4.g, color4.b, color4.a);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static TexCoord2f toJ3d(NifTexCoord coord) {
		return new TexCoord2f(coord.u, -coord.v);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Point3d toJ3dP3d(NifVector3 v) {
		return createScaledPoint((double)v.x, (double)v.y, (double)v.z);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static Point3d toJ3dP3d(double x, double y, double z) {
		return createScaledPoint(x, y, z);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	private static Point3f createScaledPoint(float x, float y, float z) {
		return new Point3f(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	private static Vector3f createScaledVector(float x, float y, float z) {
		return new Vector3f(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	private static Point3d createScaledPoint(double x, double y, double z) {
		return new Point3d(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	//package level
	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	static Point3f toJ3dP3fNif(NifVector3 v, float scale) {
		return new Point3f(v.x * BSTriShape.ES_TO_METERS_SCALE * scale, //
				v.z * BSTriShape.ES_TO_METERS_SCALE * scale, //
				-v.y * BSTriShape.ES_TO_METERS_SCALE * scale);
	}

	//package level
	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	static Point3f toJ3dP3fNif(NifVector4 v, float scale) {
		return new Point3f(v.x * BSTriShape.ES_TO_METERS_SCALE * scale, //
				v.z * BSTriShape.ES_TO_METERS_SCALE * scale, //
				-v.y * BSTriShape.ES_TO_METERS_SCALE * scale);
	}

	// NOTE: no scaling from nif to meters
	/**
	 * Creates a new object
	 * Notice that if this is used for a scale factor in 3 dims y should not be reversed
	 * But if used as a normal or an axis (also normalised) then do reverse y
	 * @param rotation
	 * @return
	 */
	public static Vector3f toJ3dNoScale(NifVector3 v) {
		return new Vector3f(v.x, v.z, -v.y);
	}

	/**
	 * Converts the internals of the input object and returns that SAME object
	 * Notice scales do NOT get y reversed
	 * @param rotation
	 * @return
	 */
	public static Vector3d toJ3d(Vector3d tScale) {
		tScale.set(tScale.x, tScale.z, tScale.y);
		return tScale;
	}

	/**
	 * Converts the internals of the input object and returns that SAME object
	 * @param p
	 */
	public static void toJ3d(Tuple3f p) {
		p.set(p.x * BSTriShape.ES_TO_METERS_SCALE, //
				p.z * BSTriShape.ES_TO_METERS_SCALE, //
				-p.y * BSTriShape.ES_TO_METERS_SCALE);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(NifMatrix33 rotation, Quat4f out) {
		NifRotToJava3DRot.makeJ3dQ4f(rotation, out);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(NifQuaternion rotation, Quat4f out) {
		out.set(rotation.x, rotation.y, rotation.z, rotation.w);
		NifRotToJava3DRot.flipAxis(out);
	}

	/**
	 * Places value into out
	 * @param v
	 * @return
	 */
	public static void toJ3d(NifVector3 v, Vector3f out) {
		createScaledVector(v.x, v.y, v.z, out);
	}

	/**
	 * Places value into out
	 * @param v
	 * @return
	 */
	public static void toJ3dP3f(NifVector3 v, Point3f out) {
		createScaledPoint(v.x, v.y, v.z, out);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(float x, float y, float z, Vector3f out) {
		createScaledVector(x, y, z, out);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3dP3f(float x, float y, float z, Point3f out) {
		createScaledPoint(x, y, z, out);
	}

	/**
	 * Creates a new object
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(NifColor4 color4, Color4f out) {
		out.set(color4.r, color4.g, color4.b, color4.a);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3d(NifTexCoord coord, TexCoord2f out) {
		out.set(coord.u, -coord.v);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3dP3d(NifVector3 v, Point3d out) {
		createScaledPoint(v.x, v.y, v.z, out);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	public static void toJ3dP3d(double x, double y, double z, Point3d out) {
		createScaledPoint(x, y, z, out);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	private static void createScaledPoint(float x, float y, float z, Point3f out) {
		out.set(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	private static void createScaledVector(float x, float y, float z, Vector3f out) {
		out.set(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	private static void createScaledPoint(double x, double y, double z, Point3d out) {
		out.set(x * BSTriShape.ES_TO_METERS_SCALE, //
				z * BSTriShape.ES_TO_METERS_SCALE, //
				-y * BSTriShape.ES_TO_METERS_SCALE);
	}

	//package level
	/**
	 * Places value into out
	 * @param rotation
	 * @return
	 */
	static void toJ3dP3fNif(NifVector3 v, float scale, Point3f out) {
		out.set(v.x * BSTriShape.ES_TO_METERS_SCALE * scale, //
				v.z * BSTriShape.ES_TO_METERS_SCALE * scale, //
				-v.y * BSTriShape.ES_TO_METERS_SCALE * scale);
	}

}
