package nif.niobject;

import java.io.IOException;
import java.io.InputStream;

import nif.ByteConvert;
import nif.NifVer;

/** Optomised version, Always alter jnif version FIRST, then optomize here
 * @author philip
 *
 */
public class NiTriShapeData extends NiTriBasedGeomData
{
	public int numTrianglePoints;

	public boolean hasTriangles;

	//OPTOMISATION
	//public NifTriangle[] triangles;
	public int[] trianglesOpt;

	//OPTOMISATION
	//public int numMatchGroups;
	//public NifMatchGroup[] matchGroups;

	public boolean readFromStream(InputStream stream, NifVer nifVer) throws IOException
	{
		boolean success = super.readFromStream(stream, nifVer);
		numTrianglePoints = ByteConvert.readInt(stream);
		if (nifVer.LOAD_VER >= NifVer.VER_10_1_0_0)
		{
			hasTriangles = ByteConvert.readBool(stream, nifVer);
		}
		else
		{
			hasTriangles = true;
		}
		
		
		// has triangles wasn't used  until a few version after it appeared
		if (nifVer.LOAD_VER <= NifVer.VER_10_0_1_3 ||hasTriangles)
		{
			//OPTOMISATION
			/*triangles = new NifTriangle[numTriangles];
			for (int i = 0; i < numTriangles; i++)
			{
				triangles[i] = new NifTriangle(stream);
			}*/
			trianglesOpt = new int[numTriangles * 3];
			for (int i = 0; i < numTriangles; i++)
			{
				trianglesOpt[i * 3 + 0] = ByteConvert.readShort(stream);
				trianglesOpt[i * 3 + 0] += trianglesOpt[i * 3 + 0] < 0 ? 65536 : 0;
				trianglesOpt[i * 3 + 1] = ByteConvert.readShort(stream);
				trianglesOpt[i * 3 + 1] += trianglesOpt[i * 3 + 1] < 0 ? 65536 : 0;
				trianglesOpt[i * 3 + 2] = ByteConvert.readShort(stream);
				trianglesOpt[i * 3 + 2] += trianglesOpt[i * 3 + 2] < 0 ? 65536 : 0;
			}
		}
		//OPTOMISATION
		/*
		numMatchGroups = ByteConvert.readUnsignedShort(stream);
		matchGroups = new NifMatchGroup[numMatchGroups];
		for (int i = 0; i < numMatchGroups; i++)
		{
			matchGroups[i] = new NifMatchGroup(stream);
		}*/

		int c = ByteConvert.readUnsignedShort(stream);
		for (int i = 0; i < c; i++)
		{
			short s = ByteConvert.readShort(stream);
			ByteConvert.readBytes(s * 2, stream);
		}

		return success;
	}
}