package nif.j3d.animation;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;

import nif.NifJ3dVisRoot;
import nif.character.TextKeyExtraDataKey;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.J3dNiTextKeyExtraData;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiTextKeyExtraData;
import tools3d.utils.scenegraph.VaryingLODBehaviour;

public class J3dNiControllerSequence extends Group
{
	private SequenceEventsBehavior sequenceEventsbehave = new SequenceEventsBehavior();

	private SequenceBehavior sequenceBehavior = new SequenceBehavior(this);

	private ArrayList<SequenceListener> sequenceListeners = new ArrayList<SequenceListener>();

	private J3dControllerLink[] controlledBlocks;

	private String fireName;

	private J3dNiTextKeyExtraData j3dNiTextKeyExtraData;

	private SequenceAlpha sequenceAlpha;

	private float prevSquenceAlphaValue = 0;

	private long lengthMS = 0;

	private float startTimeS = 0;

	private float stopTimeS = 0;

	private float lengthS = 0;

	private NiControllerSequence niControllerSequence;

	private NiToJ3dData niToJ3dData;

	private int cycleType = NiControllerSequence.CYCLE_CLAMP;

	public J3dNiControllerSequence(NiControllerSequence niControllerSequence, NiToJ3dData niToJ3dData)
	{
		this.niControllerSequence = niControllerSequence;
		this.niToJ3dData = niToJ3dData;

		fireName = niControllerSequence.name;

		startTimeS = niControllerSequence.startTime;
		stopTimeS = niControllerSequence.stopTime;
		lengthS = stopTimeS - startTimeS;

		lengthMS = (long) (lengthS * 1000);

		cycleType = niControllerSequence.cycleType;

		if (niControllerSequence.textKeys2.ref != -1)
		{
			NiTextKeyExtraData niTextKeyExtraData = (NiTextKeyExtraData) niToJ3dData.get(niControllerSequence.textKeys2);
			j3dNiTextKeyExtraData = new J3dNiTextKeyExtraData(niTextKeyExtraData);

			// just for saftey sake
			if (j3dNiTextKeyExtraData.getStartTime() != startTimeS || j3dNiTextKeyExtraData.getEndTime() != stopTimeS)
			{
				new Throwable("niTextKeyExtraData don't agree with niControllerSequence!").printStackTrace();
			}
		}
		else
		{
			System.out.println("What the hell??? niControllerSequence.textKeys2.ref == -1!!");
		}

		sequenceEventsbehave.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceEventsbehave);

