package nif;

import java.io.IOException;
import java.util.HashMap;

import nif.niobject.bgsm.BgsmFile;
import nif.niobject.bgsm.BSMaterial;
import utils.source.MeshSource;
import utils.source.file.FileMeshSource;

/**
 * 
 * 
 * @author phil
 *
 */
public class BgsmSource
{
	public static BgsmSource bgsmSource = null;

	protected static HashMap<String, BSMaterial> materialFiles = new HashMap<String, BSMaterial>();
	private static FileMeshSource fileMeshSource = new FileMeshSource();
	
	//Yes it is bullshit sorry... I have to hand these guys deep, deep into the geometry code
	public static void setBgsmSource(BgsmSource staticbgsmSource)
	{
		bgsmSource = staticbgsmSource;
	}

	public BSMaterial getMaterial(String fileName) throws IOException
	{
		// e.g. materials\Landscape\Rocks\BlastedForestRocksMoss.BGSM
		// e.g. here is a bad name C:\Projects\Fallout4\Build\PC\Data\materials\Vehicles\Frame01.BGSM

		if (fileName.toLowerCase().indexOf("materials") > 0)
		{
			fileName = fileName.toLowerCase().substring(fileName.toLowerCase().indexOf("materials"));
		}

		BSMaterial material = materialFiles.get(fileName);

		if (material == null)
		{
			material = BgsmFile.readMaterialFile(fileName, fileMeshSource.getByteBuffer(fileName));
			materialFiles.put(fileName, material);
		}

		return material;
	}
}
