package nif.j3d;

import javax.media.j3d.IndexedGeometryArray;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import nif.niobject.bs.BSTriShape;
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

	private static IndexedGeometryArray createGeometry(BSTriShape bsTriShape, boolean morphable)
	{
		// TODO: go back to J3dNiTriShape and set it up the same

		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
		if (bsTriShape.vertexFormatFlags1 != 0)
		{
			/*int[] trianglesOpt = new int[bsTriShape.numTriangles * 3];
			for (int i = 0; i < bsTriShape.numTriangles; i++)
			{
				trianglesOpt[i * 3 + 0] = bsTriShape.triangles[i].v1;
				trianglesOpt[i * 3 + 1] = bsTriShape.triangles[i].v2;
				trianglesOpt[i * 3 + 2] = bsTriShape.triangles[i].v3;
			}*/

			gi.setCoordinateIndices(bsTriShape.trianglesOpt);
			gi.setUseCoordIndexOnly(true);

			/*float[] verticesOpt = new float[bsTriShape.numVertices * 3];
			for (int i = 0; i < bsTriShape.numVertices; i++)
			{
				verticesOpt[i * 3 + 0] = bsTriShape.vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 2] = -bsTriShape.vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 1] = bsTriShape.vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
			}*/
			gi.setCoordinates(bsTriShape.verticesOpt);

			if ((bsTriShape.vertexFormatFlags2 & 0x2) != 0)
			{
				/*float[] vertexColorsOpt = new float[bsTriShape.numVertices * 4];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					vertexColorsOpt[i * 4 + 0] = bsTriShape.vertexData[i].color.r;
					vertexColorsOpt[i * 4 + 1] = bsTriShape.vertexData[i].color.g;
					vertexColorsOpt[i * 4 + 2] = bsTriShape.vertexData[i].color.b;
					vertexColorsOpt[i * 4 + 3] = bsTriShape.vertexData[i].color.a;
				}*/
				// gi.setColors4(vertexColorsOpt);
			}

			if ((bsTriShape.vertexFormatFlags2 & 0x4) != 0)
			{
				/*float[] normalsOpt = new float[bsTriShape.numVertices * 3];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					normalsOpt[i * 3 + 0] = bsTriShape.vertexData[i].normal.x;
					normalsOpt[i * 3 + 2] = -bsTriShape.vertexData[i].normal.y;
					normalsOpt[i * 3 + 1] = bsTriShape.vertexData[i].normal.z;
				}*/
				gi.setNormals(bsTriShape.normalsOpt);
			}

			/*float[] uVSetsOpt = new float[bsTriShape.numVertices * 2];
			for (int i = 0; i < bsTriShape.numVertices; i++)
			{
				uVSetsOpt[i * 2 + 0] = bsTriShape.vertexData[i].texCoord.u;
				uVSetsOpt[i * 2 + 1] = bsTriShape.vertexData[i].texCoord.v;
			}*/

			gi.setTextureCoordinateParams(1, 2);
			int[] texMap = new int[] { 0 };

			gi.setTexCoordSetMap(texMap);
			gi.setTextureCoordinates(0, bsTriShape.uVSetOpt);

			if (!((bsTriShape.vertexFormatFlags2 & 0x4) != 0))
			{
				NormalGenerator normGen = new NormalGenerator();
				normGen.generateNormals(gi);
			}

			if (STRIPIFY)
			{
				Stripifier stripifer = new Stripifier();
				stripifer.stripify(gi);
			}

			IndexedGeometryArray ita = gi.getIndexedGeometryArray(false, false, INTERLEAVE, true, BUFFERS);

			return ita;
		}
		else
		{
			return null;
		}

	}
}