package nif.character;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.MediaContainer;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.PointSound;
import org.jogamp.java3d.Sound;
import org.jogamp.java3d.SoundException;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.utils.shader.Cube;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point2f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;

import nif.ByteConvert;
import nif.NifFile;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiControllerSequence;
import nif.j3d.animation.J3dNiControllerSequence.SequenceListener;
import nif.j3d.animation.SequenceAlpha;
import nif.niobject.NiExtraData;
import nif.niobject.NiStringExtraData;
import nif.niobject.hkx.reader.HKXContents;
import nif.niobject.hkx.reader.HKXReader;
import nif.niobject.hkx.reader.InvalidPositionException;
import tools3d.audio.SimpleSounds;
import tools3d.utils.Utils3D;
import tools3d.utils.scenegraph.Fadable;
import tools3d.utils.scenegraph.VaryingLODBehaviour;
import utils.ESConfig;
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
 * Because of the above limitations all parts of the scenegraph below character must not use transformgroup but rework
 * the same change into a geometryupdate call
 * 
 * @author phil
 *
 */

// TODO: look into this for the fustum work
// The ViewInfo utility class (somewhere in com.sun.j3d.utils.universe)
// will give you view info that is up-to-date with respect to the current
// state of the scene graph.

public class NifCharacter extends BranchGroup implements Fadable
{
	public static boolean BULK_BUFFER_UPDATES = true;

	protected MediaSources mediaSources;

	protected ArrayList<J3dNiSkinInstance> allSkins = new ArrayList<J3dNiSkinInstance>();

	protected ArrayList<NifJ3dVisRoot> allOtherModels = new ArrayList<NifJ3dVisRoot>();

	protected String currentAnimation = "";

	protected String nextAnimation = "";

	protected boolean returnToIdleWhenDone = true; // if false this means just loop current

	protected List<String> idleAnimations;

	protected Group root = new Group();

	private KfJ3dRoot currentkfJ3dRoot;

	protected BlendedSkeletons blendedSkeletons;

	protected BranchGroup currentKfBg;

	private NifCharUpdateBehavior updateBehavior;

	protected ArrayList<CharacterAttachment> attachments = new ArrayList<CharacterAttachment>();

	protected J3dNiControllerSequence currentControllerSequence;

	//For use by Tes3 constructor
	protected NifCharacter(String skeletonNifFilename, MediaSources mediaSources)
	{
		this.mediaSources = mediaSources;

		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		// note node must be in scene graph
		updateBehavior = new NifCharUpdateBehavior(this, new float[] { 60f, 120f, 180f });
		addChild(updateBehavior);
		updateBehavior.setEnable(true);

		blendedSkeletons = new BlendedSkeletons(skeletonNifFilename, mediaSources.getMeshSource());

		addChild(root);

		if (NifJ3dSkeletonRoot.showBoneMarkers || J3dNiSkinInstance.showSkinBoneMarkers)
		{
			root.addChild(blendedSkeletons);
		}
	}

