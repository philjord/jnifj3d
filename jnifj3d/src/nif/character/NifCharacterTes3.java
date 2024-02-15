package nif.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.jogamp.java3d.Alpha;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Transform3D;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.character.AttachedParts.Part;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiSkinInstance;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.animation.SequenceAlpha;
import nif.j3d.animation.tes3.J3dNiControllerSequenceTes3;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import nif.niobject.NiGeometry;
import utils.source.MediaSources;

public class NifCharacterTes3 extends NifCharacter
{
	private J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper;

	private ArrayList<J3dNiGeomMorpherController> allMorphs = new ArrayList<J3dNiGeomMorpherController>();

	protected HashMap<Part, CharacterAttachment> attachmentByPart = new HashMap<Part, CharacterAttachment>();

	public NifCharacterTes3(String skeletonNifFilename, AttachedParts attachedSkinsAndParts, MediaSources mediaSources)
	{
		super(skeletonNifFilename, mediaSources);

		if (attachedSkinsAndParts != null)
		{
			for (Part part : attachedSkinsAndParts.parts.keySet())
			{
				String nifFileName = attachedSkinsAndParts.parts.get(part);
				if (nifFileName != null && nifFileName.length() > 0)
				{
					
					// TODO: odd extra transform in the keyframe data must be merged
					if (nifFileName.equals("c\\c_m_shirt_expens_3_ua.nif"))
						nifFileName = "c\\c_m_shirt_expensive_2_ua.nif";
					
					if (nifFileName.equals("a\\a_m_chitin_gauntlet.nif"))
						nifFileName = "a\\a_m_chitin_forearm.nif";

					
					NifJ3dVisRoot model = NifToJ3d.loadShapes(nifFileName, mediaSources.getMeshSource(), mediaSources.getTextureSource());

					if (model != null)
					{
						// create skins from the skeleton and skin nif
						ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(model.getNiToJ3dData(),
								blendedSkeletons.getOutputSkeleton());

						if (skins.size() > 0)
						{

							if (nifFileName.contains("skins"))
							{
								trimSkinsToPart(part, skins);
							}

							// add the skins to the scene
							for (J3dNiSkinInstance j3dNiSkinInstance : skins)
							{
								root.addChild(j3dNiSkinInstance);
							}

							allSkins.addAll(skins);

							// TODO: check this for regular characters (the parent class)
							// right now lot's of spikes and particles effect also exist in this model potentially

							for (J3dNiAVObject j3dNiAVObject : model.getNiToJ3dData().j3dNiAVObjectValues())
							{
								if (j3dNiAVObject instanceof J3dNiGeometry)
								{
									J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) j3dNiAVObject;
									NiGeometry niGeometry = (NiGeometry) j3dNiGeometry.getNiAVObject();
									if (niGeometry.skin.ref == -1)
									{
										String attachNodeName = niGeometry.parent.name;

										J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton()
												.getByName(attachNodeName);
										if (attachnode == null)
										{
											attachnode = blendedSkeletons.getOutputSkeleton().getSkeletonRoot();

										}
										
										//FIXME: look at frost atronach, particles are not listening to the
										// skeleton, but just to the skin nif bones
										//J3dNiParticleEmitter needs to be handed the bones and reset it's 
										// emitter listening system, but not the CharacterAttachment system :(
										
										CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, j3dNiGeometry, true,
												false);
										this.addChild(ca);
										attachments.add(ca);
										// no parts involved here

									}
								}
							}

						}
						else
						{
							//For TES3: add any unskinned trishapes in the skin file onto the bones
							//these will not be done by the super because the following is not true
							//NiStringExtraData nsed = (NiStringExtraData) ned;
							//if (nsed.name.equalsIgnoreCase("PRN"))

							String attachNodeName = part.getNode();

							J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton()
									.getByName(attachNodeName);
							if (attachnode != null)
							{
								 
								CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, model.getVisualRoot(), true,
										AttachedParts.isLeftSide(part.getLoc()));
								ca.setCapability(BranchGroup.ALLOW_DETACH);
								this.addChild(ca);
								attachments.add(ca);
								attachmentByPart.put(part, ca);
							}
							else
							{
								System.err.println("attach node not found ? " + attachNodeName + " in "
										+ model.getVisualRoot().getNiAVObject().nVer.fileName);
							}
						}

						// head attachment can definitely have a morph in it 
						for (J3dNiAVObject j3dNiAVObject : model.getNiToJ3dData().j3dNiAVObjectValues())
						{
							J3dNiGeomMorpherController j3dNiGeomMorpherController = j3dNiAVObject.getJ3dNiGeomMorpherController();
							if (j3dNiGeomMorpherController != null)
							{
								allMorphs.add(j3dNiGeomMorpherController);
							}
						}

					}
					else
					{
						System.err.println("Bad model name in NifCharacterTes3 " + nifFileName);
					}

				}
			}
		}

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
				{
					//idle1-9
					if (fireName.toLowerCase().startsWith("idle") && fireName.length() <= 5)
						idleAnimations.add(fireName);
				}

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

			// remove the old one
			if (currentKfBg != null)
			{
				currentKfBg.detach();
			}

			//System.out.println("currentAnimation "+currentAnimation);
			currentControllerSequence = j3dNiSequenceStreamHelper.getSequence(currentAnimation);
			//System.out.println("currentControllerSequence "+currentControllerSequence);
			if (currentControllerSequence != null)
			{

				// now add the root to the scene so the controller sequence is live
				BranchGroup newKfBg = ((J3dNiControllerSequenceTes3) currentControllerSequence).getBranchGroup();

				// in case it is already attached
				newKfBg.detach();

				// add it on
				addChild(newKfBg);

				currentControllerSequence.addSequenceListener(new SequenceSoundListener());
				currentControllerSequence.fireSequence(!returnToIdleWhenDone, 0);

				// assign currents
				currentKfBg = newKfBg;
			}
			else
			{
				System.out.println("bad animation " + currentAnimation);
			}
		}
		else if (returnToIdleWhenDone && //
				idleAnimations != null && //
				idleAnimations.size() > 0 && //
				(currentControllerSequence == null || //
						(currentControllerSequence.isNotRunning()) || //
						System.currentTimeMillis() - prevAnimTime > 10000))
		{
			int r = (int) (Math.random() * idleAnimations.size());
			r = r == idleAnimations.size() ? 0 : r;
			nextAnimation = idleAnimations.get(r);
			if (nextAnimation.length() > 0)
				updateAnimation();

			prevAnimTime = System.currentTimeMillis();
		}

		if (!noIdleMorphs && System.currentTimeMillis() - prevMorphTime > nextFireTime)
		{
			float maxLength = 0;
			if (allMorphs != null)
			{
				for (J3dNiGeomMorpherController j3dNiGeomMorpherController : getAllMorphs())
				{
					String[] morphsFrames = j3dNiGeomMorpherController.getAllMorphFrameNames();
					int r2 = (int) (Math.random() * morphsFrames.length);
					r2 = r2 == morphsFrames.length ? 0 : r2;
					String frame = morphsFrames[r2];
					j3dNiGeomMorpherController.fireFrameName(frame, false);

					if (maxLength < j3dNiGeomMorpherController.getLength())
						maxLength = j3dNiGeomMorpherController.getLength();
				}
			}

			prevMorphTime = System.currentTimeMillis();
			nextFireTime = (maxLength * 1000) + (new Random().nextFloat() * 3000f);

		}

	}

	public void removePart(Part part)
	{
		//TODO: attachmentByPArt allows one thing per part, is that right?
		CharacterAttachment ca = attachmentByPart.get(part);
		if (ca != null)
		{
			this.removeChild(ca);
			attachments.remove(ca);
			attachmentByPart.remove(part);
		}
	}

	public void addPart(Part part, String nifFileName)
	{
		//TODO: no skins no morphs for now
		if (nifFileName != null && nifFileName.length() > 0)
		{
			NifJ3dVisRoot model = NifToJ3d.loadShapes(nifFileName, mediaSources.getMeshSource(), mediaSources.getTextureSource());

			if (model != null)
			{
				String attachNodeName = part.getNode();

				J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton().getByName(attachNodeName);
				if (attachnode != null)
				{
					CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, model.getVisualRoot(), true,
							AttachedParts.isLeftSide(part.getLoc()));
					ca.setCapability(BranchGroup.ALLOW_DETACH);
					this.addChild(ca);
					attachments.add(ca);
					attachmentByPart.put(part, ca);
				}
				else
				{
					System.err.println(
							"attach node not found ? " + attachNodeName + " in " + model.getVisualRoot().getNiAVObject().nVer.fileName);
				}
			}
		}
	}

	protected long prevMorphTime = 0;

	protected float nextFireTime = 0;

	private boolean noIdleMorphs = false;

	/**
	 * Note this trims chest, hand, feet and tail if beast
	 * @param part
	 * @param skins
	 */
	private static void trimSkinsToPart(Part part, ArrayList<J3dNiSkinInstance> skins)
	{
		for (int i = 0; i < skins.size(); i++)
		{
			J3dNiSkinInstance j3dNiSkinInstance = skins.get(i);

			// all skins nodes are of a form "Tri Left Hand 0" so the node name should appear
			if (!j3dNiSkinInstance.getJ3dNiTriShape().getName().contains(part.getNode()))
			{
				skins.remove(i);
				i--;
			}
		}
	}

	public ArrayList<J3dNiGeomMorpherController> getAllMorphs()
	{
		return allMorphs;
	}

	public void setNoMorphs()
	{
		noIdleMorphs = true;
	}

}
