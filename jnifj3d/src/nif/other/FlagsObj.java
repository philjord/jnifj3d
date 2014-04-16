package nif.other;

import nif.basic.NifFlags;

public class FlagsObj
{
	public boolean[] flags = new boolean[16];

	public FlagsObj(NifFlags nPflags)
	{
		for (int i = 0; i < 16; i++)
		{
			flags[i] = (((nPflags.flags >> i) & 0x01) > 0);
		}

	}

	public String toString()
	{
		String ret = "";
		for (int i = 0; i < 16; i++)
		{
			ret += i + ":" + flags[i] + " ";
		}
		return ret;
	}
}
