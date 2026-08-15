package nif.j3d.animation.hkx;

import java.util.ArrayList;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.Bounds;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;
import org.jogamp.vecmath.Point3d;

import nif.NifJ3dVisRoot;
import nif.character.TextKeyExtraDataKey;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.J3dNiTextKeyExtraData;
import nif.j3d.animation.J3dNiControllerSequence.SequenceEventsBehavior;
import nif.j3d.animation.J3dNiControllerSequence.SequenceListener;
import nif.j3d.animation.SequenceAlpha;
import nif.j3d.animation.SequenceAlpha.SequenceAlphaListener;
import nif.j3d.animation.SequenceAlpha.SequenceInterface;
import nif.niobject.NiControllerSequence;
import nif.niobject.hkx.hkBaseObject;
import nif.niobject.hkx.animation.hkRootLevelContainer;
import nif.niobject.hkx.animation.hkRootLevelContainer.hkRootLevelContainerNamedVariant;
import nif.niobject.hkx.animation.hkaAnimationBinding;
import nif.niobject.hkx.animation.hkaAnimationContainer;
import nif.niobject.hkx.animation.hkaDefaultAnimatedReferenceFrame;
import nif.niobject.hkx.animation.hkaSkeleton;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.AnimationTracks;
import nif.niobject.hkx.animation.hkaSplineCompressedAnimation.TransformTrack;
import nif.niobject.hkx.reader.HKXContents;
import tools3d.utils.scenegraph.VaryingLODBehaviour;

/**
 * Based heavily on J3dNiControllerSequence, commonise
 * It might perhaps be better as based on J3dNiControllerManager, but kf files only have 1 sequence in them
 * 
 *  hkaAnimationContainer.animations has up to several hkaAnimationContainer.animations so perhaps that's interesting?
 */
public class J3dhkaAnimationContainer extends Group implements SequenceInterface, SequenceAlphaListener {
	private SequenceEventsBehavior		sequenceEventsbehave;

	private SequenceBehavior			sequenceBehavior		= new SequenceBehavior(this);

	private ArrayList<SequenceListener>	sequenceListeners		= new ArrayList<SequenceListener>();

	protected J3dTransformTrack[]		controlledBlocks;

	protected String					fireName;

	protected J3dNiTextKeyExtraData		j3dNiTextKeyExtraData;

	private SequenceAlpha				sequenceAlpha;

	private float						prevSquenceAlphaValue	= 0;

	protected long						lengthMS				= 0;

	protected float						startTimeS				= 0;

	protected float						stopTimeS				= 0;

	protected float						lengthS					= 0;

	private hkaAnimationContainer		hkaAnimationContainer;

	// notice a single spline pointer and a single binding pointer
	// multiple animations will go wonky
	hkaSplineCompressedAnimation		spline;
	private hkaAnimationBinding			binding;

	protected int						cycleType				= NiControllerSequence.CYCLE_CLAMP;

