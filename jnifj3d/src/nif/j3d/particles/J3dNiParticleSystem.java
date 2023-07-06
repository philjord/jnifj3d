package nif.j3d.particles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.Geometry;
import org.jogamp.java3d.GeometryUpdater;
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
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.particles.tes3.J3dNiParticles;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiTexturingProperty;
import nif.niobject.bs.BSStripParticleSystem;
import nif.niobject.controller.NiTimeController;
import nif.niobject.particle.NiMeshParticleSystem;
import nif.niobject.particle.NiPSysData;
import nif.niobject.particle.NiPSysModifier;
import nif.niobject.particle.NiPSysModifierCtlr;
import nif.niobject.particle.NiParticleSystem;
import nif.shader.ShaderSourceIO;
import tools.WeakListenerList;
import tools3d.utils.PhysAppearance;
import utils.PerTimeUpdateBehavior;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public class J3dNiParticleSystem extends J3dNiGeometry implements GeometryUpdater
{		
	private static boolean SHOW_DEBUG_LINES = true;

	private ArrayList<J3dNiPSysModifier> modifiersInOrder = new ArrayList<J3dNiPSysModifier>();

	private HashMap<String, J3dNiPSysModifier> modifiersByName = new HashMap<String, J3dNiPSysModifier>();

	public HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr> j3dNiPSysModiferCtlrsByNi = new HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr>();

	public J3dPSysData j3dPSysData;

	private J3dNiPSysModifierCtlr rootJ3dNiPSysModifierCtlr = null;

	private NiParticleSystem niParticleSystem;

	private static WeakListenerList<J3dNiParticleSystem> allParticleSystems = new WeakListenerList<J3dNiParticleSystem>();

	private BranchGroup outlinerBG1 = null;

	private BranchGroup outlinerBG2 = null;

	private Shape3D shape;

	public J3dNiParticleSystem(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{

		// the shape will not be added so we can choose to add it to a root we like in a moment
		super(niParticleSystem, niToJ3dData, textureSource, new Shape3D());
		
		shape = getShape();

		this.niParticleSystem = niParticleSystem;

		niToJ3dData.put(niParticleSystem, this);

		NiPSysData niPSysData = (NiPSysData) niToJ3dData.get(niParticleSystem.data);

		if (niPSysData != null)
		{

			j3dPSysData = new J3dPSysData(niPSysData);

			shape.setGeometry(j3dPSysData.getGeometryArray());
			
			//override any default shader appearance
			shape.setAppearance(createAppearance(niParticleSystem.properties, niToJ3dData, textureSource));

			if (niParticleSystem.worldSpace)
			{
				niToJ3dData.getJ3dRoot().addChildBeforeTrans(shape);
			}
			else
			{
				addChild(shape);
			}

			//TODO: is this a good idea? thread show blocked on update bounds
			shape.setBoundsAutoCompute(false);
			shape.setBounds(new BoundingSphere(new Point3d(0, 0, 0), 10));

			// get updated every 50 milliseconds
			addChild(new PerTimeUpdateBehavior(50, new PerTimeUpdateBehavior.CallBack() {
				@Override
				public void update()
				{
					// set this as the geom updater and do the updates when called back (again)
					j3dPSysData.getGeometryArray().updateData(J3dNiParticleSystem.this);
				}
			}));

			//2 types of sub classes with no extra data
			if (niParticleSystem instanceof BSStripParticleSystem)
			{
				//TODO: do I care?
			}
			else if (niParticleSystem instanceof NiMeshParticleSystem)
			{
				//TODO: do I care?
			}

			// prepare a root for outline to be added to
			outlinerBG1 = new BranchGroup();
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);
			addChild(outlinerBG1);
			configureOutLines();

			allParticleSystems.add(this);
		}
	}

	private void configureOutLines()
	{
		//for debug
		if (SHOW_DEBUG_LINES && outlinerBG2 == null)
		{
			//TODO: textures and debug shapes are WAY off from each other

			Shape3D outliner = new Shape3D();
			outliner.setGeometry(j3dPSysData.getGeometryArray());
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
		setUpModifers(niParticleSystem, niToJ3dData);
		setupControllers(niParticleSystem, niToJ3dData);
	}

	@Override
	public void updateData(Geometry geometry)
	{

		if (rootJ3dNiPSysModifierCtlr != null)
		{
			rootJ3dNiPSysModifierCtlr.process();
		}

		//System.out.println("frame update");
		for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			if (j3dNiPSysModifier.active)
			{
				j3dNiPSysModifier.updatePSys(50L);
			}
		}

		// now we tell the particles to update the nett effects
		j3dPSysData.updateAllTexCoords();
		j3dPSysData.recalcAllGaCoords();
		j3dPSysData.recalcSizes();
		j3dPSysData.recalcRotations();
		j3dPSysData.recalcAllGaColors();
		

	}

	public void particleCreated(int newParticleId)
	{
		if (newParticleId != -1)
		{
			// now tell all modifiers about the new particles so they can make updates to it (like add rotation etc)
			for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder)
			{
				j3dNiPSysModifier.particleCreated(newParticleId);
			}
		}
	}

	private boolean modifiersSetup = false;

	private void setUpModifers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		if (!modifiersSetup)
		{
			// for all referenced mods
			for (NifRef nr : niParticleSystem.modifiers)
			{
				NiPSysModifier niPSysModifier = (NiPSysModifier) niToJ3dData.get(nr);
				// ensure it is created
				getJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
			}

			// sort by the order number
			modifiersInOrder.clear();
			modifiersInOrder.addAll(modifiersByName.values());
			Collections.sort(modifiersInOrder, new Comparator<J3dNiPSysModifier>() {
				@Override
				public int compare(J3dNiPSysModifier o1, J3dNiPSysModifier o2)
				{
					return o1.order < o2.order ? -1 : o1.order == o2.order ? 0 : 1;
				}
			});
			modifiersSetup = true;
		}
	}

	// create controllers
	// I need to ensure all modifers are created as the controllers refer to them only by name
	private void setupControllers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		setUpModifers(niParticleSystem, niToJ3dData);
		NiTimeController cont = (NiTimeController) niToJ3dData.get(niParticleSystem.controller);
		if (cont != null)
		{
			rootJ3dNiPSysModifierCtlr = j3dNiPSysModiferCtlrsByNi.get(cont);
			if (rootJ3dNiPSysModifierCtlr == null)
			{
				rootJ3dNiPSysModifierCtlr = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, cont, niToJ3dData);
			}
		}
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(NiPSysModifier niPSysModifier, NiToJ3dData niToJ3dData)
	{
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(niPSysModifier.name);
		if (j3dNiPSysModifier == null)
		{
			j3dNiPSysModifier = J3dNiPSysModifier.createJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
			if (j3dNiPSysModifier != null)
			{
				modifiersByName.put(niPSysModifier.name, j3dNiPSysModifier);
			}
		}
		return j3dNiPSysModifier;
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(String modifierName)
	{
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(modifierName);
		if (j3dNiPSysModifier == null)
			System.out.println("J3dNiParticleSystem - modifierName " + modifierName + " not found in " + this);
		return j3dNiPSysModifier;
	}

	public J3dNiTimeController getJ3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr, NiToJ3dData niToJ3dData)
	{
		// the controlled modifer will need to be ready
		setUpModifers(niParticleSystem, niToJ3dData);

		J3dNiTimeController j3dNiTimeController = j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
		// sometimes (always?) it's external to the particle system
		if (j3dNiTimeController == null)
		{
			j3dNiTimeController = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, niPSysModifierCtlr, niToJ3dData);
		}

		return j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
	}

	@Override
	public void setOutline(Color3f c)
	{
		// TODO: needs an indicator color for particles to use, note J3dNiParticleSystem.SHOW_DEBUG_LINES is the system for now

	}

	public static boolean isSHOW_DEBUG_LINES()
	{
		return SHOW_DEBUG_LINES;
	}

	public static void setSHOW_DEBUG_LINES(boolean sHOW_DEBUG_LINES)
	{
		SHOW_DEBUG_LINES = sHOW_DEBUG_LINES;
		for (J3dNiParticleSystem ps : allParticleSystems)
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
			shaderProgram.setVertexAttrNames(new String[] { "Size", "Rotation", "SubTextureSize" });
		}

		app.setShaderProgram(shaderProgram);

		app.setMaterial(getMaterial());
		TransparencyAttributes ta = new TransparencyAttributes();

		ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
		if (J3dNiParticles.screenWidthShaderAttributeValue.getValue().equals(new Float(-1)))
			System.out.println("J3dNiParticles.screenWidth must be set for particles to show!!");
		shaderAttributeSet.put(J3dNiParticles.screenWidthShaderAttributeValue);

		for (int p = 0; p < props.length; p++)
		{
			NiProperty prop = (NiProperty) niToJ3dData.get(props[p]);
			if (prop != null)
			{
				//TODO: the NiGeometryAppearance lists heaps more texture thingies!
				// but just get oblivion working for now which is this one
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
			
		}
		app.setTransparencyAttributes(ta);
		app.setShaderAttributeSet(shaderAttributeSet);

		// this is required to turn on the point size feature sometimes
		// note this point size is ignored in the vert shader the point vertex attributes are used
		app.setPointAttributes(new PointAttributes(1, true));

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
