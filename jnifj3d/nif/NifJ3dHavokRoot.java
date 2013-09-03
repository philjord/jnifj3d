package nif;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;

public class NifJ3dHavokRoot
{
	private NiToJ3dData niToJ3dData;

	private J3dNiAVObject havokRoot;

	public NifJ3dHavokRoot(J3dNiAVObject havokRoot, NiToJ3dData niAVObjects)
	{
		this.havokRoot = havokRoot;
		niToJ3dData = niAVObjects;
	}

	public J3dNiAVObject getHavokRoot()
	{

		return havokRoot;
	}

	public NiToJ3dData getNiToJ3dData()
	{
		return niToJ3dData;
	}
}
