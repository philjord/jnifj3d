package nif.character;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
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
			System.out.println("niAVObject " + niAVObject.nVer.fileName + " " + headAttachRotNeeded);
			if (headAttachRotNeeded)
			{
				Transform3D upright = new Transform3D();
				upright.rotZ(-Math.PI / 4f);
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

			//Tri winding will be backwards now so flip faces
			if (model instanceof J3dNiGeometry)
			{
				J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) model;
				PolygonAttributes pa = j3dNiGeometry.getShape().getAppearance().getPolygonAttributes();
				pa.setBackFaceNormalFlip(true);
				pa.setCullFace(PolygonAttributes.CULL_FRONT);
			}
		}
		else
		{
			trans.setScale(niAVObject.scale);
		}

		TransformGroup tg2 = new TransformGroup();
		tg2.setTransform(trans);
		tg2.addChild(model);
		attachmentTrans.addChild(tg2);
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
