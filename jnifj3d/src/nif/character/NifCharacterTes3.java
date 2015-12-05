package nif.character;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.j3d.Alpha;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.IndexedTriangleStripArray;
import javax.media.j3d.PolygonAttributes;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.animation.J3dNiGeomMorpherController;
import nif.j3d.animation.SequenceAlpha;
import nif.j3d.animation.tes3.J3dNiControllerSequenceTes3;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import nif.niobject.NiGeometry;
import nif.niobject.NiNode;
import utils.source.MediaSources;

public class NifCharacterTes3 extends NifCharacter
{
	private J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper;

	private J3dNiControllerSequenceTes3 currentSequence;

	protected ArrayList<J3dNiGeomMorpherController> allMorphs = new ArrayList<J3dNiGeomMorpherController>();

	public NifCharacterTes3(String skeletonNifFilename, List<String> skinNifModelFilenames, MediaSources mediaSources)
	{
		super(skeletonNifFilename, skinNifModelFilenames, mediaSources, null);

		for (String skinNifModelFilename : skinNifModelFilenames)
		{
			if (skinNifModelFilename != null && skinNifModelFilename.length() > 0)
			{
				NifJ3dVisRoot model = NifToJ3d.loadShapes(skinNifModelFilename, mediaSources.getMeshSource(),
						mediaSources.getTextureSource());

				//TODO: add all morphs into a bunch for fun , but dump once kf it running geomorphs properly
				for (J3dNiAVObject j3dNiAVObject : model.getNiToJ3dData().j3dNiAVObjectValues())
				{
					J3dNiGeomMorpherController j3dNiGeomMorpherController = j3dNiAVObject.getJ3dNiGeomMorpherController();
					if (j3dNiGeomMorpherController != null)
					{
						allMorphs.add(j3dNiGeomMorpherController);
					}
				}

				boolean leftRequired = false;
				//For TES3: add any unskinned trishapes in the skin file onto the bones
				for (J3dNiAVObject j3dNiAVObject : model.getNiToJ3dData().j3dNiAVObjectValues())
				{
					if (j3dNiAVObject instanceof J3dNiGeometry)
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

							//map known values
							if (attachNodeName.contains("_Head"))
								attachNodeName = "Head";
							else if (attachNodeName.contains("_Hair"))
								attachNodeName = "Head";
							else if (attachNodeName.contains("_Neck"))
								attachNodeName = "Neck";
							else if (attachNodeName.contains("_Groin"))
								attachNodeName = "Groin";
							else if (attachNodeName.contains("_Ankle"))
								attachNodeName = "Right Ankle";
							else if (attachNodeName.contains("_Forearm"))
								attachNodeName = "Right Forearm";
							else if (attachNodeName.contains("_Foot"))
								attachNodeName = "Right Foot";
							else if (attachNodeName.contains("_Knee"))
								attachNodeName = "Right Knee";
							else if (attachNodeName.contains("_Upper Arm"))
								attachNodeName = "Right Upper Arm";
							else if (attachNodeName.contains("_Upper Leg"))
								attachNodeName = "Right Upper Leg";
							else if (attachNodeName.contains("_Wrist"))
								attachNodeName = "Right Wrist";

							J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton().get(attachNodeName);
							if (attachnode != null)
							{
								//TODO: this is possibly a bad idea?
								if (j3dNiAVObject.topOfParent != null)
									j3dNiAVObject.topOfParent.removeAllChildren();

								CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, j3dNiAVObject, true);
								this.addChild(ca);
								attachments.add(ca);
							}

							// please excuse crazy time now, both sides needed
							if (attachNodeName.contains("Right "))
							{
								leftRequired = true;
							}
						}
					}
				}

				if (leftRequired)
				{
					NifJ3dVisRoot model2 = NifToJ3d.loadShapes(skinNifModelFilename, mediaSources.getMeshSource(),
							mediaSources.getTextureSource());

					for (J3dNiAVObject j3dNiAVObject : model2.getNiToJ3dData().j3dNiAVObjectValues())
					{
						if (j3dNiAVObject instanceof J3dNiGeometry)
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

								else if (attachNodeName.contains("_Ankle"))
									attachNodeName = "Left Ankle";
								else if (attachNodeName.contains("_Forearm"))
									attachNodeName = "Left Forearm";
								else if (attachNodeName.contains("_Foot"))
									attachNodeName = "Left Foot";
								else if (attachNodeName.contains("_Knee"))
									attachNodeName = "Left Knee";
								else if (attachNodeName.contains("_Upper Arm"))
									attachNodeName = "Left Upper Arm";
								else if (attachNodeName.contains("_Upper Leg"))
									attachNodeName = "Left Upper Leg";
								else if (attachNodeName.contains("_Wrist"))
									attachNodeName = "Left Wrist";

								J3dNiAVObject attachnode = blendedSkeletons.getOutputSkeleton().getAllBonesInSkeleton().get(attachNodeName);
								if (attachnode != null)
								{
									//TODO: this is possibly a bad idea?
									if (j3dNiAVObject.topOfParent != null)
										j3dNiAVObject.topOfParent.removeAllChildren();

									CharacterAttachment ca = new CharacterAttachment((J3dNiNode) attachnode, j3dNiAVObject, true);
									this.addChild(ca);
									attachments.add(ca);
									//Note called after giving it to character attachment as this will make it morphable etc
									reverse(j3dNiGeometry);
								}
							}
						}

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

	private static void reverse(J3dNiGeometry j3dNiGeometry)
	{
		if (j3dNiGeometry instanceof J3dNiTriBasedGeom)
		{
			J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom) j3dNiGeometry;
			reverse(j3dNiTriBasedGeom, (IndexedGeometryArray) j3dNiTriBasedGeom.getBaseGeometryArray());
			reverse(j3dNiTriBasedGeom, (IndexedGeometryArray) j3dNiTriBasedGeom.getCurrentGeometryArray());
		}

	}

	private static void reverse(J3dNiTriBasedGeom j3dNiTriBasedGeom, IndexedGeometryArray ga)
	{
		if (ga != null)
		{
			if ((ga.getVertexFormat() & GeometryArray.BY_REFERENCE) != 0)
			{

				if ((ga.getVertexFormat() & GeometryArray.INTERLEAVED) != 0)
				{
					System.out.println("Unable to swap interleaved vertices for now");
					//ga.getInterleavedVertices();
				}
				else
				{
					float[] coords = ga.getCoordRefFloat();
					for (int v = 0; v < coords.length / 3; v++)
					{
						coords[v * 3 + 0] = -coords[v * 3 + 0];
					}
				}
			}
			else
			{
				float[] coords = new float[ga.getVertexCount() * 3];
				ga.getCoordinates(0, coords);
				for (int v = 0; v < coords.length / 3; v++)
				{
					coords[v * 3 + 0] = -coords[v * 3 + 0];
				}
				ga.setCoordinates(0, coords);
				j3dNiTriBasedGeom.getShape().setGeometry(ga);
			}

			//Tri winding will be backwards now so flip faces
			//Note can't touch the tri indexes as morpahbles still share them
			PolygonAttributes pa = j3dNiTriBasedGeom.getShape().getAppearance().getPolygonAttributes();
			pa.setBackFaceNormalFlip(true);
			pa.setCullFace(PolygonAttributes.CULL_FRONT);
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
