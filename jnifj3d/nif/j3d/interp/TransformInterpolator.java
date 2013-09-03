package nif.j3d.interp;

import java.util.Enumeration;

import javax.media.j3d.Alpha;
import javax.media.j3d.Interpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnElapsedFrames;

/**
 * This si a  copy of the TransformInterpolator from j3d, with target swapped out for a multiple targets and axis dumped
 * @author Administrator
 *
 */

public abstract class TransformInterpolator extends Interpolator
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

	private WakeupCriterion passiveWakeupCriterion = new WakeupOnElapsedFrames(0, true);

	public TransformInterpolator(Alpha alpha, TransformGroup target)
	{
		super(alpha);
		this.target = target;
	}

	
	public abstract void computeTransform(float alphaValue);

	public abstract void applyTransform(Transform3D t);

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration criteria)
	{
		// Handle stimulus
		WakeupCriterion criterion = passiveWakeupCriterion;

		if (getAlpha() != null)
		{
			float value = getAlpha().value();
			if (value != prevAlphaValue)
			{
				computeTransform(value);

				target.getTransform(targetTransform);
				applyTransform(targetTransform);
				target.setTransform(targetTransform);				

				prevAlphaValue = value;
			}
			if (!getAlpha().finished() && !getAlpha().isPaused())
			{
				criterion = defaultWakeupCriterion;
			}

		}

		wakeupOn(criterion);
	}

}