	public J3dhkaAnimationContainer(hkaAnimationContainer hkaAnimationContainer, HKXContents hkxContents) {
		this.hkaAnimationContainer = hkaAnimationContainer;
		sequenceEventsbehave = new SequenceEventsBehavior(this);

		/*		 
		 * 1 I'm getting J3dSkin updateGeometry causing some odd color buffer buffer underflow on some models
		 * this happens on second run of wisp and witchlight
		 * 2 I'm not yet seeing FO76 animating
		 * 3 I'm see skinning for FO4 is crazy madness
		 * 4 I haven't got float tracks being returned properly yet
		 * 5 Obviously the multi block end on end step
		 * 
		 * 
		 * FO4 has the same 3 1s plus 1 objMotion		
		
		// so in this model 1 controllerlink has one node target - bone
		// one interpolator made from  1 track of various spline or static values
		// and animated by a behavior in this calling process on each controller and hence interp and hence modifying the bone
							
		// I have block duration, so if there is more than 1 block each is that long, but the last makes it up to duration
		
		*/

		//System.out.println("var0 is hkaAnimationContainer");

		if (hkaAnimationContainer.skeletons != null) {
			System.out.println("animation has skeletons odd. " + hkaAnimationContainer.skeletons.length);
			for (long skeleIdx : hkaAnimationContainer.skeletons) {
				hkBaseObject obj = hkxContents.get(skeleIdx);
				System.out.println("obj " + obj);
			}
		}

		if (hkaAnimationContainer.animations != null) {
			// TODO: what does more than 1 mean?
			if (hkaAnimationContainer.animations.length != 1)
				System.out.println("animations.length != 1 : " + hkaAnimationContainer.animations.length);

			for (long animIdx : hkaAnimationContainer.animations) {
				hkBaseObject obj = hkxContents.get(animIdx);
				if (obj instanceof hkaSplineCompressedAnimation) {
					spline = (hkaSplineCompressedAnimation)obj;

					startTimeS = 0;// TODO: there is no start time in these one! duration = length = stoptime
					stopTimeS = spline.duration;
					lengthS = stopTimeS - startTimeS;

					lengthMS = (long)(lengthS * 1000);
					
					AnimationTracks animationTracks = spline.animationTracks;

					// just do the first one, but I need to end on end them
					if (animationTracks.transformBlocks.size() != 1) {
						System.out.println(
								"spline.blockTransformTracks.size() != 1 : " + animationTracks.transformBlocks.size());

						System.out.println("block duration = " + spline.blockDuration);
						System.out.println("anim duration " + spline.duration);
					}

					TransformTrack[] transformTracks = animationTracks.transformBlocks.get(0);
					controlledBlocks = new J3dTransformTrack[animationTracks.transformBlocks.get(0).length];
					//TODO: for my model the later blocks are run after the earlier blocks, so they are each 
					// run when the previous alpha is > 1 type thing
					for (int i = 0; i < transformTracks.length; i++) {
						J3dTransformTrack j3dTransformTrack = new J3dTransformTrack(transformTracks[i]);
						controlledBlocks[i] = j3dTransformTrack;
						addChild(j3dTransformTrack);
					}

					//public hkaAnnotationTrack[]	hkaAnimation.annotationTracks;
					//annotationTracks
					/*
					if (niControllerSequence.textKeys2.ref != -1) {
						NiTextKeyExtraData niTextKeyExtraData = (NiTextKeyExtraData)niToJ3dData.get(niControllerSequence.textKeys2);
						j3dNiTextKeyExtraData = new J3dNiTextKeyExtraData(niTextKeyExtraData);
					
						// just for saftey sake
						if (j3dNiTextKeyExtraData.getStartTime() != startTimeS || j3dNiTextKeyExtraData.getEndTime() != stopTimeS) {
							//TODO: removed during parse of FO4 lots don't agree
							//new Throwable("niTextKeyExtraData don't agree with niControllerSequence!").printStackTrace();
						}
					} else {
						System.out.println("What the hell??? niControllerSequence.textKeys2.ref == -1!!");
					}*/

					hkBaseObject objMotion = hkxContents.get(spline.extractedMotion);
					if (objMotion != null) {
						if (objMotion instanceof hkaDefaultAnimatedReferenceFrame) {
							//TODO: for skyrim I don't care, but maybe this is needed for FO4?
							//hkaDefaultAnimatedReferenceFrame motion = (hkaDefaultAnimatedReferenceFrame)objMotion;
							//System.out.println("motion " + motion.forward);
						} else {
							System.out.println("unknown extractedMotion " + objMotion);
						}
					} else {
						//do I mind no extracted motion? Skyrim has none
						//System.out.println("null extractedMotion ");
					}

				} else {
					System.out.println("unknown animation " + obj);
				}
			}
		}

		if (hkaAnimationContainer.bindings != null) {

			if (hkaAnimationContainer.bindings.length != 1)
				System.out
						.println("hkaAnimationContainer.bindings.length != 1 " + hkaAnimationContainer.bindings.length);

			for (long bindIdx : hkaAnimationContainer.bindings) {
				hkBaseObject obj = hkxContents.get(bindIdx);
				if (obj instanceof hkaAnimationBinding) {
					binding = (hkaAnimationBinding)obj;

				} else {
					System.out.println("unknown binding " + obj);
				}
			}
		}

		if (hkaAnimationContainer.attachments != null) {
			System.out.println("animations has attachments " + hkaAnimationContainer.attachments.length);

			// I'm seeing none of these in skyrim or FO4
			for (long attachIdx : hkaAnimationContainer.attachments) {
				hkBaseObject obj = hkxContents.get(attachIdx);
				System.out.println("obj " + obj);
			}
		}

		if (hkaAnimationContainer.skins != null) {
			System.out.println("animations has skins " + hkaAnimationContainer.skins.length);
			// I'm seeing none of these in skyrim or FO4
			for (long skinIdx : hkaAnimationContainer.skins) {
				hkBaseObject obj = hkxContents.get(skinIdx);
				System.out.println("obj " + obj);
			}
		}

		sequenceEventsbehave
				.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceEventsbehave);

