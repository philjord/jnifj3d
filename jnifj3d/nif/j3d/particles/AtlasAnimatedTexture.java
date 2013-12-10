package nif.j3d.particles;

import nif.compound.NifVector4;

public class AtlasAnimatedTexture
{
	//NOTE no texture name, that's part of the appearance, 
	// this class only cares about uv coords into texture
	private int uCount = 1;

	private int vCount = 1;

	private int subImageCount = 1;

	public AtlasAnimatedTexture(int subImageCount)
	{
		this.subImageCount = subImageCount;
		if (subImageCount > 1)
		{
			float sq = (float) Math.sqrt(subImageCount);

			uCount = (int) Math.pow(2, (Math.floor(Math.log(sq) / Math.log(2))));
			vCount = (int) Math.pow(2, (Math.ceil(Math.log(sq) / Math.log(2))));
		}

		float uStride = (1f / uCount);
		float vStride = (1f / vCount);
	}

	public AtlasAnimatedTexture(int NumSubtextureOffsetUVs, float AspectRatio, NifVector4[] SubtextureOffsetUVs)
	{

	}

}
