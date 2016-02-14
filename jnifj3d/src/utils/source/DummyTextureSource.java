package utils.source;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;

/**
 * For NIF verification purposes, to ensure fast loading
 * 
 */
public class DummyTextureSource implements TextureSource
{
	private Texture tex;

	/**
	 * @param bsas  
	 */
	public DummyTextureSource()
	{
		tex = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, 1, 1);
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
