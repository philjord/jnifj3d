package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSWindModifier;

public class J3dBSWindModifier extends J3dNiPSysModifier
{

	public J3dBSWindModifier(BSWindModifier bSWindModifier, NiToJ3dData niToJ3dData)
	{
		super(bSWindModifier, niToJ3dData);

	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		//TODO: this
	}
}
