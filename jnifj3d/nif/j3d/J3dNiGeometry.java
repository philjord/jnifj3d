package nif.j3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.NodeComponent;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAttribute;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SourceCodeShader;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Vector3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifTexDesc;
import nif.enums.BSShaderFlags;
import nif.enums.BSShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.VertMode;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiDitherProperty;
import nif.niobject.NiFogProperty;
import nif.niobject.NiGeometry;
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
import nif.niobject.bs.BSLeafAnimNode;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSRefractionFirePeriodController;
import nif.niobject.bs.BSShaderLightingProperty;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSTreeNode;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.Lighting30ShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public abstract class J3dNiGeometry extends J3dNiAVObject
{

	private static HashMap<NiProperty, NodeComponent> propertyLookup = new HashMap<NiProperty, NodeComponent>();

	private static HashMap<BSLightingShaderProperty, NodeComponent> bsLightingShaderPropertyLookup = new HashMap<BSLightingShaderProperty, NodeComponent>();

	//private ShaderAppearance app = new ShaderAppearance();

	private Appearance app = new Appearance();

	private Shape3D shape;

	private TextureSource textureSource;

	public J3dNiGeometry(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		this(niGeometry, niToJ3dData, textureSource, null);
	}

	/**
	 * note a non null customShape will ahve it's name set and be refed by the getShape but will not be added as a child
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

		shape.setAppearance(app);
		configureAppearance(niGeometry, niToJ3dData);

		//Some times the nif just has no texture, odd. see BSShaderNoLightingProperty

	}

	public Shape3D getShape()
	{
		return shape;
	}

	private void configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData)
	{
		NifRef[] properties = niGeometry.properties;

		Material mat = new Material();
		mat.setLightingEnable(true);
		mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
		app.setMaterial(mat);

		RenderingAttributes ra = new RenderingAttributes();
		TextureAttributes textureAttributes = new TextureAttributes();

		//TODO: this might be set by the texturing and the ppshader properties?
		textureAttributes.setTextureMode(TextureAttributes.MODULATE);
		app.setTextureAttributes(textureAttributes);

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
							app.setTextureAttributes((TextureAttributes) propertyLookup.get(ntp));
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
									app.setTexture(tex);
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
								app.setTexture(tex);
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
								app.setTexture(tex);
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

							app.setTexture(tex);
						}

						//configureShader(bssnlp);
					}
					else if (property instanceof TallGrassShaderProperty)
					{
						TallGrassShaderProperty tgsp = (TallGrassShaderProperty) property;
						Texture tex = loadTexture(tgsp.fileName, textureSource);
						if (tex != null)
						{
							app.setTexture(tex);
						}

						//configureShader(tgsp);
					}
					else if (property instanceof TileShaderProperty)
					{
						TileShaderProperty tsp = (TileShaderProperty) property;
						Texture tex = loadTexture(tsp.fileName, textureSource);
						if (tex != null)
						{
							app.setTexture(tex);
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
							//TODO: I think possibly the PolygonAttributes.CULL_NONE should be applied to anything 
							//wiht an alphaTestEnabled()? , But no many alpha object have 2 one sided geom on top each other
							//PolygonAttributes pa = new PolygonAttributes();
							//pa.setCullFace(PolygonAttributes.CULL_NONE);
							//app.setPolygonAttributes(pa);

							int alphaTestMode = NifOpenGLToJava3D.convertAlphaTestMode(nap.alphaTestMode());
							ra.setAlphaTestFunction(alphaTestMode);

							float threshold = ((nap.threshold) / 255f);//threshold range of 255(trans) to 0 (opaque) comfirmed empirically

							ra.setAlphaTestValue(threshold);

							// this is garbage but fixes the debate between giantobelisk03.nif and floralavender01.nif
							// but screws up treeaspen01.nif treeaspen want the ignore to be done too but has same falgs as obelisk
							// I think I want to always ignore alpha values in trishape color data
							// rock has alpha values of 0x21 on the painting trishape
							//this is a temp rubbish thing
							if (niToJ3dData.root() instanceof BSTreeNode || niToJ3dData.root() instanceof BSLeafAnimNode)
								ra.setIgnoreVertexColors(true);

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
							app.setTextureAttributes((TextureAttributes) propertyLookup.get(bsesp));
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
								app.setTexture(tex);
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
							app.setTexture(tex);
						}

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
						app.setTextureAttributes((TextureAttributes) bsLightingShaderPropertyLookup.get(bslsp));
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
							app.setTexture(tex);

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

				}
				else
				{
					System.out.println("Unhandled property in geometry " + prop);
				}
			}
		}
	}

	public static Texture loadTexture(String texName, TextureSource textureSource)
	{
		if (textureSource != null && texName != null && texName.length() > 0)
		{
			return textureSource.getTexture(texName);
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
							startTimeS, stopTimeS, -1);
				}

				if (j3dNiInterpolator != null)
				{
					addChild(j3dNiInterpolator);

					Alpha baseAlpha = J3dNiTimeController.createAlpha(startTimeS, stopTimeS, -1);
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

	//	private void setTexture0Ex(Appearance app, String texName, String bumpName, String imageDir)
	//	{
	//		if (texName.length() > 0 && bumpName.length() > 0)
	//		{
	//			// remove incorrect file path
	//			if (texName.startsWith("Data\\"))
	//			{
	//				texName = texName.substring(5);
	//			}
	//			if (bumpName.startsWith("Data\\"))
	//			{
	//				bumpName = bumpName.substring(5);
	//			}
	//
	//			if (texName.endsWith(".dds") && bumpName.endsWith("_n.dds"))
	//			{
	//				Texture textureColor = DDSToTexture.getTexture(new File(imageDir + texName));
	//				Texture textureDOT3NormalMap = DDSToTexture.getTexture(new File(imageDir + bumpName));
	//
	//				TextureUnitState[] stateArray = setupTextureUnitState(textureDOT3NormalMap, textureColor);
	//				//app.setTexture(tex);
	//				app.setTextureUnitState(stateArray);
	//			}
	//
	//		}
	//	}
	//
	//	// TextureUnitStates used in this application
	//
	//	TextureUnitState tuDOT3NormalMap;
	//
	//	TextureUnitState tuColor;
	//
	//	/** Where the TUs are applied *
	//	TextureUnitState[] tusArr;
	//
	//	*
	//	* setup TextureUnitStates used in this demo.     *
	//	* @return
	//	*/
	//	private TextureUnitState[] setupTextureUnitState(Texture textureDOT3NormalMap, Texture textureColor)
	//	{
	//		//texture Attributes for DOT3 normal map
	//		TextureAttributes textAttDot3 = new TextureAttributes();
	//
	//		TextureAttributes texAttColor = new TextureAttributes();
	//		texAttColor.setTextureMode(TextureAttributes.COMBINE);
	//
	//		//CombineRgbMode could be also COMBINE_ADD or COMBINE_ADD_SIGNED, with
	//		//different results
	//		texAttColor.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
	//
	//		textAttDot3.setTextureMode(TextureAttributes.COMBINE);
	//		textAttDot3.setCombineRgbMode(TextureAttributes.COMBINE_DOT3);
	//		textAttDot3.setCombineAlphaMode(TextureAttributes.COMBINE_DOT3);
	//		//textAttDot3.setTextureBlendColor(1.f, 1.0f, 1.0f, 0.0f);
	//
	//		// setup functions
	//		textAttDot3.setCombineRgbFunction(0, TextureAttributes.COMBINE_SRC_COLOR);
	//		textAttDot3.setCombineRgbFunction(1, TextureAttributes.COMBINE_SRC_COLOR);
	//		//combine with previous TUS, lightMap
	//		textAttDot3.setCombineRgbSource(0, TextureAttributes.COMBINE_TEXTURE_COLOR);
	//		textAttDot3.setCombineRgbSource(1, TextureAttributes.COMBINE_OBJECT_COLOR);
	//
	//		// create TUS
	//		tuDOT3NormalMap = new TextureUnitState(textureDOT3NormalMap, textAttDot3, null);
	//		tuColor = new TextureUnitState(textureColor, texAttColor, null);
	//
	//		// this TUS array is used by geometry at runtime
	//		TextureUnitState[] tus = new TextureUnitState[2];
	//		tus[0] = tuDOT3NormalMap;
	//		tus[1] = tuColor;
	//
	//		return tus;
	//	}
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