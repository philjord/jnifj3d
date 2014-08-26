package nif.j3d.interp;

import javax.media.j3d.Transform3D;

import nif.j3d.NifTransformGroup;

/**
 * This is a  copy of the TransformInterpolator from j3d
 * the alpha for processing is an offsetted alpha over a durations in seconds
 * but setting start to 0 and length to 1 gives a normalized range again
 * @author Administrator
 *
 */

public abstract class TransformInterpolator implements Interpolated
{
	protected boolean fixed = false;

	private Object owner = null;

	/**
	 * The NifTransformGroups node affected by this transformInterpolator
	 */
	protected NifTransformGroup target = null;

	protected float startTimeS = 0;

	protected float lengthS = 1;

	protected Transform3D targetTransform = new Transform3D();

	protected Transform3D prevTargetTransform = new Transform3D();

	// We can't use a boolean flag since it is possible 
	// that after alpha change, this procedure only run
	// once at alpha.finish(). So the best way is to
	// detect alpha value change.
	protected float prevAlphaValue = Float.NaN;

	public TransformInterpolator(NifTransformGroup target, float startTimeS, float lengthS)
	{
		this.target = target;
		this.startTimeS = startTimeS;
		this.lengthS = lengthS;
	}

	public abstract void computeTransform(float alphaValue);

	public abstract void applyTransform(Transform3D t);

	//Skyrim female/weaponadjustment has a bad left thigh in it (like a reversed rotate!) but matches bone, maybe bone is reversed?

	//do I need the multiple interps over a transform?
	//more than one can point to it, but not more than one can be animating it at once!

	@Override
	public void process(float alphaValue)
	{
		// convert to an offsetted time in seconds
		alphaValue *= lengthS;
		alphaValue += startTimeS;

		if (alphaValue != prevAlphaValue)
		{
			computeTransform(alphaValue);

			applyTransform(targetTransform);

			if (!isAffine(targetTransform))
			{
				//TODO: this is expensive I wager take out if no no affines
				System.out.println("this bummed it up " + this);
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
		Float.isNaN(matrix[0]);
		boolean byPrim = (matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1);
		boolean byMeth = ((t.getType() & Transform3D.AFFINE) != 0);

		if (byPrim != byMeth)
			System.out.println("differs? " + t);
		return byMeth;
	}

	public Object getOwner()
	{
		return owner;
	}

	public void setOwner(Object owner)
	{
		this.owner = owner;
	}
}
