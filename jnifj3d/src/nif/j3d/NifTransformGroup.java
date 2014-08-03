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

		
		//tODO: test speed improvement!!!
		//TODO: this is still expensive, think about making a generic fast version like SKinDAta?
		// the isAffine call seems pricey, but I'm ALWAYS affine
		//t.mul(transformCache);

		double[] mat = new double[16];
		t.get(mat);
		double[] t1Mat = new double[16];
		transformCache.get(t1Mat);
		mul(mat, t1Mat);
		t.set(mat);
	}

	/**
	 * Assume BOTH are Affine!!
	 * @param mat
	 * @param t1Mat
	 */
	//TODO: put in utils somewhere
	public final void mul(double[] mat, double[] t1Mat)
	{
		double tmp0, tmp1, tmp2, tmp3;
		double tmp4, tmp5, tmp6, tmp7;
		double tmp8, tmp9, tmp10, tmp11;

		tmp0 = mat[0] * t1Mat[0] + mat[1] * t1Mat[4] + mat[2] * t1Mat[8];
		tmp1 = mat[0] * t1Mat[1] + mat[1] * t1Mat[5] + mat[2] * t1Mat[9];
		tmp2 = mat[0] * t1Mat[2] + mat[1] * t1Mat[6] + mat[2] * t1Mat[10];
		tmp3 = mat[0] * t1Mat[3] + mat[1] * t1Mat[7] + mat[2] * t1Mat[11] + mat[3];
		tmp4 = mat[4] * t1Mat[0] + mat[5] * t1Mat[4] + mat[6] * t1Mat[8];
		tmp5 = mat[4] * t1Mat[1] + mat[5] * t1Mat[5] + mat[6] * t1Mat[9];
		tmp6 = mat[4] * t1Mat[2] + mat[5] * t1Mat[6] + mat[6] * t1Mat[10];
		tmp7 = mat[4] * t1Mat[3] + mat[5] * t1Mat[7] + mat[6] * t1Mat[11] + mat[7];
		tmp8 = mat[8] * t1Mat[0] + mat[9] * t1Mat[4] + mat[10] * t1Mat[8];
		tmp9 = mat[8] * t1Mat[1] + mat[9] * t1Mat[5] + mat[10] * t1Mat[9];
		tmp10 = mat[8] * t1Mat[2] + mat[9] * t1Mat[6] + mat[10] * t1Mat[10];
		tmp11 = mat[8] * t1Mat[3] + mat[9] * t1Mat[7] + mat[10] * t1Mat[11] + mat[11];

		mat[12] = mat[13] = mat[14] = 0;
		mat[15] = 1;

		mat[0] = tmp0;
		mat[1] = tmp1;
		mat[2] = tmp2;
		mat[3] = tmp3;
		mat[4] = tmp4;
		mat[5] = tmp5;
		mat[6] = tmp6;
		mat[7] = tmp7;
		mat[8] = tmp8;
		mat[9] = tmp9;
		mat[10] = tmp10;
		mat[11] = tmp11;

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
