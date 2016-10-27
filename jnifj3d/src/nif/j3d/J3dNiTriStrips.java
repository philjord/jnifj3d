package nif.j3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.vecmath.Point3d;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.IndexedGeometryArray;
import org.jogamp.java3d.IndexedTriangleStripArray;
import org.jogamp.java3d.JoglesIndexedTriangleStripArray;

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
		currentGeometryArray.setName(niTriStrips.toString() + " : " + data.nVer.fileName);
		getShape().setGeometry(currentGeometryArray);

		if (USE_FIXED_BOUNDS)
		{
			getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
			getShape().setBounds(new BoundingSphere(new Point3d(data.center.x, data.center.y, data.center.z), data.radius));
		}
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
			if (!JOGLES_OPTIMIZED_GEOMETRY || morphable)
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
				if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
				{
					itsa = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable, false), texCoordCount, texMap, 2,
							new int[] { 3, 3 }, length, stripLengths);
				}
				else
				{
					itsa = new IndexedTriangleStripArray(data.numVertices, getFormat(data, morphable, false), texCoordCount, texMap, length,
							stripLengths);
				}

				if (morphable || BUFFERS)
					itsa.setCoordIndicesRef(points);
				else
					itsa.setCoordinateIndices(0, points);

				fillIn(itsa, data, morphable, false);

				// if not morphable cache
				if (!morphable)
				{
					sharedIGAs.put(data, itsa);
				}
				return itsa;
			}
			else
			{

				int numStrips = data.numStrips;
				int[] stripLengths = data.stripLengths;

				// get full length
				int length = 0;
				for (int i = 0; i < numStrips; i++)
				{
					length += data.points[i].length;
				}

				ByteBuffer bb = ByteBuffer.allocateDirect(length * 2);
				bb.order(ByteOrder.nativeOrder());
				ShortBuffer points = bb.asShortBuffer();
				int idx = 0;
				for (int i = 0; i < numStrips; i++)
				{
					for (int j = 0; j < stripLengths[i]; j++)
					{
						points.put(idx, (short) data.points[i][j]);
						idx++;
					}
				}

				int texCoordCount = 1;

				// All tex units use the 0ith one  
				int[] texMap = new int[9];
				for (int i = 0; i < 9; i++)
					texMap[i] = 0;

				JoglesIndexedTriangleStripArray itsa;
				if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
				{
					itsa = new JoglesIndexedTriangleStripArray(data.numVertices, getFormat(data, false, true), texCoordCount, texMap, 2,
							new int[] { 3, 3 }, length, stripLengths);
				}
				else
				{
					itsa = new JoglesIndexedTriangleStripArray(data.numVertices, getFormat(data, false, true), texCoordCount, texMap,
							length, stripLengths);
				}

				itsa.setCoordIndicesRefBuffer(points);

				
				fillIn(itsa, data, false, true);
				

				sharedIGAs.put(data, itsa);

				return itsa;

			}
		}

		new Throwable("Null IGA!").printStackTrace();
		return null;
	}

	/**
	 * Code from pipeline for stitching, but not useful there, so kept here in case needed later
	 * @param strip_len
	 * @param sarray
	 * @param initialIndexIndex
	 * @param indexCoord
	 * @return
	 */
	static ShortBuffer stitchTriStrips(int strip_len, int[] sarray, int initialIndexIndex, int[] indexCoord)
	{
		//TODO: How to make degenerate tri strips joined up
		//What do we need to put in between, in order to link up the triangles? 
		//We'll need an even number of new triangles in order to preserve the winding. 
		//We can do this by repeating the last vertex of the first row, 
		//and the first vertex of the second row. http://www.learnopengles.com/tag/degenerate-triangles/

		// first how big = size0+1...1+sizeN+1...1+sizeLen
		// equals sum strips + (numstrip*2)-2
		int totalStitchedIndexSize = 0;
		for (int i = 0; i < strip_len; i++)
			totalStitchedIndexSize += sarray[i];

		totalStitchedIndexSize += (strip_len * 2) - 2;

		// now put the tristrip indexes into a  single fat buffer, with degenerates...
		int dstOffset = initialIndexIndex;
		int srcOffset = initialIndexIndex;
		ByteBuffer bb = ByteBuffer.allocateDirect(totalStitchedIndexSize * 2);
		bb.order(ByteOrder.nativeOrder());
		ShortBuffer totalIndicesBuffer = bb.asShortBuffer();

		for (int i = 0; i < strip_len; i++)
		{
			// first one no repeated first
			if (i != 0)
			{
				//repeat first
				totalIndicesBuffer.put(dstOffset, (short) indexCoord[srcOffset]);
				dstOffset++;
			}

			int count = sarray[i];
			totalIndicesBuffer.position(dstOffset);

			for (int s = 0; s < count; s++)
				totalIndicesBuffer.put(s, (short) indexCoord[srcOffset + s]);

			dstOffset += count;
			srcOffset += count;

			//last one no repeat last
			if (i != strip_len - 1)
			{
				//repeat last
				totalIndicesBuffer.put(dstOffset, (short) indexCoord[srcOffset]);
				dstOffset++;
			}

		}
		return totalIndicesBuffer;

	}
}
