package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysColorModifier;

public class J3dNiPSysColorModifier extends J3dNiPSysModifier
{

	public J3dNiPSysColorModifier(NiPSysColorModifier niPSysModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysModifier, niToJ3dData);
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{

	}

}
