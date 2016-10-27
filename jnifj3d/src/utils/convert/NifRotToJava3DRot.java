package utils.convert;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jogamp.vecmath.Matrix3f;
import org.jogamp.vecmath.Quat4f;

import nif.compound.NifMatrix33;

public class NifRotToJava3DRot
{

	/**
	 * Taken from http://sourceforge.net/p/niftools/niflib/ci/c0a1fdd9f71995fe2284ee96cb0ee1056e1c56d1/tree/src/nif_math.cpp#l343
	 * @param mat
	 * @return
	 */
	public static Quat4f makeJ3dQ4f(NifMatrix33 mat)
	{
		// ConvertFromHavok NifMatrix44 FAILED badly with this call, see oblivion lowerlass clutter
		// however testing showen below proves the sames math is not corect for 33 matrix!

	/*	if (true)
		{
			Matrix3f m = new Matrix3f(mat.m11, mat.m13, -mat.m12, //
					mat.m31, mat.m33, mat.m32, //
					-mat.m21, -mat.m23, mat.m22);

			Matrix3f m2 = new Matrix3f(mat.m11, mat.m12, mat.m13, //
					mat.m21, mat.m22, mat.m23, //
					mat.m31, mat.m32, mat.m33);

			Quat4f q = new Quat4f();
			q.set(m2);
			NifRotToJava3DRot.flipAxis(q);

			return q;
		}*/

		Quat4f quat = new Quat4f();
		float[][] m = new float[3][3];

		m[0][0] = mat.m11;
		m[0][1] = mat.m12;
		m[0][2] = mat.m13;
		m[1][0] = mat.m21;
		m[1][1] = mat.m22;
		m[1][2] = mat.m23;
		m[2][0] = mat.m31;
		m[2][1] = mat.m32;
		m[2][2] = mat.m33;

		float tr, s;
		float[] q = new float[4];
		int i, j, k;
		int[] nxt = new int[]
		{ 1, 2, 0 };

		// compute the trace of the matrix
		tr = m[0][0] + m[1][1] + m[2][2];
		// check if the trace is positive or negative
		if (tr > 0.0)
		{
			s = (float) Math.sqrt(tr + 1.0f);
			quat.w = s / 2.0f;
			s = 0.5f / s;
			quat.x = (m[1][2] - m[2][1]) * s;
			quat.y = (m[2][0] - m[0][2]) * s;
			quat.z = (m[0][1] - m[1][0]) * s;
		}
		else
		{
			// trace is negative
			i = 0;
			if (m[1][1] > m[0][0])
				i = 1;
			if (m[2][2] > m[i][i])
				i = 2;
			j = nxt[i];
			k = nxt[j];
			s = (float) Math.sqrt((m[i][i] - (m[j][j] + m[k][k])) + 1.0f);
			q[i] = s * 0.5f;
			if (s != 0.0f)
				s = 0.5f / s;
			q[3] = (m[j][k] - m[k][j]) * s;
			q[j] = (m[i][j] + m[j][i]) * s;
			q[k] = (m[i][k] + m[k][i]) * s;
			quat.x = q[0];
			quat.y = q[1];
			quat.z = q[2];
			quat.w = q[3];
		}

		return flipAxis(quat);
	}

	public static Quat4f makeJ3dQ4f(float x, float y, float z, float w)
	{
		Quat4f q = new Quat4f(x, y, z, w);
		return flipAxis(q);
	}

	public static Quat4f flipAxis(Quat4f q)
	{
		//taken from
		//http://stackoverflow.com/questions/18818102/convert-quaternion-representing-rotation-from-one-coordinate-system-to-another

		q.set(q.x, q.z, -q.y, q.w);
		return q;
	}

	@Deprecated
	public static Quat4f makeJ3dQ4fOld(NifMatrix33 rotation)
	{
		return makeJ3dQ4f(rotation.m11, rotation.m12, rotation.m13, rotation.m21, rotation.m22, rotation.m23, rotation.m31, rotation.m32,
				rotation.m33);
	}

	@Deprecated
	private static Quat4f makeJ3dQ4f(float m11, float m12, float m13, float m21, float m22, float m23, float m31, float m32, float m33)
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
	 * // NOTE to Phil in future past 14/07/2014 above is the proper way to do this, below was nearly right
	 *  NOTE to future phil, this took a looooooong time to sort out, so don't you delete it!
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
	@Deprecated
	public static float truncToDP(float in, int scale)
	{
		if (Float.isInfinite(in) || Float.isNaN(in))
			return in;

		return new BigDecimal(in).setScale(scale, RoundingMode.HALF_UP).floatValue();

		//return Math.floor(in*10^scale)/10^scaled;

		//DecimalFormat df = new DecimalFormat();
		//df.setMaximumFractionDigits(4);
		//m11 = Float.parseFloat(df.format(m11));
	}

}
