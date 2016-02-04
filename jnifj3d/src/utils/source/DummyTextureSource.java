package utils.source;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;

import javaawt.image.BufferedImage;

/**
 * For NIF verification purposes, to ensure fast loading
 * 
 */
public class DummyTextureSource implements TextureSource
{
	private BufferedImage im;

	private Texture tex;

	/**
	 * @param bsas  
	 */
	public DummyTextureSource()
	{
		im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		tex = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, im.getWidth(), im.getHeight());
		tex.setImage(0, new ImageComponent2D(ImageComponent.FORMAT_RGB, im));
	}

	@Override
	public boolean textureFileExists(String texName)
	{
		return true;
	}

	@Override
	public Texture getTexture(String texName)
	{
		return tex;
	}

	@Override
	public List<String> getFilesInFolder(String folderName)
	{
		return new ArrayList<String>();
	}

}
