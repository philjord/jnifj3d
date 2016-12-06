package nif.j3d;

import java.util.Iterator;

import org.jogamp.java3d.Billboard;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.NifVer;
import nif.enums.BillboardMode;
import nif.niobject.NiBillboardNode;
import tools3d.utils.Utils3D;
import tools3d.utils.scenegraph.Billboard2;
import utils.source.TextureSource;

/**
 * NiTrishapeNodes below NiBillboardNodes in nif files (at least in obliv) are built with X and Y being the "flat"
 * components which is not the usual format, as normally in nif Z is up.
 * 
 * Then in world placement witht eh refr they are set down on their sides so so they are "upright". All this means that
 * teh local coordinates around which the billboard must spin is Z, which is not allowed so we spin just off Z and get
 * teh effect we want. To see this in action uncomments the placement cubes
 * 
 * @author Administrator
 *
 */
public class J3dNiBillboardNode extends J3dNiNode
{
	// Note super will call addchild before this node is inited, so must be constructed on first addchild call
	private TransformGroup billboardGroup;
	private TransformGroup up;// only for POINT

	// NOTE uncompactable as this screws with things J3dNiAVObject does
	protected J3dNiBillboardNode(NiBillboardNode niBillboardNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niBillboardNode, niToJ3dData, textureSource, onlyNiNodes);

		// note this may have been configred by an addchild from the super constructor
		setupGroups();

		Billboard2 billBehave = null;
		int mode = BillboardMode.ROTATE_ABOUT_UP;
		if (niBillboardNode.nVer.LOAD_VER >= NifVer.VER_10_1_0_0)
		{
			mode = niBillboardNode.billboardMode.mode;
		}
		else
		{
			// TODO: check this
			// In pre-10.1.0.0 the Flags field is used for BillboardMode.
			// Bit 0: hidden
			// Bits 1-2: collision mode
			// Bit 3: unknown (set in most official meshes)
			// Bits 5-6: billboard mode
			mode = (niBillboardNode.flags.flags >> 5) & 0x3;
		}

		if (mode == BillboardMode.ROTATE_ABOUT_UP || mode == BillboardMode.ROTATE_ABOUT_UP2)
		{

			// AXIS rotates are fixed by the odd axis below
			billBehave = new Billboard2(billboardGroup, Billboard.ROTATE_ABOUT_AXIS, new Vector3f(0f, 0.1f, 1f));
		}
		else
		{
			//point rotates are fixed by this uprighter
			Transform3D upT = new Transform3D();
			upT.rotX(Math.PI / 2f);
			up.setTransform(upT);
			billBehave = new Billboard2(billboardGroup, Billboard.ROTATE_ABOUT_POINT, new Point3f(0, 0, 0));
		}

		billBehave.setSchedulingBounds(Utils3D.defaultBounds);
		billBehave.setEnable(true);
		addChild(billBehave);

		// test nodes to show the truth of transforms

		/*		TransformGroup tg = new TransformGroup();
				Transform3D t = new Transform3D();
				t.setTranslation(new Vector3f(0, 1, 0));
				tg.setTransform(t);
				super.addChild(tg);
		
				Cube cc = new Cube(0.05);
				tg.addChild(cc);
				Cube cc2 = new Cube(0.025);
				super.addChild(cc2);
		
				// let's add a testy 
				TransformGroup tg2 = new TransformGroup();
				Transform3D t2 = new Transform3D();
				t2.setTranslation(new Vector3f(0, 1, 0));
				tg2.setTransform(t2);
				addChild(tg2);
		
				Cube cc4 = new Cube(0.025);
				tg2.addChild(cc4);
				Cube cc5 = new Cube(0.025);
				addChild(cc5);*/

	}

	private void setupGroups()
	{
		if (billboardGroup == null)
		{
			setUncompactable();

			billboardGroup = new TransformGroup();
			billboardGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			billboardGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

			// this fixes POINT rotates but not AXIS
			up = new TransformGroup();
			super.addChild(billboardGroup);
			billboardGroup.addChild(up);
		}
	}

	@Override
	public void addChild(Node child)
	{
		if (billboardGroup == null)
		{
			setupGroups();
		}

		up.addChild(child);

	}

	@Override
	public Iterator<Node> getAllChildren()
	{
		return up.getAllChildren();
	}

	@Override
	public Node getChild(int index)
	{
		return up.getChild(index);
	}

	@Override
	public int indexOfChild(Node child)
	{
		return up.indexOfChild(child);
	}

	@Override
	public void insertChild(Node child, int index)
	{
		up.insertChild(child, index);
	}

	@Override
	public int numChildren()
	{
		return up.numChildren();
	}

	@Override
	public void removeAllChildren()
	{
		up.removeAllChildren();
	}

	@Override
	public void removeChild(int index)
	{
		up.removeChild(index);
	}

	@Override
	public void removeChild(Node child)
	{
		up.removeChild(child);
	}

	@Override
	public void setChild(Node child, int index)
	{
		up.setChild(child, index);
	}
}
