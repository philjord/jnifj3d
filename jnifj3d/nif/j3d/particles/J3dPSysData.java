package nif.j3d.particles;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;

import nif.niobject.particle.NiPSysData;

public class J3dPSysData
{
	public static int vertsPerFace = 4;

	public static int gaCoordStride = vertsPerFace * 3;

	public static int gaTexCoordStride = vertsPerFace * 2;

	public static int gaColorStride = vertsPerFace * 4;

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

	public IndexedQuadArray ga;

	private int gaVertexCount;

	private int[] particleImageIds;

	private float[] gaTexCoords;

	private int[] gaCoordIndices; //fixed at intitialization, DO NOT ALTER	

	private float[] gaCoords;

	private float[] gaColors;

	private NiPSysData niPSysData = null;

	private TransformGroup billTG;

	private AtlasAnimatedTexture atlasAnimatedTexture;

	/**
	 * NOTE!!!! all calls to this class must be in a GeomteryUpdater only.
	 * 
	 * @param niPSysData
	 */
	public J3dPSysData(NiPSysData niPSysData, TransformGroup billTG)
	{
		this.billTG = billTG;
		//Particles can consist of screen-facing textured quads //TODO: quads not tris

		//niPSysData.hasVertices;
		//niPSysData.hasNormals;
		//niPSysData.hasRadii;
		//niPSysData.hasRotationAngles;
		//niPSysData.hasRotationAxes;
		//niPSysData.hasSizes;
		//niPSysData.hasVertexColors;
		//niPSysData.HasUVQuadrants;
		// niPSysData.NumUVQuadrants;

		this.niPSysData = niPSysData;

		maxParticleCount = Math.max(niPSysData.BSMaxVertices, niPSysData.numVertices);
		gaVertexCount = maxParticleCount * vertsPerFace;

		ga = new IndexedQuadArray(gaVertexCount, GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES
				| GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.COLOR_4 | GeometryArray.BY_REFERENCE_INDICES
				| GeometryArray.USE_COORD_INDEX_ONLY, gaVertexCount);

		ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		ga.setCapability(GeometryArray.ALLOW_COUNT_WRITE);

		gaCoords = new float[gaVertexCount * 3];
		gaTexCoords = new float[gaVertexCount * 2];
		gaCoordIndices = new int[gaVertexCount];
		gaColors = new float[gaVertexCount * 4];

		particleColors = new float[maxParticleCount * colorStride];
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

		//fixed for all time
		for (int i = 0; i < maxParticleCount; i++)
		{
			gaCoordIndices[i * 4 + 0] = i * 4 + 0;
			gaCoordIndices[i * 4 + 1] = i * 4 + 1;
			gaCoordIndices[i * 4 + 2] = i * 4 + 2;
			gaCoordIndices[i * 4 + 3] = i * 4 + 3;

		}

		ga.setCoordRefFloat(gaCoords);
		ga.setTexCoordRefFloat(0, gaTexCoords);
		ga.setColorRefFloat(gaColors);
		ga.setCoordIndicesRef(gaCoordIndices);

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
			int partsToMove = activeParticleCount - indx; //particles After Indx To Move left

			shiftArray(gaCoords, indx, gaCoordStride, partsToMove);

			shiftArray(gaColors, indx, gaColorStride, partsToMove);

			shiftArray(gaTexCoords, indx, gaTexCoordStride, partsToMove);

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

			shiftArray(particleImageIds, indx, 1, partsToMove);

			activeParticleCount--;
			ga.setValidIndexCount(activeParticleCount);

		}
	}

	private void shiftArray(Object arr, int indx, int stride, int remCount)
	{
		int srcStart = indx * stride + stride;
		int destStart = indx * stride;
		int len = remCount * stride;
		System.arraycopy(arr, srcStart, arr, destStart, len);
	}

	//TODO: add many more inits and also the COPY from particle id type
	/**
	 * Always add to the end, and only adds if there is space (do nothing otherwise)
	 * @param vx 
	 * @param vy 
	 * @param z 
	 * @return the particle id of the newly created particle id
	 
	  
	 * @return  the particle id of the newly created particle id
	 */
	public int addActive(float radius, long lifeSpan, int generation, float x, float y, float z, float r, float g, float b, float a,
			float velx, float vely, float velz)
	{
		int indx = activeParticleCount;
		if (indx < maxParticleCount - 1)
		{
			particleSpawnTime[indx] = System.currentTimeMillis();

			particleAge[indx] = 0;

			particleLifeSpan[indx] = lifeSpan;

			particleGeneration[indx] = generation;

			particleRadius[indx] = radius;

			particleTranslation[indx * 3 + 0] = x;
			particleTranslation[indx * 3 + 1] = y;
			particleTranslation[indx * 3 + 2] = z;

			particleRotationAngle[indx] = 0f;

			recalcGaCoords(indx);

			particleVelocity[indx * 3 + 0] = velx;
			particleVelocity[indx * 3 + 1] = vely;
			particleVelocity[indx * 3 + 2] = velz;

			particleColors[indx * 4 + 0 + 0] = r;
			particleColors[indx * 4 + 0 + 1] = g;
			particleColors[indx * 4 + 0 + 2] = b;
			particleColors[indx * 4 + 0 + 3] = a;

			initTexCoords(indx);

			resetGaColors(indx);

			activeParticleCount++;
			ga.setValidIndexCount(activeParticleCount);
			return indx;
		}

		return -1;
	}

	private void initTexCoords(int indx)
	{

		// test oblivion/meshes/landscape/miscfirefly01.nif
		//file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Reference/NiParticle/NiPSAlignedQuadGenerator.htm
		if (niPSysData.HasUVQuadrants)
		{
			//System.out.println("niPSysData.NumUVQuadrants");
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.NumUVQuadrants);

			int subImageId = (int) (Math.random() * atlasAnimatedTexture.getSubImageCount());
			particleImageIds[indx] = subImageId;
			atlasAnimatedTexture.getUVCoords(gaTexCoords, indx, subImageId);

			/*	int uCount = 1;
				int vCount = 1;

				if (niPSysData.NumUVQuadrants > 1)
				{
					float sq = (float) Math.sqrt(niPSysData.NumUVQuadrants);

					uCount = (int) Math.pow(2, (Math.floor(Math.log(sq) / Math.log(2))));
					vCount = (int) Math.pow(2, (Math.ceil(Math.log(sq) / Math.log(2))));
				}

				float uStride = (1f / uCount);
				float vStride = (1f / vCount);

				//TODO: randomly picked for now, but needs to be an animations effect I wager
				float uStart = (float) Math.floor(Math.random() * uCount) * uStride;
				float vStart = (float) Math.floor(Math.random() * vCount) * vStride;

				gaTexCoords[indx * 4 * 2 + 0] = uStart;
				gaTexCoords[indx * 4 * 2 + 1] = vStart;
				gaTexCoords[indx * 4 * 2 + 2] = uStart + uStride;
				gaTexCoords[indx * 4 * 2 + 3] = vStart;
				gaTexCoords[indx * 4 * 2 + 4] = uStart + uStride;
				gaTexCoords[indx * 4 * 2 + 5] = vStart + vStride;
				gaTexCoords[indx * 4 * 2 + 6] = uStart;
				gaTexCoords[indx * 4 * 2 + 7] = vStart + vStride;*/
		}
		else if (niPSysData.HasSubtextureOffsetUVs)
		{
			// pick one randomly, this is called a texture atlas and is updated by a BSPSysSubTextModifier
			//TODO: get me some tex coords! these ones are garbage
			gaTexCoords[indx * 4 * 2 + 0] = 0;
			gaTexCoords[indx * 4 * 2 + 1] = 0;
			gaTexCoords[indx * 4 * 2 + 2] = 1;
			gaTexCoords[indx * 4 * 2 + 3] = 0;
			gaTexCoords[indx * 4 * 2 + 4] = 1;
			gaTexCoords[indx * 4 * 2 + 5] = 1;
			gaTexCoords[indx * 4 * 2 + 6] = 0;
			gaTexCoords[indx * 4 * 2 + 7] = 1;
		}
		else
		{
			gaTexCoords[indx * 4 * 2 + 0] = 0;
			gaTexCoords[indx * 4 * 2 + 1] = 0;
			gaTexCoords[indx * 4 * 2 + 2] = 1;
			gaTexCoords[indx * 4 * 2 + 3] = 0;
			gaTexCoords[indx * 4 * 2 + 4] = 1;
			gaTexCoords[indx * 4 * 2 + 5] = 1;
			gaTexCoords[indx * 4 * 2 + 6] = 0;
			gaTexCoords[indx * 4 * 2 + 7] = 1;
		}

	}

	/** if you alter the translation or rotation arrays directly this must be called
	 * 
	 */
	public void recalcAllGaCoords()
	{
		for (int i = 0; i < activeParticleCount; i++)
		{
			recalcGaCoords(i);
		}
	}

	//deburners
	private Transform3D trans = new Transform3D();

	private AxisAngle4f rotAA = new AxisAngle4f(0, 0, 1, 0);

	private Point3f p = new Point3f();

	private Point3f p2 = new Point3f();

	private void recalcGaCoords(int particle)
	{
		float x = particleTranslation[particle * 3 + 0];
		float y = particleTranslation[particle * 3 + 1];
		float z = particleTranslation[particle * 3 + 2];

		rotAA.setAngle(particleRotationAngle[particle]);
		trans.set(rotAA);

		float halfRad = particleRadius[particle] / 2f;

		//NOTE! we rotate halfRad around 0,0 the - + values going into the face camera trans below are correct, you do the maths
		// I'm building the 4 corners the transform will contain only a rotate
		// now make it point at the camera via the billboard behave group
		// I've got x,y,z of the center added on after teh rotate adn face to camera
		p.set(halfRad, halfRad, 0);
		trans.transform(p);

		billTG.getTransform(trans);

		p2.set(-p.x, -p.y, 0);
		trans.transform(p2);
		gaCoords[particle * 4 * 3 + 0 + 0] = x + p2.x;
		gaCoords[particle * 4 * 3 + 0 + 1] = y + p2.y;
		gaCoords[particle * 4 * 3 + 0 + 2] = z + p2.z;

		p2.set(p.x, -p.y, 0);
		trans.transform(p2);
		gaCoords[particle * 4 * 3 + 3 + 0] = x + p2.x;
		gaCoords[particle * 4 * 3 + 3 + 1] = y + p2.y;
		gaCoords[particle * 4 * 3 + 3 + 2] = z + p2.z;

		p2.set(p.x, p.y, 0);
		trans.transform(p2);
		gaCoords[particle * 4 * 3 + 6 + 0] = x + p2.x;
		gaCoords[particle * 4 * 3 + 6 + 1] = y + p2.y;
		gaCoords[particle * 4 * 3 + 6 + 2] = z + p2.z;

		p2.set(-p.x, p.y, 0);
		trans.transform(p2);
		gaCoords[particle * 4 * 3 + 9 + 0] = x + p2.x;
		gaCoords[particle * 4 * 3 + 9 + 1] = y + p2.y;
		gaCoords[particle * 4 * 3 + 9 + 2] = z + p2.z;
	}

	/**
	 * If theparticleColors array is altered this must be called per each altered entry
	 * @param particle
	 */
	public void resetAllGaColors()
	{
		for (int i = 0; i < activeParticleCount; i++)
		{
			resetGaColors(i);
		}
	}

	private void resetGaColors(int particle)
	{
		float r = particleColors[particle * 4 + 0];
		float g = particleColors[particle * 4 + 1];
		float b = particleColors[particle * 4 + 2];
		float a = particleColors[particle * 4 + 3];

		gaColors[particle * 4 * 4 + 0 + 0] = r;
		gaColors[particle * 4 * 4 + 0 + 1] = g;
		gaColors[particle * 4 * 4 + 0 + 2] = b;
		gaColors[particle * 4 * 4 + 0 + 3] = a;

		gaColors[particle * 4 * 4 + 4 + 0] = r;
		gaColors[particle * 4 * 4 + 4 + 1] = g;
		gaColors[particle * 4 * 4 + 4 + 2] = b;
		gaColors[particle * 4 * 4 + 4 + 3] = a;

		gaColors[particle * 4 * 4 + 8 + 0] = r;
		gaColors[particle * 4 * 4 + 8 + 1] = g;
		gaColors[particle * 4 * 4 + 8 + 2] = b;
		gaColors[particle * 4 * 4 + 8 + 3] = a;

		gaColors[particle * 4 * 4 + 12 + 0] = r;
		gaColors[particle * 4 * 4 + 12 + 1] = g;
		gaColors[particle * 4 * 4 + 12 + 2] = b;
		gaColors[particle * 4 * 4 + 12 + 3] = a;

	}

}
