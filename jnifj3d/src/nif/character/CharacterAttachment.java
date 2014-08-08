package nif.character;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import tools3d.utils.scenegraph.EasyTransformGroup;

/**
 * Attachments can be faces or weapons, so some or removable, some are not
 * @author philip
 *
 */
public class CharacterAttachment extends BranchGroup
{
	private EasyTransformGroup attachmentTrans = new EasyTransformGroup();//for bone trans

	private J3dNiNode attachBone;

	//TODO:  crabs and horse both not upright properly, horse skeleton has a rot above the non accum
	// I see ogre idle kf has a non accum rotation but mudcab doesn't?
	// am I including that
	// I also get teh delayed jiggle 

	public CharacterAttachment(J3dNiNode attachBone, String skeletonNifFilename, J3dNiAVObject model)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.attachBone = attachBone;
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
		}

	}

	//deburner
	private Transform3D skeletonBoneVWTrans = new Transform3D();

	public void process()
	{
		skeletonBoneVWTrans.set(attachBone.getBoneCurrentAccumedTrans());
		attachmentTrans.setTransform(skeletonBoneVWTrans);
	}

}
