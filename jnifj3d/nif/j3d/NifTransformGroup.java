package nif.j3d;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

public class NifTransformGroup extends TransformGroup
{
	private static Transform3D IDENTITY = new Transform3D();

	public NifTransformGroup()
	{
	}

	public void makeWritable()
	{
		if (!isLive() && !isCompiled())
		{
			this.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		}
	}

	public void transformMul(Transform3D t)
	{
		Transform3D temp1 = new Transform3D();
		this.getTransform(temp1);
		t.mul(temp1);
	}

	public boolean isNoImpact()
	{
		Transform3D temp2 = new Transform3D();
		this.getTransform(temp2);
		return temp2.equals(IDENTITY) && !this.getCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	}

}
