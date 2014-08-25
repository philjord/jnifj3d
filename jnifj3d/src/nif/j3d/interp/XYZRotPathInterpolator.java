package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import tools3d.utils.Utils3D;

public class XYZRotPathInterpolator extends KnotInterpolator
{
	private Vector3f interpedRot = new Vector3f();

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
			//FIXME: looks like transferring eulers into transforms into quat4f etc eventually round to a blank
			// answer is probably to for this class to hand out a vector of interp and allow a basis interp to be added in

			//TODO: trunc needed? or not?
			//	System.out.println("fx x " +Utils3D.truncToDP(xRots[0],2) + " y " + Utils3D.truncToDP(yRots[0],2)+ " z " + Utils3D.truncToDP(zRots[0],2));

			// works interpedRot.set(xRots[0]+(float)Math.PI, yRots[0]-(float)(Math.PI / 2f), zRots[0]);
			interpedRot.set(xRots[0] , yRots[0] , zRots[0]);
			//interpedRot.set(-(float)Math.PI / 2f , (float)Math.PI , zRots[0]);
			
			System.out.println("" + this + " fixed = " + interpedRot);
			temp.setEuler(new Vector3d(xRots[0], yRots[0], zRots[0]));
			//temp.invert();

			temp.setEuler(new Vector3d(Math.PI, -Math.PI / 2f, 0));

			//temp.setEuler(new Vector3d( 0,Math.PI,0));
			//I MA HERE this is right but a translate looks to be missing? the right rotate?
			// maybe the parent should be included?
			// ok allowed all up to pelvis, but maybe my translates are missing, not a bad rotate?
			// I see 3 bones inside brahmin on top of each other, all rotating
			Quat4f rot = new Quat4f();
			Utils3D.safeGetQuat(temp, rot);

			System.out.println("fx rot " + rot);
			System.out.println("fx ypr " + Utils3D.toStringQuat(rot));

			// below here dumps my quarter turn around y!!
			Transform3D targetTransform = new Transform3D();

			targetTransform.set(rot);

			Quat4f q2 = new Quat4f();
			Utils3D.safeGetQuat(targetTransform, q2);
			System.out.println("fx q2 " + q2);
			System.out.println("fx q2 = ypr " + Utils3D.toStringQuat(q2));

			System.out.println("fx targetTransform " + targetTransform);
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

	@Override
	public void applyTransform(Transform3D targetTransform)
	{
		temp.setEuler(new Vector3d(interpedRot));
		Quat4f rot = new Quat4f();
		Utils3D.safeGetQuat(temp, rot);

		//System.out.println("" + this + " y " + Utils3D.toStringQuat(rot));
		//targetTransform.setIdentity();// even this doesn't help!
		targetTransform.setRotation(rot);

		//Quat4f q2 = new Quat4f();
		//targetTransform.get(q2);
		//System.out.println("repeat = y " + Utils3D.toStringQuat(q2));
		//System.out.println("rot " + rot);
		//System.out.println("q2 " + q2);
		//System.out.println("targetTransform " + targetTransform);
	}

	public Vector3f getInterpedRot()
	{
		return interpedRot;
	}

	public void addInterpedRot(Vector3f mod)
	{
		this.interpedRot.add(mod);
	}

}
