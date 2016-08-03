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
import nif.j3d.animation.SequenceAlpha.SequenceAlphaListener;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiTextKeyExtraData;
import tools3d.utils.scenegraph.VaryingLODBehaviour;

public class J3dNiControllerSequence extends Group implements SequenceAlphaListener
{
	private SequenceEventsBehavior sequenceEventsbehave = new SequenceEventsBehavior();

	private SequenceBehavior sequenceBehavior = new SequenceBehavior(this);

	private ArrayList<SequenceListener> sequenceListeners = new ArrayList<SequenceListener>();

	protected J3dControllerLink[] controlledBlocks;

	protected String fireName;

	protected J3dNiTextKeyExtraData j3dNiTextKeyExtraData;

	private SequenceAlpha sequenceAlpha;

	private float prevSquenceAlphaValue = 0;

	protected long lengthMS = 0;

	protected float startTimeS = 0;

	protected float stopTimeS = 0;

	protected float lengthS = 0;

	private NiControllerSequence niControllerSequence;

	private NiToJ3dData niToJ3dData;

	protected int cycleType = NiControllerSequence.CYCLE_CLAMP;

	// for TES3
	protected J3dNiControllerSequence()
	{
		sequenceEventsbehave.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceEventsbehave);

		sequenceBehavior.setEnable(false);
		sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceBehavior);

	}

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
				//TODO: removed during parse of FO4 lots don't agree
				//new Throwable("niTextKeyExtraData don't agree with niControllerSequence!").printStackTrace();
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

	// temp clean up until I work out how to fire the right geommorph
	// only the last one will fire
	private J3dControllerLink singleGeomMorpher = null;

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

				if (j3dControllerLink.isControlsGeomMorpher())
					singleGeomMorpher = j3dControllerLink;
			}
		}
	}

	public boolean isNotRunning()
	{
		return sequenceAlpha == null || sequenceAlpha.finished();
	}

	public void rampDown()
	{
		sequenceAlpha.beginExit();
	}

	/**
	 * Fires the sequence in a explicitly non looping manner
	 */
	public void fireSequenceOnce()
	{
		fireSequence(true, 0);
	}

	/**
	 * This will trigger the sequence, if it has the startloop and end loop tags it will continue looping until rampDown is called
	 * otherwise if it is cycleType == CYCLE_LOOP it will loop indefinately
	 */
	public void fireSequence()
	{
		fireSequence(true, 0);
	}

	public void fireSequenceOnce(long triggerTime)
	{
		fireSequence(false, triggerTime);
	}

	public void fireSequence(long triggerTime)
	{
		fireSequence(true, triggerTime);
	}

	//TODO: I very very much need a run for a fixed time and stop style of this call

	public void fireSequence(boolean loop, long triggerTime)
	{
		sequenceEventsbehave.setEnable(false);
		sequenceBehavior.setEnable(false);
		// tell people the current is finished, only the behavior may have already
		sequenceFinished();

		if (loop)
		{
			float loopStartS = j3dNiTextKeyExtraData.getStartLoopTime();
			if (loopStartS == -1)
			{
				sequenceAlpha = new SequenceAlpha(startTimeS, triggerTime, stopTimeS, (cycleType == NiControllerSequence.CYCLE_LOOP));
			}
			else
			{
				float loopStopS = j3dNiTextKeyExtraData.getEndLoopTime();
				sequenceAlpha = new SequenceAlpha(startTimeS, triggerTime, stopTimeS, loopStartS, loopStopS, true);
			}
		}
		else
		{
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

	public void processSequence(float alphaValue)
	{
		for (J3dControllerLink j3dControllerLink : controlledBlocks)
		{
			//stupid geommorph test
			if (!j3dControllerLink.isControlsGeomMorpher() || j3dControllerLink == singleGeomMorpher)
				j3dControllerLink.process(alphaValue);
		}
	}

	/**
	 * Our physical bounds is all children bounds nound
	 * but damn slow to re calc so let's cache up! woot
	 * @see javax.media.j3d.Node#getBounds()
	 */
	protected Bounds cachedBounds = null;

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
			super(node, new float[] { 40, 120, 280 }, false, true);
		}

		@Override
		public void process()
		{
			float alphaValue = sequenceAlpha.value();
			processSequence(alphaValue);

			//turn off at the end
			if (sequenceAlpha.finished())
			{
				setEnable(false);
				sequenceFinished();
			}
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
		private WakeupOnElapsedFrames passiveWakeupCriterion = new WakeupOnElapsedFrames(5, true);

		@Override
		public void initialize()
		{
			wakeupOn(passiveWakeupCriterion);
		}

		@Override
		public void processStimulus(Enumeration critiria)
		{
			publishSequenceEvents();
			wakeupOn(passiveWakeupCriterion);
		}

	}

	@Override
	public void sequenceStarted()
	{
		for (J3dControllerLink j3dControllerLink : controlledBlocks)
		{
			//stupid geommorph test
			if (!j3dControllerLink.isControlsGeomMorpher() || j3dControllerLink == singleGeomMorpher)
				j3dControllerLink.sequenceStarted();
		}

	}

	@Override
	public void sequenceFinished()
	{
		for (J3dControllerLink j3dControllerLink : controlledBlocks)
		{
			//stupid geommorph test
			if (!j3dControllerLink.isControlsGeomMorpher() || j3dControllerLink == singleGeomMorpher)
				j3dControllerLink.sequenceFinished();
		}

	}

	@Override
	public void sequenceLooped(boolean inner)
	{
		for (J3dControllerLink j3dControllerLink : controlledBlocks)
		{
			//stupid geommorph test
			if (!j3dControllerLink.isControlsGeomMorpher() || j3dControllerLink == singleGeomMorpher)
				j3dControllerLink.sequenceLooped(inner);
		}

	}

}
