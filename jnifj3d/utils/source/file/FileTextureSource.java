package utils.source.file;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Texture;

import tools.ddstexture.DDSImage;
import tools.ddstexture.DDSTextureLoader;
import tools.ddstexture.utils.DDSDecompressor;
import tools.image.ImageFlip;
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
			tex = DDSTextureLoader.checkCachedTexture(texName);
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
			tex = DDSTextureLoader.checkCachedTexture(texName);
			if (tex != null)
				return tex;

			String[] parts = FileMediaRoots.splitOffMediaRoot(texName);
			if (texName.endsWith(".dds"))
			{
				tex = DDSTextureLoader.getTexture(new File(parts[0] + parts[1]));
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
					System.out.println("FileTextureSource.getTexture  " + texName + " " + e + " " + e.getStackTrace()[0]);
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

			DDSImage ddsImage = null;
			try
			{

				ddsImage = DDSImage.read(new File(parts[0] + parts[1]));
				BufferedImage image = new DDSDecompressor(ddsImage, 0, imageName).convertImage();

				if (image != null)
				{
					return ImageFlip.verticalflip(image);
				}
				else
				{
					System.out.println("FileTextureSource.getImage - Problem with loading image: " + imageName + "||" + parts[0] + "|"
							+ parts[1]);
				}

			}
			catch (IOException e)
			{
				System.out.println("FileTextureSource  " + imageName + " " + e + " " + e.getStackTrace()[0]);
			}
			if (ddsImage != null)
				ddsImage.close();

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
