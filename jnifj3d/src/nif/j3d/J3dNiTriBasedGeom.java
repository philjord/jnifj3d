package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.vecmath.Color4f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import tools.WeakValueHashMap;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;

//public abstract class J3dNiTriBasedGeom extends J3dNiGeometryShader
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

	/** Note if compact the return will be a strips array 
	 * 
	 * @param geometryInfo
	 * @param compact and make sharable
	 * @return
	 */
	public static GeometryArray makeGeometry(GeometryInfo geometryInfo, boolean notMorphable, Object cacheKey)
	{
		if (notMorphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(cacheKey);

			if (iga != null)
			{
				return iga;
			}
			else
			{
				geometryInfo.compact();
				IndexedGeometryArray ita = geometryInfo.getIndexedGeometryArray(true, false, true, true, false);
				sharedIGAs.put(cacheKey, ita);
				return ita;
			}
		}
		else
		{
			IndexedGeometryArray ita = geometryInfo.getIndexedGeometryArray(false, true, false, true, false);
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
			return ita;
		}

	}

	public static void loadGIBaseData(GeometryInfo gi, NiTriBasedGeomData data)
	{
		if (data.hasVertices)
		{
			//OPTOMIZATION
			/*
			Point3f[] vertices = new Point3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				vertices[i] = ConvertFromNif.toJ3dP3f(data.vertices[i]);
			}
			gi.setCoordinates(vertices);*/
			gi.setCoordinates(data.verticesOpt);
		}

		if (data.hasNormals)
		{
			Vector3f[] normals = new Vector3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				normals[i] = ConvertFromNif.toJ3dNoScale(data.normals[i]);
			}
			gi.setNormals(normals);
		}

		Color4f[] colors = new Color4f[data.numVertices];
		for (int i = 0; i < data.numVertices; i++)
		{
			if (data.hasVertexColors)
			{
				colors[i] = ConvertFromNif.toJ3d(data.vertexColors[i]);
			}
			else
			{
				colors[i] = new Color4f(1, 1, 1, 1);
			}
		}
		gi.setColors(colors);

		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int actNumUVSets = data.actNumUVSets;
		if (actNumUVSets > 0)
		{
			gi.setTextureCoordinateParams(actNumUVSets, 2);

			for (int i = 0; i < actNumUVSets; i++)
			{
				TexCoord2f[] texCoords = new TexCoord2f[data.uVSets[i].length];
				for (int j = 0; j < data.uVSets[i].length; j++)
				{
					texCoords[j] = ConvertFromNif.toJ3d(data.uVSets[i][j]);
				}
				gi.setTextureCoordinates(i, texCoords);
			}
		}
	}

	protected static int getFormat(NiTriBasedGeomData data, boolean morphable)
	{
		int vertexFormat = (data.hasVertices ? GeometryArray.COORDINATES : 0) //
				| (data.hasNormals ? GeometryArray.NORMALS : 0) //
				| (data.actNumUVSets > 0 ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
				| GeometryArray.COLOR_4 //
				| GeometryArray.BY_REFERENCE_INDICES //
				| GeometryArray.USE_COORD_INDEX_ONLY //
				| GeometryArray.BY_REFERENCE //
				| (!morphable ? GeometryArray.INTERLEAVED : 0);
		return vertexFormat;
	}

	protected static void fillIn(IndexedGeometryArray ita, NiTriBasedGeomData data, boolean morphable)
	{

		float[] normals = null;
		if (data.hasNormals)
		{
			normals = new float[data.numVertices * 3];
			for (int i = 0; i < data.numVertices; i++)
			{
				normals[i * 3 + 0] = data.normals[i].x;
				normals[i * 3 + 1] = data.normals[i].z;
				normals[i * 3 + 2] = -data.normals[i].y;
			}
		}

		float[] colors4 = new float[data.numVertices * 4];
		for (int i = 0; i < data.numVertices; i++)
		{
			if (data.hasVertexColors)
			{
				colors4[i * 4 + 0] = data.vertexColors[i].r;
				colors4[i * 4 + 1] = data.vertexColors[i].g;
				colors4[i * 4 + 2] = data.vertexColors[i].b;
				colors4[i * 4 + 3] = data.vertexColors[i].a;
			}
			else
			{
				colors4[i * 4 + 0] = 1;
				colors4[i * 4 + 1] = 1;
				colors4[i * 4 + 2] = 1;
				colors4[i * 4 + 3] = 1;
			}
		}

		int texCoordDim = 2;
		float[][] texCoordSets = null;
		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int texCoordSetCount = data.actNumUVSets;
		if (texCoordSetCount > 0)
		{
			texCoordSets = new float[texCoordSetCount][data.numVertices * texCoordDim];
			for (int i = 0; i < texCoordSetCount; i++)
			{
				for (int j = 0; j < data.uVSets[i].length; j++)
				{
					texCoordSets[i][j * 2 + 0] = data.uVSets[i][j].u;
					texCoordSets[i][j * 2 + 1] = -data.uVSets[i][j].v;
				}
			}
		}

		if (!morphable)
		{
			float[] vertexData = J3dNiTriBasedGeom.interleave(texCoordSetCount, texCoordDim, texCoordSets, null, colors4, normals,
					data.verticesOpt);
			ita.setInterleavedVertices(vertexData);
		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			float[] coords = new float[data.verticesOpt.length];
			System.arraycopy(data.verticesOpt, 0, coords, 0, data.verticesOpt.length);
			ita.setCoordRefFloat(coords);

			if (data.hasNormals)
				ita.setNormalRefFloat(normals);

			ita.setColorRefFloat(colors4);

			for (int i = 0; i < texCoordSetCount; i++)
			{
				ita.setTexCoordRefFloat(i, texCoordSets[i]);
			}
		}

	}

	/** 
	 * Copied basically from
	 * //GeometryInfo.fillIn(GeometryArray ga, boolean byRef, boolean interleaved, boolean nio)
	 * 
	 */
	private static float[] interleave(int texCoordSetCount, int texCoordDim, float[][] texCoordSets, float[] colors3, float[] colors4,
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
