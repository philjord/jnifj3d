package nif.j3d.particles.tes3;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.IndexedPointArray;
import org.jogamp.vecmath.Vector3f;

import nif.niobject.particle.NiParticlesData;
import utils.convert.ConvertFromNif;

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

	public float particlesBaseRadius;

	public long[] particleSpawnTime;

	public long[] particleAge;

	public long[] particleLifeSpan;

	public int[] particleGeneration;

	public float[] particleRadius;

	public float[] particleTranslation;

	public float[] particleRotationAngle;

	public float[] particleRotationSpeed;

	public float[] particleVelocity;

	public float[] particleColors;

	public int[] particleImageIds;

	private IndexedPointArray ga;

	private int gaVertexCount;

	private int[] gaCoordIndices; //fixed at initialization, DO NOT ALTER	

	private float[] gaCoords;

	private float[] gaColors;

	private float[] gaTexCoords;

	//private FloatBuffer sizes;
	private float[] gaVsizesF;

	private float[] gaVrotationsF;

	protected NiParticlesData niParticlesData = null;

	public J3dNiParticlesData(NiParticlesData niParticlesData)
	{
		this.niParticlesData = niParticlesData;

		this.particlesBaseRadius = niParticlesData.particlesRadius;

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
		if (niParticlesData.hasVertexColors)
			gaColors = new float[gaVertexCount * 4];

		particleSpawnTime = new long[maxParticleCount * 1];
		particleAge = new long[maxParticleCount * 1];
		particleLifeSpan = new long[maxParticleCount * 1];
		particleGeneration = new int[maxParticleCount * 1];
		particleRadius = new float[maxParticleCount * 1];
		particleTranslation = new float[maxParticleCount * translationStride];
		particleRotationAngle = new float[maxParticleCount * 1];
		particleRotationSpeed = new float[maxParticleCount * 1];
		particleVelocity = new float[maxParticleCount * velocityStride];
		particleImageIds = new int[maxParticleCount * 1];

		if (niParticlesData.hasVertexColors)
			particleColors = new float[maxParticleCount * colorStride];

		//fixed for all time
		for (int i = 0; i < gaVertexCount; i++)
		{
			gaCoordIndices[i] = i;
		}

		ga.setCoordRefFloat(gaCoords);
		ga.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		ga.setTexCoordRefFloat(0, gaTexCoords);
		ga.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
		ga.setCoordIndicesRef(gaCoordIndices);

		if (niParticlesData.hasVertexColors)
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
		ga.setCapability(GeometryArray.ALLOW_VERTEX_ATTR_WRITE);

		activeParticleCount = niParticlesData.numActive;

		setupInitialParticles();

	}

	public void reset()
	{
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
		}

		for (int indx = 0; indx < maxParticleCount; indx++)
		{
			float s = 1f;
			if (niParticlesData.hasSizes)
				s = niParticlesData.sizes[indx];
			particleRadius[indx] = ConvertFromNif.toJ3d(s * niParticlesData.particlesRadius / 2f);
		}

		updateData();

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

			shiftArray(particleSpawnTime, indx, 1, partsToMove);

			shiftArray(particleAge, indx, 1, partsToMove);

			shiftArray(particleLifeSpan, indx, 1, partsToMove);

			shiftArray(particleGeneration, indx, 1, partsToMove);

			shiftArray(particleRadius, indx, 1, partsToMove);

			shiftArray(particleTranslation, indx, translationStride, partsToMove);

			shiftArray(particleRotationAngle, indx, 1, partsToMove);

			shiftArray(particleRotationSpeed, indx, 1, partsToMove);

			shiftArray(particleVelocity, indx, velocityStride, partsToMove);

			if (niParticlesData.hasVertexColors)
				shiftArray(particleColors, indx, colorStride, partsToMove);

			shiftArray(particleImageIds, indx, 1, partsToMove);

			activeParticleCount--;
			//ga.setValidIndexCount(activeParticleCount); is called in geom update
		}
	}

	private static void shiftArray(Object arr, int indx, int stride, int remCount)
	{
		int srcStart = indx * stride + stride;
		int destStart = indx * stride;
		int len = remCount * stride;
		System.arraycopy(arr, srcStart, arr, destStart, len);

	}

	public boolean canAdd()
	{
		return activeParticleCount < maxParticleCount;
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

			particleSpawnTime[indx] = System.currentTimeMillis();

			particleAge[indx] = 0;

			particleLifeSpan[indx] = lifeSpan;

			particleGeneration[indx] = generation;

			particleRadius[indx] = radius;

			particleTranslation[indx * 3 + 0] = x;
			particleTranslation[indx * 3 + 1] = y;
			particleTranslation[indx * 3 + 2] = z;

			particleRotationAngle[indx] = 0f;
			particleRotationSpeed[indx] = 0f;

			particleVelocity[indx * 3 + 0] = velx;
			particleVelocity[indx * 3 + 1] = vely;
			particleVelocity[indx * 3 + 2] = velz;

			if (niParticlesData.hasVertexColors)
			{
				particleColors[indx * 4 + 0] = r;
				particleColors[indx * 4 + 1] = g;
				particleColors[indx * 4 + 2] = b;
				particleColors[indx * 4 + 3] = a;
			}

			particleImageIds[indx] = 0;

			activeParticleCount++;
			//ga.setValidIndexCount(activeParticleCount); called in geom update
			return indx;
		}

		return -1;
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded
	 * If particleTranslation array is altered this must be called 
	 * @param particle
	 */
	public void recalcAllGaCoords()
	{
		//TODO: this MUST be a double buffer pointer swap system, not this copy rubbish
		System.arraycopy(particleTranslation, 0, gaCoords, 0, activeParticleCount * 3);

		//TODO: I get the impression that the black dot is
		// a badly formed final particle somehow? 
		// If I take the last 5 off they are not there, but they flash during re-loop

		// set by add or inactivate but called here
		//	if (activeParticleCount > 5)
		//		ga.setValidIndexCount(activeParticleCount - 5);
		//	else
		ga.setValidIndexCount(activeParticleCount);
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded
	 * If particleRadius array is altered this must be called 
	 * @param particle
	 */
	public void recalcSizes()
	{
		System.arraycopy(particleRadius, 0, gaVsizesF, 0, activeParticleCount * 1);
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
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded
	 * If particleColors array is altered this must be called  
	 * @param particle
	 */
	public void resetAllGaColors()
	{
		if (niParticlesData.hasVertexColors)
		{
			System.arraycopy(particleColors, 0, gaColors, 0, activeParticleCount * 4);

		}

	}

	public void resetTexCoords()
	{
		for (int i = 0; i < activeParticleCount; i++)
		{
			//TODO: looks like there is no atlas system for particles
			//there is no UV at all! so I can just use the point xy
			//divided by size as uv into texture in frag

			// simply fixed 
			if (particleImageIds[i] == 0)
			{
				gaTexCoords[i * 2 + 0] = 0.5f;
				gaTexCoords[i * 2 + 1] = 0.5f;
			}
		}

	}

	public IndexedPointArray getGeometryArray()
	{
		return ga;
	}

	public void updateData()
	{
		recalcAllGaCoords();
		resetAllGaColors();
		recalcSizes();
		recalcRotations();
		// pointless for now, only altases need it resetTexCoords();
	}

	public void printoutParticleData()
	{
		System.out.println("Active Particle count = " + activeParticleCount);
		for (int indx = 0; indx < activeParticleCount; indx++)
		{
			System.out.print("" + indx);
			//System.out.print(" SpawnTime " + particleSpawnTime[indx]);

			System.out.print(" Age " + particleAge[indx]);

			System.out.print(" LifeSpan " + particleLifeSpan[indx]);

			//System.out.print(" Generation " + particleGeneration[indx]);

			System.out.print(" Radius " + particleRadius[indx]);

			System.out.print(" T(x=" + particleTranslation[indx * 3 + 0]);
			System.out.print(",y=" + particleTranslation[indx * 3 + 1]);
			System.out.print(",z=" + particleTranslation[indx * 3 + 2] + ")\t");

			System.out.print(" RotAngle " + particleRotationAngle[indx]);
			System.out.print(" RotSpeed " + particleRotationSpeed[indx]);

			System.out.print(" V(x=" + particleVelocity[indx * 3 + 0]);
			System.out.print(",y=" + particleVelocity[indx * 3 + 1]);
			System.out.print(",z=" + particleVelocity[indx * 3 + 2] + ")\t");

			if (niParticlesData.hasVertexColors)
			{
				System.out.print(" C(r= " + particleColors[indx * 4 + 0]);
				System.out.print(",g=" + particleColors[indx * 4 + 1]);
				System.out.print(",b=" + particleColors[indx * 4 + 2]);
				System.out.print(",a=" + particleColors[indx * 4 + 3] + ")\t");

				System.out.print(" gaC(r= " + gaColors[indx * 4 + 0]);
				System.out.print(",g=" + gaColors[indx * 4 + 1]);
				System.out.print(",b=" + gaColors[indx * 4 + 2]);
				System.out.print(",a=" + gaColors[indx * 4 + 3] + ")\t");

			}

			System.out.print(" ImageId " + particleImageIds[indx]);
			System.out.println("");

		}

	}

}
