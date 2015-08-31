package nif.niobject;

import java.io.IOException;
import java.io.InputStream;

import nif.ByteConvert;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifVector3;
import nif.enums.ConsistencyType;
import nif.niobject.particle.NiPSysData;
import utils.ESConfig;

/** Optomised version, Always alter jnif version FIRST, then optomize here
 * @author philip
 *
 */
public abstract class NiGeometryData extends NiObject
{
	static
	{
		System.out.println("Optomised NiGeometryData in use");
	}

	public int unknownInt1;

	public int numVertices;

	public int BSMaxVertices;

	public byte keepFlags;

	public byte compressFlags;

	public boolean hasVertices;

	//OPTOMISATION
	//public NifVector3[] vertices;
	public float[] verticesOpt;

	public int numUVSets;

	public boolean hasUV;//not used

	public int actNumUVSets;// calculated for clarity

	public int unknownInt2;

	public boolean hasNormals;

	//OPTOMISATION
	//public NifVector3[] normals;
	public float[] normalsOpt;

	//OPTOMISATION
	//public NifVector3[] binormals;
	//public NifVector3[] tangents;

	public NifVector3 center;

	public float radius;

	public short[] unknown13Shorts;

	public boolean hasVertexColors;

	//OPTOMISATION
	//public NifColor4[] vertexColors;
	public float[] vertexColorsOpt;

	//OPTOMISATION
	//public NifTexCoord[][] uVSets;
	public float[][] uVSetsOpt;

	public ConsistencyType consistencyType;

	public NifRef additionalData;

