package nif.j3d.particles;

import nif.compound.NifVector4;

public class AtlasAnimatedTexture
{
	//NOTE no texture name, that's part of the appearance, 
	// this class only cares about uv coords into texture
	private int uCount = -1;

	private int vCount = -1;

	private int subImageCount = 1;

	public AtlasAnimatedTexture(int subImageCount)
	{
		this.subImageCount = subImageCount;

		uCount = 1;
		vCount = 1;
		if (subImageCount > 1)
		{
			float sq = (float) Math.sqrt(subImageCount);

			uCount = (int) Math.pow(2, (Math.floor(Math.log(sq) / Math.log(2))));
			vCount = (int) Math.pow(2, (Math.ceil(Math.log(sq) / Math.log(2))));
		}

	}

	public AtlasAnimatedTexture(int NumSubtextureOffsetUVs, float AspectRatio, NifVector4[] SubtextureOffsetUVs)
	{

	}

	public void getUVCoords(float[] gaTexCoords, int indx, int subImage)
	{
		if (uCount != -1)
		{
			float uStride = (1f / uCount);
			float vStride = (1f / vCount);

			float uStart = subImage % uCount;
			float vStart = subImage / uCount;

			gaTexCoords[indx * 4 * 2 + 0] = uStart;
			gaTexCoords[indx * 4 * 2 + 1] = vStart;
			gaTexCoords[indx * 4 * 2 + 2] = uStart + uStride;
			gaTexCoords[indx * 4 * 2 + 3] = vStart;
			gaTexCoords[indx * 4 * 2 + 4] = uStart + uStride;
			gaTexCoords[indx * 4 * 2 + 5] = vStart + vStride;
			gaTexCoords[indx * 4 * 2 + 6] = uStart;
			gaTexCoords[indx * 4 * 2 + 7] = vStart + vStride;
		}
		else
		{

		}
	}

	public int getSubImageCount()
	{
		return subImageCount;
	}
}
