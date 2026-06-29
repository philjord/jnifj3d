package nif.j3d.particles;

import nif.compound.NifVector4;

public class AtlasAnimatedTexture {
	//NOTE no texture name, that's part of the appearance, 
	// this class only cares about uv coords into texture
	private int				uCount			= -1;

	private int				vCount			= -1;

	private int				subImageCount	= 1;

	private NifVector4[]	uvQuadrants;

	private float			aspectRatio;

	private NifVector4[]	subtextureOffsetUVs;

	public AtlasAnimatedTexture(int subImageCount) {
		this.subImageCount = subImageCount;

		uCount = 1;
		vCount = 1;
		if (subImageCount > 1) {
			float sq = (float)Math.sqrt(subImageCount);

			uCount = (int)Math.pow(2, (Math.floor(Math.log(sq) / Math.log(2))));
			vCount = (int)Math.pow(2, (Math.ceil(Math.log(sq) / Math.log(2))));
		}

	}

	public AtlasAnimatedTexture(int subImageCount, NifVector4[] UVQuadrants) {
		this.subImageCount = subImageCount;
		this.uvQuadrants = UVQuadrants;
	}

	public AtlasAnimatedTexture(float AspectRatio, NifVector4[] SubtextureOffsetUVs) {
		this.aspectRatio = AspectRatio;
		this.subtextureOffsetUVs = SubtextureOffsetUVs;
		this.subImageCount = subtextureOffsetUVs.length;
	}

	public void getUVCoords(float[] gaTexCoords, float[] gaVsubTextureSizeF, int indx, int subImage) {
		if (subImage < 0 || subImage >= subImageCount)
			throw new RuntimeException("subImage requested " + subImage + " out of range 0-" + subImageCount);

		float uStart = 0;
		float vStart = 0;

		float uStride = 1;
		float vStride = 1;

		if (uvQuadrants != null) {
			//TODO: why is fallout3 ashpile01 always going for subImage==0
			NifVector4 uv = uvQuadrants [subImage];

			uStart = uv.x;
			uStride = uv.y;
			vStart = uv.z;
			vStride = uv.w;
		} else if (subtextureOffsetUVs != null) {
			// 4x2 images image is 1024 by 512 so sizes are fixed to square
			//x is uStart, y is uStride, 
			//z is vStart, w is vStride 
			//or swap u and v
			
			//FIXME:
			//aspectRatio of 0.9 in  textures\effects\FXFireAtlas02.dds looks to be 0.9 as wide as tall
			// so I should do some squeeze up on x and y? or maybe in fact widen them out?

			NifVector4 uv = subtextureOffsetUVs [subImage];
			uStart = uv.x;
			uStride = uv.y;
			vStart = uv.z;
			vStride = uv.w;
		} else if (uCount != -1) {
			uStride = (1f / uCount);
			vStride = (1f / vCount);

			uStart = subImage % uCount;
			vStart = subImage / uCount;

		} else {
			throw new RuntimeException("Not sure how this atlas works?" + this);
		}

		gaTexCoords [indx * 2 + 0] = uStart;
		gaTexCoords [indx * 2 + 1] = vStart;

		gaVsubTextureSizeF [indx * 2 + 0] = uStride;
		gaVsubTextureSizeF [indx * 2 + 1] = vStride;

		
		//System.out.println("subImage " + subImage + " uStart " + uStart+ " vStart " + vStart+ " uStride " + uStride+ " vStride " + vStride);
		
	}

	public int getSubImageCount() {
		return subImageCount;
	}
}
