package nif.j3d;

import java.util.Enumeration;

import javax.media.j3d.Node;
import javax.media.j3d.OrderedGroup;

import nif.niobject.bs.BSOrderedNode;
import utils.source.TextureSource;

public class J3dBSOrderedNode extends J3dNiNode
{
	//Note super will call addchild before this node is inited, so must be constructed on first addchild call
	private OrderedGroup orderedGroup;

	//NOTE this class must be set uncompactable as this screws with things J3dNiAVObject does
	protected J3dBSOrderedNode(BSOrderedNode bSOrderedNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes, boolean noShares)
	{
		super(bSOrderedNode, niToJ3dData, textureSource, onlyNiNodes,   noShares);
		configureOrderedGroup();
	}

	private void configureOrderedGroup()
	{
		if (orderedGroup == null)
		{
			setUncompactable();
			orderedGroup = new OrderedGroup();
			super.addChild(orderedGroup);
		}
	}

	@Override
	public void addChild(Node child)
	{
		configureOrderedGroup();
		orderedGroup.insertChild(child, 0);// reverse order
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<Node> getAllChildren()
	{
		return orderedGroup.getAllChildren();
	}

	@Override
	public Node getChild(int index)
	{
		return orderedGroup.getChild(index);
	}

	@Override
	public int indexOfChild(Node child)
	{
		return orderedGroup.indexOfChild(child);
	}

	@Override
	public void insertChild(Node child, int index)
	{
		orderedGroup.insertChild(child, index);
	}

	@Override
	public int numChildren()
	{
		return orderedGroup.numChildren();
	}

	@Override
	public void removeAllChildren()
	{
		orderedGroup.removeAllChildren();
	}

	@Override
	public void removeChild(int index)
	{
		orderedGroup.removeChild(index);
	}

	@Override
	public void removeChild(Node child)
	{
		orderedGroup.removeChild(child);
	}

	@Override
	public void setChild(Node child, int index)
	{
		orderedGroup.setChild(child, index);
	}
}
