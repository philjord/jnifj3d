package nif.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.ImageComponent;
import org.jogamp.java3d.ImageComponent2D;
import org.jogamp.java3d.Material;
import org.jogamp.java3d.PolygonAttributes;
import org.jogamp.java3d.RenderingAttributes;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.ShaderAttribute;
import org.jogamp.java3d.ShaderAttributeArray;
import org.jogamp.java3d.ShaderAttributeObject;
import org.jogamp.java3d.ShaderAttributeSet;
import org.jogamp.java3d.ShaderAttributeValue;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureAttributes;
import org.jogamp.java3d.TextureCubeMap;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Matrix3f;
import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Vector2f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;
import org.jogamp.vecmath.Vector4f;

import nif.NifVer;
import nif.appearance.NiGeometryAppearanceFixed;
import nif.compound.NifColor3;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifTexCoord;
import nif.enums.BSLightingShaderType;
import nif.enums.BSShaderType155;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags1;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.TexClampMode;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiSpecularProperty;
import nif.niobject.NiStencilProperty;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiWireframeProperty;
import nif.niobject.NiZBufferProperty;
import nif.niobject.bgsm.BSMaterial;
import nif.niobject.bgsm.BSMaterialDataBGEM;
import nif.niobject.bgsm.BSMaterialDataBGSM;
import nif.niobject.bgsm.bsmatcdb.BSMaterialsCDB.CE2Material;
import nif.niobject.bgsm.bsmatcdb.BSMaterialsCDB.FloatVector4;
import nif.niobject.bgsm.bsmatcdb.BSMaterialsCDB.CE2BSMaterial;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSGeometry;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSShaderLightingProperty;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiTextureTransformController;
import nif.niobject.controller.NiTimeController;
import nif.shader.ShaderPrograms.Program;

import utils.convert.NifOpenGLToJava3D;
import utils.source.MaterialsSource;
import utils.source.TextureSource;

/**
 * This will build an appearance up out of a NiGeometry that can be used by a real j3dnigeometry It is based on the
 * nifskope 2.0 renderer code from jonwd7
 * 
 * TODO: The SKYRIM TREE ANIM code in the bind, is useless but should be put into a new shader type
 * 
 * https://gist.github.com/patriciogonzalezvivo/3a81453a24a542aabc63 looks like some real good lighting equations
 * 
 * possibly this sort of file from fo76utils might also have some shader binding code
 * https://github.com/fo76utils/nifskope/blob/develop/src/gl/glproperty.cpp#L1006
 */

public class NiGeometryAppearanceShader {
	public static boolean						OUTPUT_BINDINGS				= false;

	public static Material						defaultMaterial				= null;

	private NiAVObject							niAVObject;
	private NiToJ3dData							niToJ3dData;
	private TextureSource						textureSource;
	private Shape3D								shape;
	private J3dNiAVObject						target;

	private PropertyList						props;

	private ShaderAppearance					app							= new ShaderAppearance();
	private RenderingAttributes					ra							= new RenderingAttributes();
	private PolygonAttributes					pa							= new PolygonAttributes();
	private Vector2f							textureScale				= new Vector2f(1, 1);
	private Vector2f							textureOffset				= new Vector2f(0, 0);
	private TransparencyAttributes				ta							= new TransparencyAttributes();

	private GLSLShaderProgram2					shaderProgram				= null;

	private ArrayList<ShaderAttributeObject>	allShaderAttributeValues	= new ArrayList<ShaderAttributeObject>();
	private ArrayList<Binding>					allTextureUnitStateBindings	= new ArrayList<Binding>();

	private int									texunit						= 0;

	private static class Binding {
		public boolean		CUBE_MAP	= false;
		public String		samplerName	= "";
		public String		fileName	= "";
		public TexClampMode	clampType	= TexClampMode.WRAP_S_WRAP_T;

		public Binding(String samplerName, String fileName, TexClampMode clampType) {
			this.samplerName = samplerName;
			this.fileName = fileName;
			this.clampType = clampType;
		}

		public Binding(String samplerName, String fileName, boolean cubeMap) {
			this.samplerName = samplerName;
			this.fileName = fileName;
			this.CUBE_MAP = cubeMap;
		}
	}
/**
 * Only NiGeometry or BSGeometry allowed
 * @param niAVObject
 * @param niToJ3dData
 * @param textureSource
 * @param shape
 * @param target
 */
	public NiGeometryAppearanceShader(	NiAVObject niAVObject, NiToJ3dData niToJ3dData, TextureSource textureSource,
										Shape3D shape, J3dNiAVObject target) {
		
		if(!(niAVObject instanceof NiGeometry || niAVObject instanceof BSGeometry)) {
			throw new RuntimeException("Only NiGeometry or BSGeometry allowed " + niAVObject);
		}
		this.niAVObject = niAVObject;
		this.niToJ3dData = niToJ3dData;
		this.textureSource = textureSource;
		this.shape = shape;
		this.target = target;

		props = new PropertyList(niAVObject.properties, niToJ3dData);

		//ensure tangents loaded to geometries
		J3dNiTriBasedGeom.TANGENTS_BITANGENTS = true;

		//configure app defaults
		if (defaultMaterial == null) {
			defaultMaterial = new Material();
			defaultMaterial.setLightingEnable(true);
			defaultMaterial.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
			defaultMaterial.setAmbientColor(new Color3f(0.4f, 0.4f, 0.4f));
			defaultMaterial.setDiffuseColor(new Color3f(0.8f, 0.8f, 0.8f));
			defaultMaterial.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));

