package nif.niobject.bs;

import java.io.IOException;
import java.io.InputStream;

import nif.ByteConvert;
import nif.NifVer;
import nif.compound.BSVertexDataOther;
import nif.compound.BSVertexDataRigid;
import nif.compound.BSVertexDataSkinned;
import nif.compound.NifTriangle;
import nif.niobject.NiTriBasedGeom;
import tools.MiniFloat;
import utils.ESConfig;

public class BSTriShape extends NiTriBasedGeom
{
	public int vertexFormatFlags1;
	public int vertexFormat2;
	public int vertexFormat3;
	public int vertexFormat4;
	public int vertexFormatFlags5;
	public int vertexFormat6;
	public int vertexFormatFlags7;
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

	public boolean readFromStream2(InputStream stream, NifVer nifVer) throws IOException
	{
		boolean success = super.readFromStream(stream, nifVer);
		//CAREFUL!!! Optimized version!!!!

		vertexFormatFlags1 = ByteConvert.readUnsignedByte(stream);
		vertexFormat2 = ByteConvert.readUnsignedByte(stream);
		vertexFormat3 = ByteConvert.readUnsignedByte(stream);
		vertexFormat4 = ByteConvert.readUnsignedByte(stream);
		vertexFormatFlags5 = ByteConvert.readUnsignedByte(stream);
		vertexFormat6 = ByteConvert.readUnsignedByte(stream);
		vertexFormatFlags7 = ByteConvert.readUnsignedByte(stream);
		vertexFormat8 = ByteConvert.readUnsignedByte(stream);
		numTriangles = ByteConvert.readInt(stream);
		numVertices = ByteConvert.readUnsignedShort(stream);

		dataSize = ByteConvert.readInt(stream);

		if ((vertexFormatFlags7 & 0x8) != 0 || (vertexFormatFlags7 & 0x20) != 0)
		{
			System.out.println("NEW VERTEX FORMAT TO DEAL WITH! " + vertexFormatFlags7);
			//fix unoptimized first!!
		}
		if ((vertexFormatFlags7 & 0x40) != 0)
		{
			System.out.println("(vertexFormatFlags & 0x40) != 0)" + this.name + " " + nifVer.fileName);
			//fix unoptimized first!!
		}

		if (dataSize > 0)
		{
			verticesOpt = new float[numVertices * 3];
			if ((vertexFormatFlags7 & 0x4) != 0)
			{
				normalsOpt = new float[numVertices * 3];
			}
			if ((vertexFormatFlags7 & 0x1) != 0)
			{
				vertexColorsOpt = new float[numVertices * 4];
			}
			if (vertexFormatFlags1 > 2)
			{
				uVSetOpt = new float[numVertices * 2];
			}

			for (int i = 0; i < numVertices; i++)
			{
				verticesOpt[i * 3 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 2] = -MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream)) * ESConfig.ES_TO_METERS_SCALE;

				ByteConvert.readUnsignedShort(stream);

				if (vertexFormatFlags1 > 2)
				{
					uVSetOpt[i * 2 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					uVSetOpt[i * 2 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				}

				if ((vertexFormatFlags7 & 0x1) != 0)
				{
					vertexColorsOpt[i * 4 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					vertexColorsOpt[i * 4 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					vertexColorsOpt[i * 4 + 2] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					vertexColorsOpt[i * 4 + 3] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
				}

				if ((vertexFormatFlags7 & 0x2) != 0)
				{
					ByteConvert.readBytes(4, stream);
				}

				if ((vertexFormatFlags7 & 0x4) != 0)
				{
					normalsOpt[i * 3 + 0] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					normalsOpt[i * 3 + 2] = -MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));
					normalsOpt[i * 3 + 1] = MiniFloat.toFloat(ByteConvert.readUnsignedShort(stream));

					ByteConvert.readBytes(6, stream);
				}

				if ((vertexFormatFlags7 & 0x40) != 0)
				{
					ByteConvert.readBytes(8, stream);
				}
			}

			trianglesOpt = new int[numTriangles * 3];
			for (int i = 0; i < numTriangles; i++)
			{
				trianglesOpt[i * 3 + 0] = ByteConvert.readUnsignedShort(stream);
				trianglesOpt[i * 3 + 1] = ByteConvert.readUnsignedShort(stream);
				trianglesOpt[i * 3 + 2] = ByteConvert.readUnsignedShort(stream);
			}
		}
		return success;
	}

