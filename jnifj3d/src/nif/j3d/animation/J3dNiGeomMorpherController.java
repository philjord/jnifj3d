package nif.j3d.animation;

import javax.media.j3d.IndexedTriangleStripArray;
import javax.media.j3d.Shape3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiTriStrips;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiMorphData;
import nif.niobject.controller.NiGeomMorpherController;

public class J3dNiGeomMorpherController extends J3dNiTimeController
{
	private Shape3D shape;

	private NiMorphData niMorphData;

	public J3dNiGeomMorpherController(NiGeomMorpherController controller, String morphFrameName, J3dNiAVObject nodeTarget, NiToJ3dData niToJ3dData)
	{
		super(controller);

		niMorphData = (NiMorphData) niToJ3dData.get(controller.data);

		if (niMorphData != null)
		{
			//System.out.println("nodeTarget " + nodeTarget);
			if (nodeTarget instanceof J3dNiTriStrips)
			{
				shape = ((J3dNiTriStrips) nodeTarget).getShape();

				//	System.out.println("" + niMorphData.numVertices);
				IndexedTriangleStripArray itsa = (IndexedTriangleStripArray) shape.getGeometry();

				int vertexFormat = itsa.getVertexFormat();
				//	System.out.println("COORDINATES " + (vertexFormat & IndexedTriangleStripArray.COORDINATES)); //3
				//	System.out.println("NORMALS " + (vertexFormat & IndexedTriangleStripArray.NORMALS)); //3
				//	System.out.println("COLOR_4 " + (vertexFormat & IndexedTriangleStripArray.COLOR_4)); //4
				//	System.out.println("TEXTURE_COORDINATE_2 " + (vertexFormat & IndexedTriangleStripArray.TEXTURE_COORDINATE_2)); //2

				int validIndexCount = itsa.getValidIndexCount();
				int[] coordinateIndices = new int[validIndexCount];
				itsa.getCoordinateIndices(0, coordinateIndices);
				//System.out.println("coordinateIndices " + coordinateIndices.length);
				float[] interleavedVertices = itsa.getInterleavedVertices();
				//System.out.println("interleavedVertices " + interleavedVertices.length);
			}
		}
	}

	@Override
	public void update(float value)
	{
		//TODO: this, but I can't find a float data track to control the morph. 
		// I do notice that the lin has a variable that points to the morph frame name?
	}

}
