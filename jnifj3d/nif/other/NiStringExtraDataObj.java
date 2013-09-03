package nif.other;

import nif.niobject.NiStringExtraData;

public class NiStringExtraDataObj
{
	public String stringData;

	public NiStringExtraDataObj(NiStringExtraData niStringExtraData)
	{
		this.stringData = niStringExtraData.stringData;
	}

	public String toString()
	{
		return stringData;
	}
}