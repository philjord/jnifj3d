package nif.character;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.j3d.Alpha;
import javax.media.j3d.BranchGroup;

import nif.NifToJ3d;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.animation.SequenceAlpha;
import nif.j3d.animation.tes3.J3dNiControllerSequenceTes3;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import utils.source.MediaSources;

public class NifCharacterTes3 extends NifCharacter
{
	private J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper;

	private J3dNiControllerSequenceTes3 currentSequence;

	public NifCharacterTes3(String skeletonNifFilename, List<String> skinNifModelFilenames, MediaSources mediaSources)
	{
		super(skeletonNifFilename, skinNifModelFilenames, mediaSources, null);

		KfJ3dRoot kfJ3dRoot = null;
		if (skeletonNifFilename.toLowerCase().indexOf(".nif") != -1)
		{
			String kfName = skeletonNifFilename.substring(0, skeletonNifFilename.toLowerCase().indexOf(".nif")) + ".kf";
			kfJ3dRoot = NifToJ3d.loadKf(kfName, mediaSources.getMeshSource());
			if (kfJ3dRoot != null)
			{
				// just default to a 0.3 second blend?
				Alpha defaultAlpha = new SequenceAlpha(0, 0.3f, false);
				defaultAlpha.setStartTime(System.currentTimeMillis());

				NifJ3dSkeletonRoot inputSkeleton = blendedSkeletons.startNewInputAnimation(defaultAlpha);
				kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton(), allOtherModels);
				//TODO: this input skeleton needs to be reset on a new animation call, 
				//but setAnimatedSkeleton only happens once in J3dNiSequenceStreamHelper

				j3dNiSequenceStreamHelper = kfJ3dRoot.getJ3dNiSequenceStreamHelper();
				addChild(kfJ3dRoot);

				// now set the list of idle animations from the kf file 
				idleAnimations = new ArrayList<String>();
				for (String fireName : j3dNiSequenceStreamHelper.getAllSequences())
					idleAnimations.add(fireName);

				//set us up with the idle anim
				updateAnimation();
			}
			else
			{
				System.out.println("No TES3 kf file for " + skeletonNifFilename + " " + kfName);
			}
		}
		else
		{
			System.out.println("No TES3 kf file for " + skeletonNifFilename + " (missing .nif)");
		}

	}

	public J3dNiSequenceStreamHelper getJ3dNiSequenceStreamHelper()
	{
		return j3dNiSequenceStreamHelper;
	}

	/**
	 * Note no caching as the file load cache of niffile is the only step that can support it
	 */
	@Override
	protected void updateAnimation()
	{
		if (nextAnimation.length() > 0)
		{
			currentAnimation = nextAnimation;
			nextAnimation = "";

			// blending copies skeletons, but tes3 uses only 1!
			// just default to a 0.3 second blend?
			//Alpha defaultAlpha = new SequenceAlpha(0, 0.3f, false);
			//defaultAlpha.setStartTime(System.currentTimeMillis());

			//blendedSkeletons.startNewInputAnimation(defaultAlpha);

			currentSequence = j3dNiSequenceStreamHelper.getSequence(currentAnimation);

			// now add the root to the scene so the controller sequence is live
			BranchGroup newKfBg = currentSequence.getBranchGroup();

			// add it on
			addChild(newKfBg);

			currentSequence.addSequenceListener(new SequenceSoundListener());
			currentSequence.fireSequence();

			// remove the old one
			if (currentKfBg != null)
			{
				currentKfBg.detach();
			}

			// assign currents
			currentKfBg = newKfBg;
		}
		else if (idleAnimations != null && idleAnimations.size() > 0
				&& (currentSequence == null || (currentSequence.isNotRunning() && returnToIdleWhenDone)))
		{
			//TODO: I've a list of idle, measure time in idle and change from time to time
			//otherwise drop back to idle if the current has finished 
			int r = (int) (Math.random() * idleAnimations.size() - 1);
			nextAnimation = idleAnimations.get(r);
			if (nextAnimation.length() > 0)
				updateAnimation();
		}

		if (System.currentTimeMillis() - prevMorphTime > (3000 + nextFireTime))
		{
			for (J3dNiGeomMorpherController j3dNiGeomMorpherController : allMorphs)
			{
				String[] morphsFrames = j3dNiGeomMorpherController.getAllMorphFrameNames();
				int r2 = (int) (Math.random() * morphsFrames.length - 1);
				String frame = morphsFrames[r2];
				j3dNiGeomMorpherController.fireFrameName(frame);
			}
			prevMorphTime = System.currentTimeMillis();
			nextFireTime = new Random(prevMorphTime).nextFloat() * 7000f;
		}

	}
}
