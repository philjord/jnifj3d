package utils.source;

import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureUnitState;

public interface TextureSource
{
	public boolean textureFileExists(String texName);

	public Texture getTexture(String texName);

	public TextureUnitState getTextureUnitState(String texName);
	
	public TextureUnitState getTextureUnitState(String texName, boolean dropMip0);
}
