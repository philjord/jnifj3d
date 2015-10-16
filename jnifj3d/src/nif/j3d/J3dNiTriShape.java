package nif.j3d;

import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;

import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.bs.BSLODTriShape;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Stripifier;


public class J3dNiTriShape extends J3dNiTriBasedGeom
{
	public J3dNiTriShape(NiTriShape niTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriShape, niToJ3dData, textureSource);

		niToJ3dData.put(niTriShape, this);		

		//am I a skin shape in which case I need to be uncompacted ready for animation
		if (niTriShape.skin.ref != -1)
		{
			makeMorphable();
		}
		else
		{
			currentGeometryArray = createGeometry(false);
			getShape().setGeometry(currentGeometryArray);
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

		//	BSLODTriShape level data for lodding not used! But it one tree see ReachTree01
		//	http://afkmods.iguanadons.net/index.php?/topic/4133-skyrim-meshes-containing-bslodtrishape-blocks/
		//	so just turn on at each level if any are there inall 0 is far 1 is close 2 is closer

		niToJ3dData.put(bsLODTriShape, this);

		getShape().setGeometry(createGeometry(false));

		if (bsLODTriShape.skin.ref != -1)
		{
			System.err.println("BSLODTriShape has a skin reference!");
		}

	}

	@Override
	protected IndexedGeometryArray createGeometry(boolean morphable)
	{
		return createGeometry((NiTriShapeData) data, morphable);
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
			int[] texMap = new int[data.actNumUVSets];
			for (int i = 0; i < data.actNumUVSets; i++)
				texMap[i] = i;
			if (!STRIPIFY || morphable)
			{
				IndexedGeometryArray ita = new IndexedTriangleArray(data.numVertices, getFormat(data, morphable, INTERLEAVE),
						data.actNumUVSets, texMap, data.numTrianglePoints);
				if (morphable || INTERLEAVE)
					ita.setCoordIndicesRef(data.trianglesOpt);
				else
					ita.setCoordinateIndices(0, data.trianglesOpt);

				fillIn(ita, data, morphable, INTERLEAVE);

				if (!morphable)
				{
					sharedIGAs.put(data, ita);
				}
				return ita;
			}
			else
			{
				//	DO NOT DELETE this is how you make strip arrays
				// NifToJ3d.extractShapes  setControllers might complain, but it should have set morphable proper by now

				GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

				gi.setCoordinateIndices(data.trianglesOpt);
				gi.setUseCoordIndexOnly(true);
				gi.setCoordinates(data.verticesOpt);
				gi.setColors4(data.vertexColorsOpt);
				gi.setNormals(data.normalsOpt);
				if (data.actNumUVSets > 0)
				{
					gi.setTextureCoordinateParams(data.actNumUVSets, 2);
					gi.setTexCoordSetMap(texMap);
					for (int i = 0; i < data.actNumUVSets; i++)
					{
						gi.setTextureCoordinates(i, data.uVSetsOpt[i]);
					}
				}

				Stripifier stripifer = new Stripifier();
				stripifer.stripify(gi);

				IndexedGeometryArray ita = gi.getIndexedGeometryArray(false, false, INTERLEAVE, true, BUFFERS);

				if (!morphable)
				{
					sharedIGAs.put(data, ita);
				}
				return ita;
			}
		}
		//TODO: some trishapes with skin data nearby have no tris (it's in skin data)
		//data.hasTriangles = no in trees in skyrim down the switch paths
		return null;
	}
}