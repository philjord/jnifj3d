package nif.j3d.particles.tes3;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GLSLShaderProgram;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAppearance;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.ShaderProgram;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SourceCodeShader;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiTexturingProperty;
import nif.niobject.particle.NiAutoNormalParticles;
import nif.niobject.particle.NiAutoNormalParticlesData;
import nif.niobject.particle.NiParticleModifier;
import nif.niobject.particle.NiParticleSystemController;
import tools.WeakListenerList;
import tools3d.utils.PhysAppearance;
import tools3d.utils.ShaderSourceIO;
import utils.PerTimeUpdateBehavior;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public class J3dNiAutoNormalParticles extends J3dNiGeometry implements GeometryUpdater
{
	private static boolean SHOW_DEBUG_LINES = false;

	private J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData;

	private J3dNiParticleSystemController j3dNiParticleSystemController = null;

	private NiParticleSystemController niParticleSystemController;

	private static WeakListenerList<J3dNiAutoNormalParticles> allParticleSystems = new WeakListenerList<J3dNiAutoNormalParticles>();

	private BranchGroup outlinerBG1 = null;

	private BranchGroup outlinerBG2 = null;

	private Shape3D shape;

	public J3dNiAutoNormalParticles(NiAutoNormalParticles niAutoNormalParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		// the shape will not be added so we can choose to add it to a root we like in a moment
		super(niAutoNormalParticles, niToJ3dData, textureSource, new Shape3D());
		shape = getShape();
		shape.clearCapabilities();
		shape.setPickable(false);
		shape.setCollidable(false);

		niToJ3dData.put(niAutoNormalParticles, this);
		NiAutoNormalParticlesData niAutoNormalParticlesData = (NiAutoNormalParticlesData) niToJ3dData.get(niAutoNormalParticles.data);

		if (niAutoNormalParticlesData != null)
		{
			j3dNiAutoNormalParticlesData = new J3dNiAutoNormalParticlesData(niAutoNormalParticlesData);

			niParticleSystemController = (NiParticleSystemController) niToJ3dData.get(niAutoNormalParticles.controller);
			if (niParticleSystemController != null)
			{
				getShape().setGeometry(j3dNiAutoNormalParticlesData.getGeometryArray());

				//override any default shader appearance
				getShape().setAppearance(createAppearance(niAutoNormalParticles.properties, niToJ3dData, textureSource));

				addChild(getShape());

				//TODO: is this a good idea? thread show blocked on update bounds
				getShape().setBoundsAutoCompute(false);
				getShape().setBounds(new BoundingSphere(new Point3d(0, 0, 0), 10));

				// get updated every 50 milliseconds
				addChild(new PerTimeUpdateBehavior(50, new PerTimeUpdateBehavior.CallBack() {
					@Override
					public void update()
					{
						// set this as the geom updater and do the updates when called back (again)
						j3dNiAutoNormalParticlesData.getGeometryArray().updateData(J3dNiAutoNormalParticles.this);
					}
				}));

				// prepare a root for outline to be added to
				outlinerBG1 = new BranchGroup();
				outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
				outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);
				addChild(outlinerBG1);
				configureOutLines();

				allParticleSystems.add(this);
			}
		}
	}

	private void configureOutLines()
	{
		//for debug
		if (SHOW_DEBUG_LINES && outlinerBG2 == null)
		{
			//TODO: textures and debug shapes are WAY off from each other

			Shape3D outliner = new Shape3D();
			outliner.clearCapabilities();
			outliner.setPickable(false);
			outliner.setCollidable(false);
			outliner.setGeometry(j3dNiAutoNormalParticlesData.getGeometryArray());
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
		// this is done here because all nodes need to be added as we use get transform tree
		j3dNiParticleSystemController = new J3dNiParticleSystemController(niParticleSystemController, this,
				j3dNiAutoNormalParticlesData, niToJ3dData);
		
		
		setUpModifers(niToJ3dData);
	}

	@Override
	public void updateData(Geometry geometry)
	{

		if (j3dNiParticleSystemController != null)
		{
			j3dNiParticleSystemController.process();
		}

		//ok instead of this system give all modifiers to the single controller

		// now we tell the particles to update the nett effects
		j3dNiAutoNormalParticlesData.recalcAllGaCoords();

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
						j3dNiAutoNormalParticlesData, niToJ3dData);
				j3dNiParticleSystemController.modifiersInOrder.add(j3dNiParticleModifier);
				niParticleModifier = (NiParticleModifier) niToJ3dData.get(niParticleModifier.nextModifier);
			}

			modifiersSetup = true;
		}
	}

	@Override
	public void setOutline(Color3f c)
	{
		// TODO: needs an indicatr color for particles to use, note J3dNiParticleSystem.SHOW_DEBUG_LINES is the system for now

	}

	public static boolean isSHOW_DEBUG_LINES()
	{
		return SHOW_DEBUG_LINES;
	}

	public static void setSHOW_DEBUG_LINES(boolean sHOW_DEBUG_LINES)
	{
		SHOW_DEBUG_LINES = sHOW_DEBUG_LINES;
		for (J3dNiAutoNormalParticles ps : allParticleSystems)
		{
			ps.configureOutLines();
		}
	}

	private static ShaderProgram shaderProgram = null;

	public static Appearance createAppearance(NifRef[] props, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{

		ShaderAppearance app = new ShaderAppearance();
		app.clearCapabilities();
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

			shaderProgram.setShaderAttrNames(new String[] { "BaseMap" });
			String[] vertexAttrNames = new String[] { "Size" };
			shaderProgram.setVertexAttrNames(vertexAttrNames);
		}

		app.setShaderProgram(shaderProgram);

		app.setMaterial(getMaterial());
		TransparencyAttributes ta = new TransparencyAttributes();

		ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();

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
							TextureUnitState[] tus = new TextureUnitState[1];
							TextureUnitState tus0 = new TextureUnitState();
							tus0.clearCapabilities();
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
