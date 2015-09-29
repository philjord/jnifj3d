package nif.j3d;

import java.util.Enumeration;

import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;

import nif.niobject.NiAVObject;
import nif.niobject.NiSequenceStreamHelper;
import nif.niobject.bs.BSFadeNode;
import utils.convert.ConvertFromNif;

import com.sun.j3d.utils.geometry.ColorCube;

public abstract class J3dNiAVObject extends J3dNiObjectNET
{
	private NifTransformGroup transformGroup;

	private NiAVObject niAVObject;

	private boolean compactable = true;

	public ColorCube visualMarker;

	public J3dNiAVObject(NiAVObject niAVObject, NiToJ3dData niToJ3dData)
	{
		super(niAVObject, niToJ3dData);
		this.setName(niAVObject.name);

		this.niAVObject = niAVObject;

		niToJ3dData.put(niAVObject, this);

		transformGroup = new NifTransformGroup(this);

		Transform3D t1 = new Transform3D();

		if (!(niAVObject instanceof BSFadeNode))
		{
			t1.setRotation(ConvertFromNif.toJ3d(niAVObject.rotation));
			//the determinant should be near 1 now! otherwise all transforms will be crap			 
			if (Math.abs(1 - t1.determinant()) > 0.2)
			{
				System.out.println("Determinant problem in " + niAVObject.name);
			}
			t1.setTranslation(ConvertFromNif.toJ3d(niAVObject.translation));
			t1.setScale(niAVObject.scale);
		}

		transformGroup.setTransform(t1);
		super.addChild(transformGroup);
	}

	//for Tes3 kf files they are subs of NiObjectNET but I can't risk altering everything in the univers
	public J3dNiAVObject(NiSequenceStreamHelper niSequenceStreamHelper, NiToJ3dData niToJ3dData)
	{
		super(niSequenceStreamHelper, niToJ3dData);
		this.setName(niSequenceStreamHelper.name);
		// notice nothing added to niToJ3dData
		// notice no transform group
	}

	/**
	 * NOTE very expensive only use if attaching/detaching whilst live required 
	 */
	private J3dRootNode j3dRootNode;

	public J3dRootNode getRootNode()
	{
		if (j3dRootNode == null)
		{
			j3dRootNode = new J3dRootNode(this);
			j3dRootNode.addChild(this);
		}
		return j3dRootNode;
	}

	public NiAVObject getNiAVObject()
	{
		return niAVObject;
	}

	public boolean isCompactable()
	{
		return compactable;
	}

	public void setUncompactable()
	{
		// make sure the transform group exists
		recreateTransformGroup();

		transformGroup.makeWritable();
		this.compactable = false;
	}

	public void compact()
	{
		if (compactable && transformGroup != null && transformGroup.isNoImpact())
		{
			super.removeChild(transformGroup);
			Enumeration<Node> children = getAllChildren();
			removeAllChildren();
			while (children.hasMoreElements())
			{
				Node child = children.nextElement();
				super.addChild(child);
			}

			transformGroup = null;
		}
	}

	@SuppressWarnings("unchecked")
	private void recreateTransformGroup()
	{
		if (transformGroup == null)
		{
			Enumeration<Node> children = super.getAllChildren();
			super.removeAllChildren();

			transformGroup = new NifTransformGroup(this);

			while (children.hasMoreElements())
			{
				Node child = children.nextElement();
				transformGroup.addChild(child);
			}

			super.addChild(transformGroup);
		}
	}

	public NifTransformGroup getTransformGroup()
	{
		// ensure group exist if it's been removed
		recreateTransformGroup();
		return transformGroup;
	}

	/**
	 * For use by Particle system mainly to get at the real root
	 * @param child
	 */
	public void addChildBeforeTrans(Node child)
	{
		super.addChild(child);
	}

	/**
	 * For use by Particle system mainly to get at the real root
	 * @param child
	 */
	public void removeChildBeforeTrans(Node child)
	{
		super.removeChild(child);
	}

