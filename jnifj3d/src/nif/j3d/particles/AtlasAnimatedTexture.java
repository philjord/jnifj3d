package nif.j3d.particles;

import nif.compound.NifVector4;

public class AtlasAnimatedTexture
{
	//NOTE no texture name, that's part of the appearance, 
	// this class only cares about uv coords into texture
	private int uCount = -1;

	private int vCount = -1;

	private int subImageCount = 1;

	private float aspectRatio;

	private NifVector4[] subtextureOffsetUVs;

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

	public AtlasAnimatedTexture(float AspectRatio, NifVector4[] SubtextureOffsetUVs)
	{
		this.aspectRatio = AspectRatio;
		this.subtextureOffsetUVs = SubtextureOffsetUVs;
		this.subImageCount = subtextureOffsetUVs.length;
	}

	public void getUVCoords(float[] gaTexCoords, int indx, int subImage)  
	{
		if (subImage < 0 || subImage >= subImageCount)
			throw new RuntimeException("subImage requested " + subImage + " out of range 0-" + subImageCount);

		float uStride = 1;
		float vStride = 1;

		float uStart = 0;
		float vStart = 0;

		if (uCount != -1)
		{
			uStride = (1f / uCount);
			vStride = (1f / vCount);

			uStart = subImage % uCount;
			vStart = subImage / uCount;

		}
		else
		{
			// looks like x and z are u,v stride (1/8 and 1/4) for 8 across 4 down
			// image is 1024 by 512 so sizes are fixed to square
			// so start from y and w-x then take z and x worths (hope 0-0.125) is correct?

			NifVector4 uv = subtextureOffsetUVs[subImage];
			uStride = uv.x;
			vStride = uv.z;

			uStart = uv.w - uv.x;
			vStart = uv.y;

		}

		gaTexCoords[indx * 4 * 2 + 0] = uStart;
		gaTexCoords[indx * 4 * 2 + 1] = vStart;
		gaTexCoords[indx * 4 * 2 + 2] = uStart + uStride;
		gaTexCoords[indx * 4 * 2 + 3] = vStart;
		gaTexCoords[indx * 4 * 2 + 4] = uStart + uStride;
		gaTexCoords[indx * 4 * 2 + 5] = vStart + vStride;
		gaTexCoords[indx * 4 * 2 + 6] = uStart;
		gaTexCoords[indx * 4 * 2 + 7] = vStart + vStride;
	}

	public int getSubImageCount()
	{
		return subImageCount;
	}
}
