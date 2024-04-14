package utils.source.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TextureUnitState;
import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;

import utils.source.TextureSource;

public class FileTextureSource implements TextureSource
{

	public enum CompressionType
	{
		DDS, ASTC, KTX
	};

	public static CompressionType compressionType = CompressionType.DDS;

	public FileTextureSource()
	{
	}

	@Override
	public boolean textureFileExists(String texName)
	{
		if (texName != null && texName.length() > 0)
		{
			texName = texName.toLowerCase();

			if (texName.length() > 0)
			{
				// remove incorrect file path prefix, if it exists
				if (texName.startsWith("data" + File.separator))
				{
					texName = texName.substring(5);
				}

				// add the textures path part
				if (!texName.startsWith("textures"))
				{
					texName = "textures" + File.separator + texName;
				}

				if (compressionType == CompressionType.ASTC)
				{
					texName = texName.replace(".dds", ".tga.astc");
				}
				else if (compressionType == CompressionType.KTX)
				{
					texName = texName.replace(".dds", ".ktx");
				}

				Texture tex = null;
				//check cache hit
				tex = CompressedTextureLoader.checkCachedTexture(texName);
				if (tex != null)
					return true;

				String[] parts = FileMediaRoots.splitOffMediaRoot(texName);
				return new File(parts[0] + parts[1]).exists();
			}
			return false;
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
			if (texName.startsWith("data" + File.separator))
			{
				texName = texName.substring(5);
			}

			// add the textures path part
			if (!texName.startsWith("textures"))
			{
				texName = "textures" + File.separator + texName;
			}

			if (compressionType == CompressionType.ASTC)
			{
				texName = texName.replace(".dds", ".tga.astc");
			}
			else if (compressionType == CompressionType.KTX)
			{
				texName = texName.replace(".dds", ".ktx");
			}

			Texture tex = null;
			//check cache hit
			tex = CompressedTextureLoader.checkCachedTexture(texName);
			if (tex != null)
				return tex;

			String[] parts = FileMediaRoots.splitOffMediaRoot(texName);

			if (texName.endsWith(".dds"))
			{
				tex = CompressedTextureLoader.DDS.getTexture(new File(parts[0] + parts[1]));
			}
			else if (texName.endsWith(".astc") || texName.endsWith(".atc"))
			{
				tex = CompressedTextureLoader.ASTC.getTexture(new File(parts[0] + parts[1]));
			}
			else if (texName.endsWith(".ktx") )
			{
				tex = CompressedTextureLoader.KTX.getTexture(new File(parts[0] + parts[1]));
			}
			else

			{
				//FIXME: find a generic texture loading system!
				/*	try
					{
						TextureLoader tl = new TextureLoader(parts[0] + parts[1], null);
						tex = tl.getTexture();
					}
					catch (ImageException e)
					{
						System.out.println("FileTextureSource.getTexture  " + texName + " " + e + " " + e.getStackTrace()[0]);
					}*/
			}

			if (tex == null)
			{
				System.out.println(
						"FileTextureSource.getTexture - Problem with loading image: " + texName + "||" + parts[0] + "|" + parts[1]);
			}
			return tex;
		}

		return null;

	}
	@Override
	public TextureUnitState getTextureUnitState(String texName)
	{
		return getTextureUnitState(texName, false);
	}
	@Override
	public TextureUnitState getTextureUnitState(String texName, boolean dropMip0) {
		 
		texName = texName.toLowerCase();

		if (texName.length() > 0)
		{
			// remove incorrect file path prefix, if it exists
			if (texName.startsWith("data" + File.separator))
			{
				texName = texName.substring(5);
			}

			// add the textures path part
			if (!texName.startsWith("textures"))
			{
				texName = "textures" + File.separator + texName;
			}

			if (compressionType == CompressionType.ASTC)
			{
				texName = texName.replace(".dds", ".tga.astc");
			}
			else if (compressionType == CompressionType.KTX)
			{
				texName = texName.replace(".dds", ".ktx");
			}

			TextureUnitState tex = null;
			//check cache hit
			tex = CompressedTextureLoader.checkCachedTextureUnitState(texName);
			if (tex != null)
				return tex;

			String[] parts = FileMediaRoots.splitOffMediaRoot(texName);

			if (texName.endsWith(".dds"))
			{
				tex = CompressedTextureLoader.DDS.getTextureUnitState(new File(parts[0] + parts[1]));
			}
			else if (texName.endsWith(".astc") || texName.endsWith(".atc"))
			{
				tex = CompressedTextureLoader.ASTC.getTextureUnitState(new File(parts[0] + parts[1]));
			}
			else if (texName.endsWith(".ktx") )
			{
				tex = CompressedTextureLoader.KTX.getTextureUnitState(new File(parts[0] + parts[1]));
			}
			else

			{
				//FIXME: find a generic texture loading system!
				/*	try
					{
						TextureLoader tl = new TextureLoader(parts[0] + parts[1], null);
						tex = tl.getTexture();
					}
					catch (ImageException e)
					{
						System.out.println("FileTextureSource.getTexture  " + texName + " " + e + " " + e.getStackTrace()[0]);
					}*/
			}

			if (tex == null)
			{
				System.out.println(
						"FileTextureSource.getTexture - Problem with loading image: " + texName + "||" + parts[0] + "|" + parts[1]);
			}
			return tex;
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
