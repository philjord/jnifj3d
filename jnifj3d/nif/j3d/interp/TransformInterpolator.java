package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * This is a  copy of the TransformInterpolator from j3d, with target swapped out for a multiple targets and axis dumped
 * @author Administrator
 *
 */

public abstract class TransformInterpolator implements Interpolated
{
	/**
	 * The TransformGroup node affected by this transformInterpolator
	 */
	protected TransformGroup target = null;

	private Transform3D targetTransform = new Transform3D();

	// We can't use a boolean flag since it is possible 
	// that after alpha change, this procedure only run
	// once at alpha.finish(). So the best way is to
	// detect alpha value change.
	private float prevAlphaValue = Float.NaN;

	public TransformInterpolator(TransformGroup target)
	{
		this.target = target;
	}

	public abstract void computeTransform(float alphaValue);

	public abstract void applyTransform(Transform3D t);

	@Override
	public void process(float alphaValue)
	{
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
