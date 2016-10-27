package nif.j3d;

import java.util.ArrayList;

import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;

import nif.NifVer;
import nif.j3d.animation.SequenceAlpha.SequenceAlphaListener;
import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.NiSequenceStreamHelper;
import nif.niobject.bs.BSFadeNode;
import tools3d.utils.leafnode.Cube;
import utils.convert.ConvertFromNif;

public abstract class J3dNiAVObject extends J3dNiObjectNET implements SequenceAlphaListener
{
	protected NiAVObject niAVObject;

	public Cube visualMarker;

	private ArrayList<AccumNodeListener> transformListeners = null;

	public J3dNiAVObject(NiAVObject niAVObject, NiToJ3dData niToJ3dData)
	{
		super(niAVObject, niToJ3dData);
		this.setName(niAVObject.name);

		this.niAVObject = niAVObject;

		niToJ3dData.put(niAVObject, this);

		Transform3D t1 = new Transform3D();

		if (!ignoreTopTransformRot(niAVObject))
		{
			t1.setRotation(ConvertFromNif.toJ3d(niAVObject.rotation));
		}
		/*
				//the determinant should be near 1 now! otherwise all transforms will be crap			 
					if (Math.abs(1 - t1.determinant()) > 0.2)
				{
							System.out.println("Determinant problem in " + niAVObject.name);
				}*/
		t1.setTranslation(ConvertFromNif.toJ3d(niAVObject.translation));
		t1.setScale(niAVObject.scale);

		this.setTransform(t1);
	}

