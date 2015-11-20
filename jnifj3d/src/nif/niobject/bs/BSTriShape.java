package nif.niobject.bs;

import java.io.IOException;
import java.io.InputStream;

import nif.ByteConvert;
import nif.NifVer;
import nif.niobject.NiTriBasedGeom;
import tools.MiniFloat;
import utils.ESConfig;

public class BSTriShape extends NiTriBasedGeom
{
	public int vertexFormat1;
	public int vertexFormat2;
	public int vertexFormat3;
	public int vertexFormat4;
	public int vertexFormat5;
	public int vertexFormat6;
	public int vertexFormatFlags;
	public int vertexFormat8;
	public int numTriangles;
	public int numVertices;
	public int dataSize;

	//OPTOMISATION
	//public BSVertexData[] vertexData;
	public float[] verticesOpt;
	public float[] normalsOpt;
	public float[] vertexColorsOpt;
	public float[] uVSetOpt;

	//OPTOMISATION
	//public NifTriangle[] triangles;
	public int[] trianglesOpt;

	public boolean readFromStream(InputStream stream, NifVer nifVer) throws IOException
	{
		boolean success = super.readFromStream(stream, nifVer);
		//CAREFUL!!! Optimized version!!!!

		vertexFormat1 = ByteConvert.readUnsignedByte(stream);
		vertexFormat2 = ByteConvert.readUnsignedByte(stream);
		vertexFormat3 = ByteConvert.readUnsignedByte(stream);
		vertexFormat4 = ByteConvert.readUnsignedByte(stream);
		vertexFormat5 = ByteConvert.readUnsignedByte(stream);
		vertexFormat6 = ByteConvert.readUnsignedByte(stream);
		vertexFormatFlags = ByteConvert.readUnsignedByte(stream);
		vertexFormat8 = ByteConvert.readUnsignedByte(stream);
		numTriangles = ByteConvert.readInt(stream);
		numVertices = ByteConvert.readUnsignedShort(stream);

		dataSize = ByteConvert.readInt(stream);

		verticesOpt = new float[numVertices * 3];
		if ((vertexFormatFlags & 0x4) != 0)
		{
			normalsOpt = new float[numVertices * 3];
		}
		if ((vertexFormatFlags & 0x1) != 0)
		{
			vertexColorsOpt = new float[numVertices * 4];
		}
		if (vertexFormat1 > 2)
		{
			uVSetOpt = new float[numVertices * 2];
		}

		for (int i = 0; i < numVertices; i++)
		{			
			verticesOpt[i * 3 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;
			verticesOpt[i * 3 + 2] = -MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;
			verticesOpt[i * 3 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;
			
			ByteConvert.readUnsignedShort(stream);

			if (vertexFormat1 > 2)
			{
				uVSetOpt[i * 2 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				uVSetOpt[i * 2 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));				
			}

			if ((vertexFormatFlags & 0x1) != 0)
			{
				vertexColorsOpt[i * 4 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				vertexColorsOpt[i * 4 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				vertexColorsOpt[i * 4 + 2] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				vertexColorsOpt[i * 4 + 3] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
			}

			if ((vertexFormatFlags & 0x2) != 0)
			{
				ByteConvert.readBytes(4, stream);
			}

			if ((vertexFormatFlags & 0x4) != 0)
			{
				normalsOpt[i * 3 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				normalsOpt[i * 3 + 2] = -MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				normalsOpt[i * 3 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				
				
				ByteConvert.readBytes(6, stream);
			}
		}

		trianglesOpt = new int[numTriangles * 3];
		for (int i = 0; i < numTriangles; i++)
		{
			trianglesOpt[i * 3 + 0] = ByteConvert.readUnsignedShort(stream);
			trianglesOpt[i * 3 + 1] = ByteConvert.readUnsignedShort(stream);
			trianglesOpt[i * 3 + 2] = ByteConvert.readUnsignedShort(stream);
		}

		return success;
	}

}
