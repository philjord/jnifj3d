package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysGrowFadeModifier;

public class J3dNiPSysGrowFadeModifier extends J3dNiPSysModifier
{
	public J3dNiPSysGrowFadeModifier(NiPSysGrowFadeModifier niPSysGrowFadeModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysGrowFadeModifier, niToJ3dData);
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{

	}

	@Override
	public void particleCreated(int id)
	{

	}

}
