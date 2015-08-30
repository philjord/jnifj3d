package nif.j3d;

import java.util.ArrayList;

import nif.niobject.NiAVObject;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriShape;
import nif.niobject.NiTriStrips;
import nif.niobject.RootCollisionNode;
import utils.PhysAppearance;

/**
 * NOTE for trival render only now, bullet does the hard work!
 * normals ignored as slow and cause huge indexify operations due to diff indices counts
 * @author philip
 *
 */
public class J3dRootCollisionNode extends J3dNiAVObject
{
	private ArrayList<J3dNiTriBasedGeom> j3dNiNodes = new ArrayList<J3dNiTriBasedGeom>();

	public J3dRootCollisionNode(RootCollisionNode niNode, NiToJ3dData niToJ3dData)
	{
		super(niNode, niToJ3dData);
		for (int i = 0; i < niNode.numChildren; i++)
		{
			NiAVObject child = (NiAVObject) niToJ3dData.get(niNode.children[i]);
			if (child != null)
			{

				if (child instanceof NiTriBasedGeom)
				{
					NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom) child;
					J3dNiTriBasedGeom ntbg = null;

					if (niTriBasedGeom instanceof NiTriShape)
					{
						NiTriShape niTriShape = (NiTriShape) niTriBasedGeom;
						ntbg = new J3dNiTriShape(niTriShape, niToJ3dData, null);
						ntbg.getShape().setAppearance(new PhysAppearance());
					}
					else if (niTriBasedGeom instanceof NiTriStrips)
					{
						NiTriStrips niTriStrips = (NiTriStrips) niTriBasedGeom;
						ntbg = new J3dNiTriStrips(niTriStrips, niToJ3dData, null);
						ntbg.getShape().setAppearance(new PhysAppearance());
					}
					j3dNiNodes.add(ntbg);
					addChild(ntbg);
				}
				else
				{
					System.out.println("NON! NiTriBasedGeom child of " + this);
				}
			}
		}
	}

}
