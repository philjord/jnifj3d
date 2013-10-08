package nif.j3d;

import java.util.Enumeration;

import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import nif.niobject.NiAVObject;
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
		super(niAVObject);
		this.setName(niAVObject.name);

		this.niAVObject = niAVObject;

		niToJ3dData.put(niAVObject, this);

		transformGroup = new NifTransformGroup();

		Transform3D t1 = new Transform3D();
		// TODO: for enclavetable01 no rotation, is this the general case? d
		if (!(niAVObject instanceof BSFadeNode))
		{
			t1.setRotation(ConvertFromNif.toJ3d(niAVObject.rotation));
			t1.setTranslation(ConvertFromNif.toJ3d(niAVObject.translation));
			t1.setScale(niAVObject.scale);
		}

		transformGroup.setTransform(t1);

		super.addChild(transformGroup);

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
		this.compactable = false;
		if (!compactable)
		{
			recreateTransformGroup();
			transformGroup.makeWritable();
		}
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

			transformGroup = new NifTransformGroup();

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
	 * I wish I had record how this works :( I think it need the root of the model (or the non accum node for preference)
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

	//NOTE not for the use of ANYTHING but the getFulltree rollup system!!!! It can point to madness very easily
	private J3dNiAVObject topOfParent;

	private void getTreeTransformImpl(Transform3D t, J3dNiAVObject rootJ3dNiAVObject)
	{
		if (this != rootJ3dNiAVObject)
		{
			if (this.getParent() instanceof J3dNiAVObject)
			{
				((J3dNiAVObject) getParent()).getTreeTransformImpl(t, rootJ3dNiAVObject);
			}
			else if (this.getParent() instanceof TransformGroup && this.topOfParent != null)
			{
				// in this case our parent is the bootom of the nif group and so we call the topofparent pointer				
				this.topOfParent.getTreeTransformImpl(t, rootJ3dNiAVObject);
			}
		}

		if (transformGroup != null)
		{
			transformGroup.transformMul(t);
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