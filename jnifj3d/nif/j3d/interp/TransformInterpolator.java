package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * This is a  copy of the TransformInterpolator from j3d, with target swapped out for a multiple targets and axis dumped
 * AND!!! the alpha for processing is an offsetted alpha over a durations in seconds
 * but setting start to 0 and length to 1 gives a normalized range again
 * @author Administrator
 *
 */

public abstract class TransformInterpolator implements Interpolated
{
	protected boolean fixed = false;

	/**
	 * The TransformGroup node affected by this transformInterpolator
	 */
	protected TransformGroup target = null;

	protected float startTimeS = 0;

	protected float lengthS = 1;

	private Transform3D targetTransform = new Transform3D();

	// We can't use a boolean flag since it is possible 
	// that after alpha change, this procedure only run
	// once at alpha.finish(). So the best way is to
	// detect alpha value change.
	private float prevAlphaValue = Float.NaN;

	public TransformInterpolator(TransformGroup target, float startTimeS, float lengthS)
	{
		this.target = target;
		this.startTimeS = startTimeS;
		this.lengthS = lengthS;
	}

	public abstract void computeTransform(float alphaValue);

	public abstract void applyTransform(Transform3D t);

	@Override
	public void process(float alphaValue)
	{
		// convert to an offsetted time in seconds
		alphaValue *= lengthS;
		alphaValue += startTimeS;
		
		if (alphaValue != prevAlphaValue)
		{
			computeTransform(alphaValue);

			target.getTransform(targetTransform);
			applyTransform(targetTransform);
			target.setTransform(targetTransform);

			prevAlphaValue = alphaValue;
		}
	}

}
