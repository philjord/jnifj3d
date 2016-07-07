package utils.convert;

import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import utils.ESConfig;
import nif.compound.NifColor4;
import nif.compound.NifMatrix33;
import nif.compound.NifQuaternion;
import nif.compound.NifTexCoord;
import nif.compound.NifVector3;

public class ConvertFromNif
{
	public static Quat4f toJ3d(NifMatrix33 rotation)
	{
		return NifRotToJava3DRot.makeJ3dQ4f(rotation);
	}

	public static Quat4f toJ3d(NifQuaternion rotation)
	{
		Quat4f q = new Quat4f(rotation.x, rotation.y, rotation.z, rotation.w);
		return NifRotToJava3DRot.flipAxis(q);
	}

	public static float toJ3d(float x)
	{
		return x * ESConfig.ES_TO_METERS_SCALE;
	}

	public static Vector3f toJ3d(NifVector3 v)
	{
		return createScaledVector(v.x, v.y, v.z);
	}

	public static Point3f toJ3dP3f(NifVector3 v)
	{
		return createScaledPoint(v.x, v.y, v.z);
	}

	public static void toJ3d(Tuple3f p)
	{
		p.set(p.x * ESConfig.ES_TO_METERS_SCALE, //
				p.z * ESConfig.ES_TO_METERS_SCALE, //
				-p.y * ESConfig.ES_TO_METERS_SCALE);
	}

	public static Point3f toJ3dP3f(float x, float y, float z)
	{
		return createScaledPoint(x, y, z);
	}

	private static Point3f createScaledPoint(float x, float y, float z)
	{
		return new Point3f(x * ESConfig.ES_TO_METERS_SCALE, //
				z * ESConfig.ES_TO_METERS_SCALE, //
				-y * ESConfig.ES_TO_METERS_SCALE);
	}

	private static Vector3f createScaledVector(float x, float y, float z)
	{
		return new Vector3f(x * ESConfig.ES_TO_METERS_SCALE, //
				z * ESConfig.ES_TO_METERS_SCALE, //
				-y * ESConfig.ES_TO_METERS_SCALE);
	}

	// NOTE: no scale
	public static Vector3f toJ3dNoScale(NifVector3 v)
	{
		return new Vector3f(v.x, v.z, -v.y);
	}

	public static Point3d toJ3dP3d(NifVector3 v)
	{
		return createScaledPoint((double) v.x, (double) v.y, (double) v.z);
	}

	private static Point3d createScaledPoint(double x, double y, double z)
	{
		return new Point3d(x * ESConfig.ES_TO_METERS_SCALE, //
				z * ESConfig.ES_TO_METERS_SCALE, //
				-y * ESConfig.ES_TO_METERS_SCALE);
	}

	public static Color4f toJ3d(NifColor4 color4)
	{
		return new Color4f(color4.r, color4.g, color4.b, color4.a);
	}

	public static TexCoord2f toJ3d(NifTexCoord coord)
	{
		return new TexCoord2f(coord.u, -coord.v);
	}

}
