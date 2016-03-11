package nif.j3d.particles;

import javax.media.j3d.Appearance;
import javax.media.j3d.GLSLShaderProgram;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAppearance;
import javax.media.j3d.ShaderAttribute;
import javax.media.j3d.ShaderAttributeArray;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.ShaderProgram;
import javax.media.j3d.SourceCodeShader;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;

import nif.niobject.particle.NiPSysData;
import tools3d.utils.ShaderSourceIO;

public class J3dPSysData
{
	public static int vertsPerFace = 6;

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

	public int[] particleImageIds;

	public IndexedTriangleArray ga;

	private int gaVertexCount;

	private float[] gaTexCoords;

	private int[] gaCoordIndices; //fixed at intitialization, DO NOT ALTER	

	private float[] gaCoords;

	private float[] gaColors;

	private NiPSysData niPSysData = null;

	private TransformGroup billTG;

	public AtlasAnimatedTexture atlasAnimatedTexture;

	//https://www.opengl.org/discussion_boards/showthread.php/166796-GLSL-PointSprites-different-sizes

	/**
	 * NOTE!!!! all calls to this class must be in a GeomteryUpdater only. And violently single threaded
	 * 
	 * @param niPSysData
	 */
	public J3dPSysData(NiPSysData niPSysData, TransformGroup billTG)
	{
		this.billTG = billTG;
		//Particles can consist of screen-facing textured quads  

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

		ga = new IndexedTriangleArray(gaVertexCount,
				GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.COLOR_4
						| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY,
				gaVertexCount);

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
			gaCoordIndices[i * 4 + 0] = i * 4 + 0;//1
			gaCoordIndices[i * 4 + 1] = i * 4 + 1;//2
			gaCoordIndices[i * 4 + 2] = i * 4 + 2;//3
			gaCoordIndices[i * 4 + 3] = i * 4 + 3;//1
			gaCoordIndices[i * 4 + 4] = i * 4 + 4;//3
			gaCoordIndices[i * 4 + 6] = i * 4 + 5;//4

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

			billTG.getTransform(transF);
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
		//file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Reference/NiParticle/NiPSAlignedQuadGenerator.htm
		if (niPSysData.HasUVQuadrants)
		{
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.NumUVQuadrants);

			// default the modifer will set a start point
			particleImageIds[indx] = 0;
			atlasAnimatedTexture.getUVCoords(gaTexCoords, indx, particleImageIds[indx]);
		}
		else if (niPSysData.HasSubtextureOffsetUVs)
		{
			atlasAnimatedTexture = new AtlasAnimatedTexture(niPSysData.AspectRatio, niPSysData.SubtextureOffsetUVs);

			// default the modifer will set a start point
			particleImageIds[indx] = 0;
			atlasAnimatedTexture.getUVCoords(gaTexCoords, indx, particleImageIds[indx]);
		}
		else
		{
			// simply fixed
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

	public void updateAllTexCoords()
	{
		if (niPSysData.HasUVQuadrants || niPSysData.HasSubtextureOffsetUVs)
		{
			for (int indx = 0; indx < activeParticleCount; indx++)
			{
				atlasAnimatedTexture.getUVCoords(gaTexCoords, indx, particleImageIds[indx]);
			}
		}
	}

	/** if you alter the translation or rotation arrays directly this must be called
	 * 
	 */
	public void recalcAllGaCoords()
	{
		billTG.getTransform(transF);
		for (int i = 0; i < activeParticleCount; i++)
		{
			recalcGaCoords(i);
		}
	}

	//deburners
	private Transform3D transR = new Transform3D();

	private Transform3D transF = new Transform3D();

	private AxisAngle4f rotAA = new AxisAngle4f(0, 0, 1, 0);

	private Point3f p = new Point3f();

	/**
	 * billTG.getTransform(transF); must be called prior
	 */
	private void recalcGaCoords(int particle)
	{
		//NOTE! we rotate halfRad around 0,0 the - + values going into the face camera trans below are correct, you do the maths
		// I'm building the 4 corners the transform will contain only a rotate
		// now make it point at the camera via the billboard behave group
		// I've got x,y,z of the center added on after the rotate and face to camera

		float x = particleTranslation[particle * 3 + 0];
		float y = particleTranslation[particle * 3 + 1];
		float z = particleTranslation[particle * 3 + 2];

		rotAA.setAngle(particleRotationAngle[particle]);
		transR.set(rotAA);

		//TODO: radius is naturally a half, why half again? Did game bryo say so
		float halfRad = particleRadius[particle] / 2f;
		// TODO: these are just 2 matrix operations, I could make it simpler no doubt

		p.set(-halfRad, -halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 0 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 0 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 0 + 2] = z + p.z;//1

		p.set(halfRad, -halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 3 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 3 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 3 + 2] = z + p.z;//2

		p.set(halfRad, halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 6 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 6 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 6 + 2] = z + p.z;//3

		p.set(-halfRad, -halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 9 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 9 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 9 + 2] = z + p.z;//1

		p.set(halfRad, halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 12 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 12 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 12 + 2] = z + p.z;//3

		p.set(-halfRad, halfRad, 0);
		transR.transform(p);
		transF.transform(p);
		gaCoords[particle * 6 * 3 + 15 + 0] = x + p.x;
		gaCoords[particle * 6 * 3 + 15 + 1] = y + p.y;
		gaCoords[particle * 6 * 3 + 15 + 2] = z + p.z;//4
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
		//TODO: the texture shader has a gradient color texture under grayscale texture holder
		//textures\effects\gradients\GradFlame01.dds

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

	private static ShaderProgram shaderProgram = null;

	protected Appearance createAppearance(Texture tex)
	{
		// Create the shader attribute set
		ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();

		ShaderAppearance app = new ShaderAppearance();

		if (shaderProgram == null)
		{

			String vertexProgram = ShaderSourceIO.getTextFileAsString("shaders/water.vert");
			String fragmentProgram = ShaderSourceIO.getTextFileAsString("shaders/water.frag");

			Shader[] shaders = new Shader[2];
			shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
				public String toString()
				{
					return "vertexProgram";
				}
			};
			shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram) {
				public String toString()
				{
					return "fragmentProgram";
				}
			};

			final String[] shaderAttrNames = { "envMap", "numWaves", "amplitude", "wavelength", "speed", "direction", "time" };

			shaderProgram = new GLSLShaderProgram() {
				public String toString()
				{
					return "Water Shader Program";
				}
			};
			shaderProgram.setShaders(shaders);
			shaderProgram.setShaderAttrNames(shaderAttrNames);

			ShaderAttribute shaderAttribute = new ShaderAttributeValue("envMap", new Integer(0));
			shaderAttributeSet.put(shaderAttribute);

			shaderAttribute = new ShaderAttributeValue("numWaves", new Integer(4));
			shaderAttributeSet.put(shaderAttribute);

			Float[] amplitude = new Float[4];
			Float[] wavelength = new Float[4];
			Float[] speed = new Float[4];
			Point2f[] direction = new Point2f[4];
			for (int i = 0; i < 4; ++i)
			{
				amplitude[i] = 0.2f / (i + 1);
				wavelength[i] = (float) (8 * Math.PI / (i + 1));
				speed[i] = 1.0f + 2 * i;
				float angle = uniformRandomInRange(-Math.PI / 3, Math.PI / 3);
				direction[i] = new Point2f((float) Math.cos(angle), (float) Math.sin(angle));
			}

			ShaderAttributeArray amplitudes = new ShaderAttributeArray("amplitude", amplitude);
			ShaderAttributeArray wavelengths = new ShaderAttributeArray("wavelength", wavelength);
			ShaderAttributeArray speeds = new ShaderAttributeArray("speed", speed);
			ShaderAttributeArray directions = new ShaderAttributeArray("direction", direction);
			shaderAttributeSet.put(amplitudes);
			shaderAttributeSet.put(wavelengths);
			shaderAttributeSet.put(speeds);
			shaderAttributeSet.put(directions);

		}

		((ShaderAppearance) app).setShaderProgram(shaderProgram);
		((ShaderAppearance) app).setShaderAttributeSet(shaderAttributeSet);

		TextureUnitState[] tus = new TextureUnitState[1];
		TextureUnitState tus0 = new TextureUnitState();
		tus0.setTexture(tex);

		//TextureCubeMap textureCubeMap = new TextureCubeMap();

		tus[0] = tus0;
		app.setTextureUnitState(tus);

		app.setMaterial(getLandMaterial());

		app.setRenderingAttributes(new RenderingAttributes());

		return app;
	}

	public Material getLandMaterial()
	{

		Material landMaterial = new Material();

		landMaterial.setShininess(100.0f); // water is  very shiny, generally
		landMaterial.setDiffuseColor(0.5f, 0.5f, 0.6f);
		landMaterial.setSpecularColor(1.0f, 1.0f, 1.0f);
		landMaterial.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

		return landMaterial;
	}

	private static float uniformRandomInRange(double d, double e)
	{
		return (float) (d + (Math.random() * (e - d)));
	}

}
