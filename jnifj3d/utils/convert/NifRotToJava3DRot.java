package utils.convert;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;

import nif.compound.NifMatrix33;

public class NifRotToJava3DRot
{

	public static Quat4f makeJ3dQ4f(float x, float y, float z, float w)
	{
		Quat4f q = new Quat4f(x, y, z, w);
		return flipAxis(q);
	}

	public static Quat4f makeJ3dQ4f(NifMatrix33 rotation)
	{
		return makeJ3dQ4f(rotation.m11, rotation.m12, rotation.m13, rotation.m21, rotation.m22, rotation.m23, rotation.m31, rotation.m32,
				rotation.m33);
	}

	public static Quat4f makeJ3dQ4f(float m11, float m12, float m13, float m21, float m22, float m23, float m31, float m32, float m33)
	{
		m11 = truncToDP(m11, 4);
		m12 = truncToDP(m12, 4);
		m13 = truncToDP(m13, 4);
		m21 = truncToDP(m21, 4);
		m22 = truncToDP(m22, 4);
		m23 = truncToDP(m23, 4);
		m31 = truncToDP(m31, 4);
		m32 = truncToDP(m32, 4);
		m33 = truncToDP(m33, 4);

		Matrix3f in = new Matrix3f(m11, m12, m13, m21, m22, m23, m31, m32, m33);
		// NOTE setting an AA from a M33 is actually provably broken, the quat must be used
		Quat4f q = new Quat4f();
		q.set(in);

		return flipAxis(q);
	}

	/**
	 * // NOTE to future phil, this took a looooooong time to sort out, so don't you delete it!
		// relating to bridge commander and 3.1 only others not tested.
		// If we look at the reported values in nifskope they are rounded to 4 d.p. and the rotations work nicely.
		// The nif file editor apparently writes out floats close (8d.p.) to 0 and 1 but not actually 0 or 1
		// these slightly inaccurate value mean that the matix is not in a normalized state and calls to quat or
		// axisangle will silently fail (cos the matrix is bum) and we get (silently) null rotations back.
		// Round to 4 decimal places, as otherwise the matrix is actually not normalised (the encoded values are a bit
		// off? weird!)

		// TODO: can I not check for this case before doing all the expensve parsing below?
		// Matrix3f in = new Matrix3f(m11, m12, m13, m21, m22, m23, m31, m32, m33);
		// if (in.determinant() != 1)

	 * @param in
	 * @param scale
	 * @return
	 */
	private static float truncToDP(float in, int scale)
	{
		if (Float.isInfinite(in) || Float.isNaN(in))
			return in;

		return new BigDecimal(in).setScale(scale, RoundingMode.HALF_UP).floatValue();

		//return Math.floor(in*10^scale)/10^scaled;

		//DecimalFormat df = new DecimalFormat();
		//df.setMaximumFractionDigits(4);
		//m11 = Float.parseFloat(df.format(m11));
	}

	private static Quat4f flipAxis(Quat4f q)
	{
		//TODO: I can do this directly in my quat! test
		//http://stackoverflow.com/questions/18818102/convert-quaternion-representing-rotation-from-one-coordinate-system-to-another

		//AxisAngle4f a = new AxisAngle4f();
		//a.set(q);

		//a.set(a.x, a.z, -a.y, a.angle);
		//q.set(a);
		
		q.set(q.x, q.z, -q.y, q.w);
		return q;
	}

	/**
	 * This exists because if teh ww figure below is actually slightly negative(due to bad interpolated quats)
	 * Then teh output quat is just NaNs
	 * @param t1
	 * @param q1
	 */
	public static void safeGetQuat(Transform3D t1, Quat4f q1)
	{

		float[] mat = new float[16];
		t1.get(mat);
		float[] rot = new float[9];
		rot[0] = mat[0];
		rot[1] = mat[1];
		rot[2] = mat[2];
		rot[3] = mat[4];
		rot[4] = mat[5];
		rot[5] = mat[6];
		rot[6] = mat[8];
		rot[7] = mat[9];
		rot[8] = mat[10];

		double ww = 0.25 * (1.0 + rot[0] + rot[4] + rot[8]);

		// no negatives or the sqrt below starts baking Nans
		ww = (ww < 0 ? 0 : ww);
		if (!(ww < 1.0e-10))
		{
			q1.w = (float) Math.sqrt(ww);
			ww = 0.25 / q1.w;
			q1.x = (float) ((rot[7] - rot[5]) * ww);
			q1.y = (float) ((rot[2] - rot[6]) * ww);
			q1.z = (float) ((rot[3] - rot[1]) * ww);
			return;
		}

		q1.w = 0.0f;
		ww = -0.5 * (rot[4] + rot[8]);
		if (!((ww < 0 ? -ww : ww) < 1.0e-10))
		{
			q1.x = (float) Math.sqrt(ww);
			ww = 0.5 / q1.x;
			q1.y = (float) (rot[3] * ww);
			q1.z = (float) (rot[6] * ww);
			return;
		}

		q1.x = 0.0f;
		ww = 0.5 * (1.0 - rot[8]);
		if (!((ww < 0 ? -ww : ww) < 1.0e-10))
		{
			q1.y = (float) Math.sqrt(ww);
			q1.z = (float) (rot[7] / (2.0 * q1.y));
			return;
		}

		q1.y = 0.0f;
		q1.z = 1.0f;
	}

}
