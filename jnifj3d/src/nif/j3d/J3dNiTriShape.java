package nif.j3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.JoglesIndexedTriangleArray;

import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.bs.BSLODTriShape;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

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
			currentGeometryArray.setName(niTriShape.toString() + " : " + data.nVer.fileName);
			getShape().setGeometry(currentGeometryArray);

			if (USE_FIXED_BOUNDS)
			{
				getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
				getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center), ConvertFromNif.toJ3d(data.radius)));
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
			int texCoordCount = 1;

			// All tex units use the 0ith , all others are ignored
			int[] texMap = new int[9];
			for (int i = 0; i < 9; i++)
				texMap[i] = 0;

			if (!JOGLES_OPTIMIZED_GEOMETRY || morphable)
			{
				IndexedGeometryArray ita;
				if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
				{
					ita = new IndexedTriangleArray(data.numVertices, getFormat(data, morphable, false), texCoordCount, texMap, 2,
							new int[] { 3, 3 }, data.numTrianglePoints);
					ita.clearCapabilities();
				}
				else
				{
					ita = new IndexedTriangleArray(data.numVertices, getFormat(data, morphable, false), texCoordCount, texMap,
							data.numTrianglePoints);
					ita.clearCapabilities();
				}

				if (morphable || BUFFERS)
					ita.setCoordIndicesRef(data.trianglesOpt);
				else
					ita.setCoordinateIndices(0, data.trianglesOpt);

				fillIn(ita, data, morphable, false);

				if (!morphable)
				{
					sharedIGAs.put(data, ita);
				}
				return ita;
			}
			else
			{
				JoglesIndexedTriangleArray ita;
				if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
				{
					ita = new JoglesIndexedTriangleArray(data.numVertices, getFormat(data, false, true), texCoordCount, texMap, 2,
							new int[] { 3, 3 }, data.numTrianglePoints);
					ita.clearCapabilities();
				}
				else
				{
					ita = new JoglesIndexedTriangleArray(data.numVertices, getFormat(data, false, true), texCoordCount, texMap,
							data.numTrianglePoints);
					ita.clearCapabilities();
				}

				ByteBuffer bb = ByteBuffer.allocateDirect(data.trianglesOpt.length * 2);
				bb.order(ByteOrder.nativeOrder());
				ShortBuffer indBuf = bb.asShortBuffer();
				for (int s = 0; s < data.trianglesOpt.length; s++)
					indBuf.put(s, (short) data.trianglesOpt[s]);
				indBuf.position(0);

				ita.setCoordIndicesRefBuffer(indBuf);

				fillIn(ita, data, false, true);

				sharedIGAs.put(data, ita);

				return ita;
			}

		}
		//TODO: some trishapes with skin data nearby have no tris (it's in skin data)
		//data.hasTriangles = no in trees in skyrim down the switch paths
		return null;
	}
}