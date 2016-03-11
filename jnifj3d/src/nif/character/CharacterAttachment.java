package nif.character;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Quat4f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.niobject.NiAVObject;
import tools3d.utils.scenegraph.Fadable;
import utils.convert.ConvertFromNif;

/**
 * NOTE no transformGroups can be used under a character, hence the madness below Attachments can be faces or weapons,
 * so some are removable, some are not
 * 
 * @author philip
 *
 */
public class CharacterAttachment extends BranchGroup implements GeometryUpdater, Fadable
{
	private J3dNiNode attachBone;

	private J3dNiAVObject model;

	private ArrayList<J3dNiTriBasedGeom> geoms = new ArrayList<J3dNiTriBasedGeom>();

	private HashMap<GeometryArray, J3dNiTriBasedGeom> arrayToGeomMap = new HashMap<GeometryArray, J3dNiTriBasedGeom>();

	private HashMap<GeometryArray, Transform3D> transformMap = new HashMap<GeometryArray, Transform3D>();

	// TODO: this doesn't have to be a branchgroup if it a dog face is stuck on permanently,
	// possibly make skins separate from gear that can be removed?

	// TODO:  I see animations going the wrong way ? still

	// I see from oblivion that it appears to be the same REFR and the same attachment that
	// ends up on the ground everytime, odd. Not running heaps of animations appears to have decreased it hugely

	public CharacterAttachment(J3dNiNode attachBone, boolean headAttachRotNeeded, J3dNiAVObject model)
	{
		this(attachBone, headAttachRotNeeded, model, false);
	}

	// for TES3 if attachment is from "inside" the skin file ignore parents (which are actually the bone)
	public CharacterAttachment(J3dNiNode attachBone, J3dNiAVObject model, boolean noParents)
	{
		this(attachBone, false, model, noParents);
	}

	private CharacterAttachment(J3dNiNode attachBone, boolean headAttachRotNeeded, J3dNiAVObject model, boolean noParents)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.attachBone = attachBone;
		this.model = model;

