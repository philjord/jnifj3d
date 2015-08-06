package nif.j3d;

import java.util.Enumeration;

import javax.media.j3d.Node;
import javax.media.j3d.Switch;

import nif.niobject.NiSwitchNode;
import utils.source.TextureSource;

/**
 * Note this class is used for LOD detail or something? 
 * I notice it is definately used for birds next birdsnest01.nif for plucked and unplucked egg also for floracabbage
 * 
 * It is also used by Trees to go from waving up close detail version to stationary but still geometric version
 * 
 * I can't (yet) see any indicator, except that the tristrips below the ninnodes below the switch have *01:0 or *01_1:0 on the name
 * I presume it's in the esm file or the engine directly?
 * 
 * NiSwitchNode always have 2 children, for now select last one
 * 
 * the close up tree is in fact skinning and animated nicely, 
 * so for now I make that one SUPER close only (cos it's invisible)
 * 
 * The select of the second(non plucked) FLOR switch must reside in the REFR itslef
 * 
 * The first switch path (highest detail I think?) is the only one with a havok attached (e.g. treeaspen01.nif)
 * 
 * @author philip
 *
 */
public class J3dNiSwitchNode extends J3dNiNode
{

	//Note super will call addchild before this node is inited, so must be constructed on first addchild call

	//NOTE uncompactable as this screws with things J3dNiAVObject does

	protected Switch switchGroup;

	protected J3dNiSwitchNode(NiSwitchNode niSwitchNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niSwitchNode, niToJ3dData, textureSource, onlyNiNodes);
		configureOrderedGroup();
	}

	protected void configureOrderedGroup()
	{
		if (switchGroup == null)
		{
			setUncompactable();
			switchGroup = new Switch(0);
			switchGroup.setCapability(Switch.ALLOW_SWITCH_WRITE);
			super.addChild(switchGroup);
		}
	}

	@Override
	public void addChild(Node child)
	{
		configureOrderedGroup();
		switchGroup.addChild(child);
		switchGroup.setWhichChild(0);
		//switchGroup.setWhichChild(switchGroup.numChildren() - 1);
	}

	@Override
	public int numChildren()
	{
		return switchGroup.numChildren();
	}

	@Override
	public Node getChild(int index)
	{
		return switchGroup.getChild(index);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<Node> getAllChildren()
	{
		return switchGroup.getAllChildren();
	}

	@Override
	public int indexOfChild(Node child)
	{
		return switchGroup.indexOfChild(child);
	}

	@Override
	public void insertChild(Node child, int index)
	{
		switchGroup.insertChild(child, index);
	}

	@Override
	public void removeAllChildren()
	{
		switchGroup.removeAllChildren();
	}

	@Override
	public void removeChild(int index)
	{
		switchGroup.removeChild(index);
	}

	@Override
	public void removeChild(Node child)
	{
		switchGroup.removeChild(child);
	}

	@Override
	public void setChild(Node child, int index)
	{
		switchGroup.setChild(child, index);
	}
}
