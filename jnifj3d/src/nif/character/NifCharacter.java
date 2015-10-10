package nif.character;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.MediaContainer;
import javax.media.j3d.Node;
import javax.media.j3d.PointSound;
import javax.media.j3d.Sound;
import javax.media.j3d.SoundException;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.J3dNiControllerSequence.SequenceListener;
import nif.j3d.animation.SequenceAlpha;
import nif.niobject.NiExtraData;
import nif.niobject.NiStringExtraData;
import tools3d.utils.Utils3D;
import tools3d.utils.scenegraph.Fadable;
import tools3d.utils.scenegraph.VaryingLODBehaviour;
import utils.source.MediaSources;

/**
 * //https://www.mail-archive.com/java3d-interest@java.sun.com/msg23102.html
 * >There are a few latency issues in the Java 3D 1.3 architecture that could
>affect what you're doing:
>
>1) Updates to geometry and texture data have a 1-frame latency.
>2) Updates to transforms and scene graph structure have a 2-frame latency.
>3) Methods such as getImagePlateToVworld() in Canvas3D query the internal
>   representation of the Java 3D scene graph, which has the 2-frame latency
>   previously mentioned, so you can't use those methods directly to
>   synchronize view dependent scene graph updates.*/

/**
 * Because of teh above limitations all parts of teh scenegraph below character 
 * must not use transformgroup but rework the same change into a geometryupdate call
 * @author phil
 *
 */

//TODO: look into this for the fustum work
//The ViewInfo utility class (somewhere in com.sun.j3d.utils.universe)
//will give you view info that is up-to-date with respect to the current
//state of the scene graph.

public class NifCharacter extends BranchGroup implements Fadable
{
	private MediaSources mediaSources;

	private ArrayList<J3dNiSkinInstance> allSkins = new ArrayList<J3dNiSkinInstance>();

	protected ArrayList<NifJ3dVisRoot> allOtherModels = new ArrayList<NifJ3dVisRoot>();

	protected String currentAnimation = "";

	protected String nextAnimation = "";

	protected boolean returnToIdleWhenDone = true;

	protected List<String> idleAnimations;

	private KfJ3dRoot currentkfJ3dRoot;

	protected BlendedSkeletons blendedSkeletons;

	protected BranchGroup currentKfBg;

	private NifCharUpdateBehavior updateBehavior;

	
	protected ArrayList<CharacterAttachment> attachments = new ArrayList<CharacterAttachment>();

	public NifCharacter(String skeletonNifFilename, List<String> skinNifModelFilenames, MediaSources mediaSources,
			List<String> idleAnimations)
	{
		this.mediaSources = mediaSources;

		this.idleAnimations = idleAnimations;

		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		//note node must be in scene graph
		updateBehavior = new NifCharUpdateBehavior(this, new float[]
		{ 60f, 120f, 180f });
		addChild(updateBehavior);
		updateBehavior.setEnable(true);

		blendedSkeletons = new BlendedSkeletons(skeletonNifFilename, mediaSources.getMeshSource());

		Group bg = new Group();
		addChild(bg);

		if (NifJ3dSkeletonRoot.showBoneMarkers || J3dNiSkinInstance.showSkinBoneMarkers)
		{
			bg.addChild(blendedSkeletons);
		}

		for (String skinNifModelFilename : skinNifModelFilenames)
		{
			if (skinNifModelFilename != null && skinNifModelFilename.length() > 0)
			{
				NifJ3dVisRoot model = NifToJ3d.loadShapes(skinNifModelFilename, mediaSources.getMeshSource(),
						mediaSources.getTextureSource());

				// create skins from the skeleton and skin nif
				ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(model.getNiToJ3dData(),
						blendedSkeletons.getOutputSkeleton());

				if (skins.size() > 0)
				{
					// add the skins to the scene
					for (J3dNiSkinInstance j3dNiSkinInstance : skins)
					{
						bg.addChild(j3dNiSkinInstance);
					}

					allSkins.addAll(skins);
				}
				else
				{
					// add any non skin based gear from other files like hats!!	
					allOtherModels.add(model);

					// use an nistringextra of weapon and shield, node name of prn for extra data
					for (NiExtraData ned : model.getVisualRoot().getExtraDataList())
					{
						if (ned instanceof NiStringExtraData)
						{
							NiStringExtraData nsed = (NiStringExtraData) ned;
							if (nsed.name.equalsIgnoreCase("PRN"))
							{
								J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton()
										.get(nsed.stringData);
								if (attachnode != null)
								{
									CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, model.getVisualRoot());
									this.addChild(ca);
									attachments.add(ca);
									break;
								}
							}
						}
					}
				}

				
			}
		}

		//set us up with the idle anim
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

	/**
	 * Note no caching as the file load cache of niffile is the only step that can support it
	 */
	protected void updateAnimation()
	{
		if (nextAnimation.length() > 0)
		{
			currentAnimation = nextAnimation;
			nextAnimation = "";

			KfJ3dRoot kfJ3dRoot = NifToJ3d.loadKf(currentAnimation, mediaSources.getMeshSource());
			if (kfJ3dRoot != null)
			{
				// just default to a 0.3 second blend?
				Alpha defaultAlpha = new SequenceAlpha(0, 0.3f, false);
				defaultAlpha.setStartTime(System.currentTimeMillis());

				NifJ3dSkeletonRoot inputSkeleton = blendedSkeletons.startNewInputAnimation(defaultAlpha);
				kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton(), allOtherModels);

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
		else if (idleAnimations.size() > 0 && (currentkfJ3dRoot == null || //
				(currentkfJ3dRoot.getJ3dNiControllerSequence().isNotRunning() && returnToIdleWhenDone) || //
				System.currentTimeMillis() - prevAnimTime > 10000))
		{
			// The above measures time in idle and changes from once it's been 10 seconds in case of looping idle
			//otherwise drop back to idle if the current has finished 
			int r = (int) (Math.random() * idleAnimations.size() - 1);
			nextAnimation = idleAnimations.get(r);
			if (nextAnimation.length() > 0)
				updateAnimation();

			prevAnimTime = System.currentTimeMillis();

		}

	}

	protected long prevAnimTime = 0;

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
		MediaContainer soundContainer = mediaSources.getSoundSource().getMediaContainer(soundKey);
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
					//PointSound sound1 = new PointSound();
					//TODO: stop previous sound perhaps, walk sounds? use teh animation end event
					//FIXME: fallout gets major bad wav formats
					//addObjectSound(sound1, params[0], 10.0f);

				}
				catch (SoundException e)
				{
					e.printStackTrace();
				}
			}

		}
	}

	class NifCharUpdateBehavior extends VaryingLODBehaviour
	{
		public NifCharUpdateBehavior(Node node, float[] dists)
		{
			super(node, dists, true, true);
			setSchedulingBounds(Utils3D.defaultBounds);
		}

		@Override
		public void initialize()
		{
			super.initialize();
		}

		@Override
		public void process()
		{
			updateAnimation();
			blendedSkeletons.updateOutputBones();

			for (J3dNiSkinInstance j3dNiSkinInstance : allSkins)
			{
				j3dNiSkinInstance.processSkinInstance();
			}

			for (CharacterAttachment ca : attachments)
			{
				ca.process();
			}

		}

	}

	@Override
	public void fade(float percent)
	{
		for (J3dNiSkinInstance j3dNiSkinInstance : allSkins)
		{
			j3dNiSkinInstance.fade(percent);
		}

		for (CharacterAttachment ca : attachments)
		{
			ca.fade(percent);
		}

	}
}