		sequenceBehavior.setEnable(false);
		sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceBehavior);
	}

	public void addSequenceListener(SequenceListener sequenceListener)
	{
		if (!sequenceListeners.contains(sequenceListener))
		{
			sequenceEventsbehave.setEnable(false);
			sequenceListeners.add(sequenceListener);
			sequenceEventsbehave.setEnable(true);
		}
	}

	public void removeSequenceListener(SequenceListener sequenceListener)
	{
		sequenceEventsbehave.setEnable(false);
		sequenceListeners.remove(sequenceListener);
		sequenceEventsbehave.setEnable(true);
	}

	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton)
	{
		setAnimatedNodes(allBonesInSkeleton, null);

	}

	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		controlledBlocks = new J3dControllerLink[niControllerSequence.numControlledBlocks];

		for (int i = 0; i < niControllerSequence.numControlledBlocks; i++)
		{
			if (niControllerSequence.controlledBlocks[i] != null)
			{
				J3dControllerLink j3dControllerLink = new J3dControllerLink(niControllerSequence.controlledBlocks[i], niToJ3dData,
						startTimeS, stopTimeS, allBonesInSkeleton, allOtherModels);
				controlledBlocks[i] = j3dControllerLink;
				addChild(j3dControllerLink);
			}
		}
	}

	public boolean isNotRunning()
	{
		return sequenceAlpha == null || sequenceAlpha.finished();
	}

	/**
	 * Fires the sequence in a explicitly non looping manner
	 */
	public void fireSequenceOnce()
	{
		fireSequence(true);
	}

	/**
	 * This will trigger the sequnce, if it has the startloop and end loop tags it will continue looping until rampDown is called
	 * otherwise if it ais cycleType == CYCLE_LOOP it will loop indefinately
	 */
	public void fireSequence()
	{
		fireSequence(false);
	}

	public void rampDown()
	{
		sequenceAlpha.beginExit();
	}

	/*
	 * TODO:
	 * I need to have one varying behavior up here, work out the node dist just once(it's expensive)
	 * call the alpha value just once, and then call all the interpolators below here with a process and a 
	 * single alpha value given to each
	 *  
	 *
	 */
	private void fireSequence(boolean forceOnce)
	{
		sequenceEventsbehave.setEnable(false);
		sequenceBehavior.setEnable(false);

		if (forceOnce)
		{
			sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);
		}
		else
		{
			float loopStartS = j3dNiTextKeyExtraData.getStartLoopTime();
			if (loopStartS == -1)
			{
				sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, (cycleType == NiControllerSequence.CYCLE_LOOP));
			}
			else
			{
				float loopStopS = j3dNiTextKeyExtraData.getEndLoopTime();
				sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, loopStartS, loopStopS, true);
			}
		}
		prevSquenceAlphaValue = 0;
		sequenceAlpha.start();

		// fire off any time ==0 events
		publishSequenceEvents();
		sequenceEventsbehave.setEnable(true);

		sequenceBehavior.setEnable(true);// disbales after loop if required

	}

	public void publishSequenceEvents()
	{
		if (sequenceAlpha != null)
		{
			float newSequenceAlphaValue = sequenceAlpha.value() * lengthS;

			// this || makes it go round one more time at alpha ==lengthS (the end) to fire the "end" key
			if (newSequenceAlphaValue < lengthS || prevSquenceAlphaValue < lengthS)
			{
				// have we gone round the loop perhaps? if so fire events from prev to loop end then loop start to new
				if (newSequenceAlphaValue < prevSquenceAlphaValue)
				{
					// prev to loop end
					for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData())
					{
						if (textKeyExtraDataKey.getTime() > prevSquenceAlphaValue
								&& textKeyExtraDataKey.getTime() <= sequenceAlpha.getLoopEndTimeS())
						{
							publishEvent(textKeyExtraDataKey);
						}
					}

					//loop start to current
					for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData())
					{
						if (textKeyExtraDataKey.getTime() >= sequenceAlpha.getLoopStartTimeS()
								&& textKeyExtraDataKey.getTime() <= newSequenceAlphaValue)
						{
							publishEvent(textKeyExtraDataKey);
						}
					}
				}
				else
				// just events from prev to new
				{
					for (TextKeyExtraDataKey textKeyExtraDataKey : j3dNiTextKeyExtraData.getKfSequenceTimeData())
					{
						if (textKeyExtraDataKey.getTime() > prevSquenceAlphaValue && textKeyExtraDataKey.getTime() <= newSequenceAlphaValue)
						{
							publishEvent(textKeyExtraDataKey);
						}
					}
				}

				prevSquenceAlphaValue = newSequenceAlphaValue;
			}
		}
	}

	private void publishEvent(TextKeyExtraDataKey textKeyExtraDataKey)
	{
		for (SequenceListener sequenceListener : sequenceListeners)
		{
			sequenceListener.sequenceEventFired(textKeyExtraDataKey.getTextKey(), textKeyExtraDataKey.getTextParams(),
					textKeyExtraDataKey.getTime());
		}
	}

	public String getFireName()
	{
		return fireName;
	}

	public long getLengthMS()
	{
		return lengthMS;
	}

	public J3dNiTextKeyExtraData getJ3dNiTextKeyExtraData()
	{
		return j3dNiTextKeyExtraData;
	}

	/**
	 * Our physical bounds is all children bounds nound
	 * but damn slow to re calc so let's cache up! woot
	 * @see javax.media.j3d.Node#getBounds()
	 */
	private Bounds cachedBounds = null;

	@Override
	public Bounds getBounds()
	{
		if (cachedBounds != null)
			return cachedBounds;

		BoundingSphere ret = new BoundingSphere((BoundingSphere) null);
		for (J3dControllerLink j3dControllerLink : controlledBlocks)
		{
			ret.combine(j3dControllerLink.getBounds());
		}
		// if we hit nothing below us (e.g. just animated bones) give it a plenty big radius
		if (ret.isEmpty())
			ret.setRadius(50);

		cachedBounds = ret;
		return ret;
	}

	public class SequenceBehavior extends VaryingLODBehaviour
	{
		public SequenceBehavior(Node node)
		{
			// NOTE!!!! these MUST be active, otherwise the headless locale that might be running physics doesn't continuously render
			super(node, new float[]
			{ 40, 120, 280 }, false, true);
		}

		@Override
		public void process()
		{
			float alphaValue = sequenceAlpha.value();
			for (J3dControllerLink j3dControllerLink : controlledBlocks)
			{
				j3dControllerLink.process(alphaValue);
			}

			//turn off at the end
			if (sequenceAlpha.finished())
				setEnable(false);
		}
	}

	/**
	 * This listener will be called back whenever a key event occurs in the sequence timeline defined by the text keys
	 * @author philip
	 *
	 */
	public interface SequenceListener
	{
		public void sequenceEventFired(String key, String params[], float time);
	}

	class SequenceEventsBehavior extends Behavior
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
			publishSequenceEvents();
			wakeupOn(passiveWakeupCriterion);
		}

	}

}
