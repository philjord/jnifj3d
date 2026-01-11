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
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiTextureTransformController;
import nif.niobject.controller.NiTimeController;
import utils.convert.NifOpenGLToJava3D;
import utils.source.BgsmSource;
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

			if (niGeometry.nVer.BS_Version < 130 && (hasSoftlight || hasRimlight)) {
				registerBind("LightMask", fileName(bslsp, 2), clamp);
			}

			// Backlight params
			boolean hasBacklight = SkyrimShaderPropertyFlags2.isBitSet(bslsp.ShaderFlags2,
					SkyrimShaderPropertyFlags2.SLSF2_Back_Lighting);
			if (sm != null)
				hasBacklight = sm.bBackLighting != 0;
			uni1i("hasBacklight", hasBacklight);

			if (niGeometry.nVer.BS_Version < 130 && hasBacklight) {
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

			if (hasSpecularMap && (niGeometry.nVer.BS_Version == 130 || !hasBacklight)) {
				if (sm == null)
					registerBind("SpecularMap", fileName(bslsp, 7), clamp);
				else
					registerBind("SpecularMap", fileName(bslsp, 2), clamp);
			}

			if (niGeometry.nVer.BS_Version == 130) {
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

			if (niGeometry.nVer.BS_Version < 130 && hasHeightMap) {
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
			EffectMaterial em = getMaterial(bsesp);

			clamp = bsesp.TextureClampMode;
			clamp.mode = clamp.mode ^ TexClampMode.MIRRORED_S_MIRRORED_T.mode;

			String SourceTexture = em == null ? bsesp.SourceTexture : em.BaseTexture;
			boolean hasSourceTexture = SourceTexture != null && SourceTexture.trim().length() > 0;
			String GreyscaleMap = em == null ? bsesp.GreyscaleTexture : em.GrayscaleTexture;
			boolean hasGreyscaleMap = GreyscaleMap != null && GreyscaleMap.trim().length() > 0;
			String EnvMap = em == null ? bsesp.EnvMapTexture : em.EnvmapTexture;
			boolean hasEnvMap = EnvMap != null && EnvMap.trim().length() > 0;
			String NormalMap = em == null ? bsesp.NormalTexture : em.NormalTexture;
			boolean hasNormalMap = NormalMap != null && NormalMap.trim().length() > 0;
			String EnvMask = em == null ? bsesp.EnvMaskTexture : em.EnvmapMaskTexture;
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
			if (niGeometry.nVer.BS_Version == 130)
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

			if (niGeometry.nVer.BS_Version == 130) {
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
								+ niGeometry.nVer.fileName);
		}

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

		// Each TexureUnit needs to be allocated to a sampler2D in the shader by getting setting the
		// TUS id to be the value of the uniform, like any other uniform
		uni1i(binding.samplerName, texunit++);
		return tus;
	}

	private boolean hasFileName(BSLightingShaderProperty bslsp, int textureSlot) {
		String fn = fileName(bslsp, textureSlot);
		return fn != null && fn.trim().length() > 0;
	}

	private static EffectMaterial getMaterial(BSEffectShaderProperty bsesp) {
		// FO4 has material files pointed at by name
		if (bsesp.name.toLowerCase().endsWith(".bgem")) {
			try {
				return BgsmSource.bgsmSource.getEffectMaterial(bsesp.name);
			} catch (ClassCastException e) {
				//ClassCastException: class nif.niobject.bgsm.ShaderMaterial cannot be cast to class nif.niobject.bgsm.EffectMaterial 
				// extracting shapes from Furniture\ProtectronPod\ProtectronPod01.nif
				System.out.println("badness trying to get an EffectMaterial called " + bsesp.name);
				e.printStackTrace();
			}
		}
		return null;
	}

	private static ShaderMaterial getMaterial(BSLightingShaderProperty bslsp) {
		// FO4 has material files pointed at by name
		if (bslsp.name.toLowerCase().endsWith(".bgsm")) {
			return BgsmSource.bgsmSource.getShaderMaterial(bslsp.name);

		}
		return null;
	}

	private String fileName(BSLightingShaderProperty bslsp, int textureSlot) {
		if (bslsp != null) {
			// FO4 has material files pointed at by name
			ShaderMaterial material = getMaterial(bslsp);
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

}
