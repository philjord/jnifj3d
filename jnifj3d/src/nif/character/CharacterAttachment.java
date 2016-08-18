package nif.character;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiNode;
import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import tools3d.utils.scenegraph.Fadable;

/**
 * New note note below old, if I double buffer all coord updates then update geoms should arrive at teh
 * same frame as transform updates and world is happy again
 * NOTE no transformGroups can be used under a character, hence the madness below Attachments can be faces or weapons,
 * so some are removable, some are not
 * 
 * @author philip
 *
 */
public class CharacterAttachment extends BranchGroup implements Fadable
{
	private J3dNiNode attachBone;

	private J3dNiAVObject model;

	private TransformGroup attachmentTrans = new TransformGroup();//for bone trans

	// TODO: this doesn't have to be a branchgroup if it a dog face is stuck on permanently,
	// possibly make skins separate from gear that can be removed?

	// TODO:  I see animations going the wrong way ? still

	// I see from oblivion that it appears to be the same REFR and the same attachment that
	// ends up on the ground everytime, odd. Not running heaps of animations appears to have decreased it hugely

	public CharacterAttachment(J3dNiNode attachBone, boolean headAttachRotNeeded, J3dNiAVObject model)
	{
		this(attachBone, headAttachRotNeeded, model, false, false);
	}

	// for TES3 if attachment is from "inside" the skin file ignore parents (which are actually the bone)
	public CharacterAttachment(J3dNiNode attachBone, J3dNiAVObject model, boolean noParents, boolean leftSide)
	{
		this(attachBone, false, model, noParents, leftSide);
	}

	private CharacterAttachment(J3dNiNode attachBone, boolean headAttachRotNeeded, J3dNiAVObject model, boolean noParents, boolean leftSide)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.attachBone = attachBone;
		this.model = model;
		attachmentTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.addChild(attachmentTrans);

		// ensure detached
		if (model.topOfParent != null)
			model.topOfParent.removeChild(model);

		// TODO: see Meshes\Weapons\Hand2Hand\PowerFistRigid.NIF for interesting setup that topofparent misses
		if (model.getParent() != null)
			((Group) model.getParent()).removeChild(model);

		Transform3D trans = new Transform3D();
		NiAVObject niAVObject = model.getNiAVObject();
		if (!J3dNiAVObject.ignoreTopTransformRot(niAVObject))
		{
			if (headAttachRotNeeded)
			{
				Transform3D upright = new Transform3D();
				upright.rotZ(-Math.PI / 2f);
				Quat4f up = new Quat4f();
				upright.get(up);
				trans.setRotation(up);
			}
			else
			{
				//OK morrowind Neck's don't want any rotations? all seems well without this
				//trans.setRotation(ConvertFromNif.toJ3d(niAVObject.rotation));

				//TODO: but oblivion helmets screwed see toddland
			}
		}

		// apparently negative scaling is how you mirror, with thanks to Brandano on #niftools IRC
		if (leftSide)
		{
			//- scale does the mirror job, and all looks good now?
			trans.setScale(new Vector3d(-niAVObject.scale, niAVObject.scale, niAVObject.scale));

			reverseWindings(model);
		}
		else
		{
			trans.setScale(niAVObject.scale);
		}

		//FIXME: TES3 fix up  
		//TODO: what so crazy key frame single translation madness
		//c_m_shirt_expens_3_ua has odd extra transform

		TransformGroup tg2 = new TransformGroup();
		tg2.setTransform(trans);
		tg2.addChild(model);
		attachmentTrans.addChild(tg2);
	}

	private void reverseWindings(J3dNiAVObject model)
	{

		//Tri winding will be backwards now so flip faces
		if (model instanceof J3dNiNode)
		{
			J3dNiNode j3dNiNode = ((J3dNiNode) model);
			for (int i = 0; i < j3dNiNode.numChildren(); i++)
			{
				Node child = j3dNiNode.getChild(i);
				if (child instanceof J3dNiAVObject)
				{
					reverseWindings((J3dNiAVObject) child);
				}
			}
		}
		else if (model instanceof J3dNiGeometry)
		{
			J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) model;
			PolygonAttributes pa = j3dNiGeometry.getShape().getAppearance().getPolygonAttributes();
			if (pa == null)
			{
				pa = new PolygonAttributes();
				j3dNiGeometry.getShape().getAppearance().setPolygonAttributes(pa);
			}

			pa.setBackFaceNormalFlip(true);
			pa.setCullFace(PolygonAttributes.CULL_FRONT);
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

	private Transform3D skeletonBoneVWTrans = new Transform3D();

	public void process()
	{
		skeletonBoneVWTrans.set(attachBone.getBoneCurrentAccumedTrans());
		attachmentTrans.setTransform(skeletonBoneVWTrans);

		//Notice teh referred to attachedment is ALSO a geomorph!!
		// so geomorph should place it etc

		//TODO: geommorphs aren't running any more

	}

}
