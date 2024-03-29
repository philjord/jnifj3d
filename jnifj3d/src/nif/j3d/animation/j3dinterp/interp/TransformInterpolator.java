package nif.j3d.animation.j3dinterp.interp;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;

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

	/**
	 * The NifTransformGroups node affected by this transformInterpolator
	 */
	protected TransformGroup target = null;

	protected float startTimeS = 0;

	protected float lengthS = 1;

	protected Transform3D targetTransform = new Transform3D();

	protected Transform3D prevTargetTransform = new Transform3D();

	// We can't use a boolean flag since it is possible 
	// that after alpha change, this procedure only run
	// once at alpha.finish(). So the best way is to
	// detect alpha value change.
	protected float prevAlphaValue = Float.NaN;

	public TransformInterpolator(TransformGroup target, float startTimeS, float lengthS)
	{
		this.target = target;
		this.startTimeS = startTimeS;
		this.lengthS = lengthS;
	}

	public abstract void computeTransform(float alphaValue);

	protected abstract void applyTransform(Transform3D t);

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

			//if (!isAffine(targetTransform))
			{
				//this is expensive I wager take out if no no affines
				//System.out.println("this bummed it up " + this);
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

	public static boolean isAffine(Transform3D t)
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
}
