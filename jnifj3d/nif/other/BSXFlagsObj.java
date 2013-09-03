package nif.other;

import nif.niobject.bs.BSXFlags;

public class BSXFlagsObj
{
	public boolean[] flags = new boolean[32];

	public BSXFlagsObj(BSXFlags bSXFlags)
	{
		// Bit 0 : enable animation, clutter/apple01.nif has this on,
		// but is not animated
		// Bit 1 : enable collision
		// Bit 2 : unknown, usually zero
		// Bit 3 : toggable? unknown, set to 1 on oblivion signs, looks
		// like isDynamic , will react to being hit, or maybe is activatable
		// arenaheavybag01.nif has it on and is not activatable. Must mean dynamical, benirusdoor is on.
		// possibly means file contains OL_ANIM_STATIC values
		// diamond clutter does not have it on
		// Bit 4 : unknown, usually zero
		// Bit 5 : trigger?

		for (int i = 0; i < 32; i++)
		{
			flags[i] = (((bSXFlags.integerData >> i) & 0x01) > 0);
		}

	}

	public String toString()
	{
		String ret = "";
		for (int i = 0; i < 32; i++)
		{
			ret += i + ":" + flags[i] + " ";
		}
		return ret;
	}
}