package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import tools3d.utils.Utils3D;

public class XYZRotPathInterpolator extends KnotInterpolator
{
	private Vector3d interpedRot = new Vector3d();

	float[] xKnots;

	float[] xRots;

	float[] yKnots;

	float[] yRots;

	float[] zKnots;

	float[] zRots;

	public XYZRotPathInterpolator(float[] xKnots, float[] xRots, float[] yKnots, float[] yRots, float[] zKnots, float[] zRots)
	{
		//note dummy knots
		super(new float[]
		{ 0 });

		this.xKnots = xKnots;
		this.xRots = xRots;
		this.yKnots = yKnots;
		this.yRots = yRots;
		this.zKnots = zKnots;
		this.zRots = zRots;

		fixed = isFixed();
		if (fixed)
		{
			interpedRot.set(xRots[0], yRots[0], zRots[0]);
		}
	}

	private boolean isFixed()
	{
		//check for a fixed value
		for (int i = 0; i + 1 < xRots.length; i++)
		{
			if (xRots[i] != xRots[i + 1])
				return false;
		}
		for (int i = 0; i + 1 < yRots.length; i++)
		{
			if (yRots[i] != yRots[i + 1])
				return false;
		}
		for (int i = 0; i + 1 < zRots.length; i++)
		{
			if (zRots[i] != zRots[i + 1])
				return false;
		}

		return true;
	}

	private Transform3D temp = new Transform3D();

	@Override
	public void computeTransform(float alphaValue)
	{
		if (!fixed)
		{
			float xCurrentInterpolationValue = 0;
			int xCurrentKnotIndex = 0;
			float yCurrentInterpolationValue = 0;
			int yCurrentKnotIndex = 0;
			float zCurrentInterpolationValue = 0;
			int zCurrentKnotIndex = 0;

			for (int i = 0; i < xKnots.length; i++)
			{
				if ((i == 0 && alphaValue <= xKnots[i]) || (i > 0 && alphaValue >= xKnots[i - 1] && alphaValue <= xKnots[i]))
				{
					if (i == 0)
					{
						xCurrentInterpolationValue = 0f;
						xCurrentKnotIndex = 0;
					}
					else
					{
						xCurrentInterpolationValue = (alphaValue - xKnots[i - 1]) / (xKnots[i] - xKnots[i - 1]);
						xCurrentKnotIndex = i - 1;
					}
					break;
				}
			}
			for (int i = 0; i < yKnots.length; i++)
			{
				if ((i == 0 && alphaValue <= yKnots[i]) || (i > 0 && alphaValue >= yKnots[i - 1] && alphaValue <= yKnots[i]))
				{
					if (i == 0)
					{
						yCurrentInterpolationValue = 0f;
						yCurrentKnotIndex = 0;
					}
					else
					{
						yCurrentInterpolationValue = (alphaValue - yKnots[i - 1]) / (yKnots[i] - yKnots[i - 1]);
						yCurrentKnotIndex = i - 1;
					}
					break;
				}
			}
			for (int i = 0; i < zKnots.length; i++)
			{
				if ((i == 0 && alphaValue <= zKnots[i]) || (i > 0 && alphaValue >= zKnots[i - 1] && alphaValue <= zKnots[i]))
				{
					if (i == 0)
					{
						zCurrentInterpolationValue = 0f;
						zCurrentKnotIndex = 0;
					}
					else
					{
						zCurrentInterpolationValue = (alphaValue - zKnots[i - 1]) / (zKnots[i] - zKnots[i - 1]);
						zCurrentKnotIndex = i - 1;
					}
					break;
				}
			}

			if (xCurrentKnotIndex == 0 && xCurrentInterpolationValue == 0f)
			{
				interpedRot.x = xRots[0];
			}
			else
			{
				interpedRot.x = xRots[xCurrentKnotIndex] + (xRots[xCurrentKnotIndex + 1] - xRots[xCurrentKnotIndex])
						* xCurrentInterpolationValue;
			}

			if (yCurrentKnotIndex == 0 && yCurrentInterpolationValue == 0f)
			{
				interpedRot.y = yRots[0];
			}
			else
			{
				interpedRot.y = yRots[yCurrentKnotIndex] + (yRots[yCurrentKnotIndex + 1] - yRots[yCurrentKnotIndex])
						* yCurrentInterpolationValue;
			}

			if (zCurrentKnotIndex == 0 && zCurrentInterpolationValue == 0f)
			{
				interpedRot.z = zRots[0];
			}
			else
			{
				interpedRot.z = zRots[zCurrentKnotIndex] + (zRots[zCurrentKnotIndex + 1] - zRots[zCurrentKnotIndex])
						* zCurrentInterpolationValue;
			}

		}
	}

	private Quat4f rot = new Quat4f();

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		
		
		temp.setEuler(interpedRot);
		Utils3D.safeGetQuat(temp, rot);
		targetTransform.setRotation(rot);
		
		
		//non static frame rotations
	/*	Transform3D tx = new Transform3D();
		Transform3D ty = new Transform3D();
		Transform3D tz = new Transform3D();
		tx.rotX(interpedRot.x);
		ty.rotY(interpedRot.y);
		tz.rotZ(interpedRot.z);
		
		temp.set(tx);		
		temp.mul(tz);
		temp.mul(ty);
		
		Utils3D.safeGetQuat(temp, rot);
		targetTransform.setRotation(rot);*/
	}
}