	public boolean readFromStream(InputStream stream, NifVer nifVer) throws IOException
	{
		boolean success = super.readFromStream(stream, nifVer);
		if (nifVer.LOAD_VER >= NifVer.VER_10_2_0_0)
		{
			unknownInt1 = ByteConvert.readInt(stream);
		}

		if (!(this instanceof NiPSysData))
		{
			numVertices = ByteConvert.readUnsignedShort(stream);
		}
		else
		{
			if (nifVer.LOAD_VER < NifVer.VER_20_2_0_7 || nifVer.LOAD_USER_VER < 11)
			{
				numVertices = ByteConvert.readUnsignedShort(stream);
			}
			else if (nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11 && !nifVer.isBP())
			{
				BSMaxVertices = ByteConvert.readUnsignedShort(stream);
			}
		}
		if (nifVer.LOAD_VER >= NifVer.VER_10_1_0_0)
		{
			keepFlags = ByteConvert.readByte(stream);
			compressFlags = ByteConvert.readByte(stream);
		}

		hasVertices = ByteConvert.readBool(stream, nifVer);
		if (hasVertices)
		{
			//OPTOMIZATION
			/*
			vertices = new NifVector3[numVertices];
			for (int i = 0; i < numVertices; i++)
			{
				vertices[i] = new NifVector3(stream);
			}*/

			verticesOpt = new float[numVertices * 3];
			for (int i = 0; i < numVertices; i++)
			{
				verticesOpt[i * 3 + 0] = ByteConvert.readFloat(stream) * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 2] = -ByteConvert.readFloat(stream) * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 1] = ByteConvert.readFloat(stream) * ESConfig.ES_TO_METERS_SCALE;
			}
		}
		if (nifVer.LOAD_VER >= NifVer.VER_10_0_1_0)
		{
			numUVSets = ByteConvert.readUnsignedShort(stream);
		}

		if (!(this instanceof NiPSysData) && nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER == 12 && !nifVer.isBP())
		{
			unknownInt2 = ByteConvert.readInt(stream);
		}

		hasNormals = ByteConvert.readBool(stream, nifVer);
		if (hasNormals)
		{
			//OPTOMIZATION
			/*
			normals = new NifVector3[numVertices];
			for (int i = 0; i < numVertices; i++)
			{
				normals[i] = new NifVector3(stream);
			}*/

			normalsOpt = new float[numVertices * 3];
			for (int i = 0; i < numVertices; i++)
			{
				normalsOpt[i * 3 + 0] = ByteConvert.readFloat(stream);
				normalsOpt[i * 3 + 2] = -ByteConvert.readFloat(stream);
				normalsOpt[i * 3 + 1] = ByteConvert.readFloat(stream);
			}
			if (nifVer.LOAD_VER >= NifVer.VER_10_1_0_0)
			{
				if ((numUVSets & 61440) != 0)
				{
					//OPTOMISATION
					/*
					binormals = new NifVector3[numVertices];
					for (int i = 0; i < numVertices; i++)
					{
						binormals[i] = new NifVector3(stream);
					}
					tangents = new NifVector3[numVertices];
					for (int i = 0; i < numVertices; i++)
					{
						tangents[i] = new NifVector3(stream);
					}*/
					for (int i = 0; i < numVertices; i++)
					{
						ByteConvert.readBytes(3 * 4, stream);
					}

					for (int i = 0; i < numVertices; i++)
					{
						ByteConvert.readBytes(3 * 4, stream);
					}
				}
			}
		}
		center = new NifVector3(stream);
		radius = ByteConvert.readFloat(stream);

		if (nifVer.LOAD_VER == NifVer.VER_20_3_0_9 && nifVer.LOAD_USER_VER == 131072)
		{
			unknown13Shorts = ByteConvert.readShorts(13, stream);
		}

		hasVertexColors = ByteConvert.readBool(stream, nifVer);
		if (hasVertexColors)
		{
			//OPTOMISATION
			/*
			vertexColors = new NifColor4[numVertices];
			for (int i = 0; i < numVertices; i++)
			{
				vertexColors[i] = new NifColor4(stream);
			}*/

			vertexColorsOpt = new float[numVertices * 4];
			for (int i = 0; i < numVertices; i++)
			{
				vertexColorsOpt[i * 4 + 0] = ByteConvert.readFloat(stream);
				vertexColorsOpt[i * 4 + 1] = ByteConvert.readFloat(stream);
				vertexColorsOpt[i * 4 + 2] = ByteConvert.readFloat(stream);
				vertexColorsOpt[i * 4 + 3] = ByteConvert.readFloat(stream);
			}
		}

		if (nifVer.LOAD_VER <= NifVer.VER_4_2_2_0)
		{
			numUVSets = ByteConvert.readUnsignedShort(stream);
		}
		if (nifVer.LOAD_VER <= NifVer.VER_4_0_0_2)
		{
			hasUV = ByteConvert.readBool(stream, nifVer);
		}

		//calculated actual value based on version
		if (nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11 && !nifVer.isBP())
		{
			actNumUVSets = numUVSets & 1;
		}
		else
		{
			actNumUVSets = numUVSets & 63;
		}
					
		//OPTOMISATION
		/*
		uVSets = new NifTexCoord[actNumUVSets][numVertices];
		for (int j = 0; j < actNumUVSets; j++)
		{
			for (int i = 0; i < numVertices; i++)
			{
				uVSets[j][i] = new NifTexCoord(stream);
			}
		}*/
		uVSetsOpt = new float[actNumUVSets][numVertices * 2];
		for (int j = 0; j < actNumUVSets; j++)
		{
			for (int i = 0; i < numVertices; i++)
			{
				uVSetsOpt[j][i * 2 + 0] = ByteConvert.readFloat(stream);
				uVSetsOpt[j][i * 2 + 1] = ByteConvert.readFloat(stream);
			}
		}

		if ((!(this instanceof NiPSysData) || nifVer.LOAD_USER_VER < 12) && nifVer.LOAD_VER >= NifVer.VER_10_0_1_0)
		{
			consistencyType = new ConsistencyType(stream);
		}

		if ((!(this instanceof NiPSysData) || nifVer.LOAD_USER_VER < 12) && nifVer.LOAD_VER >= NifVer.VER_20_0_0_4)
		{
			additionalData = new NifRef(NiAdditionalGeometryData.class, stream);
		}
		return success;
	}

}