			defaultMaterial.setShininess(33f);//33 cos jonwd7 says it's a good default
		}
		app.setMaterial(defaultMaterial);

		// These three are only set if it moves from the default values  
		//app.setRenderingAttributes(ra);
		//app.setPolygonAttributes(pa);	
		//app.setTransparencyAttributes(ta);

	}

	public Appearance getAppearance() {
		return app;
	}

	public String setupShaderProgram() {
		ShaderPrograms.loadShaderPrograms();

		for (ShaderPrograms.Program program : ShaderPrograms.programs.values()) {
			//System.out.println("program checked " + program);
			if (program.isStatusOk()) {
				if (niAVObject instanceof NiGeometry) {
					if (program.eval((NiGeometry)niAVObject, niToJ3dData, props) && setupProgram(program))
						return program.getName();
				} else if (niAVObject instanceof BSGeometry) {
					// new BSMaterialsDB system uses a new list of shader setup 
					if (program.eval((BSGeometry)niAVObject, niToJ3dData, props) && setupProgramCE2(program))
						return program.getName();
				} else {
					throw new RuntimeException("Only NiGeometry or BSGeometry allowed " + niAVObject);
				}
			}
		}

		if (niAVObject instanceof NiGeometry) {
			System.err.println("ARRRRRRRRRRRRRRRRRRRRRRGGGH FFP attempt " + niAVObject.nVer.fileName);
		}else if (niAVObject instanceof BSGeometry) {
			// no comment, things like Meshes\Markers\EditorMarkers\MarkerDummyA.nif have no material
		}
		//null mean use fixed
		return null;
	}

	//https://github.com/niftools/nifskope/blob/3a85ac55e65cc60abc3434cc4aaca2a5cc712eef/src/gl/renderer.cpp#L643
	//https://github.com/niftools/nifskope/blob/develop/src/gl/renderer.cpp
	private boolean setupProgram(ShaderPrograms.Program prog) {

		if (OUTPUT_BINDINGS)
			System.out.println("using prog " + prog.getName());

		this.shaderProgram = prog.shaderProgram;
		GLSLShaderProgram2.ALLOW_ANY_UNIFORM_NAME = false;

		// note time controllers below need appearance set on the shape now
		shape.setAppearance(app);

		// 3.1 and down NiTextureProperty NiMultiTextureProperty
		// not seen often BSSkyShaderProperty? BSWaterShaderProperty?
		// NiTexturingPropertyC 1 BSShaderLightingPropertyC 1, then bsprop is overridden by texprop
		// BSShaderLightingProperty has dozens of sub classes like BSShaderPPLightingProperty,  BSShaderNoLightingProperty  
		// BSEffectShaderProperty appears to always be alone

		NiTexturingProperty texprop = (NiTexturingProperty)props.get(NiTexturingProperty.class);
		BSShaderLightingProperty bsprop = (BSShaderLightingProperty)props.get(BSShaderLightingProperty.class);
		BSLightingShaderProperty bslsp = props.getBSLightingShaderProperty();
		BSSkyShaderProperty skyShaderProps = (BSSkyShaderProperty)props.get(BSSkyShaderProperty.class);
		BSWaterShaderProperty waterShaderProps = (BSWaterShaderProperty)props.get(BSWaterShaderProperty.class);

		TexClampMode clamp = TexClampMode.WRAP_S_WRAP_T;

		if (texprop != null || bsprop != null || bslsp != null) {
			if (bslsp != null) {
				clamp = bslsp.TextureClampMode;
			}

			String textureUnitName = "BaseMap";
			if (texprop != null) {
				registerBind(textureUnitName, fileName(texprop), clamp);
			} else if (bsprop != null) {
				registerBind(textureUnitName, fileName(bsprop, 0), clamp);
			} else if (bslsp != null) {
				registerBind(textureUnitName, fileName(bslsp, 0), clamp);
			}

			textureUnitName = "NormalMap";
			if (shaderProgram.programHasVar(textureUnitName)) {
				if (texprop != null) {
					String fname = fileName(texprop);

					if (fname != null && !fname.isEmpty()) {
						int pos = fname.indexOf("_");

						if (pos >= 0)
							fname = fname.substring(0, pos) + "_n.dds";
						else if ((pos = fname.lastIndexOf(".")) >= 0)
							fname = fname.substring(0, pos) + "_n" + fname.substring(pos);

						registerBind(textureUnitName, fname, clamp);
					}
				} else if (bsprop != null) {
					registerBind(textureUnitName, fileName(bsprop, 1), clamp);
				} else if (bslsp != null) {
					registerBind(textureUnitName, fileName(bslsp, 1), clamp);
				}
			}

			textureUnitName = "GlowMap";
			if (shaderProgram.programHasVar(textureUnitName)) {
				if (texprop != null) {
					String fname = fileName(texprop);

					if (fname != null && !fname.isEmpty()) {
						int pos = fname.indexOf("_");

						if (pos >= 0)
							fname = fname.substring(0, pos) + "_g.dds";
						else if ((pos = fname.lastIndexOf(".")) >= 0)
							fname = fname.substring(0, pos) + "_g" + fname.substring(pos);

						registerBind(textureUnitName, fname, clamp);
					}
				} else if (bsprop != null) {
					registerBind(textureUnitName, fileName(bsprop, 2), clamp);
				} else if (bslsp != null) {
					BSMaterialDataBGSM sm = getMaterial(bslsp);
					if (sm == null)
						registerBind(textureUnitName, fileName(bslsp, 2), clamp);
					else
						registerBind(textureUnitName, fileName(bslsp, 5), clamp);
				}
			}
		}
		//with materials glowmap is in fact 5! and spec is now 2 not 2 and 7 as previous

		// BSLightingShaderProperty
		if (bslsp != null) {
			BSMaterialDataBGSM sm = getMaterial(bslsp);

			uni1f("lightingEffect1", bslsp.LightingEffect1);
			uni1f("lightingEffect2", bslsp.LightingEffect2);

			if (sm == null)
				uni1f("alpha", bslsp.Alpha);
			else
				uni1f("alpha", sm.fAlpha);

			if (sm == null) {
				if (bslsp.UVScale != null)
					textureScale.set(bslsp.UVScale.u, bslsp.UVScale.v);
				if (bslsp.UVOffSet != null)
					textureOffset.set(bslsp.UVOffSet.u, bslsp.UVOffSet.v);
			} else {
				textureScale.set(sm.fUScale, sm.fVScale);
				textureOffset.set(sm.fUOffset, sm.fVOffset);
			}

			boolean hasGreyScaleColor = SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteColor);
			if (sm != null)
				hasGreyScaleColor = sm.bGrayscaleToPaletteColor != 0;

			uni1i("greyscaleColor", hasGreyScaleColor);
			if (hasGreyScaleColor) {
				registerBind("GreyscaleMap", fileName(bslsp, 3), TexClampMode.MIRRORED_S_MIRRORED_T);
			}

			boolean hasTintColor = bslsp.HairTintColor != null || bslsp.SkinTintColor != null;
			uni1i("hasTintColor", hasTintColor);
			if (hasTintColor) {
				NifColor3 tC = bslsp.HairTintColor != null ? bslsp.HairTintColor : bslsp.SkinTintColor;
				uni3f("tintColor", tC.r, tC.g, tC.b);
			}

			boolean hasTintMask = (bslsp.ShaderType instanceof BSShaderType155 ? (BSShaderType155)bslsp.ShaderType == BSShaderType155.FaceTint : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_FaceTint);
			boolean hasDetailMask = hasTintMask;

			uni1i("hasDetailMask", hasDetailMask);
			if (hasDetailMask) {
				registerBind("DetailMask", fileName(bslsp, 3), clamp);
			}

			uni1i("hasDetailMask", hasTintMask);
			if (hasTintMask) {
				registerBind("TintMask", fileName(bslsp, 6), clamp);
			}

			// Rim & Soft params
			boolean hasSoftlight = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Soft_Lighting);
			uni1i("hasSoftlight", hasSoftlight);

			boolean hasRimlight = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Rim_Lighting);
			if (sm != null)
				hasRimlight = sm.bRimLighting != 0;
			uni1i("hasRimlight", hasRimlight);

			if (niAVObject.nVer.BS_Version < 130 && (hasSoftlight || hasRimlight)) {
				registerBind("LightMask", fileName(bslsp, 2), clamp);
			}

			// Backlight params
			boolean hasBacklight = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Back_Lighting);
			if (sm != null)
				hasBacklight = sm.bBackLighting != 0;
			uni1i("hasBacklight", hasBacklight);

			if (niAVObject.nVer.BS_Version < 130 && hasBacklight) {
				registerBind("BacklightMap", fileName(bslsp, 7), clamp);
			}

			// Glow params
			if (sm == null) {
				boolean hasEmittance = SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
						SkyrimShaderPropertyFlags1.SLSF1_Own_Emit);
				uni1i("hasEmit", hasEmittance);
				if (hasEmittance)
					uni1f("glowMult", bslsp.EmissiveMultiple);
				else
					uni1f("glowMult", 0);

				boolean hasGlowMap = (bslsp.ShaderType instanceof BSShaderType155 ? (BSShaderType155)bslsp.ShaderType == BSShaderType155.Glow : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_GlowShader)
										&& SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
												SkyrimShaderPropertyFlags2.SLSF2_Glow_Map)
										&& hasFileName(bslsp, 2);
				uni1i("hasGlowMap", hasGlowMap);
				if (bslsp.EmissiveColor != null)
					uni3f("glowColor", bslsp.EmissiveColor.r, bslsp.EmissiveColor.g, bslsp.EmissiveColor.b);
			} else {
				boolean hasEmittance = sm.bEmitEnabled != 0;
				uni1i("hasEmit", hasEmittance);
				if (hasEmittance)
					uni1f("glowMult", sm.fEmittanceMult);
				else
					uni1f("glowMult", 0);

				boolean hasGlowMap = sm.bGlowmap != 0;
				uni1i("hasGlowMap", hasGlowMap);

				if (sm.cEmittanceColor != null)
					uni3f("glowColor", sm.cEmittanceColor.r, sm.cEmittanceColor.g, sm.cEmittanceColor.b);
			}

			// Specular params
			if (sm == null)
				uni1f("specStrength", bslsp.SpecularStrength);
			else
				uni1f("specStrength", sm.fSpecularMult);

			// Assure specular power does not break the shaders
			float gloss = bslsp.Glossiness;
			if (sm != null)
				gloss = sm.fSmoothness;
			uni1f("specGlossiness", (gloss > 0.0) ? gloss : 1.0f);

			if (sm == null) {
				if (bslsp.SpecularColor != null)
					uni3f("specColor", bslsp.SpecularColor.r, bslsp.SpecularColor.g, bslsp.SpecularColor.b);
			} else {
				uni3f("specColor", sm.cSpecularColor.r, sm.cSpecularColor.g, sm.cSpecularColor.b);
			}

			boolean hasSpecularMap = SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Specular);
			if (sm != null)
				hasSpecularMap = sm.bSpecularEnabled != 0 && hasFileName(bslsp, 2);
			uni1i("hasSpecularMap", hasSpecularMap);

			if (hasSpecularMap && (niAVObject.nVer.BS_Version == 130 || !hasBacklight)) {
				if (sm == null)
					registerBind("SpecularMap", fileName(bslsp, 7), clamp);
				else
					registerBind("SpecularMap", fileName(bslsp, 2), clamp);
			}

			if (niAVObject.nVer.BS_Version == 130) {
				boolean isDoubleSided = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
						SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
				if (sm != null)
					isDoubleSided = sm.bTwoSided != 0;
				uni1i("doubleSided", isDoubleSided);

				if (isDoubleSided) {
					pa.setCullFace(PolygonAttributes.CULL_NONE);
					pa.setBackFaceNormalFlip(true);
				}

				if (sm == null) {
					uni1f("paletteScale", bslsp.GrayscaletoPaletteScale);
					uni1f("fresnelPower", bslsp.FresnelPower);
					uni1f("rimPower", 2.0f);
					uni1f("backlightPower", bslsp.BacklightPower);
				} else {
					uni1f("paletteScale", sm.fGrayscaleToPaletteScale);
					uni1f("fresnelPower", sm.fFresnelPower);
					uni1f("rimPower", sm.fRimPower);
					uni1f("backlightPower", sm.fBacklightPower);
				}
			}

			// Multi-Layer
			boolean hasMultiLayerParallax = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Multi_Layer_Parallax);
			if (hasMultiLayerParallax) {
				NifTexCoord inS = bslsp.ParallaxInnerLayerTextureScale;
				uni2f("innerScale", inS.u, inS.v);

				uni1f("innerThickness", bslsp.ParallaxInnerLayerThickness);

				uni1f("outerRefraction", bslsp.ParallaxRefractionScale);
				uni1f("outerReflection", bslsp.ParallaxEnvmapStrength);

				registerBind("InnerMap", fileName(bslsp, 6), clamp);
			}

			// Environment Mapping
			boolean hasEnvironmentMap = (bslsp.ShaderType instanceof BSShaderType155 ? false : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_EnvironmentMap)
										&& SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
												SkyrimShaderPropertyFlags1.SLSF1_Environment_Mapping);

			if (sm != null)
				hasEnvironmentMap = sm.bEnvironmentMapping != 0;

			boolean hasCubeMap = (bslsp.ShaderType instanceof BSShaderType155 ? false : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_EnvironmentMap
																						|| (bslsp.ShaderType instanceof BSShaderType155 ? (BSShaderType155)bslsp.ShaderType == BSShaderType155.EyeEnvmap : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_EyeEnvmap)
																						|| (bslsp.ShaderType instanceof BSShaderType155 ? false : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_MultiLayerParallax))
									&& hasEnvironmentMap && hasFileName(bslsp, 4);

			if (sm != null)
				hasCubeMap = sm.bEnvironmentMapping != 0 && hasFileName(bslsp, 4);

			uni1i("hasCubeMap", hasCubeMap);

			boolean useEnvironmentMask = hasEnvironmentMap && hasFileName(bslsp, 5);
			if (sm != null)
				useEnvironmentMask = hasEnvironmentMap && sm.bGlowmap != 0 && hasFileName(bslsp, 5);

			uni1i("hasEnvMask", useEnvironmentMask);

			if (hasCubeMap && hasEnvironmentMap) {
				float envReflection = 0;
				if ((bslsp.ShaderType instanceof BSShaderType155 ? false : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_EnvironmentMap))
					envReflection = bslsp.EnvironmentMapScale;
				else if ((bslsp.ShaderType instanceof BSShaderType155 ? (BSShaderType155)bslsp.ShaderType == BSShaderType155.EyeEnvmap : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_EyeEnvmap))
					envReflection = bslsp.EyeCubemapScale;
				if (sm != null)
					envReflection = sm.fEnvironmentMappingMaskScale;

				uni1f("envReflection", envReflection);

				if (useEnvironmentMask)
					registerBind("EnvironmentMap", fileName(bslsp, 5), clamp);

				registerBindCube("CubeMap", fileName(bslsp, 4));

			} else {
				// In the case that the cube texture has already been bound, but SLSF1_Environment_Mapping is not set, 
				//assure that it removes reflections.
				uni1f("envReflection", 0);
			}

			// Parallax
			boolean hasHeightMap = (bslsp.ShaderType instanceof BSShaderType155 ? (BSShaderType155)bslsp.ShaderType == BSShaderType155.Terrain : (BSLightingShaderType)bslsp.ShaderType == BSLightingShaderType.ST_Heightmap);
			hasHeightMap |= SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Parallax) && hasFileName(bslsp, 3);

			if (niAVObject.nVer.BS_Version < 130 && hasHeightMap) {
				registerBind("HeightMap", fileName(bslsp, 3), clamp);
			}

			// vertex alpha is ignored when SF_Vertex_Animation is present
			// http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
			boolean isVertexAlphaAnimation = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim);
			uni1i("isVertexAlphaAnimation", isVertexAlphaAnimation);
		}

		// note this will be sole texturer if present
		BSEffectShaderProperty bsesp = (BSEffectShaderProperty)props.get(BSEffectShaderProperty.class);
		if (bsesp != null) {
			BSMaterialDataBGEM em = getMaterial(bsesp);

			clamp = bsesp.TextureClampMode;
			clamp.mode = clamp.mode ^ TexClampMode.MIRRORED_S_MIRRORED_T.mode;

			String SourceTexture = em == null ? bsesp.SourceTexture : em.BaseTexture;
			boolean hasSourceTexture = SourceTexture != null && SourceTexture.trim().length() > 0;
			String GreyscaleMap = em == null ? bsesp.GreyscaleTexture : em.GrayscaleToPaletteTexture;
			boolean hasGreyscaleMap = GreyscaleMap != null && GreyscaleMap.trim().length() > 0;
			String EnvMap = em == null ? bsesp.EnvMapTexture : em.CubeMapTexture;
			boolean hasEnvMap = EnvMap != null && EnvMap.trim().length() > 0;
			String NormalMap = em == null ? bsesp.NormalTexture : em.NormalTexture;
			boolean hasNormalMap = NormalMap != null && NormalMap.trim().length() > 0;
			String EnvMask = em == null ? bsesp.EnvMaskTexture : em.EnvironmentMaskTexture;
			boolean hasEnvMask = EnvMask != null && EnvMask.trim().length() > 0;

			registerBind("SourceTexture", SourceTexture, clamp);

			boolean isDoubleSided = SkyrimShaderPropertyFlags2.isBitSet(bsesp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
			if (em != null)
				isDoubleSided = em.bTwoSided != 0;
			uni1i("doubleSided", isDoubleSided);

			if (isDoubleSided) {
				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);
			}

			if (em == null) {
				textureScale.set(bsesp.UVScale.u, bsesp.UVScale.v);
				textureOffset.set(bsesp.UVOffSet.u, bsesp.UVOffSet.v);
			} else {
				textureScale.set(em.fUScale, em.fVScale);
				textureOffset.set(em.fUOffset, em.fVOffset);
			}

			uni1i("hasSourceTexture", hasSourceTexture);
			uni1i("hasGreyscaleMap", hasGreyscaleMap);

			boolean greyscaleAlpha = SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteAlpha);
			if (em != null)
				greyscaleAlpha = em.bGrayscaleToPaletteAlpha != 0;
			uni1i("greyscaleAlpha", greyscaleAlpha);

			boolean greyscaleColor = SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteColor);
			if (em != null)
				greyscaleColor = em.bGrayscaleToPaletteColor != 0;
			uni1i("greyscaleColor", greyscaleColor);

			boolean useFalloff = SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Use_Falloff);
			if (em != null)
				useFalloff = em.bFalloffEnabled != 0;
			uni1i("useFalloff", useFalloff);

			boolean vertexAlpha = SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
					SkyrimShaderPropertyFlags1.SLSF1_Vertex_Alpha);
			uni1i("vertexAlpha", vertexAlpha);// no em
			boolean vertexColors = SkyrimShaderPropertyFlags2.isBitSet(bsesp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Vertex_Colors);
			uni1i("vertexColors", vertexColors);// no em

			boolean hasWeaponBlood = SkyrimShaderPropertyFlags2.isBitSet(bsesp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Weapon_Blood);
			if (niAVObject.nVer.BS_Version == 130)
				hasWeaponBlood = false;
			uni1i("hasWeaponBlood", hasWeaponBlood);

			// Glow params
			if (em == null) {
				uni4f("glowColor", bsesp.BaseColor.r, bsesp.BaseColor.g, bsesp.BaseColor.b, bsesp.BaseColor.a);
				uni1f("glowMult", bsesp.BaseColorScale);
			} else {
				uni4f("glowColor", em.cBaseColor.r, em.cBaseColor.g, em.cBaseColor.b, em.fAlpha);
				uni1f("glowMult", em.fBaseColorScale);
			}
			// Falloff params
			if (em == null) {
				uni4f("falloffParams", bsesp.FalloffStartAngle, bsesp.FalloffStopAngle, bsesp.FalloffStartOpacity,
						bsesp.FalloffStopOpacity);
				uni1f("falloffDepth", bsesp.SoftFalloffDepth);
			} else {
				uni4f("falloffParams", em.fFalloffStartAngle, em.fFalloffStopAngle, em.fFalloffStartOpacity,
						em.fFalloffStopOpacity);
				uni1f("falloffDepth", em.fSoftDepth);
			}

			// BSEffectShader textures
			registerBind("GreyscaleMap", GreyscaleMap, TexClampMode.MIRRORED_S_MIRRORED_T);

			if (niAVObject.nVer.BS_Version == 130) {
				if (em == null)
					uni1f("lightingInfluence", 0f);
				else
					uni1f("lightingInfluence", em.fLightingInfluence);

				uni1i("hasNormalMap", hasNormalMap);
				if (hasNormalMap)
					registerBind("NormalMap", NormalMap, clamp);

				uni1i("hasCubeMap", hasEnvMap);
				uni1i("hasEnvMask", hasEnvMask);

				if (hasEnvMap) {
					if (em == null)
						uni1f("envReflection", bsesp.EnvironmentMapScale);
					else
						uni1f("envReflection", em.fEnvironmentMappingMaskScale);

					if (hasEnvMask)
						registerBind("SpecularMap", EnvMask, clamp);

					String textureUnitName = "CubeMap";
					registerBind(textureUnitName, fileName(bsprop, 2), clamp);
				} else {
					uni1f("envReflection", 0);
				}

			}

			boolean isVertexAlphaAnimation = SkyrimShaderPropertyFlags2.isBitSet(bsesp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim);
			uni1i("isVertexAlphaAnimation", isVertexAlphaAnimation);

		}

		// BSESP/BSLSP do not always need an NiAlphaProperty, and appear to override it at times
		boolean translucent = (bslsp != null) && (bslsp.Alpha < 1.0f || SkyrimShaderPropertyFlags1
				.isBitSet(bslsp.ShaderFlags1, SkyrimShaderPropertyFlags1.SLSF1_Refraction));
		translucent |= (bsesp != null) && props.get(NiAlphaProperty.class) == null && bsesp.BaseColor.a < 1.0f;

		BSMaterial m = bslsp != null ? getMaterial(bslsp) : bsesp != null ? getMaterial(bsesp) : null;
		if (m == null) {
			glProperty((NiAlphaProperty)props.get(NiAlphaProperty.class));
			glProperty((NiMaterialProperty)props.get(NiMaterialProperty.class),
					(NiSpecularProperty)props.get(NiSpecularProperty.class));
			glProperty((NiZBufferProperty)props.get(NiZBufferProperty.class));
			glProperty((NiStencilProperty)props.get(NiStencilProperty.class));
			glProperty((NiWireframeProperty)props.get(NiWireframeProperty.class));
		} else {
			glPropertyAlpha(m);
			glMaterial(m);
			glMaterialZBuffer(m);
			glMaterialStencil(m);
			glMaterialWireframe(m);
		}
		
		
	
		
		

		boolean depthTest = true;
		depthTest |= (bslsp != null) && SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
				SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);
		depthTest |= (bsesp != null) && SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
				SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);

		if (!depthTest) {
			//FIXME: I wonder if this is right?
			ra.setDepthBufferEnable(false);
		}

		boolean depthWrite = true;
		depthWrite |= (bslsp != null) && SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
				SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		depthWrite |= (bsesp != null) && SkyrimShaderPropertyFlags2.isBitSet(bsesp.ShaderFlags2,
				SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		if (!depthWrite || translucent) {
			//FIXME: I wonder if this is right?
			ra.setDepthBufferWriteEnable(false);
		}

		if (translucent) {
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
			ta.setDstBlendFunction(TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA);

			// If mesh is alpha tested, override threshold (but not istestenabled notice)
			ra.setAlphaTestFunction(RenderingAttributes.GREATER);
			ra.setAlphaTestValue(0.1f);
		}

		//FO4 onwards
		if (waterShaderProps != null) {
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
			ta.setDstBlendFunction(TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA);
			ta.setTransparency(0.5f);
		}

		//PJs decalling business
		boolean isDecal = false;
		isDecal |= (bslsp != null)
					&& (SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1, SkyrimShaderPropertyFlags1.SLSF1_Decal)
						|| SkyrimShaderPropertyFlags1.isBitSet(bslsp.ShaderFlags1,
								SkyrimShaderPropertyFlags1.SLSF1_Dynamic_Decal));
		isDecal |= (bsesp != null)
					&& (SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1, SkyrimShaderPropertyFlags1.SLSF1_Decal)
						|| SkyrimShaderPropertyFlags1.isBitSet(bsesp.ShaderFlags1,
								SkyrimShaderPropertyFlags1.SLSF1_Dynamic_Decal));
		if (isDecal) {
			pa.setPolygonOffset(0.02f);
			pa.setPolygonOffsetFactor(0.04f);
		}

		TextureAttributes textureAttributes = null;
		NiTimeController controller = null;
		if (texprop != null) {
			controller = (NiTimeController)niToJ3dData.get(texprop.controller);
		} else if (bsprop != null) {
			controller = (NiTimeController)niToJ3dData.get(bsprop.controller);
		}
		if (bslsp != null) {
			controller = (NiTimeController)niToJ3dData.get(bslsp.controller);
		}
		if (bsesp != null) {
			controller = (NiTimeController)niToJ3dData.get(bsesp.controller);
		}
		if (controller != null) {
			if (controller instanceof NiTextureTransformController) {
				textureAttributes = niToJ3dData.getTextureAttributes(controller.refId);
			}
		}
		if (textureAttributes == null)
			textureAttributes = new TextureAttributes();

		// ok for a given texturing prop I must build a set of tus and also a set of shader name
		// bindings then pull it out of the nitoj3d object

		// Time controllers are exactly like this, by refId in all, I just need to share
		// the target back out again, so each one should maybe 
		// NiAlphaController target = TransparencyAttributes
		// NiFlipController target = appearance or it textureunitstates
		// NiMaterialColor	target = Material
		// NiVisContorller  target = renderingAttributes
		// NiLightColor / Dimmer / radius target = PointLight
		// NiUVController ?tricky currently just creates a TextureTransformController	
		// NiGeomMorp == geometry data (singleton?)
		// all the crazy NiPSysModifiers?			
		// NiExtraDataController - not used by me yet
		// NiControllerManager controller controller
		// NiKeyframeController like single interp
		// NiSingleInterpController joins a controller to an interpolator

		//honestly the textureunitstates should also be by texturing property and bsep and bslsp
		// so where they are reused more than once they are the exact same objects

		// don't share if we will be controlled or transformed
		boolean sharable = (controller == null	&& textureScale.x == 1 && textureScale.y == 1 && textureOffset.x == 0
							&& textureOffset.y == 0);
		// note non shared TUS have default read caps on

		// Texture Unit state does not require the same aggression as Java3D will find equivalence
		// but it seem expensive and wasteful to me
		TextureUnitState[] tus = new TextureUnitState[allTextureUnitStateBindings.size()];
		for (int i = 0; i < allTextureUnitStateBindings.size(); i++) {
			Binding binding = allTextureUnitStateBindings.get(i);
			if (binding.CUBE_MAP) {
				tus[i] = bindCube(binding);
			} else {
				tus[i] = bind(binding, sharable);
			}

			if (tus[i] != null) {
				if ((textureScale.x != 1 || textureScale.y != 1 || textureOffset.x != 0 || textureOffset.y != 0)) {
					Transform3D textureTransform = new Transform3D();
					textureTransform.setScale(new Vector3d(textureScale.x, textureScale.y, 0));
					textureTransform.setTranslation(new Vector3f(textureOffset.x, textureOffset.y, 0));
					//System.out.println("textureScale " + textureScale);
					//System.out.println("textureOffset " + textureOffset);
					textureAttributes.setTextureTransform(textureTransform);
					tus[i].setTextureAttributes(textureAttributes);
				}

				if (controller != null)
					tus[i].setTextureAttributes(textureAttributes);
			}

		}

		// Shape merging demand aggressive appearance sharing, and hence component re-use
		// Shaders are newer and not well support for Shape merging
		ShaderAttributeSet shaderAttributeSet = getShaderAttributeSet(shaderProgram, allShaderAttributeValues);

		app.setTextureUnitState(tus);
		app.setShaderProgram(shaderProgram);
		app.setShaderAttributeSet(shaderAttributeSet);

		if (ra.getDepthBufferEnable() != true	|| ra.getStencilEnable() == true || ra.getDepthBufferEnable() != true
			|| ra.getDepthBufferWriteEnable() != true || ra.getAlphaTestFunction() != RenderingAttributes.ALWAYS)
			app.setRenderingAttributes(ra);

		if (pa.getCullFace() != PolygonAttributes.CULL_BACK || pa.getPolygonOffset() != 0.0
			|| pa.getPolygonOffsetFactor() != 0.0)
			app.setPolygonAttributes(pa);

		if (ta.getTransparencyMode() != TransparencyAttributes.NONE)
			app.setTransparencyAttributes(ta);

		// empty these 2 temps
		allShaderAttributeValues.clear();
		allTextureUnitStateBindings.clear();

		//so for now I'm sharing the texture attributes to ensure tex transforms, 
		//but how about alpha and vertex colors and Flip? they won't be shared, so the second usage may not animate?

		//Setting up controller must be done after the appearance is properly set up so the 
		// controller can get at the pieces
		if (controller != null) {
			if (controller instanceof NiTextureTransformController) {
				// did we get a pre made one or should we set it up now?
				if (niToJ3dData.getTextureAttributes(controller.refId) == null) {
					NiGeometryAppearanceFixed.setUpTimeController(controller, niToJ3dData, textureSource, target);
					niToJ3dData.putTextureAttributes(controller.refId, textureAttributes);
				}
			} else {
				NiGeometryAppearanceFixed.setUpTimeController(controller, niToJ3dData, textureSource, target);
			}
		}
		return true;
	}

	

	

	private static WeakHashMap<GLSLShaderProgram2, WeakHashMap<ShaderAttributeSet, ShaderAttributeSet>> shaderAttributeSetsByProgram = new WeakHashMap<GLSLShaderProgram2, WeakHashMap<ShaderAttributeSet, ShaderAttributeSet>>();

	private static ShaderAttributeSet getShaderAttributeSet(GLSLShaderProgram2 shaderProgram,
															List<ShaderAttributeObject> newShaderAttributeValues) {
		ShaderAttributeSet sas = null;
		WeakHashMap<ShaderAttributeSet, ShaderAttributeSet> currentShaderAttributeSets = null;
		synchronized (shaderAttributeSetsByProgram) {
			currentShaderAttributeSets = shaderAttributeSetsByProgram.get(shaderProgram);

			if (currentShaderAttributeSets == null) {
				currentShaderAttributeSets = new WeakHashMap<ShaderAttributeSet, ShaderAttributeSet>();
				shaderAttributeSetsByProgram.put(shaderProgram, currentShaderAttributeSets);
			}
		}
		synchronized (currentShaderAttributeSets) {
			for (ShaderAttributeSet currShaderAttributeSet : currentShaderAttributeSets.keySet()) {
				boolean equal = currShaderAttributeSet.size() == newShaderAttributeValues.size();
				if (equal) {
					for (int i = 0; i < newShaderAttributeValues.size(); i++) {
						ShaderAttribute newSav = newShaderAttributeValues.get(i);
						ShaderAttribute currSav = currShaderAttributeSet.get(newSav.getAttributeName());
						if (currSav == null || newSav.getCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE)
							|| currSav.getCapability(ShaderAttributeValue.ALLOW_VALUE_WRITE)
							|| !newSav.equals(currSav)) {
							equal = false;
							break;
						}
					}
				}

				if (equal) {
					sas = currShaderAttributeSet;
					break;
				}
			}
		}

		if (sas == null) {
			if (OUTPUT_BINDINGS)
				System.out.println("Shader attributes and bound value:");
			sas = new ShaderAttributeSet();
			sas.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
			for (ShaderAttributeObject sav : newShaderAttributeValues) {
				if (OUTPUT_BINDINGS) {
					if (sav instanceof ShaderAttributeArray) {
						System.out.print(sav.getAttributeName() + "={");
						for (Object v : (Object[])sav.getValue())
							System.out.print(v + ", ");
						System.out.println("}");
					} else {
						System.out.println(sav.getAttributeName() + "=" + sav.getValue());
					}
				}
				sas.put(sav);
			}
			synchronized (currentShaderAttributeSets) {
				currentShaderAttributeSets.put(sas, null);
			}
		}
		return sas;

	}

	private void glProperty(NiWireframeProperty nwp) {
		// TODO later
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
	}

	private void glMaterialWireframe(BSMaterial m) {
		// TODO later
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
	}

	private void glProperty(NiStencilProperty nsp) {
		if (nsp != null) {
			if (nsp.getDrawMode() == FaceDrawMode.DRAW_BOTH) {
				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);
			}

			//TODO: jonwd7 does not do this?
			if (nsp.isStencilEnable()) {
				ra.setStencilEnable(true);
				ra.setStencilWriteMask(nsp.stencilMask);
				ra.setStencilFunction(NifOpenGLToJava3D.convertStencilFunction(nsp.stencilFunction()), nsp.stencilRef,
						nsp.stencilMask);
				ra.setStencilOp(NifOpenGLToJava3D.convertStencilAction(nsp.failAction()), //
						NifOpenGLToJava3D.convertStencilAction(nsp.zFailAction()), //
						NifOpenGLToJava3D.convertStencilAction(nsp.passAction()));
			}
		}

	}

	private void glMaterialStencil(BSMaterial m) {
		if (m != null) {
			if (m.bTwoSided != 0) {
				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);
			}
		}
	}

	private void glProperty(NiZBufferProperty nzp) {
		if (nzp != null) {
			//FIXME: FO4 shows this is a bad thing, not sure what's a good thing here
			/*ra.setDepthBufferEnable((nzp.flags.flags & 0x01) != 0);
			ra.setDepthBufferWriteEnable((nzp.flags.flags & 0x02) != 0);
			if (nzp.function != null)
				ra.setDepthTestFunction(NifOpenGLToJava3D.convertStencilFunction(nzp.function.mode));
				*/
		}
	}

	private void glMaterialZBuffer(BSMaterial m) {
		if (m != null) {
			//FIXME: FO4 shows this is a bad thing, not sure what's a good thing here
			/*ra.setDepthBufferEnable(true);// really? not sure
			ra.setDepthBufferWriteEnable(m.bZBufferWrite != 0);
			ra.setDepthTestFunction(NifOpenGLToJava3D.convertStencilFunction(m.bZBufferTest));
			*/

		}
	}

	private void glProperty(NiMaterialProperty nmp, NiSpecularProperty nsp) {
		if (nmp != null) {
			Material mat = new Material();
			mat.setLightingEnable(true);
			mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

			if (!(nmp.nVer.LOAD_VER == NifVer.VER_20_2_0_7
					&& (nmp.nVer.LOAD_USER_VER == 11 || nmp.nVer.LOAD_USER_VER == 12) && nmp.nVer.BS_Version > 21)) {
				mat.setAmbientColor(nmp.ambientColor.r, nmp.ambientColor.g, nmp.ambientColor.b);
				mat.setDiffuseColor(nmp.diffuseColor.r, nmp.diffuseColor.g, nmp.diffuseColor.b);
			}
			// ambient and diffuse: mat default to 0.2 an 1 respectively

			mat.setEmissiveColor(nmp.emissiveColor.r, nmp.emissiveColor.g, nmp.emissiveColor.b);

			//in nifskope they blend the nmp.alpha value with the colors but that makes everything disappear for me when alpha is 0

			if (nmp.alpha != 1.0) {
				ta.setTransparencyMode(TransparencyAttributes.BLENDED);
				ta.setTransparency(1.0f - nmp.alpha); // notice the reversal of value here
			}

			if (nsp != null && (nsp.flags.flags & 0x01) == 0) {
				mat.setShininess(0.0f);
				mat.setSpecularColor(0, 0, 0);
			} else {
				mat.setShininess(nmp.glossiness);
				mat.setSpecularColor(nmp.specularColor.r, nmp.specularColor.g, nmp.specularColor.b);
			}

			app.setMaterial(mat);
		}

	}

	private void glMaterial(BSMaterial m) {
		if (m != null) {
			if (m instanceof BSMaterialDataBGSM) {
				Material mat = new Material();
				mat.setLightingEnable(true);
				mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

				// where are ambient and diffuse? mat default to 0.2 an 1 respectively

				BSMaterialDataBGSM sm = (BSMaterialDataBGSM)m;
				if (sm.bEmitEnabled != 0)
					mat.setEmissiveColor(sm.cEmittanceColor.r, sm.cEmittanceColor.g, sm.cEmittanceColor.b);

				/*	should this be like nialphaproperty above??
				 if (n.fAlpha != 1.0)
					{
						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
						ta.setTransparency(1.0f - m.fAlpha); // notice the reversal of value here
					}*/

				if (sm.bSpecularEnabled != 0) {
					mat.setShininess(sm.fSmoothness);
					mat.setSpecularColor(sm.cSpecularColor.r, sm.cSpecularColor.g, sm.cSpecularColor.b);
				}
				app.setMaterial(mat);
			}

		}

	}

	private void glProperty(NiAlphaProperty nap) {

		if (nap != null) {
			glProperty(nap.alphaBlendingEnable(), nap.sourceBlendMode(), nap.destinationBlendMode(),
					nap.alphaTestEnabled(), nap.alphaTestMode(), nap.threshold);
		} else {
			glProperty(false, 0, 0, false, 0, 0);
		}
	}

	private void glPropertyAlpha(BSMaterial m) {
		//Notice material only uses GREATER for alpha test function
		if (m != null) {
			glProperty(m.bAlphaBlend != 0, m.iAlphaSrc, m.iAlphaDst, m.bAlphaTest != 0, NiAlphaProperty.GL_GREATER,
					m.iAlphaTestRef);
			if (m.bDecal != 0) {
				pa.setPolygonOffset(0.02f);
				pa.setPolygonOffsetFactor(0.04f);
			}
		} else {
			glProperty(false, 0, 0, false, 0, 0);
		}

	}

	private void glProperty(boolean alphaBlendingEnable, int sourceBlendMode, int destinationBlendMode,
							boolean alphaTestEnabled, int alphaTestMode, float threshold) {
		if (alphaBlendingEnable) {
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(sourceBlendMode, true));
			ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(destinationBlendMode, false));

			// I think the PolygonAttributes.CULL_NONE should be applied to anything
			// with an alphaTestEnabled(), flat_lod trees from skyrim prove it
			// obviously transparent stuff can be seen from the back quite often
			// TODO: this is about right?
			//pa.setCullFace(PolygonAttributes.CULL_NONE);
			//pa.setBackFaceNormalFlip(true);
		} else {
			//screen door puts things in the second pass, but I see the ordering problem either way
			//PJ-what? I might want no blend, but alpha test
			ta.setTransparencyMode(TransparencyAttributes.NONE);
		}

		if (alphaTestEnabled) {
			ra.setAlphaTestFunction(NifOpenGLToJava3D.convertAlphaTestMode(alphaTestMode));
			ra.setAlphaTestValue((threshold) / 255f);// threshold range of 255 to 0 confirmed empirically
		}
	}

	 
	
	// Sets a float
	private void uni1f(String var, float x) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, Float.valueOf(x)));
	}		

	// Sets a vec2 (two floats)
	private void uni2f(String var, float x, float y) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector2f(x, y)));
	}

	// Sets a vec3 (three floats)
	private void uni3f(String var, float x, float y, float z) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector3f(x, y, z)));
	}

	// Sets a vec4 (four floats)
	private void uni4f(String var, float x, float y, float z, float w) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(x, y, z, w)));
	};
	private void uni4f(String var, FloatVector4 vec) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(vec.x, vec.y, vec.z, vec.w)));
	};
	private void uni4c(String var, int c, boolean isSRGB ) {
		FloatVector4	vec= new FloatVector4(c);
		vec.mult(1.0f / 255.0f);
		//if ( isSRGB )
		//	x = DDSTexture16::srgbExpand( x );
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(vec.x, vec.y, vec.z, vec.w)));
	}
	private void uni4srgb(String var, FloatVector4 vec ) {
		//x = DDSTexture16::srgbExpand( x );
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(vec.x, vec.y, vec.z, vec.w)));
	}

	// Sets a an int in sahder, from a boolean in code
	private void uni1i(String var, boolean val) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var,  Integer.valueOf(val?1:0)));
	};
	
	// Sets a boolean
	private void uni1b(String var, boolean val) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, Boolean.valueOf(val)));
	};

	// Sets an integer  
	private void uni1i(String var, int val) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, Integer.valueOf(val)));
	};

	// Sets a mat3 (3x3 matrix)
	private void uni3m(String var, NifMatrix33 val) {
		if (shaderProgram.programHasVar(var))
			uni3m(var, new Matrix3f(val.data()));
	}

	private void uni3m(String var, Matrix3f val) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, val));
	};

	// Sets a mat4 (4x4 matrix)
	private void uni4m(String var, NifMatrix44 val) {
		if (shaderProgram.programHasVar(var))
			uni4m(var, new Matrix4f(val.data()));
	};

	private void uni4m(String var, Matrix4f val) {
		if (shaderProgram.programHasVar(var))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, val));
	};
	
	
	// set an array of values
	private void uni1fv(String var, float[] vals, int maxParams) {
		//NOTE! must always call programHasVar to add name to list
		if (shaderProgram.programHasVar(var)) {
			Float[] v2 = new Float[vals.length];
			for (int i = 0; i < maxParams; i++) {
				v2[i] = Float.valueOf(vals[i]);
			}
			allShaderAttributeValues.add(new ShaderAttributeArray2(var, v2));
		}
	}

	// set an array of values
	private void uni1iv(String var, int[] vals, int maxParams) {
		if (shaderProgram.programHasVar(var)) {
			Integer[] v2 = new Integer[vals.length];
			for (int i = 0; i < maxParams; i++) {
				v2[i] = Integer.valueOf(vals[i]);
			}
			allShaderAttributeValues.add(new ShaderAttributeArray2(var, v2));
		}
	}

	// set an array of values
	private void uni1bv(String var, boolean[] vals, int maxParams) {
		if (shaderProgram.programHasVar(var)) {
			Boolean[] v2 = new Boolean[vals.length];
			for (int i = 0; i < maxParams; i++) {
				v2[i] = Boolean.valueOf(vals[i]);
			}
			allShaderAttributeValues.add(new ShaderAttributeArray2(var, v2));
		}
	}
	
	// set an array of values
	private void uni4fv(String var, FloatVector4[] vals, int maxParams) {
		if (shaderProgram.programHasVar(var)) {
			Vector4f[] v2 = new Vector4f[vals.length];
			for (int i = 0; i < maxParams; i++) {
				v2[i] = new Vector4f(vals[i].x, vals[i].y, vals[i].z, vals[i].w);
			}

			allShaderAttributeValues.add(new ShaderAttributeArray2(var, v2));
		}
	}
	
	 
	

	private void registerBindCube(String samplerName, String fileName) {
		if (shaderProgram.programHasVar(samplerName) && fileName != null && fileName.length() > 0) {
			Binding binding = new Binding(samplerName, fileName, true);
			allTextureUnitStateBindings.add(binding);
		}
	}

	private TextureUnitState bindCube(Binding binding) {
		TextureUnitState tus = null;

		if (J3dNiGeometry.textureExists(binding.fileName, textureSource)) {
			Texture tex = J3dNiGeometry.loadTexture(binding.fileName, textureSource);

			if (tex != null) {
				ImageComponent[] ics = tex.getImages();
				TextureCubeMap tcm = new TextureCubeMap(
						ics.length <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP, Texture.RGBA,
						tex.getWidth());

				for (int f = 0; f < 6; f++)
					for (int l = 0; l < ics.length; l++)
						tcm.setImage(l, f, (ImageComponent2D)ics[l]);
				tus = new TextureUnitState();
				tus.setTexture(tcm);
				tus.setName(binding.fileName);
			}
		} else {
			System.out.println("bindCube BSLightingShaderProperty " + binding.fileName + " No Texture found for nif "
								+ niAVObject.nVer.fileName);
		}
		
		if(tus == null) {
			//this is ok, handing back a null TUS just disables that textureunit, so the texunit++ is probably right
		}
		// Each TexureUnit needs to be allocated to a sampler2D in the shader by getting setting the
		// TUS id to be the value of the uniform, like any other uniform
		if (OUTPUT_BINDINGS)
			System.out.println("Cube " + binding.samplerName + " texunit " + texunit + " file=" + binding.fileName);
 		
		uni1i(binding.samplerName, texunit++);		
		
		return tus;
	}

	private void registerBind(String samplerName, String fileName, TexClampMode clamp) {
		if (shaderProgram.programHasVar(samplerName) && fileName != null && fileName.length() > 0) {  
			Binding binding = new Binding(samplerName, fileName, clamp);
			allTextureUnitStateBindings.add(binding);
		}
	}
	
	private TextureUnitState bind(Binding binding, boolean shared) {
		TextureUnitState tus = null;

		if (shared) {
			//TODO: jonwd7 suggest texture slot is the decaling place, see his fixed pipeline
			// also these should go through as shader uniforms I reckon
			//textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
			//		: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);

			tus = J3dNiGeometry.loadTextureUnitState(binding.fileName, textureSource);
			// if tus is null due to no texture a warning will have been published by now
		} else {
			Texture tex = J3dNiGeometry.loadTexture(binding.fileName, textureSource);
			if (tex == null) {
				// if tus is null due to no texture a warning will have been published by now
				// notice tus left as null!
			} else {
				tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(binding.fileName);
			}
		}		
	 
		
		if(tus == null) {
			//this is ok, handing back a null TUS just disables that textureunit, so the texunit++ is probably right
		}
		// Each TexureUnit needs to be allocated to a sampler2D in the shader by getting setting the
		// TUS id to be the value of the uniform, like any other uniform
		if (OUTPUT_BINDINGS)
			System.out.println("" + binding.samplerName + " texunit " + texunit + " file=" + binding.fileName);
		
		uni1i(binding.samplerName, texunit++); 
		return tus;		 
	}

	private boolean hasFileName(BSLightingShaderProperty bslsp, int textureSlot) {
		String fn = fileName(bslsp, textureSlot);
		return fn != null && fn.trim().length() > 0;
	}

	public static BSMaterialDataBGEM getMaterial(BSEffectShaderProperty bsesp) {
		// FO4 has material files pointed at by name
		if (bsesp.name.toLowerCase().endsWith(".bgem")) {
			try {
				return MaterialsSource.bgsmSource.getEffectMaterial(bsesp.name);
			} catch (ClassCastException e) {
				//ClassCastException: class nif.niobject.bgsm.ShaderMaterial cannot be cast to class nif.niobject.bgsm.EffectMaterial 
				// extracting shapes from Furniture\ProtectronPod\ProtectronPod01.nif
				System.out.println("badness trying to get an EffectMaterial called " + bsesp.name);
				e.printStackTrace();
			}
		}
		return null;
	}

	private static BSMaterialDataBGSM getMaterial(BSLightingShaderProperty bslsp) {
		// FO4 has material files pointed at by name
		if (bslsp.name.toLowerCase().endsWith(".bgsm")) {
			return MaterialsSource.bgsmSource.getShaderMaterial(bslsp.name);

		}
		return null;
	}

	private String fileName(BSLightingShaderProperty bslsp, int textureSlot) {
		if (bslsp != null) {
			// FO4 has material files pointed at by name
			BSMaterialDataBGSM material = getMaterial(bslsp);
			if (material != null) {
				switch (textureSlot) {
					case 0:
						return material.DiffuseTexture;
					case 1:
						return material.NormalTexture;
					case 2:
						return material.SmoothSpecTexture;
					case 3:
						return material.GreyscaleTexture;
					case 4:
						return material.EnvmapTexture;
					case 5:
						return material.GlowTexture;
					case 6:
						return material.InnerLayerTexture;
					case 7:
						return material.WrinklesTexture;
					case 8:
						return material.DisplacementTexture;
					default:
						System.out.println(
								"fileName(BSLightingShaderProperty bslsp, int textureSlot) bad slot " + textureSlot);
						break;
				}
			} else if (bslsp.TextureSet != null && bslsp.TextureSet.ref != -1) {
				BSShaderTextureSet texSet = (BSShaderTextureSet)niToJ3dData.get(bslsp.TextureSet);
				return texSet.textures[textureSlot];
			}

		}
		return "";

	}

	private String fileName(NiTexturingProperty ntp) {
		// now set the texture
		if (ntp != null && ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1) {
			NiSourceTexture niSourceTexture = (NiSourceTexture)niToJ3dData.get(ntp.baseTexture.source);
			return niSourceTexture.fileName.string;
		}
		return null;
	}

	private String fileName(BSShaderProperty bsprop, int textureSlot) {
		if (bsprop instanceof BSShaderPPLightingProperty) {
			BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty)bsprop;
			BSShaderTextureSet bbsts = (BSShaderTextureSet)niToJ3dData.get(bsspplp.textureSet);

			return bbsts.textures[textureSlot];
		} else {
			if (textureSlot == 0) {
				if (bsprop instanceof BSShaderNoLightingProperty) {
					BSShaderNoLightingProperty bssnlp = (BSShaderNoLightingProperty)bsprop;
					return bssnlp.fileName;
				} else if (bsprop instanceof TileShaderProperty) {
					TileShaderProperty tsp = (TileShaderProperty)bsprop;
					return tsp.fileName;
				} else if (bsprop instanceof TallGrassShaderProperty) {
					TallGrassShaderProperty tgsp = (TallGrassShaderProperty)bsprop;
					return tgsp.fileName;
				} else if (bsprop instanceof SkyShaderProperty) {
					SkyShaderProperty tsp = (SkyShaderProperty)bsprop;
					return tsp.fileName;
				} else if (bsprop instanceof WaterShaderProperty) {
					//WaterShaderProperty tsp = (WaterShaderProperty) bsprop;
					//TODO: water shader there!
				}
			} else {
				//System.out.println("is this an error or fine?");
			}
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	
	// to dumy off the scene code below easily
	public static class Scene {
		
 
// these are in updateSettings
			//int	tmp = settings.value( "Settings/Render/General/Mesh Cache Size", 128 ).toInt();
			//cfg.meshCacheSize = std::uint8_t( std::clamp< int >( ( tmp + 4 ) >> 3, 1, 128 ) );
			//tmp = settings.value( "Settings/Render/General/Cube Map Bgnd", 1 ).toInt();
			//globalUniforms.cubeBgndMipLevel = std::clamp< int >( tmp, -1, 6 );
			//tmp = settings.value( "Settings/Render/General/Sf Parallax Steps", 200 ).toInt();
			//globalUniforms.sfParallaxMaxSteps = std::clamp< int >( tmp, 16, 512 );
			//globalUniforms.sfParallaxScale = settings.value( "Settings/Render/General/Sf Parallax Scale", 0.0f).toFloat();
			//globalUniforms.sfParallaxOffset = settings.value( "Settings/Render/General/Sf Parallax Offset", 0.5f).toFloat();
		public static String cubeMapPathFO76 =   "textures/shared/cubemaps/mipblur_defaultoutside1.dds" ;
		public static String cubeMapPathSTF = "textures/cubemaps/cell_cityplazacube.dds" ;
			//setCacheSize( std::uint32_t( cfg.meshCacheSize ) << 23 );
			//TexCache::loadSettings( settings );
		
		
		public static String white = "#FFFFFFFF";
		public static String black = "#FF000000";
		public static String lighting = "#FF00F040";
		public static String reflectivity = "#FF0A0A0A";
		public static String gray = "#FF808080s";
		public static String magenta = "#FFFF00FF";
		public static String default_n = "#FFFF8080";
		public static String default_ns = "#FFFF8080n";
		public static String cube_sk = "textures/cubemaps/bleakfallscube_e.dds";
		public static String cube_fo4 = "textures/shared/cubemaps/mipblur_defaultoutside1.dds";
		public static String grayCube = "#FF555555c";
		public static String pbr_lut_sf = "sfpbr.dds";
		 
		

		public static final String DoCubeMapping = "DoCubeMapping";
		public static final String DoLighting = "DoLighting";
		public static final String DoSpecular = "DoSpecular";
		public static final String DoGlow = "DoGlow";
		public static final String DoBlending = "DoBlending";
		public static final String DoVertexColors = "DoVertexColors";
		
		public static final String VisNormalsOnly = "VisNormalsOnly";
		public static final int DoTexturing = 1;
		public static final int DoErrorColor = 2;


		public int options;
		public boolean hasOption(String op) {
			return true;
		}
		public boolean hasVisMode(String mode) {
			return false;
		}
		
	}
	public static class Mesh {

		public boolean depthWrite = true;
		public boolean depthTest = true;
		public boolean translucent = false; 
		
	}
	
	private boolean setupProgramCE2(Program prog) {

		if (OUTPUT_BINDINGS) {
			System.out.println("****************************** setupProgramCE2 ****************************** ");
		}
		//https://github.com/fo76utils/nifskope/blob/develop/src/gl/renderer.cpp#L200
		
		if(!prog.getName().equals("stf_default.prog")) {
			if(prog.getName().equals("z_ffp_default.prog"))
				return false; // FIXME: no need to tell  anyone, check the conditions at some point

			System.err.println("setupProgramCE2 expecting prog stf_default.prog not " + prog.getName());
			return false;
		}
		
		if (!(this.niAVObject instanceof BSGeometry)) {
			System.err.println("!(this.niAVObject instanceof BSGeometry) " + this.niAVObject);
			return false;
		} 
			
		this.shaderProgram = prog.shaderProgram;
				
		// note time controllers below need appearance set on the shape now
		shape.setAppearance(app);
			
		//this shader is super complex, so we just set names furiously and then the pipeline does validation very late on
		GLSLShaderProgram2.ALLOW_ANY_UNIFORM_NAME = true;


		BSGeometry bsGeometry = (BSGeometry)niAVObject;
		
		
		if (OUTPUT_BINDINGS)
			System.out.println("using prog " + prog.getName() + "for BSGeometry "+ bsGeometry.name);

		//https://github.com/fo76utils/nifskope/blob/develop/src/gl/renderer.cpp#L200
		BSShaderProperty lsp = (BSShaderProperty)niToJ3dData.get(bsGeometry.ShaderProperty);

		
		//tester to highlight bits of a model for fun
		if (bsGeometry.name.contains("not any thing")) {
			System.err.println("HIGHLIGHTING in yellow");
			uni1i("highlighter", 1);
		} else {
			//FIXME: damn it, the reuse of shader must make unset uniforms stay as previous, gosh darn it all
			// perhaps I want to share a shader, but give every mesh it's own shader attributes, unless the material is
			// identical, so cache up the result of this call by material name! could be, could be			

			// this can be tested by using OUTPUT_BINDINGS==true to suppress any sharing, then none of the odd errors on shader variables
			
			// so I need a much better shader sharign system. I wonder if teh CE1 code needs looking at as well perhaps?
			//wtf 0 doens't reset it but -1 does! like 0 is ignored soemwhere along teh way? what?
			// but 09 did work before so some damn oddity
			uni1i("highlighter", -1);// shouldn't have to do this surely, or are my uniforms staying stuck on somehow??
		}

		Scene scene = new Scene();
		Mesh mesh = new Mesh();

		if (lsp == null) {
			System.err.println("BSLightingShaderProperty is null, sad");
			return false;
		}

		if (lsp.name == null || lsp.name.length() == 0) {
			//happens on things like Meshes\Markers\EditorMarkers\MarkerDummyA.nif
			return false;
		}
		
		if (OUTPUT_BINDINGS)
			System.out.println("BSLightingShaderProperty " + lsp.name);
		
		
		CE2BSMaterial ce2bsm = (CE2BSMaterial)MaterialsSource.bgsmSource.readMaterialFileCDB(lsp.name);
		if (ce2bsm == null) {
			System.err.println("ce2bsm is null, also sad " + lsp.name);
			return false;
		}
		CE2Material mat = ce2bsm.ce2material;
		 
		if (mat == null) {
			System.err.println("mat is null, also sad");
			return false;
		}
		boolean useErrorColor = false;
		mesh.depthWrite = true;
		mesh.depthTest = true;
		boolean isEffect = ((mat.flags & CE2Material.Flag_IsEffect) != 0 && mat.shaderRoute != 0);
		if (isEffect) {
			mesh.depthWrite = (mat.effectSettings.flags & CE2Material.EffectFlag_ZWrite) != 0;
			mesh.depthTest = (mat.effectSettings.flags & CE2Material.EffectFlag_ZTest) != 0;
		}

		// texturing

		int[] texunit = new int[] {0};// so it can be treated as an address

		// Always bind cube to texture units 0 (specular) and 1 (diffuse),
		// regardless of shader settings
		boolean hasCubeMap = scene.hasOption(Scene.DoCubeMapping) && scene.hasOption(Scene.DoLighting);
		//TODO: see CubeMap above
		//GLint uniCubeMap = uniLocation( "CubeMap" );
		//if ( uniCubeMap < 0 )
		//	return false;
		//fn.glActiveTexture( GL_TEXTURE0 + texunit );
		//hasCubeMap = hasCubeMap && bindCube( Scene.cubeMapPathSTF );
				//if ( !hasCubeMap )
		//	scene.bindCube( grayCube, 1 );
		//fn.glUniform1i( uniCubeMap, texunit++ );

		//note always bind so always sampler 0
		registerBindCube("CubeMap", Scene.cubeMapPathSTF);
		texunit[0]++;
		
		//TODO: see CubeMap above
		//uniCubeMap = uniLocation( "CubeMap2" );
		//if ( uniCubeMap < 0 )
		//	return false;
		//fn.glActiveTexture( GL_TEXTURE0 + texunit );
		//hasCubeMap = hasCubeMap && scene.bindCube( cfg.cubeMapPathSTF, 2 );
		//if ( !hasCubeMap ) 
		//	scene.bindCube( grayCube, 1 );
		//fn.glUniform1i( uniCubeMap, texunit++ );
			
		//note always bind so always sampler 1
		registerBindCube("CubeMap2", Scene.cubeMapPathSTF);
		texunit[0]++;
		
		uni1b("hasCubeMap", hasCubeMap);

		// texture unit 2 is reserved for the environment BRDF LUT texture
		//fn.glActiveTexture( GL_TEXTURE0 + texunit );
		//if ( !lsp.bind( Scene.pbr_lut_sf, true, TexClampMode.CLAMP_S_CLAMP_T ) )
		//	return false;
		texunit[0]++;

		//String	emptyTexturePath = "";

		uni1b("hasSpecular", scene.hasOption(Scene.DoSpecular));
		uni1i("lm.shaderModel", mat.shaderModel);

		// emissive settings
		if ((mat.flags & CE2Material.Flag_LayeredEmissivity) != 0 && scene.hasOption(Scene.DoGlow)) {
			CE2Material.LayeredEmissiveSettings sp = mat.layeredEmissiveSettings;
			uni1b("lm.layeredEmissivity.isEnabled", sp.isEnabled);
			uni1i("lm.layeredEmissivity.firstLayerIndex", sp.layer1Index);
			uni4c("lm.layeredEmissivity.firstLayerTint", sp.layer1Tint, true);
			uni1i("lm.layeredEmissivity.firstLayerMaskIndex", sp.layer1MaskIndex);
			uni1i("lm.layeredEmissivity.secondLayerIndex", (sp.layer2Active ? (sp.layer2Index) : -1));
			uni4c("lm.layeredEmissivity.secondLayerTint", sp.layer2Tint, true);
			uni1i("lm.layeredEmissivity.secondLayerMaskIndex", sp.layer2MaskIndex);
			uni1i("lm.layeredEmissivity.firstBlenderIndex", sp.blender1Index);
			uni1i("lm.layeredEmissivity.firstBlenderMode", sp.blender1Mode);
			uni1i("lm.layeredEmissivity.thirdLayerIndex", (sp.layer3Active ? (sp.layer3Index) : -1));
			uni4c("lm.layeredEmissivity.thirdLayerTint", sp.layer3Tint, true);
			uni1i("lm.layeredEmissivity.thirdLayerMaskIndex", sp.layer3MaskIndex);
			uni1i("lm.layeredEmissivity.secondBlenderIndex", sp.blender2Index);
			uni1i("lm.layeredEmissivity.secondBlenderMode", sp.blender2Mode);
			uni1f("lm.layeredEmissivity.emissiveClipThreshold", sp.clipThreshold);
			uni1b("lm.layeredEmissivity.adaptiveEmittance", sp.adaptiveEmittance);
			uni1f("lm.layeredEmissivity.luminousEmittance", sp.luminousEmittance);
			uni1f("lm.layeredEmissivity.exposureOffset", sp.exposureOffset);
			uni1b("lm.layeredEmissivity.enableAdaptiveLimits", sp.enableAdaptiveLimits);
			uni1f("lm.layeredEmissivity.maxOffsetEmittance", sp.maxOffset);
			uni1f("lm.layeredEmissivity.minOffsetEmittance", sp.minOffset);
		} else {
			uni1b("lm.layeredEmissivity.isEnabled", false);
		}
		if ((mat.flags & CE2Material.Flag_Emissive) != 0 && scene.hasOption(Scene.DoGlow)) {
			CE2Material.EmissiveSettings sp = mat.emissiveSettings;
			uni1b("lm.emissiveSettings.isEnabled", sp.isEnabled);
			uni1i("lm.emissiveSettings.emissiveSourceLayer", sp.sourceLayer);
			uni4srgb("lm.emissiveSettings.emissiveTint", sp.emissiveTint);
			uni1i("lm.emissiveSettings.emissiveMaskSourceBlender", sp.maskSourceBlender);
			uni1f("lm.emissiveSettings.emissiveClipThreshold", sp.clipThreshold);
			uni1b("lm.emissiveSettings.adaptiveEmittance", sp.adaptiveEmittance);
			uni1f("lm.emissiveSettings.luminousEmittance", sp.luminousEmittance);
			uni1f("lm.emissiveSettings.exposureOffset", sp.exposureOffset);
			uni1b("lm.emissiveSettings.enableAdaptiveLimits", sp.enableAdaptiveLimits);
			uni1f("lm.emissiveSettings.maxOffsetEmittance", sp.maxOffset);
			uni1f("lm.emissiveSettings.minOffsetEmittance", sp.minOffset);
		} else {
			uni1b("lm.emissiveSettings.isEnabled", false);
		}

		// translucency settings
		if ((mat.flags & CE2Material.Flag_Translucency) != 0) {
			CE2Material.TranslucencySettings sp = mat.translucencySettings;
			uni1b("lm.translucencySettings.isEnabled", sp.isEnabled);
			uni1b("lm.translucencySettings.isThin", sp.isThin);
			uni1b("lm.translucencySettings.flipBackFaceNormalsInViewSpace", sp.flipBackFaceNormalsInVS);
			uni1b("lm.translucencySettings.useSSS", sp.useSSS);
			uni1f("lm.translucencySettings.sssWidth", sp.sssWidth);
			uni1f("lm.translucencySettings.sssStrength", sp.sssStrength);
			uni1f("lm.translucencySettings.transmissiveScale", sp.transmissiveScale);
			uni1f("lm.translucencySettings.transmittanceWidth", sp.transmittanceWidth);
			uni1f("lm.translucencySettings.specLobe0RoughnessScale", sp.specLobe0RoughnessScale);
			uni1f("lm.translucencySettings.specLobe1RoughnessScale", sp.specLobe1RoughnessScale);
			uni1i("lm.translucencySettings.transmittanceSourceLayer", sp.sourceLayer);
			
			mesh.translucent = sp.isEnabled;
		} else {
			uni1b("lm.translucencySettings.isEnabled", false);
		}

		// decal settings
		if ((mat.flags & CE2Material.Flag_IsDecal) != 0) {
			CE2Material.DecalSettings sp = mat.decalSettings;
			uni1b("lm.decalSettings.isDecal", sp.isDecal);
			uni1f("lm.decalSettings.materialOverallAlpha", sp.decalAlpha);
			uni1i("lm.decalSettings.writeMask", (sp.writeMask));
			uni1b("lm.decalSettings.isPlanet", sp.isPlanet);
			uni1b("lm.decalSettings.isProjected", sp.isProjected);
			uni1b("lm.decalSettings.useParallaxOcclusionMapping", sp.useParallaxMapping);
			FloatVector4 replUniform = new FloatVector4(0.0f);
			int texUniform = getSFTexture(texunit, replUniform, (sp.surfaceHeightMap), 0, 0, null);
			uni1i("lm.decalSettings.surfaceHeightMap", texUniform);
			uni1f("lm.decalSettings.parallaxOcclusionScale", sp.parallaxOcclusionScale);
			uni1b("lm.decalSettings.parallaxOcclusionShadows", sp.parallaxOcclusionShadows);
			uni1i("lm.decalSettings.maxParralaxOcclusionSteps", sp.maxParallaxSteps);
			uni1i("lm.decalSettings.renderLayer", sp.renderLayer);
			uni1b("lm.decalSettings.useGBufferNormals", sp.useGBufferNormals);
			uni1i("lm.decalSettings.blendMode", sp.blendMode);
			uni1b("lm.decalSettings.animatedDecalIgnoresTAA", sp.animatedDecalIgnoresTAA);
		} else {
			uni1b("lm.decalSettings.isDecal", false);
		}

		// effect settings
		uni1b("lm.isEffect", isEffect);
		uni1b("lm.hasOpacityComponent", (isEffect && (mat.flags & CE2Material.Flag_HasOpacityComponent) != 0));
		int layeredEdgeFalloffFlags = 0;
		if (isEffect) {
			CE2Material.EffectSettings sp = mat.effectSettings;
			if ((mat.flags & CE2Material.Flag_LayeredEdgeFalloff) != 0)
				layeredEdgeFalloffFlags = mat.layeredEdgeFalloff.activeLayersMask & 0x07;
			uni1b("lm.effectSettings.vertexColorBlend", (sp.flags & CE2Material.EffectFlag_VertexColorBlend) != 0);
			// these settings appear to be unused, effects are always alpha tested with a threshold of 1/128

			//uni1b( "lm.effectSettings.isAlphaTested", bool(sp.flags & CE2Material.EffectFlag_IsAlphaTested) );
			//uni1f( "lm.effectSettings.alphaTestThreshold", sp.alphaThreshold );

			uni1b("lm.effectSettings.noHalfResOptimization", (sp.flags & CE2Material.EffectFlag_NoHalfResOpt) != 0);
			uni1b("lm.effectSettings.softEffect", (sp.flags & CE2Material.EffectFlag_SoftEffect) != 0);
			uni1f("lm.effectSettings.softFalloffDepth", sp.softFalloffDepth);
			uni1b("lm.effectSettings.emissiveOnlyEffect", (sp.flags & CE2Material.EffectFlag_EmissiveOnly) != 0);
			uni1b("lm.effectSettings.emissiveOnlyAutomaticallyApplied",
					(sp.flags & CE2Material.EffectFlag_EmissiveOnlyAuto) != 0);
			uni1b("lm.effectSettings.receiveDirectionalShadows", (sp.flags & CE2Material.EffectFlag_DirShadows) != 0);
			uni1b("lm.effectSettings.receiveNonDirectionalShadows",
					(sp.flags & CE2Material.EffectFlag_NonDirShadows) != 0);
			uni1b("lm.effectSettings.isGlass", (sp.flags & CE2Material.EffectFlag_IsGlass) != 0);
			uni1b("lm.effectSettings.frosting", (sp.flags & CE2Material.EffectFlag_Frosting) != 0);
			uni1f("lm.effectSettings.frostingUnblurredBackgroundAlphaBlend", sp.frostingBgndBlend);
			uni1f("lm.effectSettings.frostingBlurBias", sp.frostingBlurBias);
			uni1f("lm.effectSettings.materialOverallAlpha", sp.materialAlpha);
			uni1b("lm.effectSettings.zTest", (sp.flags & CE2Material.EffectFlag_ZTest) != 0);
			uni1b("lm.effectSettings.zWrite", (sp.flags & CE2Material.EffectFlag_ZWrite) != 0);
			uni1i("lm.effectSettings.blendingMode", sp.blendMode);
			uni1b("lm.effectSettings.backLightingEnable", (sp.flags & CE2Material.EffectFlag_BacklightEnable) != 0);
			uni1f("lm.effectSettings.backlightingScale", sp.backlightScale);
			uni1f("lm.effectSettings.backlightingSharpness", sp.backlightSharpness);
			uni1f("lm.effectSettings.backlightingTransparencyFactor", sp.backlightTransparency);
			uni4f("lm.effectSettings.backLightingTintColor", sp.backlightTintColor);
			uni1b("lm.effectSettings.depthMVFixup", (sp.flags & CE2Material.EffectFlag_MVFixup) != 0);
			uni1b("lm.effectSettings.depthMVFixupEdgesOnly", (sp.flags & CE2Material.EffectFlag_MVFixupEdgesOnly) != 0);
			uni1b("lm.effectSettings.forceRenderBeforeOIT", (sp.flags & CE2Material.EffectFlag_RenderBeforeOIT) != 0);
			uni1i("lm.effectSettings.depthBiasInUlp", sp.depthBias);
			// opacity component
			if ((mat.flags & CE2Material.Flag_HasOpacityComponent) != 0) {
				uni1i("lm.opacity.firstLayerIndex", mat.opacityLayer1);
				uni1b("lm.opacity.secondLayerActive", (mat.flags & CE2Material.Flag_OpacityLayer2Active) != 0);
				if ((mat.flags & CE2Material.Flag_OpacityLayer2Active) != 0) {
					uni1i("lm.opacity.secondLayerIndex", mat.opacityLayer2);
					uni1i("lm.opacity.firstBlenderIndex", mat.opacityBlender1);
					uni1i("lm.opacity.firstBlenderMode", mat.opacityBlender1Mode);
				}
				uni1b("lm.opacity.thirdLayerActive", (mat.flags & CE2Material.Flag_OpacityLayer3Active) != 0);
				if ((mat.flags & CE2Material.Flag_OpacityLayer3Active) != 0) {
					uni1i("lm.opacity.thirdLayerIndex", mat.opacityLayer3);
					uni1i("lm.opacity.secondBlenderIndex", mat.opacityBlender2);
					uni1i("lm.opacity.secondBlenderMode", mat.opacityBlender2Mode);
				}
				uni1f("lm.opacity.specularOpacityOverride", mat.specularOpacityOverride);
			}
		}
		if (layeredEdgeFalloffFlags != 0) {
			CE2Material.LayeredEdgeFalloff sp = mat.layeredEdgeFalloff;
			uni1fv("lm.layeredEdgeFalloff.falloffStartAngles", sp.falloffStartAngles, 3);
			uni1fv("lm.layeredEdgeFalloff.falloffStopAngles", sp.falloffStopAngles, 3);
			uni1fv("lm.layeredEdgeFalloff.falloffStartOpacities", sp.falloffStartOpacities, 3);
			uni1fv("lm.layeredEdgeFalloff.falloffStopOpacities", sp.falloffStopOpacities, 3);
			if (sp.useRGBFalloff)
				layeredEdgeFalloffFlags = layeredEdgeFalloffFlags | 0x80;
		}
		uni1i("lm.layeredEdgeFalloff.flags", layeredEdgeFalloffFlags);

		// alpha settings
		if ((mat.flags & CE2Material.Flag_HasOpacity) != 0) {
			uni1b("lm.alphaSettings.hasOpacity", true);
			uni1f("lm.alphaSettings.alphaTestThreshold", mat.alphaThreshold);
			uni1i("lm.alphaSettings.opacitySourceLayer", mat.alphaSourceLayer);
			uni1i("lm.alphaSettings.alphaBlenderMode", mat.alphaBlendMode);
			uni1b("lm.alphaSettings.useDetailBlendMask", (mat.flags & CE2Material.Flag_AlphaDetailBlendMask) != 0);
			uni1b("lm.alphaSettings.useVertexColor", (mat.flags & CE2Material.Flag_AlphaVertexColor) != 0);
			uni1i("lm.alphaSettings.vertexColorChannel", mat.alphaVertexColorChannel);
			CE2Material.UVStream uvStream = mat.alphaUVStream;
			if (uvStream == null)
				uvStream = CE2Material.defaultUVStream();
			uni4f("lm.alphaSettings.opacityUVstream.scaleAndOffset", uvStream.scaleAndOffset);
			uni1b("lm.alphaSettings.opacityUVstream.useChannelTwo", (uvStream.channel > 1));
			uni1f("lm.alphaSettings.heightBlendThreshold", mat.alphaHeightBlendThreshold);
			uni1f("lm.alphaSettings.heightBlendFactor", mat.alphaHeightBlendFactor);
			uni1f("lm.alphaSettings.position", mat.alphaPosition);
			uni1f("lm.alphaSettings.contrast", mat.alphaContrast);
			uni1b("lm.alphaSettings.useDitheredTransparency", (mat.flags & CE2Material.Flag_DitheredTransparency) != 0);
		} else {
			uni1b("lm.alphaSettings.hasOpacity", false);
		}

		// detail blender settings
		if ((mat.flags & CE2Material.Flag_UseDetailBlender) != 0 && mat.detailBlenderSettings.isEnabled) {
			CE2Material.DetailBlenderSettings sp = mat.detailBlenderSettings;
			uni1b("lm.detailBlender.detailBlendMaskSupported", true);
			CE2Material.UVStream uvStream = sp.uvStream;
			if (uvStream == null)
				uvStream = CE2Material.defaultUVStream();
			FloatVector4 replUniform = new FloatVector4(0.0f);
			int texUniform = getSFTexture(texunit, replUniform, (sp.texturePath), sp.textureReplacement,
					(sp.textureReplacementEnabled) ? 1 : 0, uvStream);
			uni1i("lm.detailBlender.maskTexture", texUniform);
			if (texUniform < 0)
				uni4f("lm.detailBlender.maskTextureReplacement", replUniform);
			uni4f("lm.detailBlender.uvStream.scaleAndOffset", uvStream.scaleAndOffset);
			uni1b("lm.detailBlender.uvStream.useChannelTwo", (uvStream.channel > 1));
		} else {
			uni1b("lm.detailBlender.detailBlendMaskSupported", false);
		}

		// material layers
		int[] texUniforms = new int[9];
		FloatVector4[] replUniforms = new FloatVector4[9];
		// limit the number of layers to 6, or 2 if the shader model is Eye1Layer, or 5 for Skin5Layer

		int numLayers = countr_one(
				mat.layerMask & (mat.shaderModel != 41 ? (mat.shaderModel != 48 ? 0x3F : 0x1F) : 0x03));
		uni1i("lm.numLayers", numLayers);

		for (int i = 0; i < numLayers; i++) {
			CE2Material.Layer layer = mat.layers[i];
			int textureSlotMap = 0;
			int textureReplModes = 0x0055955E; // 2, 3, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1
			CE2Material.Blender blender = null;
			byte blendMode = 3; // "None"
			if (i != 0) {
				blender = mat.blenders[i - 1];
				if (blender == null)
					blender = CE2Material.defaultBlender();
				else
					blendMode = blender.blendMode;
				if (blendMode == 4) {
					// CharacterCombine: remap color, roughness and metalness to overlay texture slots (0,3,4 . 14,15,16)
					textureSlotMap = 0x000CC00E;
				}
			}
			CE2Material.Material material = layer.material;
			if (material == null)
				material = CE2Material.defaultMaterial();
			CE2Material.TextureSet textureSet = material.textureSet;
			if (textureSet == null)
				textureSet = CE2Material.defaultTextureSet();
			uni1f("lm.layers[" + (i) + "].material.textureSet.floatParam", textureSet.floatParam);
			for (int j = 0; j < 9; j++) {
				int k = j + (textureSlotMap & 15);
				String texturePath = textureSet.texturePaths[k];
				int textureReplacement = textureSet.textureReplacements[k];
				int textureReplacementMode = ((textureSet.textureReplacementMask
												& (1 << k)) == 0 ? 0 : (textureReplModes & 3));
				textureSlotMap = textureSlotMap >> 4;
				textureReplModes = textureReplModes >> 2;
				CE2Material.UVStream uvStream = layer.uvStream;
				if (j == 0) {
					if ((scene.hasVisMode(Scene.VisNormalsOnly) && scene.hasOption(Scene.DoLighting))
						|| useErrorColor) {
						texturePath = "";
						textureReplacement = (useErrorColor ? 0xFFFF00FF : 0xFFFFFFFF);
						textureReplacementMode = 1;
					} else if ((texturePath == null || texturePath.length() == 0)	&& textureReplacementMode == 0
								&& (scene.options & (Scene.DoTexturing | Scene.DoErrorColor)) != Scene.DoTexturing) {
						textureReplacement = ((scene.options & Scene.DoTexturing) != 0 ? 0xFFFF00FF : 0xFFFFFFFF);
						textureReplacementMode = 1;
					}
				} else if (j == 1 && !scene.hasOption(Scene.DoLighting)) {
					texturePath = "";
					textureReplacement = 0xFFFF8080;
					textureReplacementMode = 3;
				} else if (j == 2 && (mat.flags & CE2Material.Flag_HasOpacity) != 0 && i == mat.alphaSourceLayer) {
					uvStream = mat.alphaUVStream;
				}
				replUniforms[j] = new FloatVector4(0.0f);
				texUniforms[j] = getSFTexture(texunit, replUniforms[j], texturePath, textureReplacement,
						textureReplacementMode, uvStream);
			}
			if (blendMode == 4) {
				// set default color (0.5) for overlay textures in CharacterCombine blend mode
				if (texUniforms[0] != 0) {
					texUniforms[0] = -1;
					replUniforms[0] = new FloatVector4(0.5f);
				}
				if (texUniforms[3] != 0) {
					texUniforms[3] = -1;
					replUniforms[3] = new FloatVector4(0.5f);
				}
				if (texUniforms[4] != 0) {
					texUniforms[4] = -1;
					replUniforms[4] = new FloatVector4(0.5f);
				}
			}
			if (mat.shaderModel == 44) { // Hair1Layer
				if (texUniforms[3] != 0 && (mat.flags & CE2Material.Flag_IsHair) != 0 && mat.hairSettings != null) {
					float hairRoughness = mat.hairSettings.roughness;
					texUniforms[3] = -1;
					replUniforms[3] = new FloatVector4(((hairRoughness - 2.0f) * hairRoughness + 2.0f) * hairRoughness);
				}
			}
			uni1iv(("lm.layers[" + (i) + "].material.textureSet.textures"), texUniforms, 9);
			uni4fv(("lm.layers[" + (i) + "].material.textureSet.textureReplacements"), replUniforms, 9);

			CE2Material.UVStream uvStream = layer.uvStream;
			if (uvStream == null)
				uvStream = CE2Material.defaultUVStream();
			FloatVector4 uvScaleAndOffset = (uvStream.scaleAndOffset);
			uni4srgb(("lm.layers[" + (i) + "].material.color"), layer.material.color);
			// disable vertex color tint for 1LayerMouth
			int materialFlags = layer.material.colorModeFlags & (mat.shaderModel != 9 ? 3 : 1);
			if ((layer.material.flipbookFlags & 1) != 0)
				materialFlags = materialFlags | setFlipbookParameters((layer.material), uvScaleAndOffset);
			uni1i(("lm.layers[" + (i) + "].material.flags"), materialFlags);
			uni4f(("lm.layers[" + (i) + "].uvStream.scaleAndOffset"), uvScaleAndOffset);
			uni1b(("lm.layers[" + (i) + "].uvStream.useChannelTwo"), (uvStream.channel > 1));

			if (blender == null)
				continue;
			uvStream = blender.uvStream;
			if (uvStream == null)
				uvStream = CE2Material.defaultUVStream();
			uni4f(("lm.blenders[" + (i - 1) + "].uvStream.scaleAndOffset"), uvStream.scaleAndOffset);
			uni1b(("lm.blenders[" + (i - 1) + "].uvStream.useChannelTwo"), (uvStream.channel > 1));
			FloatVector4 replUniform = new FloatVector4(0.0f);
			int texUniform = getSFTexture(texunit, replUniform, (blender.texturePath), blender.textureReplacement,
					(blender.textureReplacementEnabled) ? 1 : 0, uvStream);
			uni1i(("lm.blenders[" + (i - 1) + "].maskTexture"), texUniform);
			if (texUniform < 0)
				uni4f(("lm.blenders[" + (i - 1) + "].maskTextureReplacement"), replUniform);
			uni1i(("lm.blenders[" + (i - 1) + "].blendMode"), (int)(blendMode));
			uni1i(("lm.blenders[" + (i - 1) + "].colorChannel"), (int)(blender.colorChannel));
			uni1fv(("lm.blenders[" + (i - 1) + "].floatParams"), blender.floatParams,
					CE2Material.Blender.maxFloatParams);
			uni1bv(("lm.blenders[" + (i - 1) + "].boolParams"), blender.boolParams, CE2Material.Blender.maxBoolParams );
		}
		
		// registerBind in getSFTexture plus the TextureUnitState[] tus calls below replace this
		//uniSampler( ("textureUnits"), 2, texunit - 2, TexCache.num_texture_units - 2 );
	
		//This is auto done by setting the Appearance attributes
		//mesh.setUniforms( prog );
		uni4f("vertexColorOverride", new FloatVector4(scene.hasOption(Scene.DoVertexColors) ? 0.0f : 1.0f));
		
		// setup alpha blending and testing
		int	alphaFlags = 0;
		if (mat != null && scene.hasOption(Scene.DoBlending)) {
			if (isEffect || (~(mat.flags) & (CE2Material.Flag_IsDecal | CE2Material.Flag_AlphaBlending)) == 0) {
				int blendMode;
				if (!isEffect) {
					blendMode = mat.decalSettings.blendMode;
				} else if ((mat.effectSettings.flags
							& (CE2Material.EffectFlag_EmissiveOnly | CE2Material.EffectFlag_EmissiveOnlyAuto)) == 0) {
					blendMode = mat.effectSettings.blendMode;
				} else {
					blendMode = 1; // emissive only: additive blending
				}
				//setupGLBlendModeSF( blendMode, prog.f ); //TODO: possibly quite important
				alphaFlags = 2;
			}

			if (isEffect)
				alphaFlags |= ((mat.effectSettings.flags & CE2Material.EffectFlag_IsAlphaTested) != 0) ? 1 : 0;
			else
				alphaFlags |= ((mat.flags & CE2Material.Flag_HasOpacity) != 0 && mat.alphaThreshold > 0.0f) ? 1 : 0;

			if ( (mat.flags & CE2Material.Flag_IsDecal)!=0 ) {
				//fn.glEnable( GL_POLYGON_OFFSET_FILL );
				//fn.glPolygonOffset( -1.0f, -1.0f );
				pa.setPolygonOffset(0.02f);
				pa.setPolygonOffsetFactor(0.04f);
			}
		}		 

		uni1i("alphaFlags", alphaFlags);
		if ((alphaFlags & 2) == 0)
			ta.setTransparencyMode(TransparencyAttributes.NONE);//fn.glDisable( GL_BLEND );
		
		
		//FIXME: soem thigns are transparent some are not
		if (mesh.translucent) {
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
			ta.setDstBlendFunction(TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA);

			// If mesh is alpha tested, override threshold (but not istestenabled notice)
			ra.setAlphaTestFunction(RenderingAttributes.GREATER);
			ra.setAlphaTestValue(0.1f);
		}	
	

		//FIXME: these 3 are note working properly yet
//		ra.setDepthBufferEnable(mesh.depthTest);
//		ra.setDepthBufferWriteEnable(!mesh.depthWrite || mesh.translucent ? false : true);
//		ra.setDepthTestFunction(RenderingAttributes.LESS_OR_EQUAL);
		//fn.glDepthMask( !mesh.depthWrite || mesh.translucent ? GL_FALSE : GL_TRUE );
		//fn.glDepthFunc( GL_LEQUAL );
		
				
		if ((mat.flags & CE2Material.Flag_TwoSided) != 0) {
			//fn.glDisable( GL_CULL_FACE );
			pa.setCullFace(PolygonAttributes.CULL_NONE);
			pa.setBackFaceNormalFlip(true);
		} else {
			//fn.glEnable( GL_CULL_FACE );
			//fn.glCullFace( GL_BACK );
			pa.setCullFace(PolygonAttributes.CULL_BACK);
			pa.setBackFaceNormalFlip(false);
		}
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);//fn.glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
			 
		if (ra.getDepthBufferEnable() != true	|| ra.getStencilEnable() == true || ra.getDepthBufferWriteEnable() != true
			|| ra.getAlphaTestFunction() != RenderingAttributes.ALWAYS)
			app.setRenderingAttributes(ra);

		if (pa.getCullFace() != PolygonAttributes.CULL_BACK || pa.getPolygonOffset() != 0.0
			|| pa.getPolygonOffsetFactor() != 0.0)
			app.setPolygonAttributes(pa);

		if (ta.getTransparencyMode() != TransparencyAttributes.NONE)
			app.setTransparencyAttributes(ta);
		
		//there are some magic values in the shader that I'll set now for fun
		// could be rotated about a bit I'm guessing
		uni3m("envMapRotation", new Matrix3f(1, 0, 0, 0, 1, 0, 0, 0, 1));//identity

		//Directly from CE1 above at the end to "set" the values a bit like the mesh.setUniforms( prog ); above	
		NiTimeController controller = null;
		// no texture attributes

		//controller skipped 
		
		
		if (OUTPUT_BINDINGS) {
			System.out.println("Shader material: " + mat);
		}
		
		
		// nothing stops sharing for this shaders
		boolean sharable = true;
		// note non shared TUS have default read caps on

		// Texture Unit state does not require the same aggression as Java3D will find equivalence
		// but it seem expensive and wasteful to me
		TextureUnitState[] tus = new TextureUnitState[allTextureUnitStateBindings.size()];
		for (int i = 0; i < allTextureUnitStateBindings.size(); i++) {
			Binding binding = allTextureUnitStateBindings.get(i);
			if (binding.CUBE_MAP) {
				tus[i] = bindCube(binding);
			} else {
				tus[i] = bind(binding, sharable);
			}
		}

 
		// Shape merging demand aggressive appearance sharing, and hence component re-use
		// Shaders are newer and not well support for Shape merging
		ShaderAttributeSet shaderAttributeSet = getShaderAttributeSet(shaderProgram, allShaderAttributeValues);

		app.setTextureUnitState(tus);
		app.setShaderProgram(shaderProgram);
		app.setShaderAttributeSet(shaderAttributeSet);

		// empty these 2 temps
		allShaderAttributeValues.clear();
		allTextureUnitStateBindings.clear();

		//so for now I'm sharing the texture attributes to ensure tex transforms, 
		//but how about alpha and vertex colors and Flip? they won't be shared, so the second usage may not animate?

		//Setting up controller must be done after the appearance is properly set up so the 
		// controller can get at the pieces
/*		if (controller != null) {
			if (controller instanceof NiTextureTransformController) {
				// did we get a pre made one or should we set it up now?
				if (niToJ3dData.getTextureAttributes(controller.refId) == null) {
					NiGeometryAppearanceFixed.setUpTimeController(controller, niToJ3dData, textureSource, target);
					niToJ3dData.putTextureAttributes(controller.refId, textureAttributes);
				}
			} else {
				NiGeometryAppearanceFixed.setUpTimeController(controller, niToJ3dData, textureSource, target);
			}
		}*/
		
		
		// cos of the new accept all uniforms system for starfield
		prog.refreshShaders();		
		return true;
	}
	
	

	private static int[][] blendModeMap = new int[][] {
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_DST_COLOR,TransparencyAttributes.BLEND_ZERO,TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ZERO},
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_SRC_ALPHA,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA},
		new int[] {TransparencyAttributes.BLEND_ZERO,TransparencyAttributes.BLEND_ONE,TransparencyAttributes.BLEND_ZERO,TransparencyAttributes.BLEND_ONE},
		};
		
	void setupGLBlendModeSF( int blendMode )
	{
	
	// these lists are in sets of 4 and blendMode chooses the row then the 4 are set
	
		// source RGB, destination RGB, source alpha, destination alpha
	/*	static const GLenum blendModeMap[32] = {
			GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// AlphaBlend
			GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// Additive
			GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// SourceSoftAdditive (alpha is squared in the shader)
			GL_DST_COLOR, GL_ZERO, GL_DST_ALPHA, GL_ZERO,	// Multiply
			GL_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// DestinationSoftAdditive
			GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// TODO: DestinationInvertedSoftAdditive
			GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA,	// TODO: TakeSmaller
			GL_ZERO, GL_ONE, GL_ZERO, GL_ONE	// None
		};
		const GLenum *	p = &( blendModeMap[blendMode << 2] );
		fn->glEnable( GL_BLEND );
		fn->glBlendFuncSeparate( p[0], p[1], p[2], p[3] );*/
			
		int[] p = blendModeMap[blendMode << 2];
		//Like this, though I only have the first 2!!
		ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		ta.setSrcBlendFunction(p[0]);
		ta.setDstBlendFunction(p[1]);

		//TODO: is this more correct? It's from CE1
		// If mesh is alpha tested, override threshold (but not istestenabled notice)
		//ra.setAlphaTestFunction(RenderingAttributes.GREATER);
		//ra.setAlphaTestValue(0.1f);	
		

	}
	
	static int setFlipbookParameters( CE2Material.Material m, FloatVector4 uvScaleAndOffset )
	{
		int	flipbookColumns = Math.min( m.flipbookColumns, 127 );
		int	flipbookRows = Math.min( m.flipbookRows, 127 );
		int	flipbookFrames = flipbookColumns * flipbookRows;
		if ( flipbookFrames < 2 )
			return 0;
		float	flipbookFPMS = Math.min( Math.max( m.flipbookFPS, 1.0f ), 100.0f ) * 0.001f;
		double	flipbookFrame = System.currentTimeMillis();//double( std::chrono::duration_cast< std::chrono::milliseconds >( std::chrono::steady_clock::now().time_since_epoch() ).count() );
		flipbookFrame = flipbookFrame * flipbookFPMS / (double)( flipbookFrames );
		flipbookFrame = flipbookFrame - Math.floor( flipbookFrame );
		int	n = Math.min( (int)( flipbookFrame * (double)( flipbookFrames ) ), flipbookFrames - 1 );
		uvScaleAndOffset.x +=  0.0f;
		uvScaleAndOffset.y +=0.0f;
		uvScaleAndOffset.z +=(float)(n % flipbookColumns);
		uvScaleAndOffset.w +=(float)(n / flipbookColumns);		
		float	w = (float)( flipbookColumns );
		float	h = (float)( flipbookRows );
		uvScaleAndOffset.x= uvScaleAndOffset.x/w;
		uvScaleAndOffset.y= uvScaleAndOffset.y/h;
		uvScaleAndOffset.z=uvScaleAndOffset.z/w;
		uvScaleAndOffset.w= uvScaleAndOffset.w/h;
		return 4;
	}
	
	//return texture units indexs (-3) so a fine number I'd say texUnit is an int address
	int getSFTexture(int[] texunit, FloatVector4 replUniform, String texturePath,
						int textureReplacement, int textureReplacementMode, CE2Material.UVStream uvStream) {
		
		do {
			if (texturePath != null && texturePath.length() > 0) {	
				 
				// I'm not activating anything so no way to fail
				//if (!(texunit >= 3  && scene.textures.activateTextureUnit(texunit) != null))
				//	break;
	
				TexClampMode clampMode = TexClampMode.WRAP_S_WRAP_T;
				if (uvStream != null) {
					TexClampMode[] clampModes = new TexClampMode[] {TexClampMode.WRAP_S_WRAP_T,
						TexClampMode.CLAMP_S_CLAMP_T, TexClampMode.MIRRORED_S_MIRRORED_T, TexClampMode.BORDER_S_BORDER_T};
					clampMode = (TexClampMode)(clampModes[uvStream.textureAddressMode & 3]);
				}			
			 
				// noting that 0-2 texture were bound above (as 2 cubes and environment BRDF LUT texture)
				
				// the .s2D is because 
				// It would appear that arrays as terminal variables need to be loaded by the attributearray system
				// but non terminal variables can be loaded by the single system!
				registerBind("textureUnits[" + texunit[0] + "].s2D", texturePath, clampMode);
			
				if (clampMode == TexClampMode.BORDER_S_BORDER_T) {
					// use replacement color as border (this may be incorrect)
					FloatVector4 c = new FloatVector4(
							convertTextureReplacementColor(textureReplacement, textureReplacementMode));
					//TODO: what should I do here?
					//glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, (c.x));
				}
	
				return texunit[0]++;// note the ++ applies to the arrray value after the return, bit complicated, c++y
			}
		} while (false);

		if (textureReplacementMode > 0) {
			replUniform.set(convertTextureReplacementColor(textureReplacement, textureReplacementMode));
			return -1;
		}

		return 0;
	}

	static FloatVector4 convertTextureReplacementColor(int textureReplacement, int replacementMode) {
		FloatVector4 c = new FloatVector4(textureReplacement);
		c.mult(1.0f / 255.0f);
		if (replacementMode < 2)
			return c;
		if (replacementMode == 2)
			return c;//TODO: I wonder DDSTexture16::srgbExpand( c );
		c.add(c);
		c.sub(1.0f);
		return c;
	}

	// mutha ucking c++ coders
	//https://en.cppreference.com/w/cpp/numeric/countr_one.html
	private static int countr_one(int x) {
		for (int i = 0; i < 8; i++)
			if (((1 << i) & x) == 0)
				return i;
		return 0;
	}

}
