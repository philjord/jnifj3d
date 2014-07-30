package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;

import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.bs.BSLODTriShape;
import utils.source.TextureSource;

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
			getShape().setGeometry(createGeometry(data, false));
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

		getShape().setGeometry(createGeometry(data, false));

		if (bsLODTriShape.skin.ref != -1)
		{
			System.err.println("BSLODTriShape has a skin reference!");
		}

	}

	public GeometryArray getCurrentGeometryArray()
	{
		return currentGeometryArray;
	}

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		baseGeometryArray = createGeometry(data, true);
		currentGeometryArray = createGeometry(data, true);
		getShape().setGeometry(currentGeometryArray);
	}

	public static IndexedGeometryArray createGeometry(NiTriShapeData data, boolean morphable)
	{
		if (!morphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(data);

			if (iga != null)
			{
				return iga;
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

			if (!morphable)
			{
				sharedIGAs.put(data, ita);
			}
			return ita;
		}
		//TODO: some trishapes with skin data nearby have no tris (it's in skin data)
		//data.hasTriangles = no in trees in skyrim down the switch paths
		return null;
	}
}