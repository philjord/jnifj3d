package nif;

import java.io.IOException;
import java.util.HashMap;

import nif.niobject.bgsm.BgsmFile;
import utils.source.MeshSource;

/**
 * Yes it is bullshit sorry... I have to hand these guys deep, deep into the geometry code
 * 
 * @author phil
 *
 */
public class BgsmSource
{

	private static MeshSource MESH_SOURCE = null;
	private static HashMap<String, BgsmFile> bgsmFiles = new HashMap<String, BgsmFile>();

	public static void setBgsmSource(MeshSource meshSource)
	{
		MESH_SOURCE = meshSource;
	}

	public static BgsmFile getBgsmFile(String fileName) throws IOException
	{
		if (MESH_SOURCE != null)
		{
			// e.g. materials\Landscape\Rocks\BlastedForestRocksMoss.BGSM
			// e.g. here is a bad name C:\Projects\Fallout4\Build\PC\Data\materials\Vehicles\Frame01.BGSM
			
			if(fileName.toLowerCase().indexOf("materials") > 0)
			{
				fileName = fileName.toLowerCase().substring(fileName.toLowerCase().indexOf("materials"));
			}
			
			BgsmFile bgsmFile = bgsmFiles.get(fileName);

			if (bgsmFile == null)
			{
				bgsmFile = BgsmFile.readBgsmFile(fileName, MESH_SOURCE.getInputStreamForFile(fileName));
				bgsmFiles.put(fileName, bgsmFile);
			}

			return bgsmFile;
		}
		else
		{
			new Throwable("Mesh Source must be set in BgsmSource using setBgsmSource(MeshSource meshSource) before requesting bgsm files").printStackTrace();
		}
		return null;
	}
}
