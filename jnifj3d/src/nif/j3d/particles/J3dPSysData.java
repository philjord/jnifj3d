package nif.j3d.particles;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.IndexedPointArray;

import nif.niobject.particle.NiPSysData;

public class J3dPSysData
{
	public static int gaCoordStride = 3;

	public static int gaColorStride = 4;

	public static int gaTexCoordStride = 2;

	public static int translationStride = 3;

	public static int velocityStride = 3;

	public static int colorStride = 4;

	public int maxParticleCount;

	public int activeParticleCount;

	public float[] particleColors;

	public long[] particleSpawnTime;

	public long[] particleAge;

	public long[] particleLifeSpan;

	public int[] particleGeneration;

	public float[] particleRadius;
	
	public float[] particleSize;

	public float[] particleTranslation;

	public float[] particleRotationAngle;

	public float[] particleRotationSpeed;

	public float[] particleVelocity;

	public int[] particleImageIds;

	private IndexedPointArray ga;

	private int gaVertexCount;

	private float[] gaTexCoords;

	private int[] gaCoordIndices; //fixed at initialization, DO NOT ALTER	

	private float[] gaCoords;

	private float[] gaColors;
	
	private float[] gaVsizesF;

	private float[] gaVrotationsF;
	
	private float[] gaVsubTextureSizeF;

	private NiPSysData niPSysData = null;

	public AtlasAnimatedTexture atlasAnimatedTexture;

	//https://www.opengl.org/discussion_boards/showthread.php/166796-GLSL-PointSprites-different-sizes

	/**
	 * NOTE!!!! all calls to this class must be in a GeomteryUpdater only. And violently single threaded
	 * 
	 * @param niPSysData
	 */
	public J3dPSysData(NiPSysData niPSysData)
	{
		//Particles are points

		//niPSysData.hasVertices;
		//niPSysData.hasNormals;
		//niPSysData.hasRadii;
		//niPSysData.hasRotationAngles;
		//niPSysData.hasRotationAxes;
		//niPSysData.hasSizes;
		//niPSysData.hasVertexColors;
		//niPSysData.HasUVQuadrants;
		//niPSysData.NumUVQuadrants;

		this.niPSysData = niPSysData;

		maxParticleCount = Math.max(niPSysData.BSMaxVertices, niPSysData.numVertices);
		gaVertexCount = maxParticleCount;// 1 vertices per particle, this is PointArrays

		ga = new IndexedPointArray(gaVertexCount,
				GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.COLOR_4
						| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY | GeometryArray.VERTEX_ATTRIBUTES,
						1, new int[] { 0 }, 3, new int[] { 1, 1, 2 }, gaVertexCount);

		ga.setName("Particles System");

		ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		ga.setCapability(GeometryArray.ALLOW_COUNT_WRITE);

		gaCoords = new float[gaVertexCount * 3];
		gaTexCoords = new float[gaVertexCount * 2];
		gaCoordIndices = new int[gaVertexCount];
		gaColors = new float[gaVertexCount * 4];// alpha included

		particleColors = new float[maxParticleCount * colorStride];
		particleSpawnTime = new long[maxParticleCount * 1];
		particleAge = new long[maxParticleCount * 1];
		particleLifeSpan = new long[maxParticleCount * 1];
		particleGeneration = new int[maxParticleCount * 1];
		particleRadius = new float[maxParticleCount * 1];
		particleSize = new float[maxParticleCount * 1];
		
		particleTranslation = new float[maxParticleCount * translationStride];
		particleRotationAngle = new float[maxParticleCount * 1];
		particleRotationSpeed = new float[maxParticleCount * 1];
		particleVelocity = new float[maxParticleCount * velocityStride];
		particleImageIds = new int[maxParticleCount * 1];
		
			
		//fixed for all time, recall these are points
		for (int i = 0; i < gaVertexCount; i++)
		{
			gaCoordIndices[i] = i;
		}
		
		ga.setCoordRefFloat(gaCoords);
		ga.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		ga.setTexCoordRefFloat(0, gaTexCoords);
		ga.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
		ga.setCoordIndicesRef(gaCoordIndices);

		if (niPSysData.hasVertexColors)
		{
			ga.setColorRefFloat(gaColors);
			ga.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		}

		//ByteBuffer bb = ByteBuffer.allocateDirect(maxParticleCount);
		//bb.order(ByteOrder.nativeOrder());
		//sizes = bb.asFloatBuffer();
		gaVsizesF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(0, gaVsizesF);
		gaVrotationsF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(1, gaVrotationsF);
		gaVsubTextureSizeF = new float[maxParticleCount*2];
		//can be modified but default to 1,1
		for (int i = 0; i < maxParticleCount; i++) {
			gaVsubTextureSizeF [i * 2 + 0] = 1f;
			gaVsubTextureSizeF [i * 2 + 1] = 1f;
		}
		ga.setVertexAttrRefFloat(2, gaVsubTextureSizeF);
		ga.setCapability(GeometryArray.ALLOW_VERTEX_ATTR_WRITE);

		// this will always be 0, otherwise the run up system gets particles on screen for the first frame
		activeParticleCount = niPSysData.numActive;

	}

