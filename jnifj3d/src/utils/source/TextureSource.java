package utils.source;

import java.util.List;

import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureUnitState;

public interface TextureSource
{
	public boolean textureFileExists(String texName);

	public Texture getTexture(String texName);

	public List<String> getFilesInFolder(String folderName);

	public TextureUnitState getTextureUnitState(String texName);
}
