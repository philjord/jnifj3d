package nif.j3d.particles;

import org.jogamp.java3d.BoundingBox;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.IndexedPointArray;
import org.jogamp.vecmath.Color4f;
import org.jogamp.vecmath.Vector3f;

import nif.niobject.particle.NiPSysData;

public class J3dPSysData {
	public static int			gaCoordStride			= 3;

	public static int			gaColorStride			= 4;

	public static int			gaTexCoordStride		= 2;

	public static int			gaSubTextureSizeStride	= 2;

	public static int			translationStride		= 3;

	public static int			velocityStride			= 3;

	public static int			colorStride				= 4;

	public int					maxParticleCount;

	public int					activeParticleCount;

	public float[]				particleColors;

	public long[]				particleSpawnTime;

	public long[]				particleAge;

	public long[]				particleLifeSpan;

	public int[]				particleGeneration;

	public float[]				particleRadius;					// radius (can be multiplied from 0-1 by size if size != 1)

	public float[]				particleSize;					// 0-1 from grow/fade to indicate size ratio

	public float[]				particleTranslation;

	public float[]				particleRotationAngle;

	public float[]				particleRotationSpeed;

	public float[]				particleVelocity;

	public int[]				particleImageIds;

	protected IndexedPointArray	ga;

	protected int				gaVertexCount;

	protected float[]			gaTexCoords;

	protected int[]				gaCoordIndices;					//fixed at initialization, DO NOT ALTER	

	protected float[]			gaCoords;

	protected float[]			gaColors;

	protected float[]			gaVradiiF;

	//protected float[]			gaVrotationsF;//swapped to calc cos and sin for speed
	protected float[]			gaVrcosF;
	protected float[]			gaVrsinF;//TODO: should be a stride 2 construct

	protected float[]			gaVsubTextureSizeF;

	protected NiPSysData		niPSysData				= null;

	public AtlasAnimatedTexture	atlasAnimatedTexture;

	/**
	 * For J3dPSysDataTest sub class only
	 * 
	 */
	protected J3dPSysData() {

	}

	//https://www.opengl.org/discussion_boards/showthread.php/166796-GLSL-PointSprites-different-sizes

	public J3dPSysData(NiPSysData niPSysData) {
		//FIXME I should check these conditions and alter the array storage
		//niPSysData.hasVertices;
		if (niPSysData.hasNormals)
			System.err.println("Wait! Wahat? niPSysData.hasNormals");
		//niPSysData.hasRotationAngles;
		//niPSysData.hasRotationAxes;
		//niPSysData.hasSizes;
		//niPSysData.hasVertexColors;
		//niPSysData.HasUVQuadrants;
		//niPSysData.NumUVQuadrants;

		this.niPSysData = niPSysData;

		maxParticleCount = Math.max(niPSysData.BSMaxVertices, niPSysData.numVertices);
		gaVertexCount = maxParticleCount;// 1 vertices per particle, this is PointArrays

		ga = new IndexedPointArray(
				gaVertexCount,
				GeometryArray.BY_REFERENCE	| GeometryArray.COORDINATES
								| GeometryArray.TEXTURE_COORDINATE_2

								| (niPSysData.hasVertexColors ? GeometryArray.COLOR_4 : 0)

								| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY
								| GeometryArray.VERTEX_ATTRIBUTES,
				1, new int[] {0}, 4, new int[] {1, 1, 1, 2}, gaVertexCount);

		ga.setName("Particles System");

		ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		ga.setCapability(GeometryArray.ALLOW_COUNT_WRITE);

		gaCoordIndices = new int[gaVertexCount];
		//fixed for all time, recall these are points
		for (int i = 0; i < gaVertexCount; i++) {
			gaCoordIndices[i] = i;
		}

		gaCoords = new float[gaVertexCount * 3];
		gaTexCoords = new float[gaVertexCount * 2];

		if (niPSysData.hasVertexColors) {
			particleColors = new float[maxParticleCount * colorStride];
		}
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

		ga.setCoordRefFloat(gaCoords);
		ga.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		ga.setTexCoordRefFloat(0, gaTexCoords);
		ga.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
		ga.setCoordIndicesRef(gaCoordIndices);

		if (niPSysData.hasVertexColors) {
			gaColors = new float[gaVertexCount * 4];// alpha included
			ga.setColorRefFloat(gaColors);
			ga.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		}

		//TODO: flip to byte buffers surely
		//ByteBuffer bb = ByteBuffer.allocateDirect(maxParticleCount);
		//bb.order(ByteOrder.nativeOrder());
		//sizes = bb.asFloatBuffer();
		gaVradiiF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(0, gaVradiiF);
		//gaVrotationsF = new float[maxParticleCount];
		//ga.setVertexAttrRefFloat(1, gaVrotationsF);
		gaVrcosF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(1, gaVrcosF);
		gaVrsinF = new float[maxParticleCount];
		ga.setVertexAttrRefFloat(2, gaVrsinF);
		gaVsubTextureSizeF = new float[maxParticleCount * 2];
		ga.setVertexAttrRefFloat(3, gaVsubTextureSizeF);
		ga.setCapability(GeometryArray.ALLOW_VERTEX_ATTR_WRITE);

		// this will always be 0, otherwise the run up system gets particles on screen for the first frame
		activeParticleCount = niPSysData.numActive;

	}