	/**
	 * In activate by copying the array backwards over the slot up to the active number
	 * the inactive part of the array is left with garbage, as the valid vertex count causes this to be ignored
	 * @param indx
	 */
	public void inactivateParticle(int indx)
	{
		if (indx < activeParticleCount)
		{
			int partsToMove = (activeParticleCount - indx) - 1; //particles After Indx To Move left

			shiftArray(gaCoords, indx, gaCoordStride, partsToMove);

			shiftArray(gaColors, indx, gaColorStride, partsToMove);

			shiftArray(gaTexCoords, indx, gaTexCoordStride, partsToMove);

			shiftArray(particleColors, indx, colorStride, partsToMove);

			shiftArray(particleSpawnTime, indx, 1, partsToMove);

			shiftArray(particleAge, indx, 1, partsToMove);

			shiftArray(particleLifeSpan, indx, 1, partsToMove);

			shiftArray(particleGeneration, indx, 1, partsToMove);

			shiftArray(particleRadius, indx, 1, partsToMove);
			
			shiftArray(particleSize, indx, 1, partsToMove);

			shiftArray(particleTranslation, indx, translationStride, partsToMove);

			shiftArray(particleRotationAngle, indx, 1, partsToMove);

			shiftArray(particleRotationSpeed, indx, 1, partsToMove);

			shiftArray(particleVelocity, indx, velocityStride, partsToMove);

			shiftArray(particleImageIds, indx, 1, partsToMove);

			activeParticleCount--;
			ga.setValidIndexCount(activeParticleCount);
		}
	}

	private static void shiftArray(Object arr, int indx, int stride, int remCount)
	{
		int srcStart = indx * stride + stride;
		int destStart = indx * stride;
		int len = remCount * stride;
		System.arraycopy(arr, srcStart, arr, destStart, len);
	}

	//TODO: add many more inits and also the COPY from particle id type
	/**
	 * Always add to the end, and only adds if there is space (do nothing otherwise)	
	 * @return  the particle id of the newly created particle id
	 */
	public int addActive(float radius, float size, long lifeSpan, int generation, float x, float y, float z, float r, float g, float b, float a,
			float velx, float vely, float velz)
	{

		if (activeParticleCount < maxParticleCount)
		{
			int indx = activeParticleCount;

			particleSpawnTime[indx] = System.currentTimeMillis();

			particleAge[indx] = 0;

			particleLifeSpan[indx] = lifeSpan;

			particleGeneration[indx] = generation;

			particleRadius[indx] = radius;
			
			particleSize[indx] = size;

			particleTranslation[indx * 3 + 0] = x;
			particleTranslation[indx * 3 + 1] = y;
			particleTranslation[indx * 3 + 2] = z;

			particleRotationAngle[indx] = 0f;

			gaCoords[indx * 3 + 0] = x;
			gaCoords[indx * 3 + 1] = y;
			gaCoords[indx * 3 + 2] = z;

			particleVelocity[indx * 3 + 0] = velx;
			particleVelocity[indx * 3 + 1] = vely;
			particleVelocity[indx * 3 + 2] = velz;

			particleColors[indx * 4 + 0] = r;
			particleColors[indx * 4 + 1] = g;
			particleColors[indx * 4 + 2] = b;
			particleColors[indx * 4 + 3] = a;

			gaColors[indx * 4 + 0] = r;
			gaColors[indx * 4 + 1] = g;
			gaColors[indx * 4 + 2] = b;
			gaColors[indx * 4 + 3] = a;

			initTexCoords(indx);

			activeParticleCount++;
			ga.setValidIndexCount(activeParticleCount);
			return indx;
		}

		return -1;
	}