	public BSVertexDataRigid[] vertexDataRigid;
	public BSVertexDataSkinned[] vertexDataSkinned;
	public BSVertexDataOther[] vertexDataOther;
	public NifTriangle[] triangles;

	public boolean readFromStream(InputStream stream, NifVer nifVer) throws IOException
	{
		boolean success = super.readFromStream(stream, nifVer);
		//CAREFUL CAREFUL!!! Optimized version  exists in jnifj3d!!!

		vertexFormatFlags1 = ByteConvert.readUnsignedByte(stream);
		vertexFormat2 = ByteConvert.readUnsignedByte(stream);
		vertexFormat3 = ByteConvert.readUnsignedByte(stream);
		vertexFormat4 = ByteConvert.readUnsignedByte(stream);
		vertexFormatFlags5 = ByteConvert.readUnsignedByte(stream);
		vertexFormat6 = ByteConvert.readUnsignedByte(stream);
		vertexFormatFlags7 = ByteConvert.readUnsignedByte(stream);
		vertexFormat8 = ByteConvert.readUnsignedByte(stream);
		numTriangles = ByteConvert.readInt(stream);
		numVertices = ByteConvert.readUnsignedShort(stream);

		dataSize = ByteConvert.readInt(stream);

		if ((vertexFormatFlags7 & 0x8) != 0 || (vertexFormatFlags7 & 0x20) != 0)
		{
			System.out.println("NEW VERTEX FORMAT TO DEAL WITH! " + vertexFormatFlags7);
		}
		if ((vertexFormatFlags7 & 0x40) != 0)
		{
			System.out.println("(vertexFormatFlags & 0x40) != 0) " + this.name + " " + nifVer.fileName);
		}

		if (dataSize > 0)
		{
			// good for testing formats
			// f:\game media\fallout4\meshes\landscape\animated\primegroundattack01\primegroundattack01.nif
			if ((vertexFormatFlags7 & 0x1) != 0)
			{
				if ((vertexFormatFlags7 & 0x4) == 0)
				{
					vertexDataRigid = new BSVertexDataRigid[numVertices];
					for (int v = 0; v < numVertices; v++)
					{
						vertexDataRigid[v] = new BSVertexDataRigid(vertexFormatFlags7, stream);
						//System.out.println("" + v + " " + vertexData[v]);
					}
				}
				else if (vertexFormatFlags5 == 0)
				{
					vertexDataSkinned = new BSVertexDataSkinned[numVertices];
					for (int v = 0; v < numVertices; v++)
					{
						vertexDataSkinned[v] = new BSVertexDataSkinned(vertexFormatFlags7, stream);
						//System.out.println("" + v + " " + vertexData[v]);
					}
				}
			}

			if ((vertexFormatFlags7 & 0x1) == 0 || vertexFormatFlags5 > 0)
			{
				vertexDataOther = new BSVertexDataOther[numVertices];
				for (int v = 0; v < numVertices; v++)
				{
					vertexDataOther[v] = new BSVertexDataOther(vertexFormatFlags1, stream);
					//System.out.println("" + v + " " + vertexData[v]);
				}
			}

			triangles = new NifTriangle[numTriangles];
			for (int t = 0; t < numTriangles; t++)
			{
				triangles[t] = new NifTriangle(stream);
				//System.out.println("" + t + " " + triangles[t]);
			}
		}
		return success;
	}

}