	public NifCharacter(String skeletonNifFilename, List<String> skinNifModelFilenames, MediaSources mediaSources,
			List<String> idleAnimations)
	{
		this(skeletonNifFilename, mediaSources);

		this.idleAnimations = idleAnimations;

		for (String skinNifModelFilename : skinNifModelFilenames)
		{
			if (skinNifModelFilename != null && skinNifModelFilename.length() > 0)
			{
				NifJ3dVisRoot model = NifToJ3d.loadShapes(skinNifModelFilename, mediaSources.getMeshSource(),
						mediaSources.getTextureSource());

				if (model != null)
				{
					// create skins from the skeleton and skin nif
					ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(model.getNiToJ3dData(),
							blendedSkeletons.getOutputSkeleton());
					
					
					//FIXME! it seems the loaded model is skinned to a 0,0,0 single point or somethign, because the test code below does attached a nif file to teh character, at the feet 
					
					//NifJ3dVisRoot model2 = NifToJ3d.loadShapes(ESConfig.TES_MESH_PATH + "actors\\character\\characterassets\\malebody.nif", 
					//		mediaSources.getMeshSource(),	mediaSources.getTextureSource());
					//root.addChild(model2.getVisualRoot());
					root.addChild(new Cube(0.1,0.1,0.1,1,1,1));
					
					
					if (skins.size() > 0)
					{
						// add the skins to the scene
						for (J3dNiSkinInstance j3dNiSkinInstance : skins)
						{
							root.addChild(j3dNiSkinInstance);
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
								if (nsed.name.equalsIgnoreCase("PRN") || nsed.name.indexOf("Prn") != -1)
								{
									String attachBoneName = nsed.stringData;
									J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton()
											.getByName(attachBoneName);
									if (attachnode != null)
									{

										boolean headAttachRotNeeded = false;
										if (attachnode.getNiAVObject().name.equals("Bip01 Head")
												&& skeletonNifFilename.contains("characters\\_male"))
										{
											headAttachRotNeeded = mediaSources.getMeshSource().nifFileExists(
													skinNifModelFilename.substring(0, skinNifModelFilename.length() - 3) + "egm");
										}

										// For Oblivion heads
										// head gear in oblivion has an egm file next to it
										// hair attached badly, imperial iron helmet (and steel) attached badly,
										// chainmail helmet attached good, chain mail has bone nodes and is nicely placed,
										// not really attachment style, just a regular body part
										// I notice helmet.egm file next to helmet.nif?? egm starts with FREGM002
										// F:\game media\Oblivion\meshes\armor\iron\m

										CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, headAttachRotNeeded,
												model.getVisualRoot());
										this.addChild(ca);
										attachments.add(ca);
										break;
									} else
									{
										System.out.println("Attch Bone not found " + attachBoneName);
									}
								}
							} 
						}
					}

				}
				else
				{
					System.err.println("Bad model name in NifCharacter " + skinNifModelFilename);
				}
			}
		}

		// set us up with the idle anim
		updateAnimation();
	}

	/**
	 * This keep the head pointing toward the rotation and the body upright
	 * 
	 * @param pitch
	 */
	public void setHeadPitch(double pitch)
	{
		Transform3D t = new Transform3D();
		t.rotX(pitch);

		// if (nifJ3dSkeletonRoot.getHeadJ3dNiNode() != null)
		{
			// TODO: some animations include head movement so this needs to add into or override
			// causes crazy head movements
			// nifJ3dSkeletonRoot.getHeadJ3dNiNode().getTransformGroup().getQuatRotTransformGroup().setTransform(t);
		}

	}

	/**
	 * Note no caching as the file load cache of niffile is the only step that can support it
	 */
	protected void updateAnimation()
	{
		if (nextAnimation.length() > 0) {		
			if(nextAnimation.endsWith(".kf")) {
				currentAnimation = nextAnimation;
				nextAnimation = "";
	
				// We need the nifFile.blocks and the KfJ3dRoot
				//KfJ3dRoot kfJ3dRoot = NifToJ3d.loadKf(currentAnimation, mediaSources.getMeshSource());
				
				KfJ3dRoot kfJ3dRoot = null;
				NifFile nifFile = NifToJ3d.loadNiObjects(currentAnimation, mediaSources.getMeshSource());
				if (nifFile != null) {
					kfJ3dRoot = NifToJ3d.extractKf(nifFile);
				
					if (kfJ3dRoot != null) {
						// just default to a 0.3 second blend?
						Alpha defaultAlpha = new SequenceAlpha(0, 0.3f, false);
						defaultAlpha.setStartTime(System.currentTimeMillis());
											
						
						NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
						NifJ3dSkeletonRoot inputSkeleton = blendedSkeletons.startNewInputAnimation(defaultAlpha);
						kfJ3dRoot.setAnimatedSkeleton(inputSkeleton.getAllBonesInSkeleton(), allOtherModels, niToJ3dData);
		
						// now add the root to the scene so the controller sequence is live
						BranchGroup newKfBg = new BranchGroup();
						newKfBg.setCapability(BranchGroup.ALLOW_DETACH);
						newKfBg.setCapability(Group.ALLOW_CHILDREN_WRITE);
		
						newKfBg.addChild(kfJ3dRoot);
						// add it on
						addChild(newKfBg);
						currentControllerSequence = kfJ3dRoot.getJ3dNiControllerSequence();
		
						currentControllerSequence.addSequenceListener(new SequenceSoundListener());
						currentControllerSequence.fireSequence(!returnToIdleWhenDone, 0);
		
						// remove the old one
						if (currentKfBg != null) {
							currentKfBg.detach();
						}
		
						// assign currents
						currentKfBg = newKfBg;
						currentkfJ3dRoot = kfJ3dRoot;
					}
				}
				else
				{
					System.out.println("kf file does not exist :) " + currentAnimation);
				}
	
			} else if(nextAnimation.endsWith(".hkx")) {
				
				currentAnimation = nextAnimation;
				nextAnimation = "";
				
	/*			ByteBuffer bb = mediaSources.getMeshSource().getByteBuffer(currentAnimation);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				
				HKXReader reader = new HKXReader(bb);
				try
				{
					HKXContents hkxContents = reader.read();
					
					//FIXME:!!!!
					//need to be able to read hkx file, which I can do somewhat with HKXReader...
					// umm? not surelet's have a bash at it shall we
				}
				catch (InvalidPositionException e)
				{
					e.printStackTrace();
				} catch (IOException e) {					
					e.printStackTrace();
				}
*/
			}
		}		
		else if (returnToIdleWhenDone && // 
				idleAnimations.size() > 0 && //
				(currentControllerSequence == null || //
						(currentControllerSequence.isNotRunning()) || //
						System.currentTimeMillis() - prevAnimTime > 10000)) {
			// The above measures time in idle and changes from once it's been 10 seconds in case of looping idle
			// otherwise drop back to idle if the current has finished
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
	 * 
	 * @param fileName
	 * @param returnToIdle
	 */
	public void addToQueue(String fileName, boolean returnToIdle)
	{
		//TODO: why not restart it? probably what is wanted?
		if (!fileName.equals(currentAnimation))
		{
			this.returnToIdleWhenDone = returnToIdle;
			nextAnimation = fileName;
		}
	}

	/**
	 * This will immediately cancel the current animation and start the new one
	 * Note the animation is laoded on this thread so this could be a slow call
	 * 
	 * 
	 * @param fileName
	 * @param returnToIdle
	 */
	public void startAnimation(String fileName, boolean returnToIdle)
	{
		//TODO: why not restart it? probably what is wanted?

		this.returnToIdleWhenDone = returnToIdle;
		nextAnimation = fileName;
		updateAnimation();
	}

	public String getCurrentAnimation()
	{
		return currentAnimation;
	}

	public J3dNiControllerSequence getCurrentControllerSequence()
	{
		return currentControllerSequence;
	}

	public NifJ3dSkeletonRoot getOutputSkeleton()
	{
		return blendedSkeletons.getOutputSkeleton();
	}

	public NifJ3dSkeletonRoot getInputSkeleton()
	{
		return blendedSkeletons.getInputSkeleton();
	}

	// TODO: this and the sound sequence listener below should be generic'ed
	protected void addObjectSound(PointSound sound, String soundKey, float edge)
	{
		// Create the media container to load the sound
		MediaContainer soundContainer = mediaSources.getSoundSource().getMediaContainer(soundKey);
		// Use the loaded data in the sound
		sound.setSoundData(soundContainer);
		sound.setInitialGain(1.0f);
		sound.setPosition(new Point3f(0, 0, 0));

		// Allow use to switch the sound on and off
		sound.setCapability(Sound.ALLOW_ENABLE_READ);
		sound.setCapability(Sound.ALLOW_ENABLE_WRITE);
		sound.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));

		// Set it to loop 1
		sound.setLoop(0);// Sound.INFINITE_LOOPS);
		// Use the edge value to set to extent of the sound
		Point2f[] attenuation = { new Point2f(0.0f, 1.0f), new Point2f(edge, 0.1f) };
		sound.setDistanceGain(attenuation);

		sound.setEnable(true);

		// Add the sound to the group
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
					// PointSound sound1 = new PointSound();
					// TODO: stop previous sound perhaps, walk sounds? use the animation end event
					// FIXME: fallout gets major bad wav formats
					// addObjectSound(sound1, params[0], 10.0f);

					//System.out.println("oh my god sound fired? take a look in NifCharacter! " + params[0]);

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

	@Override
	public void setOutline(Color3f c)
	{
		for (J3dNiSkinInstance j3dNiSkinInstance : allSkins)
		{
			j3dNiSkinInstance.setOutline(c);
		}

		for (CharacterAttachment ca : attachments)
		{
			ca.setOutline(c);
		}

	}

	/**
	 * start at the Sound\\ folder for media source
	 * @param soundFileName
	 */
	public void playSound(String soundFileName, int maximumAttenuationDistance, int loopCount)
	{

		//TODO: I need to detach and discard these sounds once played the loop count times
		if (soundFileName.endsWith("mp3"))
		{
			InputStream is = mediaSources.getSoundSource().getInputStream(soundFileName);
			BranchGroup soundBG = SimpleSounds.createPointSoundMp3(is, maximumAttenuationDistance, loopCount);
			if (soundBG != null)
				this.addChild(soundBG);
		}
		else
		{
			MediaContainer mc = mediaSources.getSoundSource().getMediaContainer(soundFileName);
			BranchGroup soundBG = SimpleSounds.createPointSound(mc, maximumAttenuationDistance, loopCount);
			this.addChild(soundBG);
		}
	}
	
	public void playBackgroundSound(String soundFileName, int loopCount, float gain)
	{
		if (soundFileName.endsWith("mp3"))
		{
			InputStream is = mediaSources.getSoundSource().getInputStream(soundFileName);
			SimpleSounds.playBackgroundSoundMp3(is, loopCount, gain);
		}
		else
		{
			MediaContainer mc = mediaSources.getSoundSource().getMediaContainer(soundFileName);
			SimpleSounds.playBackgroundSound(mc, loopCount, gain);
		}
	}
	

	

}
