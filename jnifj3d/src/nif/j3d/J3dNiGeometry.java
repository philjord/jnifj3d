package nif.j3d;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.TransparencyAttributes;

import nif.appearance.NiGeometryAppearance;
import nif.niobject.NiGeometry;
import tools3d.utils.AppearanceFactory;
import tools3d.utils.scenegraph.Fadable;
import utils.source.TextureSource;

public abstract class J3dNiGeometry extends J3dNiAVObject implements Fadable
{
	private Appearance normalApp;

	private Shape3D shape;

	private TransparencyAttributes normalTA = null;

	private TransparencyAttributes faderTA = null;

	public J3dNiGeometry(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		this(niGeometry, niToJ3dData, textureSource, null);
	}

	/**
	 * note a non null customShape will have it's name set and be refed by the getShape but will not be added as a child
	 * 
	 * @param niGeometry
	 * @param blocks
	 * @param niToJ3dData
	 * @param imageDir
	 * @param customShape
	 */
	public J3dNiGeometry(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D customShape)
	{
		super(niGeometry, niToJ3dData);

		if (customShape == null)
		{
			shape = new Shape3D();
			addChild(shape);
		}
		else
		{
			// Don't add as this is for particles which is different
			shape = customShape;
		}
		shape.setName("" + this.getClass().getSimpleName() + ":" + niGeometry.name);

		// no texture source probably just wants the geometry set up
		if (textureSource != null)
		{

			normalApp = ((NiGeometryAppearance) AppearanceFactory.currentAppearanceFactory).configureAppearance(niGeometry, niToJ3dData,
					textureSource, shape, this);

			/*
			 * System.out.println("niGeometry " +niGeometry.nVer.fileName); System.out.println(
			 * "normalApp.getTransparencyAttributes() " + normalApp.getTransparencyAttributes()); System.out.println(
			 * "normalApp.getRenderingAttributes() " + normalApp.getRenderingAttributes()); System.out.println(
			 * "normalApp.getColoringAttributes() " + normalApp.getColoringAttributes()); System.out.println(
			 * "normalApp.getLineAttributes() " + normalApp.getLineAttributes()); System.out.println(
			 * "normalApp.getTextureUnitCount() " + normalApp.getTextureUnitCount()); System.out.println(
			 * "normalApp.getMaterial() " + normalApp.getMaterial()); System.out.println(
			 * "normalApp.getPolygonAttributes() " + normalApp.getPolygonAttributes());
			 */

			// Some times the nif just has no texture, odd. see BSShaderNoLightingProperty

		}

	}

	public Shape3D getShape()
	{
		return shape;
	}

	public static boolean textureExists(String texName, TextureSource ts)
	{
		if (ts != null && texName != null && texName.length() > 0)
		{
			// morrowind has bmp and tga endings ?
			texName = texName.toLowerCase().trim();
			if (texName.endsWith(".bmp"))
				texName = texName.substring(0, texName.indexOf(".bmp")) + ".dds";
			else if (texName.endsWith(".tga"))
				texName = texName.substring(0, texName.indexOf(".tga")) + ".dds";

			return ts.textureFileExists(texName);
		}

		return false;
	}

	public static Texture loadTexture(String texName, TextureSource ts)
	{
		if (ts != null && texName != null && texName.length() > 0)
		{
			// morrowind has bmp and tga endings ?
			texName = texName.toLowerCase().trim();
			if (texName.endsWith(".bmp"))
				texName = texName.substring(0, texName.indexOf(".bmp")) + ".dds";
			else if (texName.endsWith(".tga"))
				texName = texName.substring(0, texName.indexOf(".tga")) + ".dds";

			return ts.getTexture(texName);
		}

		return null;
	}

	public static TextureUnitState loadTextureUnitState(String texName, TextureSource ts)
	{
		if (ts != null && texName != null && texName.length() > 0)
		{
			// morrowind has bmp and tga endings ?
			texName = texName.toLowerCase().trim();
			if (texName.endsWith(".bmp"))
				texName = texName.substring(0, texName.indexOf(".bmp")) + ".dds";
			else if (texName.endsWith(".tga"))
				texName = texName.substring(0, texName.indexOf(".tga")) + ".dds";

			return ts.getTextureUnitState(texName);
		}

		return null;
	}

	private float currentTrans = -1f;

	/**
	 * the appearance's transparency in the range [0.0, 1.0] with 0.0 being fully opaque and 1.0 being fully transparent
	 * 
	 * @see tools3d.utils.scenegraph.Fadable#fade(float)
	 */
	@Override
	public void fade(float percent)
	{
		// check for setup indicator
		if (percent == -1f)
		{
			// Various parts to allow fading in and out
			normalTA = normalApp.getTransparencyAttributes();
			normalApp.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
			normalApp.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
			faderTA = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0f);
			faderTA.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		}
		else
		{
			// setting transparency is expensive early out it if possible
			if (currentTrans != percent)
			{
				if (percent <= 0 || percent >= 1.0f)
				{
					if (normalApp.getTransparencyAttributes() != normalTA)
					{
						// System.out.println("set normal");
						normalApp.setTransparencyAttributes(normalTA);
					}
				}
				else
				{
					if (normalApp.getTransparencyAttributes() != faderTA)
						normalApp.setTransparencyAttributes(faderTA);

					// System.out.println("fade set to " + percent);
					faderTA.setTransparency(percent);
				}
				currentTrans = percent;
			}
		}
	}

}