	//  Oblivion does not ignore root rotations (will return false here
	// all other games assume the placement type operations happen on the root node
	// excpet morrowind does not ignore Bip01 node transforms
	public static boolean ignoreTopTransformRot(NiAVObject niAVObject)
	{
		boolean ignoreTopTransform = (niAVObject instanceof BSFadeNode) || //fallout and upwards
				(niAVObject.nVer.LOAD_VER < NifVer.VER_10_0_1_0 && // morrowind
						niAVObject instanceof NiNode && niAVObject.parent == null && !niAVObject.name.equals("Bip01")); // check for root
		return ignoreTopTransform;
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

	public void setUncompactable()
	{
		makeWritable();

	}

	/**
	 * For use by Particle system mainly to get at the real root
	 * @param child
	 */
	public void addChildBeforeTrans(Node child)
	{
		//TODO: bad news impossible now, need to find the parent or root or something else
		super.addChild(child);
	}

	/**
	 * For use by Particle system mainly to get at the real root
	 * @param child
	 */
	public void removeChildBeforeTrans(Node child)
	{
		//TODO: bad news impossible now, need to find the parent or root or something else
		super.removeChild(child);
	}

	@Override
	public void addChild(Node child)
	{
		if (child instanceof J3dNiAVObject)
		{
			((J3dNiAVObject) child).topOfParent = this;
		}

		super.addChild(child);

	}

	@Override
	public void insertChild(Node child, int index)
	{
		if (child instanceof J3dNiAVObject)
		{
			((J3dNiAVObject) child).topOfParent = this;
		}

		super.insertChild(child, index);
	}

	@Override
	public void setChild(Node child, int index)
	{
		if (child instanceof J3dNiAVObject)
		{
			((J3dNiAVObject) child).topOfParent = this;
		}
		super.setChild(child, index);
	}

	/**
	 * It stops multiplying when it meets the root object, it does NOT include the trans of the root!
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
			}

			transformMul(t);
		}

	}

	public void setVisualMarker(boolean visualMarkerOn)
	{
		if (visualMarkerOn && visualMarker == null)
		{
			visualMarker = new Cube(0.01f);
			this.addChild(visualMarker);
		}
		else if (!visualMarkerOn && visualMarker != null)
		{
			this.removeChild(visualMarker);
			visualMarker = null;
		}
	}

	public void makeWritable()
	{
		if (!isLive() && !isCompiled())
		{
			this.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			this.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		}
	}

	// this is turned on if transformMul is called at all (from the getTreeTransformImpl)
	private Transform3D transformCache;

	@Override
	public void setTransform(Transform3D t1)
	{
		//notice that the listener can modify the transform if desired
		if (transformListeners != null)
		{
			for (AccumNodeListener tl : transformListeners)
			{
				tl.transformSet(t1);
			}
		}

		if (transformCache != null)
			transformCache.set(t1);

		super.setTransform(t1);

	}

	public void transformMul(Transform3D t)
	{
		if (transformCache == null)
		{
			transformCache = new Transform3D();
			this.getTransform(transformCache);
		}

		//TODO: this is still expensive, think about making a generic fast version like SkinData?
		// the isAffine call seems pricey, but I'm ALWAYS affine, so I use faster version below
		//t.mul(transformCache);
		affineTransfromMul(t, transformCache);

	}

	/**
	 * Assume BOTH are Affine!!
	 * same as t.mul(t1); basically
	 * @param mat
	 * @param t1Mat
	 */
	//TODO: put in utils somewhere
	public static void affineTransfromMul(Transform3D t, Transform3D t1)
	{
		double[] mat = new double[16];
		t.get(mat);
		double[] t1Mat = new double[16];
		t1.get(t1Mat);
		double tmp0, tmp1, tmp2, tmp3;
		double tmp4, tmp5, tmp6, tmp7;
		double tmp8, tmp9, tmp10, tmp11;

		if (Double.isNaN(mat[0]))
		{
			new Throwable("Spotted a NaN!").printStackTrace();
		}

		tmp0 = mat[0] * t1Mat[0] + mat[1] * t1Mat[4] + mat[2] * t1Mat[8];
		tmp1 = mat[0] * t1Mat[1] + mat[1] * t1Mat[5] + mat[2] * t1Mat[9];
		tmp2 = mat[0] * t1Mat[2] + mat[1] * t1Mat[6] + mat[2] * t1Mat[10];
		tmp3 = mat[0] * t1Mat[3] + mat[1] * t1Mat[7] + mat[2] * t1Mat[11] + mat[3];
		tmp4 = mat[4] * t1Mat[0] + mat[5] * t1Mat[4] + mat[6] * t1Mat[8];
		tmp5 = mat[4] * t1Mat[1] + mat[5] * t1Mat[5] + mat[6] * t1Mat[9];
		tmp6 = mat[4] * t1Mat[2] + mat[5] * t1Mat[6] + mat[6] * t1Mat[10];
		tmp7 = mat[4] * t1Mat[3] + mat[5] * t1Mat[7] + mat[6] * t1Mat[11] + mat[7];
		tmp8 = mat[8] * t1Mat[0] + mat[9] * t1Mat[4] + mat[10] * t1Mat[8];
		tmp9 = mat[8] * t1Mat[1] + mat[9] * t1Mat[5] + mat[10] * t1Mat[9];
		tmp10 = mat[8] * t1Mat[2] + mat[9] * t1Mat[6] + mat[10] * t1Mat[10];
		tmp11 = mat[8] * t1Mat[3] + mat[9] * t1Mat[7] + mat[10] * t1Mat[11] + mat[11];

		mat[12] = mat[13] = mat[14] = 0;
		mat[15] = 1;

		mat[0] = tmp0;
		mat[1] = tmp1;
		mat[2] = tmp2;
		mat[3] = tmp3;
		mat[4] = tmp4;
		mat[5] = tmp5;
		mat[6] = tmp6;
		mat[7] = tmp7;
		mat[8] = tmp8;
		mat[9] = tmp9;
		mat[10] = tmp10;
		mat[11] = tmp11;

		t.set(mat);
	}

	private static Transform3D IDENTITY = new Transform3D();

	public boolean isNoImpact()
	{
		Transform3D temp2 = new Transform3D();
		this.getTransform(temp2);
		return temp2.equals(IDENTITY) && !this.getCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	}

	public void compact()
	{
		// TODO: is there anything to do now? iterate through children?

	}

	public void addTransformListener(AccumNodeListener tl)
	{
		if (transformListeners == null)
			transformListeners = new ArrayList<AccumNodeListener>();

		transformListeners.add(tl);
	}

	public void removeTransformListener(AccumNodeListener tl)
	{
		if (transformListeners != null)
			transformListeners.remove(tl);
	}

	public interface AccumNodeListener extends SequenceAlphaListener
	{
		public void transformSet(Transform3D t1);

	}

	@Override
	public void sequenceFinished()
	{
		if (transformListeners != null)
		{
			for (AccumNodeListener tl : transformListeners)
			{
				tl.sequenceFinished();
			}
		}
	}

	@Override
	public void sequenceStarted()
	{
		if (transformListeners != null)
		{
			for (AccumNodeListener tl : transformListeners)
			{
				tl.sequenceStarted();
			}
		}
	}

	@Override
	public void sequenceLooped(boolean inner)
	{
		if (transformListeners != null)
		{
			for (AccumNodeListener tl : transformListeners)
			{
				tl.sequenceLooped(inner);
			}
		}
	}

}
