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

	// this is turned on if transformMul is called at all ( from the getTreeTransformImpl)
	public Transform3D transformCache;

	public void setTransform(Transform3D t1)
	{
		if (transformCache != null)
			transformCache.set(t1);

		super.setTransform(t1);
	}

	public void transformMul(Transform3D t)
	{
		if (transformCache == null)
		{
			transformCache = new Transform3D();
			this.getTransform(transformCache);
		}

		t.mul(transformCache);
	}

	private Transform3D temp2;

	public boolean isNoImpact()
	{
		if (temp2 == null)
			temp2 = new Transform3D();
		this.getTransform(temp2);
		return temp2.equals(IDENTITY) && !this.getCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	}

}
