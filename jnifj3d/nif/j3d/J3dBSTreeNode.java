package nif.j3d;

import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.bs.BSTreeNode;
import utils.source.TextureSource;

public class J3dBSTreeNode extends J3dNiAVObject
{

	public J3dBSTreeNode(BSTreeNode niNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niNode, niToJ3dData);

		for (int i = 0; i < niNode.numChildren; i++)
		{
			NiAVObject child = (NiAVObject) niToJ3dData.get(niNode.children[i]);
			if (child != null)
			{
				if (child instanceof NiNode)
				{
					J3dNiNode j3dNiNode = J3dNiNode.createNiNode((NiNode) child, niToJ3dData, textureSource, onlyNiNodes);
					addChild(j3dNiNode);
				}
				else
				{
					System.out.println("J3dBSTreeNode - unhandled child " + child);
				}
			}
		}

		//TODO: bones and nimation gear now
	}

}
