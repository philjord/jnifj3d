package nif.j3d;

import nif.NiObjectList;
import nif.character.TextKeyExtraDataKey;
import nif.niobject.NiObject;
import nif.niobject.NiTextKeyExtraData;

public class J3dNiTextKeyExtraData
{
	private TextKeyExtraDataKey[] kfSequenceTimeData;

	// common keys	
	private float startTime = -1;

	private float startLoopTime = -1;

	private float endLoopTime = -1;

	private float endTime = -1;

	public J3dNiTextKeyExtraData(NiTextKeyExtraData niTextKeyExtraData)
	{
		kfSequenceTimeData = getKFSequenceTimeData(niTextKeyExtraData);

		//preload super common ones
		for (TextKeyExtraDataKey tked : kfSequenceTimeData)
		{
			if (tked.getTextKey().equals("start"))
			{
				startTime = tked.getTime();
			}
			else if (tked.getTextKey().equals("StartLoop"))
			{
				startLoopTime = tked.getTime();
			}
			else if (tked.getTextKey().equals("EndLoop"))
			{
				endLoopTime = tked.getTime();
			}
			else if (tked.getTextKey().equals("end"))
			{
				endTime = tked.getTime();
			}
		}
	}

	public float getTimeForString(String key)
	{
		if (kfSequenceTimeData != null)
		{
			TextKeyExtraDataKey data = getTimeForString(kfSequenceTimeData, key);
			if (data != null)
			{
				return data.getTime();
			}
		}

		return -1f;
	}

	private static TextKeyExtraDataKey[] getKFSequenceTimeData(NiTextKeyExtraData niTextKeyExtraData)
	{
		TextKeyExtraDataKey[] newKFSequenceTimeData = new TextKeyExtraDataKey[niTextKeyExtraData.numTextKeys];

		for (int i = 0; i < niTextKeyExtraData.numTextKeys; i++)
		{
			newKFSequenceTimeData[i] = new TextKeyExtraDataKey(niTextKeyExtraData.textKeys[i]);
		}
		return newKFSequenceTimeData;
	}

	private static TextKeyExtraDataKey[] getKFSequenceTimeData(NiObjectList niObjects)
	{
		for (NiObject no : niObjects)
		{
			if (no instanceof NiTextKeyExtraData)
			{
				return getKFSequenceTimeData((NiTextKeyExtraData) no);
			}
		}
		return null;
	}

	/**
	 * Equals on text before :
	 * @param niTextKeyExtraData
	 * @param key
	 * @return
	 */
	public static TextKeyExtraDataKey getTimeForString(TextKeyExtraDataKey[] kfSequenceTimeData, String key)
	{
		for (int i = 0; i < kfSequenceTimeData.length; i++)
		{
			TextKeyExtraDataKey d = kfSequenceTimeData[i];

			if (d.getTextKey().equals(key))
			{
				return d;
			}
		}
		return null;
	}

	/**
	
	 * @param niToJ3dData
	 * @param key
	 * @return -1 if no exist
	 */
	public static float getTimeForString(NiToJ3dData niToJ3dData, String key)
	{

		TextKeyExtraDataKey[] kfSequenceTimeData = getKFSequenceTimeData(niToJ3dData.getNiObjects());
		if (kfSequenceTimeData != null)
		{
			TextKeyExtraDataKey data = getTimeForString(kfSequenceTimeData, key);
			if (data != null)
			{
				return data.getTime();
			}
		}
		return -1;
	}

	public TextKeyExtraDataKey[] getKfSequenceTimeData()
	{
		return kfSequenceTimeData;
	}

	public float getStartTime()
	{
		return startTime;
	}

	public float getStartLoopTime()
	{
		return startLoopTime;
	}

	public float getEndLoopTime()
	{
		return endLoopTime;
	}

	public float getEndTime()
	{
		return endTime;
	}
}
