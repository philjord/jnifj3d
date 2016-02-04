package utils.source;

import java.util.List;

import javax.media.j3d.Texture;

public interface TextureSource
{
	public boolean textureFileExists(String texName);

	public Texture getTexture(String texName);

	public List<String> getFilesInFolder(String folderName);
}
