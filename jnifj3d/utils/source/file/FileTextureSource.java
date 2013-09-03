package utils.source.file;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Texture;

import tools.image.SimpleImageLoader;
import tools.texture.DDSToTexture;
import utils.source.TextureSource;

import com.sun.j3d.utils.image.ImageException;
import com.sun.j3d.utils.image.TextureLoader;

public class FileTextureSource implements TextureSource
{
	public FileTextureSource()
	{
	}

	@Override
	public boolean textureFileExists(String texName)
	{
		texName = texName.toLowerCase();

		if (texName.length() > 0)
		{
			// remove incorrect file path prefix, if it exists
			if (texName.startsWith("data\\"))
			{
				texName = texName.substring(5);
			}

			// add the textures path part
			if (!texName.startsWith("textures"))
			{
				texName = "textures\\" + texName;
			}

			Texture tex = null;
			//check cache hit
			tex = DDSToTexture.checkCachedTexture(texName);
			if (tex != null)
				return true;

			String[] parts = FileMediaRoots.splitOffMediaRoot(texName);
			return new File(parts[0] + parts[1]).exists();
		}
		return false;

	}

	@Override
	public Texture getTexture(String texName)
	{

		texName = texName.toLowerCase();

		if (texName.length() > 0)
		{
			// remove incorrect file path prefix, if it exists
			if (texName.startsWith("data\\"))
			{
				texName = texName.substring(5);
			}

			// add the textures path part
			if (!texName.startsWith("textures"))
			{
				texName = "textures\\" + texName;
			}

			Texture tex = null;
			//check cache hit
			tex = DDSToTexture.checkCachedTexture(texName);
			if (tex != null)
				return tex;

			String[] parts = FileMediaRoots.splitOffMediaRoot(texName);
			if (texName.endsWith(".dds"))
			{
				tex = DDSToTexture.getTexture(new File(parts[0] + parts[1]));
			}
			else
			{
				try
				{
					TextureLoader tl = new TextureLoader(parts[0] + parts[1], null);
					tex = tl.getTexture();
				}
				catch (ImageException e)
				{
					System.out.println("FileTextureSource.getTexture  " + texName + " " + e.getMessage());
				}
			}

			if (tex == null)
			{
				System.out.println("FileTextureSource.getTexture - Problem with loading image: " + texName + "||" + parts[0] + "|"
						+ parts[1]);
			}
			return tex;
		}

		return null;

	}

	@Override
	public Image getImage(String imageName)
	{

		imageName = imageName.toLowerCase();

		if (imageName.length() > 0)
		{
			// remove incorrect file path prefix, if it exists
			if (imageName.startsWith("data\\"))
			{
				imageName = imageName.substring(5);
			}

			// add the textures path part
			if (!imageName.startsWith("textures"))
			{
				imageName = "textures\\" + imageName;
			}

			String[] parts = FileMediaRoots.splitOffMediaRoot(imageName);
			Image image = SimpleImageLoader.getImage(parts[0] + parts[1]);

			if (image == null)
			{
				System.out.println("FileTextureSource.getImage - Problem with loading image: " + imageName + "||" + parts[0] + "|"
						+ parts[1]);
			}
			return image;
		}

		return null;

	}

	@Override
	public List<String> getFilesInFolder(String folderName)
	{
		String[] parts = FileMediaRoots.splitOffMediaRoot(folderName);

		ArrayList<String> ret = new ArrayList<String>();
		for (File f : new File(parts[0] + parts[1]).listFiles())
		{
			ret.add(f.getAbsolutePath());
		}

		return ret;
	}
}
