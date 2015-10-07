package nif.j3d.animation.tes3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;

import nif.NifJ3dVisRoot;
import nif.compound.NifKey;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiSequenceStreamHelper;
import nif.niobject.NiStringExtraData;
import nif.niobject.NiTextKeyExtraData;
import nif.niobject.controller.NiKeyframeController;

public class J3dNiSequenceStreamHelper extends J3dNiAVObject
{
	private NiSequenceStreamHelper niSequenceStreamHelper;

	private NiToJ3dData niToJ3dData;

	private J3dNiKeyframeController[] j3dNiKeyframeControllers;

	private J3dNiControllerSequenceTes3[] sequences;

	public J3dNiSequenceStreamHelper(NiSequenceStreamHelper niSequenceStreamHelper, NiToJ3dData niToJ3dData)
	{
		super(niSequenceStreamHelper, niToJ3dData);
		this.niSequenceStreamHelper = niSequenceStreamHelper;
		this.niToJ3dData = niToJ3dData;

	}

	/**
	 * to do geomorphs ??
	 * @param allBonesInSkeleton
	 * @param allOtherModels
	 */
	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		// build a list of controllers
		List<J3dNiKeyframeController> j3dNiKeyframeControllerList = new ArrayList<J3dNiKeyframeController>();
		NiKeyframeController controller = (NiKeyframeController) niToJ3dData.get(niSequenceStreamHelper.controller);

		NiTextKeyExtraData ntked = (NiTextKeyExtraData) niToJ3dData.get(niSequenceStreamHelper.extraData);
		// skip teh first extra data as it is textkeys bone node names start below it
		NiStringExtraData nsed = (NiStringExtraData) niToJ3dData.get(ntked.NextExtraData);

		while (controller != null)
		{
			String nodeName = nsed.stringData;
			J3dNiAVObject nodeTarget = allBonesInSkeleton.get(nodeName);
			if (nodeTarget == null)
			{
				// this is likely fine, animations for nifs that miss out the bone, ignore.
			}
			else
			{
				J3dNiKeyframeController j3dNiKeyframeController = new J3dNiKeyframeController(controller, niToJ3dData, nodeTarget);
				j3dNiKeyframeControllerList.add(j3dNiKeyframeController);
			}
			// next!
			controller = (NiKeyframeController) niToJ3dData.get(controller.nextController);
			// roll nsed forward too and hope they stay in synch (they should do)
			nsed = (NiStringExtraData) niToJ3dData.get(nsed.NextExtraData);
		}
		j3dNiKeyframeControllers = new J3dNiKeyframeController[j3dNiKeyframeControllerList.size()];
		j3dNiKeyframeControllerList.toArray(j3dNiKeyframeControllers);

		// now from text string build all animations as sequences, give each sequence all interps

		// find all unique animation names
		HashSet<String> namesFound = new HashSet<String>();
		TimeKeyValue[] tkvs = parseTimeKeyValues(ntked);
		for (TimeKeyValue tkv : tkvs)
		{
			for (KeyValue kv : tkv.keyValues)
			{
				// skip sounds for now (can be mixed case)
				if (!kv.key.toLowerCase().equals("soundgen") //
						&& !kv.key.toLowerCase().equals("sound"))
				{
					// TODO: issue I've seen Knockout and KnockOut in xgreatbonewalker.nif
					namesFound.add(kv.key);
				}
			}
		}

		//and contruct sequences
		List<J3dNiControllerSequenceTes3> j3dNiControllerSequenceList = new ArrayList<J3dNiControllerSequenceTes3>();
		for (Object fireName : namesFound.toArray())
		{
			j3dNiControllerSequenceList.add(new J3dNiControllerSequenceTes3((String) fireName, tkvs, j3dNiKeyframeControllers));
		}

		sequences = new J3dNiControllerSequenceTes3[j3dNiControllerSequenceList.size()];
		j3dNiControllerSequenceList.toArray(sequences);

	}

	BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 50);

	@Override
	public Bounds getBounds()
	{
		return bounds;
	}

	public J3dNiControllerSequenceTes3 getSequence(String action)
	{
		for (int i = 0; i < sequences.length; i++)
		{
			if (sequences[i].getFireName().equals(action))
			{
				return sequences[i];
			}
		}
		return null;
	}

	public String[] getAllSequences()
	{
		String[] strings = new String[sequences.length];
		for (int i = 0; i < sequences.length; i++)
		{
			strings[i] = sequences[i].getFireName();
		}
		return strings;
	}

	public long[] getAllSequenceLengths()
	{
		long[] lengths = new long[sequences.length];
		for (int i = 0; i < sequences.length; i++)
		{
			lengths[i] = sequences[i].getLengthMS();
		}
		return lengths;
	}

	public static TimeKeyValue[] parseTimeKeyValues(NiTextKeyExtraData ntked)
	{
		TimeKeyValue[] ret = new TimeKeyValue[ntked.textKeys.length];
		for (int i = 0; i < ntked.textKeys.length; i++)
		{
			NifKey key = ntked.textKeys[i];
			ret[i] = new TimeKeyValue(key.time, parseKeyValues((String) key.value));
		}
		return ret;
	}

	public static KeyValue[] parseKeyValues(String input)
	{
		List<KeyValue> keyValues = new ArrayList<KeyValue>();
		String[] kvParts = input.split("\r\n");
		for (int i = 0; i < kvParts.length; i++)
		{
			String[] parts = kvParts[i].split(": ");
			//often blanks etc
			if (parts.length == 2)
				keyValues.add(new KeyValue(parts[0].trim(), parts[1].trim()));
			 
		}

		KeyValue[] ret = new KeyValue[keyValues.size()];
		keyValues.toArray(ret);
		return ret;
	}

	public static class TimeKeyValue
	{
		public float time = 0;

		public KeyValue[] keyValues;

		public TimeKeyValue(float time, KeyValue[] keyValues)
		{
			this.time = time;
			this.keyValues = keyValues;
		}
	}

	public static class KeyValue
	{
		public String key;

		public String value;

		public KeyValue(String key, String value)
		{
			this.key = key;
			this.value = value;
		}

	}
}