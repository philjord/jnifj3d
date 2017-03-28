package nif.j3d.particles.tes3;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Material;
import org.jogamp.java3d.PointAttributes;
import org.jogamp.java3d.Shader;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.ShaderAttributeSet;
import org.jogamp.java3d.ShaderAttributeValue;
import org.jogamp.java3d.ShaderProgram;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.SourceCodeShader;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiTexturingProperty;
import nif.niobject.particle.NiParticleModifier;
import nif.niobject.particle.NiParticleSystemController;
import nif.niobject.particle.NiParticles;
import tools.WeakListenerList;
import tools3d.utils.PhysAppearance;
import tools3d.utils.ShaderSourceIO;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public class J3dNiParticles extends J3dNiGeometry
{
	//THIS MUST BE SET WHEN SCREEN SIZE CHANGES!!!
	private static float screenWidth = -1;

	private static ShaderAttributeValue screenWidthShaderAttributeValue = new ShaderAttributeValue("screenWidth", new Float(screenWidth));

	static
	{
		screenWidthShaderAttributeValue.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
	}

	public static void setScreenWidth(float newWidth)
	{
		screenWidth = newWidth;
		screenWidthShaderAttributeValue.setValue(new Float(screenWidth));
	}

	private static boolean SHOW_DEBUG_LINES = false;

	// keep for a reset
	//	private NiParticles niParticles;
	//	private NiToJ3dData niToJ3dData;
	//	private TextureSource textureSource;

	protected J3dNiParticlesData j3dNiParticlesData;

	private J3dNiParticleSystemController j3dNiParticleSystemController = null;

	private NiParticleSystemController niParticleSystemController;

	private static WeakListenerList<J3dNiParticles> allParticleSystems = new WeakListenerList<J3dNiParticles>();

	private BranchGroup outlinerBG1 = null;

	private BranchGroup outlinerBG2 = null;

	protected Shape3D shape;

	public J3dNiParticles(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		// the shape will not be added so we can choose to add it to a root we like in a moment
		super(niParticles, niToJ3dData, textureSource, new Shape3D());

		//		this.niParticles = niParticles;
		//		this.niToJ3dData = niToJ3dData;
		//		this.textureSource = textureSource;

		shape = getShape();

		niToJ3dData.put(niParticles, this);
		allParticleSystems.add(this);

		init(niParticles, niToJ3dData, textureSource);

	}

	protected void init(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		if (j3dNiParticlesData != null)
		{
			niParticleSystemController = (NiParticleSystemController) niToJ3dData.get(niParticles.controller);
			if (niParticleSystemController != null)
			{
				shape.setGeometry(j3dNiParticlesData.getGeometryArray());

				//override any default shader appearance
				shape.setAppearance(createAppearance(niParticles.properties, niToJ3dData, textureSource));

				addChild(shape);

				//TODO: is this a good idea? 
				shape.setBoundsAutoCompute(false);
				shape.setBounds(new BoundingSphere(new Point3d(0, 0, 0), 10));

				// prepare a root for outline to be added to
				outlinerBG1 = new BranchGroup();
				outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
				outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);
				addChild(outlinerBG1);
				configureOutLines();

			}
		}
	}

	/**
	 * NOt in fact called because I think things shouldn't be reset ever, even for non looping particles
	 * @param niParticles
	 * @param niToJ3dData
	 * @param textureSource
	 */
	protected void reset(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		j3dNiParticlesData.reset();
		if (niParticleSystemController != null)
		{
			j3dNiParticleSystemController.reset();
		}
	}

	private void configureOutLines()
	{
		//for debug
		if (SHOW_DEBUG_LINES && outlinerBG2 == null)
		{
			//TODO: textures and debug shapes are WAY off from each other

			Shape3D outliner = new Shape3D();
			outliner.setGeometry(j3dNiParticlesData.getGeometryArray());
			outliner.setAppearance(PhysAppearance.makeAppearance());

			outlinerBG2 = new BranchGroup();
			outlinerBG2.setCapability(BranchGroup.ALLOW_DETACH);
			outlinerBG2.addChild(outliner);
			outlinerBG1.addChild(outlinerBG2);
		}
		else if (!SHOW_DEBUG_LINES && outlinerBG2 != null)
		{
			outlinerBG2.detach();
			outlinerBG2 = null;
		}

	}

	/**
	 * Note this override the NiObjectNET method completely
	 * @see nif.j3d.J3dNiObjectNET#setupController(nif.j3d.NiToJ3dData)
	 */
	@Override
	public void setupController(NiToJ3dData niToJ3dData)
	{
		if (niParticleSystemController != null)
		{
			// this is done here because all nodes need to be added as we use get transform tree
			j3dNiParticleSystemController = new J3dNiParticleSystemController(niParticleSystemController, this, j3dNiParticlesData,
					niToJ3dData);

			addChild(j3dNiParticleSystemController);

			setUpModifers(niToJ3dData);

			//FIXME: should I auto fire this guy? how to know when to remove magic effects for example?
			j3dNiParticleSystemController.fireSequenceLooping();
		}

	}

	public void particleCreated(int newParticleId)
	{
		if (newParticleId != -1)
		{
			// now tell all modifiers about the new particles so they can make updates to it (like add rotation etc)
			for (J3dNiParticleModifier j3dNiParticleModifier : j3dNiParticleSystemController.modifiersInOrder)
			{
				j3dNiParticleModifier.particleCreated(newParticleId);
			}
		}
	}

	private boolean modifiersSetup = false;

	private void setUpModifers(NiToJ3dData niToJ3dData)
	{
		// in a next system rather than a ref list

		if (!modifiersSetup)
		{
			NiParticleModifier niParticleModifier = (NiParticleModifier) niToJ3dData.get(niParticleSystemController.particleExtra);

			// for all referenced mods
			while (niParticleModifier != null)
			{
				// ensure it is created
				J3dNiParticleModifier j3dNiParticleModifier = J3dNiParticleModifier.createJ3dNiParticleModifier(niParticleModifier,
						j3dNiParticlesData, niToJ3dData);
				j3dNiParticleSystemController.modifiersInOrder.add(j3dNiParticleModifier);
				niParticleModifier = (NiParticleModifier) niToJ3dData.get(niParticleModifier.nextModifier);
			}

			modifiersSetup = true;
		}
	}

	@Override
	public void setOutline(Color3f c)
	{
		// TODO: needs an indicator color for particles to use, note J3dNiParticleSystem.SHOW_DEBUG_LINES is the system for now

	}

	public boolean isNotRunning()
	{
		if (niParticleSystemController != null)
		{
			return j3dNiParticleSystemController.isNotRunning();
		}
		return true;
	}

	public void fireSequenceLooping()
	{
		if (niParticleSystemController != null)
		{
			j3dNiParticleSystemController.fireSequenceLooping();
		}
	}

	public void fireSequence()
	{
		// non looping always reset the data before firing
		// I feel this is rubbish in fact
		//reset(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)

		if (niParticleSystemController != null)
		{
			j3dNiParticleSystemController.fireSequence();
		}
	}

	public float getLengthS()
	{
		if (niParticleSystemController != null)
		{
			return j3dNiParticleSystemController.getLengthS();
		}
		return 0;
	}

	public long getLengthMS()
	{
		return (long) (getLengthS() * 1000);
	}

	public static boolean isSHOW_DEBUG_LINES()
	{
		return SHOW_DEBUG_LINES;
	}

	public static void setSHOW_DEBUG_LINES(boolean sHOW_DEBUG_LINES)
	{
		SHOW_DEBUG_LINES = sHOW_DEBUG_LINES;
		for (J3dNiParticles ps : allParticleSystems)
		{
			ps.configureOutLines();
		}
	}

	private static ShaderProgram shaderProgram = null;

	public static Appearance createAppearance(NifRef[] props, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		ShaderAppearance app = new ShaderAppearance();
		if (shaderProgram == null)
		{

			String vertexProgram = ShaderSourceIO.getTextFileAsString("shaders/particles.vert");
			String fragmentProgram = ShaderSourceIO.getTextFileAsString("shaders/particles.frag");

			Shader[] shaders = new Shader[2];
			shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
				@Override
				public String toString()
				{
					return "vertexProgram";
				}
			};
			shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram) {
				@Override
				public String toString()
				{
					return "fragmentProgram";
				}
			};

			shaderProgram = new GLSLShaderProgram() {
				@Override
				public String toString()
				{
					return "Particles Shader Program";
				}
			};
			shaderProgram.setShaders(shaders);

			shaderProgram.setShaderAttrNames(new String[] { "BaseMap", "screenWidth" });
			String[] vertexAttrNames = new String[] { "Size", "Rotation" };
			shaderProgram.setVertexAttrNames(vertexAttrNames);
		}

		app.setShaderProgram(shaderProgram);

		app.setMaterial(getMaterial());
		TransparencyAttributes ta = new TransparencyAttributes();

		ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
		if (screenWidth == -1)
			System.out.println("J3dNiParticles.screenWidth must be set for particles to show!!");
		shaderAttributeSet.put(screenWidthShaderAttributeValue);

		for (int p = 0; p < props.length; p++)
		{
			NiProperty prop = (NiProperty) niToJ3dData.get(props[p]);
			if (prop != null)
			{
				if (prop instanceof NiTexturingProperty)
				{
					NiTexturingProperty ntp = (NiTexturingProperty) prop;

					// now set the texture
					if (ntp != null && ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1)
					{
						NiSourceTexture niSourceTexture = (NiSourceTexture) niToJ3dData.get(ntp.baseTexture.source);
						String fileName = niSourceTexture.fileName.string;

						Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
						if (tex == null)
						{
							System.out.println(
									"TextureUnitState bind " + fileName + " no Texture found for nif " + niSourceTexture.nVer.fileName);
							// notice tus left as null!
						}
						else
						{
							//POINT array data can't use mipmaps, texture loader default to nicest min filter
							if(!tex.isLive() && !tex.isCompiled())
								tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
							
							TextureUnitState[] tus = new TextureUnitState[1];
							TextureUnitState tus0 = new TextureUnitState();
							tus0.setTexture(tex);
							tus0.setName(fileName);

							tus[0] = tus0;
							app.setTextureUnitState(tus);

							String textureUnitName = "BaseMap";
							shaderAttributeSet.put(new ShaderAttributeValue(textureUnitName, new Integer(0)));
						}
					}
				}
				else if (prop instanceof NiAlphaProperty)
				{
					NiAlphaProperty nap = (NiAlphaProperty) prop;

					if (nap.alphaBlendingEnable())
					{

						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
						ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.sourceBlendMode(), true));
						ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.destinationBlendMode(), false));
					}

					//if(nap.alphaTestEnabled()	){nap.alphaTestMode(), nap.threshold

				}
				else if (prop instanceof NiMaterialProperty)
				{
					NiMaterialProperty nmp = (NiMaterialProperty) prop;
					Material mat = new Material();
					mat.setLightingEnable(true);
					mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

					if (!(nmp.nVer.LOAD_VER == NifVer.VER_20_2_0_7 && (nmp.nVer.LOAD_USER_VER == 11 || nmp.nVer.LOAD_USER_VER == 12)
							&& nmp.nVer.LOAD_USER_VER2 > 21))
					{
						mat.setAmbientColor(nmp.ambientColor.r, nmp.ambientColor.g, nmp.ambientColor.b);
						mat.setDiffuseColor(nmp.diffuseColor.r, nmp.diffuseColor.g, nmp.diffuseColor.b);
					}

					mat.setEmissiveColor(nmp.emissiveColor.r, nmp.emissiveColor.g, nmp.emissiveColor.b);

					//in nifskope they blend teh nmp.alpha value with teh colors but that makes evrythign dissappear for me when alpha is 0

					if (nmp.alpha != 1.0)
					{
						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
					}

					mat.setShininess(0.0f);
					mat.setSpecularColor(0, 0, 0);

					app.setMaterial(mat);
				}
			}
			app.setTransparencyAttributes(ta);
			app.setShaderAttributeSet(shaderAttributeSet);

			// this is required to turn on the point size feature sometimes
			// note this point size is ignored in the vert shader the point vertex attributes are used
			app.setPointAttributes(new PointAttributes(1, true));
		}

		return app;
	}

	private static Material m;

	public static Material getMaterial()
	{
		if (m == null)
		{
			m = new Material();
			m.setShininess(0);
			m.setDiffuseColor(1.0f, 1.0f, 1.0f);
			m.setSpecularColor(1.0f, 1.0f, 1.0f);
			m.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
		}

		return m;
	}

}
