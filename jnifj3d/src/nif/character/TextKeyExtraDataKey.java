package nif.character;

import nif.compound.NifKey;

public class TextKeyExtraDataKey
{
	private String textKey = "";

	private String[] textParams = null;//optional, sometimes more than 1

	private float time = -1;

	// for TES3
	public TextKeyExtraDataKey(String textKey, float time)
	{
		this.textKey = textKey;
		this.time = time;
	}

	public TextKeyExtraDataKey(NifKey nifKey)
	{
		String value = (String) nifKey.value;
		String[] vals = value.split(":", 2);
		textKey = vals[0];
		textParams = vals.length > 1 ? vals[1].trim().split(" ", 0) : new String[]
		{ "" };

		time = nifKey.time;
	}

	public String getTextKey()
	{
		return textKey;
	}

	public String[] getTextParams()
	{
		return textParams;
	}

	public float getTime()
	{
		return time;
	}

	/**
	 * Note sounds appear to be encoded
	 * Sound
	 * textParam: AMBDustDevilLPM
	 * would be amb folder dustdevil folder
	 * amb_dustdevil_01_lpm.wav file
	 * so is there a lookup for this? possibly the original bsa file had it? only the "_01_" part is unspecified
	 * but the first foler is caps as is the extension part
	 */
	/*
	 * all have 
	 * key: start and key: end
	 * where start is 0.0 and end is the end time
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\1hmattackleftdown_a.kf
	 * key: Hit
	 * key: a:R (or key: a:L)
	 * key: BlendIn:1
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\1gtholster.kf
	 * key: Blend: 1
	 * key: prn: Bip01 Pelvis
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\1gtequip.kf
	 * key: Enum: Equip
	 * key: Attach
	 * key: Prn: Bip01 R Hand
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\1gtattackthrow5.kf
	 * key: Sound: WPNGrenadeThrowA
	 * key: Hold
	 * key: Sound: WPNGrenadeThrowB
	 * key: Release
	 * key: Attach
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\idleanims\3rdp_cowering.kf
	 * key: StartLoop
	 * key: EndLoop
	 * 
	 * File: C:\game media\Fallout\meshes\characters\_male\idleanims\3rdp_specialidle_wowmanizing01.kf
	 * key: Blend: 15
	 * key: StartLoop
	 * key: Sound: NPCHumanDrinkingBottleGulp
	 * key: EndLoop
	 * 
	 * 
	 * Others seen
	 * key: Decal: 0
	 * key: Enum: Left
	 * key: prn: Bip01 L Hand
	 * key: Detach
	 * key: a:3
	 * key: Enum: Unequip
	 * key: Fire
	 * key: BlendOut:8
	 * 
	 * 
	 * locomotion
	 * key: m:L
	 * key: m:2
	 * 
	 * idleanims
	 * key: Enum: Weapon1
	 * key: Start and key: End (also hand lower case wrapped around it(what?))
	 * 
	 * File: C:\game media\Fallout\meshes\effects\forcefieldfx\forcefieldleftglow02.nif
	 * textKey:  -GlobalRatio 100 -GlobalCompressFloats true -GlobalDontCompress false
	 * textParam:
	 * time: 1.0E-5
	 * 
	 * 
	 * I see this after a long list of sounds playing
	 * File: C:\game media\Fallout\meshes\effects\tranqexitdoorfx.nif
	 * textKey: enum 
	 * textParam: StopSounds
	 * 
	 * 
	 * DOUBLE param space seprated, look like the second is a name given to the sound so the stop sound can stop it individually
	 * File: C:\game media\Fallout\meshes\effects\ambient\fxdustwhirlwind01.nif
	 * textKey: start
	 * textParam:
	 * time: 0.0
	 * textKey: Sound
	 * textParam: AMBDustDevilLPM SoundNodeDustDevil
	 * time: 1.0E-5
	 * textKey: Enum
	 * textParam: StopSounds SoundNodeDustDevil
	 * time: 9.766667
	 * textKey: end
	 * textParam:
	 * time: 12.0
	 * 
	 */
}