	@Override
	public void addChild(Node child)
	{
		if (transformGroup != null)
		{
			transformGroup.addChild(child);
			if (child instanceof J3dNiAVObject)
			{
				((J3dNiAVObject) child).topOfParent = this;
			}
		}
		else
		{
			super.addChild(child);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<Node> getAllChildren()
	{
		if (transformGroup != null)
		{
			return transformGroup.getAllChildren();
		}
		else
		{
			return super.getAllChildren();
		}
	}

	@Override
	public Node getChild(int index)
	{
		if (transformGroup != null)
		{
			return transformGroup.getChild(index);
		}
		else
		{
			return super.getChild(index);
		}
	}

	@Override
	public int indexOfChild(Node child)
	{
		if (transformGroup != null)
		{
			return transformGroup.indexOfChild(child);
		}
		else
		{
			return super.indexOfChild(child);
		}
	}

	@Override
	public void insertChild(Node child, int index)
	{
		if (transformGroup != null)
		{
			transformGroup.insertChild(child, index);
			if (child instanceof J3dNiAVObject)
			{
				((J3dNiAVObject) child).topOfParent = this;
			}
		}
		else
		{
			super.insertChild(child, index);
		}
	}

	@Override
	public int numChildren()
	{
		if (transformGroup != null)
		{
			return transformGroup.numChildren();
		}
		else
		{
			return super.numChildren();
		}
	}

	@Override
	public void removeAllChildren()
	{
		if (transformGroup != null)
		{
			transformGroup.removeAllChildren();
		}
		else
		{
			super.removeAllChildren();
		}
	}

	@Override
	public void removeChild(int index)
	{
		if (transformGroup != null)
		{
			transformGroup.removeChild(index);
		}
		else
		{
			super.removeChild(index);
		}
	}

	@Override
	public void removeChild(Node child)
	{
		if (transformGroup != null)
		{
			transformGroup.removeChild(child);
		}
		else
		{
			super.removeChild(child);
		}
	}

	@Override
	public void setChild(Node child, int index)
	{
		if (transformGroup != null)
		{
			transformGroup.setChild(child, index);
			if (child instanceof J3dNiAVObject)
			{
				((J3dNiAVObject) child).topOfParent = this;
			}
		}
		else
		{
			super.setChild(child, index);
		}
	}

	/**
	 * It stops multipling when it meets the root object, it does NOT include thte trasn of the root!
	 * @param t
	 * @param rootJ3dNiAVObject
	 */
	public void getTreeTransform(Transform3D t, J3dNiAVObject rootJ3dNiAVObject)
	{
		// blank it out
		t.setIdentity();

		// each call adds to current up the tree
		getTreeTransformImpl(t, rootJ3dNiAVObject);
	}

	//NOTE not for the use of ANYTHING but the getTreeTransform rollup system!!!!
	// and the bone top parent system too
	// It can point to madness very easily!
	public J3dNiAVObject topOfParent;

	private void getTreeTransformImpl(Transform3D t, J3dNiAVObject rootJ3dNiAVObject)
	{
		if (this != rootJ3dNiAVObject)
		{
			if (this.topOfParent != null)
			{
				// in this case our parent is the bottom of the nif group and so we call the topofparent pointer				
				this.topOfParent.getTreeTransformImpl(t, rootJ3dNiAVObject);
			}
			else
			{
				// getParent is an expensive call (mad?) so avoid repeats.
				Object parent = this.getParent();
				if (parent instanceof J3dNiAVObject)
				{
					((J3dNiAVObject) parent).getTreeTransformImpl(t, rootJ3dNiAVObject);
				}
				//moved to the above spot but needs testing
				/*else if (parent instanceof TransformGroup && this.topOfParent != null)
				{
					// in this case our parent is the bottom of the nif group and so we call the topofparent pointer				
					this.topOfParent.getTreeTransformImpl(t, rootJ3dNiAVObject);
				}*/
			}

			if (transformGroup != null)
			{
				transformGroup.transformMul(t);
			}
		}

	}

	public void setVisualMarker(boolean visualMarkerOn)
	{
		if (visualMarkerOn && visualMarker == null)
		{
			visualMarker = new ColorCube(0.01f);
			this.addChild(visualMarker);
		}
		else if (!visualMarkerOn && visualMarker != null)
		{
			this.removeChild(visualMarker);
			visualMarker = null;
		}
	}
}