	/**
	 * In activate by copying the array backwards over the slot up to the active number the inactive part of the array
	 * is left with garbage, as the valid vertex count causes this to be ignored
	 * @param indx
	 */
	public void inactivateParticle(int indx) {
		if (indx < activeParticleCount) {
			int partsToMove = (activeParticleCount - indx) - 1; //particles After Indx To Move left

			shiftArray(gaCoords, indx, gaCoordStride, partsToMove);
			if (niPSysData.hasVertexColors)
				shiftArray(gaColors, indx, gaColorStride, partsToMove);
			shiftArray(gaTexCoords, indx, gaTexCoordStride, partsToMove);
			shiftArray(gaVradiiF, indx, 1, partsToMove);
			//shiftArray(gaVrotationsF, indx, 1, partsToMove);
			shiftArray(gaVrcosF, indx, 1, partsToMove);
			shiftArray(gaVrsinF, indx, 1, partsToMove);
			shiftArray(gaVsubTextureSizeF, indx, gaSubTextureSizeStride, partsToMove);

			if (niPSysData.hasVertexColors)
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

	private static void shiftArray(Object arr, int indx, int stride, int remCount) {
		int srcStart = indx * stride + stride;
		int destStart = indx * stride;
		int len = remCount * stride;
		System.arraycopy(arr, srcStart, arr, destStart, len);
	}

	/**
	 * Always add to the end, and only adds if there is space (do nothing otherwise)
	 * @return the particle id of the newly created particle id
	 */
	public int addActive(	float radius, long lifeSpan, int generation, float x, float y, float z, float r, float g,
							float b, float a, float velx, float vely, float velz) {

		if (activeParticleCount < maxParticleCount) {
			int indx = activeParticleCount;

			particleSpawnTime[indx] = System.currentTimeMillis();

			particleAge[indx] = 0;

			particleLifeSpan[indx] = lifeSpan;

			particleGeneration[indx] = generation;

			particleRadius[indx] = radius;
			gaVradiiF[indx] = radius;
			boundsMaxSize = boundsMaxSize > radius ? boundsMaxSize : radius;// for the debug box

			particleSize[indx] = 1f;// size always defaults to 1 and grow/fade can alter if needed				

			particleTranslation[indx * 3 + 0] = x;
			particleTranslation[indx * 3 + 1] = y;
			particleTranslation[indx * 3 + 2] = z;
			gaCoords[indx * 3 + 0] = x;
			gaCoords[indx * 3 + 1] = y;
			gaCoords[indx * 3 + 2] = z;

			particleRotationAngle[indx] = 0f;
			//gaVrotationsF[indx] = 0f;
			gaVrcosF[indx] = 0f;
			gaVrsinF[indx] = 0f;

			particleVelocity[indx * 3 + 0] = velx;
			particleVelocity[indx * 3 + 1] = vely;
			particleVelocity[indx * 3 + 2] = velz;

			if (niPSysData.hasVertexColors) {
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

	private void initTexCoords(int indx) {
		//file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Reference/NiParticle/NiPSAlignedQuadGenerator.htm
		if (niPSysData.NumSubtextureOffsetUVs > 0) {
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.AspectRatio, niPSysData.SubtextureOffsetUVs);

			// a J3dBSPSysSubTexModifier will control this, but without it we just go random selection
			particleImageIds[indx] = (int)(Math.random() * atlasAnimatedTexture.getSubImageCount());
			atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds[indx]);
		} else {
			//if there is no atlas system for particles there is no real UV at all!
			// I can use the frags virtual coords as the uv, but I still need to have the start and stride as default
			//particleImageIds[indx] should be 0 at this point

			// simply fixed, start point is the 0,0
			gaTexCoords[indx * 2 + 0] = 0f;
			gaTexCoords[indx * 2 + 1] = 0f;
			//default to 1,1
			gaVsubTextureSizeF[indx * 2 + 0] = 1f;
			gaVsubTextureSizeF[indx * 2 + 1] = 1f;
		}
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded Call on each
	 * update
	 */
	public void updateAllTexCoords() {
		//TODO: I should only call this when the sub text is updated by niPSysModifier [BSPSysSubTexModifier] 
		if (niPSysData.NumSubtextureOffsetUVs > 0) {
			for (int indx = 0; indx < activeParticleCount; indx++) {
				atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds[indx]);
			}
		}
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
	 * particleTranslation is altered this must be called
	 */
	public void recalcAllGaCoords() {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE;
		float maxY = Float.MIN_VALUE;
		float maxZ = Float.MIN_VALUE;

		for (int i = 0; i < activeParticleCount; i++) {
			// with points we simply push the particles across to the gaCoords, in fact we only need agCoords

			float x = particleTranslation[i * 3 + 0];
			float y = particleTranslation[i * 3 + 1];
			float z = particleTranslation[i * 3 + 2];

			minX = minX < x ? minX : x;
			minY = minY < y ? minY : y;
			minZ = minZ < z ? minZ : z;
			maxX = maxX > x ? maxX : x;
			maxY = maxY > y ? maxY : y;
			maxZ = maxZ > z ? maxZ : z;

			gaCoords[i * 3 + 0] = x;
			gaCoords[i * 3 + 1] = y;
			gaCoords[i * 3 + 2] = z;
		}

		bounds.setLower(minX - boundsMaxSize, minY - boundsMaxSize, minZ - boundsMaxSize);
		bounds.setUpper(maxX + boundsMaxSize, maxY + boundsMaxSize, maxZ + boundsMaxSize);

	}

	protected float		boundsMaxSize	= 0;				// modified in add active
	public BoundingBox	bounds			= new BoundingBox();

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
	 * particleRadius is altered this must be called.
	 */
	public void recalcSizes() {
		// size is radius (but might be  * size multiplier from grow/fade which is a 0 to 1 if it exists and 1 otherwise)
		for (int i = 0; i < activeParticleCount; i++) {
			float r = particleRadius[i] * particleSize[i];
			gaVradiiF[i] = r;
		}
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
	 * particleRotationAngle is altered this must be called
	 */
	public void recalcRotations() {
		for (int i = 0; i < activeParticleCount; i++) {
			//gaVrotationsF[i] = particleRotationAngle[i];
			gaVrcosF[i] = (float)Math.cos(particleRotationAngle[i]);
			gaVrsinF[i] = (float)Math.sin(particleRotationAngle[i]);
		}
	}

	/**
	 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
	 * particleColors is altered this must be called
	 */
	public void recalcAllGaColors() {
		if (niPSysData.hasVertexColors) {
			for (int i = 0; i < activeParticleCount; i++) {
				//TODO: the texture shader has a gradient color texture under grayscale texture holder
				//textures\effects\gradients\GradFlame01.dds

				gaColors[i * 4 + 0] = particleColors[i * 4 + 0];
				gaColors[i * 4 + 1] = particleColors[i * 4 + 1];
				gaColors[i * 4 + 2] = particleColors[i * 4 + 2];
				gaColors[i * 4 + 3] = particleColors[i * 4 + 3];
			}
		}

	}

	public IndexedPointArray getGeometryArray() {
		return ga;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * A class that is for testing data only Auto opening node: ArchiveFile:Oblivion -
	 * Meshes.bsa/meshes/effects/dustcloud.nif
	 * 
	 * Notice I have an issue where a particle nif loaded after another one fails to render
	 */
	public static class J3dPSysDataTest extends J3dPSysData // to allow pointers to remain the same
	{

		static int		TOTAL_PARTS			= -1;	//-1 for disable
		static float	FIXED_RADIUS		= -1;	//-1 for disable
		static float	FIXED_ROTATION		= -1;	//-1 for disable
		static Vector3f	FIXED_LOCATION		= null;	//new Vector3f(0.0f, 0.0f, 0);	//null for disable
		static Color4f	FIXED_COLOR			= null;	//new Color4f(1, 0, 0, 0.8f);	//null for disable
		static boolean	CUT_OUT_INACT		= false;
		static boolean	CUT_OUT_GROW_FADE	= false;
		static long		SLEEP_OVERRIDE		= 50;

		//https://www.opengl.org/discussion_boards/showthread.php/166796-GLSL-PointSprites-different-sizes

		public J3dPSysDataTest(NiPSysData niPSysData) {
			super();

			//FIXME I should check these conditions and alter the array storage
			//niPSysData.hasVertices;
			if (niPSysData.hasNormals)
				System.err.println("Wait! Wahat? niPSysData.hasNormals");
			//niPSysData.hasRadii;
			//niPSysData.hasRotationAngles;
			//niPSysData.hasRotationAxes;
			//niPSysData.hasSizes;
			//niPSysData.hasVertexColors;
			//niPSysData.HasUVQuadrants;
			//niPSysData.NumUVQuadrants;

			this.niPSysData = niPSysData;

			maxParticleCount = Math.max(niPSysData.BSMaxVertices, niPSysData.numVertices);

			// *********************************
			if (TOTAL_PARTS > 0)
				maxParticleCount = TOTAL_PARTS;

			//little debug output
			if (niPSysData.NumSubtextureOffsetUVs > 0) {
				System.out.println("atlasAnimatedTexture!!!!");
				// fallout 3 and above has these see code
			} else {
				//oblivion has these			
				// is there a non atlas that has multiple images in the texture?			
				System.out.println("Not atlasAnimatedTexture");
			}

			gaVertexCount = maxParticleCount;// 1 vertices per particle, this is PointArrays

			ga = new IndexedPointArray(
					gaVertexCount,
					GeometryArray.BY_REFERENCE	| GeometryArray.COORDINATES
									| GeometryArray.TEXTURE_COORDINATE_2
									| (niPSysData.hasVertexColors ? GeometryArray.COLOR_4 : 0)
									| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY
									| GeometryArray.VERTEX_ATTRIBUTES,
					1, new int[] {0}, 4, new int[] {1, 1, 1, 2}, gaVertexCount);

			ga.setName("Particles System");

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
			ga.setCapability(GeometryArray.ALLOW_COUNT_WRITE);

			gaCoordIndices = new int[gaVertexCount];
			//fixed for all time, recall these are points
			for (int i = 0; i < gaVertexCount; i++) {
				gaCoordIndices[i] = i;
			}

			gaCoords = new float[gaVertexCount * 3];
			gaTexCoords = new float[gaVertexCount * 2];

			if (niPSysData.hasVertexColors) {
				particleColors = new float[maxParticleCount * colorStride];
			}
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

			ga.setCoordRefFloat(gaCoords);
			ga.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
			ga.setTexCoordRefFloat(0, gaTexCoords);
			ga.setCapability(GeometryArray.ALLOW_TEXCOORD_WRITE);
			ga.setCoordIndicesRef(gaCoordIndices);

			if (niPSysData.hasVertexColors) {
				gaColors = new float[gaVertexCount * 4];// alpha included
				ga.setColorRefFloat(gaColors);
				ga.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
			}

			//ByteBuffer bb = ByteBuffer.allocateDirect(maxParticleCount);
			//bb.order(ByteOrder.nativeOrder());
			//sizes = bb.asFloatBuffer();
			gaVradiiF = new float[maxParticleCount];
			ga.setVertexAttrRefFloat(0, gaVradiiF);
			//gaVrotationsF = new float[maxParticleCount];
			//ga.setVertexAttrRefFloat(1, gaVrotationsF);
			gaVrcosF = new float[maxParticleCount];
			ga.setVertexAttrRefFloat(1, gaVrcosF);
			gaVrsinF = new float[maxParticleCount];
			ga.setVertexAttrRefFloat(2, gaVrsinF);
			gaVsubTextureSizeF = new float[maxParticleCount * 2];
			ga.setVertexAttrRefFloat(3, gaVsubTextureSizeF);
			ga.setCapability(GeometryArray.ALLOW_VERTEX_ATTR_WRITE);

			// this will always be 0, otherwise the run up system gets particles on screen for the first frame
			activeParticleCount = niPSysData.numActive;

		}

		/**
		 * In activate by copying the array backwards over the slot up to the active number the inactive part of the
		 * array is left with garbage, as the valid vertex count causes this to be ignored
		 * @param indx
		 */
		@Override
		public void inactivateParticle(int indx) {
			// *********************************
			if (CUT_OUT_INACT)
				return;// no inactive no spawning

			if (indx < activeParticleCount) {
				int partsToMove = (activeParticleCount - indx) - 1; //particles After Indx To Move left

				shiftArray(gaCoords, indx, gaCoordStride, partsToMove);
				if (niPSysData.hasVertexColors)
					shiftArray(gaColors, indx, gaColorStride, partsToMove);
				shiftArray(gaTexCoords, indx, gaTexCoordStride, partsToMove);
				shiftArray(gaVradiiF, indx, 1, partsToMove);
				//shiftArray(gaVrotationsF, indx, 1, partsToMove);
				shiftArray(gaVrcosF, indx, 1, partsToMove);
				shiftArray(gaVrsinF, indx, 1, partsToMove);				
				shiftArray(gaVsubTextureSizeF, indx, gaSubTextureSizeStride, partsToMove);

				if (niPSysData.hasVertexColors)
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

		private static void shiftArray(Object arr, int indx, int stride, int remCount) {
			int srcStart = indx * stride + stride;
			int destStart = indx * stride;
			int len = remCount * stride;
			System.arraycopy(arr, srcStart, arr, destStart, len);
		}

		/**
		 * Always add to the end, and only adds if there is space (do nothing otherwise)
		 * @return the particle id of the newly created particle id
		 */
		@Override
		public int addActive(	float radius, long lifeSpan, int generation, float x, float y, float z, float r, float g,
								float b, float a, float velx, float vely, float velz) {

			if (activeParticleCount < maxParticleCount) {
				int indx = activeParticleCount;

				particleSpawnTime[indx] = System.currentTimeMillis();

				particleAge[indx] = 0;

				particleLifeSpan[indx] = lifeSpan;

				particleGeneration[indx] = generation;

				particleRadius[indx] = radius;
				gaVradiiF[indx] = radius;
				boundsMaxSize = boundsMaxSize > radius ? boundsMaxSize : radius;// for the debug box

				particleSize[indx] = 1f;// size always defaults to 1 and grow/fade can alter if needed				

				particleTranslation[indx * 3 + 0] = x;
				particleTranslation[indx * 3 + 1] = y;
				particleTranslation[indx * 3 + 2] = z;
				gaCoords[indx * 3 + 0] = x;
				gaCoords[indx * 3 + 1] = y;
				gaCoords[indx * 3 + 2] = z;

				particleRotationAngle[indx] = 0f;
				//gaVrotationsF[indx] = 0f;
				gaVrcosF[indx] = 0f;
				gaVrsinF[indx] = 0f;

				particleVelocity[indx * 3 + 0] = velx;
				particleVelocity[indx * 3 + 1] = vely;
				particleVelocity[indx * 3 + 2] = velz;

				if (niPSysData.hasVertexColors) {
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

		private void initTexCoords(int indx) {
			//file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Reference/NiParticle/NiPSAlignedQuadGenerator.htm
			if (niPSysData.NumSubtextureOffsetUVs > 0) {
				atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.AspectRatio, niPSysData.SubtextureOffsetUVs);

				// a J3dBSPSysSubTexModifier will control this, but without it we just go random selection
				// this one is missing the J3dBSPSysSubTexModifier so not sure? who updates it?
				// is it it random and teh particle disappears later?
				//Auto opening node: ArchiveFile:Skyrim - Meshes.bsa/meshes/clutter/woodfires/fireplacewood01burning.nif

				//this is a 2 high by 4 wide 
				//J3dNiParticleSystem FlamesSmall01 baseTexture textures\effects\FXFireAtlas02.dds

				particleImageIds[indx] = (int)(Math.random() * atlasAnimatedTexture.getSubImageCount());
				atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds[indx]);
				// fallout 3 and skyrim has these
			} else {
				//oblivion has these

				// is there a non atlas that has multiple images in the texture?

				//if there is no atlas system for particles there is no real UV at all!
				// I can use the frags virtual coords as the uv, but I still need to have the start and stride as default

				//particleImageIds[indx] should be 0 at this point
				gaTexCoords[indx * 2 + 0] = 0f;
				gaTexCoords[indx * 2 + 1] = 0f;
				gaVsubTextureSizeF[indx * 2 + 0] = 1f;
				gaVsubTextureSizeF[indx * 2 + 1] = 1f;

			}
		}

		/**
		 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded Call on
		 * each update
		 */
		@Override
		public void updateAllTexCoords() {
			//TODO: I should only call this when the sub text is updated by niPSysModifier [BSPSysSubTexModifier] 
			if (niPSysData.NumSubtextureOffsetUVs > 0) {
				for (int indx = 0; indx < activeParticleCount; indx++) {
					atlasAnimatedTexture.getUVCoords(gaTexCoords, gaVsubTextureSizeF, indx, particleImageIds[indx]);
				}
			}
		}

		/**
		 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
		 * particleTranslation is altered this must be called
		 */
		@Override
		public void recalcAllGaCoords() {
			float minX = Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float minZ = Float.MAX_VALUE;
			float maxX = Float.MIN_VALUE;
			float maxY = Float.MIN_VALUE;
			float maxZ = Float.MIN_VALUE;

			for (int i = 0; i < activeParticleCount; i++) {
				// with points we simply push the particles across to the gaCoords, in fact we only need agCoords

				float x = particleTranslation[i * 3 + 0];
				float y = particleTranslation[i * 3 + 1];
				float z = particleTranslation[i * 3 + 2];

				// *********************************
				if (FIXED_LOCATION != null) {
					x = FIXED_LOCATION.x;
					y = FIXED_LOCATION.y;
					z = FIXED_LOCATION.z;
				}

				minX = minX < x ? minX : x;
				minY = minY < y ? minY : y;
				minZ = minZ < z ? minZ : z;
				maxX = maxX > x ? maxX : x;
				maxY = maxY > y ? maxY : y;
				maxZ = maxZ > z ? maxZ : z;

				gaCoords[i * 3 + 0] = x;
				gaCoords[i * 3 + 1] = y;
				gaCoords[i * 3 + 2] = z;

			}

			// now set the bounds
			bounds.setLower(minX - boundsMaxSize, minY - boundsMaxSize, minZ - boundsMaxSize);
			bounds.setUpper(maxX + boundsMaxSize, maxY + boundsMaxSize, maxZ + boundsMaxSize);
			//System.out.println("maxSize " +maxSize);
			//System.out.println("bounds set " + bounds);

		}

		/**
		 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
		 * particleRadius is altered this must be called.
		 */
		@Override
		public void recalcSizes() {
			// size is radius (but might be  * size multiplier from grow/fade which is a 0 to 1 if it exists and 1 otherwise)		
			for (int i = 0; i < activeParticleCount; i++) {
				float r = particleRadius[i] * particleSize[i];

				// *********************************
				if (FIXED_RADIUS > 0) {
					r = FIXED_RADIUS;
					boundsMaxSize = FIXED_RADIUS;
				} else if (CUT_OUT_GROW_FADE) {
					r = particleRadius[i];
				}

				gaVradiiF[i] = r;
			}
		}

		/**
		 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
		 * particleRotationAngle is altered this must be called
		 */
		@Override
		public void recalcRotations() {
			for (int i = 0; i < activeParticleCount; i++) {
				//gaVrotationsF[i] = particleRotationAngle[i];
				gaVrcosF[i] = (float)Math.cos(particleRotationAngle[i]);
				gaVrsinF[i] = (float)Math.sin(particleRotationAngle[i]);

				// *********************************
				if (FIXED_ROTATION != -1) {
					//gaVrotationsF[i] = FIXED_ROTATION;
					gaVrcosF[i] = (float)Math.cos(FIXED_ROTATION);
					gaVrsinF[i] = (float)Math.sin(FIXED_ROTATION);
				}
			}
		}

		/**
		 * NOTE!!!! all calls to this method must be in a GeomteryUpdater only. And violently single threaded If
		 * particleColors is altered this must be called
		 */
		@Override
		public void recalcAllGaColors() {
			if (niPSysData.hasVertexColors) {
				for (int i = 0; i < activeParticleCount; i++) {

					//TODO: the texture shader has a gradient color texture under grayscale texture holder
					//textures\effects\gradients\GradFlame01.dds

					float r = particleColors[i * 4 + 0];
					float g = particleColors[i * 4 + 1];
					float b = particleColors[i * 4 + 2];
					float a = particleColors[i * 4 + 3];

					// *********************************
					if (FIXED_COLOR != null) {
						r = FIXED_COLOR.x;
						g = FIXED_COLOR.y;
						b = FIXED_COLOR.z;
						a = FIXED_COLOR.w;
					}

					gaColors[i * 4 + 0] = r;
					gaColors[i * 4 + 1] = g;
					gaColors[i * 4 + 2] = b;
					gaColors[i * 4 + 3] = a;
				}
			}

		}

		/*

		 * FIXME: NOTE bug, the first loaded particle system looks good, others seem to be 1 pixel wide etc, so look out!
		 * find the bug at some point
		 * 
		 * 
		 FIXME: each time the bounds is update oddly the particles themselves seem to flicker
		 
		
		SKYRIM
		
		
		// this one has a grey texture needs integrating, by way of NiGeometryShaderAppearance
		Auto opening node: ArchiveFile:Skyrim - Meshes.bsa/meshes/clutter/woodfires/fireplacewood01burning.nif
		atlasAnimatedTexture!!!!
		J3dNiParticleSystem created FlamesSmall01
		J3dNiParticleSystem FlamesSmall01 baseTexture textures\effects\FXFireAtlas02.dds
		J3dNiParticleSystem FlamesSmall01 GreyscaleTexture textures\effects\gradients\GradFireExplosion.dds
		TextureUnitState bind textures\effects\FXFireAtlas02.dds
		TextureUnitState bind textures\effects\gradients\GradFireExplosion.dds
		J3dNiParticleSystem FlamesSmall01 color = NifColor4: r=0.94901973,g=0.7294118,b=0.61960787,a=1.0
		using prog sk_effectshader.prog
		SourceTexture texunit 0 file=textures\effects\FXFireAtlas04.dds
		GreyscaleMap texunit 1 file=textures\effects\gradients\GradFireExplosion.dds
		
				
		
		// the keys seem far to large and the dust cloud is in teh wrong spot?
		/// // keys are going upwards?
		Auto opening node: ArchiveFile:DLCShiveringIsles - Meshes.bsa/meshes/effects/se03keys.nif
		Creating J3dNiPSysModifier from [NiPSysCylinderEmitter] 
		J3dNiPSysEmitter parent of next Emitter speed 24.0 speedVariation 0.14400001 declination 0.0 declinationVariation 3.1415927 planarAngle 0.0 planarAngleVariation 6.2831855 initialColor (1.0, 1.0, 1.0, 1.0) initialRadius 75.0 radiusVariation 22.499998 lifeSpan 2.5 lifeSpanVariation 1.6666667
		J3dNiPSysCylinderEmitter niPSysCylinderEmitter.radius 190.85454 niPSysCylinderEmitter.height 69.12
		
		
		I notice most of my water particels in skyrim are firing off at right angles to wehre they should be too
		
		
		Auto opening node: ArchiveFile:Skyrim - Meshes.bsa/meshes/effects/fxicewraithtest.nif
		// shows that the square particle is clearly cutting off one side of the atlas texture, I'm thinking aspect ratio?
		
		
		
		*********** NEW file Meshes\Effects\FXAmbSnowBlowingSm.nif
		niPSysModifier [NiPSysBombModifier]
		*********** NEW file Meshes\Effects\FXSplashLargeChurnNoRapids.nif
		niPSysModifier [NiPSysBombModifier] 
		*********** NEW file Meshes\Effects\FXSplashLargeChurnNoRapids.nif
		niPSysModifier [NiPSysColliderManager] 
		*********** NEW file Meshes\Effects\FXWaterfallBodySlope.nif
		niPSysModifier [NiPSysColliderManager] 
		
		 * 
		 */
	}
}
