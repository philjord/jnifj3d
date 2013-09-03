package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.bs.BSLODTriShape;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

/**
 * This class has a base geometry and a current to allow skin instances to deform the base
 * @author philip
 *
 */
public class J3dNiTriShape extends J3dNiTriBasedGeom
{
	private IndexedGeometryArray baseIndexedGeometryArray;

	private IndexedGeometryArray currentIndexedGeometryArray;

	private GeometryInfo geometryInfo = null;

	public J3dNiTriShape(NiTriShape niTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriShape, niToJ3dData, textureSource);

		niToJ3dData.put(niTriShape, this);
		NiTriShapeData data = (NiTriShapeData) niToJ3dData.get(niTriShape.data);

		geometryInfo = makeGeometryInfo(data);
		if (geometryInfo != null)
		{
			//am I a skin shape in which case I need to be uncompacted ready for animation
			if (niTriShape.skin.ref != -1)
			{
				baseIndexedGeometryArray = makeGeometry(geometryInfo, false);
				 
				// odd calls because GeometryInfo doesn't want to produce 2 arrays in some cases (TES5), and clones fails
				GeometryInfo gi2 = new GeometryInfo(baseIndexedGeometryArray);
				currentIndexedGeometryArray = makeGeometry(gi2, false);
				getShape().setGeometry(currentIndexedGeometryArray);
			}
			else
			{
				getShape().setGeometry(makeGeometry(geometryInfo, true));
			}
		}

	}

	 
		
	 

	/**
	 * NOTE ignore skin ref (what would that be?)
	 * @param bsLODTriShape
	 * @param niToJ3dData
	 * @param textureSource
	 */
	public J3dNiTriShape(BSLODTriShape bsLODTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(bsLODTriShape, niToJ3dData, textureSource);

		niToJ3dData.put(bsLODTriShape, this);
		NiTriShapeData data = (NiTriShapeData) niToJ3dData.get(bsLODTriShape.data);

		geometryInfo = makeGeometryInfo(data);
		if (geometryInfo != null)
		{
			getShape().setGeometry(makeGeometry(geometryInfo, true));
		}
		if (bsLODTriShape.skin.ref != -1)
		{
			System.err.println("BSLODTriShape has a skin reference!");
		}

	}

	public IndexedGeometryArray getBaseIndexedGeometryArray()
	{
		return baseIndexedGeometryArray;
	}

	public IndexedGeometryArray getCurrentIndexedGeometryArray()
	{
		return currentIndexedGeometryArray;
	}

	public static IndexedGeometryArray makeGeometry(GeometryInfo geometryInfo, boolean compact)
	{
		if (compact)
		{
			geometryInfo.compact();
			Stripifier st = new Stripifier();
			st.stripify(geometryInfo);
		}

		IndexedGeometryArray ita = geometryInfo.getIndexedGeometryArray(compact, !compact, compact, true, false);

		if (!compact)
		{
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		}

		return ita;

	}

	public static GeometryInfo makeGeometryInfo(NiTriShapeData data)
	{
		GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

		if (data.hasVertices)
		{
			Point3f[] vertices = new Point3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				vertices[i] = ConvertFromNif.toJ3dP3f(data.vertices[i]);
			}
			geometryInfo.setCoordinates(vertices);
		}

		if (data.hasNormals)
		{
			Vector3f[] normals = new Vector3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				normals[i] = ConvertFromNif.toJ3dNoScale(data.normals[i]);
			}
			geometryInfo.setNormals(normals);
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
		geometryInfo.setColors(colors);

		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int actNumUVSets = data.actNumUVSets;
		if (actNumUVSets > 0)
		{
			geometryInfo.setTextureCoordinateParams(actNumUVSets, 2);

			for (int i = 0; i < actNumUVSets; i++)
			{
				TexCoord2f[] texCoords = new TexCoord2f[data.uVSets[i].length];
				for (int j = 0; j < data.uVSets[i].length; j++)
				{
					texCoords[j] = ConvertFromNif.toJ3d(data.uVSets[i][j]);
				}
				geometryInfo.setTextureCoordinates(i, texCoords);
			}

		}

		if (data.hasVertices && data.hasTriangles)
		{
			int[] triangles = new int[data.numTrianglePoints];
			for (int i = 0; i < data.numTriangles; i++)
			{
				triangles[i * 3 + 0] = data.triangles[i].v1;
				triangles[i * 3 + 1] = data.triangles[i].v2;
				triangles[i * 3 + 2] = data.triangles[i].v3;
			}

			geometryInfo.setCoordinateIndices(triangles);
			geometryInfo.setUseCoordIndexOnly(true);

			if (!data.hasNormals)
			{
				NormalGenerator normalGenerator = new NormalGenerator();
				normalGenerator.generateNormals(geometryInfo);
			}

			return geometryInfo;
		}
		else
		{
			//TODO: some trishapes with skin data nearby have no tris (it's in skin data)
			return null;
		}

	}

}