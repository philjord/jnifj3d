package nif.j3d.animation.tes3;

import java.util.ArrayList;
import java.util.List;

import nif.character.TextKeyExtraDataKey;
import nif.j3d.J3dNiTextKeyExtraData;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper.KeyValue;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper.TimeKeyValue;

public class J3dNiTextKeyExtraDataTes3 extends J3dNiTextKeyExtraData
{
	public J3dNiTextKeyExtraDataTes3(String fireName, TimeKeyValue[] timeKeyValues)
	{
		startTime = 0;
		startLoopTime = 0;
		endLoopTime = 0;
		endTime = 0;
		// need to pull all keys out wiht name form  list and build times and events
		List<TextKeyExtraDataKey> kfSequenceTimeDataList = new ArrayList<TextKeyExtraDataKey>();

		for (TimeKeyValue timeKeyValue : timeKeyValues)
		{
			for (KeyValue keyValue : timeKeyValue.keyValues)
			{
				//is it "for us"
				if (keyValue.key.equals(fireName))
				{
					TextKeyExtraDataKey tked = new TextKeyExtraDataKey(keyValue.value, timeKeyValue.time);
					// note not the same as supers keys
					if (tked.getTextKey().toLowerCase().equals("start"))
					{
						startTime = tked.getTime();
					}
					else if (tked.getTextKey().toLowerCase().equals("loop start"))
					{
						startLoopTime = tked.getTime();
					}
					else if (tked.getTextKey().toLowerCase().equals("loop stop"))
					{
						endLoopTime = tked.getTime();
					}
					else if (tked.getTextKey().toLowerCase().equals("stop"))
					{
						endTime = tked.getTime();
					}
					else if (tked.getTextKey().toLowerCase().equals("hit"))
					{
						//known
					}
					// lots of crazy spell cast ones
					else
					{
						//System.out.println("Unknown keyValue fireName=[" + keyValue.key + "] key=[" + keyValue.value + "]");
					}
				}
			}
		}
		kfSequenceTimeData = new TextKeyExtraDataKey[kfSequenceTimeDataList.size()];
		kfSequenceTimeDataList.toArray(kfSequenceTimeData);
	}
}
