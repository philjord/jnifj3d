package nif.j3d.animation.tes3;

import java.util.ArrayList;
import java.util.List;

import nif.character.TextKeyExtraDataKey;
import nif.j3d.J3dNiTextKeyExtraData;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper.KeyValue;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper.TimeKeyValue;

public class J3dNiTextKeyExtraDataTes3 extends J3dNiTextKeyExtraData
{
	public J3dNiTextKeyExtraDataTes3(String fireName, TimeKeyValue[] timeKeyValues, int keyValuesStartposition)
	{
		startTime = 0;
		startLoopTime = -1;
		endLoopTime = -1;
		endTime = 0;
		// need to pull all keys out with name form  list and build times and events
		List<TextKeyExtraDataKey> kfSequenceTimeDataList = new ArrayList<TextKeyExtraDataKey>();

		// notice we start of the start value (or should do!)
		for (int i = keyValuesStartposition; i < timeKeyValues.length; i++)
		{
			TimeKeyValue timeKeyValue = timeKeyValues[i];

			for (KeyValue keyValue : timeKeyValue.keyValues)
			{
				//is it "for us" or a sound gen
				if (keyValue.key.equalsIgnoreCase(fireName))
				{
					TextKeyExtraDataKey tked = new TextKeyExtraDataKey(keyValue.value, timeKeyValue.time);
					// note not the same as supers keys

					// all keys are teh name +":" + label text (includes spaces) + [start stop]

					// spell can have release
					// weapon can have hit
					// soundgen can have right left land (general names for sounds?		

					// ok so BowAndArrow: has Shoot sub type but also Unequip sub type, 
					// obviously the sub type requied to start and stop nees to be understood.
					// Loop does not appear to be a subtype 
					// Shield has a Block sub type with start and end

					// so the words that can end it are
					//start 
					//loop start being different, but only applies to walk, turn, run and idle?
					//stop
					//equip
					//unequip
					//attack
					//hit
					//release
					//attach
					//detach

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
					else if (tked.getTextKey().toLowerCase().equals("stop") || tked.getTextKey().toLowerCase().equals("stop."))
					{
						endTime = tked.getTime();
						// no more values for us now
						break;
					}
					else if (tked.getTextKey().toLowerCase().equals("hit"))
					{
						//known
					}

					// lots of crazy spell cast ones
					else
					{
				//		System.out.println("Unknown keyValue fireName=[" + keyValue.key + "] key=[" + keyValue.value + "]");
					}
				}
				else if (keyValue.key.equalsIgnoreCase("sound") || keyValue.key.equalsIgnoreCase("soundgen"))
				{
					// all sounds are picked up by whatever firename is processing them
					//TODO:

					//oddly SwishM is a SOUN but Left for soundgen is not? (needs something added to generate a sound a number?
					// Sound: BowShoot is not a SOUN file either

					/*	SNDG =   168 (    50,     75.86,     94)
								Sound Generator
								NAME = Name? (DEFAULT0001, ALIT0001, etc...)
								DATA = Sound Type Data (4 bytes, long)
									0 = Left Foot
									1 = Right Foot
									2 = Swim Left
									3 = Swim Right
									4 = Moan
									5 = Roar
									6 = Scream
									7 = Land
									
					more name examples, so possibly the sndg type versus soundgen versus creature?
					SNDG clannfear0004
					SNDG clannfear0005
					SNDG clannfear0006
					SNDG daedroth0004
					SNDG daedroth0005
					SNDG daedroth0006
					SNDG Kwama Queen0004
					
					CREA bonewalker
					CREA clannfear
					CREA Kwama Queen
					CREA scrib_vaba-amus - just SNDG scrib0000
					
					no NPC_ types so SNDG DEFAULT0001 must be for them
									*/

				}
				else
				{
					// this is fine as multiple firenames can run through concurrently
				}
			}
		}
		kfSequenceTimeData = new TextKeyExtraDataKey[kfSequenceTimeDataList.size()];
		kfSequenceTimeDataList.toArray(kfSequenceTimeData);
	}
}