	private void initTexCoords(int indx) {
		//file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Reference/NiParticle/NiPSAlignedQuadGenerator.htm
		if (niPSysData.HasUVQuadrants) {
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.NumUVQuadrants, niPSysData.UVQuadrants);

			// a J3dBSPSysSubTexModifier will control this, but without it we just go random selection
			particleImageIds [indx] = (int)(Math.random() * atlasAnimatedTexture.getSubImageCount());
			atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds [indx]);
		} else if (niPSysData.HasSubtextureOffsetUVs) {
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.AspectRatio, niPSysData.SubtextureOffsetUVs);

			// a J3dBSPSysSubTexModifier will control this, but without it we just go random selection
			particleImageIds [indx] = (int)(Math.random() * atlasAnimatedTexture.getSubImageCount());
			atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds [indx]);
		} else {
			//if there is no atlas system for particles there is no real UV at all!
			// I can use the frags virtual coords as the uv, but I still need to have the start and stride as default
			if (particleImageIds [indx] == 0) {
				// simply fixed, start point is the 0,0
				gaTexCoords [indx * 2 + 0] = 0f;
				gaTexCoords [indx * 2 + 1] = 0f;
				//can be modified but default to 1,1 as the end of a texture
				gaVsubTextureSizeF [indx * 2 + 0] = 1f;
				gaVsubTextureSizeF [indx * 2 + 1] = 1f;
			}
		}
	}

	public void updateAllTexCoords()
	{
		if (niPSysData.HasUVQuadrants || niPSysData.HasSubtextureOffsetUVs)
		{
			for (int indx = 0; indx < activeParticleCount; indx++)
			{
				atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds[indx]);
			}
		}
	}

	/** if you alter the translation or rotation arrays directly this must be called
	 * 
	 */
	public void recalcAllGaCoords()
	{
		for (int i = 0; i < activeParticleCount; i++)
		{

			// with points we simply push the particles across to the gaCoords, in fact we only need agCoords

			float x = particleTranslation[i * 3 + 0];
			float y = particleTranslation[i * 3 + 1];
			float z = particleTranslation[i * 3 + 2];

			gaCoords[i * 3 + 0] = x;
			gaCoords[i * 3 + 1] = y;
			gaCoords[i * 3 + 2] = z;
		}
	}
	
	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded
	 * If particleRadius array is altered this must be called 
	 * @param particle
	 */
	public void recalcSizes()
	{
		// actual size is radius * size multiplier from grow/fade
		for(int i = 0 ; i < particleRadius.length;i++ ) {
			gaVsizesF[i] = particleRadius[i] * particleSize[i];
		}
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded
	 * If particleRotationAngle array is altered this must be called 
	 * @param particle
	 */
	public void recalcRotations()
	{
		System.arraycopy(particleRotationAngle, 0, gaVrotationsF, 0, activeParticleCount * 1);
	}
	
	/**
	 * If theparticleColors array is altered this must be called per each altered entry
	 * @param particle
	 */	
	public void recalcAllGaColors()
	{
		for (int i = 0; i < activeParticleCount; i++)
		{

			//TODO: the texture shader has a gradient color texture under grayscale texture holder
			//textures\effects\gradients\GradFlame01.dds

			float r = particleColors[i * 4 + 0];
			float g = particleColors[i * 4 + 1];
			float b = particleColors[i * 4 + 2];
			float a = particleColors[i * 4 + 3];

			gaColors[i * 4 + 0] = r;
			gaColors[i * 4 + 1] = g;
			gaColors[i * 4 + 2] = b;
			gaColors[i * 4 + 3] = a;
		}

	}
	

	public IndexedPointArray getGeometryArray()
	{
		return ga;
	}

}
