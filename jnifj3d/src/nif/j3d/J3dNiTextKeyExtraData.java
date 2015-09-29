package nif.j3d;

import nif.character.TextKeyExtraDataKey;
import nif.niobject.NiTextKeyExtraData;

public class J3dNiTextKeyExtraData
{
	protected TextKeyExtraDataKey[] kfSequenceTimeData;

	// common keys	
	protected float startTime = -1;

	protected float startLoopTime = -1;

	protected float endLoopTime = -1;

	protected float endTime = -1;

	//for TES3
	protected J3dNiTextKeyExtraData()
	{

	}

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
