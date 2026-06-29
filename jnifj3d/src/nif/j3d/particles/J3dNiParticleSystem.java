package nif.j3d.particles;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.ColoringAttributes;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.Geometry;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.GeometryUpdater;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.LineAttributes;
import org.jogamp.java3d.Material;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.PointAttributes;
import org.jogamp.java3d.PolygonAttributes;
import org.jogamp.java3d.RenderingAttributes;
import org.jogamp.java3d.Shader;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.ShaderAttributeSet;
import org.jogamp.java3d.ShaderAttributeValue;
import org.jogamp.java3d.ShaderProgram;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.SourceCodeShader;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureAttributes;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.java3d.TriangleArray;
import org.jogamp.java3d.utils.shader.SimpleShaderAppearance;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.enums.VertMode;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiVertexColorProperty;
import nif.niobject.NiZBufferProperty;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSStripParticleSystem;
import nif.niobject.controller.NiTimeController;
import nif.niobject.particle.NiMeshParticleSystem;
import nif.niobject.particle.NiPSysCollider;
import nif.niobject.particle.NiPSysData;
import nif.niobject.particle.NiPSysModifier;
import nif.niobject.particle.NiPSysModifierCtlr;
import nif.niobject.particle.NiParticleSystem;
import nif.shader.ShaderSourceIO;
import tools.WeakListenerList;
import tools3d.utils.PhysAppearance;
import tools3d.utils.Utils3D;
import utils.PerTimeUpdateBehavior;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public class J3dNiParticleSystem extends J3dNiGeometry implements GeometryUpdater {

	public static boolean				DEBUG_DATA						= false;
	public static boolean				MODIFIER_DEBUG_DATA				= true;
	//TODO: when I limit to a single nothign shows :(
	private static final String			SINGLE_SYSTEM_NAME				= "SuperSpray01";							//null;

	//THIS MUST BE SET WHEN SCREEN SIZE CHANGES!!!
	private static float				screenWidth						= -1;
	// NOTE! this screen attribute is used by both Tes3 J3dNiParticles and J3dNiParticleSystem
	public static ShaderAttributeValue	screenWidthShaderAttributeValue	= new ShaderAttributeValue("screenWidth",
			new Float(screenWidth));

	static {
		screenWidthShaderAttributeValue.setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
		screenWidthShaderAttributeValue.setCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE);
	}

	//TODO: I should really accept a glWindow and listen for myself
	public static void setScreenWidth(float newWidth) {
		System.out.println("J3dNiParticle setScreenWidth " + newWidth);
		screenWidth = newWidth;
		screenWidthShaderAttributeValue.setValue(new Float(screenWidth));
	}

	//Used only to publish out show outlines config change
	private static WeakListenerList<J3dNiParticleSystem>		allParticleSystems			= new WeakListenerList<J3dNiParticleSystem>();

	private static boolean										SHOW_DEBUG_LINES			= false;													// flick it on with the beth settings

	private ArrayList<J3dNiPSysModifier>						modifiersInOrder			= new ArrayList<J3dNiPSysModifier>();

	private HashMap<String, J3dNiPSysModifier>					modifiersByName				= new HashMap<String, J3dNiPSysModifier>();

	private HashMap<Integer, J3dNiPSysCollider>					collidersByRefId			= new HashMap<Integer, J3dNiPSysCollider>();

	public HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr>	j3dNiPSysModiferCtlrsByNi	= new HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr>();

	public J3dPSysData											j3dPSysData;

	private J3dNiPSysModifierCtlr								rootJ3dNiPSysModifierCtlr	= null;

	private NiParticleSystem									niParticleSystem;

	private BranchGroup											outlinerBG1					= null;

	private BranchGroup											outlinerBG3					= null;

	private Shape3D												boundsCube;
	//limit outline up dates to every 10 updates
	private int													outlineUpdateCount			= 0;

	private Shape3D												shape;
	private long												sleep						= 50;														//20 fps for updates
	boolean														worldSpace					= false;

	public J3dNiParticleSystem(	NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData,
								TextureSource textureSource) {

		// the shape will not be added so we can choose to add it to a root we like in a moment
		super(niParticleSystem, niToJ3dData, textureSource, new Shape3D());

		shape = getShape();

		this.niParticleSystem = niParticleSystem;

		niToJ3dData.put(niParticleSystem, this);

		NiPSysData niPSysData = (NiPSysData)niToJ3dData.get(niParticleSystem.data);

		if (niPSysData != null) {

			if (DEBUG_DATA) {

				j3dPSysData = new J3dPSysData.J3dPSysDataTest(niPSysData);
				sleep = J3dPSysData.J3dPSysDataTest.SLEEP_OVERRIDE;
				System.out.println("J3dNiParticleSystem created " + niParticleSystem.name);
			} else {
				j3dPSysData = new J3dPSysData(niPSysData);
			}

			shape.setGeometry(j3dPSysData.getGeometryArray());

			//override any default shader appearance
			shape.setAppearance(createAppearance(niParticleSystem, niToJ3dData, textureSource));

			// prepare a root for outline to be added to
			outlinerBG1 = new BranchGroup();
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);

			this.worldSpace = niParticleSystem.worldSpace;
			if (worldSpace) {
				niToJ3dData.getJ3dRoot().addChildBeforeTrans(shape);
				niToJ3dData.getJ3dRoot().addChildBeforeTrans(outlinerBG1);
			} else {

				// this one will make emitter etc a real pain, watch out
				addChild(shape);
				addChild(outlinerBG1);
			}

			//replaced on each updateData call
			shape.setBoundsAutoCompute(false);
			shape.setBounds(new BoundingSphere(new Point3d(0, 0, 0), 10));
			shape.setCapability(Shape3D.ALLOW_BOUNDS_WRITE);

			// get updated every sleep milliseconds (defaults to 50)
			addChild(new PerTimeUpdateBehavior(sleep, new PerTimeUpdateBehavior.CallBack() {
				@Override
				public void update() {

					// don't update non singels
					//	if(SINGLE_SYSTEM_NAME != null && !niParticleSystem.name.equals(SINGLE_SYSTEM_NAME))				
					//		return;

					// set this as the geom updater and do the updates when called back (again)
					j3dPSysData.getGeometryArray().updateData(J3dNiParticleSystem.this);
				}
			}));

			//2 types of sub classes with no extra data
			if (niParticleSystem instanceof BSStripParticleSystem) {
				//TODO: do I care?
			} else if (niParticleSystem instanceof NiMeshParticleSystem) {
				//TODO: do I care?
			}

			// find out if teh debugs lines is already on and put some outlines on if so
			configureOutLines();
			// put it in the pile for outline notices
			allParticleSystems.add(this);
		}
	}


	private void configureOutLines() {
		//for debug
		if (SHOW_DEBUG_LINES) {
			if (outlinerBG3 == null) {
				Point3d ld = new Point3d();
				Point3d ud = new Point3d();
				outlinerBG3 = new BranchGroup();
				outlinerBG3.setCapability(Node.ALLOW_PARENT_READ);
				outlinerBG3.setCapability(BranchGroup.ALLOW_DETACH);
				boundsCube = bhkBoxShape(ld, ud);
				boundsCube.setAppearance(makeOutlineApp());
				outlinerBG3.addChild(boundsCube);
			}
			if (outlinerBG3.getParent() == null) {
				outlinerBG1.addChild(outlinerBG3);
			}
		}
		if (!SHOW_DEBUG_LINES && outlinerBG3 != null) {
			outlinerBG3.detach();
		}

	}

	/**
	 * Note this override the NiObjectNET method completely
	 * @see nif.j3d.J3dNiObjectNET#setupController(nif.j3d.NiToJ3dData)
	 */
	@Override
	public void setupController(NiToJ3dData niToJ3dData) {
		//	if (DEBUG_DATA) {
		//		if(SINGLE_SYSTEM_NAME != null && !niParticleSystem.name.equals(SINGLE_SYSTEM_NAME))
		//			return;
		//	}
		setUpModifers(niParticleSystem, niToJ3dData);
		setupControllers(niParticleSystem, niToJ3dData);
	}

	@Override
	public void updateData(Geometry geometry) {
		if (rootJ3dNiPSysModifierCtlr != null) {
			rootJ3dNiPSysModifierCtlr.process();
		}

		// age all the particles now TODO: I notice spawn time and now also give particle age?
		for (int pId = 0; pId < j3dPSysData.activeParticleCount; pId++) {
			j3dPSysData.particleAge[pId] += sleep;
		}

		for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder) {
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			if (j3dNiPSysModifier.active) {
				j3dNiPSysModifier.updatePSys(sleep);
			}
		}

		// now we tell the particles to update the nett effects
		j3dPSysData.updateAllTexCoords();
		j3dPSysData.recalcRotations();
		j3dPSysData.recalcAllGaColors();
		j3dPSysData.recalcSizes();
		j3dPSysData.recalcAllGaCoords();

		//FIXME: there is a note say the bounds of particles is not used and is set to Util3D.defautlBounds
		// investigate this
		shape.setBounds(j3dPSysData.bounds);
		//System.out.println("bounds set to "+j3dPSysData.bounds);

		//System.out.println("active count set to "+j3dPSysData.activeParticleCount);

		if (SHOW_DEBUG_LINES) {
			outlineUpdateCount++;
			if (outlineUpdateCount >= 10) {
				outlineUpdateCount = 0;

				if (j3dPSysData.activeParticleCount > 0) {
					if (outlinerBG3 != null) {
						//Point3d c = new Point3d();
						Point3d ld = new Point3d();
						Point3d ud = new Point3d();
						//j3dPSysData.bounds.getCenter(c);
						j3dPSysData.bounds.getLower(ld);
						j3dPSysData.bounds.getUpper(ud);

						J3DBuffer b = ((TriangleArray)boundsCube.getGeometry()).getCoordRefBuffer();
						FloatBuffer buff = (FloatBuffer)b.getBuffer();
						buff.rewind();
						buff.put(bhkBoxCoords(ld, ud));

						//System.out.println("particles "+ j3dPSysData.activeParticleCount+ " "+j3dPSysData.maxParticleCount+" bounds " + j3dPSysData.bounds);
					}
				}
			}

		}
	}

	public void particleCreated(int newParticleId) {
		if (newParticleId != -1) {
			// now tell all modifiers about the new particles so they can make updates to it (like add rotation etc)
			for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder) {
				j3dNiPSysModifier.particleCreated(newParticleId);
			}
		}
	}

	private boolean modifiersSetup = false;

	private void setUpModifers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData) {
		if (!modifiersSetup) {
			// for all referenced mods
			for (NifRef nr : niParticleSystem.modifiers) {
				NiPSysModifier niPSysModifier = (NiPSysModifier)niToJ3dData.get(nr);
				// ensure it is created
				getJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
			}

			// sort by the order number
			modifiersInOrder.clear();
			modifiersInOrder.addAll(modifiersByName.values());
			Collections.sort(modifiersInOrder, new Comparator<J3dNiPSysModifier>() {
				@Override
				public int compare(J3dNiPSysModifier o1, J3dNiPSysModifier o2) {
					return o1.order < o2.order ? -1 : o1.order == o2.order ? 0 : 1;
				}
			});
			modifiersSetup = true;
		}
	}

	// create controllers
	// I need to ensure all modifers are created as the controllers refer to them only by name
	private void setupControllers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData) {
		setUpModifers(niParticleSystem, niToJ3dData);
		NiTimeController cont = (NiTimeController)niToJ3dData.get(niParticleSystem.controller);
		if (cont != null) {
			rootJ3dNiPSysModifierCtlr = j3dNiPSysModiferCtlrsByNi.get(cont);
			if (rootJ3dNiPSysModifierCtlr == null) {
				rootJ3dNiPSysModifierCtlr = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, cont, niToJ3dData);
			}
		}
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(NiPSysModifier niPSysModifier, NiToJ3dData niToJ3dData) {
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(niPSysModifier.name);
		if (j3dNiPSysModifier == null) {
			j3dNiPSysModifier = J3dNiPSysModifier.createJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
			if (j3dNiPSysModifier != null) {
				modifiersByName.put(niPSysModifier.name, j3dNiPSysModifier);
			}
		}
		return j3dNiPSysModifier;
	}

	public J3dNiPSysCollider getJ3dNiPSysCollider(NiPSysCollider niPSysCollider, NiToJ3dData niToJ3dData) {
		J3dNiPSysCollider j3dNiPSysCollider = collidersByRefId.get(niPSysCollider.refId);
		if (j3dNiPSysCollider == null) {
			j3dNiPSysCollider = J3dNiPSysCollider.createJ3dNiPSysCollider(niPSysCollider, niToJ3dData, this);
			if (j3dNiPSysCollider != null) {
				collidersByRefId.put(niPSysCollider.refId, j3dNiPSysCollider);
			}
		}
		return j3dNiPSysCollider;
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(String modifierName) {
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(modifierName);
		if (j3dNiPSysModifier == null)
			System.out.println("J3dNiParticleSystem - modifierName " + modifierName + " not found in " + this);
		return j3dNiPSysModifier;
	}

	public J3dNiTimeController getJ3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr,
														NiToJ3dData niToJ3dData) {
		// the controlled modifier will need to be ready
		setUpModifers(niParticleSystem, niToJ3dData);

		J3dNiTimeController j3dNiTimeController = j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
		// sometimes (always?) it's external to the particle system
		if (j3dNiTimeController == null) {
			j3dNiTimeController = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, niPSysModifierCtlr,
					niToJ3dData);
		}

		return j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
	}

	@Override
	public void setOutline(Color3f c) {
		// TODO: needs an indicator color for particles to use, note J3dNiParticleSystem.SHOW_DEBUG_LINES is the system for now

	}

	public static boolean isSHOW_DEBUG_LINES() {
		return SHOW_DEBUG_LINES;
	}

	public static void setSHOW_DEBUG_LINES(boolean sHOW_DEBUG_LINES) {
		SHOW_DEBUG_LINES = sHOW_DEBUG_LINES;
		for (J3dNiParticleSystem ps : allParticleSystems) {
			ps.configureOutLines();
		}
	}

	private static ShaderProgram shaderProgram = null;

	public static Appearance createAppearance(	NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData,
												TextureSource textureSource) {
		NifRef[] props = niParticleSystem.properties;
		ShaderAppearance app = new ShaderAppearance();
		if (shaderProgram == null) {

			// also used by fallout3 ob is with atlas textures set to 1x1
			String vertexProgramStr = "shaders/ob_particles.vert";
			String fragmentProgramStr = "shaders/ob_particles.frag";

			if (niToJ3dData.nifVer.LOAD_VER == NifVer.VER_20_2_0_7) {
				if (niToJ3dData.nifVer.BS_GT_FO3()) {
					//vertexProgramStr = "shaders/sk_particles.vert";
					//fragmentProgramStr = "shaders/sk_particles.frag";
				}
			}

			if (DEBUG_DATA) {
				vertexProgramStr = "shaders/debug_particles.vert";
				fragmentProgramStr = "shaders/debug_particles.frag";
			}

			String vertexProgram = ShaderSourceIO.getTextFileAsString(vertexProgramStr);
			String fragmentProgram = ShaderSourceIO.getTextFileAsString(fragmentProgramStr);

			Shader[] shaders = new Shader[2];
			shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
				@Override
				public String toString() {
					return "vertexProgram";
				}
			};
			shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT,
					fragmentProgram) {
				@Override
				public String toString() {
					return "fragmentProgram";
				}
			};

			shaderProgram = new GLSLShaderProgram() {
				@Override
				public String toString() {
					return "Particles Shader Program";
				}
			};
			shaderProgram.setShaders(shaders);

			shaderProgram.setShaderAttrNames(new String[] {"BaseMap", "screenWidth"});

			// gaVsizesF, gaVrcosF, gaVrsinF, gaVsubTextureSizeF in J3dPSysData but the name is not used again only the index of 0,1,2
			shaderProgram.setVertexAttrNames(new String[] {"Size", "rCos", "rSin", "SubTextureSize"});
		}

		app.setShaderProgram(shaderProgram);

		ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
		if (screenWidthShaderAttributeValue.getValue().equals(new Float(-1)))
			System.err.println("J3dNiParticleSystem.screenWidth must be set for particles to show!!");
		shaderAttributeSet.put(screenWidthShaderAttributeValue);

		// many properties touch these parts
		Material mat = getDefaultMaterial();
		TransparencyAttributes ta = new TransparencyAttributes();

		for (int p = 0; p < props.length; p++) {
			NiProperty prop = (NiProperty)niToJ3dData.get(props[p]);
			if (prop != null) {
				//TODO: the NiGeometryAppearanceShader lists heaps more texture thingies!
				// but just get oblivion working for now which is this one
				if (prop instanceof NiTexturingProperty) {
					NiTexturingProperty ntp = (NiTexturingProperty)prop;

					// now set the texture
					if (ntp != null && ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1) {
						NiSourceTexture niSourceTexture = (NiSourceTexture)niToJ3dData.get(ntp.baseTexture.source);
						String fileName = niSourceTexture.fileName.string;

						if (DEBUG_DATA) {
							System.out.println(
									"J3dNiParticleSystem " + niParticleSystem.name + " baseTexture " + fileName);
						}

						Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
						if (tex == null) {
							System.out.println("TextureUnitState bind " + fileName + " no Texture found for nif "
												+ niSourceTexture.nVer.fileName);
							// notice tus left as null!
						} else {
							//POINT array data can't use mipmaps, texture loader default to nicest min filter
							if (!tex.isLive() && !tex.isCompiled())
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
				} else if (prop instanceof NiAlphaProperty) {
					NiAlphaProperty nap = (NiAlphaProperty)prop;

					if (nap.alphaBlendingEnable()) {

						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
						ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.sourceBlendMode(), true));
						ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.destinationBlendMode(), false));
					}

					//if(nap.alphaTestEnabled()	){nap.alphaTestMode(), nap.threshold

				} else if (prop instanceof NiMaterialProperty) {
					NiMaterialProperty nmp = (NiMaterialProperty)prop;

					if (!(nmp.nVer.LOAD_VER == NifVer.VER_20_2_0_7
							&& (nmp.nVer.LOAD_USER_VER == 11 || nmp.nVer.LOAD_USER_VER == 12)
							&& nmp.nVer.BS_Version > 21)) {
						mat.setAmbientColor(nmp.ambientColor.r, nmp.ambientColor.g, nmp.ambientColor.b);
						mat.setDiffuseColor(nmp.diffuseColor.r, nmp.diffuseColor.g, nmp.diffuseColor.b);
					}

					mat.setEmissiveColor(nmp.emissiveColor.r, nmp.emissiveColor.g, nmp.emissiveColor.b);

					//in nifskope they blend the nmp.alpha value with the colors but that makes everything disappear for me when alpha is 0
					// but this might be a screen door alpha not translucency
					if (nmp.alpha != 1.0) {
						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
					}

					app.setMaterial(mat);
				} else if (prop instanceof BSEffectShaderProperty) {
					//skyrim at least (this and an NiAlphaProperty seen in testing)
					BSEffectShaderProperty bsesp = (BSEffectShaderProperty)prop;

					// now set the texture						
					String fileName0 = bsesp.SourceTexture;
					String fileName1 = bsesp.GreyscaleTexture;//gradblood, this guy is color
					if (DEBUG_DATA) {
						System.out
								.println("J3dNiParticleSystem " + niParticleSystem.name + " baseTexture " + fileName0);
						System.out.println(
								"J3dNiParticleSystem " + niParticleSystem.name + " GreyscaleTexture " + fileName1);
					}

					Texture tex0 = J3dNiGeometry.loadTexture(fileName0, textureSource);
					if (fileName0 != null && fileName0.length() > 0 && tex0 == null) {
						System.out.println("TextureUnitState SourceTexture bind "	+ fileName0
											+ " Texture not found for nif " + bsesp.nVer.fileName);
					} else {

						if (DEBUG_DATA) {
							System.out.println("TextureUnitState SourceTexture bind " + fileName0);

						}

						//POINT array data can't use mipmaps, texture loader default to nicest min filter
						if (!tex0.isLive() && !tex0.isCompiled())
							tex0.setMinFilter(Texture.BASE_LEVEL_LINEAR);

						TextureUnitState[] tus = new TextureUnitState[1];
						TextureUnitState tus0 = new TextureUnitState();
						tus0.setTexture(tex0);
						tus0.setName(fileName0);

						if (bsesp.UVOffSet.u != 0	|| bsesp.UVOffSet.v != 0 || bsesp.UVScale.u != 1
							|| bsesp.UVScale.v != 1 || bsesp.controller.ref != -1) {
							TextureAttributes textureAttributes = new TextureAttributes();
							Transform3D transform = new Transform3D();
							transform.setTranslation(new Vector3d(-bsesp.UVOffSet.u, -bsesp.UVOffSet.v, 0));
							transform.setScale(new Vector3d(bsesp.UVScale.u, bsesp.UVScale.v, 0));
							textureAttributes.setTextureTransform(transform);
							tus0.setTextureAttributes(textureAttributes);
						}

						tus[0] = tus0;
						app.setTextureUnitState(tus);

						String textureUnitName = "BaseMap";
						shaderAttributeSet.put(new ShaderAttributeValue(textureUnitName, new Integer(0)));
					}

					Texture tex1 = J3dNiGeometry.loadTexture(fileName1, textureSource);
					if (fileName1 != null && fileName1.length() > 0 && tex1 == null) {
						System.out.println("TextureUnitState GreyscaleTexture bind "	+ fileName1
											+ " Texture not found for nif " + bsesp.nVer.fileName);
						// notice tus left as null!
					} else {
						if (DEBUG_DATA) {
							System.out.println("TextureUnitState GreyscaleTexture bind " + fileName1);
						}
						//umm?? FIXME:
						//probably by opeing the NiShaderAppearance code and having a wee looky loo
					}

					mat.setDiffuseColor(bsesp.BaseColor.r, bsesp.BaseColor.g, bsesp.BaseColor.b);
					if (DEBUG_DATA)
						System.out.println(
								"J3dNiParticleSystem " + niParticleSystem.name + " color = " + bsesp.BaseColor);

				} else if (prop instanceof NiVertexColorProperty) {
					NiVertexColorProperty nvcp = (NiVertexColorProperty)prop;
					if (nvcp.vertexMode != null) {
						if (nvcp.vertexMode.mode == VertMode.VERT_MODE_SRC_IGNORE) {
							RenderingAttributes ra = app.getRenderingAttributes();
							if (ra == null)
								ra = new RenderingAttributes();
							app.setRenderingAttributes(ra);
							ra.setIgnoreVertexColors(true);
						} else {
							mat.setColorTarget(NifOpenGLToJava3D.convertVertexMode(nvcp.vertexMode.mode));
						}
					}
				} else if (prop instanceof NiZBufferProperty) {
					//no other appearance uses this yet
				} else {
					System.out.println(
							"J3dNiParticleSystem " + niParticleSystem.name + " property not investigated " + prop);

				}
			}

		}
		app.setMaterial(mat);
		app.setTransparencyAttributes(ta);
		app.setShaderAttributeSet(shaderAttributeSet);

		// this is required to turn on the point size feature sometimes
		// note this point size is ignored in the vert shader the point vertex attributes are used
		app.setPointAttributes(new PointAttributes(1, true));

		return app;
	}

	public static Material getDefaultMaterial() {

		Material m = new Material();
		m.setShininess(0);
		m.setDiffuseColor(1.0f, 1.0f, 1.0f);
		m.setSpecularColor(0, 0, 0); //particles not shiny
		m.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

		return m;
	}

	public static Appearance makeOutlineApp() {
		Color3f c = new Color3f(0, 1, 1);
		//Outliner gear, note empty geom should be ignored
		Appearance app = new SimpleShaderAppearance(c);
		// lineAntialiasing MUST be true, to force this to be done during rendering pass (otherwise it's hidden)
		LineAttributes la = new LineAttributes(4, LineAttributes.PATTERN_SOLID, true);
		app.setLineAttributes(la);
		PolygonAttributes pa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, 0.0f,
				true, 0.0f);
		app.setPolygonAttributes(pa);
		ColoringAttributes colorAtt = new ColoringAttributes(c, ColoringAttributes.FASTEST);
		app.setColoringAttributes(colorAtt);

		RenderingAttributes ra2 = new RenderingAttributes();
		ra2.setIgnoreVertexColors(true);
		// draw it even when hidden
		ra2.setDepthBufferEnable(false);
		ra2.setDepthTestFunction(RenderingAttributes.ALWAYS);

		app.setRenderingAttributes(ra2);
		return app;
	}

	private static final int defaultFormat = GeometryArray.COORDINATES	| GeometryArray.BY_REFERENCE
												| GeometryArray.USE_NIO_BUFFER;

	private static Shape3D bhkBoxShape(Point3d ld, Point3d ud) {
		TriangleArray cube = new TriangleArray(36, defaultFormat);

		J3DBuffer coords = new J3DBuffer(Utils3D.makeFloatBuffer(bhkBoxCoords(ld, ud)));

		cube.setCoordRefBuffer(coords);
		//cube.setCapability(TriangleArray.ALLOW_COORDINATE_WRITE);
		cube.setCapability(TriangleArray.ALLOW_REF_DATA_READ);

		// Put geometry into Shape3d
		Shape3D shape = new Shape3D();
		shape.setGeometry(cube);
		shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);

		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}

	private static float[] bhkBoxCoords(Point3d ld, Point3d ud) {
		Point3f l = new Point3f(ld);
		Point3f u = new Point3f(ud);

		float[] scaledVerts = new float[] {
			// front face
			u.x, l.y, u.z, //1
			u.x, u.y, u.z, //2
			l.x, u.y, u.z, //3
			u.x, l.y, u.z, //1
			l.x, u.y, u.z, //3
			l.x, l.y, u.z, //4
			// back face
			l.x, l.y, l.z, //1
			l.x, u.y, l.z, //2
			u.x, u.y, l.z, //3
			l.x, l.y, l.z, //1
			u.x, u.y, l.z, //3
			u.x, l.y, l.z, //4
			// right face
			u.x, l.y, l.z, //1
			u.x, u.y, l.z, //2
			u.x, u.y, u.z, //3
			u.x, l.y, l.z, //1
			u.x, u.y, u.z, //3
			u.x, l.y, u.z, //4
			// left face
			l.x, l.y, u.z, //1
			l.x, u.y, u.z, //2
			l.x, u.y, l.z, //3
			l.x, l.y, u.z, //1
			l.x, u.y, l.z, //3
			l.x, l.y, l.z, //4
			// top face
			u.x, u.y, u.z, //1
			u.x, u.y, l.z, //2
			l.x, u.y, l.z, //3
			u.x, u.y, u.z, //1
			l.x, u.y, l.z, //3
			l.x, u.y, u.z, //4
			// bottom face
			l.x, l.y, u.z, //1
			l.x, l.y, l.z, //2
			u.x, l.y, l.z, //3
			l.x, l.y, u.z, //1
			u.x, l.y, l.z, //3
			u.x, l.y, u.z,};//4

		return scaledVerts;
	}

}
