package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;

import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.bs.BSLODTriShape;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;

/**
 * This class has a base geometry and a current to allow skin instances to deform the base
 * @author philip
 *
 */
public class J3dNiTriShape extends J3dNiTriBasedGeom
{
	private GeometryArray currentGeometryArray;

	private NiTriShapeData data;

	public J3dNiTriShape(NiTriShape niTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriShape, niToJ3dData, textureSource);

		niToJ3dData.put(niTriShape, this);
		data = (NiTriShapeData) niToJ3dData.get(niTriShape.data);

		//am I a skin shape in which case I need to be uncompacted ready for animation
		if (niTriShape.skin.ref != -1)
		{
			makeMorphable();
		}
		else
		{
			//GeometryInfo geometryInfo = makeGeometryInfo(data);
			//if (geometryInfo != null)
			//{
			//	getShape().setGeometry(makeGeometry(geometryInfo, true, data));
			//}

			experimentalShape(false);
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
		data = (NiTriShapeData) niToJ3dData.get(bsLODTriShape.data);

		//GeometryInfo geometryInfo = makeGeometryInfo(data);
		//if (geometryInfo != null)
		//{
		//	getShape().setGeometry(makeGeometry(geometryInfo, true, data));
		//}
		experimentalShape(false);

		if (bsLODTriShape.skin.ref != -1)
		{
			System.err.println("BSLODTriShape has a skin reference!");
		}

	}

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		GeometryInfo geometryInfo = makeGeometryInfo(data);
		if (geometryInfo != null)
		{
			baseGeometryArray = makeGeometry(geometryInfo, false, null);

			// odd calls because GeometryInfo doesn't want to produce 2 arrays in some cases (TES5), and clones fails
			GeometryInfo gi2 = new GeometryInfo(baseGeometryArray);
			currentGeometryArray = makeGeometry(gi2, false, null);
			getShape().setGeometry(currentGeometryArray);
		}
	}

	public GeometryArray getCurrentGeometryArray()
	{
		return currentGeometryArray;
	}

	public static GeometryInfo makeGeometryInfo(NiTriShapeData data)
	{
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

		loadGIBaseData(gi, data);

		if (data.hasVertices && data.hasTriangles)
		{
			int[] triangles = new int[data.numTrianglePoints];
			for (int i = 0; i < data.numTriangles; i++)
			{
				triangles[i * 3 + 0] = data.triangles[i].v1;
				triangles[i * 3 + 1] = data.triangles[i].v2;
				triangles[i * 3 + 2] = data.triangles[i].v3;
			}

			gi.setCoordinateIndices(triangles);
			gi.setUseCoordIndexOnly(true);

			return gi;
		}
		else
		{
			//TODO: some trishapes with skin data nearby have no tris (it's in skin data)
			return null;
		}

	}

	private void experimentalShape(boolean morphable)
	{
		if (!morphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(data);

			if (iga != null)
			{
				getShape().setGeometry(iga);
				return;
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

			IndexedGeometryArray ita = new IndexedTriangleArray(data.numVertices, getFormat(data, morphable), data.numTrianglePoints);
			ita.setCoordIndicesRef(triangles);
			fillIn(ita, data, morphable);
			getShape().setGeometry(ita);
			if (!morphable)
			{
				sharedIGAs.put(data, ita);
			}
		}
	}

}