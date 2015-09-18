package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;

import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import tools.WeakValueHashMap;
import utils.source.TextureSource;

public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{
	protected GeometryArray baseGeometryArray;

	public J3dNiTriBasedGeom(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriBasedGeom, niToJ3dData, textureSource);
	}

	public abstract void makeMorphable();

	public GeometryArray getBaseGeometryArray()
	{
		return baseGeometryArray;
	}

	//Note self expunging cache
	protected static WeakValueHashMap<Object, IndexedGeometryArray> sharedIGAs = new WeakValueHashMap<Object, IndexedGeometryArray>();

	protected static int getFormat(NiTriBasedGeomData data, boolean morphable, boolean interleave)
	{
		int vertexFormat = (data.hasVertices ? GeometryArray.COORDINATES : 0) //
				| (data.hasNormals ? GeometryArray.NORMALS : 0) //
				| (data.actNumUVSets > 0 ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
				| (data.vertexColorsOpt != null ? GeometryArray.COLOR_4 : 0) //
				| GeometryArray.BY_REFERENCE_INDICES //
				| GeometryArray.USE_COORD_INDEX_ONLY //
				| (morphable || interleave ? GeometryArray.BY_REFERENCE : 0)//
				| (!morphable && interleave ? GeometryArray.INTERLEAVED : 0);
		return vertexFormat;
	}

	protected static void fillIn(GeometryArray ga, NiTriBasedGeomData data, boolean morphable, boolean interleave)
	{
		//Note consistency type in nif file also dictates morphable
		float[] normals = null;
		if (data.hasNormals)
		{
			normals = data.normalsOpt;
		}

		float[] colors4 = null;
		if (data.hasVertexColors)
		{
			colors4 = data.vertexColorsOpt;
		}
		else
		{
			//TODO: do I really need a default white color set on everything?
			/*colors4 = new float[data.numVertices * 4];
			for (int i = 0; i < data.numVertices; i++)
			{
				colors4[i * 4 + 0] = 1;
				colors4[i * 4 + 1] = 1;
				colors4[i * 4 + 2] = 1;
				colors4[i * 4 + 3] = 1;
			}*/
		}

		int texCoordDim = 2;
		float[][] texCoordSets = null;
		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int texCoordSetCount = data.actNumUVSets;

		if (texCoordSetCount > 0)
		{
			texCoordSets = data.uVSetsOpt;
		}

		if (!morphable)
		{
			if (interleave)
			{
				float[] vertexData = J3dNiTriBasedGeom.interleave(texCoordSetCount, texCoordDim, texCoordSets, null, colors4, normals,
						data.verticesOpt);
				ga.setInterleavedVertices(vertexData);
			}
			else
			{
				ga.setCoordinates(0, data.verticesOpt);

				if (data.hasNormals)
					ga.setNormals(0, normals);

				if (data.hasVertexColors)
					ga.setColors(0, colors4);

				for (int i = 0; i < texCoordSetCount; i++)
				{
					ga.setTextureCoordinates(i, 0, texCoordSets[i]);
				}
			}
		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			float[] coords = new float[data.verticesOpt.length];
			System.arraycopy(data.verticesOpt, 0, coords, 0, data.verticesOpt.length);
			ga.setCoordRefFloat(coords);

			if (data.hasNormals)
				ga.setNormalRefFloat(normals);

			ga.setColorRefFloat(colors4);

			for (int i = 0; i < texCoordSetCount; i++)
			{
				ga.setTexCoordRefFloat(i, texCoordSets[i]);
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		}

	}

	/** 
	 * Copied basically from
	 * //GeometryInfo.fillIn(GeometryArray ga, boolean byRef, boolean interleaved, boolean nio)
	 * 
	 */
	public static float[] interleave(int texCoordSetCount, int texCoordDim, float[][] texCoordSets, float[] colors3, float[] colors4,
			float[] normals, float[] coordinates)
	{
		// Calculate number of words per vertex
		int wpv = 3; // Always have coordinate data
		if (normals != null)
			wpv += 3;
		if (colors3 != null)
			wpv += 3;
		else if (colors4 != null)
			wpv += 4;
		wpv += (texCoordSetCount * texCoordDim);

		int coordCount = coordinates.length / 3;
		// Build array of interleaved data
		float[] d = new float[wpv * coordCount];

		// Fill in the array
		int offset = 0;
		for (int i = 0; i < coordCount; i++)
		{
			if (texCoordDim == 2)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 2 + 0];
					d[offset++] = texCoordSets[j][i * 2 + 1];
				}
			}
			else if (texCoordDim == 3)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 3 + 0];
					d[offset++] = texCoordSets[j][i * 3 + 1];
					d[offset++] = texCoordSets[j][i * 3 + 2];
				}
			}
			else if (texCoordDim == 4)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 4 + 0];
					d[offset++] = texCoordSets[j][i * 4 + 1];
					d[offset++] = texCoordSets[j][i * 4 + 2];
					d[offset++] = texCoordSets[j][i * 4 + 3];
				}
			}

			if (colors3 != null)
			{
				d[offset++] = colors3[i * 3 + 0];
				d[offset++] = colors3[i * 3 + 1];
				d[offset++] = colors3[i * 3 + 2];
			}
			else if (colors4 != null)
			{
				d[offset++] = colors4[i * 4 + 0];
				d[offset++] = colors4[i * 4 + 1];
				d[offset++] = colors4[i * 4 + 2];
				d[offset++] = colors4[i * 4 + 3];
			}

			if (normals != null)
			{
				d[offset++] = normals[i * 3 + 0];
				d[offset++] = normals[i * 3 + 1];
				d[offset++] = normals[i * 3 + 2];
			}

			d[offset++] = coordinates[i * 3 + 0];
			d[offset++] = coordinates[i * 3 + 1];
			d[offset++] = coordinates[i * 3 + 2];
		}

		return d;
	}
}
