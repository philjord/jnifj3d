package nif.niobject;

import java.io.IOException;
import java.io.InputStream;

import utils.ESConfig;
import nif.ByteConvert;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifColor4;
import nif.compound.NifTexCoord;
import nif.compound.NifVector3;
import nif.enums.ConsistencyType;
import nif.niobject.particle.NiPSysData;

/** Optomised version of NiGeometryData in jnif
 * Always alter jnif version FIRST, then optomize here
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

	public int actNumUVSets;// calculated for clarity

	public int unknownInt2;

	public boolean hasNormals;

	public NifVector3[] normals;

	//OPTOMISATION
	//public NifVector3[] binormals;
	//public NifVector3[] tangents;

	public NifVector3 center;

	public float radius;

	public short[] unknown13Shorts;

	public boolean hasVertexColors;

	public NifColor4[] vertexColors;

	public NifTexCoord[][] uVSets;

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
			else if (nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11)
			{
				BSMaxVertices = ByteConvert.readUnsignedShort(stream);
			}
		}

		keepFlags = ByteConvert.readByte(stream);
		compressFlags = ByteConvert.readByte(stream);

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

		numUVSets = ByteConvert.readUnsignedShort(stream);

		if (!(this instanceof NiPSysData) && nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER == 12)
		{
			unknownInt2 = ByteConvert.readInt(stream);
		}

		hasNormals = ByteConvert.readBool(stream, nifVer);
		if (hasNormals)
		{
			normals = new NifVector3[numVertices];
			for (int i = 0; i < numVertices; i++)
			{
				normals[i] = new NifVector3(stream);
			}
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
				stream.skip(numVertices * 3 * 4);
				stream.skip(numVertices * 3 * 4);
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
			vertexColors = new NifColor4[numVertices];
			for (int i = 0; i < numVertices; i++)
			{
				vertexColors[i] = new NifColor4(stream);
			}
		}

		//claculated actual value based on version
		if (nifVer.LOAD_VER >= NifVer.VER_20_2_0_7 && nifVer.LOAD_USER_VER >= 11 && nifVer.LOAD_VER != NifVer.VER_20_3_0_9)
		{
			actNumUVSets = numUVSets & 1;
		}
		else
		{
			actNumUVSets = numUVSets & 63;
		}
		uVSets = new NifTexCoord[actNumUVSets][numVertices];
		for (int j = 0; j < actNumUVSets; j++)
		{
			for (int i = 0; i < numVertices; i++)
			{
				uVSets[j][i] = new NifTexCoord(stream);
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