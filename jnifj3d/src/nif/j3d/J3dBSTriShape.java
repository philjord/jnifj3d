package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Stripifier;

import nif.compound.BSVertexData;
import nif.niobject.bs.BSTriShape;
import utils.ESConfig;
import utils.source.TextureSource;

public class J3dBSTriShape extends J3dNiTriBasedGeom
{
	/**
	 * Note BSLODMeshTriShape also arrives here
	 * 
	 * @param bsTriShape
	 * @param niToJ3dData
	 * @param textureSource
	 */
	public J3dBSTriShape(BSTriShape bsTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(bsTriShape, niToJ3dData, textureSource);

		niToJ3dData.put(bsTriShape, this);

		currentGeometryArray = createGeometry(false);
		getShape().setGeometry(currentGeometryArray);

	}

	@Override
	protected IndexedGeometryArray createGeometry(boolean morphable)
	{
		return createGeometry((BSTriShape) this.niAVObject, morphable);
	}

	public static IndexedGeometryArray createGeometry(BSTriShape bsTriShape, boolean morphable)
	{

		//TODO: very much stop using the GI system

		if (!morphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(bsTriShape);

			if (iga != null)
			{
				return iga;
			}
		}

		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
		if (TANGENTS_BITANGENTS)
		{
			gi.setVertexAttributes(2, new int[] { 3, 3 });
		}

		if (bsTriShape.dataSize > 0)
		{

			if (BSTriShape.LOAD_OPTIMIZED)
			{
				gi.setCoordinateIndices(bsTriShape.trianglesOpt);
			}
			else
			{
				int[] trianglesOpt = new int[bsTriShape.numTriangles * 3];
				for (int i = 0; i < bsTriShape.numTriangles; i++)
				{
					trianglesOpt[i * 3 + 0] = bsTriShape.triangles[i].v1;
					trianglesOpt[i * 3 + 1] = bsTriShape.triangles[i].v2;
					trianglesOpt[i * 3 + 2] = bsTriShape.triangles[i].v3;
				}
				gi.setCoordinateIndices(trianglesOpt);
			}

			gi.setUseCoordIndexOnly(true);

			BSVertexData[] vertexData = bsTriShape.vertexData;

			if (BSTriShape.LOAD_OPTIMIZED)
			{
				gi.setCoordinates(bsTriShape.verticesOpt);
			}
			else
			{
				float[] verticesOpt = new float[bsTriShape.numVertices * 3];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					verticesOpt[i * 3 + 0] = vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
					verticesOpt[i * 3 + 2] = -vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
					verticesOpt[i * 3 + 1] = vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
				}
				gi.setCoordinates(verticesOpt);
			}

			if (BSTriShape.LOAD_OPTIMIZED)
			{
				if (bsTriShape.uVSetOpt != null)
				{
					gi.setTextureCoordinateParams(1, 2);
					int[] texMap = new int[] { 0 };
					gi.setTexCoordSetMap(texMap);

					gi.setTextureCoordinates(0, bsTriShape.uVSetOpt);
				}
			}
			else
			{
				if (vertexData[0].texCoord != null)
				{
					gi.setTextureCoordinateParams(1, 2);
					int[] texMap = new int[] { 0 };
					gi.setTexCoordSetMap(texMap);

					float[] uVSetOpt = new float[bsTriShape.numVertices * 2];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						uVSetOpt[i * 2 + 0] = vertexData[i].texCoord.u;
						uVSetOpt[i * 2 + 1] = vertexData[i].texCoord.v;
					}

					gi.setTextureCoordinates(0, uVSetOpt);
				}
			}

			if (BSTriShape.LOAD_OPTIMIZED)
			{
				if (bsTriShape.normalsOpt != null)
				{
					gi.setNormals(bsTriShape.normalsOpt);
				}
			}
			else
			{
				if (vertexData[0].normal != null)
				{
					float[] normalsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						normalsOpt[i * 3 + 0] = vertexData[i].normal.x;
						normalsOpt[i * 3 + 2] = -vertexData[i].normal.y;
						normalsOpt[i * 3 + 1] = vertexData[i].normal.z;
					}
					gi.setNormals(normalsOpt);
				}
			}

			if (BSTriShape.LOAD_OPTIMIZED)
			{
				if (bsTriShape.colorsOpt != null)
				{
					gi.setColors4(bsTriShape.colorsOpt);
				}
			}
			else
			{
				if (vertexData[0].color != null)
				{
					float[] vertexColorsOpt = new float[bsTriShape.numVertices * 4];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						vertexColorsOpt[i * 4 + 0] = vertexData[i].color.r;
						vertexColorsOpt[i * 4 + 1] = vertexData[i].color.g;
						vertexColorsOpt[i * 4 + 2] = vertexData[i].color.b;
						vertexColorsOpt[i * 4 + 3] = vertexData[i].color.a;
					}
					gi.setColors4(vertexColorsOpt);
				}
			}

			if (STRIPIFY && !morphable)
			{
				Stripifier stripifer = new Stripifier();
				stripifer.stripify(gi);
			}

			IndexedGeometryArray ita = gi.getIndexedGeometryArray(false, false, INTERLEAVE && !morphable, true, BUFFERS && !morphable);

			if (morphable)
			{
				ita.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
				ita.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
			}

			if (TANGENTS_BITANGENTS)
			{
				if (BSTriShape.LOAD_OPTIMIZED)
				{
					if (bsTriShape.tangentsOpt != null)
					{
						ita.setVertexAttrs(0, 0, bsTriShape.tangentsOpt);
						ita.setVertexAttrs(1, 0, bsTriShape.binormalsOpt);
					}
				}
				else
				{
					if (vertexData[0].tangent != null)
					{
						float[] tangentsOpt = new float[bsTriShape.numVertices * 3];
						for (int i = 0; i < bsTriShape.numVertices; i++)
						{
							tangentsOpt[i * 3 + 0] = vertexData[i].tangent.x;
							tangentsOpt[i * 3 + 2] = -vertexData[i].tangent.y;
							tangentsOpt[i * 3 + 1] = vertexData[i].tangent.z;
						}
						ita.setVertexAttrs(0, 0, tangentsOpt);

						float[] bitangentsOpt = new float[bsTriShape.numVertices * 3];
						for (int i = 0; i < bsTriShape.numVertices; i++)
						{
							bitangentsOpt[i * 3 + 0] = vertexData[i].bitangentX;
							bitangentsOpt[i * 3 + 2] = -vertexData[i].bitangentY;
							bitangentsOpt[i * 3 + 1] = vertexData[i].bitangentZ;
						}
						ita.setVertexAttrs(1, 0, bitangentsOpt);
					}
				}
			}
			if (!morphable)
			{
				sharedIGAs.put(bsTriShape, ita);
			}
			return ita;
		}

		return null;

	}
}