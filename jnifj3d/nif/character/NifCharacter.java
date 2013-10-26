package nif.character;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.media.j3d.Alpha;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.MediaContainer;
import javax.media.j3d.PointSound;
import javax.media.j3d.Sound;
import javax.media.j3d.SoundException;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point2f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.J3dNiControllerSequence.SequenceListener;
import utils.source.MeshSource;
import utils.source.TextureSource;
import utils.source.file.FileSoundSource;

public class NifCharacter extends BranchGroup
{

	private MeshSource meshSource;

	private ArrayList<J3dNiSkinInstance> skins;

	private UpdateAnimationBehavior animationBehave = new UpdateAnimationBehavior();

	private String currentAnimation = "";

	private String nextAnimation = "";

	private boolean returnToIdleWhenDone = true;

	private String idleAnimation;

	private KfJ3dRoot currentkfJ3dRoot;

	private BlendedSkeletons blendedSkeletons;

	private BranchGroup currentKfBg;

	public NifCharacter(String skeletonNifFilename, String[] skinNifModelFilenames, MeshSource meshSource, TextureSource textureSource,
			String idleAnimation)
	{
		this.meshSource = meshSource;
		this.idleAnimation = idleAnimation;

		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		TransformGroup bg = new TransformGroup();

		// drop by 1 meter cos the nonaccum animations lift use up by 1 meter
		Vector3f dropDown = new Vector3f(0, -1f, 0);
		Transform3D t1 = new Transform3D();
		t1.set(dropDown);
		bg.setTransform(t1);

		blendedSkeletons = new BlendedSkeletons(skeletonNifFilename, meshSource);

		// for bone blending updates
		addChild(blendedSkeletons);

		for (String skinNifModelFilename : skinNifModelFilenames)
		{
			NifJ3dVisRoot skin = NifToJ3d.loadShapes(skinNifModelFilename, meshSource, textureSource, true);

			// create skins from the skeleton and skin nif
			skins = J3dNiSkinInstance.createSkins(skin.getNiToJ3dData(), blendedSkeletons.getOutputSkeleton());

			// add the skins to the scene
			for (J3dNiSkinInstance j3dNiSkinInstance : skins)
			{
				bg.addChild(j3dNiSkinInstance);
			}
		}

		//TODO: and add any non skin based gear like hats!!

		addChild(bg);
		animationBehave.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(animationBehave);

		//set us up with the idle anim
		nextAnimation = idleAnimation;
		updateAnimation();
	}

	/**
	 * This keep the head pointing toward the rotation and the body upright
	 * @param pitch
	 */
	public void setHeadPitch(double pitch)
	{
		Transform3D t = new Transform3D();
		t.rotX(pitch);

		//if (nifJ3dSkeletonRoot.getHeadJ3dNiNode() != null)
		{
			//TODO: some animations include head movement so this needs to add into or override
			//causes crazy head movements
			//nifJ3dSkeletonRoot.getHeadJ3dNiNode().getTransformGroup().getQuatRotTransformGroup().setTransform(t);
		}

	}

	//TODO: a better cahcing system here, kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton()); is expensive
	private HashMap<String, KfJ3dRoot> cachedAnimations = new HashMap<String, KfJ3dRoot>();

