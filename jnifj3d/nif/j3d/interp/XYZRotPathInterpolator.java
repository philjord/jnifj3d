package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

public class XYZRotPathInterpolator extends TransformInterpolator
{

	

	private Quat4f rot = new Quat4f();

	float[] xKnots;

	float[] xRots;

	float[] yKnots;

	float[] yRots;

	float[] zKnots;

	float[] zRots;

	public XYZRotPathInterpolator(TransformGroup target, float[] xKnots, float[] xRots, float[] yKnots, float[] yRots, float[] zKnots,
			float[] zRots)
	{
		//note dummy knots
		super(target);

		this.xKnots = xKnots;
		this.xRots = xRots;
		this.yKnots = yKnots;
		this.yRots = yRots;
		this.zKnots = zKnots;
		this.zRots = zRots;
		
		fixed = isFixed();
		if (fixed)
		{
			temp.setEuler(new Vector3d(xRots[0], yRots[0], zRots[0]));
			temp.get(rot);
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
		for (int i = 0; i + 1 < yRots.length; i++)
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

			float xRot = 0;
			float yRot = 0;
			float zRot = 0;

			if (xCurrentKnotIndex == 0 && xCurrentInterpolationValue == 0f)
			{
				xRot = xRots[0];
			}
			else
			{
				xRot = xRots[xCurrentKnotIndex] + (xRots[xCurrentKnotIndex + 1] - xRots[xCurrentKnotIndex]) * xCurrentInterpolationValue;
			}

			if (yCurrentKnotIndex == 0 && yCurrentInterpolationValue == 0f)
			{
				yRot = yRots[0];
			}
			else
			{
				yRot = yRots[yCurrentKnotIndex] + (yRots[yCurrentKnotIndex + 1] - yRots[yCurrentKnotIndex]) * yCurrentInterpolationValue;
			}

			if (zCurrentKnotIndex == 0 && zCurrentInterpolationValue == 0f)
			{
				zRot = zRots[0];
			}
			else
			{
				zRot = zRots[zCurrentKnotIndex] + (zRots[zCurrentKnotIndex + 1] - zRots[zCurrentKnotIndex]) * zCurrentInterpolationValue;
			}

			temp.setEuler(new Vector3d(xRot, yRot, zRot));
			temp.get(rot);
		}
	}

	public void applyTransform(Transform3D targetTransform)
	{
		targetTransform.setRotation(rot);
	}
}