		getAndAttachAllGeom(model, headAttachRotNeeded, noParents);
	}

	private void getAndAttachAllGeom(J3dNiAVObject node, boolean headAttachRotNeeded, boolean noParents)
	{
		if (node instanceof J3dNiNode)
		{
			J3dNiNode j3dNiNode = (J3dNiNode) node;
			for (int i = 0; i < j3dNiNode.numChildren(); i++)
			{
				Node n = j3dNiNode.getChild(i);
				if (n instanceof J3dNiAVObject)
					getAndAttachAllGeom((J3dNiAVObject) n, headAttachRotNeeded, noParents);
			}
		}
		else if (node instanceof J3dNiTriBasedGeom)
		{
			J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom) node;

			// needs to be morphable
			j3dNiTriBasedGeom.makeMorphable();
			GeometryArray geoArray = j3dNiTriBasedGeom.getCurrentGeometryArray();

			// record the geom for use in the updateDAta call
			arrayToGeomMap.put(geoArray, j3dNiTriBasedGeom);

			// make a transform up to the root
			Transform3D trans = new Transform3D();
			Transform3D temp1 = new Transform3D();
			NiAVObject niAVObject = j3dNiTriBasedGeom.getNiAVObject();
			while (niAVObject != null)
			{
				if (!J3dNiAVObject.ignoreTopTransformRot(niAVObject))
				{
					if (headAttachRotNeeded)
					{
						Transform3D upright = new Transform3D();
						upright.rotZ(-Math.PI / 4f);
						Quat4f up = new Quat4f();
						upright.get(up);
						temp1.setRotation(up);
					}
					else
					{
						temp1.setRotation(ConvertFromNif.toJ3d(niAVObject.rotation));
					}
				}
				else
				{
					temp1.setRotation(new Quat4f(0, 0, 0, 1));
				}
				temp1.setTranslation(ConvertFromNif.toJ3d(niAVObject.translation));
				temp1.setScale(niAVObject.scale);

				trans.mul(temp1, trans);

				if (noParents)
					break;

				niAVObject = niAVObject.parent;
			}

			// blank the j3dNiTriBasedGeom transform as this is now embedded above
			j3dNiTriBasedGeom.getTransformGroup().setTransform(new Transform3D());

			// record teh transform for use in updateData
			transformMap.put(geoArray, trans);

			// ensure detached
			if (j3dNiTriBasedGeom.topOfParent != null)
				j3dNiTriBasedGeom.topOfParent.removeChild(j3dNiTriBasedGeom);

			// TODO: see Meshes\Weapons\Hand2Hand\PowerFistRigid.NIF for interesting setup that topofparent misses
			if (j3dNiTriBasedGeom.getParent() != null)
				((Group) j3dNiTriBasedGeom.getParent()).removeChild(j3dNiTriBasedGeom);

			// add it to scene
			addChild(j3dNiTriBasedGeom);
			geoms.add(j3dNiTriBasedGeom);
		}

	}

	private Transform3D skeletonBoneVWTrans = new Transform3D();

	public void process()
	{
		skeletonBoneVWTrans.set(attachBone.getBoneCurrentAccumedTrans());

		for (J3dNiTriBasedGeom geom : geoms)
		{
			geom.getCurrentGeometryArray().updateData(this);
		}
	}

	@Override
	public void fade(float percent)
	{
		if (model instanceof Fadable)
			((Fadable) model).fade(percent);
	}

	@Override
	public void setOutline(Color3f c)
	{

		if (model instanceof Fadable)
			((Fadable) model).setOutline(c);
	}

	// deburner
	private Transform3D temp = new Transform3D();

	@Override
	public void updateData(Geometry geometry)
	{
		// add the bone trans into the root
		Transform3D trans = transformMap.get(geometry);
		temp.mul(skeletonBoneVWTrans, trans);

		// holder of the transform data to speed up transform (possibly)
		double[] accTransMat = new double[16];
		// get accumulatorTrans out to a stright float [] to speed up transform (possibly)
		temp.get(accTransMat);

		// now to incorporate the geomorphs changes (if any)

		FloatBuffer srcVs = null;
		J3dNiTriBasedGeom j3dNiTriBasedGeom = arrayToGeomMap.get(geometry);
		// if reset current will hold the verts reset to base with the geomorphs changes
		J3dNiGeomMorpherController geoMorph = j3dNiTriBasedGeom.getJ3dNiGeomMorpherController();
		if (geoMorph != null && geoMorph.isVertsResetOffBase())
		{
			srcVs = (FloatBuffer) j3dNiTriBasedGeom.getCurrentGeometryArray().getCoordRefBuffer().getBuffer();
			// recall that this has now screwed with them and will look to see if geo reset again
			geoMorph.setVertsResetOffBase(false);
		}
		else
		{
			// need to start from base as no geommorph has done a reset for us
			srcVs = (FloatBuffer) j3dNiTriBasedGeom.getBaseGeometryArray().getCoordRefBuffer().getBuffer();
		}

		FloatBuffer vs = (FloatBuffer) ((GeometryArray) geometry).getCoordRefBuffer().getBuffer();

		for (int vIdx = 0; vIdx < vs.limit() / 3; vIdx++)
		{
			float px = srcVs.get(vIdx * 3 + 0);
			float py = srcVs.get(vIdx * 3 + 1);
			float pz = srcVs.get(vIdx * 3 + 2);

			// transform point by using code from Transform3D.transform(Point3f) to speed up transform (possibly)
			float x = (float) (accTransMat[0] * px + accTransMat[1] * py + accTransMat[2] * pz + accTransMat[3]);
			float y = (float) (accTransMat[4] * px + accTransMat[5] * py + accTransMat[6] * pz + accTransMat[7]);
			pz = (float) (accTransMat[8] * px + accTransMat[9] * py + accTransMat[10] * pz + accTransMat[11]);
			px = x;
			py = y;

			vs.put(vIdx * 3 + 0, px);
			vs.put(vIdx * 3 + 1, py);
			vs.put(vIdx * 3 + 2, pz);
		}
	}
}
