package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.J3DBuffer;

import nif.compound.BSVertexData;
import nif.niobject.bs.BSTriShape;
import tools3d.utils.Utils3D;
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

		if (bsTriShape.dataSize > 0)
		{
			// All tex units use the 0ith , all others are ignored
			int[] texMap = new int[9];
			for (int i = 0; i < 9; i++)
				texMap[i] = 0;

			int vertexFormat = 0;
			if (BSTriShape.LOAD_OPTIMIZED)
			{
				vertexFormat = GeometryArray.COORDINATES //
						| (bsTriShape.normalsOpt != null ? GeometryArray.NORMALS : 0) //
						| (bsTriShape.uVSetOpt != null ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
						| (bsTriShape.colorsOpt != null ? GeometryArray.COLOR_4 : 0) //
						| GeometryArray.USE_COORD_INDEX_ONLY //
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
						| ((!morphable && BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0) //
						| ((bsTriShape.normalsOpt != null && bsTriShape.tangentsOpt != null && TANGENTS_BITANGENTS)
								? GeometryArray.VERTEX_ATTRIBUTES : 0);
			}
			else
			{
				//TODO: non optomized version of a format
			}

			IndexedGeometryArray iga;
			if (bsTriShape.normalsOpt != null && bsTriShape.tangentsOpt != null && TANGENTS_BITANGENTS)
			{
				iga = new IndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, 2, new int[] { 3, 3 },
						bsTriShape.numTriangles * 3);
			}
			else
			{
				iga = new IndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, bsTriShape.numTriangles * 3);
			}

			if (morphable)
				iga.setCoordIndicesRef(bsTriShape.trianglesOpt);
			else
				iga.setCoordinateIndices(0, bsTriShape.trianglesOpt);

			fillIn(iga, bsTriShape, morphable);

			if (!morphable)
			{
				sharedIGAs.put(bsTriShape, iga);
			}
			return iga;
		}

		return null;
	}

	private static void fillIn(GeometryArray ga, BSTriShape bsTriShape, boolean morphable)
	{
		BSVertexData[] vertexData = bsTriShape.vertexData;

		float[] verticesOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			verticesOpt = bsTriShape.verticesOpt;
		}
		else
		{
			verticesOpt = new float[bsTriShape.numVertices * 3];
			for (int i = 0; i < bsTriShape.numVertices; i++)
			{
				verticesOpt[i * 3 + 0] = vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 2] = -vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 1] = vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
			}
		}

		float[] uVSetOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.uVSetOpt != null)
			{
				uVSetOpt = bsTriShape.uVSetOpt;
			}
		}
		else
		{
			if (vertexData[0].texCoord != null)
			{
				uVSetOpt = new float[bsTriShape.numVertices * 2];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					uVSetOpt[i * 2 + 0] = vertexData[i].texCoord.u;
					uVSetOpt[i * 2 + 1] = vertexData[i].texCoord.v;
				}
			}
		}

		float[] normalsOpt = null;

		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.normalsOpt != null)
			{
				normalsOpt = bsTriShape.normalsOpt;

			}
		}
		else
		{
			if (vertexData[0].normal != null)
			{
				normalsOpt = new float[bsTriShape.numVertices * 3];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					normalsOpt[i * 3 + 0] = vertexData[i].normal.x;
					normalsOpt[i * 3 + 2] = -vertexData[i].normal.y;
					normalsOpt[i * 3 + 1] = vertexData[i].normal.z;
				}
			}
		}

		float[] tangentsOpt = null;
		float[] binormalsOpt = null;
		if (TANGENTS_BITANGENTS)
		{
			if (BSTriShape.LOAD_OPTIMIZED)
			{
				if (bsTriShape.normalsOpt != null)
				{
					tangentsOpt = bsTriShape.tangentsOpt;
					binormalsOpt = bsTriShape.binormalsOpt;
				}
			}
			else
			{
				if (vertexData[0].tangent != null)
				{
					tangentsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						tangentsOpt[i * 3 + 0] = vertexData[i].tangent.x;
						tangentsOpt[i * 3 + 2] = -vertexData[i].tangent.y;
						tangentsOpt[i * 3 + 1] = vertexData[i].tangent.z;
					}
					binormalsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						binormalsOpt[i * 3 + 0] = vertexData[i].bitangentX;
						binormalsOpt[i * 3 + 2] = -vertexData[i].bitangentY;
						binormalsOpt[i * 3 + 1] = vertexData[i].bitangentZ;
					}
				}
			}
		}

		float[] vertexColorsOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.colorsOpt != null)
			{
				vertexColorsOpt = bsTriShape.colorsOpt;
			}
		}
		else
		{
			if (vertexData[0].color != null)
			{
				vertexColorsOpt = new float[bsTriShape.numVertices * 4];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					vertexColorsOpt[i * 4 + 0] = vertexData[i].color.r;
					vertexColorsOpt[i * 4 + 1] = vertexData[i].color.g;
					vertexColorsOpt[i * 4 + 2] = vertexData[i].color.b;
					vertexColorsOpt[i * 4 + 3] = vertexData[i].color.a;
				}
			}
		}

		if (!morphable)
		{
			if (!BUFFERS)
			{
				ga.setCoordinates(0, verticesOpt);

				if (normalsOpt != null)
					ga.setNormals(0, normalsOpt);

				if (vertexColorsOpt != null)
					ga.setColors(0, vertexColorsOpt);

				if (uVSetOpt != null)
				{
					ga.setTextureCoordinates(0, 0, uVSetOpt);
				}

				if (normalsOpt != null && tangentsOpt != null)
				{
					//TODO: here https://www.opengl.org/sdk/docs/tutorials/ClockworkCoders/attributes.php
					// says 6 and 7 are spare, I'm assuming java3d and openlGL sort this out?
					// must test on nvidia hardware
					ga.setVertexAttrs(0, 0, tangentsOpt);
					ga.setVertexAttrs(1, 0, binormalsOpt);
				}
			}
			else
			{
				ga.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(verticesOpt)));

				if (normalsOpt != null)
					ga.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(normalsOpt)));

				if (vertexColorsOpt != null)
					ga.setColorRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(vertexColorsOpt)));

				if (uVSetOpt != null)
				{
					ga.setTexCoordRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(uVSetOpt)));
				}

				if (normalsOpt != null && tangentsOpt != null)
				{
					ga.setVertexAttrRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(tangentsOpt)));
					ga.setVertexAttrRefBuffer(1, new J3DBuffer(Utils3D.makeFloatBuffer(binormalsOpt)));
				}
			}

		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			float[] coords = new float[verticesOpt.length];
			System.arraycopy(verticesOpt, 0, coords, 0, verticesOpt.length);
			ga.setCoordRefFloat(coords);

			if (normalsOpt != null)
				ga.setNormalRefFloat(normalsOpt);

			if (vertexColorsOpt != null)
				ga.setColorRefFloat(vertexColorsOpt);

			if (uVSetOpt != null)
			{
				ga.setTexCoordRefFloat(0, uVSetOpt);
			}

			if (normalsOpt != null && tangentsOpt != null)
			{
				ga.setVertexAttrRefFloat(0, tangentsOpt);
				ga.setVertexAttrRefFloat(1, binormalsOpt);
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

		}
	}

}