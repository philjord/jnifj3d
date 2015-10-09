package nif.character;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import tools3d.utils.scenegraph.EasyTransformGroup;
import tools3d.utils.scenegraph.Fadable;

/**
 * Attachments can be faces or weapons, so some are removable, some are not
 * @author philip
 *
 */
public class CharacterAttachment extends BranchGroup implements Fadable
{
	//TODO: this guy will always run late, as I get teh udate output bone then set the trans now
	// but the trans will only be reflacted in teh next frame??? whereas skin geo update 
	//will be shown in next frame?
	// not entirely sure in fact? why can't I attach directly to the bone anyway?
	private EasyTransformGroup attachmentTrans = new EasyTransformGroup();//for bone trans

	private J3dNiNode attachBone;

	private J3dNiAVObject model;

	//TODO:  crabs and horse both not upright properly, horse skeleton has a rot above the non accum
	// I see ogre idle kf has a non accum rotation but mudcab doesn't?
	// am I including that
	// I also get the delayed jiggle 
	
	//TODO: this doesn't ahve toe be a branchgroup if it a dog face it stuck on permanently,
	//posssiby make skins seperate from gear?
	
	// faces etc need to use update geometry just like the skins do, and not use any sort of transformg group at all

	public CharacterAttachment(J3dNiNode attachBone, String skeletonNifFilename, J3dNiAVObject model)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.attachBone = attachBone;
		this.model = model;
		this.addChild(attachmentTrans);

		// TODO: still not right I want heads of humans only!
		if (attachBone.getName().equalsIgnoreCase("Bip01 head") && skeletonNifFilename.indexOf("haracter") != -1)
		{
			EasyTransformGroup tg2 = new EasyTransformGroup();
			tg2.rotZ(-Math.PI / 2d);
			tg2.addChild(model);
			attachmentTrans.addChild(tg2);
		}
		else
		{
			attachmentTrans.addChild(model);
			//addChild(model);
			 
		}
		// can't attach to bone because why? bones not atached in scene graph?
		// but even when it is attached I get exactly the same issue
		// so simply putting a break point in process I see head is
		// always perfect to body, so my issue is that the transfrom does not get 
		//reflected until 2 frames after t is done, the transform update below is too slow??
	}

	//deburner
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	public void process()
	{
		skeletonBoneVWTrans.set(attachBone.getBoneCurrentAccumedTrans());
		attachmentTrans.setTransform(skeletonBoneVWTrans);
		
		//Notice teh referred to attachedment is ALSO a geomorph!!
		// so geomorph should place it etc
		
	}

	@Override
	public void fade(float percent)
	{
		if (model instanceof Fadable)
			((Fadable) model).fade(percent);
	}

}