	private void updateAnimation()
	{
		if (nextAnimation.length() > 0)
		{
			currentAnimation = nextAnimation;
			nextAnimation = "";

			KfJ3dRoot kfJ3dRoot = null;
			kfJ3dRoot = cachedAnimations.get(currentAnimation);

			if (kfJ3dRoot == null)
			{
				kfJ3dRoot = NifToJ3d.loadKf(currentAnimation, meshSource);
				if (kfJ3dRoot != null)
				{
					// just default to a 0.3 second blend?
					Alpha defaultAlpha = new Alpha(1, 0, 0, 300, 0, 0);
					defaultAlpha.setStartTime(System.currentTimeMillis());

					NifJ3dSkeletonRoot inputSkeleton = blendedSkeletons.startNewInputAnimation(defaultAlpha);
					kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton());

					cachedAnimations.put(currentAnimation, kfJ3dRoot);

					// now add the root to the scene so the controller sequence is live
					BranchGroup newKfBg = new BranchGroup();
					newKfBg.setCapability(BranchGroup.ALLOW_DETACH);
					newKfBg.setCapability(Group.ALLOW_CHILDREN_WRITE);

					newKfBg.addChild(kfJ3dRoot);
					// add it on
					addChild(newKfBg);

					kfJ3dRoot.getJ3dNiControllerSequence().addSequenceListener(new SequenceSoundListener());
					kfJ3dRoot.getJ3dNiControllerSequence().fireSequence();

					// remove the old one
					if (currentKfBg != null)
					{
						currentKfBg.detach();
					}

					// assign currents
					currentKfBg = newKfBg;
					currentkfJ3dRoot = kfJ3dRoot;
				}
				else
				{
					System.out.println("kf file does not exist :) " + currentAnimation);
				}
			}
			else
			{

				// just default to a 0.3 second blend?
				Alpha defaultAlpha = new Alpha(1, 0, 0, 300, 0, 0);
				defaultAlpha.setStartTime(System.currentTimeMillis());

				blendedSkeletons.startNewInputAnimation(defaultAlpha);
				//kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton());

				// now add the root to the scene so the controller sequence is live
				BranchGroup newKfBg = new BranchGroup();
				newKfBg.setCapability(BranchGroup.ALLOW_DETACH);
				newKfBg.setCapability(Group.ALLOW_CHILDREN_WRITE);
				kfJ3dRoot.detach();
				newKfBg.addChild(kfJ3dRoot);
				// add it on
				addChild(newKfBg);

				kfJ3dRoot.getJ3dNiControllerSequence().addSequenceListener(new SequenceSoundListener());
				kfJ3dRoot.getJ3dNiControllerSequence().fireSequence();

				// remove the old one
				if (currentKfBg != null)
				{
					currentKfBg.detach();
				}

				// assign currents
				currentKfBg = newKfBg;
				currentkfJ3dRoot = kfJ3dRoot;
			}

		}

		//otherwise drop back to idle if the current has finished   
		if (idleAnimation.length() > 0
				&& (currentkfJ3dRoot == null || (currentkfJ3dRoot.getJ3dNiControllerSequence().isNotRunning() && returnToIdleWhenDone)))
		{
			nextAnimation = idleAnimation;
			updateAnimation();
		}

	}

	/**
	 * This only sets teh new animation if it is different from our current, otherwise ignore
	 * @param fileName
	 * @param returnToIdle
	 */
	public void startAnimation(String fileName, boolean returnToIdle)
	{
		if (!fileName.equals(currentAnimation))
		{
			this.returnToIdleWhenDone = returnToIdle;
			nextAnimation = fileName;
		}
	}

	//TODO: this and the sound sequence listener below should be generic'ed
	protected void addObjectSound(PointSound sound, String soundKey, float edge)
	{
		//Create the media container to load the sound
		MediaContainer soundContainer = new FileSoundSource().getMediaContainer(soundKey);
		//Use the loaded data in the sound
		sound.setSoundData(soundContainer);
		sound.setInitialGain(1.0f);
		sound.setPosition(new Point3f(0, 0, 0));

		//Allow use to switch the sound on and off
		sound.setCapability(Sound.ALLOW_ENABLE_READ);
		sound.setCapability(Sound.ALLOW_ENABLE_WRITE);
		sound.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		//Set it to loop 1
		sound.setLoop(0);//Sound.INFINITE_LOOPS);
		//Use the edge value to set to extent of the sound
		Point2f[] attenuation =
		{ new Point2f(0.0f, 1.0f), new Point2f(edge, 0.1f) };
		sound.setDistanceGain(attenuation);

		sound.setEnable(true);

		//Add the sound to the   group
		BranchGroup bg = new BranchGroup();
		bg.addChild(sound);
		this.addChild(bg);
	}

	class SequenceSoundListener implements SequenceListener
	{

		@Override
		public void sequenceEventFired(String key, String[] params, float time)
		{
			if (key.equalsIgnoreCase("Sound"))
			{
				try
				{
					PointSound sound1 = new PointSound();
					//TODO: stop previous sound perhaps, walk sounds? use teh animation end event
					addObjectSound(sound1, params[0], 10.0f);

				}
				catch (SoundException e)
				{
					e.printStackTrace();
				}
			}

		}
	}

	class UpdateAnimationBehavior extends Behavior
	{
		private WakeupOnElapsedFrames passiveWakeupCriterion = new WakeupOnElapsedFrames(10, true);

		public void initialize()
		{
			wakeupOn(passiveWakeupCriterion);
		}

		@SuppressWarnings(
		{ "unchecked", "rawtypes" })
		public void processStimulus(Enumeration critiria)
		{
			updateAnimation();
			wakeupOn(passiveWakeupCriterion);
		}

	}
}
