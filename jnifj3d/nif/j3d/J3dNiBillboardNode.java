package nif.j3d;

import java.util.Enumeration;

import javax.media.j3d.Billboard;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import utils.source.TextureSource;

import nif.enums.BillboardMode;
import nif.niobject.NiBillboardNode;

/**
 * @author Administrator
 *
 */
public class J3dNiBillboardNode extends J3dNiNode
{
	private static BoundingSphere defaultBounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY);

	//Note super will call addchild before this node is intied, so must be constructed on first addchild call
	private TransformGroup billboardGroup;

	private TransformGroup uprighterGroup;

	//NOTE uncompactable as this screws with things J3dNiAVObject does
	protected J3dNiBillboardNode(NiBillboardNode niBillboardNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niBillboardNode, niToJ3dData, textureSource, onlyNiNodes);

		// note this may have been configred by an addchild from the super constructor
		if (billboardGroup == null)
		{
			setupGroups();
		}

		Billboard billBehave = null;

		if (niBillboardNode.billboardMode.mode == BillboardMode.ROTATE_ABOUT_UP
				|| niBillboardNode.billboardMode.mode == BillboardMode.ROTATE_ABOUT_UP2)
		{
			billBehave = new Billboard(billboardGroup, Billboard.ROTATE_ABOUT_AXIS, new Vector3f(0, 1, 0));
		}
		else
		{
			billBehave = new Billboard(billboardGroup, Billboard.ROTATE_ABOUT_POINT, new Point3f(0, 0, 0));
		}

		billBehave.setSchedulingBounds(defaultBounds);
		billBehave.setEnable(true);
		addChild(billBehave);
	}

	private void setupGroups()
	{
		setUncompactable();
		billboardGroup = new TransformGroup();

		uprighterGroup = new TransformGroup();
		// turn z to be our y up (-z to y)  
		Transform3D rot = new Transform3D();
		rot.rotX(-Math.PI / 2f);
		uprighterGroup.setTransform(rot);

		super.addChild(billboardGroup);
		billboardGroup.addChild(uprighterGroup);
		billboardGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	}

	@Override
	public void addChild(Node child)
	{
		if (billboardGroup == null)
		{
			setupGroups();
		}

		uprighterGroup.addChild(child);

	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<Node> getAllChildren()
	{
		return uprighterGroup.getAllChildren();
	}

	@Override
	public Node getChild(int index)
	{
		return uprighterGroup.getChild(index);
	}

	@Override
	public int indexOfChild(Node child)
	{
		return uprighterGroup.indexOfChild(child);
	}

	@Override
	public void insertChild(Node child, int index)
	{
		uprighterGroup.insertChild(child, index);
	}

	@Override
	public int numChildren()
	{
		return uprighterGroup.numChildren();
	}

	@Override
	public void removeAllChildren()
	{
		uprighterGroup.removeAllChildren();
	}

	@Override
	public void removeChild(int index)
	{
		uprighterGroup.removeChild(index);
	}

	@Override
	public void removeChild(Node child)
	{
		uprighterGroup.removeChild(child);
	}

	@Override
	public void setChild(Node child, int index)
	{
		uprighterGroup.setChild(child, index);
	}
}
