package nif.appearance;

import java.io.IOException;
import java.util.WeakHashMap;

import javax.media.j3d.Alpha;
import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.NodeComponent;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Vector3d;

import nif.BgsmSource;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifTexDesc;
import nif.enums.BSShaderFlags;
import nif.enums.BSShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.VertMode;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
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
import nif.niobject.bgsm.BSMaterial;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSRefractionFirePeriodController;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiSingleInterpController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

public class NiGeometryAppearanceFixed implements NiGeometryAppearance
{
	private static WeakHashMap<Object, TextureAttributes> textureAttributesLookup = new WeakHashMap<Object, TextureAttributes>();

	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target)
	{
		NifRef[] properties = niGeometry.properties;

		Appearance app = new Appearance();
		Material mat = new Material();
		mat.setLightingEnable(true);
		mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
		mat.setShininess(0.33f * 128);
		app.setMaterial(mat);

		// NOTE must set these as teh default behavior with null breaks stenciling
		// TODO: raise a bug about this and stencils in the bugzilla
		// RAISE_BUG:
		RenderingAttributes ra = new RenderingAttributes();
		app.setRenderingAttributes(ra);

		PolygonAttributes pa = new PolygonAttributes();
		app.setPolygonAttributes(pa);

		TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NONE, 0f);
		app.setTransparencyAttributes(ta);

		TextureUnitState[] tus = new TextureUnitState[1];
		TextureUnitState tus0 = new TextureUnitState();
		tus[0] = tus0;
		TextureAttributes textureAttributes = new TextureAttributes();

		textureAttributes.setTextureMode(TextureAttributes.MODULATE);

		tus0.setTextureAttributes(textureAttributes);

		app.setTextureUnitState(tus);

		// note time controllers below need appearance set on the shape now
		shape.setAppearance(app);

		// apply various apperance properties
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
						if (textureAttributesLookup.get(ntp) != null)
						{
							tus0.setTextureAttributes(textureAttributesLookup.get(ntp));
						}
						else
						{
							textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
									: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);
							setUpTimeController(ntp, niToJ3dData, textureSource, target);
							textureAttributesLookup.put(ntp, textureAttributes);
						}

						NiSourceTexture niSourceTexture = null;

						// now set the texture
						if (ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1)
						{
							niSourceTexture = (NiSourceTexture) niToJ3dData.get(ntp.baseTexture.source);
						}
						else
						{
							if (ntp.shaderTextures != null)
							{
								// use it if there's only 1
								if (ntp.shaderTextures.length == 1)
								{
									NifTexDesc textureData = ntp.shaderTextures[0].textureData;
									if (textureData.source.ref != -1)
									{
										niSourceTexture = (NiSourceTexture) niToJ3dData.get(textureData.source);
									}
								}
								else if (ntp.nVer.isBP())
								{// odd black prophecy stuff
									niSourceTexture = lookUpBP(ntp, niToJ3dData);
								}

							}
						}

						if (niSourceTexture != null)
						{
							if (niSourceTexture.useExternal == 0)
							{
								new Throwable("niSourceTexture.useExternal == 0!!").printStackTrace();
							}

							Texture tex = J3dNiGeometry.loadTexture(niSourceTexture.fileName.string, textureSource);
							tus0.setTexture(tex);
						}

					}
					else if (property instanceof BSShaderPPLightingProperty)
					{
						BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty) property;

						BSShaderTextureSet bbsts = (BSShaderTextureSet) niToJ3dData.get(bsspplp.textureSet);

						if (bbsts.numTextures > 0 && textureSource != null)
						{
							Texture tex = J3dNiGeometry.loadTexture(bbsts.textures[0], textureSource);
							tus0.setTexture(tex);
						}

						// refraction lighting is troublesome without shaders, disable for now, or if no texture 
						if (bsspplp.shaderFlags.isBitSet(BSShaderFlags.SF_REFRACTION)
								|| bsspplp.shaderFlags.isBitSet(BSShaderFlags.SF_FIRE_REFRACTION) || bbsts.numTextures == 0)
						{
							ra.setVisible(false);
						}
						setUpTimeController(bsspplp, niToJ3dData, textureSource, target);
					}
					else if (property instanceof BSShaderNoLightingProperty)
					{
						BSShaderNoLightingProperty bssnlp = (BSShaderNoLightingProperty) property;

						textureAttributes.setTextureMode(TextureAttributes.REPLACE);

						if (bssnlp.shaderType.type == BSShaderType.SHADER_NOLIGHTING)
						{
							mat.setLightingEnable(false);
						}

						// refraction lighting is troublesome without shaders, disable for now, or if no texture 
						if (bssnlp.shaderFlags.isBitSet(BSShaderFlags.SF_REFRACTION)
								|| bssnlp.shaderFlags.isBitSet(BSShaderFlags.SF_FIRE_REFRACTION) || bssnlp.fileName.length() == 0)
						{
							ra.setVisible(false);
						}

						// glow map texture
						Texture tex = J3dNiGeometry.loadTexture(bssnlp.fileName, textureSource);
						tus0.setTexture(tex);

					}
					else if (property instanceof TallGrassShaderProperty)
					{
						TallGrassShaderProperty tgsp = (TallGrassShaderProperty) property;
						Texture tex = J3dNiGeometry.loadTexture(tgsp.fileName, textureSource);
						tus0.setTexture(tex);

					}
					else if (property instanceof TileShaderProperty)
					{
						TileShaderProperty tsp = (TileShaderProperty) property;
						Texture tex = J3dNiGeometry.loadTexture(tsp.fileName, textureSource);
						tus0.setTexture(tex);
					}
					else if (property instanceof NiMaterialProperty)
					{
						NiMaterialProperty nmp = (NiMaterialProperty) property;

						if (!(niToJ3dData.nifVer.LOAD_VER == NifVer.VER_20_2_0_7
								&& (niToJ3dData.nifVer.LOAD_USER_VER == 11 || niToJ3dData.nifVer.LOAD_USER_VER == 12)
								&& niToJ3dData.nifVer.LOAD_USER_VER2 > 21))
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
						}
						else
						{
							ta.setTransparencyMode(TransparencyAttributes.SCREEN_DOOR);
						}

						if (nap.alphaTestEnabled())
						{
							// I think the PolygonAttributes.CULL_NONE should be applied to anything
							// with an alphaTestEnabled(), flat_lod trees from skyrim prove it
							// obviously transparent stuff can be seen from the back quite often
							pa.setCullFace(PolygonAttributes.CULL_NONE);
							pa.setBackFaceNormalFlip(true);

							int alphaTestMode = NifOpenGLToJava3D.convertAlphaTestMode(nap.alphaTestMode());
							ra.setAlphaTestFunction(alphaTestMode);

							float threshold = ((nap.threshold) / 255f);// threshold range of 255 to 0 comfirmed
																		// empirically
							ra.setAlphaTestValue(threshold);
						}

					}
					else if (property instanceof NiStencilProperty)
					{
						NiStencilProperty nsp = (NiStencilProperty) property;

						// - this dictates the two sided polygon (e.g. butterfly)
						if (nsp.getDrawMode() == FaceDrawMode.DRAW_BOTH)
						{
							pa.setCullFace(PolygonAttributes.CULL_NONE);
							pa.setBackFaceNormalFlip(true);
						}

						if (nsp.isStencilEnable())
						{
							ra.setStencilEnable(true);
							ra.setStencilWriteMask(nsp.stencilMask);
							ra.setStencilFunction(NifOpenGLToJava3D.convertStencilFunction(nsp.stencilFunction()), nsp.stencilRef,
									nsp.stencilMask);
							ra.setStencilOp(NifOpenGLToJava3D.convertStencilAction(nsp.failAction()), //
									NifOpenGLToJava3D.convertStencilAction(nsp.zFailAction()), //
									NifOpenGLToJava3D.convertStencilAction(nsp.passAction()));
						}
					}
					else if (property instanceof BSEffectShaderProperty)
					{
						BSEffectShaderProperty bsesp = (BSEffectShaderProperty) property;

						// have we already constructed the texture attributes and time controller it?
						if (textureAttributesLookup.get(bsesp) != null)
						{
							tus0.setTextureAttributes(textureAttributesLookup.get(bsesp));
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

							setUpTimeController(bsesp, niToJ3dData, textureSource, target);

							textureAttributesLookup.put(bsesp, textureAttributes);
						}

						// now set the texture
						if (bsesp.SourceTexture.length() > 0 && textureSource != null)
						{
							Texture tex = J3dNiGeometry.loadTexture(bsesp.SourceTexture, textureSource);
							tus0.setTexture(tex);
						}

						mat.setEmissiveColor(bsesp.EmissiveColor.r, bsesp.EmissiveColor.g, bsesp.EmissiveColor.b);

					}
					else if (property instanceof BSSkyShaderProperty)
					{
						BSSkyShaderProperty bsssp = (BSSkyShaderProperty) property;

						Texture tex = J3dNiGeometry.loadTexture(bsssp.SourceTexture, textureSource);
						tus0.setTexture(tex);
					}
					else if (property instanceof NiZBufferProperty)
					{
					}
					else if (property instanceof BSWaterShaderProperty)
					{
					}
					else if (property instanceof WaterShaderProperty)
					{
					}
					else if (property instanceof SkyShaderProperty)
					{
					}
					else if (property instanceof NiSpecularProperty)
					{
					}
					else if (property instanceof NiWireframeProperty)
					{ // uncommon
					}
					else if (property instanceof NiDitherProperty)
					{ // uncommon
					}
					else if (property instanceof NiShadeProperty)
					{// uncommon
					}
					else if (property instanceof NiFogProperty)
					{ // uncommon
					}
					else if (property instanceof NiTextureModeProperty)
					{ // uncommmon, related to PS2?
					}
					else if (property instanceof NiMultiTextureProperty)
					{ // only in 3.1?
					}
					else if (property instanceof NiTextureProperty)
					{ // only in ver3.0 to 3.1?
					}
					else
					{
						System.out.println("J3dNiGeometry - unhandled property " + property);
					}
				}
				else if (prop instanceof BSLightingShaderProperty)
				{
					BSLightingShaderProperty bslsp = (BSLightingShaderProperty) prop;

					// have we already constructed it?
					if (textureAttributesLookup.get(bslsp) != null)
					{
						tus0.setTextureAttributes(textureAttributesLookup.get(bslsp));
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
						setUpTimeController(controller, niToJ3dData, textureSource, target);

						textureAttributesLookup.put(bslsp, textureAttributes);
					}

					// now set the texture
					// FO4 has material files pointed at by name
					if (bslsp.Name.toLowerCase().endsWith(".bgsm") || bslsp.Name.toLowerCase().endsWith(".bgem"))
					{
						// if the bgsm file exists the textureset may have bad .tga files in it (or good .dds ones)
						// but the bgsm definitely has good textures
						try
						{
							BSMaterial material = BgsmSource.getMaterial(bslsp.Name);
							if (material != null)
							{
								Texture tex = J3dNiGeometry.loadTexture(material.textureList.get(0), textureSource);
								tus0.setTexture(tex);
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}

					}
					else if (bslsp.TextureSet.ref != -1)
					{
						BSShaderTextureSet texSet = (BSShaderTextureSet) niToJ3dData.get(bslsp.TextureSet);

						Texture tex = J3dNiGeometry.loadTexture(texSet.textures[0], textureSource);
						tus0.setTexture(tex);
					}

					float specStrength = (bslsp.SpecularStrength / 999f);
					mat.setSpecularColor(bslsp.SpecularColor.r * specStrength, //
							bslsp.SpecularColor.g * specStrength, //
							bslsp.SpecularColor.b * specStrength);

					mat.setShininess((bslsp.Glossiness / 999f) * 128f);

					mat.setEmissiveColor(bslsp.EmissiveColor.r, bslsp.EmissiveColor.g, bslsp.EmissiveColor.b);

					if (bslsp.Alpha != 1.0f)
					{
						ta.setTransparency(1f - bslsp.Alpha);
						ta.setTransparencyMode(TransparencyAttributes.BLENDED);
					}

					// apparently the The vertex colors are used as well, just not the alpha component when
					// SF_Vertex_Animation is present
					// http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
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

		return app;

	}

	/**
	 * NOTE This is not like the J3dNiObjectNET controller set up as the J3dNiGeometry uses it to control only it's
	 * appearance parts So it only handles time controller that will appear in NiAVObject.properties for NiGeometry
	 * types Notice that the controller comes off a NiObjectNET object not a J3dNiObjectNET objects, hence
	 * J3dNiObjectNET.setupController is never called on the object so we don't get double creation I should however at
	 * some point merge this with NiObjectNET set up controllers, the nodeTarget is all that differs now
	 * 
	 * @param parent
	 * @param niObjectNET
	 * @param app
	 * @param blocks
	 */
	public static void setUpTimeController(NiProperty property, NiToJ3dData niToJ3dData, TextureSource textureSource, J3dNiAVObject target)
	{
		NiTimeController controller = (NiTimeController) niToJ3dData.get(property.controller);
		setUpTimeController(controller, niToJ3dData, textureSource, target);
	}
	
	public static void setUpTimeController(BSLightingShaderProperty bslsp, NiToJ3dData niToJ3dData, TextureSource textureSource, J3dNiAVObject target)
	{
		NiTimeController controller = (NiTimeController) niToJ3dData.get(bslsp.controller);
		setUpTimeController(controller, niToJ3dData, textureSource, target);
	}

	private static void setUpTimeController(NiTimeController controller, NiToJ3dData niToJ3dData, TextureSource textureSource,
			J3dNiAVObject target)
	{
		if (controller != null)
		{
			if (controller instanceof NiSingleInterpController)
			{
				NiSingleInterpController niSingleInterpController = (NiSingleInterpController) controller;
				float startTimeS = controller.startTime;
				float stopTimeS = controller.stopTime;

				J3dNiTimeController j3dNiTimeController = J3dNiTimeController.createJ3dNiTimeController(controller, niToJ3dData, target,
						textureSource);

				J3dNiInterpolator j3dNiInterpolator = null;
				if (j3dNiTimeController != null)
				{
					NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(niSingleInterpController.interpolator);
					j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, niInterpolator, niToJ3dData,
							startTimeS, stopTimeS);
				}

				if (j3dNiInterpolator != null)
				{
					target.addChild(j3dNiInterpolator);

					Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
					j3dNiInterpolator.fire(baseAlpha);
				}

			}
			else if (controller instanceof BSRefractionFirePeriodController)
			{

			}

			else
			{
				System.out.println("non NiSingleInterpController for j3dgeometry " + controller + " in " + controller.nVer.fileName);
			}

			NiTimeController nextController = (NiTimeController) niToJ3dData.get(controller.nextController);
			if (nextController != null)
			{
				if (nextController instanceof NiMultiTargetTransformController)
				{
					// this is an object palette ignore
				}
				else
				{
					// TODO: this is used by the particle modifer controller system, see J3dParticleSystem
					// I've also seen texturetransform controllers U then V use this
					setUpTimeController(nextController, niToJ3dData, textureSource, target);
				}

			}
		}
	}

	/**
	 * Black Prophecy has odd textures and crazy file formats	 * 	
	 * Black Prophecy textures order?
	 * engines sometimes only have 1 or 2
	 * occulsion (occ_blank.dds) - optional?
	 * color (diff_blank.dds)
	 * bump
	 * specular
	 * glow (glow_blank.dds)
	 * cubemap, for reflections? - optional
	 * @param ntp
	 * @param niToJ3dData
	 * @return
	 */
	private static NiSourceTexture lookUpBP(NiTexturingProperty ntp, NiToJ3dData niToJ3dData)
	{

		// use 0 unless occlusion, in which case use 1
		NifTexDesc textureData = ntp.shaderTextures[0].textureData;
		if (textureData.source.ref != -1)
		{
			NiSourceTexture niSourceTexture = (NiSourceTexture) niToJ3dData.get(textureData.source);

			if (niSourceTexture.fileName.string.contains("_lod") || niSourceTexture.fileName.string.equals("occ_blank.dds")
					|| niSourceTexture.fileName.string.contains("_occ"))
			{
				textureData = ntp.shaderTextures[1].textureData;
				if (textureData.source.ref != -1)
				{
					return (NiSourceTexture) niToJ3dData.get(textureData.source);
				}
			}
			return niSourceTexture;

		}
		return null;
	}

}
