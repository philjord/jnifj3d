package nif.j3d;

import java.util.ArrayList;
import java.util.List;

import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.NiSwitchNode;
import nif.niobject.bs.BSMultiBoundNode;
import nif.niobject.bs.BSTreeNode;
import utils.source.TextureSource;

public class J3dBSTreeNode extends J3dNiAVObject
{
	public J3dBSTreeNode(BSTreeNode niNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niNode, niToJ3dData);

		if (niNode.numChildren != 1)
			System.out.println("J3dBSTreeNode niNode.numChildren != 1 " + niNode.numChildren);

		NiAVObject mb = (NiAVObject) niToJ3dData.get(niNode.children[0]);
		if (!(mb instanceof BSMultiBoundNode))
			System.out.println("!BSMultiBoundNode");

		ArrayList<NiNode> switchChildren = new ArrayList<NiNode>();
		gatherSwitchChildren((BSMultiBoundNode) mb, niToJ3dData, switchChildren);

		for (int i = 0; i < switchChildren.size(); i++)
		{
			NiNode c = switchChildren.get(i);
			J3dNiNode j3dNiNode = J3dNiNode.createNiNode(c, niToJ3dData, textureSource, onlyNiNodes);
			addChild(j3dNiNode);
		}

		//TODO: first child has a skin instance for animating, so it's trishapedata has no triangles!

		//TODO: allow LOD switching from the J3dTREE 

		//TODO: bones and animation gear now
	}

	private void gatherSwitchChildren(NiNode root, NiToJ3dData niToJ3dData, List<NiNode> list)
	{
		// if we've found them do nothing allow fall up
		if (list.size() != 0)
		{
			return;
		}

		//see if there is a switch below me
		for (int i = 0; i < root.numChildren; i++)
		{
			NiAVObject c = (NiAVObject) niToJ3dData.get(root.children[i]);
			if (c instanceof NiNode)
			{
				NiNode child = (NiNode) c;
				if (child != null)
				{
					gatherSwitchChildren(child, niToJ3dData, list);
				}
			}
		}

		//if this is a switch and we found no switches below us add my children
		if (list.size() == 0 && root instanceof NiSwitchNode)
		{
			for (int i = 0; i < root.numChildren; i++)
			{
				NiNode child = (NiNode) niToJ3dData.get(root.children[i]);
				list.add(child);
			}
		}

	}

}
