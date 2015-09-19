package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleStripArray;

import nif.niobject.NiTriStrips;
import nif.niobject.NiTriStripsData;
import utils.source.TextureSource;

/**
 * NOTE! Skyrim appears to not use these any more! only trishape
 * @author philip
 *
 */
public class J3dNiTriStrips extends J3dNiTriBasedGeom
{
	public static boolean INTERLEAVE = true;

	private NiTriStripsData data;

	public J3dNiTriStrips(NiTriStrips niTriStrips, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriStrips, niToJ3dData, textureSource);
		niToJ3dData.put(niTriStrips, this);
		data = (NiTriStripsData) niToJ3dData.get(niTriStrips.data);

		getShape().setGeometry(createGeometry(data, false));

	}

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		GeometryArray newGeom = createGeometry(data, true);
		getShape().setGeometry(newGeom);
		baseGeometryArray = newGeom;
	}

	public static IndexedGeometryArray createGeometry(NiTriStripsData data, boolean morphable)
	{
		// if not morphable check cache
		if (!morphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(data);

			if (iga != null)
			{
				return iga;
			}
		}

		if (data.hasVertices && data.hasPoints)
		{
			int numStrips = data.numStrips;
			int[] stripLengths = data.stripLengths;
			int[] points = null;

			// get full length
			int length = 0;
			for (int i = 0; i < numStrips; i++)
			{
				length += data.points[i].length;
			}

			points = new int[length];
			int idx = 0;
			for (int i = 0; i < numStrips; i++)
			{
				for (int j = 0; j < stripLengths[i]; j++)
				{
					points[idx] = data.points[i][j];
					idx++;
				}
			}

			int[] texMap = new int[data.actNumUVSets];
			for (int i = 0; i < data.actNumUVSets; i++)
				texMap[i] = i;

			IndexedGeometryArray itsa = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable, INTERLEAVE),
					data.actNumUVSets, texMap, length, stripLengths);
			if (INTERLEAVE)
				itsa.setCoordIndicesRef(points);
			else
				itsa.setCoordinateIndices(0, points);

			fillIn(itsa, data, morphable, INTERLEAVE);

			// if not morphable cache
			if (!morphable)
			{
				sharedIGAs.put(data, itsa);
			}
			return itsa;
		}

		new Throwable("Null IGA!").printStackTrace();
		return null;
	}
}
