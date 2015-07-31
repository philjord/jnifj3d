package nif.j3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.GLSLShaderProgram;
import javax.media.j3d.Material;
import javax.media.j3d.NodeComponent;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAppearance;
import javax.media.j3d.ShaderAttribute;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.ShaderProgram;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SourceCodeShader;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Vector3d;

import nif.NifToJ3d;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifTexDesc;
import nif.enums.BSShaderFlags;
import nif.enums.BSShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.VertMode;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiDitherProperty;
import nif.niobject.NiFogProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiGeometryData;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiMultiTextureProperty;
import nif.niobject.NiObject;
import nif.niobject.NiProperty;
import nif.niobject.NiShadeProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiSpecularProperty;
import nif.niobject.NiStencilProperty;
import nif.niobject.NiTextureModeProperty;
import nif.niobject.NiTextureProperty;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiVertexColorProperty;
import nif.niobject.NiWireframeProperty;
import nif.niobject.NiZBufferProperty;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSRefractionFirePeriodController;
import nif.niobject.bs.BSShaderLightingProperty;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.Lighting30ShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;
import tools3d.utils.scenegraph.Fadable;
import utils.convert.ConvertFromNif;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

import com.sun.j3d.utils.shader.StringIO;

public abstract class J3dNiGeometry extends J3dNiAVObject implements Fadable
{

	private static HashMap<NiProperty, NodeComponent> propertyLookup = new HashMap<NiProperty, NodeComponent>();

	private static HashMap<BSLightingShaderProperty, NodeComponent> bsLightingShaderPropertyLookup = new HashMap<BSLightingShaderProperty, NodeComponent>();

	private Appearance normalApp;

	private Shape3D shape;

	private TextureSource textureSource;

	private TransparencyAttributes normalTA = null;

	private TransparencyAttributes faderTA = null;

	public J3dNiGeometry(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		this(niGeometry, niToJ3dData, textureSource, null);
	}

