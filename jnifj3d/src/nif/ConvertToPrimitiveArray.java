package nif;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class ConvertToPrimitiveArray
{
	public static float[] convert(Point3f[] in)
	{
		float[] out = new float[in.length * 3];
		for (int i = 0; i < in.length; i++)
		{
			out[i * 3 + 0] = in[i].x;
			out[i * 3 + 1] = in[i].y;
			out[i * 3 + 2] = in[i].z;
		}
		return out;
	}

	public static float[] convert(Vector3f[] in)
	{
		float[] out = new float[in.length * 3];
		for (int i = 0; i < in.length; i++)
		{
			out[i * 3 + 0] = in[i].x;
			out[i * 3 + 1] = in[i].y;
			out[i * 3 + 2] = in[i].z;
		}
		return out;
	}

}
