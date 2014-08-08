package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import nif.j3d.NifTransformGroup;
import nif.j3d.animation.interp.J3dNiTransformInterpolator;
import nif.niobject.interpolator.NiTransformInterpolator;

public class RotPosScaleInterpolator extends TransformInterpolator
{
	private PositionPathInterpolator positionPathInterpolator;

	private ScalePathInterpolator scalePathInterpolator;

	private XYZRotPathInterpolator xYZRotPathInterpolator;

	private RotationPathInterpolator quatRotInterpolator;

	private Vector3f defaultTrans = new Vector3f();

	private Quat4f defaultRot = new Quat4f(0f, 0f, 0f, 1f);

	private float defaultScale = 1;

	public RotPosScaleInterpolator(NifTransformGroup target, float startTimeS, float lengthS,
			PositionPathInterpolator positionPathInterpolator, ScalePathInterpolator scalePathInterpolator,
			XYZRotPathInterpolator xYZRotPathInterpolator, RotationPathInterpolator quatRotInterpolator, Vector3f defaultTrans,
			Quat4f defaultRot, float defaultScale)
	{
		super(target, startTimeS, lengthS);
		this.positionPathInterpolator = positionPathInterpolator;

		this.scalePathInterpolator = scalePathInterpolator;

		this.xYZRotPathInterpolator = xYZRotPathInterpolator;

		this.quatRotInterpolator = quatRotInterpolator;

		this.defaultTrans = defaultTrans;

		this.defaultRot = defaultRot;

		this.defaultScale = defaultScale;
	}

	/**
	 * Method overrride as we have 3 elements to update and the rotation can be one of 2 types
	 * @see nif.j3d.animation.interp.J3dNiInterpolator#process(float)
	 */
	@Override
	public void process(float alphaValue)
	{
		// convert to an offsetted time in seconds
		alphaValue *= lengthS;
		alphaValue += startTimeS;

		//FIXME: remove check to make sure nothing else is touching my target
		//TODO: this isn't working somehow I just had 2 different thing changing a target with owner Bip01 Sack
		// skeleton is getting a chance too, but it has only it's identiy transform to apply
		// presumably it applie it first in the update thread then the actual one overrides, 
		// only sack has a transcontroller in skeleton, plus the other ones that are stuffed too by the look

		// So my thinking is that the skeleton transform must be done first, then the animation transform on top of that.
		// So I wonder how I can apply that as there is no point currently that resets the transform
		
		// I think my problem is I have 2 transforms for this one node, and I NEED both, but in other cases I only need the 
		// one unique one to set things absolutely
		// to have both I need to multiply them together, which menas I need to know what teh skeletona and what's not!!
		// but it's more complex still becuase I need to know if a skeleton has altered my value before me and keep the alteration!
		// so I could put a skeleton trans into a cache on the target and then mul in if I find it with a normal trans later??  

		Object inter = ts.get(target);
		if (inter != null && inter != this)
		{
			//System.out.println("inter " + inter + " this " + this);
		}
		ts.put(target, this);

		if (alphaValue != prevAlphaValue)
		{
			if (target.getOwner().getName().equals("Bip01 Sack"))
			{
				System.out.println("spotted " + ((NiTransformInterpolator) ((J3dNiTransformInterpolator) this.getOwner()).getOwner()).nVer);
				System.out.println(""+((NiTransformInterpolator) ((J3dNiTransformInterpolator) this.getOwner()).getOwner()).nVer.fileName.contains("skeleton"));
			}			
			 
			

			if (xYZRotPathInterpolator != null)
			{
				xYZRotPathInterpolator.computeTransform(alphaValue);
				xYZRotPathInterpolator.applyTransform(targetTransform);
			}
			else if (quatRotInterpolator != null)
			{
				quatRotInterpolator.computeTransform(alphaValue);
				quatRotInterpolator.applyTransform(targetTransform);
			}
			else
			{
				targetTransform.setRotation(defaultRot);
			}

			if (positionPathInterpolator != null)
			{
				positionPathInterpolator.computeTransform(alphaValue);
				positionPathInterpolator.applyTransform(targetTransform);
			}
			else
			{
				targetTransform.setTranslation(defaultTrans);
			}

			if (scalePathInterpolator != null)
			{
				scalePathInterpolator.computeTransform(alphaValue);
				scalePathInterpolator.applyTransform(targetTransform);
			}
			else
			{
				targetTransform.setScale(defaultScale);
			}

			//**** handle extra skeleton transform that sometimes appears
			if (((NiTransformInterpolator) ((J3dNiTransformInterpolator) this.getOwner()).getOwner()).nVer.fileName.contains("skeleton"))
			{
				if(target.skeletonTrans==null)
				{
				 target.skeletonTrans = new Transform3D();
				}
				 target.skeletonTrans.set(targetTransform);
			}
			else
			{
				if(target.skeletonTrans!=null)
				{
					targetTransform.mul(  target.skeletonTrans, targetTransform);
					 target.skeletonTrans.setIdentity();
				}
			}
			//******* Nope! skeleton animations are running at half speed!
			// what I need to to see if the skeleton transfomr exists then use the base rotr for teh node

			if (!isAffine(targetTransform))
			{
				System.out.println("rps this bummed it up " + this);
			}

			//only set on a change
			if (!targetTransform.equals(prevTargetTransform))
			{
				target.setTransform(targetTransform);
				prevTargetTransform.set(targetTransform);
			}

			prevAlphaValue = alphaValue;
		}

	}

	private static boolean isAffine(Transform3D t)
	{
		float[] matrix = new float[16];
		t.get(matrix);
		boolean hasNAN = false;
		for (int i = 0; i < 16; i++)
			hasNAN = hasNAN || Float.isNaN(matrix[i]);
		boolean byPrim = (matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1);
		boolean byMeth = ((t.getType() & Transform3D.AFFINE) != 0);

		if (hasNAN)
			System.out.println("hasNAN");
		if (byPrim != byMeth)
			System.out.println("differs? " + t);
		return byMeth;
	}

	@Override
	public void computeTransform(float alphaValue)
	{
		//dummy as process does it special
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyTransform(Transform3D t)
	{
		//dummy as process does it special		
		throw new UnsupportedOperationException();
	}
}
