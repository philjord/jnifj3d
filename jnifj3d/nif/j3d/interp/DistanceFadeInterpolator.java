package nif.j3d.interp;

import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Interpolator;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;

public class DistanceFadeInterpolator extends Interpolator
{
	// reused temporaries
	private Point3d viewPosition = new Point3d();

	private Transform3D xform = new Transform3D();

	private TransparencyAttributes originalTransparencyAttributes;

	private TransparencyAttributes transparencyAttributes;

	private Appearance appearance;

	private Node node;

	private float transparentDist = 100;

	private float opaqueDist = 80;

	private boolean hasOriginalTASet = true;

	private WakeupOnElapsedFrames wakeupFrame5 = new WakeupOnElapsedFrames(5, true);

	public DistanceFadeInterpolator(float transparentDist, float opaqueDist, Appearance appearance, Node node)
	{
		this.transparentDist = transparentDist;
		this.opaqueDist = opaqueDist;
		this.appearance = appearance;
		this.node = node;
		originalTransparencyAttributes = appearance.getTransparencyAttributes();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

		transparencyAttributes = new TransparencyAttributes();
		transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		transparencyAttributes.setTransparencyMode(TransparencyAttributes.NICEST);
		transparencyAttributes.setTransparency(1.0f);

	}

	public void initialize()
	{
		super.initialize();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration arg0)
	{
		// get viewplatforms's location in virutal world
		View v = this.getView();
		if (v == null)
		{
			wakeupOn(wakeupFrame5);
			return;
		}
		Canvas3D canvas = v.getCanvas3D(0);

		// get the eye point
		canvas.getCenterEyeInImagePlate(viewPosition);

		// transform the points to the Billboard's space
		canvas.getImagePlateToVworld(xform); // xform is ImagePlateToVworld

		xform.transform(viewPosition);

		// we need to use getLocalToVworld()
		// to get the localToVworld which includes the static transform

		node.getLocalToVworld(xform);

		xform.invert(); // xform is vWorldToLocal

		// transfom points to local coord sys
		xform.transform(viewPosition);

		// I wager viewPosition is the eye point in the local transforms coordinates, I wager?
		// so let's just use the length for setting the wakeup
		double dist = Math.sqrt(viewPosition.x * viewPosition.x + viewPosition.y * viewPosition.y + viewPosition.z * viewPosition.z);
		// System.out.println("dist " + dist);

		float alpha = 1.0f;
		if (dist > transparentDist)
		{
			alpha = 1.0f;
		}
		else if (dist < opaqueDist)
		{
			alpha = 0.0f;
		}
		else
		{
			alpha = ((float) dist - opaqueDist) / (transparentDist - opaqueDist);
		}

		if (alpha == 0)
		{
			if (!hasOriginalTASet)
			{
				appearance.setTransparencyAttributes(originalTransparencyAttributes);
				hasOriginalTASet = true;
			}
		}
		else
		{
			if (hasOriginalTASet)
			{
				appearance.setTransparencyAttributes(transparencyAttributes);
				hasOriginalTASet = false;
			}
			transparencyAttributes.setTransparency(alpha);
		}
		wakeupOn(wakeupFrame5);

	}
}
