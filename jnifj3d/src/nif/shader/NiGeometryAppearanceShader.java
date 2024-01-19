package nif.shader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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

import nif.BgsmSource;
import nif.NifVer;
import nif.appearance.NiGeometryAppearanceFixed;
import nif.compound.NifColor3;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifTexCoord;
import nif.enums.BSLightingShaderPropertyShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags1;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.TexClampMode;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
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
import nif.niobject.bgsm.EffectMaterial;
import nif.niobject.bgsm.ShaderMaterial;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSShaderLightingProperty;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiTextureTransformController;
import nif.niobject.controller.NiTimeController;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

/**
 * This will build an appearance up out of a NiGeometry that can be used by a real j3dnigeometry It is based on the
 * nifskope 2.0 renderer code from jonwd7
 * 
 * TODO: The SKYRIM TREE ANIM code in the bind, is useless but should be put into a new shader type
 * 
 * https://gist.github.com/patriciogonzalezvivo/3a81453a24a542aabc63 looks like some real good lighting equations
 */

public class NiGeometryAppearanceShader {
	public static boolean						OUTPUT_BINDINGS				= false;

	public static Material						defaultMaterial				= null;

	private NiGeometry							niGeometry;
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

	private ArrayList<ShaderAttributeValue2>	allShaderAttributeValues	= new ArrayList<ShaderAttributeValue2>();
	private ArrayList<Binding>					allTextureUnitStateBindings	= new ArrayList<Binding>();

	private int									texunit						= 0;

	private static class Binding {
		public static int	CUBE_MAP	= -123;
		public String		samplerName	= "";
		public String		fileName	= "";
		public int			clampType	= -1;

		public Binding(String samplerName, String fileName, int clampType) {
			this.samplerName = samplerName;
			this.fileName = fileName;
			this.clampType = clampType;
		}
	}

	public NiGeometryAppearanceShader(	NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource,
										Shape3D shape, J3dNiAVObject target) {
		this.niGeometry = niGeometry;
		this.niToJ3dData = niToJ3dData;
		this.textureSource = textureSource;
		this.shape = shape;
		this.target = target;

		props = new PropertyList(niGeometry.properties, niToJ3dData);

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
			if (program.isStatusOk() && setupProgram(program))
				return program.getName();
		}

