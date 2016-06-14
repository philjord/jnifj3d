package nif.j3d.particles.tes3;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedPointArray;
import javax.vecmath.Vector3f;

import nif.niobject.particle.NiParticlesData;

public class J3dNiParticlesData
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

	public float[] particleTranslation;

	public float[] particleRotationAngle;

	public float[] particleRotationSpeed;

	public float[] particleVelocity;

	//public int[] particleImageIds;

	private IndexedPointArray ga;

	private int gaVertexCount;

	private float[] gaTexCoords;

	private int[] gaCoordIndices; //fixed at intitialization, DO NOT ALTER	

	private float[] gaCoords;

	private float[] gaColors;

	protected NiParticlesData niParticlesData = null;

	//private FloatBuffer sizes;
	private float[] sizesF;

	private float[] rotationsF;

	//https://www.opengl.org/discussion_boards/showthread.php/166796-GLSL-PointSprites-different-sizes

	/**
	 * NOTE!!!! all calls to this class must be in a GeomteryUpdater only. And violently single threaded
	 * 
	 * @param niPSysData
	 */
	public J3dNiParticlesData(NiParticlesData niParticlesData)
	{
		//Particles are points

		//numVertices
		//hasVertices
		//vertices
		// normals = no
		// center
		//radius
		// vertex colors
		// no uv
		//numParticles
		//particlesRadius;
		//numAcitve
		// has sizes
		//sizes

		this.niParticlesData = niParticlesData;

		maxParticleCount = niParticlesData.numVertices;
		gaVertexCount = maxParticleCount;

		ga = new IndexedPointArray(gaVertexCount,
				GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 //
						| (niParticlesData.hasVertexColors ? GeometryArray.COLOR_4 : 0) | GeometryArray.BY_REFERENCE_INDICES
						| GeometryArray.USE_COORD_INDEX_ONLY | GeometryArray.VERTEX_ATTRIBUTES,
				1, new int[] { 0 }, 2, new int[] { 1, 1 }, gaVertexCount);

		ga.setName("Particles System");

		ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		ga.setCapability(GeometryArray.ALLOW_COUNT_WRITE);

		gaCoords = new float[gaVertexCount * 3];
		gaTexCoords = new float[gaVertexCount * 2];
		gaCoordIndices = new int[gaVertexCount];

		particleSpawnTime = new long[maxParticleCount * 1];
		particleAge = new long[maxParticleCount * 1];
		particleLifeSpan = new long[maxParticleCount * 1];
		particleGeneration = new int[maxParticleCount * 1];
		particleRadius = new float[maxParticleCount * 1];
		particleTranslation = new float[maxParticleCount * translationStride];
		particleRotationAngle = new float[maxParticleCount * 1];
		particleRotationSpeed = new float[maxParticleCount * 1];
		particleVelocity = new float[maxParticleCount * velocityStride];
		//particleImageIds = new int[maxParticleCount * 1];

		if (niParticlesData.hasVertexColors)
		{
			particleColors = new float[maxParticleCount * colorStride];
			gaColors = new float[gaVertexCount * 4];
		}

		//fixed for all time
		for (int i = 0; i < gaVertexCount; i++)
		{
			gaCoordIndices[i] = i;
		}

		ga.setCoordRefFloat(gaCoords);
		ga.setTexCoordRefFloat(0, gaTexCoords);
		ga.setCoordIndicesRef(gaCoordIndices);

		if (niParticlesData.hasVertexColors)
			ga.setColorRefFloat(gaColors);

		//ByteBuffer bb = ByteBuffer.allocateDirect(maxParticleCount);
		//bb.order(ByteOrder.nativeOrder());
		//sizes = bb.asFloatBuffer();
		sizesF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(0, sizesF);
		rotationsF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(1, rotationsF);
		ga.setCapability(GeometryArray.ALLOW_VERTEX_ATTR_WRITE);

		activeParticleCount = niParticlesData.numActive;

		setupInitialParticles();

	}

	protected void setupInitialParticles()
	{
		// I have various initial values in data, then I need the controller to add some more

		for (int indx = 0; indx < maxParticleCount; indx++)
		{
			//NOTICE optimization
			//NifVector3 nv = niAutoNormalParticlesData.vertices[indx];
			//Vector3f v = ConvertFromNif.toJ3d(nv);
			Vector3f v = new Vector3f(niParticlesData.verticesOptBuf.get(indx * 3 + 0), //
					niParticlesData.verticesOptBuf.get(indx * 3 + 1), //
					niParticlesData.verticesOptBuf.get(indx * 3 + 2));

			particleTranslation[indx * 3 + 0] = v.x;
			particleTranslation[indx * 3 + 1] = v.y;
			particleTranslation[indx * 3 + 2] = v.z;
		}

		recalcAllGaCoords();

		if (niParticlesData.hasVertexColors)
		{
			for (int indx = 0; indx < maxParticleCount; indx++)
			{
				//NOTICE optimization
				//NifColor4 nc = niAutoNormalParticlesData.vertexColors[indx];

				particleColors[indx * 4 + 0] = niParticlesData.vertexColorsOptBuf.get(indx * 4 + 0);
				particleColors[indx * 4 + 1] = niParticlesData.vertexColorsOptBuf.get(indx * 4 + 1);
				particleColors[indx * 4 + 2] = niParticlesData.vertexColorsOptBuf.get(indx * 4 + 2);
				particleColors[indx * 4 + 3] = niParticlesData.vertexColorsOptBuf.get(indx * 4 + 3);
			}

			resetAllGaColors();
		}

		for (int indx = 0; indx < maxParticleCount; indx++)
		{
			float s = 1f;
			if (niParticlesData.hasSizes)
				s = niParticlesData.sizes[indx];
			particleRadius[indx] = s * niParticlesData.particlesRadius;
		}
		recalcSizes();

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

			if (niParticlesData.hasVertexColors)
				shiftArray(gaColors, indx, gaColorStride, partsToMove);

			shiftArray(gaTexCoords, indx, gaTexCoordStride, partsToMove);

			if (niParticlesData.hasVertexColors)
				shiftArray(particleColors, indx, colorStride, partsToMove);

			shiftArray(particleSpawnTime, indx, 1, partsToMove);

			shiftArray(particleAge, indx, 1, partsToMove);

			shiftArray(particleLifeSpan, indx, 1, partsToMove);

			shiftArray(particleGeneration, indx, 1, partsToMove);

			shiftArray(particleRadius, indx, 1, partsToMove);

			shiftArray(particleTranslation, indx, translationStride, partsToMove);

			shiftArray(particleRotationAngle, indx, 1, partsToMove);

			shiftArray(particleRotationSpeed, indx, 1, partsToMove);

			shiftArray(particleVelocity, indx, velocityStride, partsToMove);

			//shiftArray(particleImageIds, indx, 1, partsToMove);

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
	public int addActive(float radius, long lifeSpan, int generation, float x, float y, float z, float r, float g, float b, float a,
			float velx, float vely, float velz)
	{

		if (activeParticleCount < maxParticleCount)
		{
			int indx = activeParticleCount;
			// odd only 1-40 are first then 56-59 are being used as active??
			System.out.println("new particle = " + indx);

			particleSpawnTime[indx] = System.currentTimeMillis();

			particleAge[indx] = 0;

			particleLifeSpan[indx] = lifeSpan;

			particleGeneration[indx] = generation;

			particleRadius[indx] = radius;
			sizesF[indx] = particleRadius[indx] * 2; // * 2 because it's a radius

			particleTranslation[indx * 3 + 0] = x;
			particleTranslation[indx * 3 + 1] = y;
			particleTranslation[indx * 3 + 2] = z;
			gaCoords[indx * 3 + 0] = x;
			gaCoords[indx * 3 + 1] = y;
			gaCoords[indx * 3 + 2] = z;

			particleRotationAngle[indx] = 0f;
			rotationsF[indx] = particleRotationAngle[indx];

			particleVelocity[indx * 3 + 0] = velx;
			particleVelocity[indx * 3 + 1] = vely;
			particleVelocity[indx * 3 + 2] = velz;

			if (niParticlesData.hasVertexColors)
			{
				particleColors[indx * 4 + 0] = r;
				particleColors[indx * 4 + 1] = g;
				particleColors[indx * 4 + 2] = b;
				particleColors[indx * 4 + 3] = a;
				gaColors[indx * 4 + 0] = r;
				gaColors[indx * 4 + 1] = g;
				gaColors[indx * 4 + 2] = b;
				gaColors[indx * 4 + 3] = a;
			}

			initTexCoords(indx);

			activeParticleCount++;
			ga.setValidIndexCount(activeParticleCount);
			return indx;
		}

		return -1;
	}

	private void initTexCoords(int indx)
	{

		//TODO: looks like there is no atlas system for particles
		//there is no UV at all! so I can just use the point xy
		//divided by size as uv into texture in frag

		// simply fixed
		gaTexCoords[indx * 2 + 0] = 0.5f;
		gaTexCoords[indx * 2 + 1] = 0.5f;

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

	public void recalcSizes()
	{
		for (int indx = 0; indx < activeParticleCount; indx++)
		{
			//sizes.put(indx, particleRadius[indx]);
			sizesF[indx] = particleRadius[indx] * 2; // *2 because it's a radius
		}
	}

	public void recalcRotations()
	{
		for (int indx = 0; indx < activeParticleCount; indx++)
		{
			//sizes.put(indx, particleRadius[indx]);
			rotationsF[indx] = particleRotationAngle[indx];
		}
	}

	/**
	 * If theparticleColors array is altered this must be called per each altered entry
	 * @param particle
	 */
	public void resetAllGaColors()
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
