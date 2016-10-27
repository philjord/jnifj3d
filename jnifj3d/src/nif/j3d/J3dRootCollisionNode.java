package nif.j3d;

import java.util.ArrayList;

import org.jogamp.java3d.Group;

import nif.niobject.AvoidNode;
import nif.niobject.NiAVObject;
import nif.niobject.NiTriShape;
import nif.niobject.RootCollisionNode;
import tools3d.utils.PhysAppearance;

/**
 * NOTE for trival render only now, bullet does the hard work!
 * normals ignored as slow and cause huge indexify operations due to diff indices counts
 * @author philip
 *
 */
public class J3dRootCollisionNode extends Group
{

	//F:\game media\Morrowind\Meshes\base_anim.1st.nif fix
	private ArrayList<J3dNiTriBasedGeom> j3dNiNodes = new ArrayList<J3dNiTriBasedGeom>();

	public J3dRootCollisionNode(RootCollisionNode niNode, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < niNode.numChildren; i++)
		{
			NiAVObject child = (NiAVObject) niToJ3dData.get(niNode.children[i]);
			if (child != null)
			{
				if (child instanceof NiTriShape)
				{
					NiTriShape niTriShape = (NiTriShape) child;
					J3dNiTriBasedGeom ntbg = new J3dNiTriShape(niTriShape, niToJ3dData, null);
					ntbg.getShape().setAppearance(PhysAppearance.makeAppearance());

					j3dNiNodes.add(ntbg);
					addChild(ntbg);
				}
				else if (child instanceof AvoidNode)
				{
					// possibly for AI? ignore
				}
				else
				{
					System.out.println("NON! NiTriBasedGeom child of " + this + " " + child);
				}
			}
		}
	}

}