		System.err.println("ARRRRRRRRRRRRRRRRRRRRRRGGGH FFP attempt " + niGeometry.nVer.fileName);
		//null mean use fixed
		return null;
	}

	
	//https://github.com/niftools/nifskope/blob/3a85ac55e65cc60abc3434cc4aaca2a5cc712eef/src/gl/renderer.cpp#L643
	//https://github.com/niftools/nifskope/blob/develop/src/gl/renderer.cpp
	private boolean setupProgram(ShaderPrograms.Program prog) {
		if (!prog.conditions.eval(niGeometry, niToJ3dData, props))
			return false;

		if (OUTPUT_BINDINGS)
			System.out.println("using prog " + prog.getName());

		this.shaderProgram = prog.shaderProgram;

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

		int clamp = TexClampMode.WRAP_S_WRAP_T;

		if (texprop != null || bsprop != null || bslsp != null) {
			if (bslsp != null) {
				clamp = bslsp.TextureClampMode.mode;
			}

			String textureUnitName = "BaseMap";
			if (texprop != null) {
				registerBind(textureUnitName, fileName(texprop, 0), clamp);
			} else if (bsprop != null) {
				registerBind(textureUnitName, fileName(bsprop, 0), clamp);
			} else if (bslsp != null) {
				registerBind(textureUnitName, fileName(bslsp, 0), clamp);
			}

			textureUnitName = "NormalMap";
			if (shaderProgram.programHasVar(textureUnitName)) {
				if (texprop != null) {
					String fname = fileName(texprop, 0);

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
					String fname = fileName(texprop, 0);

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
					ShaderMaterial sm = (ShaderMaterial)getMaterial(bslsp);
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
			ShaderMaterial sm = (ShaderMaterial)getMaterial(bslsp);

			uni1f("lightingEffect1", bslsp.LightingEffect1);
			uni1f("lightingEffect2", bslsp.LightingEffect2);

			if (sm == null)
				uni1f("alpha", bslsp.Alpha);
			else
				uni1f("alpha", sm.fAlpha);

			if (sm == null) {
				textureScale.set(bslsp.UVScale.u, bslsp.UVScale.v);
				textureOffset.set(bslsp.UVOffSet.u, bslsp.UVOffSet.v);
			} else {
				textureScale.set(sm.fUScale, sm.fVScale);
				textureOffset.set(sm.fUOffset, sm.fVOffset);
			}

			boolean hasGreyScaleColor = bslsp.ShaderFlags1
					.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteColor);
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

			boolean hasTintMask = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_FaceTint;
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
			boolean hasSoftlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Soft_Lighting);
			uni1i("hasSoftlight", hasSoftlight);

			boolean hasRimlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Rim_Lighting);
			if (sm != null)
				hasRimlight = sm.bRimLighting != 0;
			uni1i("hasRimlight", hasRimlight);

			if (niGeometry.nVer.LOAD_USER_VER2 < 130 && (hasSoftlight || hasRimlight)) {
				registerBind("LightMask", fileName(bslsp, 2), clamp);
			}

			// Backlight params
			boolean hasBacklight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Back_Lighting);
			if (sm != null)
				hasBacklight = sm.bBackLighting != 0;
			uni1i("hasBacklight", hasBacklight);

			if (niGeometry.nVer.LOAD_USER_VER2 < 130 && hasBacklight) {
				registerBind("BacklightMap", fileName(bslsp, 7), clamp);
			}

			// Glow params
			if (sm == null) {
				boolean hasEmittance = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Own_Emit);
				uni1i("hasEmit", hasEmittance);
				if (hasEmittance)
					uni1f("glowMult", bslsp.EmissiveMultiple);
				else
					uni1f("glowMult", 0);

				boolean hasGlowMap = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_GlowShader
										&& bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Glow_Map)
										&& hasFileName(bslsp, 2);
				uni1i("hasGlowMap", hasGlowMap);

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

			if (sm == null)
				uni3f("specColor", bslsp.SpecularColor.r, bslsp.SpecularColor.g, bslsp.SpecularColor.b);
			else
				uni3f("specColor", sm.cSpecularColor.r, sm.cSpecularColor.g, sm.cSpecularColor.b);

			boolean hasSpecularMap = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Specular);
			if (sm != null)
				hasSpecularMap = sm.bSpecularEnabled != 0 && hasFileName(bslsp, 2);
			uni1i("hasSpecularMap", hasSpecularMap);

			if (hasSpecularMap && (niGeometry.nVer.LOAD_USER_VER2 == 130 || !hasBacklight)) {
				if (sm == null)
					registerBind("SpecularMap", fileName(bslsp, 7), clamp);
				else
					registerBind("SpecularMap", fileName(bslsp, 2), clamp);
			}

			if (niGeometry.nVer.LOAD_USER_VER2 == 130) {
				boolean isDoubleSided = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
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
			boolean hasMultiLayerParallax = bslsp.ShaderFlags2
					.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Multi_Layer_Parallax);
			if (hasMultiLayerParallax) {
				NifTexCoord inS = bslsp.ParallaxInnerLayerTextureScale;
				uni2f("innerScale", inS.u, inS.v);

				uni1f("innerThickness", bslsp.ParallaxInnerLayerThickness);

				uni1f("outerRefraction", bslsp.ParallaxRefractionScale);
				uni1f("outerReflection", bslsp.ParallaxEnvmapStrength);

				registerBind("InnerMap", fileName(bslsp, 6), clamp);
			}

			// Environment Mapping
			boolean hasEnvironmentMap = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap
										&& bslsp.ShaderFlags1
												.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Environment_Mapping);

			hasEnvironmentMap |= bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap
									&& bslsp.ShaderFlags1
											.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Eye_Environment_Mapping);
			if (sm != null)
				hasEnvironmentMap = sm.bEnvironmentMapping != 0;

			boolean hasCubeMap = (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap
									|| bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap
									|| bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_MultiLayerParallax)
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
				if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap)
					envReflection = bslsp.EnvironmentMapScale;
				else if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap)
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
			boolean hasHeightMap = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_Heightmap;
			hasHeightMap |= bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Parallax)
							&& hasFileName(bslsp, 3);

			if (niGeometry.nVer.LOAD_USER_VER2 < 130 && hasHeightMap) {
				registerBind("HeightMap", fileName(bslsp, 3), clamp);
			}

			// vertex alpha is ignored when SF_Vertex_Animation is present
			// http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
			boolean isVertexAlphaAnimation = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim);
			uni1i("isVertexAlphaAnimation", isVertexAlphaAnimation);
		}

		// note this will be sole texturer if present
		BSEffectShaderProperty bsesp = (BSEffectShaderProperty)props.get(BSEffectShaderProperty.class);
		if (bsesp != null) {
			EffectMaterial em = (EffectMaterial)getMaterial(bsesp);

			clamp = bsesp.TextureClampMode.mode;
			clamp = clamp ^ TexClampMode.MIRRORED_S_MIRRORED_T;

			String SourceTexture = em == null ? bsesp.SourceTexture : em.textureList.get(0);
			boolean hasSourceTexture = SourceTexture != null && SourceTexture.trim().length() > 0;
			String GreyscaleMap = em == null ? bsesp.GreyscaleTexture : em.textureList.get(1);
			boolean hasGreyscaleMap = GreyscaleMap != null && GreyscaleMap.trim().length() > 0;
			String EnvMap = em == null ? bsesp.EnvMapTexture : em.textureList.get(2);
			boolean hasEnvMap = EnvMap != null && EnvMap.trim().length() > 0;
			String NormalMap = em == null ? bsesp.NormalTexture : em.textureList.get(3);
			boolean hasNormalMap = NormalMap != null && NormalMap.trim().length() > 0;
			String EnvMask = em == null ? bsesp.EnvMaskTexture : em.textureList.get(4);
			boolean hasEnvMask = EnvMask != null && EnvMask.trim().length() > 0;

			registerBind("SourceTexture", SourceTexture, clamp);

			boolean isDoubleSided = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
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

			boolean greyscaleAlpha = bsesp.ShaderFlags1
					.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteAlpha);
			if (em != null)
				greyscaleAlpha = em.bGrayscaleToPaletteAlpha != 0;
			uni1i("greyscaleAlpha", greyscaleAlpha);

			boolean greyscaleColor = bsesp.ShaderFlags1
					.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteColor);
			if (em != null)
				greyscaleColor = em.bGrayscaleToPaletteColor != 0;
			uni1i("greyscaleColor", greyscaleColor);

			boolean useFalloff = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Use_Falloff);
			if (em != null)
				useFalloff = em.bFalloffEnabled != 0;
			uni1i("useFalloff", useFalloff);

			boolean vertexAlpha = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Vertex_Alpha);
			uni1i("vertexAlpha", vertexAlpha);// no em
			boolean vertexColors = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Vertex_Colors);
			uni1i("vertexColors", vertexColors);// no em

			boolean hasWeaponBlood = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Weapon_Blood);
			if (niGeometry.nVer.LOAD_USER_VER2 == 130)
				hasWeaponBlood = false;
			uni1i("hasWeaponBlood", hasWeaponBlood);

			// Glow params
			if (em == null) {
				uni4f("glowColor", bsesp.EmissiveColor.r, bsesp.EmissiveColor.g, bsesp.EmissiveColor.b,
						bsesp.EmissiveColor.a);
				uni1f("glowMult", bsesp.EmissiveMultiple);
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

			if (niGeometry.nVer.LOAD_USER_VER2 == 130) {
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

			boolean isVertexAlphaAnimation = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim);
			uni1i("isVertexAlphaAnimation", isVertexAlphaAnimation);

		}

		// BSESP/BSLSP do not always need an NiAlphaProperty, and appear to override it at times
		boolean translucent = (bslsp != null)
								&& (bslsp.Alpha < 1.0f
									|| bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Refraction));
		translucent |= (bsesp != null) && props.get(NiAlphaProperty.class) == null && bsesp.EmissiveColor.a < 1.0f;
		
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
		depthTest |= (bslsp != null) && bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);
		depthTest |= (bsesp != null) && bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);

		if (!depthTest) {
			ra.setDepthBufferEnable(false);
		}

		boolean depthWrite = true;
		depthWrite |= (bslsp != null) && bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		depthWrite |= (bsesp != null) && bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		if (!depthWrite || translucent) {
			ra.setDepthBufferWriteEnable(false);
		}

		//override alpha prop and material
		if (translucent) {
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(TransparencyAttributes.BLEND_SRC_ALPHA);
			ta.setDstBlendFunction(TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA);

			// If mesh is alpha tested, override threshold (but not istestenabled notice)
			ra.setAlphaTestFunction(RenderingAttributes.GREATER);
			ra.setAlphaTestValue(0.1f);
		}

		//PJs decalling business
		boolean isDecal = false;
		isDecal |= (bslsp != null) && (bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Decal)
										|| bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Dynamic_Decal));
		isDecal |= (bsesp != null) && (bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Decal)
										|| bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Dynamic_Decal));
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
			if (binding.clampType == Binding.CUBE_MAP) {
				tus [i] = bindCube(binding);
			} else {
				tus [i] = bind(binding, sharable);
			}

			if (tus [i] != null) {
				if ((textureScale.x != 1 || textureScale.y != 1 || textureOffset.x != 0 || textureOffset.y != 0)) {
					Transform3D textureTransform = new Transform3D();
					textureTransform.setScale(new Vector3d(textureScale.x, textureScale.y, 0));
					textureTransform.setTranslation(new Vector3f(textureOffset.x, textureOffset.y, 0));
					//System.out.println("textureScale " + textureScale);
					//System.out.println("textureOffset " + textureOffset);
					textureAttributes.setTextureTransform(textureTransform);
					tus [i].setTextureAttributes(textureAttributes);
				}

				if (controller != null)
					tus [i].setTextureAttributes(textureAttributes);
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
															List<ShaderAttributeValue2> newShaderAttributeValues) {
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
			sas = new ShaderAttributeSet();
			sas.setCapability(ShaderAttributeSet.ALLOW_ATTRIBUTES_READ);
			for (ShaderAttributeValue sav : newShaderAttributeValues) {
				if (OUTPUT_BINDINGS)
					System.out.println(sav.getAttributeName() + " " + sav.getValue());
				sas.put(sav);
			}
			synchronized (currentShaderAttributeSets) {
				currentShaderAttributeSets.put(sas, sas);
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
			//See FO4 for testing
			ra.setDepthBufferEnable((nzp.flags.flags & 0x01) != 0);
			ra.setDepthBufferWriteEnable((nzp.flags.flags & 0x02) != 0);
			if (nzp.function != null)
				ra.setDepthTestFunction(NifOpenGLToJava3D.convertStencilFunction(nzp.function.mode));
		}
	}

	private void glMaterialZBuffer(BSMaterial m) {
		if (m != null) {
			//See FO4 for testing
			ra.setDepthBufferEnable(true);// really? not sure
			ra.setDepthBufferWriteEnable(m.bZBufferWrite != 0);
			ra.setDepthTestFunction(NifOpenGLToJava3D.convertStencilFunction(m.bZBufferTest));

		}
	}

	private void glProperty(NiMaterialProperty nmp, NiSpecularProperty nsp) {
		if (nmp != null) {
			Material mat = new Material();
			mat.setLightingEnable(true);
			mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

			if (!(nmp.nVer.LOAD_VER == NifVer.VER_20_2_0_7
					&& (nmp.nVer.LOAD_USER_VER == 11 || nmp.nVer.LOAD_USER_VER == 12)
					&& nmp.nVer.LOAD_USER_VER2 > 21)) {
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
			if (m instanceof ShaderMaterial) {
				Material mat = new Material();
				mat.setLightingEnable(true);
				mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

				// where are ambient and diffuse? mat default to 0.2 an 1 respectively

				ShaderMaterial sm = (ShaderMaterial)m;
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
		if (shaderProgram.programHasVar(var, x))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Float(x)));
	}

	// Sets a vec2 (two floats)
	private void uni2f(String var, float x, float y) {
		if (shaderProgram.programHasVar(var, x, 2))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector2f(x, y)));
	}

	// Sets a vec3 (three floats)
	private void uni3f(String var, float x, float y, float z) {
		if (shaderProgram.programHasVar(var, x, 3))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector3f(x, y, z)));
	}

	// Sets a vec4 (four floats)
	private void uni4f(String var, float x, float y, float z, float w) {
		if (shaderProgram.programHasVar(var, x, 4))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(x, y, z, w)));
	};

	// Sets a boolean
	private void uni1i(String var, boolean val) {
		if (shaderProgram.programHasVar(var, 1))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Integer(val ? 1 : 0)));
	};

	// Sets an integer  
	private void uni1i(String var, int val) {
		if (shaderProgram.programHasVar(var, val))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Integer(val)));
	};

	// Sets a mat3 (3x3 matrix)
	private void uni3m(String var, NifMatrix33 val) {
		uni3m(var, new Matrix3f(val.data()));
	}

	private void uni3m(String var, Matrix3f val) {
		if (shaderProgram.programHasVar(var, 1.0f, 3))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, val));
	};

	// Sets a mat4 (4x4 matrix)
	private void uni4m(String var, NifMatrix44 val) {
		uni4m(var, new Matrix4f(val.data()));
	};

	private void uni4m(String var, Matrix4f val) {
		if (shaderProgram.programHasVar(var, 1.0f, 4))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, val));
	};

	private void registerBindCube(String samplerName, String fileName) {
		if (shaderProgram.programHasVar(samplerName) && fileName != null && fileName.length() > 0) {
			Binding binding = new Binding(samplerName, fileName, Binding.CUBE_MAP);
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
						tcm.setImage(l, f, (ImageComponent2D)ics [l]);
				tus = new TextureUnitState();
				tus.setTexture(tcm);
				tus.setName(binding.fileName);
			}
		} else {
			System.out.println("bindCube BSLightingShaderProperty " + binding.fileName + " No Texture found for nif "
								+ niGeometry.nVer.fileName);
		}

		uni1i(binding.samplerName, texunit++);
		return tus;
	}

	private void registerBind(String samplerName, String fileName, int clamp) {
		if (shaderProgram.programHasVar(samplerName) && fileName != null && fileName.length() > 0) {
			Binding binding = new Binding(samplerName, fileName, clamp);
			allTextureUnitStateBindings.add(binding);
		}
	}

	private TextureUnitState bind(Binding binding, boolean shared) {

		TextureUnitState tus = null;
		if (!warningsGiven.contains(binding.fileName)) {
			if (shared) {
				//TODO: jonwd7 suggest texture slot is the decaling place, see his fixed pipeline
				// also these should go through as shader uniforms I reckon
				//textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
				//		: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);

				tus = J3dNiGeometry.loadTextureUnitState(binding.fileName, textureSource);
				if (tus == null && !warningsGiven.contains(binding.fileName)) {
					System.out.println("Shared TextureUnitState bind "	+ binding.fileName
										+ " no TextureUnitState found for nif " + niGeometry.nVer.fileName);
					warningsGiven.add(binding.fileName);
				}
			} else {
				Texture tex = J3dNiGeometry.loadTexture(binding.fileName, textureSource);
				if (tex == null && !warningsGiven.contains(binding.fileName)) {
					System.out.println("TextureUnitState bind " + binding.fileName + " no Texture found for nif "
										+ niGeometry.nVer.fileName);
					warningsGiven.add(binding.fileName);
					// notice tus left as null!
				} else {
					tus = new TextureUnitState();
					tus.setTexture(tex);
					tus.setName(binding.fileName);
				}
			}
		}

		// Each TexureUnit needs to be allocated to a sampler2D in the shader by getting setting the
		// TUS id to be the value of the uniform, like any other uniform
		uni1i(binding.samplerName, texunit++);
		return tus;
	}

	private static HashSet<String> warningsGiven = new HashSet<String>();

	private boolean hasFileName(BSLightingShaderProperty bslsp, int textureSlot) {
		String fn = fileName(bslsp, textureSlot);
		return fn != null && fn.trim().length() > 0;
	}

	private static BSMaterial getMaterial(BSEffectShaderProperty bsesp) {
		// FO4 has material files pointed at by name
		if (bsesp.name.toLowerCase().endsWith(".bgem")) {
			try {
				return BgsmSource.getMaterial(bsesp.name);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static BSMaterial getMaterial(BSLightingShaderProperty bslsp) {
		// FO4 has material files pointed at by name
		if (bslsp.Name.toLowerCase().endsWith(".bgsm")) {
			try {
				return BgsmSource.getMaterial(bslsp.Name);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String fileName(BSLightingShaderProperty bslsp, int textureSlot) {
		if (bslsp != null) {
			// FO4 has material files pointed at by name
			BSMaterial material = getMaterial(bslsp);
			if (material != null) {
				return material.textureList.get(textureSlot);
			} else if (bslsp.TextureSet.ref != -1) {
				BSShaderTextureSet texSet = (BSShaderTextureSet)niToJ3dData.get(bslsp.TextureSet);
				return texSet.textures [textureSlot];
			}

		}
		return "";

	}

	private String fileName(NiTexturingProperty ntp, int textureSlot) {
		//TODO: for now it appears that this is only EVER asking for 0 so let's just trap all others!
		if (textureSlot != 0) {
			new Throwable("Non 0 texSlot for TexturingProperty!").printStackTrace();
		}

		// now set the texture
		if (ntp != null && textureSlot == 0 && ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1) {
			NiSourceTexture niSourceTexture = (NiSourceTexture)niToJ3dData.get(ntp.baseTexture.source);
			return niSourceTexture.fileName.string;
		}

		return null;
	}

	private String fileName(BSShaderProperty bsprop, int textureSlot) {
		if (bsprop instanceof BSShaderPPLightingProperty) {
			BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty)bsprop;
			BSShaderTextureSet bbsts = (BSShaderTextureSet)niToJ3dData.get(bsspplp.textureSet);

			return bbsts.textures [textureSlot];
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

}
