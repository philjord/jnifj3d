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

			IndexedGeometryArray ita = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable), length, stripLengths);
			ita.setCoordIndicesRef(points);
			fillIn(ita, data, morphable);

			// if not morphable cache
			if (!morphable)
			{
				sharedIGAs.put(data, ita);
			}
			return ita;
		}

		new Throwable("Null IGA!").printStackTrace();
		return null;
	}
}
