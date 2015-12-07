package nif.j3d;

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
	public J3dNiTriStrips(NiTriStrips niTriStrips, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriStrips, niToJ3dData, textureSource);
		niToJ3dData.put(niTriStrips, this);

		currentGeometryArray = createGeometry(false);
		getShape().setGeometry(currentGeometryArray);
	}

	@Override
	protected IndexedGeometryArray createGeometry(boolean morphable)
	{
		return createGeometry((NiTriStripsData) data, morphable);
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

			int texCoordCount = 1;

			// All tex units use the 0ith one  
			int[] texMap = new int[9];
			for (int i = 0; i < 9; i++)
				texMap[i] = 0;
			 
			IndexedGeometryArray itsa;
			if (data.hasNormals && (data.numUVSets & 61440) != 0 && TANGENTS_BITANGENTS)
			{
				itsa = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable, INTERLEAVE), texCoordCount, texMap, 2,
						new int[] { 3, 3 }, length, stripLengths);
			}
			else
			{
				itsa = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable, INTERLEAVE),
						texCoordCount, texMap, length, stripLengths);
			}
			
			if (morphable || INTERLEAVE)
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
