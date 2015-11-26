package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Stripifier;

import nif.compound.BSVertexDataOther;
import nif.compound.BSVertexDataRigid;
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
		// TODO: go back to J3dNiTriShape and set it up the same cache and optimized

		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

		//TODO: remove check once not broken
		if ((bsTriShape.vertexFormatFlags7 & 0x40) != 0)
			return null;

		if (bsTriShape.dataSize > 0)
		{
			int[] trianglesOpt = new int[bsTriShape.numTriangles * 3];
			for (int i = 0; i < bsTriShape.numTriangles; i++)
			{
				trianglesOpt[i * 3 + 0] = bsTriShape.triangles[i].v1;
				trianglesOpt[i * 3 + 1] = bsTriShape.triangles[i].v2;
				trianglesOpt[i * 3 + 2] = bsTriShape.triangles[i].v3;
			}
			gi.setCoordinateIndices(trianglesOpt);
			//gi.setCoordinateIndices(bsTriShape.trianglesOpt);

			gi.setUseCoordIndexOnly(true);

			if ((bsTriShape.vertexFormatFlags7 & 0x1) != 0)
			{
				BSVertexDataRigid[] vertexData = null;

				if ((bsTriShape.vertexFormatFlags7 & 0x4) == 0)
				{
					vertexData = bsTriShape.vertexDataRigid;
				}
				else if (bsTriShape.vertexFormatFlags5 == 0)
				{
					vertexData = bsTriShape.vertexDataSkinned;
				}

				if (vertexData != null)
				{
					float[] verticesOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						verticesOpt[i * 3 + 0] = vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
						verticesOpt[i * 3 + 2] = -vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
						verticesOpt[i * 3 + 1] = vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
					}
					gi.setCoordinates(verticesOpt);
					//gi.setCoordinates(bsTriShape.verticesOpt);

					float[] normalsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						normalsOpt[i * 3 + 0] = vertexData[i].normal.x;
						normalsOpt[i * 3 + 2] = -vertexData[i].normal.y;
						normalsOpt[i * 3 + 1] = vertexData[i].normal.z;
					}
					gi.setNormals(normalsOpt);
					//gi.setNormals(bsTriShape.normalsOpt);

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
					//gi.setTextureCoordinates(0, bsTriShape.uVSetOpt);

					if ((bsTriShape.vertexFormatFlags7 & 0x2) != 0)
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
						//gi.setColors4(0, bsTriShape.colorOpt);
					}

				}
				else
				{
					System.out.println("vertexDataRigid == null " + bsTriShape + " " + bsTriShape.nVer.fileName);
					return null;
				}
			}
			else
			{
				BSVertexDataOther[] vertexData = bsTriShape.vertexDataOther;
				int vertexFormatFlags = bsTriShape.vertexFormatFlags1;

				float[] verticesOpt = new float[bsTriShape.numVertices * 3];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					verticesOpt[i * 3 + 0] = vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
					verticesOpt[i * 3 + 2] = -vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
					verticesOpt[i * 3 + 1] = vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
				}
				gi.setCoordinates(verticesOpt);
				//gi.setCoordinates(bsTriShape.verticesOpt);

				if (vertexFormatFlags == 3)
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
					//gi.setTextureCoordinates(0, bsTriShape.uVSetOpt);
				}

				if (vertexFormatFlags > 3 && vertexFormatFlags != 7)
				{
					float[] normalsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						normalsOpt[i * 3 + 0] = vertexData[i].normal.x;
						normalsOpt[i * 3 + 2] = -vertexData[i].normal.y;
						normalsOpt[i * 3 + 1] = vertexData[i].normal.z;
					}
					gi.setNormals(normalsOpt);
					//gi.setNormals(bsTriShape.normalsOpt);
				}

				if (vertexFormatFlags == 6 || vertexFormatFlags == 7 || vertexFormatFlags == 9 || vertexFormatFlags == 10)
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
					//gi.setColors4(0, bsTriShape.colorOpt);
				}

			}
			if (STRIPIFY && !morphable)
			{
				Stripifier stripifer = new Stripifier();
				stripifer.stripify(gi);
			}

			IndexedGeometryArray ita = gi.getIndexedGeometryArray(false, true, INTERLEAVE && !morphable, true, BUFFERS && !morphable);

			if (morphable)
			{
				ita.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
				ita.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
			}
			return ita;
		}

		return null;

	}
}