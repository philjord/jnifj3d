package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleStripArray;

import nif.niobject.NiTriStrips;
import nif.niobject.NiTriStripsData;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;

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

		//getShape().setGeometry(makeGeometry(makeGeometryInfo(data), true, data));
		experimentalShape(false);

	}

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		GeometryArray newGeom = makeGeometry(makeGeometryInfo(data), false, null);
		getShape().setGeometry(newGeom);
		baseGeometryArray = newGeom;
	}

	private static GeometryInfo makeGeometryInfo(NiTriStripsData data)
	{
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

		loadGIBaseData(gi, data);

		int numStrips = data.numStrips;
		int[] stripLengths = data.stripLengths;
		int[] points = null;
		if (data.hasPoints)
		{
			// get full length
			int length = 0;
			for (int i = 0; i < numStrips; i++)
			{
				length += data.points[i].length;
			}

			gi.setStripCounts(stripLengths);
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

			gi.setCoordinateIndices(points);
			gi.setUseCoordIndexOnly(true);
		}

		return gi;

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
			getShape().setGeometry(ita);

			if (!morphable)
			{
				sharedIGAs.put(data, ita);
			}
		}
	}

}
