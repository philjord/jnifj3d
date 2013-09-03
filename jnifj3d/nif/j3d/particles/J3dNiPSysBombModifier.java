package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysBombModifier;

public class J3dNiPSysBombModifier extends J3dNiPSysModifier
{

	public J3dNiPSysBombModifier(NiPSysBombModifier niPSysBombModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysBombModifier, niToJ3dData);

	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		//TODO: this
	}
}
