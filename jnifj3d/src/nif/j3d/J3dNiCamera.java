package nif.j3d;

import nif.niobject.NiCamera;

public class J3dNiCamera extends J3dNiAVObject
{
	public J3dNiCamera(NiCamera niCamera, NiToJ3dData niToJ3dData)
	{
		super(niCamera, niToJ3dData);
		niToJ3dData.put(niCamera, this);

	}

}
