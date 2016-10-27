package nif.j3d.animation.tes3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.Bounds;
import org.jogamp.vecmath.Point3d;

import nif.NifJ3dVisRoot;
import nif.character.TextKeyExtraDataKey;
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
		// skip the first extra data as it is textkeys bone node names start below it
		NiStringExtraData nsed = (NiStringExtraData) niToJ3dData.get(ntked.NextExtraData);

		while (controller != null)
		{
			String nodeName = nsed.stringData;
			J3dNiAVObject nodeTarget = allBonesInSkeleton.getByName(nodeName);
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

		// find all unique animation names, ignore case in all cases
		HashSet<String> namesFound = new HashSet<String>();
		List<J3dNiControllerSequenceTes3> j3dNiControllerSequenceList = new ArrayList<J3dNiControllerSequenceTes3>();
		TimeKeyValue[] tkvs = parseTimeKeyValues(ntked);
		for (int i = 0; i < tkvs.length; i++)
		{
			TimeKeyValue tkv = tkvs[i];
			for (KeyValue kv : tkv.keyValues)
			{
				String key = kv.key.toLowerCase();
				TextKeyExtraDataKey tked = new TextKeyExtraDataKey(kv.value, tkv.time);

				// only form a J3dNiControllerSequenceTes3 from the value start (it will search for it's own stop)
				if (tked.getTextKey().toLowerCase().trim().endsWith("start"))
				{
					if (!namesFound.contains(key))
					{
						namesFound.add(key);

						//	if (niToJ3dData.nifVer.fileName.contains("base_anim"))
						//		System.out.println("Key " + key + "\t" + kv.value);

						j3dNiControllerSequenceList.add(new J3dNiControllerSequenceTes3(key, tkvs, i, j3dNiKeyframeControllers));
					}
					else
					{
						// this is apparently totally possible, and using the first seems ok 
						//as the sequence resets itself to the second anyway
						//Key start found twice!!! attack2 at 6.7333336 in Meshes\r\xCliffRacer.kf

						//System.err.println(" Key start found twice!!! " + key + " at " + tkv.time + " in " + niToJ3dData.nifVer.fileName);
					}

				}
			}
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

			//if (((String) key.value).toLowerCase().contains("weapon"))
			//	System.out.println("key.value " + key.value);

			ret[i] = new TimeKeyValue(key.time, parseKeyValues((String) key.value));
		}
		return ret;
	}

	public static KeyValue[] parseKeyValues(String input)
	{
		/*	List<KeyValue> keyValues2 = new ArrayList<KeyValue>();
			String[] kvParts = input.split("\r\n");
			for (int i = 0; i < kvParts.length; i++)
			{
				String[] parts = kvParts[i].split(": ");
				//often blanks etc
				if (parts.length == 2)
					keyValues2.add(new KeyValue(parts[0].trim(), parts[1].trim()));
		
			}*/

		List<KeyValue> keyValues = new ArrayList<KeyValue>();
		int pos = 0, end;
		while ((end = input.indexOf("\r\n", pos)) >= 0)
		{
			String kvPart = input.substring(pos, end);
			int sIdx = kvPart.indexOf(": ");
			if (sIdx != -1)
			{

				// Spell cast and handtohand have sub types for now
				// so also grab the next word
				String key = kvPart.substring(0, sIdx).trim();
				String value = kvPart.substring(sIdx + 2).trim();
				if (key.startsWith("Weapon") || key.equals("HandToHand") || key.equals("SpellCast"))
				{
					key = key + " " + value.substring(0, value.indexOf(" "));
					value = value.substring(value.indexOf(" ") + 1);
				}

				keyValues.add(new KeyValue(key, value));

			}
			pos = end + 1;
		}
		// add the last (not finished with \r\n) entry in
		String kvPart = input.substring(pos);
		int sIdx = kvPart.indexOf(": ");
		if (sIdx != -1)
		{
			// Spell cast and handtohand have sub types for now
			// so also grab the next word
			String key = kvPart.substring(0, sIdx).trim();
			String value = kvPart.substring(sIdx + 2).trim();
			if (key.startsWith("Weapon") || key.equals("HandToHand") || key.equals("SpellCast"))
			{
				key = key + " " + value.substring(0, value.indexOf(" "));
				value = value.substring(value.indexOf(" ") + 1);
			}

			keyValues.add(new KeyValue(key, value));
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