		sequenceBehavior.setEnable(false);
		sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceBehavior);
	}

	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton, HKXContents hkxSkeletonContents) {
		setAnimatedNodes(allBonesInSkeleton, null, hkxSkeletonContents);
	}

	public void setAnimatedNodes(	J3dNiDefaultAVObjectPalette allBonesInSkeleton,
									ArrayList<NifJ3dVisRoot> allOtherModels, HKXContents hkxSkeletonContents) {

		if (hkxSkeletonContents != null) {
			hkRootLevelContainer hkRootLevelContainer = (hkRootLevelContainer)hkxSkeletonContents.get(0);
			// grab the first variant option for fun
			hkRootLevelContainerNamedVariant var0 = hkRootLevelContainer.NamedVariants[0];
			hkBaseObject variant = hkxSkeletonContents.get(var0.variant);
			if (variant instanceof hkaAnimationContainer) {
				hkaAnimationContainer skelehkaAnimationContainer = (hkaAnimationContainer)variant;
				if (skelehkaAnimationContainer.skeletons != null) {
					// 0 is the full animation skeleton, 1 looks like a simpler ragdoll version (no fingers)

					hkaSkeleton skeleMapper = (hkaSkeleton)hkxSkeletonContents
							.get(skelehkaAnimationContainer.skeletons[0]);
					//System.out.println("skele " + skeleMapper.name);

					// transformTrackToBoneIndices in binding can be null if track to skeleton.hkx bone mapping is 1 to 1
					if (binding.transformTrackToBoneIndices != null) {
						for (int i = 0; i < binding.transformTrackToBoneIndices.length; i++) {
							J3dTransformTrack j3dTransformTrack = controlledBlocks[i];
							int bone = binding.transformTrackToBoneIndices[i];
							j3dTransformTrack.setBinding(skeleMapper.bones[bone].name, spline.numFrames, lengthS,
									allBonesInSkeleton);
						}
					} else {
						for (int i = 0; i < controlledBlocks.length; i++) {
							J3dTransformTrack j3dTransformTrack = controlledBlocks[i];
							if (skeleMapper.bones.length > i) {
								j3dTransformTrack.setBinding(skeleMapper.bones[i].name, spline.numFrames, lengthS,
										allBonesInSkeleton);
							} else {
								System.out.println("not enough bones for control blocks!");
							}
						}
					}

				} else {
					System.out.println("hkxSkeletonContents hkaAnimationContainer has no skeletons");
				}
			} else {
				System.out.println("hkxSkeletonContents variant[0] not hkaAnimationContainer but " + variant);
			}

		} else {
			System.out.println("hkxSkeletonContents == null");
		}

	}

	@Override
	public boolean isNotRunning() {
		return sequenceAlpha == null || sequenceAlpha.finished();
	}

	public void rampDown() {
		sequenceAlpha.beginExit();
	}

	/**
	 * Fires the sequence in a explicitly non looping manner
	 */
	public void fireSequenceOnce() {
		fireSequence(true, 0);
	}

	/**
	 * This will trigger the sequence, if it has the startloop and end loop tags it will continue looping until rampDown is called
	 * otherwise if it is cycleType == CYCLE_LOOP it will loop indefinately
	 */
	public void fireSequence() {
		fireSequence(true, 0);
	}

	public void fireSequenceOnce(long triggerTime) {
		fireSequence(false, triggerTime);
	}

	public void fireSequence(long triggerTime) {
		fireSequence(true, triggerTime);
	}

	//TODO: I very very much need a run for a fixed time and stop style of this call

	@Override
	public void fireSequence(boolean loop, long triggerTime) {
		sequenceEventsbehave.setEnable(false);
		sequenceBehavior.setEnable(false);
		// tell people the current is finished, only the behavior may have already
		sequenceFinished();

		if (loop) {
			float loopStartS = j3dNiTextKeyExtraData.getStartLoopTime();
			if (loopStartS == -1) {
				sequenceAlpha = new SequenceAlpha(startTimeS, triggerTime, stopTimeS,
						(cycleType == NiControllerSequence.CYCLE_LOOP));
			} else {
				float loopStopS = j3dNiTextKeyExtraData.getEndLoopTime();
				sequenceAlpha = new SequenceAlpha(startTimeS, triggerTime, stopTimeS, loopStartS, loopStopS, true);
			}
		} else {
			//in theory the start time is working right here right now?
			sequenceAlpha = new SequenceAlpha(startTimeS, triggerTime, stopTimeS, false);
		}
		prevSquenceAlphaValue = 0;
		sequenceAlpha.setSequenceAlphaListener(this);
		sequenceAlpha.start();

		// fire off any time ==0 events
		publishSequenceEvents();
		sequenceEventsbehave.setEnable(true);

		sequenceBehavior.setEnable(true);// disables after loop if required

	}

	@Override
	public void addSequenceListener(SequenceListener sequenceListener) {
		if (!sequenceListeners.contains(sequenceListener)) {
			sequenceEventsbehave.setEnable(false);
			sequenceListeners.add(sequenceListener);
			sequenceEventsbehave.setEnable(true);
		}
	}

	@Override
	public void removeSequenceListener(SequenceListener sequenceListener) {
		sequenceEventsbehave.setEnable(false);
		sequenceListeners.remove(sequenceListener);
		sequenceEventsbehave.setEnable(true);
	}

	@Override
	public void publishSequenceEvents() {
		if (sequenceAlpha != null) {
			float newSequenceAlphaValue = sequenceAlpha.value() * lengthS;

			// event annontation not yet done so this is null
			if (j3dNiTextKeyExtraData != null) {
				// this || makes it go round one more time at alpha ==lengthS (the end) to fire the "end" key
				if (newSequenceAlphaValue < lengthS || prevSquenceAlphaValue < lengthS) {
					// have we gone round the loop perhaps? if so fire events from prev to loop end then loop start to new
					if (newSequenceAlphaValue < prevSquenceAlphaValue) {
						// prev to loop end
						for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData()) {
							if (textKeyExtraDataKey.getTime() > prevSquenceAlphaValue
								&& textKeyExtraDataKey.getTime() <= sequenceAlpha.getLoopEndTimeS()) {
								publishEvent(textKeyExtraDataKey);
							}
						}

						//loop start to current
						for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData()) {
							if (textKeyExtraDataKey.getTime() >= sequenceAlpha.getLoopStartTimeS()
								&& textKeyExtraDataKey.getTime() <= newSequenceAlphaValue) {
								publishEvent(textKeyExtraDataKey);
							}
						}
					} else {// just events from prev to new
						for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData()) {
							if (textKeyExtraDataKey.getTime() > prevSquenceAlphaValue
								&& textKeyExtraDataKey.getTime() <= newSequenceAlphaValue) {
								publishEvent(textKeyExtraDataKey);
							}
						}
					}

					prevSquenceAlphaValue = newSequenceAlphaValue;
				}
			}
		}
	}

	private void publishEvent(TextKeyExtraDataKey textKeyExtraDataKey) {
		for (SequenceListener sequenceListener : sequenceListeners) {
			sequenceListener.sequenceEventFired(textKeyExtraDataKey.getTextKey(), textKeyExtraDataKey.getTextParams(),
					textKeyExtraDataKey.getTime());
		}
	}

	public String getFireName() {
		return fireName;
	}

	public long getLengthMS() {
		return lengthMS;
	}

	public J3dNiTextKeyExtraData getJ3dNiTextKeyExtraData() {
		return j3dNiTextKeyExtraData;
	}

	public void processSequence(float alphaValue) {
		for (J3dTransformTrack j3dControllerLink : controlledBlocks) {
			j3dControllerLink.process(alphaValue);
		}
	}

	/**
	 * Our physical bounds is all children bounds nound
	 * but damn slow to re calc so let's cache up! woot
	 * @see org.jogamp.java3d.Node#getBounds()
	 */
	protected Bounds cachedBounds = null;

	@Override
	public Bounds getBounds() {
		if (cachedBounds != null)
			return cachedBounds;

		BoundingSphere ret = new BoundingSphere((BoundingSphere)null);
		for (J3dTransformTrack j3dControllerLink : controlledBlocks) {
			ret.combine(j3dControllerLink.getBounds());
		}
		// if we hit nothing below us (e.g. just animated bones) give it a plenty big radius
		if (ret.isEmpty())
			ret.setRadius(50);

		cachedBounds = ret;
		return ret;
	}

	public class SequenceBehavior extends VaryingLODBehaviour {
		public SequenceBehavior(Node node) {
			// NOTE!!!! these MUST be active, otherwise the headless locale that might be running physics doesn't continuously render
			super(node, new float[] {40, 120, 280}, false, true);
		}

		@Override
		public void process() {
			float alphaValue = sequenceAlpha.value();
			processSequence(alphaValue);

			//turn off at the end
			if (sequenceAlpha.finished()) {
				setEnable(false);
				sequenceFinished();
			}
		}

	}

	@Override
	public void sequenceStarted() {
		for (J3dTransformTrack j3dControllerLink : controlledBlocks) {
			j3dControllerLink.sequenceStarted();
		}

	}

	@Override
	public void sequenceFinished() {
		for (J3dTransformTrack j3dControllerLink : controlledBlocks) {
			j3dControllerLink.sequenceFinished();
		}

	}

	@Override
	public void sequenceLooped(boolean inner) {
		for (J3dTransformTrack j3dControllerLink : controlledBlocks) {
			j3dControllerLink.sequenceLooped(inner);
		}

	}

}
