package nif.j3d.animation.j3dinterp.interp;


/**
 * Called by some controlling behavior handing in an alpha value 
 * for this interpolator to do it's work
 * @author philip
 *
 */
public interface Interpolated
{
	public void process(float alphaValue);
}
