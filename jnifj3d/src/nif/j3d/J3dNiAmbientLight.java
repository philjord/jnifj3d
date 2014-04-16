package nif.j3d;

import nif.niobject.NiAmbientLight;

public class J3dNiAmbientLight extends J3dNiLight
{
	public J3dNiAmbientLight(NiAmbientLight niAmbientLight, NiToJ3dData niToJ3dData)
	{
		super(niAmbientLight, niToJ3dData);
		niToJ3dData.put(niAmbientLight, this);

	}

}