	/**
	 * note a non null customShape will have it's name set and be refed by the getShape but will not be added as a child
	 * @param niGeometry
	 * @param blocks
	 * @param niToJ3dData
	 * @param imageDir
	 * @param customShape
	 */
	public J3dNiGeometry(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D customShape)
	{
		super(niGeometry, niToJ3dData);
		this.textureSource = textureSource;

		if (customShape == null)
		{
			shape = new Shape3D();

			addChild(shape);
		}
		else
		{
			//Don't add as this is for particles which is different
			shape = customShape;
		}
		shape.setName("" + this.getClass().getSimpleName() + ":" + niGeometry.name);

		NiGeometryData data = (NiGeometryData) niToJ3dData.get(niGeometry.data);

		shape.setBoundsAutoCompute(false);
		shape.setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center), ConvertFromNif.toJ3d(data.radius)));

		if (!NifToJ3d.USE_SHADERS)
		{
			normalApp = new Appearance();
		}
		else
		{
			normalApp = new ShaderAppearance();
		}
		configureAppearance(niGeometry, niToJ3dData, normalApp);

		//Some times the nif just has no texture, odd. see BSShaderNoLightingProperty

		// Various parts to allow fading in and out
		normalTA = normalApp.getTransparencyAttributes();
		normalApp.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
		normalApp.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
		faderTA = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0f);
		faderTA.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);

		if (NifToJ3d.USE_SHADERS)
		{

			if (shaderProgram == null)
			{
				try
				{
					vertexProgram = StringIO.readFully("./fixedpipeline.vert");
					fragmentProgram = StringIO.readFully("./fixedpipeline.frag");
				}
				catch (IOException e)
				{
					System.err.println(e);
				}

				Shader[] shaders = new Shader[2];
				shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram);
				shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram);
				final String[] shaderAttrNames =
				{ "tex" };
				final Object[] shaderAttrValues =
				{ new Integer(0) };
				shaderProgram = new GLSLShaderProgram();
				shaderProgram.setShaders(shaders);
				shaderProgram.setShaderAttrNames(shaderAttrNames);

				// Create the shader attribute set
				shaderAttributeSet = new ShaderAttributeSet();
				for (int i = 0; i < shaderAttrNames.length; i++)
				{
					ShaderAttribute shaderAttribute = new ShaderAttributeValue(shaderAttrNames[i], shaderAttrValues[i]);
					shaderAttributeSet.put(shaderAttribute);
				}

				// Create shader appearance to hold the shader program and
				// shader attributes
			}
			((ShaderAppearance) normalApp).setShaderProgram(shaderProgram);
			((ShaderAppearance) normalApp).setShaderAttributeSet(shaderAttributeSet);

			//transparency
			//fog
			//lights (4?)

			//then bump and glow maps

			//land multi texturing(fixed should work too)
			//lod why no nigeometry?

		}

	}

	private static ShaderProgram shaderProgram = null;

	private static ShaderAttributeSet shaderAttributeSet = null;

	private static String vertexProgram = null;

	private static String fragmentProgram = null;

	public Shape3D getShape()
	{
		if (shape.getAppearance() == null)
		{
			new Throwable("what?").printStackTrace();
			;
		}

		return shape;
	}

	private void configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, Appearance app)
	{
		NifRef[] properties = niGeometry.properties;

		Material mat = new Material();
		mat.setLightingEnable(true);
		mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
		app.setMaterial(mat);

		RenderingAttributes ra = new RenderingAttributes();
		TextureUnitState[] tus = new TextureUnitState[1];
		TextureUnitState tus0 = new TextureUnitState();
		tus[0] = tus0;
		TextureAttributes textureAttributes = new TextureAttributes();

		//TODO: this might be set by the texturing and the ppshader properties?

		textureAttributes.setTextureMode(TextureAttributes.MODULATE);

		tus0.setTextureAttributes(textureAttributes);

		app.setTextureUnitState(tus);

		// note time controllers below need appearance set on the shape now
		shape.setAppearance(normalApp);

		//don't set unless needed
		TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0f);

		//apply various apperance properties
		for (int i = 0; i < properties.length; i++)
		{
			NiObject prop = niToJ3dData.get(properties[i]);
			if (prop != null)
			{
				if (prop instanceof NiProperty)
				{
					NiProperty property = (NiProperty) prop;
					if (property instanceof NiTexturingProperty)
					{
						NiTexturingProperty ntp = (NiTexturingProperty) property;

						// have we already constructed it?
						if (propertyLookup.get(ntp) != null)
						{
							tus0.setTextureAttributes((TextureAttributes) propertyLookup.get(ntp));
						}
						else
						{
							textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
									: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);

							//TODO: I wonder if the highlight mode is equal to TextureAttribute COMBINE?
							setUpTimeController(ntp, niToJ3dData);

							propertyLookup.put(ntp, textureAttributes);

						}

						//now set the texture
						if (ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1 && textureSource != null)
						{
							NiSourceTexture nst = (NiSourceTexture) niToJ3dData.get(ntp.baseTexture.source);
							if (nst.useExternal != 0)
							{
								Texture tex = loadTexture(nst.fileName.string, textureSource);
								if (tex != null)
								{
									tus0.setTexture(tex);
								}

							}
						}

						//Black Prophecy textures order?
						// engines sometimes only have 1 or 2  
						//occulsion (occ_blank.dds) - optional?
						//color	(diff_blank.dds)
						//bump
						//specular
						//glow (glow_blank.dds)
						//cubemap, for reflections? - optional

						NiSourceTexture niSourceTexture = null;

						// use it if there's only 1
						if (ntp.shaderTextures.length == 1)
						{
							NifTexDesc textureData = ntp.shaderTextures[0].textureData;
							if (textureData.source.ref != -1)
							{
								niSourceTexture = (NiSourceTexture) niToJ3dData.get(textureData.source);
							}
						}
						else if (ntp.shaderTextures.length > 1)
						{
							// use 0 unless occulsion, in which case use 1
							NifTexDesc textureData = ntp.shaderTextures[0].textureData;
							if (textureData.source.ref != -1)
							{
								niSourceTexture = (NiSourceTexture) niToJ3dData.get(textureData.source);
								if (niSourceTexture.fileName.string.contains("_lod")
										|| niSourceTexture.fileName.string.equals("occ_blank.dds")
										|| niSourceTexture.fileName.string.contains("_occ"))
								{
									textureData = ntp.shaderTextures[1].textureData;
									if (textureData.source.ref != -1)
									{
										niSourceTexture = (NiSourceTexture) niToJ3dData.get(textureData.source);
									}
								}
							}

						}

						if (niSourceTexture != null)
						{
							Texture tex = loadTexture(niSourceTexture.fileName.string, textureSource);

							if (tex != null)
							{
								tus0.setTexture(tex);
							}

						}

					}
					else if (property instanceof BSShaderPPLightingProperty)
					{
						BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty) property;

						BSShaderTextureSet bbsts = (BSShaderTextureSet) niToJ3dData.get(bsspplp.textureSet);

						if (bbsts.numTextures > 0 && textureSource != null)
						{
							Texture tex = loadTexture(bbsts.textures[0], textureSource);
							if (tex != null)
							{
								tus0.setTexture(tex);
							}
						}

						// refraction lighting is troublesome without shaders, disable for now, or if no texture, or parallax
						if (bsspplp.shaderFlags.isBitSet(BSShaderFlags.SF_REFRACTION)
								|| bsspplp.shaderFlags.isBitSet(BSShaderFlags.SF_FIRE_REFRACTION) || bbsts.numTextures == 0)
						{
							ra.setVisible(false);
							app.setRenderingAttributes(ra);
						}
						//configureShader(bsspplp);

						setUpTimeController(bsspplp, niToJ3dData);
					}
					else if (property instanceof BSShaderNoLightingProperty)
					{
						BSShaderNoLightingProperty bssnlp = (BSShaderNoLightingProperty) property;

						textureAttributes.setTextureMode(TextureAttributes.REPLACE);

						if (bssnlp.shaderType.type == BSShaderType.SHADER_NOLIGHTING)
						{
							mat.setLightingEnable(false);
						}

						// refraction lighting is troublesome without shaders, disable for now, or if no texture, or parallax
						if (bssnlp.shaderFlags.isBitSet(BSShaderFlags.SF_REFRACTION)
								|| bssnlp.shaderFlags.isBitSet(BSShaderFlags.SF_FIRE_REFRACTION) || bssnlp.fileName.length() == 0)
						{
							ra.setVisible(false);
							app.setRenderingAttributes(ra);
						}

						// glow map texture
						Texture tex = loadTexture(bssnlp.fileName, textureSource);
						if (tex != null)
						{

							tus0.setTexture(tex);
						}

						//configureShader(bssnlp);
					}
					else if (property instanceof TallGrassShaderProperty)
					{
						TallGrassShaderProperty tgsp = (TallGrassShaderProperty) property;
						Texture tex = loadTexture(tgsp.fileName, textureSource);
						if (tex != null)
						{
							tus0.setTexture(tex);
						}

						//configureShader(tgsp);
					}
					else if (property instanceof TileShaderProperty)
					{
						TileShaderProperty tsp = (TileShaderProperty) property;
						Texture tex = loadTexture(tsp.fileName, textureSource);
						if (tex != null)
						{
							tus0.setTexture(tex);
						}

						//configureShader(tsp);
					}
					else if (property instanceof NiMaterialProperty)
					{
						NiMaterialProperty nmp = (NiMaterialProperty) property;

						if (!(niToJ3dData.nifVer.LOAD_VER == NifVer.VER_20_2_0_7
								&& (niToJ3dData.nifVer.LOAD_USER_VER == 11 || niToJ3dData.nifVer.LOAD_USER_VER == 12) && niToJ3dData.nifVer.LOAD_USER_VER2 > 21))
						{
							mat.setAmbientColor(nmp.ambientColor.r, nmp.ambientColor.g, nmp.ambientColor.b);
							mat.setDiffuseColor(nmp.diffuseColor.r, nmp.diffuseColor.g, nmp.diffuseColor.b);
						}
						mat.setSpecularColor(nmp.specularColor.r, nmp.specularColor.g, nmp.specularColor.b);
						mat.setEmissiveColor(nmp.emissiveColor.r, nmp.emissiveColor.g, nmp.emissiveColor.b);

						mat.setShininess(nmp.glossiness);

						if (nmp.alpha != 1.0f)
						{
							ta.setTransparency(1 - nmp.alpha);
							ta.setTransparencyMode(TransparencyAttributes.BLENDED);
							app.setTransparencyAttributes(ta);
						}

					}
					else if (property instanceof NiVertexColorProperty)
					{
						NiVertexColorProperty nvcp = (NiVertexColorProperty) niToJ3dData.get(properties[i]);
						if (nvcp.vertexMode != null)
						{
							if (nvcp.vertexMode.mode == VertMode.VERT_MODE_SRC_IGNORE)
							{
								ra.setIgnoreVertexColors(true);
							}
							else
							{
								mat.setColorTarget(NifOpenGLToJava3D.convertVertexMode(nvcp.vertexMode.mode));
							}
						}
					}
					else if (property instanceof NiAlphaProperty)
					{
						NiAlphaProperty nap = (NiAlphaProperty) property;

						if (nap.alphaBlendingEnable())
						{
							ta.setTransparencyMode(TransparencyAttributes.BLENDED);
							ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.sourceBlendMode(), true));
							ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.destinationBlendMode(), false));
							app.setTransparencyAttributes(ta);
						}
						else
						{
							ta.setTransparencyMode(TransparencyAttributes.SCREEN_DOOR);
						}

						if (nap.alphaTestEnabled())
						{
							//I think the PolygonAttributes.CULL_NONE should be applied to anything 
							//with an alphaTestEnabled(), flat_lod trees from skyrim prove it 
							//obviously transparent stuff can be seen from the back quite often
							PolygonAttributes pa = new PolygonAttributes();
							pa.setCullFace(PolygonAttributes.CULL_NONE);
							app.setPolygonAttributes(pa);

							int alphaTestMode = NifOpenGLToJava3D.convertAlphaTestMode(nap.alphaTestMode());
							ra.setAlphaTestFunction(alphaTestMode);

							float threshold = ((nap.threshold) / 255f);//threshold range of 255 to 0  comfirmed empirically

							ra.setAlphaTestValue(threshold);

							app.setRenderingAttributes(ra);
						}

					}
					else if (property instanceof NiStencilProperty)
					{

						NiStencilProperty nsp = (NiStencilProperty) property;

						// - this dictates the two sided polygon (e.g. butterfly)			
						if (nsp.getDrawMode() == FaceDrawMode.DRAW_BOTH)
						{
							PolygonAttributes pa = new PolygonAttributes();
							pa.setCullFace(PolygonAttributes.CULL_NONE);
							app.setPolygonAttributes(pa);
						}

						/*
						 * Note Although it works (with the slight mod below) for a single model nicely
						 * When multiple models are involved, the transparent ones (like light glow) are sorted and rendered back to front
						 * This means we get the opposite effect, where the back most glow gets to stamp the stencil and the front one leaves a space for it
						 * which obviously looks madness. Maybe shaders deal with this for real?
						 * Possibly setting the stencilbuffer to 0 after each render might help?
						 */
						if (nsp.isStencilEnable() && false)//isStencilEnable tends to be false even though the prop is attached
						{
							ra.setStencilEnable(true);

							//ra.setStencilFunction(NifOpenGLToJava3D.convertStencilFunction(nsp.stencilFunction()), nsp.stencilRef, nsp.stencilMask);
							ra.setStencilFunction(RenderingAttributes.GREATER_OR_EQUAL, nsp.stencilRef, nsp.stencilMask);
							ra.setStencilOp(NifOpenGLToJava3D.convertStencilAction(nsp.failAction()), //
									NifOpenGLToJava3D.convertStencilAction(nsp.zFailAction()),//
									NifOpenGLToJava3D.convertStencilAction(nsp.passAction()));

							app.setRenderingAttributes(ra);
						}
					}
					else if (property instanceof BSEffectShaderProperty)
					{
						BSEffectShaderProperty bsesp = (BSEffectShaderProperty) property;

						// have we already constructed the texture attributes and time controller it?
						if (propertyLookup.get(bsesp) != null)
						{
							tus0.setTextureAttributes((TextureAttributes) propertyLookup.get(bsesp));
						}
						else
						{
							if (bsesp.UVOffSet.u != 0 || bsesp.UVOffSet.v != 0 || bsesp.UVScale.u != 1 || bsesp.UVScale.v != 1
									|| bsesp.controller.ref != -1)
							{

								Transform3D transform = new Transform3D();
								transform.setTranslation(new Vector3d(-bsesp.UVOffSet.u, -bsesp.UVOffSet.v, 0));
								transform.setScale(new Vector3d(bsesp.UVScale.u, bsesp.UVScale.v, 0));
								textureAttributes.setTextureTransform(transform);
							}

							setUpTimeController(bsesp, niToJ3dData);

							propertyLookup.put(bsesp, textureAttributes);
						}

						//now set the texture
						if (bsesp.SourceTexture.length() > 0 && textureSource != null)
						{
							Texture tex = loadTexture(bsesp.SourceTexture, textureSource);
							if (tex != null)
							{
								tus0.setTexture(tex);
							}
						}

						mat.setEmissiveColor(bsesp.EmissiveColor.r, bsesp.EmissiveColor.g, bsesp.EmissiveColor.b);

					}
					else if (property instanceof NiZBufferProperty)
					{
						//uncommon
						// RenderingAttributes  
					}
					else if (property instanceof NiShadeProperty)
					{
						//uncommon
					}
					else if (property instanceof NiSpecularProperty)
					{
						//uncommon
					}
					else if (property instanceof NiWireframeProperty)
					{
						//uncommon
					}
					else if (property instanceof NiDitherProperty)
					{
						//uncommon
					}
					else if (property instanceof NiFogProperty)
					{
						//uncommon
					}
					else if (property instanceof NiTextureModeProperty)
					{
						//uncommmon, related to PS2?
					}
					else if (property instanceof NiMultiTextureProperty)
					{
						//only in  3.1?	
					}
					else if (property instanceof NiTextureProperty)
					{
						//only in ver3.0 to 3.1?				
					}
					else if (property instanceof BSWaterShaderProperty)
					{
						//TODO: BSWaterShaderProperty
						//System.out.println("BSWaterShaderProperty");
					}
					else if (property instanceof BSSkyShaderProperty)
					{
						BSSkyShaderProperty bsssp = (BSSkyShaderProperty) property;

						Texture tex = loadTexture(bsssp.SourceTexture, textureSource);
						if (tex != null)
						{
							tus0.setTexture(tex);
						}
					}
					else if (property instanceof WaterShaderProperty)
					{
						//TODO: WaterShaderProperty	
					}
					else if (property instanceof SkyShaderProperty)
					{
						//TODO: SkyShaderProperty				
					}
					else
					{
						System.out.println("J3dNiGeometry - unhandled property " + property);
					}
				}
				else if (prop instanceof BSLightingShaderProperty)
				{
					BSLightingShaderProperty bslsp = (BSLightingShaderProperty) prop;
					//				System.out.println("BSLightingShaderProperty!!!!!!!!!!!!!!!");

					// have we already constructed it?
					if (bsLightingShaderPropertyLookup.get(bslsp) != null)
					{
						tus0.setTextureAttributes((TextureAttributes) bsLightingShaderPropertyLookup.get(bslsp));
					}
					else
					{
						if (bslsp.UVOffSet.u != 0 || bslsp.UVOffSet.v != 0 || bslsp.UVScale.u != 1 || bslsp.UVScale.v != 1
								|| bslsp.controller.ref != -1)
						{
							Transform3D transform = new Transform3D();
							transform.setTranslation(new Vector3d(-bslsp.UVOffSet.u, -bslsp.UVOffSet.v, 0));
							transform.setScale(new Vector3d(bslsp.UVScale.u, bslsp.UVScale.v, 0));
							textureAttributes.setTextureTransform(transform);

						}

						NiSingleInterpController controller = (NiSingleInterpController) niToJ3dData.get(bslsp.controller);
						setUpTimeController(controller, niToJ3dData);

						bsLightingShaderPropertyLookup.put(bslsp, textureAttributes);
					}

					//now set the texture
					if (bslsp.TextureSet.ref != -1)
					{
						BSShaderTextureSet texSet = (BSShaderTextureSet) niToJ3dData.get(bslsp.TextureSet);

						Texture tex = loadTexture(texSet.textures[0], textureSource);
						if (tex != null)
						{
							tus0.setTexture(tex);
						}

					}

					float specStrength = (bslsp.SpecularStrength / 999f);
					mat.setSpecularColor(bslsp.SpecularColor.r * specStrength, //
							bslsp.SpecularColor.g * specStrength,//
							bslsp.SpecularColor.b * specStrength);

					mat.setShininess((bslsp.Glossiness / 999f) * 128f);

					mat.setEmissiveColor(bslsp.EmissiveColor.r, bslsp.EmissiveColor.g, bslsp.EmissiveColor.b);

					if (bslsp.Alpha != 1.0f)
					{
						ta.setTransparency(1f - bslsp.Alpha);
						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
						app.setTransparencyAttributes(ta);
					}

					// apparently the The vertex colors are used as well, just not the alpha component when SF_Vertex_Animation is present
					//http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
					if (bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim))
					{
						textureAttributes.setTextureMode(TextureAttributes.COMBINE);
						textureAttributes.setCombineAlphaMode(TextureAttributes.COMBINE_REPLACE);
					}
				}
				else
				{
					System.out.println("Unhandled property in geometry " + prop);
				}
			}
		}

	}

	public Texture loadTexture(String texName, TextureSource ts)
	{
		if (ts != null && texName != null && texName.length() > 0)
		{
			return ts.getTexture(texName);
		}

		return null;
	}

	/**
	 * NOTE This is not like the J3dNiObjectNET controller set up as the J3dNiGeometry uses it to control only it's appearance parts 
	 * So it only handles time controller that will appear in NiAVObject.properties for NiGeometry types
	 * Notice that teh controller comes off a NiObjectNET object not a J3dNiObjectNET objects, hence J3dNiObjectNET.setupController is
	 * never called on the object so we don't get double creation
	 * I should however at some point mere this with NiObjectNET set up controllers, the nodeTarget is all that differs now
	 * @param parent
	 * @param niObjectNET
	 * @param app
	 * @param blocks
	 */
	private void setUpTimeController(NiProperty property, NiToJ3dData niToJ3dData)
	{
		NiTimeController controller = (NiTimeController) niToJ3dData.get(property.controller);
		setUpTimeController(controller, niToJ3dData);
	}

	private void setUpTimeController(NiTimeController controller, NiToJ3dData niToJ3dData)
	{
		if (controller != null)
		{
			if (controller instanceof NiSingleInterpController)
			{
				NiSingleInterpController niSingleInterpController = (NiSingleInterpController) controller;
				float startTimeS = controller.startTime;
				float stopTimeS = controller.stopTime;

				NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(niSingleInterpController.interpolator);

				J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, niToJ3dData, this,
						textureSource);

				J3dNiInterpolator j3dNiInterpolator = null;
				if (j3dNiTimeController != null)
				{
					j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator, niToJ3dData,
							startTimeS, stopTimeS);
				}

				if (j3dNiInterpolator != null)
				{
					addChild(j3dNiInterpolator);

					Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
					j3dNiInterpolator.fire(baseAlpha);
				}

				NiTimeController nextController = (NiTimeController) niToJ3dData.get(controller.nextController);
				if (nextController != null)
				{
					if (nextController instanceof NiMultiTargetTransformController)
					{
						//this is an object palette ignore
					}
					else
					{
						//TODO: this is used by the particle modifer controller system, see J3dParticleSystem
						//I've also seen texturetransform controllers U then V use this						
						setUpTimeController(nextController, niToJ3dData);
					}

				}

			}
			else if (controller instanceof BSRefractionFirePeriodController)
			{

			}
			else
			{
				System.out.println("non NiSingleInterpController for j3dgeometry " + controller);
			}
		}
	}

	private float currentTrans = -1f;

	/**
	 * the appearance's transparency in the range [0.0, 1.0] with 0.0 being fully opaque and 1.0 being fully transparent
	 * @see tools3d.utils.scenegraph.Fadable#fade(float)
	 */
	@Override
	public void fade(float percent)
	{
		//setting transparency is expensive early out it if possible
		if (currentTrans != percent)
		{
			if (percent <= 0 || percent >= 1.0f && normalApp.getTransparencyAttributes() != normalTA)
			{
				//System.out.println("set normal");
				normalApp.setTransparencyAttributes(normalTA);
			}
			else
			{
				if (normalApp.getTransparencyAttributes() != faderTA)
					normalApp.setTransparencyAttributes(faderTA);

				//	System.out.println("fade set to " + percent);
				faderTA.setTransparency(percent);
			}
			currentTrans = percent;
		}
	}

	private void configureShader(BSShaderProperty bssp)
	{

		if (bssp instanceof BSShaderLightingProperty)
		{
			BSShaderLightingProperty bsslp = (BSShaderLightingProperty) bssp;
			if (bsslp instanceof BSShaderPPLightingProperty)
			{
				BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty) bsslp;

				if (bsspplp instanceof Lighting30ShaderProperty)
				{
					Lighting30ShaderProperty lsp = (Lighting30ShaderProperty) bsspplp;
				}

				/*
				 * ST<*> stands from shader tree with STB=branch STFROND = frond STLEAF=leaf
				 * SL<*> stand for lighting S for standard?
				 * GRASS = tall grass
				 * 
				 * 
				SHADER_TALL_GRASS = GRASS*
				SHADER_DEFAULT = ?
				SHADER_SKY =  SKY*
				SHADER_WATER =  
				SHADER_LIGHTING30 SL*
				SHADER_TILE =  
				SHADER_NOLIGHTING  =
				 */

				//bsspplp.unknownInt2 values like 0, 1, 9, 16385, 32769, 40961 looks like bit flags 2,4,36 found in landscape
				//bsspplp.unknownInt3 value 3, 0 0 on some robot files, 0 on some landscape				
				String shaderProgName = "";

				if (bssp.shaderType.type == BSShaderType.SHADER_DEFAULT)
				{
					shaderProgName += "";
				}

				//File sf = new File(".\\simple_fp.cg");
				File sf = new File(".\\multitex_fp.cg");

				try
				{
					BufferedReader fr = new BufferedReader(new FileReader(sf));

					String fragmentProgram = "";
					String line = fr.readLine();
					while (line != null)
					{
						fragmentProgram += line + "\n";
						line = fr.readLine();
					}

					fr.close();

					Shader[] shaders = new Shader[1];
					shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_CG, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram);
					final String[] shaderAttrNames =
					{ "cloudFactor" };
					final Object[] shaderAttrValues =
					{ new Float(0.6f), };
					//ShaderProgram shaderProgram = new CgShaderProgram();
					//shaderProgram.setShaders(shaders);
					//shaderProgram.setShaderAttrNames(shaderAttrNames);

					// Create the shader attribute set
					ShaderAttributeSet shaderAttributeSet = new ShaderAttributeSet();
					for (int i = 0; i < shaderAttrNames.length; i++)
					{
						ShaderAttribute shaderAttribute = new ShaderAttributeValue(shaderAttrNames[i], shaderAttrValues[i]);
						shaderAttributeSet.put(shaderAttribute);
					}

					// Create shader appearance to hold the shader program and
					// shader attributes

					//app.setShaderProgram(shaderProgram);
					//app.setShaderAttributeSet(shaderAttributeSet);

				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

			}
		}

	}
}
