package nif.character;

import java.util.ArrayList;
import java.util.Random;

import javax.media.j3d.Alpha;
import javax.media.j3d.BranchGroup;

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
import nif.j3d.particles.tes3.J3dNiParticles;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import utils.source.MediaSources;

public class NifCharacterTes3 extends NifCharacter
{
	private J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper;

	private J3dNiControllerSequenceTes3 currentSequence;

	protected ArrayList<J3dNiGeomMorpherController> allMorphs = new ArrayList<J3dNiGeomMorpherController>();

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
					NifJ3dVisRoot model = NifToJ3d.loadShapes(nifFileName, mediaSources.getMeshSource(), mediaSources.getTextureSource());

					if (model != null)
					{						
						// create skins from the skeleton and skin nif
						ArrayList<J3dNiSkinInstance> skins = J3dNiSkinInstance.createSkins(model.getNiToJ3dData(),
								blendedSkeletons.getOutputSkeleton());

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
							
							//For TES3: add any unskinned trishapes in the skin file onto the bones
							//these will not be done by the super because the following is not true
							//NiStringExtraData nsed = (NiStringExtraData) ned;if (nsed.name.equalsIgnoreCase("PRN"))
							for (J3dNiAVObject j3dNiAVObject : model.getNiToJ3dData().j3dNiAVObjectValues())
							{
								// don't re attach particles as the anme is not right
								if (j3dNiAVObject instanceof J3dNiGeometry)
								{
									if (j3dNiAVObject instanceof J3dNiParticles)
									{
										//FIXME: possibly just leave these where ever they are?
										// or should they be a special character attachment
										// attached to root bone and able to be fired?
									}
									else
									{
										J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) j3dNiAVObject;
										NiGeometry niGeometry = (NiGeometry) j3dNiGeometry.getNiAVObject();
										if (niGeometry.skin.ref == -1)
										{
											String attachNodeName = niGeometry.name;
											NiNode parent = niGeometry.parent;
											if (parent != null)
											{
												attachNodeName = parent.name;
											}
											attachNodeName = part.getNode();

											J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton()
													.get(attachNodeName);
											if (attachnode != null)
											{
												CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, j3dNiGeometry, true,
														AttachedParts.isLeftSide(part.getLoc()));
												this.addChild(ca);
												attachments.add(ca);
											}
											else
											{
												System.err.println("attach node not found ? " + attachNodeName + " in "
														+ j3dNiGeometry.getNiAVObject().nVer.fileName);
											}
										}
										else
										{
											System.err.println("How did a skin ref get into the attachment system? " + nifFileName);
										}
									}
								}
							}
							
							
							// add any non skin based gear from other files like hats!!
							//TODO: do I need to? allOtherModels.add(model);

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

			// remove the old one
			if (currentKfBg != null)
			{
				currentKfBg.detach();
			}

			currentSequence = j3dNiSequenceStreamHelper.getSequence(currentAnimation);

			// now add the root to the scene so the controller sequence is live
			BranchGroup newKfBg = currentSequence.getBranchGroup();

			// add it on
			addChild(newKfBg);

			currentSequence.addSequenceListener(new SequenceSoundListener());
			currentSequence.fireSequence();

			// assign currents
			currentKfBg = newKfBg;
		}
		else if (idleAnimations != null && //
				idleAnimations.size() > 0 && //
				(currentSequence == null || //
						(currentSequence.isNotRunning() && returnToIdleWhenDone) || //
						System.currentTimeMillis() - prevAnimTime > 10000))
		{
			int r = (int) (Math.random() * idleAnimations.size());
			r = r == idleAnimations.size() ? 0 : r;
			nextAnimation = idleAnimations.get(r);
			if (nextAnimation.length() > 0)
				updateAnimation();

			prevAnimTime = System.currentTimeMillis();
		}

		if (System.currentTimeMillis() - prevMorphTime > nextFireTime)
		{
			float maxLength = 0;
			if (allMorphs != null)
			{
				for (J3dNiGeomMorpherController j3dNiGeomMorpherController : allMorphs)
				{
					String[] morphsFrames = j3dNiGeomMorpherController.getAllMorphFrameNames();
					int r2 = (int) (Math.random() * morphsFrames.length);
					r2 = r2 == morphsFrames.length ? 0 : r2;
					String frame = morphsFrames[r2];
					j3dNiGeomMorpherController.fireFrameName(frame);

					if (maxLength < j3dNiGeomMorpherController.getLength())
						maxLength = j3dNiGeomMorpherController.getLength();
				}
			}

			prevMorphTime = System.currentTimeMillis();
			nextFireTime = (maxLength * 1000) + (new Random().nextFloat() * 3000f);

		}

	}

	protected long prevMorphTime = 0;

	protected float nextFireTime = 0;

}
